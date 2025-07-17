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
import com.program.braintrainer.chess.model.Color as ChessColor // Alias da se ne bi mešalo sa androidx.compose.ui.graphics.Color
import com.program.braintrainer.chess.model.Piece
import com.program.braintrainer.chess.model.PieceType
import com.program.braintrainer.chess.model.Square
import com.program.braintrainer.chess.parser.FenParser
import com.program.braintrainer.ui.theme.BrainTrainerTheme
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource // Za učitavanje drawable resursa
import com.program.braintrainer.R
import com.program.braintrainer.chess.model.Difficulty // Uvozimo Difficulty
import com.program.braintrainer.chess.model.Module // Uvozimo Module

@Composable
fun ChessScreen(
    module: Module, // PRIMA ODABRANI MODUL
    difficulty: Difficulty, // PRIMA ODABRANU TEŽINU
    onGameFinished: () -> Unit // PRIMA LAMBDA FUNKCIJU ZA NAVIGACIJU NAZAD
) {
    // Ovo će biti privremena pozicija za prikaz
    val initialFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val (board, activeColor) = FenParser.parseFenToBoard(initialFen)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween // Raspoređuje sadržaj vertikalno
    ) {
        // Gornji deo: Vreme, Broj zagonetke, itd.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Mod: ${module.title}", // Prikazujemo odabrani mod
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Težina: ${difficulty.label}", // Prikazujemo odabranu težinu
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
                text = "Zagonetka: 1/10",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Na potezu: ${if (activeColor == ChessColor.WHITE) "Beli" else "Crni"}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Srednji deo: Šahovska tabla
        ChessBoardComposable(board = board)

        // Donji deo: Dugmići
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { /* TODO: Logika za sledeću zagonetku */ }) {
                Text("Sledeća zagonetka")
            }
            Button(onClick = { /* TODO: Logika za prikaz rešenja */ }) {
                Text("Prikaži rešenje")
            }
            Button(onClick = { onGameFinished() }) { // POZIVAMO LAMBDA FUNKCIJU ZA NAVIGACIJU NAZAD
                Text("Završi igru")
            }
        }
    }
}

@Composable
fun ChessBoardComposable(board: Board) {
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
                        Color(0xFFEEEED2) // Svetlo polje
                    } else {
                        Color(0xFF769656) // Tamno polje
                    }

                    Box(
                        modifier = Modifier
                            .size(squareSize)
                            .background(backgroundColor),
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
        // U Preview-u moramo da prosledimo neke podrazumevane vrednosti
        ChessScreen(module = Module.Module1, difficulty = Difficulty.EASY, onGameFinished = {})
    }
}