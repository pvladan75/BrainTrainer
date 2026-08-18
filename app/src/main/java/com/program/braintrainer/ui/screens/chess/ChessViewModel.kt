package com.program.braintrainer.ui.screens.chess

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
    private var solutionPlaybackJob: Job? = null
    private var hintJob: Job? = null

    init {
        viewModelScope.launch {
            settingsManager.settingsFlow.collect { settings ->
                isPremium = settings.isPremiumUser
                isSoundEnabled = settings.isSoundEnabled
            }
        }
        loadSession()
    }

    // ---------------------------------------------------------------- sesija

    private fun loadSession() {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                problemLoader.loadProblemsForModuleAndDifficulty(module, difficulty)
            }
            session = if (loaded.size >= PUZZLES_PER_SESSION) {
                loaded.shuffled().take(PUZZLES_PER_SESSION)
            } else {
                loaded.shuffled()
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
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
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
            _uiState.update { it.copy(outcome = PuzzleOutcome.Failed) }
            return
        }

        if (state.usedSolution) {
            correctStreak = 0
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

        _uiState.update { it.copy(outcome = PuzzleOutcome.Solved(score, premiumBonus)) }
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
        const val PUZZLES_PER_SESSION = 10
        const val HINT_MOVES = 3
        const val HINT_STEP_MS = 1500L
        const val SOLUTION_STEP_MS = 1500L
        val PLAYER_COLOR = ChessColor.WHITE
        val BLACK_KING = Piece(PieceType.KING, ChessColor.BLACK)
    }
}
