package com.program.braintrainer.ui.screens.chess

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.program.braintrainer.R
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module
import com.program.braintrainer.gamification.getAchievementsList
import com.program.braintrainer.score.PuzzleScore
import com.program.braintrainer.util.playSound

/**
 * Ekran za igru. Ne drži stanje partije - sve dolazi iz [ChessViewModel]-a.
 */
@Composable
fun ChessScreen(
    module: Module,
    difficulty: Difficulty,
    viewModel: ChessViewModel,
    onGameFinished: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ChessUiEvent.InvalidMove ->
                    snackbarHostState.showSnackbar(context.getString(R.string.snackbar_invalid_move))

                ChessUiEvent.Module1MustCapture ->
                    snackbarHostState.showSnackbar(context.getString(R.string.snackbar_module1_must_capture))

                ChessUiEvent.SolverFailed ->
                    snackbarHostState.showSnackbar(context.getString(R.string.snackbar_solver_failed))

                is ChessUiEvent.AchievementUnlocked -> {
                    val title = getAchievementsList(context).find { it.id == event.id }?.title
                    if (title != null) {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.snackbar_achievement_unlocked, title)
                        )
                    }
                }

                is ChessUiEvent.PlaySound -> playSound(
                    context,
                    when (event.sound) {
                        GameSound.SUCCESS -> R.raw.succes
                        GameSound.FAILURE -> R.raw.failed
                    }
                )
            }
        }
    }

    // Tajmer ne sme da radi dok je aplikacija u pozadini.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onScreenResumed()
                Lifecycle.Event.ON_STOP -> viewModel.onScreenPaused()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val actions = remember(viewModel) {
        ChessActions(
            onSquareClick = viewModel::onSquareClick,
            onHintClick = viewModel::onHintClick,
            onShowSolution = viewModel::onShowSolution,
            onNextPuzzle = viewModel::onNextPuzzle,
            onPreviousMove = viewModel::onPreviousSolutionMove,
            onNextMove = viewModel::onNextSolutionMove,
            onPlayPauseSolution = viewModel::onPlayPauseSolution,
            onSurrender = viewModel::onSurrender,
            onRestart = viewModel::onRestartPuzzle,
            onDismissDefendedSquare = viewModel::onDismissDefendedSquare
        )
    }

    ChessScreenContent(
        module = module,
        difficulty = difficulty,
        state = state,
        actions = actions,
        snackbarHostState = snackbarHostState,
        onGameFinished = onGameFinished
    )
}

/**
 * Prikaz bez ijedne zavisnosti na ViewModel - lako se pregleda u @Preview-u.
 */
@Composable
fun ChessScreenContent(
    module: Module,
    difficulty: Difficulty,
    state: ChessUiState,
    actions: ChessActions,
    snackbarHostState: SnackbarHostState,
    onGameFinished: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            val isLandscape =
                LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GameInfoPanel(
                        module = module,
                        difficulty = difficulty,
                        sessionSize = state.sessionSize,
                        currentSessionProblemIndex = state.currentIndexInSession,
                        elapsedTime = state.elapsedSeconds,
                        showTime = !state.hideTimer,
                        optimalMoves = state.optimalMoves,
                        playerMoveCount = state.playerMoveCount,
                        isLandscape = true,
                        modifier = Modifier.weight(1f)
                    )
                    ChessBoardComposable(
                        state.board,
                        state.selectedSquare,
                        actions.onSquareClick,
                        state.highlightedHintMove,
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .aspectRatio(1f)
                    )
                    GameControls(state, actions, isLandscape = true, modifier = Modifier.weight(1f))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    GameInfoPanel(
                        module = module,
                        difficulty = difficulty,
                        sessionSize = state.sessionSize,
                        currentSessionProblemIndex = state.currentIndexInSession,
                        elapsedTime = state.elapsedSeconds,
                        showTime = !state.hideTimer,
                        optimalMoves = state.optimalMoves,
                        playerMoveCount = state.playerMoveCount,
                        isLandscape = false
                    )
                    ChessBoardComposable(
                        state.board,
                        state.selectedSquare,
                        actions.onSquareClick,
                        state.highlightedHintMove,
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .aspectRatio(1f)
                    )
                    GameControls(state, actions, isLandscape = false)
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        state.outcome?.let { outcome ->
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.dialog_title_puzzle_status)) },
                text = { Text(outcomeMessage(module, outcome)) },
                dismissButton = {
                    TextButton(onClick = actions.onShowSolution) {
                        Text(stringResource(R.string.button_solution))
                    }
                },
                confirmButton = {
                    TextButton(onClick = actions.onNextPuzzle) {
                        Text(
                            if (state.isLastPuzzle) stringResource(R.string.button_end)
                            else stringResource(R.string.button_next)
                        )
                    }
                }
            )
        }

        if (state.showNoMoreMovesDialog) {
            NoMoreMovesDialog(
                onShowSolution = actions.onShowSolution,
                onNewGame = actions.onNextPuzzle
            )
        }

        if (state.showSessionEndDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.dialog_title_session_end)) },
                text = {
                    Column {
                        Text(stringResource(R.string.dialog_message_session_end))
                        Text(
                            stringResource(
                                R.string.dialog_message_total_xp,
                                state.totalXpAtSessionEnd
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = onGameFinished) {
                        Text(stringResource(R.string.button_main_menu))
                    }
                }
            )
        }

        state.defendedSquareBoard?.let { board ->
            DefendedSquareDialog(board = board, onDismiss = actions.onDismissDefendedSquare)
        }
    }
}

@Composable
private fun GameControls(
    state: ChessUiState,
    actions: ChessActions,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    GameControlsPanel(
        showSolutionPath = state.showSolutionPath,
        isPlayingSolution = state.isPlayingSolution,
        solutionMoveIndex = state.solutionMoveIndex,
        solutionMoveCount = state.solutionMoveCount,
        onShowSolutionClick = actions.onShowSolution,
        onNextPuzzleClick = actions.onNextPuzzle,
        onPreviousMoveClick = actions.onPreviousMove,
        onPlayPauseClick = actions.onPlayPauseSolution,
        onNextMoveClick = actions.onNextMove,
        onHintClick = actions.onHintClick,
        onSurrenderClick = actions.onSurrender,
        onRestartClick = actions.onRestart,
        isRestartEnabled = state.isRestartEnabled,
        isLandscape = isLandscape,
        modifier = modifier
    )
}

/**
 * Prevodi [PuzzleOutcome] u tekst dijaloga. Bodovi su već izračunati u
 * [com.program.braintrainer.score.ScoreCalculator]; ovde se samo formatiraju.
 */
@Composable
private fun outcomeMessage(module: Module, outcome: PuzzleOutcome): String = when (outcome) {
    PuzzleOutcome.Surrendered -> stringResource(R.string.game_result_surrendered)
    PuzzleOutcome.SolvedWithHelp -> stringResource(R.string.game_result_solved_with_help)
    PuzzleOutcome.Failed -> when (module) {
        Module.Module1 -> stringResource(R.string.game_result_fail_no_captures)
        else -> stringResource(R.string.game_result_fail_no_moves)
    }

    is PuzzleOutcome.Solved -> solvedMessage(outcome.score)
}

@Composable
private fun solvedMessage(score: PuzzleScore): String = buildString {
    append(stringResource(R.string.game_result_success_title))
    append(stringResource(R.string.game_result_base_points, score.basePoints))
    append(stringResource(R.string.game_result_time_bonus, score.timeBonus))
    append(
        stringResource(
            R.string.game_result_moves_penalty,
            score.efficiencyPenalty,
            score.extraMoves
        )
    )
    append(
        stringResource(
            R.string.game_result_mistakes_penalty,
            score.mistakePenalty,
            score.mistakes
        )
    )
    if (score.isPerfect) {
        append(stringResource(R.string.game_result_streak_bonus, score.streakBonus, score.streak))
    } else {
        append(stringResource(R.string.game_result_streak_lost))
    }
    append(stringResource(R.string.game_result_total_xp, score.totalXp))
}
