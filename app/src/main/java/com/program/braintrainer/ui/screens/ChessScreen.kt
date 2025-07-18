package com.program.braintrainer.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.program.braintrainer.R
import com.program.braintrainer.chess.model.data.ProblemLoader
import com.program.braintrainer.chess.model.Board
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module
import com.program.braintrainer.chess.model.Piece
import com.program.braintrainer.chess.model.PieceType
import com.program.braintrainer.chess.model.Problem
import com.program.braintrainer.chess.model.Square
import com.program.braintrainer.chess.parser.FenParser
import com.program.braintrainer.score.ScoreManager
import com.program.braintrainer.ui.theme.BrainTrainerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.program.braintrainer.chess.model.Color as ChessColor


@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ChessScreen(
    module: Module,
    difficulty: Difficulty,
    onGameFinished: () -> Unit
) {
    val context = LocalContext.current
    val problemLoader = remember { ProblemLoader(context) }

    val problems: List<Problem> = remember(module, difficulty) {
        problemLoader.loadProblemsForModuleAndDifficulty(module, difficulty)
    }

    // --- Stanja za igru ---
    var currentProblemIndex by remember { mutableStateOf(0) }
    var currentProblem by remember { mutableStateOf<Problem?>(null) }
    var currentBoard by remember { mutableStateOf(Board()) }
    val activePlayerColor = ChessColor.WHITE
    var selectedSquare by remember { mutableStateOf<Square?>(null) }

    // --- Stanja za rešenje ---
    var showSolutionPath by remember { mutableStateOf(false) }
    var solutionMoveIndex by remember { mutableStateOf(-1) }
    var isPlayingSolution by remember { mutableStateOf(false) }
    var usedSolution by remember { mutableStateOf(false) }

    // --- Stanja za dijaloge i poruke ---
    var showGameResultDialog by remember { mutableStateOf(false) }
    var gameResultMessage by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showDefendedSquareDialog by remember { mutableStateOf(false) }
    var boardForDialog by remember { mutableStateOf<Board?>(null) }
    var showSessionEndDialog by remember { mutableStateOf(false) }

    // --- Stanja za bodovanje i tajmer ---
    val scoreManager = remember { ScoreManager(context) }
    var currentSessionScore by remember { mutableStateOf(0) }
    var elapsedTimeInSeconds by remember { mutableStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }

    fun stopTimer() {
        isTimerRunning = false
    }

    LaunchedEffect(currentProblemIndex) {
        elapsedTimeInSeconds = 0
        isTimerRunning = true
    }

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (true) {
                delay(1000L)
                elapsedTimeInSeconds++
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
            showSolutionPath = false
            solutionMoveIndex = -1
            isPlayingSolution = false
            usedSolution = false
            showGameResultDialog = false
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

    fun checkGameStatus() {
        fun addPoints() {
            if (!usedSolution) {
                val basePoints = when (difficulty) {
                    Difficulty.EASY -> 10
                    Difficulty.MEDIUM -> 20
                    Difficulty.HARD -> 30
                }
                val maxTimeForBonus = when (difficulty) {
                    Difficulty.EASY -> 30
                    Difficulty.MEDIUM -> 60
                    Difficulty.HARD -> 90
                }
                val timeBonus = maxOf(0, maxTimeForBonus - elapsedTimeInSeconds)
                val totalPoints = basePoints + timeBonus
                currentSessionScore += totalPoints
                gameResultMessage += "\n\n🏆 Osvojili ste $totalPoints poena ($basePoints osnovnih + $timeBonus bonus za vreme)."
            } else {
                gameResultMessage += "\n\n(Niste osvojili poene jer ste koristili rešenje.)"
            }
        }

        when (module) {
            Module.Module1 -> {
                if (!currentBoard.hasBlackPiecesRemaining()) {
                    stopTimer()
                    gameResultMessage = "Sve crne figure su pojedene. Zagonetka rešena!"
                    addPoints()
                    showGameResultDialog = true
                } else if (!currentBoard.hasAnyLegalCaptureMove(activePlayerColor)) {
                    stopTimer()
                    gameResultMessage = "Promašaj! Nema više legalnih poteza uzimanja."
                    showGameResultDialog = true
                }
            }
            Module.Module2 -> {
                if (!currentBoard.hasBlackPiecesRemaining()) {
                    stopTimer()
                    gameResultMessage = "Sve crne figure su pojedene. Zagonetka rešena!"
                    addPoints()
                    showGameResultDialog = true
                } else if (!currentBoard.hasAnyLegalMove(activePlayerColor)) {
                    stopTimer()
                    gameResultMessage = "Promašaj! Nema više legalnih poteza."
                    showGameResultDialog = true
                }
            }
            Module.Module3 -> {
                val blackKingExists = currentBoard.pieces.any { it.value == Piece(PieceType.KING, ChessColor.BLACK) }
                if (!blackKingExists) {
                    stopTimer()
                    gameResultMessage = "Crni kralj je pojeden. Zagonetka rešena!"
                    addPoints()
                    showGameResultDialog = true
                } else if (!currentBoard.hasAnyLegalMove(activePlayerColor)) {
                    stopTimer()
                    gameResultMessage = "Promašaj! Nema više legalnih poteza."
                    showGameResultDialog = true
                }
            }
        }
    }

    val onSquareClick: (Square) -> Unit = click@{ clickedSquare ->
        if (showSolutionPath || showGameResultDialog || showSessionEndDialog) return@click
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
                        return@click
                    }
                }
                currentBoard = newBoard
                selectedSquare = endSquare
                checkGameStatus()
            }
        } else {
            coroutineScope.launch { snackbarHostState.showSnackbar("Ne možete pomeriti figuru tako!") }
        }
    }

    val onNextPuzzle: () -> Unit = {
        showGameResultDialog = false
        if (currentProblemIndex + 1 < problemsInSession.size) {
            currentProblemIndex++
        } else {
            stopTimer()
            scoreManager.saveScore(module, difficulty, currentSessionScore)
            showSessionEndDialog = true
        }
    }

    val onShowSolution: () -> Unit = {
        if (!showSolutionPath) {
            stopTimer()
            usedSolution = true
        }
        showSolutionPath = !showSolutionPath
        isPlayingSolution = false
        currentProblem?.let {
            val (initialBoard, _) = FenParser.parseFenToBoard(it.fen)
            currentBoard = initialBoard
            selectedSquare = initialBoard.pieces.entries.firstOrNull { p -> p.value.color == ChessColor.WHITE }?.key
            solutionMoveIndex = -1
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

    val onSurrender: () -> Unit = {
        stopTimer()
        usedSolution = true
        gameResultMessage = "Predali ste se. Zagonetka nije rešena."
        showGameResultDialog = true
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val configuration = LocalConfiguration.current

        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                GameInfoPanel(module, difficulty, currentProblem, problemsInSession, currentSessionProblemIndex, Modifier.weight(1f), elapsedTimeInSeconds)
                ChessBoardComposable(currentBoard, selectedSquare, onSquareClick, modifier = Modifier.weight(1.2f).fillMaxHeight().aspectRatio(1f))
                GameControlsPanel(showSolutionPath, isPlayingSolution, solutionMoveIndex, currentProblem, onShowSolution, onNextPuzzle, onPreviousMove, { isPlayingSolution = !isPlayingSolution }, onNextMove, onSurrender, modifier = Modifier.weight(1f))
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceAround) {
                GameInfoPanel(module, difficulty, currentProblem, problemsInSession, currentSessionProblemIndex, elapsedTime = elapsedTimeInSeconds)
                ChessBoardComposable(currentBoard, selectedSquare, onSquareClick, modifier = Modifier.fillMaxWidth(0.95f).aspectRatio(1f))
                GameControlsPanel(showSolutionPath, isPlayingSolution, solutionMoveIndex, currentProblem, onShowSolution, onNextPuzzle, onPreviousMove, { isPlayingSolution = !isPlayingSolution }, onNextMove, onSurrender)
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))

        if (showGameResultDialog) {
            AlertDialog(onDismissRequest = {}, title = { Text("Status zagonetke") }, text = { Text(gameResultMessage) }, confirmButton = { TextButton(onClick = onNextPuzzle) { Text(if (currentProblemIndex + 1 < problemsInSession.size) "Sledeća zagonetka" else "Pogledaj rezultat") } })
        }

        if (showSessionEndDialog) {
            val highScore = scoreManager.getHighScore(module, difficulty)
            AlertDialog(onDismissRequest = {}, title = { Text("Kraj sesije") }, text = { Column { Text("Završili ste sesiju sa ukupno $currentSessionScore poena.") ; Text("Najbolji rezultat za ovaj mod je: $highScore poena.") ; if (currentSessionScore > highScore && currentSessionScore > 0) { Text("\n🎉 Čestitamo, postavili ste novi rekord!", color = MaterialTheme.colorScheme.primary) } } }, confirmButton = { TextButton(onClick = { showSessionEndDialog = false; onGameFinished() }) { Text("Glavni Meni") } })
        }

        if (showDefendedSquareDialog && boardForDialog != null) {
            DefendedSquareDialog(board = boardForDialog!!, onDismiss = { showDefendedSquareDialog = false })
        }
    }
}

// Ostatak fajla ostaje uglavnom isti, sa ključnim izmenama u ChessBoardComposable i DefendedSquareDialog

@Composable
fun GameInfoPanel(
    module: Module,
    difficulty: Difficulty,
    currentProblem: Problem?,
    problemsInSession: List<Problem>,
    currentSessionProblemIndex: Int,
    modifier: Modifier = Modifier,
    elapsedTime: Int
) {
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
    onSurrenderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = onShowSolutionClick) { Text(if (showSolutionPath) "Sakrij" else "Rešenje") }
            Button(onClick = onNextPuzzleClick) { Text("Sledeća") }
        }
        if (showSolutionPath) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onPreviousMoveClick, enabled = solutionMoveIndex > 0) { Text("<<") }
                Button(onClick = onPlayPauseClick, enabled = currentProblem?.solution?.moves?.isNotEmpty() == true) { Text(if (isPlayingSolution) "||" else ">") }
                Button(onClick = onNextMoveClick, enabled = currentProblem?.solution?.moves?.isNotEmpty() == true && solutionMoveIndex < (currentProblem.solution.moves.size)) { Text(">>") }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSurrenderClick, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Predajem se") }
    }
}


// *** KLJUČNA IZMENA #1 ***
@Composable
fun ChessBoardComposable(
    board: Board,
    selectedSquare: Square?,
    onSquareClick: (Square) -> Unit,
    modifier: Modifier = Modifier // Dodat modifikator
) {
    // Koristimo BoxWithConstraints da dobijemo raspoloživu širinu i na osnovu nje izračunamo veličinu polja
    BoxWithConstraints(
        modifier = modifier
            .background(Color.DarkGray)
            .aspectRatio(1f) // Održavamo tablu kvadratnom
    ) {
        val squareSize = this.maxWidth / 8 // Veličina polja se sada bazira na širini kontejnera

        Column {
            for (rank in 7 downTo 0) {
                Row {
                    for (file in 0..7) {
                        val square = Square.fromCoordinates(file, rank)
                        val piece = board.getPiece(square)
                        val backgroundColor = if ((file + rank) % 2 == 0) Color(0xFFEEEED2) else Color(0xFF769656)
                        Box(
                            modifier = Modifier
                                .size(squareSize) // Koristimo izračunatu veličinu
                                .background(if (square == selectedSquare) Color.Yellow.copy(alpha = 0.6f) else backgroundColor)
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

// *** KLJUČNA IZMENA #2 ***
@Composable
fun DefendedSquareDialog(board: Board, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Branjeno Polje!", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                Text("Ne možete stati na polje koje napada protivnička figura.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 16.dp))

                // Prosleđujemo modifikator da ograničimo veličinu table unutar dijaloga
                ChessBoardComposable(
                    board = board,
                    onSquareClick = {},
                    selectedSquare = null,
                    modifier = Modifier.fillMaxWidth() // Tabla će se raširiti do ivica kartice
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Pokušaj ponovo") }
            }
        }
    }
}

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