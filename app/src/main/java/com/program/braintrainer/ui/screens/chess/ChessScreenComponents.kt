package com.program.braintrainer.ui.screens.chess

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.program.braintrainer.R
import com.program.braintrainer.chess.model.*
import com.program.braintrainer.ui.difficultyLabel
import com.program.braintrainer.ui.moduleTitle
import com.program.braintrainer.chess.model.Color as ChessColor

/**
 * Vizuelni delovi ekrana za igru: tabla, info panel, kontrole i dijalozi.
 * Odvojeni od ChessScreen.kt, koji drži stanje i tok partije.
 */

@Composable
fun NoMoreMovesDialog(onShowSolution: () -> Unit, onNewGame: () -> Unit) {
    AlertDialog(onDismissRequest = { }, title = { Text(stringResource(R.string.dialog_title_no_more_moves)) }, text = { Text(stringResource(R.string.dialog_message_no_more_moves_m1)) }, dismissButton = { TextButton(onClick = onShowSolution) { Text(stringResource(R.string.button_review_solution)) } }, confirmButton = { TextButton(onClick = onNewGame) { Text(stringResource(R.string.button_new_puzzle)) } })
}

@SuppressLint("DefaultLocale")
@Composable
fun GameInfoPanel(
    module: Module,
    difficulty: Difficulty,
    sessionSize: Int,
    currentSessionProblemIndex: Int,
    elapsedTime: Int,
    showTime: Boolean,
    optimalMoves: Int,
    playerMoveCount: Int,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val minutes = elapsedTime / 60
    val seconds = elapsedTime % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    val goalTextResId = when (module) {
        Module.Module1 -> R.string.goal_module_1
        Module.Module2 -> R.string.goal_module_2
        Module.Module3 -> R.string.goal_module_3
    }

    val infoTextStyle = MaterialTheme.typography.bodyLarge
    val goalTextStyle = MaterialTheme.typography.titleMedium
    val textColor = MaterialTheme.colorScheme.onSurface

    Column(
        // U pejzažu panel stoji tik uz tablu, pa mu treba razmak s desne strane:
        // bez njega se "Time: 00:09" dodiruje sa ivicom table.
        modifier = modifier.padding(end = if (isLandscape) 12.dp else 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = moduleTitle(module),
            style = infoTextStyle,
            textAlign = TextAlign.Center,
            color = textColor
        )
        Text(
            text = difficultyLabel(difficulty),
            style = infoTextStyle,
            color = textColor
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(if (isLandscape) 1f else 0.8f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (sessionSize > 0) stringResource(
                    R.string.info_panel_puzzle_progress,
                    currentSessionProblemIndex + 1,
                    sessionSize
                ) else stringResource(R.string.info_panel_loading),
                style = infoTextStyle,
                color = textColor
            )
            if (showTime) {
                Text(
                    text = stringResource(R.string.info_panel_time, timeString),
                    style = infoTextStyle,
                    color = textColor
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (optimalMoves > 0) {
            Text(
                text = stringResource(R.string.info_panel_moves_progress, playerMoveCount, optimalMoves),
                style = infoTextStyle,
                color = textColor
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = goalTextResId),
            style = goalTextStyle,
            textAlign = TextAlign.Center,
            color = textColor
        )
    }
}


@Composable
fun GameControlsPanel(
    showSolutionPath: Boolean,
    isPlayingSolution: Boolean,
    solutionMoveIndex: Int,
    solutionMoveCount: Int,
    onShowSolutionClick: () -> Unit,
    onNextPuzzleClick: () -> Unit,
    onPreviousMoveClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextMoveClick: () -> Unit,
    onHintClick: () -> Unit,
    onSurrenderClick: () -> Unit,
    onRestartClick: () -> Unit,
    isRestartEnabled: Boolean,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onShowSolutionClick) { Text(if (showSolutionPath) stringResource(R.string.button_hide) else stringResource(R.string.button_solution)) }
                Button(onClick = onHintClick) { Text(stringResource(R.string.button_hint)) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onRestartClick, enabled = isRestartEnabled) { Text(stringResource(R.string.button_restart)) }
                Button(onClick = onNextPuzzleClick) { Text(stringResource(R.string.button_next)) }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onShowSolutionClick) { Text(if (showSolutionPath) stringResource(R.string.button_hide) else stringResource(R.string.button_solution)) }
                Button(onClick = onHintClick) { Text(stringResource(R.string.button_hint)) }
                Button(onClick = onNextPuzzleClick) { Text(stringResource(R.string.button_next)) }
            }
        }

        if (showSolutionPath) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.solution_controls_title), style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onPreviousMoveClick, enabled = solutionMoveIndex > 0) { Text(stringResource(R.string.button_previous_move)) }
                Button(onClick = onPlayPauseClick, enabled = solutionMoveCount > 0) { Text(if (isPlayingSolution) stringResource(R.string.button_pause) else stringResource(R.string.button_play)) }
                Button(onClick = onNextMoveClick, enabled = solutionMoveCount > 0 && solutionMoveIndex < solutionMoveCount) { Text(stringResource(R.string.button_next_move)) }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLandscape) {
            Button(
                onClick = onSurrenderClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.button_surrender))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onRestartClick, enabled = isRestartEnabled) { Text(stringResource(R.string.button_restart)) }
                Button(
                    onClick = onSurrenderClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.button_surrender))
                }
            }
        }
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
    BoxWithConstraints(modifier = modifier
        .background(Color.DarkGray)
        .aspectRatio(1f)) {
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
        Card(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(stringResource(R.string.dialog_title_defended_square), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                Text(stringResource(R.string.dialog_message_defended_square), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 16.dp))
                ChessBoardComposable(board = board, onSquareClick = {}, selectedSquare = null, highlightedHintMove = null, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.button_try_again)) }
            }
        }
    }
}

/**
 * Direktno mapiranje figure na drawable.
 *
 * Ranije se koristio `resources.getIdentifier()` - spor (traženje po imenu) i nekompatibilan
 * sa `isShrinkResources`, jer R8 ne vidi te reference pa bi obrisao slike figura.
 */
@DrawableRes
fun getPieceDrawableResId(piece: Piece): Int = when (piece.color) {
    ChessColor.WHITE -> when (piece.type) {
        PieceType.PAWN -> R.drawable.wp
        PieceType.KNIGHT -> R.drawable.wn
        PieceType.BISHOP -> R.drawable.wb
        PieceType.ROOK -> R.drawable.wr
        PieceType.QUEEN -> R.drawable.wq
        PieceType.KING -> R.drawable.wk
    }
    ChessColor.BLACK -> when (piece.type) {
        PieceType.PAWN -> R.drawable.bp
        PieceType.KNIGHT -> R.drawable.bn
        PieceType.BISHOP -> R.drawable.bb
        PieceType.ROOK -> R.drawable.br
        PieceType.QUEEN -> R.drawable.bq
        PieceType.KING -> R.drawable.bk
    }
}
