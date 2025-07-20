package com.program.braintrainer.ui.screens

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.program.braintrainer.chess.model.data.ProblemLoader
import com.program.braintrainer.chess.model.Board
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module
import com.program.braintrainer.chess.model.Move
import com.program.braintrainer.chess.model.Piece
import com.program.braintrainer.chess.model.PieceType
import com.program.braintrainer.chess.model.Problem
import com.program.braintrainer.chess.model.Square
import com.program.braintrainer.chess.parser.FenParser
import com.program.braintrainer.chess.solver.UniversalPuzzleSolver
import com.program.braintrainer.rules.Module1Rules
import com.program.braintrainer.rules.Module2Rules
import com.program.braintrainer.rules.Module3Rules
import com.program.braintrainer.score.ScoreManager
import com.program.braintrainer.ui.theme.BrainTrainerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import com.program.braintrainer.chess.model.Color as ChessColor

// ===================================================================
// ===         CENTRALNO MESTO ZA PODEŠAVANJE BODOVANJA            ===
// ===================================================================
data class ScoringParams(
    val basePointsEasy: Int = 10,
    val basePointsMedium: Int = 20,
    val basePointsHard: Int = 30,
    val maxTimeForBonusEasy: Int = 30,
    val maxTimeForBonusMedium: Int = 60,
    val maxTimeForBonusHard: Int = 90,
    val penaltyPerMistake: Int = 5,
    val penaltyPerExtraMove: Int = 2,
    val streakBonusEasy: Int = 5,
    val streakBonusMedium: Int = 10,
    val streakBonusHard: Int = 15
)
// ===================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ChessScreen(
    module: Module,
    difficulty: Difficulty,
    onGameFinished: () -> Unit
) {
    val context = LocalContext.current
    val problemLoader = remember { ProblemLoader(context) }
    val scoringParams = remember { ScoringParams() }
    val problems: List<Problem> = remember(module, difficulty) {
        problemLoader.loadProblemsForModuleAndDifficulty(module, difficulty)
    }

    var currentProblemIndex by remember { mutableStateOf(0) }
    var currentProblem by remember { mutableStateOf<Problem?>(null) }
    var currentBoard by remember { mutableStateOf(Board()) }
    val activePlayerColor = ChessColor.WHITE
    var selectedSquare by remember { mutableStateOf<Square?>(null) }
    var showSolutionPath by remember { mutableStateOf(false) }
    var solutionMoveIndex by remember { mutableStateOf(-1) }
    var isPlayingSolution by remember { mutableStateOf(false) }
    var usedSolution by remember { mutableStateOf(false) }
    var showGameResultDialog by remember { mutableStateOf(false) }
    var showNoMoreMovesDialog by remember { mutableStateOf(false) }
    var gameResultMessage by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showDefendedSquareDialog by remember { mutableStateOf(false) }
    var boardForDialog by remember { mutableStateOf<Board?>(null) }
    var showSessionEndDialog by remember { mutableStateOf(false) }
    val scoreManager = remember { ScoreManager(context) }
    var lastAwardedXp by remember { mutableStateOf(0) } // NOVO: Pamtimo XP iz poslednje zagonetke
    var elapsedTimeInSeconds by remember { mutableStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var defendedSquareMistakes by remember { mutableStateOf(0) }
    var playerMoveCount by remember { mutableStateOf(0) }
    var correctStreak by remember { mutableStateOf(0) }
    var hintMoves by remember { mutableStateOf<List<Move>>(emptyList()) }
    var isShowingHint by remember { mutableStateOf(false) }
    var hintMoveIndex by remember { mutableStateOf(0) }

    fun stopTimer() {
        isTimerRunning = false
    }

    fun startTimer() {
        isTimerRunning = true
    }

    LaunchedEffect(currentProblemIndex) {
        elapsedTimeInSeconds = 0
        isTimerRunning = true
        defendedSquareMistakes = 0
        playerMoveCount = 0
        isShowingHint = false
        hintMoves = emptyList()
        showSolutionPath = false
        solutionMoveIndex = -1
        isPlayingSolution = false
        usedSolution = false
        lastAwardedXp = 0
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
                startTimer() // IZMENA: Pokreni tajmer nakon što se hint završi
            }
        }
    }

    LaunchedEffect(currentProblemIndex, problems) {
        if (problems.isNotEmpty() && currentProblemIndex < problems.size) {
            val problem = problems[currentProblemIndex]
            currentProblem = problem
            val (board, _) = FenParser.parseFenToBoard(problem.fen)
            currentBoard = board
            selectedSquare = board.pieces.entries.firstOrNull { it.value.color == ChessColor.WHITE }?.key
            showGameResultDialog = false
            showNoMoreMovesDialog = false
            gameResultMessage = ""
        }
    }

    LaunchedEffect(isPlayingSolution) {
        if (isPlayingSolution && showSolutionPath) {
            val solutionMoves = currentProblem?.solution?.moves
            if (!solutionMoves.isNullOrEmpty()) {
                val startFromIndex = if (solutionMoveIndex == -1) 0 else solutionMoveIndex
                for (i in startFromIndex until solutionMoves.size) {
                    if (!isPlayingSolution) break
                    val move = solutionMoves[i]
                    val (start, end) = FenParser.parseMove(move)
                    currentBoard.applyMove(start, end)?.let { newBoard ->
                        currentBoard = newBoard
                        solutionMoveIndex = i + 1
                        selectedSquare = end
                    }
                    delay(1200L)
                }
                isPlayingSolution = false
            }
        }
    }

    val problemsInSession = remember(problems) {
        if (problems.size >= 10) problems.take(10) else problems
    }
    val currentSessionProblemIndex = if (problemsInSession.isNotEmpty()) currentProblemIndex % problemsInSession.size else 0

    fun checkGameStatus(isSuccess: Boolean) {
        stopTimer()
        if (!isSuccess) {
            correctStreak = 0
            gameResultMessage = when (module) {
                Module.Module1 -> "Promašaj! Nema više legalnih poteza uzimanja."
                else -> "Promašaj! Nema više legalnih poteza."
            }
            showGameResultDialog = true
            return
        }
        var detailedMessage = "Zagonetka rešena!\n\n"
        var finalPoints = 0
        if (!usedSolution) {
            val basePoints = when (difficulty) {
                Difficulty.EASY -> scoringParams.basePointsEasy
                Difficulty.MEDIUM -> scoringParams.basePointsMedium
                Difficulty.HARD -> scoringParams.basePointsHard
            }
            detailedMessage += "✅ Osnovni poeni: +$basePoints\n"
            val maxTimeForBonus = when (difficulty) {
                Difficulty.EASY -> scoringParams.maxTimeForBonusEasy
                Difficulty.MEDIUM -> scoringParams.maxTimeForBonusMedium
                Difficulty.HARD -> scoringParams.maxTimeForBonusHard
            }
            val timeBonus = max(0, maxTimeForBonus - elapsedTimeInSeconds)
            detailedMessage += "🕒 Bonus za vreme: +$timeBonus\n"
            val optimalMoves = currentProblem?.solution?.moves?.size ?: playerMoveCount
            val extraMoves = max(0, playerMoveCount - optimalMoves)
            val efficiencyPenalty = extraMoves * scoringParams.penaltyPerExtraMove
            detailedMessage += "🔻 Kazna za poteze: -$efficiencyPenalty ($extraMoves poteza viška)\n"
            val mistakePenalty = defendedSquareMistakes * scoringParams.penaltyPerMistake
            detailedMessage += "🔻 Kazna za greške: -$mistakePenalty ($defendedSquareMistakes grešaka)\n"
            val streakBonus = if (defendedSquareMistakes == 0 && extraMoves == 0) {
                correctStreak++
                val bonusPerStreak = when(difficulty) {
                    Difficulty.EASY -> scoringParams.streakBonusEasy
                    Difficulty.MEDIUM -> scoringParams.streakBonusMedium
                    Difficulty.HARD -> scoringParams.streakBonusHard
                }
                val totalBonus = correctStreak * bonusPerStreak
                detailedMessage += "🔥 Bonus za niz: +$totalBonus ($correctStreak zagonetki zaredom)\n"
                totalBonus
            } else {
                correctStreak = 0
                detailedMessage += "🔥 Bonus za niz: +0 (bilo je grešaka)\n"
                0
            }
            finalPoints = max(0, basePoints + timeBonus + streakBonus - efficiencyPenalty - mistakePenalty)

            // IZMENA: Dodajemo XP u ScoreManager
            scoreManager.addXp(finalPoints)
            lastAwardedXp = finalPoints // Pamtimo za prikaz u dijalogu

            detailedMessage += "\n🏆 Osvojeno XP: $finalPoints"
        } else {
            correctStreak = 0
            lastAwardedXp = 0
            detailedMessage = "Zagonetka rešena uz pomoć.\n\n(Niste osvojili XP poene jer ste koristili rešenje.)"
        }
        gameResultMessage = detailedMessage
        showGameResultDialog = true
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

    val onNextPuzzle: () -> Unit = {
        showGameResultDialog = false
        showNoMoreMovesDialog = false
        if (currentProblemIndex + 1 < problemsInSession.size) {
            currentProblemIndex++
        } else {
            stopTimer()
            // Sesija je gotova, ne moramo čuvati pojedinačni skor sesije više
            showSessionEndDialog = true
        }
    }

    fun showRewardedAdForHint(onAdCompleted: () -> Unit) {
        Log.d("AdSystem", "Prikazujem reklamu za hint...")
        onAdCompleted()
    }

    val onHintClick: () -> Unit = hint@{
        if (isShowingHint || showSolutionPath) return@hint
        stopTimer()
        showRewardedAdForHint {
            coroutineScope.launch(Dispatchers.Default) {
                val rules = when (module) {
                    Module.Module1 -> Module1Rules()
                    Module.Module2 -> Module2Rules()
                    Module.Module3 -> Module3Rules()
                }
                val solver = UniversalPuzzleSolver(rules)
                val solution = solver.solve(currentBoard)
                if (solution.isSolved) {
                    launch(Dispatchers.Main) {
                        hintMoves = solution.path.take(3)
                        hintMoveIndex = 0
                        isShowingHint = true
                    }
                } else {
                    launch(Dispatchers.Main) {
                        snackbarHostState.showSnackbar("Solver nije uspeo da pronađe rešenje.")
                        startTimer() // Vrati tajmer ako solver ne uspe
                    }
                }
            }
        }
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

    val onNextMove: () -> Unit = {
        isPlayingSolution = false
        val solutionMoves = currentProblem?.solution?.moves
        if (solutionMoves != null && solutionMoveIndex < solutionMoves.size) {
            val nextMoveIndex = if (solutionMoveIndex == -1) 0 else solutionMoveIndex
            val (start, end) = FenParser.parseMove(solutionMoves[nextMoveIndex])
            currentBoard.applyMove(start, end)?.let { newBoard ->
                currentBoard = newBoard
                solutionMoveIndex = nextMoveIndex + 1
                selectedSquare = end
            }
        }
    }

    val onSquareClick: (Square) -> Unit = click@{ clickedSquare ->
        if (showSolutionPath || showGameResultDialog || showSessionEndDialog || showNoMoreMovesDialog || isShowingHint) return@click
        val pieceOnClickedSquare = currentBoard.getPiece(clickedSquare)
        if (selectedSquare == null) {
            if (pieceOnClickedSquare != null && pieceOnClickedSquare.color == activePlayerColor) {
                selectedSquare = clickedSquare
            }
            return@click
        }
        val startSquare = selectedSquare!!
        val endSquare = clickedSquare
        if (startSquare == endSquare || (pieceOnClickedSquare != null && pieceOnClickedSquare.color == activePlayerColor)) {
            selectedSquare = clickedSquare
            return@click
        }
        if (currentBoard.isValidMove(startSquare, endSquare)) {
            if (module == Module.Module1 && currentBoard.getPiece(endSquare) == null) {
                coroutineScope.launch { snackbarHostState.showSnackbar("U ovom modulu svaki potez mora biti uzimanje!") }
                return@click
            }
            val newBoard = currentBoard.applyMove(startSquare, endSquare)
            if (newBoard != null) {
                if (module == Module.Module2 || module == Module.Module3) {
                    val attackedByBlackOnNewBoard = newBoard.getAttackedSquares(ChessColor.BLACK)
                    if (attackedByBlackOnNewBoard.contains(endSquare)) {
                        boardForDialog = newBoard
                        showDefendedSquareDialog = true
                        selectedSquare = startSquare
                        defendedSquareMistakes++
                        return@click
                    }
                }
                currentBoard = newBoard
                selectedSquare = endSquare
                playerMoveCount++
                when(module) {
                    Module.Module1 -> {
                        if (!currentBoard.hasBlackPiecesRemaining()) {
                            checkGameStatus(isSuccess = true)
                        } else if (!currentBoard.hasAnyLegalCaptureMove(activePlayerColor)) {
                            stopTimer()
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
            coroutineScope.launch { snackbarHostState.showSnackbar("Ne možete pomeriti figuru tako!") }
        }
    }

    val onSurrender: () -> Unit = {
        stopTimer()
        usedSolution = true
        correctStreak = 0
        gameResultMessage = "Predali ste se. Zagonetka nije rešena."
        showGameResultDialog = true
    }

    val currentlyHighlightedHintMove = if (isShowingHint) hintMoves.getOrNull(hintMoveIndex) else null

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val configuration = LocalConfiguration.current
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                GameInfoPanel(module, difficulty, currentProblem, problemsInSession, currentSessionProblemIndex, Modifier.weight(1f), elapsedTimeInSeconds)
                ChessBoardComposable(currentBoard, selectedSquare, onSquareClick, currentlyHighlightedHintMove, modifier = Modifier.weight(1.2f).fillMaxHeight().aspectRatio(1f))
                GameControlsPanel(showSolutionPath, isPlayingSolution, solutionMoveIndex, currentProblem, onShowSolution, onNextPuzzle, onPreviousMove, { isPlayingSolution = !isPlayingSolution }, onNextMove, onHintClick, onSurrender, modifier = Modifier.weight(1f))
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceAround) {
                GameInfoPanel(module, difficulty, currentProblem, problemsInSession, currentSessionProblemIndex, elapsedTime = elapsedTimeInSeconds)
                ChessBoardComposable(currentBoard, selectedSquare, onSquareClick, currentlyHighlightedHintMove, modifier = Modifier.fillMaxWidth(0.95f).aspectRatio(1f))
                GameControlsPanel(showSolutionPath, isPlayingSolution, solutionMoveIndex, currentProblem, onShowSolution, onNextPuzzle, onPreviousMove, { isPlayingSolution = !isPlayingSolution }, onNextMove, onHintClick, onSurrender)
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        if (showGameResultDialog) {
            AlertDialog(onDismissRequest = {}, title = { Text("Status zagonetke") }, text = { Text(gameResultMessage) }, dismissButton = { TextButton(onClick = onShowSolution) { Text("Rešenje") } }, confirmButton = { TextButton(onClick = onNextPuzzle) { Text(if (currentProblemIndex + 1 < problemsInSession.size) "Sledeća zagonetka" else "Kraj sesije") } })
        }
        if (showNoMoreMovesDialog) {
            NoMoreMovesDialog(onShowSolution = onShowSolution, onNewGame = onNextPuzzle)
        }
        if (showSessionEndDialog) {
            val totalXp = scoreManager.getTotalXp()
            AlertDialog(onDismissRequest = {}, title = { Text("Kraj sesije") }, text = { Column { Text("Završili ste sesiju.") ; Text("Ukupno XP poena: $totalXp") } }, confirmButton = { TextButton(onClick = { showSessionEndDialog = false; onGameFinished() }) { Text("Glavni Meni") } })
        }
        if (showDefendedSquareDialog && boardForDialog != null) {
            DefendedSquareDialog(board = boardForDialog!!, onDismiss = { showDefendedSquareDialog = false })
        }
    }
}

@Composable
fun NoMoreMovesDialog(onShowSolution: () -> Unit, onNewGame: () -> Unit) {
    AlertDialog(onDismissRequest = { }, title = { Text("Nema više poteza") }, text = { Text("Nažalost, ostali ste bez mogućih poteza kojima biste pojeli preostale crne figure.") }, dismissButton = { TextButton(onClick = onShowSolution) { Text("Pregled rešenja") } }, confirmButton = { TextButton(onClick = onNewGame) { Text("Nova zagonetka") } })
}

@Composable
fun GameInfoPanel(module: Module, difficulty: Difficulty, currentProblem: Problem?, problemsInSession: List<Problem>, currentSessionProblemIndex: Int, modifier: Modifier = Modifier, elapsedTime: Int) {
    val minutes = elapsedTime / 60
    val seconds = elapsedTime % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text = "Mod: ${module.title}", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(text = "Težina: ${difficulty.label}", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "🕒 Vreme: $timeString", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = if (problemsInSession.isNotEmpty()) "Zagonetka: ${currentSessionProblemIndex + 1}/${problemsInSession.size}" else "Učitavanje...", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Na potezu: Beli", style = MaterialTheme.typography.bodyLarge)
        currentProblem?.let {
            Text(text = "Cilj: ${it.description}", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun GameControlsPanel(
    showSolutionPath: Boolean,
    isPlayingSolution: Boolean,
    solutionMoveIndex: Int,
    currentProblem: Problem?,
    onShowSolutionClick: () -> Unit,
    onNextPuzzleClick: () -> Unit,
    onPreviousMoveClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextMoveClick: () -> Unit,
    onHintClick: () -> Unit,
    onSurrenderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = onShowSolutionClick) { Text(if (showSolutionPath) "Sakrij" else "Rešenje") }
            Button(onClick = onHintClick) { Text("Hint") }
            Button(onClick = onNextPuzzleClick) { Text("Sledeća") }
        }

        if (showSolutionPath) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Rešenje iz JSON-a:", style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onPreviousMoveClick, enabled = solutionMoveIndex > 0) { Text("<<") }
                Button(onClick = onPlayPauseClick, enabled = currentProblem?.solution?.moves?.isNotEmpty() == true) { Text(if (isPlayingSolution) "||" else "Play") }
                Button(onClick = onNextMoveClick, enabled = currentProblem?.solution?.moves?.isNotEmpty() == true && solutionMoveIndex < (currentProblem.solution.moves.size)) { Text(">>") }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSurrenderClick, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Predajem se") }
    }
}

@Composable
fun ChessBoardComposable(
    board: Board,
    selectedSquare: Square?,
    onSquareClick: (Square) -> Unit,
    highlightedHintMove: Move?,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.background(Color.DarkGray).aspectRatio(1f)) {
        val squareSize = this.maxWidth / 8
        Column {
            for (rank in 7 downTo 0) {
                Row {
                    for (file in 0..7) {
                        val square = Square.fromCoordinates(file, rank)
                        val piece = board.getPiece(square)
                        val backgroundColor = if ((file + rank) % 2 == 0) Color(0xFFEEEED2) else Color(0xFF769656)

                        val isHintStart = highlightedHintMove?.start == square
                        val isHintEnd = highlightedHintMove?.end == square
                        val hintColor = Color.Cyan.copy(alpha = 0.7f)

                        val finalBackgroundColor = when {
                            isHintStart || isHintEnd -> hintColor
                            square == selectedSquare -> Color.Yellow.copy(alpha = 0.6f)
                            else -> backgroundColor
                        }

                        Box(
                            modifier = Modifier
                                .size(squareSize)
                                .background(finalBackgroundColor)
                                .border(
                                    width = if (isHintStart || isHintEnd) 2.dp else 0.dp,
                                    color = if (isHintStart || isHintEnd) Color.Blue else Color.Transparent
                                )
                                .clickable { onSquareClick(square) },
                            contentAlignment = Alignment.Center
                        ) {
                            piece?.let {
                                val drawableResId = getPieceDrawableResId(it)
                                Image(painter = painterResource(id = drawableResId), contentDescription = "${it.color} ${it.type}", modifier = Modifier.fillMaxSize(0.9f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DefendedSquareDialog(board: Board, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Branjeno Polje!", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                Text("Ne možete stati na polje koje napada protivnička figura.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 16.dp))
                ChessBoardComposable(board = board, onSquareClick = {}, selectedSquare = null, highlightedHintMove = null, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Pokušaj ponovo") }
            }
        }
    }
}

@SuppressLint("DiscouragedApi")
@Composable
fun getPieceDrawableResId(piece: Piece): Int {
    val context = LocalContext.current
    val colorPrefix = if (piece.color == ChessColor.WHITE) "w" else "b"
    val typeSuffix = when (piece.type) {
        PieceType.PAWN -> "p"
        PieceType.KNIGHT -> "n"
        PieceType.BISHOP -> "b"
        PieceType.ROOK -> "r"
        PieceType.QUEEN -> "q"
        PieceType.KING -> "k"
    }
    val resourceName = "${colorPrefix}${typeSuffix}"
    return context.resources.getIdentifier(resourceName, "drawable", context.packageName)
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740, name = "Portrait Preview")
@Composable
fun PreviewChessScreenPortrait() {
    BrainTrainerTheme {
        ChessScreen(module = Module.Module2, difficulty = Difficulty.EASY, onGameFinished = {})
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 480, name = "Landscape Preview")
@Composable
fun PreviewChessScreenLandscape() {
    BrainTrainerTheme {
        ChessScreen(module = Module.Module2, difficulty = Difficulty.EASY, onGameFinished = {})
    }
}
