package com.program.braintrainer.ui.screens

import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets // <-- DODAT IMPORT
import androidx.compose.foundation.layout.safeDrawing // <-- DODAT IMPORT
import androidx.compose.foundation.layout.windowInsetsPadding // <-- DODAT IMPORT
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.program.braintrainer.R
import com.program.braintrainer.chess.model.*
import com.program.braintrainer.chess.model.data.AppSettings
import com.program.braintrainer.chess.model.data.ProblemLoader
import com.program.braintrainer.chess.model.data.SettingsManager
import com.program.braintrainer.chess.parser.FenParser
import com.program.braintrainer.chess.solver.UniversalPuzzleSolver
import com.program.braintrainer.gamification.AchievementManager
import com.program.braintrainer.gamification.PuzzleResultData
import com.program.braintrainer.rules.Module1Rules
import com.program.braintrainer.rules.Module2Rules
import com.program.braintrainer.rules.Module3Rules
import com.program.braintrainer.score.PuzzleScore
import com.program.braintrainer.score.ScoreCalculator
import com.program.braintrainer.score.ScoreManager
import com.program.braintrainer.score.ScoringParams
import com.program.braintrainer.util.playSound
import com.program.braintrainer.ui.difficultyLabel
import com.program.braintrainer.ui.moduleTitle
import com.program.braintrainer.ui.theme.BrainTrainerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.program.braintrainer.chess.model.Color as ChessColor


@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ChessScreen(
    module: Module,
    difficulty: Difficulty,
    onGameFinished: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    val scoreManager = remember { ScoreManager(context) }
    val achievementManager = remember { AchievementManager(context, settingsManager) }
    val scoringParams = remember { ScoringParams() }

    val isPremium by settingsManager.settingsFlow.map { it.isPremiumUser }.collectAsState(initial = false)

    var problems by remember { mutableStateOf<List<Problem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(key1 = module, key2 = difficulty) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val loadedProblems = ProblemLoader(context).loadProblemsForModuleAndDifficulty(module, difficulty)
            problems = loadedProblems
        }
        isLoading = false
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var currentProblemIndex by remember { mutableIntStateOf(0) }
    var currentProblem by remember { mutableStateOf<Problem?>(null) }
    var currentBoard by remember { mutableStateOf(Board()) }
    val activePlayerColor = ChessColor.WHITE
    var selectedSquare by remember { mutableStateOf<Square?>(null) }
    var showSolutionPath by remember { mutableStateOf(false) }
    var solutionMoveIndex by remember { mutableIntStateOf(-1) }
    var isPlayingSolution by remember { mutableStateOf(false) }
    var usedSolution by remember { mutableStateOf(false) }
    var showGameResultDialog by remember { mutableStateOf(false) }
    var showNoMoreMovesDialog by remember { mutableStateOf(false) }
    var gameResultMessage by remember { mutableStateOf("") }
    var showDefendedSquareDialog by remember { mutableStateOf(false) }
    var boardForDialog by remember { mutableStateOf<Board?>(null) }
    var showSessionEndDialog by remember { mutableStateOf(false) }
    var lastAwardedXp by remember { mutableIntStateOf(0) }
    var elapsedTimeInSeconds by remember { mutableIntStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var defendedSquareMistakes by remember { mutableIntStateOf(0) }
    var playerMoveCount by remember { mutableIntStateOf(0) }
    var correctStreak by remember { mutableIntStateOf(0) }
    var hintMoves by remember { mutableStateOf<List<Move>>(emptyList()) }
    var isShowingHint by remember { mutableStateOf(false) }
    var hintMoveIndex by remember { mutableIntStateOf(0) }
    val problemsInSession = remember(problems) {
        if (problems.size >= 10) problems.shuffled().take(10) else problems.shuffled()
    }
    val currentSessionProblemIndex = if (problemsInSession.isNotEmpty()) currentProblemIndex % problemsInSession.size else 0

    fun resetPuzzleState() {
        currentProblem?.let {
            val (board, _) = FenParser.parseFenToBoard(it.fen)
            currentBoard = board
            selectedSquare = board.pieces.entries.firstOrNull { p -> p.value.color == ChessColor.WHITE }?.key
            playerMoveCount = 0
            defendedSquareMistakes = 0
            isShowingHint = false
            hintMoves = emptyList()
        }
    }

    LaunchedEffect(problemsInSession, currentProblemIndex) {
        if (problemsInSession.isNotEmpty()) {
            val problem = problemsInSession[currentProblemIndex]
            currentProblem = problem
            resetPuzzleState()
            elapsedTimeInSeconds = 0
            isTimerRunning = true
            showSolutionPath = false
            solutionMoveIndex = -1
            isPlayingSolution = false
            usedSolution = false
            lastAwardedXp = 0
            showGameResultDialog = false
            showNoMoreMovesDialog = false
            gameResultMessage = ""
        }
    }

    fun stopTimer() { isTimerRunning = false }
    fun startTimer() { isTimerRunning = true }

    fun showHint() {
        coroutineScope.launch(Dispatchers.Default) {
            val rules = when (module) {
                Module.Module1 -> Module1Rules()
                Module.Module2 -> Module2Rules()
                Module.Module3 -> Module3Rules()
            }
            val solver = UniversalPuzzleSolver(rules)
            val solution = solver.solve(currentBoard)
            launch(Dispatchers.Main) {
                if (solution.isSolved) {
                    hintMoves = solution.path.take(3)
                    hintMoveIndex = 0
                    isShowingHint = true
                } else {
                    snackbarHostState.showSnackbar(context.getString(R.string.snackbar_solver_failed))
                    startTimer()
                }
            }
        }
    }

    val onHintClick: () -> Unit = { showHint() }

    fun checkGameStatus(isSuccess: Boolean) {
        stopTimer()

        coroutineScope.launch {
            if (settingsManager.settingsFlow.first().isSoundEnabled) {
                val soundToPlay = if (isSuccess && !usedSolution) R.raw.succes else R.raw.failed
                playSound(context, soundToPlay)
            }
        }

        val optimalMoves = currentProblem?.solution?.moves?.size ?: playerMoveCount
        val isPerfect = defendedSquareMistakes == 0 && playerMoveCount <= optimalMoves

        if (isSuccess && !usedSolution) {
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
                mistakesMade = defendedSquareMistakes,
                timeTakenSeconds = elapsedTimeInSeconds,
                currentStreak = scoreManager.getPerfectStreak(),
                totalPuzzlesSolved = scoreManager.getTotalPuzzlesSolved(),
                totalSolvedInModule = scoreManager.getSolvedInModule(module)
            )
            coroutineScope.launch(Dispatchers.IO) {
                achievementManager.checkAndUnlockAchievements(resultData)
            }
        } else {
            scoreManager.resetPerfectStreak()
        }

        if (!isSuccess) {
            correctStreak = 0
            gameResultMessage = when (module) {
                Module.Module1 -> context.getString(R.string.game_result_fail_no_captures)
                else -> context.getString(R.string.game_result_fail_no_moves)
            }
            showGameResultDialog = true
            return
        }

        if (usedSolution) {
            correctStreak = 0
            lastAwardedXp = 0
            gameResultMessage = context.getString(R.string.game_result_solved_with_help)
            showGameResultDialog = true
            return
        }

        val score = ScoreCalculator.calculate(
            difficulty = difficulty,
            elapsedSeconds = elapsedTimeInSeconds,
            playerMoveCount = playerMoveCount,
            optimalMoves = optimalMoves,
            mistakes = defendedSquareMistakes,
            isPerfect = isPerfect,
            previousStreak = correctStreak,
            params = scoringParams
        )
        correctStreak = score.streak
        lastAwardedXp = score.totalXp
        scoreManager.addXp(score.totalXp)

        var message = buildResultMessage(context, score)
        if (isPremium && score.totalXp > 0) {
            scoreManager.addXp(score.totalXp)
            message += context.getString(R.string.game_result_premium_bonus, score.totalXp)
        }
        gameResultMessage = message
        showGameResultDialog = true
    }

    val onNextPuzzle: () -> Unit = {
        showGameResultDialog = false
        showNoMoreMovesDialog = false
        if (currentProblemIndex + 1 < problemsInSession.size) {
            currentProblemIndex++
        } else {
            stopTimer()
            showSessionEndDialog = true
        }
    }

    val onShowSolution: () -> Unit = {
        if (!showSolutionPath) {
            stopTimer()
            usedSolution = true
            correctStreak = 0
        }
        showGameResultDialog = false
        showNoMoreMovesDialog = false
        showSolutionPath = !showSolutionPath
        isPlayingSolution = false
        currentProblem?.let {
            val (initialBoard, _) = FenParser.parseFenToBoard(it.fen)
            currentBoard = initialBoard
            selectedSquare = initialBoard.pieces.entries.firstOrNull { p -> p.value.color == ChessColor.WHITE }?.key
            solutionMoveIndex = -1
        }
    }

    val onRestartPuzzle: () -> Unit = {
        resetPuzzleState()
        startTimer()
    }

    val onPreviousMove: () -> Unit = {
        isPlayingSolution = false
        val solutionMoves = currentProblem?.solution?.moves
        if (solutionMoves != null) {
            if (solutionMoveIndex > 0) {
                solutionMoveIndex--
                val (initialBoard, _) = FenParser.parseFenToBoard(currentProblem!!.fen)
                var tempBoard = initialBoard
                for (i in 0 until solutionMoveIndex) {
                    val (start, end) = FenParser.parseMove(solutionMoves[i])
                    tempBoard = tempBoard.applyMove(start, end) ?: tempBoard
                }
                currentBoard = tempBoard
                selectedSquare = if (solutionMoveIndex > 0) {
                    FenParser.parseMove(solutionMoves[solutionMoveIndex - 1]).second
                } else {
                    initialBoard.pieces.entries.firstOrNull { it.value.color == ChessColor.WHITE }?.key
                }
            } else if (solutionMoveIndex <= 0) {
                val (initialBoard, _) = FenParser.parseFenToBoard(currentProblem!!.fen)
                currentBoard = initialBoard
                selectedSquare = initialBoard.pieces.entries.firstOrNull { it.value.color == ChessColor.WHITE }?.key
                solutionMoveIndex = -1
            }
        }
    }

    fun playNextSolutionMove() {
        val solutionMoves = currentProblem?.solution?.moves
        if (solutionMoves != null) {
            val nextMoveIndex = if (solutionMoveIndex == -1) 0 else solutionMoveIndex
            if (nextMoveIndex < solutionMoves.size) {
                val (start, end) = FenParser.parseMove(solutionMoves[nextMoveIndex])
                currentBoard.applyMove(start, end)?.let { newBoard ->
                    currentBoard = newBoard
                    solutionMoveIndex = nextMoveIndex + 1
                    selectedSquare = end
                }
            }
        }
    }

    val onNextMove: () -> Unit = {
        isPlayingSolution = false
        playNextSolutionMove()
    }

    LaunchedEffect(isPlayingSolution, currentProblem) {
        if (isPlayingSolution) {
            val solutionMoves = currentProblem?.solution?.moves
            while (isPlayingSolution && solutionMoves != null && solutionMoveIndex < solutionMoves.size) {
                playNextSolutionMove()
                delay(1500L)
            }
            isPlayingSolution = false
        }
    }

    val onSquareClick: (Square) -> Unit = click@{ clickedSquare ->
        if (showSolutionPath || showGameResultDialog || showSessionEndDialog || showNoMoreMovesDialog || isShowingHint) {
            return@click
        }
        val pieceOnClickedSquare = currentBoard.getPiece(clickedSquare)
        if (selectedSquare == null) {
            if (pieceOnClickedSquare != null && pieceOnClickedSquare.color == activePlayerColor) {
                selectedSquare = clickedSquare
            }
            return@click
        }
        val startSquare = selectedSquare!!
        if (startSquare == clickedSquare || (pieceOnClickedSquare != null && pieceOnClickedSquare.color == activePlayerColor)) {
            selectedSquare = clickedSquare
            return@click
        }
        if (currentBoard.isValidMove(startSquare, clickedSquare)) {
            if (module == Module.Module1 && currentBoard.getPiece(clickedSquare) == null) {
                coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.snackbar_module1_must_capture)) }
                return@click
            }
            val newBoard = currentBoard.applyMove(startSquare, clickedSquare)
            if (newBoard != null) {
                if (module == Module.Module2 || module == Module.Module3) {
                    val attackedByBlackOnNewBoard = newBoard.getAttackedSquares(ChessColor.BLACK)
                    if (attackedByBlackOnNewBoard.contains(clickedSquare)) {
                        boardForDialog = newBoard
                        showDefendedSquareDialog = true
                        selectedSquare = startSquare
                        defendedSquareMistakes++
                        return@click
                    }
                }
                currentBoard = newBoard
                selectedSquare = clickedSquare
                playerMoveCount++
                when (module) {
                    Module.Module1 -> {
                        if (!currentBoard.hasBlackPiecesRemaining()) {
                            checkGameStatus(isSuccess = true)
                        } else if (!currentBoard.hasAnyLegalCaptureMove(activePlayerColor)) {
                            stopTimer()
                            correctStreak = 0
                            scoreManager.resetPerfectStreak()
                            showNoMoreMovesDialog = true
                        }
                    }
                    Module.Module2 -> {
                        if (!currentBoard.hasBlackPiecesRemaining()) checkGameStatus(isSuccess = true)
                        else if (!currentBoard.hasAnyLegalMove(activePlayerColor)) checkGameStatus(isSuccess = false)
                    }
                    Module.Module3 -> {
                        if (!currentBoard.pieces.any { it.value == Piece(PieceType.KING, ChessColor.BLACK) }) checkGameStatus(isSuccess = true)
                        else if (!currentBoard.hasAnyLegalMove(activePlayerColor)) checkGameStatus(isSuccess = false)
                    }
                }
            }
        } else {
            coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.snackbar_invalid_move)) }
        }
    }

    val onSurrender: () -> Unit = {
        stopTimer()
        usedSolution = true
        correctStreak = 0
        gameResultMessage = context.getString(R.string.game_result_surrendered)
        showGameResultDialog = true
    }

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (true) {
                delay(1000L)
                elapsedTimeInSeconds++
            }
        }
    }

    LaunchedEffect(isShowingHint, hintMoveIndex) {
        if (isShowingHint && hintMoves.isNotEmpty() && hintMoveIndex < hintMoves.size) {
            delay(1500L)
            if (hintMoveIndex < hintMoves.size - 1) {
                hintMoveIndex++
            } else {
                isShowingHint = false
                hintMoves = emptyList()
                startTimer()
            }
        }
    }

    val currentlyHighlightedHintMove = if (isShowingHint) hintMoves.getOrNull(hintMoveIndex) else null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing) // <-- DODATA LINIJA
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            val optimalMoves = currentProblem?.solution?.moves?.size ?: 0
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val isRestartEnabled = !usedSolution && !showGameResultDialog && !showNoMoreMovesDialog

            if (isLandscape) {
                Row(modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    GameInfoPanel(
                        module = module,
                        difficulty = difficulty,
                        problemsInSession = problemsInSession,
                        currentSessionProblemIndex = currentSessionProblemIndex,
                        elapsedTime = elapsedTimeInSeconds,
                        optimalMoves = optimalMoves,
                        playerMoveCount = playerMoveCount,
                        isLandscape = true,
                        modifier = Modifier.weight(1f)
                    )
                    ChessBoardComposable(currentBoard, selectedSquare, onSquareClick, currentlyHighlightedHintMove, modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .aspectRatio(1f))
                    GameControlsPanel(
                        showSolutionPath = showSolutionPath,
                        isPlayingSolution = isPlayingSolution,
                        solutionMoveIndex = solutionMoveIndex,
                        currentProblem = currentProblem,
                        onShowSolutionClick = onShowSolution,
                        onNextPuzzleClick = onNextPuzzle,
                        onPreviousMoveClick = onPreviousMove,
                        onPlayPauseClick = { isPlayingSolution = !isPlayingSolution },
                        onNextMoveClick = onNextMove,
                        onHintClick = onHintClick,
                        onSurrenderClick = onSurrender,
                        onRestartClick = onRestartPuzzle,
                        isRestartEnabled = isRestartEnabled,
                        isLandscape = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceAround) {
                    GameInfoPanel(
                        module = module,
                        difficulty = difficulty,
                        problemsInSession = problemsInSession,
                        currentSessionProblemIndex = currentSessionProblemIndex,
                        elapsedTime = elapsedTimeInSeconds,
                        optimalMoves = optimalMoves,
                        playerMoveCount = playerMoveCount,
                        isLandscape = false
                    )
                    ChessBoardComposable(currentBoard, selectedSquare, onSquareClick, currentlyHighlightedHintMove, modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .aspectRatio(1f))
                    GameControlsPanel(
                        showSolutionPath = showSolutionPath,
                        isPlayingSolution = isPlayingSolution,
                        solutionMoveIndex = solutionMoveIndex,
                        currentProblem = currentProblem,
                        onShowSolutionClick = onShowSolution,
                        onNextPuzzleClick = onNextPuzzle,
                        onPreviousMoveClick = onPreviousMove,
                        onPlayPauseClick = { isPlayingSolution = !isPlayingSolution },
                        onNextMoveClick = onNextMove,
                        onHintClick = onHintClick,
                        onSurrenderClick = onSurrender,
                        onRestartClick = onRestartPuzzle,
                        isRestartEnabled = isRestartEnabled,
                        isLandscape = false
                    )
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))

        if (showGameResultDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.dialog_title_puzzle_status)) },
                text = { Text(gameResultMessage) },
                dismissButton = {
                    TextButton(onClick = onShowSolution) { Text(stringResource(R.string.button_solution)) }
                },
                confirmButton = {
                    TextButton(onClick = onNextPuzzle) {
                        Text(if (currentProblemIndex + 1 < problemsInSession.size) stringResource(R.string.button_next) else stringResource(R.string.button_end))
                    }
                }
            )
        }

        if (showNoMoreMovesDialog) {
            NoMoreMovesDialog(onShowSolution = onShowSolution, onNewGame = onNextPuzzle)
        }
        if (showSessionEndDialog) {
            val totalXp = scoreManager.getTotalXp()
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.dialog_title_session_end)) },
                text = {
                    Column {
                        Text(stringResource(R.string.dialog_message_session_end))
                        Text(stringResource(R.string.dialog_message_total_xp, totalXp))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showSessionEndDialog = false
                        onGameFinished()
                    }) {
                        Text(stringResource(R.string.button_main_menu))
                    }
                }
            )
        }
        if (showDefendedSquareDialog && boardForDialog != null) {
            DefendedSquareDialog(board = boardForDialog!!, onDismiss = { showDefendedSquareDialog = false })
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740, name = "Portrait Preview")
@Composable
fun PreviewChessScreenPortrait() {
    BrainTrainerTheme(
        appSettings = AppSettings(
            isSoundEnabled = true,
            appTheme = SettingsManager.AppTheme.SYSTEM,
            isPremiumUser = false
        )
    ) {
        ChessScreen(module = Module.Module2, difficulty = Difficulty.EASY, onGameFinished = {})
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 480, name = "Landscape Preview")
@Composable
fun PreviewChessScreenLandscape() {
    BrainTrainerTheme(
        appSettings = AppSettings(
            isSoundEnabled = true,
            appTheme = SettingsManager.AppTheme.SYSTEM,
            isPremiumUser = false
        )
    ) {
        ChessScreen(module = Module.Module2, difficulty = Difficulty.EASY, onGameFinished = {})
    }
}

/**
 * Sastavlja tekst dijaloga od već izračunatog rezultata. Računica je u
 * [ScoreCalculator]; ovde se samo formatira.
 */
private fun buildResultMessage(context: Context, score: PuzzleScore): String {
    val sb = StringBuilder(context.getString(R.string.game_result_success_title))
    sb.append(context.getString(R.string.game_result_base_points, score.basePoints))
    sb.append(context.getString(R.string.game_result_time_bonus, score.timeBonus))
    sb.append(context.getString(R.string.game_result_moves_penalty, score.efficiencyPenalty, score.extraMoves))
    sb.append(context.getString(R.string.game_result_mistakes_penalty, score.mistakePenalty, score.mistakes))
    if (score.isPerfect) {
        sb.append(context.getString(R.string.game_result_streak_bonus, score.streakBonus, score.streak))
    } else {
        sb.append(context.getString(R.string.game_result_streak_lost))
    }
    sb.append(context.getString(R.string.game_result_total_xp, score.totalXp))
    return sb.toString()
}
