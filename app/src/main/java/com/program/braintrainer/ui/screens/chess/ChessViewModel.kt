package com.program.braintrainer.ui.screens.chess

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.program.braintrainer.chess.model.Board
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module
import com.program.braintrainer.chess.model.Piece
import com.program.braintrainer.chess.model.PieceType
import com.program.braintrainer.chess.model.Problem
import com.program.braintrainer.chess.model.Square
import com.program.braintrainer.chess.model.data.ProblemLoader
import com.program.braintrainer.chess.model.data.SettingsManager
import com.program.braintrainer.chess.parser.FenParser
import com.program.braintrainer.chess.solver.UniversalPuzzleSolver
import com.program.braintrainer.gamification.AchievementManager
import com.program.braintrainer.gamification.PuzzleResultData
import com.program.braintrainer.rules.Module1Rules
import com.program.braintrainer.rules.Module2Rules
import com.program.braintrainer.rules.Module3Rules
import com.program.braintrainer.rules.PuzzleRules
import com.program.braintrainer.score.ScoreCalculator
import com.program.braintrainer.score.ScoreManager
import com.program.braintrainer.score.ScoringParams
import com.program.braintrainer.stats.AttemptOutcome
import com.program.braintrainer.stats.AttemptRepository
import com.program.braintrainer.stats.PuzzleAttempt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.program.braintrainer.chess.model.Color as ChessColor

/**
 * Drži celokupan tok jedne sesije zagonetki.
 *
 * Ranije je sve ovo živelo u ChessScreen composable-u kao 24 `remember`
 * promenljive i pet `LaunchedEffect`-a. Ovde je stanje na jednom mestu, tajmer i
 * reprodukcija rešenja su obični [Job]-ovi, a UI dobija samo [ChessUiState].
 *
 * ViewModel namerno ne drži Context: poruke se emituju kao [ChessUiEvent] i
 * [PuzzleOutcome], a UI ih prevodi preko `stringResource`.
 */
class ChessViewModel(
    private val module: Module,
    private val difficulty: Difficulty,
    private val problemLoader: ProblemLoader,
    private val scoreManager: ScoreManager,
    private val settingsManager: SettingsManager,
    private val achievementManager: AchievementManager,
    private val attemptRepository: AttemptRepository,
    /** Prazno za običnu sesiju; popunjeno kad se vežbaju baš određene zagonetke. */
    private val puzzleIds: List<String> = emptyList(),
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val scoringParams: ScoringParams = ScoringParams()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChessUiState())
    val uiState: StateFlow<ChessUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ChessUiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<ChessUiEvent> = _events.asSharedFlow()

    private var session: List<Problem> = emptyList()
    private var currentIndex = 0
    private var correctStreak = 0

    private var isPremium = false
    private var isSoundEnabled = true

    private var timerJob: Job? = null

    /** Zagonetka teče — tajmer treba da radi čim je ekran u prvom planu. */
    private var isTimerActive = false

    /** Dok je aplikacija u pozadini vreme ne teče, da se ne gubi vremenski bonus. */
    private var isScreenInForeground = true
    private var solutionPlaybackJob: Job? = null
    private var hintJob: Job? = null

    init {
        viewModelScope.launch {
            settingsManager.settingsFlow.collect { settings ->
                isPremium = settings.isPremiumUser
                isSoundEnabled = settings.isSoundEnabled
            }
        }
        viewModelScope.launch {
            achievementManager.newlyUnlockedAchievementFlow.collect { achievement ->
                _events.emit(ChessUiEvent.AchievementUnlocked(achievement.id))
            }
        }

        val snapshot = savedStateHandle.get<String>(KEY_SNAPSHOT)?.let(::decodeSnapshot)
        if (snapshot != null) restoreSession(snapshot) else loadSession()

        // Snimanje ide iz jednog mesta: sve što UI vidi, vidi i snapshot.
        viewModelScope.launch {
            uiState.collect { state -> persist(state) }
        }
    }

    // ------------------------------------------------- preživljavanje procesa

    /**
     * Ono što je potrebno da se partija nastavi tamo gde je stala. Zagonetke se
     * pamte po ID-ju, a ne cele — sesija se posle ubijanja procesa pročita iz
     * assets-a preko [ProblemLoader.loadProblemsByIds].
     */
    @Serializable
    private data class SessionSnapshot(
        val puzzleIds: List<String>,
        val currentIndex: Int,
        val correctStreak: Int,
        val boardFen: String,
        val elapsedSeconds: Int,
        val playerMoveCount: Int,
        val mistakes: Int,
        val usedSolution: Boolean,
        val puzzleFinished: Boolean
    )

    private fun persist(state: ChessUiState) {
        if (state.isLoading || session.isEmpty()) return

        if (state.showSessionEndDialog) {
            // Sesija je gotova; nema šta da se nastavlja.
            savedStateHandle.remove<String>(KEY_SNAPSHOT)
            return
        }

        val snapshot = SessionSnapshot(
            puzzleIds = session.map { it.id },
            currentIndex = currentIndex,
            correctStreak = correctStreak,
            boardFen = state.board.toFEN(),
            elapsedSeconds = state.elapsedSeconds,
            playerMoveCount = state.playerMoveCount,
            mistakes = state.mistakes,
            usedSolution = state.usedSolution,
            puzzleFinished = state.outcome != null || state.showNoMoreMovesDialog
        )
        savedStateHandle[KEY_SNAPSHOT] = snapshotJson.encodeToString(snapshot)
    }

    private fun decodeSnapshot(stored: String): SessionSnapshot? = try {
        snapshotJson.decodeFromString<SessionSnapshot>(stored)
    } catch (exception: Exception) {
        println("ERROR: Could not restore chess session. ${exception.message}")
        null
    }

    private fun restoreSession(snapshot: SessionSnapshot) {
        viewModelScope.launch {
            val restored = withContext(Dispatchers.IO) {
                problemLoader.loadProblemsByIds(module, difficulty, snapshot.puzzleIds)
            }
            if (restored.size != snapshot.puzzleIds.size) {
                // Assets su se promenili ispod nas — bolje nova sesija nego pola stare.
                loadSession()
                return@launch
            }

            session = restored
            correctStreak = snapshot.correctStreak
            _uiState.update { it.copy(isLoading = false, sessionSize = session.size) }

            if (snapshot.puzzleFinished) {
                // Proces je ubijen dok je stajao dijalog o ishodu; ta zagonetka je
                // već obračunata, pa se kreće od sledeće.
                if (snapshot.currentIndex + 1 >= session.size) {
                    loadSession()
                    return@launch
                }
                currentIndex = snapshot.currentIndex + 1
                startCurrentPuzzle()
                return@launch
            }

            currentIndex = snapshot.currentIndex.coerceIn(0, session.lastIndex)
            startCurrentPuzzle()

            val board = FenParser.parseFenToBoard(snapshot.boardFen).first
            _uiState.update {
                it.copy(
                    board = board,
                    selectedSquare = firstWhiteSquare(board),
                    elapsedSeconds = snapshot.elapsedSeconds,
                    playerMoveCount = snapshot.playerMoveCount,
                    mistakes = snapshot.mistakes,
                    usedSolution = snapshot.usedSolution
                )
            }
        }
    }

    // ---------------------------------------------------------------- sesija

    private fun loadSession() {
        viewModelScope.launch {
            session = withContext(Dispatchers.IO) {
                if (puzzleIds.isEmpty()) {
                    problemLoader.loadRandomProblems(module, difficulty, PUZZLES_PER_SESSION)
                } else {
                    problemLoader.loadProblemsByIds(
                        module,
                        difficulty,
                        puzzleIds.take(PUZZLES_PER_SESSION)
                    )
                }
            }
            currentIndex = 0
            _uiState.update { it.copy(isLoading = false, sessionSize = session.size) }
            startCurrentPuzzle()
        }
    }

    private fun startCurrentPuzzle() {
        val problem = session.getOrNull(currentIndex) ?: return
        val board = FenParser.parseFenToBoard(problem.fen).first

        _uiState.update {
            it.copy(
                board = board,
                selectedSquare = firstWhiteSquare(board),
                optimalMoves = problem.solution.moves.size,
                solutionMoveCount = problem.solution.moves.size,
                currentIndexInSession = currentIndex,
                isLastPuzzle = currentIndex + 1 >= session.size,
                elapsedSeconds = 0,
                playerMoveCount = 0,
                mistakes = 0,
                showSolutionPath = false,
                isPlayingSolution = false,
                solutionMoveIndex = -1,
                usedSolution = false,
                highlightedHintMove = null,
                outcome = null,
                showNoMoreMovesDialog = false,
                defendedSquareBoard = null
            )
        }
        startTimer()
    }

    fun onNextPuzzle() {
        cancelHint()
        if (currentIndex + 1 < session.size) {
            currentIndex++
            startCurrentPuzzle()
        } else {
            stopTimer()
            _uiState.update {
                it.copy(
                    outcome = null,
                    showNoMoreMovesDialog = false,
                    showSessionEndDialog = true,
                    totalXpAtSessionEnd = scoreManager.getTotalXp()
                )
            }
        }
    }

    // ---------------------------------------------------------------- tajmer

    private fun startTimer() {
        isTimerActive = true
        restartTicker()
    }

    private fun stopTimer() {
        isTimerActive = false
        restartTicker()
    }

    /** Poziva se iz ekrana na `ON_STOP` — odbrojavanje staje na zatečenoj sekundi. */
    fun onScreenPaused() {
        isScreenInForeground = false
        restartTicker()
    }

    /** Poziva se iz ekrana na `ON_START` — nastavlja se ako zagonetka još traje. */
    fun onScreenResumed() {
        isScreenInForeground = true
        restartTicker()
    }

    private fun restartTicker() {
        timerJob?.cancel()
        timerJob = if (isTimerActive && isScreenInForeground) {
            viewModelScope.launch {
                while (true) {
                    delay(1000L)
                    _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                }
            }
        } else {
            null
        }
    }

    // ------------------------------------------------------------ potez igrača

    fun onSquareClick(clicked: Square) {
        val state = _uiState.value
        if (state.isBoardLocked) return

        val board = state.board
        val pieceOnClicked = board.getPiece(clicked)
        val selected = state.selectedSquare

        if (selected == null) {
            if (pieceOnClicked != null && pieceOnClicked.color == PLAYER_COLOR) {
                _uiState.update { it.copy(selectedSquare = clicked) }
            }
            return
        }

        // Klik na sopstvenu figuru samo pomera izbor.
        if (selected == clicked || (pieceOnClicked != null && pieceOnClicked.color == PLAYER_COLOR)) {
            _uiState.update { it.copy(selectedSquare = clicked) }
            return
        }

        if (!board.isValidMove(selected, clicked)) {
            _events.tryEmit(ChessUiEvent.InvalidMove)
            return
        }

        if (module == Module.Module1 && pieceOnClicked == null) {
            _events.tryEmit(ChessUiEvent.Module1MustCapture)
            return
        }

        val newBoard = board.applyMove(selected, clicked) ?: return

        // Moduli 2 i 3: sletanje na branjeno polje je greška, potez se poništava.
        if (module != Module.Module1 && newBoard.getAttackedSquares(ChessColor.BLACK).contains(clicked)) {
            _uiState.update {
                it.copy(
                    defendedSquareBoard = newBoard,
                    selectedSquare = selected,
                    mistakes = it.mistakes + 1
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                board = newBoard,
                selectedSquare = clicked,
                playerMoveCount = it.playerMoveCount + 1
            )
        }
        evaluatePosition(newBoard)
    }

    private fun evaluatePosition(board: Board) {
        when (module) {
            Module.Module1 -> when {
                !board.hasBlackPiecesRemaining() -> finishPuzzle(isSuccess = true)
                !board.hasAnyLegalCaptureMove(PLAYER_COLOR) -> {
                    stopTimer()
                    correctStreak = 0
                    scoreManager.resetPerfectStreak()
                    recordAttempt(_uiState.value, AttemptOutcome.FAILED, earnedXp = 0)
                    _uiState.update { it.copy(showNoMoreMovesDialog = true) }
                }
            }

            Module.Module2 -> when {
                !board.hasBlackPiecesRemaining() -> finishPuzzle(isSuccess = true)
                !board.hasAnyLegalMove(PLAYER_COLOR) -> finishPuzzle(isSuccess = false)
            }

            Module.Module3 -> when {
                !board.pieces.any { it.value == BLACK_KING } -> finishPuzzle(isSuccess = true)
                !board.hasAnyLegalMove(PLAYER_COLOR) -> finishPuzzle(isSuccess = false)
            }
        }
    }

    fun onDismissDefendedSquare() {
        _uiState.update { it.copy(defendedSquareBoard = null) }
    }

    // ------------------------------------------------------------------ ishod

    private fun finishPuzzle(isSuccess: Boolean) {
        stopTimer()
        val state = _uiState.value

        if (isSoundEnabled) {
            val sound = if (isSuccess && !state.usedSolution) GameSound.SUCCESS else GameSound.FAILURE
            _events.tryEmit(ChessUiEvent.PlaySound(sound))
        }

        val optimalMoves = state.optimalMoves.takeIf { it > 0 } ?: state.playerMoveCount
        val isPerfect = state.mistakes == 0 && state.playerMoveCount <= optimalMoves

        if (isSuccess && !state.usedSolution) {
            recordSolvedStatistics(state, isPerfect)
        } else {
            scoreManager.resetPerfectStreak()
        }

        if (!isSuccess) {
            correctStreak = 0
            recordAttempt(state, AttemptOutcome.FAILED, earnedXp = 0)
            _uiState.update { it.copy(outcome = PuzzleOutcome.Failed) }
            return
        }

        if (state.usedSolution) {
            correctStreak = 0
            recordAttempt(state, AttemptOutcome.SOLVED_WITH_HELP, earnedXp = 0)
            _uiState.update { it.copy(outcome = PuzzleOutcome.SolvedWithHelp) }
            return
        }

        val score = ScoreCalculator.calculate(
            difficulty = difficulty,
            elapsedSeconds = state.elapsedSeconds,
            playerMoveCount = state.playerMoveCount,
            optimalMoves = optimalMoves,
            mistakes = state.mistakes,
            isPerfect = isPerfect,
            previousStreak = correctStreak,
            params = scoringParams
        )
        correctStreak = score.streak
        scoreManager.addXp(score.totalXp)

        val premiumBonus = if (isPremium && score.totalXp > 0) score.totalXp else 0
        if (premiumBonus > 0) scoreManager.addXp(premiumBonus)

        recordAttempt(state, AttemptOutcome.SOLVED, earnedXp = score.totalXp + premiumBonus)
        _uiState.update { it.copy(outcome = PuzzleOutcome.Solved(score, premiumBonus)) }
    }

    /**
     * Upisuje odigranu zagonetku u istoriju. Zove se za **svaki** ishod, jer
     * dnevnik grešaka živi baš od onih koje nisu rešene.
     *
     * Ponovno pokretanje iste zagonetke daje nov zapis; istorija je dnevnik
     * pokušaja, ne stanje zagonetke.
     */
    private fun recordAttempt(state: ChessUiState, outcome: AttemptOutcome, earnedXp: Int) {
        val problem = session.getOrNull(currentIndex) ?: return
        val attempt = PuzzleAttempt(
            puzzleId = problem.id,
            module = module,
            difficulty = difficulty,
            finishedAt = System.currentTimeMillis(),
            outcome = outcome,
            elapsedSeconds = state.elapsedSeconds,
            playerMoves = state.playerMoveCount,
            optimalMoves = state.solutionMoveCount,
            mistakes = state.mistakes,
            earnedXp = earnedXp
        )
        viewModelScope.launch { attemptRepository.record(attempt) }
    }

    private fun recordSolvedStatistics(state: ChessUiState, isPerfect: Boolean) {
        scoreManager.incrementTotalPuzzlesSolved()
        scoreManager.incrementSolvedInModule(module)
        scoreManager.incrementSolvedCount(module, difficulty)

        if (isPerfect) {
            scoreManager.incrementPerfectStreak()
            scoreManager.incrementPerfectSolvedCount(module, difficulty)
        } else {
            scoreManager.resetPerfectStreak()
        }

        val resultData = PuzzleResultData(
            module = module,
            difficulty = difficulty,
            wasSuccess = true,
            mistakesMade = state.mistakes,
            timeTakenSeconds = state.elapsedSeconds,
            currentStreak = scoreManager.getPerfectStreak(),
            totalPuzzlesSolved = scoreManager.getTotalPuzzlesSolved(),
            totalSolvedInModule = scoreManager.getSolvedInModule(module)
        )
        viewModelScope.launch(Dispatchers.IO) {
            achievementManager.checkAndUnlockAchievements(resultData)
        }
    }

    fun onSurrender() {
        stopTimer()
        cancelHint()
        correctStreak = 0
        recordAttempt(_uiState.value, AttemptOutcome.SURRENDERED, earnedXp = 0)
        _uiState.update { it.copy(usedSolution = true, outcome = PuzzleOutcome.Surrendered) }
    }

    // ------------------------------------------------------------------- hint

    fun onHintClick() {
        if (hintJob?.isActive == true) return

        hintJob = viewModelScope.launch {
            val solution = withContext(Dispatchers.Default) {
                UniversalPuzzleSolver(rulesForModule()).solve(_uiState.value.board)
            }
            if (!solution.isSolved) {
                _events.tryEmit(ChessUiEvent.SolverFailed)
                return@launch
            }
            for (move in solution.path.take(HINT_MOVES)) {
                _uiState.update { it.copy(highlightedHintMove = move) }
                delay(HINT_STEP_MS)
            }
            _uiState.update { it.copy(highlightedHintMove = null) }
        }
    }

    private fun cancelHint() {
        hintJob?.cancel()
        hintJob = null
        _uiState.update { it.copy(highlightedHintMove = null) }
    }

    // --------------------------------------------------------------- rešenje

    fun onShowSolution() {
        val state = _uiState.value
        cancelHint()
        solutionPlaybackJob?.cancel()

        val turningOn = !state.showSolutionPath
        if (turningOn) {
            stopTimer()
            correctStreak = 0
        }

        val problem = session.getOrNull(currentIndex)
        val initialBoard = problem?.let { FenParser.parseFenToBoard(it.fen).first } ?: state.board

        _uiState.update {
            it.copy(
                usedSolution = it.usedSolution || turningOn,
                showSolutionPath = turningOn,
                isPlayingSolution = false,
                solutionMoveIndex = -1,
                outcome = null,
                showNoMoreMovesDialog = false,
                board = initialBoard,
                selectedSquare = firstWhiteSquare(initialBoard)
            )
        }
    }

    fun onRestartPuzzle() {
        cancelHint()
        val problem = session.getOrNull(currentIndex) ?: return
        val board = FenParser.parseFenToBoard(problem.fen).first
        _uiState.update {
            it.copy(
                board = board,
                selectedSquare = firstWhiteSquare(board),
                playerMoveCount = 0,
                mistakes = 0,
                highlightedHintMove = null
            )
        }
        startTimer()
    }

    fun onNextSolutionMove() {
        solutionPlaybackJob?.cancel()
        _uiState.update { it.copy(isPlayingSolution = false) }
        stepSolutionForward()
    }

    fun onPreviousSolutionMove() {
        solutionPlaybackJob?.cancel()
        _uiState.update { it.copy(isPlayingSolution = false) }

        val moves = session.getOrNull(currentIndex)?.solution?.moves ?: return
        val targetIndex = (_uiState.value.solutionMoveIndex - 1).coerceAtLeast(-1)
        applySolutionUpTo(moves, targetIndex)
    }

    fun onPlayPauseSolution() {
        if (_uiState.value.isPlayingSolution) {
            solutionPlaybackJob?.cancel()
            _uiState.update { it.copy(isPlayingSolution = false) }
            return
        }

        _uiState.update { it.copy(isPlayingSolution = true) }
        solutionPlaybackJob = viewModelScope.launch {
            val moves = session.getOrNull(currentIndex)?.solution?.moves.orEmpty()
            while (_uiState.value.solutionMoveIndex < moves.size) {
                stepSolutionForward()
                delay(SOLUTION_STEP_MS)
            }
            _uiState.update { it.copy(isPlayingSolution = false) }
        }
    }

    private fun stepSolutionForward() {
        val moves = session.getOrNull(currentIndex)?.solution?.moves ?: return
        val state = _uiState.value
        val nextIndex = if (state.solutionMoveIndex == -1) 0 else state.solutionMoveIndex
        if (nextIndex >= moves.size) return

        val (start, end) = FenParser.parseMove(moves[nextIndex])
        val newBoard = state.board.applyMove(start, end) ?: return
        _uiState.update {
            it.copy(board = newBoard, solutionMoveIndex = nextIndex + 1, selectedSquare = end)
        }
    }

    /** Premotava rešenje na [targetIndex] odigranih poteza, računajući od početne pozicije. */
    private fun applySolutionUpTo(moves: List<String>, targetIndex: Int) {
        val problem = session.getOrNull(currentIndex) ?: return
        val initialBoard = FenParser.parseFenToBoard(problem.fen).first

        if (targetIndex <= 0) {
            _uiState.update {
                it.copy(
                    board = initialBoard,
                    selectedSquare = firstWhiteSquare(initialBoard),
                    solutionMoveIndex = -1
                )
            }
            return
        }

        var board = initialBoard
        for (i in 0 until targetIndex) {
            val (start, end) = FenParser.parseMove(moves[i])
            board = board.applyMove(start, end) ?: board
        }
        _uiState.update {
            it.copy(
                board = board,
                solutionMoveIndex = targetIndex,
                selectedSquare = FenParser.parseMove(moves[targetIndex - 1]).second
            )
        }
    }

    // ------------------------------------------------------------- pomoćne

    private fun rulesForModule(): PuzzleRules = when (module) {
        Module.Module1 -> Module1Rules()
        Module.Module2 -> Module2Rules()
        Module.Module3 -> Module3Rules()
    }

    private fun firstWhiteSquare(board: Board): Square? =
        board.pieces.entries.firstOrNull { it.value.color == PLAYER_COLOR }?.key

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        solutionPlaybackJob?.cancel()
        hintJob?.cancel()
    }

    private companion object {
        const val KEY_SNAPSHOT = "chess_session_snapshot"
        val snapshotJson = Json { ignoreUnknownKeys = true }

        const val PUZZLES_PER_SESSION = 10
        const val HINT_MOVES = 3
        const val HINT_STEP_MS = 1500L
        const val SOLUTION_STEP_MS = 1500L
        val PLAYER_COLOR = ChessColor.WHITE
        val BLACK_KING = Piece(PieceType.KING, ChessColor.BLACK)
    }
}
