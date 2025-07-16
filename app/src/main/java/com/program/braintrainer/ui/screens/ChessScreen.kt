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

@Composable
fun ChessScreen() {
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
            Button(onClick = { /* TODO: Logika za predaju */ }) {
                Text("Predajem se")
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
                        // OVDE MENJAMO: Umesto Text, sada koristimo Image
                        piece?.let {
                            val drawableResId = getPieceDrawableResId(it)
                            Image(
                                painter = painterResource(id = drawableResId),
                                contentDescription = "${it.color} ${it.type}",
                                modifier = Modifier.fillMaxSize() // Slika će popuniti celo polje
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pomoćna funkcija koja na osnovu Piece objekta vraća odgovarajući drawable resurs ID.
 * Ova funkcija pretpostavlja da su nazivi tvojih drawable fajlova standardizovani:
 * npr. 'wp.png' za belog pešaka, 'bn.png' za crnog skakača.
 */
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
    // Koristimo Androidov resursni sistem za dohvaćanje ID-a drawable-a
    // Naziv resursa mora odgovarati tvom imenu datoteke (bez .png ekstenzije)
    val resourceName = "$colorPrefix$typeSuffix"
    return LocalContext.current.resources.getIdentifier(resourceName, "drawable", LocalContext.current.packageName)
}


@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PreviewChessScreen() {
    BrainTrainerTheme {
        ChessScreen()
    }
}