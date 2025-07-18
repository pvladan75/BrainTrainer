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
import com.program.braintrainer.chess.data.ProblemLoader // Pretpostavka da je ovo ispravna putanja
import com.program.braintrainer.chess.model.Board
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module
import com.program.braintrainer.chess.model.Piece
import com.program.braintrainer.chess.model.PieceType
import com.program.braintrainer.chess.model.Problem
import com.program.braintrainer.chess.model.Square
import com.program.braintrainer.chess.parser.FenParser
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
    // Pretpostavka je da ste ProblemLoader uspešno prebacili u klasu
    val problemLoader = remember { ProblemLoader(context) }

    // Učitavanje i mešanje zagonetki
    val problems = remember(module, difficulty) {
        problemLoader.loadProblemsForModuleAndDifficulty(module, difficulty).shuffled()
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
    var gameResultMessage by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // **NOVO**: Stanje za prikaz dijaloga za branjeno polje
    var showDefendedSquareDialog by remember { mutableStateOf(false) }
    // **NOVO**: Stanje za čuvanje table sa pogrešnim potezom
    var boardForDialog by remember { mutableStateOf<Board?>(null) }


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
        } else {
            // Ako nema više problema, završi igru
            if (problems.isNotEmpty()) {
                onGameFinished()
            }
            currentProblem = null
            currentBoard = Board()
            selectedSquare = null
        }
    }

    LaunchedEffect(isPlayingSolution) {
        if (isPlayingSolution) {
            val solutionMoves = currentProblem?.solution?.moves
            if (solutionMoves != null && solutionMoves.isNotEmpty()) {
                val startFromIndex = if (solutionMoveIndex == -1) 0 else solutionMoveIndex

                for (i in startFromIndex until solutionMoves.size) {
                    if (!isPlayingSolution) break
                    val move = solutionMoves[i]
                    val (start, end) = FenParser.parseMove(move)
                    val newBoard = currentBoard.applyMove(start, end)
                    if (newBoard != null) {
                        currentBoard = newBoard
                        solutionMoveIndex = i + 1
                        selectedSquare = end
                    }
                    delay(1000L)
                }
                isPlayingSolution = false
            } else {
                isPlayingSolution = false
            }
        }
    }

    val problemsInSession = remember(problems) {
        if (problems.size >= 10) problems.take(10) else problems
    }
    val currentSessionProblemIndex = if (problemsInSession.isNotEmpty()) currentProblemIndex % problemsInSession.size else 0

    fun checkGameStatus() {
        when (module) {
            Module.Module1 -> {
                if (!currentBoard.hasBlackPiecesRemaining()) {
                    gameResultMessage = "Čestitamo! Sve crne figure su pojedene. Zagonetka rešena!"
                    if (usedSolution) gameResultMessage += "\n(Niste osvojili poene jer ste koristili rešenje.)"
                    showGameResultDialog = true
                } else if (!currentBoard.hasAnyLegalCaptureMove(activePlayerColor)) {
                    gameResultMessage = "Promašaj! Nema više legalnih poteza uzimanja, a ostale su crne figure."
                    showGameResultDialog = true
                }
            }
            Module.Module2 -> {
                if (!currentBoard.hasBlackPiecesRemaining()) {
                    gameResultMessage = "Čestitamo! Sve crne figure su pojedene. Zagonetka rešena!"
                    if (usedSolution) gameResultMessage += "\n(Niste osvojili poene jer ste koristili rešenje.)"
                    showGameResultDialog = true
                } else if (!currentBoard.hasAnyLegalMove(activePlayerColor)) {
                    gameResultMessage = "Promašaj! Nema više legalnih poteza za belog, a ostale su crne figure."
                    showGameResultDialog = true
                }
            }
            Module.Module3 -> {
                val blackKingExists = currentBoard.pieces.any { it.value == Piece(PieceType.KING, ChessColor.BLACK) }
                if (!blackKingExists) {
                    gameResultMessage = "Čestitamo! Crni kralj je pojeden. Zagonetka rešena!"
                    if (usedSolution) gameResultMessage += "\n(Niste osvojili poene jer ste koristili rešenje.)"
                    showGameResultDialog = true
                } else if (!currentBoard.hasAnyLegalMove(activePlayerColor)) {
                    gameResultMessage = "Promašaj! Nema više legalnih poteza za belog, a crni kralj nije pojeden."
                    showGameResultDialog = true
                }
            }
        }
    }

    val onSquareClick: (Square) -> Unit = click@{ clickedSquare ->
        if (showSolutionPath || showGameResultDialog) {
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
        val endSquare = clickedSquare

        if (startSquare == endSquare || (pieceOnClickedSquare != null && pieceOnClickedSquare.color == activePlayerColor)) {
            selectedSquare = clickedSquare
            return@click
        }

        val isCapture = pieceOnClickedSquare != null && pieceOnClickedSquare.color != activePlayerColor

        if (module == Module.Module1 && !isCapture) {
            coroutineScope.launch { snackbarHostState.showSnackbar("U ovom modulu svaki potez mora biti uzimanje!") }
            // Možemo smatrati da je korišćenje pomoći ako ne prate pravila
        } else if (currentBoard.isValidMove(startSquare, endSquare)) {
            val newBoard = currentBoard.applyMove(startSquare, endSquare)
            if (newBoard != null) {
                if (module == Module.Module2 || module == Module.Module3) {
                    val attackedByBlackOnNewBoard = newBoard.getAttackedSquares(ChessColor.BLACK)
                    if (attackedByBlackOnNewBoard.contains(endSquare)) {
                        // **IZMENJENO**: Prikazujemo novi dijalog umesto da završimo igru
                        boardForDialog = newBoard // Sačuvaj pogrešnu poziciju za prikaz
                        showDefendedSquareDialog = true // Pokaži dijalog
                        // Ne ažuriramo `currentBoard` jer potez nije validan
                        selectedSquare = startSquare // Vrati selekciju na početno polje
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

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val configuration = LocalConfiguration.current

        val onNextPuzzle: () -> Unit = {
            if (currentProblemIndex + 1 < problemsInSession.size) {
                currentProblemIndex++
            } else {
                onGameFinished()
            }
        }

        val onShowSolution: () -> Unit = {
            showSolutionPath = !showSolutionPath
            isPlayingSolution = false
            currentProblem?.let {
                val (initialBoard, _) = FenParser.parseFenToBoard(it.fen)
                currentBoard = initialBoard
                selectedSquare = initialBoard.pieces.entries.firstOrNull { it.value.color == ChessColor.WHITE }?.key
                solutionMoveIndex = -1
                usedSolution = true
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
                } else if (solutionMoveIndex == 0 || solutionMoveIndex == -1) {
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
                val newBoard = currentBoard.applyMove(start, end)
                if (newBoard != null) {
                    currentBoard = newBoard
                    solutionMoveIndex = nextMoveIndex + 1
                    selectedSquare = end
                }
            }
        }

        val onSurrender: () -> Unit = {
            gameResultMessage = "Predali ste se. Zagonetka nije rešena."
            showGameResultDialog = true
            usedSolution = true
        }

        val onPlayPause: () -> Unit = {
            isPlayingSolution = !isPlayingSolution
        }

        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GameInfoPanel(
                    module, difficulty, currentProblem, problemsInSession, currentSessionProblemIndex, Modifier.weight(1f)
                )
                ChessBoardComposable(currentBoard, selectedSquare, onSquareClick)
                GameControlsPanel(
                    showSolutionPath = showSolutionPath,
                    isPlayingSolution = isPlayingSolution,
                    solutionMoveIndex = solutionMoveIndex,
                    currentProblem = currentProblem,
                    problemsInSession = problemsInSession,
                    currentSessionProblemIndex = currentSessionProblemIndex,
                    onShowSolutionClick = onShowSolution,
                    onNextPuzzleClick = onNextPuzzle,
                    onPreviousMoveClick = onPreviousMove,
                    onPlayPauseClick = onPlayPause,
                    onNextMoveClick = onNextMove,
                    onSurrenderClick = onSurrender,
                    modifier = Modifier.weight(1f)
                )
            }
        } else { // Portrait mode
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround
            ) {
                GameInfoPanel(
                    module, difficulty, currentProblem, problemsInSession, currentSessionProblemIndex
                )
                ChessBoardComposable(currentBoard, selectedSquare, onSquareClick)
                GameControlsPanel(
                    showSolutionPath = showSolutionPath,
                    isPlayingSolution = isPlayingSolution,
                    solutionMoveIndex = solutionMoveIndex,
                    currentProblem = currentProblem,
                    problemsInSession = problemsInSession,
                    currentSessionProblemIndex = currentSessionProblemIndex,
                    onShowSolutionClick = onShowSolution,
                    onNextPuzzleClick = onNextPuzzle,
                    onPreviousMoveClick = onPreviousMove,
                    onPlayPauseClick = onPlayPause,
                    onNextMoveClick = onNextMove,
                    onSurrenderClick = onSurrender
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Dijalog za kraj igre
        if (showGameResultDialog) {
            AlertDialog(
                onDismissRequest = { /* Ne dozvoli zatvaranje */ },
                title = { Text("Status Zagonetke") },
                text = { Text(gameResultMessage) },
                confirmButton = {
                    TextButton(onClick = {
                        showGameResultDialog = false
                        onNextPuzzle()
                    }) {
                        Text("Sledeća zagonetka")
                    }
                }
            )
        }

        // **NOVO**: Prikaz dijaloga za branjeno polje
        if (showDefendedSquareDialog && boardForDialog != null) {
            DefendedSquareDialog(
                board = boardForDialog!!,
                onDismiss = { showDefendedSquareDialog = false }
            )
        }
    }
}

// --- Bez izmena u ostalim Composable funkcijama ispod, osim u DefendedSquareDialog ---

@Composable
fun GameInfoPanel(
    module: Module,
    difficulty: Difficulty,
    currentProblem: Problem?,
    problemsInSession: List<Problem>,
    currentSessionProblemIndex: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Mod: ${module.title}", style = MaterialTheme.typography.titleLarge)
        Text(text = "Težina: ${difficulty.label}", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        // Implementacija tajmera bi išla ovde
        Text(text = "Vreme: 00:00", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (problemsInSession.isNotEmpty()) "Zagonetka: ${currentSessionProblemIndex + 1}/${problemsInSession.size}" else "Nema zagonetki",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Na potezu: Beli", style = MaterialTheme.typography.bodyLarge)
        currentProblem?.let {
            Text(
                text = "Cilj: ${it.description}",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun GameControlsPanel(
    showSolutionPath: Boolean,
    isPlayingSolution: Boolean,
    solutionMoveIndex: Int,
    currentProblem: Problem?,
    problemsInSession: List<Problem>,
    currentSessionProblemIndex: Int,
    onShowSolutionClick: () -> Unit,
    onNextPuzzleClick: () -> Unit,
    onPreviousMoveClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextMoveClick: () -> Unit,
    onSurrenderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onShowSolutionClick) {
                Text(if (showSolutionPath) "Sakrij" else "Rešenje")
            }
            Button(
                onClick = onNextPuzzleClick,
                enabled = problemsInSession.isNotEmpty() && currentSessionProblemIndex + 1 < problemsInSession.size
            ) {
                Text("Sledeća")
            }
        }

        if (showSolutionPath) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onPreviousMoveClick, enabled = solutionMoveIndex > -1) {
                    Text("<<")
                }
                Button(
                    onClick = onPlayPauseClick,
                    enabled = currentProblem?.solution?.moves?.isNotEmpty() == true
                ) {
                    Text(if (isPlayingSolution) "||" else ">")
                }
                Button(
                    onClick = onNextMoveClick,
                    enabled = currentProblem?.solution?.moves?.isNotEmpty() == true && solutionMoveIndex < (currentProblem.solution.moves.size)
                ) {
                    Text(">>")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSurrenderClick, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
            Text("Predajem se")
        }
    }
}

@Composable
fun ChessBoardComposable(
    board: Board,
    selectedSquare: Square?,
    onSquareClick: (Square) -> Unit
) {
    val squareSize = LocalConfiguration.current.screenWidthDp.dp / 10 // Prilagodljiva veličina
    val boardSize = squareSize * 8

    Column(
        modifier = Modifier
            .size(boardSize)
            .background(Color.DarkGray)
    ) {
        for (rank in 7 downTo 0) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (file in 0..7) {
                    val square = Square.fromCoordinates(file, rank)
                    val piece = board.getPiece(square)

                    val backgroundColor = if ((file + rank) % 2 == 0) {
                        Color(0xFFEEEED2) // Svetlo polje
                    } else {
                        Color(0xFF769656) // Tamno polje
                    }

                    Box(
                        modifier = Modifier
                            .size(squareSize)
                            .background(
                                when {
                                    square == selectedSquare -> Color.Yellow.copy(alpha = 0.6f)
                                    else -> backgroundColor
                                }
                            )
                            .clickable { onSquareClick(square) },
                        contentAlignment = Alignment.Center
                    ) {
                        piece?.let {
                            val drawableResId = getPieceDrawableResId(it)
                            Image(
                                painter = painterResource(id = drawableResId),
                                contentDescription = "${it.color} ${it.type}",
                                modifier = Modifier.fillMaxSize(0.9f) // Figura je malo manja od polja
                            )
                        }
                    }
                }
            }
        }
    }
}

// **ISPRAVLJENA FUNKCIJA**
@Composable
fun DefendedSquareDialog(
    board: Board, // Prosleđujemo tablu sa pogrešnim potezom
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Branjeno Polje!",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Ne možete stati na polje koje napada protivnička figura. Pogledajte poziciju da vidite grešku.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // **ISPRAVKA**: Koristimo ChessBoardComposable umesto nepostojećeg BoardView
                ChessBoardComposable(
                    board = board,
                    onSquareClick = {}, // Tabla u dijalogu je samo za prikaz, bez interakcije
                    selectedSquare = null
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pokušaj ponovo")
                }
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
    // Koristimo context.resources za pristup resursima
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