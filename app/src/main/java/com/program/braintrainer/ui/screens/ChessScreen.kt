package com.program.braintrainer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.program.braintrainer.chess.model.Board
import com.program.braintrainer.chess.model.Color as ChessColor
import com.program.braintrainer.chess.model.Piece
import com.program.braintrainer.chess.model.PieceType
import com.program.braintrainer.chess.model.Square
import com.program.braintrainer.chess.parser.FenParser
import com.program.braintrainer.ui.theme.BrainTrainerTheme
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.program.braintrainer.R
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module
import com.program.braintrainer.chess.data.ProblemLoader
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun ChessScreen(
    module: Module,
    difficulty: Difficulty,
    onGameFinished: () -> Unit
) {
    val context = LocalContext.current
    val problemLoader = remember { ProblemLoader(context) }

    val problems = remember(module, difficulty) {
        problemLoader.loadProblemsForModuleAndDifficulty(module, difficulty)
    }

    var currentProblemIndex by remember { mutableStateOf(0) }
    var currentProblem by remember { mutableStateOf<com.program.braintrainer.chess.model.Problem?>(null) }
    var currentBoard by remember { mutableStateOf(Board()) }
    val activePlayerColor = ChessColor.WHITE // Beli je uvek na potezu

    var selectedSquare by remember { mutableStateOf<Square?>(null) }
    var showSolutionPath by remember { mutableStateOf(false) }

    LaunchedEffect(currentProblemIndex, problems) {
        if (problems.isNotEmpty()) {
            val problem = problems[currentProblemIndex]
            currentProblem = problem
            val (board, _) = FenParser.parseFenToBoard(problem.fen)
            currentBoard = board
            selectedSquare = null
            showSolutionPath = false
        } else {
            currentProblem = null
            currentBoard = Board()
            selectedSquare = null
            showSolutionPath = false
        }
    }

    val problemsInSession = remember(problems) {
        if (problems.size >= 10) problems.take(10) else problems
    }
    val currentSessionProblemIndex = currentProblemIndex % problemsInSession.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Mod: ${module.title}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Težina: ${difficulty.label}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Vreme: 00:00",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (problemsInSession.isNotEmpty()) "Zagonetka: ${currentSessionProblemIndex + 1}/${problemsInSession.size}" else "Nema zagonetki",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Na potezu: Beli",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            currentProblem?.let {
                Text(
                    text = "Cilj: ${it.description}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        ChessBoardComposable(
            board = currentBoard,
            selectedSquare = selectedSquare,
            solutionPath = if (showSolutionPath) currentProblem?.solution?.moves else null,
            onSquareClick = { clickedSquare ->
                if (selectedSquare == null) {
                    val piece = currentBoard.getPiece(clickedSquare)
                    if (piece != null && piece.color == ChessColor.WHITE) {
                        selectedSquare = clickedSquare
                    }
                } else {
                    val pieceOnClickedSquare = currentBoard.getPiece(clickedSquare)

                    if (pieceOnClickedSquare != null && pieceOnClickedSquare.color == ChessColor.WHITE) {
                        selectedSquare = clickedSquare
                    } else {
                        val startSquare = selectedSquare!!
                        val endSquare = clickedSquare

                        // KORISTI NOVU isValidMove METODU
                        if (currentBoard.isValidMove(startSquare, endSquare)) {
                            val newBoard = currentBoard.applyMove(startSquare, endSquare)
                            if (newBoard != null) {
                                currentBoard = newBoard
                                println("Potez ${startSquare.toString()}${endSquare.toString()} odigran.")
                                selectedSquare = endSquare // Figura ostaje selektovana na novoj poziciji
                            } else {
                                println("Nevažeći potez (applyMove vratio null) ili nepoznat razlog).")
                                selectedSquare = null // Poništi selekciju ako potez nije uspeo
                            }
                        } else {
                            println("Nevažeći potez (prema pravilima kretanja figure).")
                            selectedSquare = null // Poništi selekciju ako je potez ilegalan
                        }
                    }
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (problems.isNotEmpty() && currentProblemIndex < problems.size - 1 && currentSessionProblemIndex < problemsInSession.size - 1) {
                        currentProblemIndex++
                    } else {
                        onGameFinished()
                    }
                },
                enabled = problemsInSession.isNotEmpty() && currentSessionProblemIndex < problemsInSession.size - 1
            ) {
                Text("Sledeća zagonetka")
            }
            Button(onClick = {
                showSolutionPath = !showSolutionPath
            }) {
                Text(if (showSolutionPath) "Sakrij rešenje" else "Prikaži rešenje")
            }
            Button(onClick = { onGameFinished() }) {
                Text("Predajem se / Završi sesiju")
            }
        }
    }
}

@Composable
fun ChessBoardComposable(
    board: Board,
    selectedSquare: Square?,
    solutionPath: List<String>?,
    onSquareClick: (Square) -> Unit
) {
    val squareSize = 40.dp
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
                        Color(0xFFEEEED2)
                    } else {
                        Color(0xFF769656)
                    }

                    val squareModifier = Modifier
                        .size(squareSize)
                        .background(
                            when {
                                square == selectedSquare -> Color.Yellow.copy(alpha = 0.5f)
                                solutionPath != null && isSquareInSolutionPath(square, solutionPath) -> Color.Red.copy(alpha = 0.5f)
                                else -> backgroundColor
                            }
                        )
                        .clickable { onSquareClick(square) }

                    Box(
                        modifier = squareModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        piece?.let {
                            val drawableResId = getPieceDrawableResId(it)
                            Image(
                                painter = painterResource(id = drawableResId),
                                contentDescription = "${it.color} ${it.type}",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

fun isSquareInSolutionPath(square: Square, solutionMoves: List<String>): Boolean {
    for (move in solutionMoves) {
        val (start, end) = FenParser.parseMove(move)
        if (square == start || square == end) {
            return true
        }
    }
    return false
}


@Composable
fun getPieceDrawableResId(piece: Piece): Int {
    val colorPrefix = if (piece.color == ChessColor.WHITE) "w" else "b"
    val typeSuffix = when (piece.type) {
        PieceType.PAWN -> "p"
        PieceType.KNIGHT -> "n"
        PieceType.BISHOP -> "b"
        PieceType.ROOK -> "r"
        PieceType.QUEEN -> "q"
        PieceType.KING -> "k"
    }
    val resourceName = "$colorPrefix$typeSuffix"
    return LocalContext.current.resources.getIdentifier(resourceName, "drawable", LocalContext.current.packageName)
}


@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PreviewChessScreen() {
    BrainTrainerTheme {
        ChessScreen(module = Module.Module1, difficulty = Difficulty.EASY, onGameFinished = {})
    }
}