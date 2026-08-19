package com.program.braintrainer.ui.screens.chess

import com.program.braintrainer.chess.model.Board
import com.program.braintrainer.chess.model.Move
import com.program.braintrainer.chess.model.Square
import com.program.braintrainer.gamification.AchievementId
import com.program.braintrainer.score.PuzzleScore

/**
 * Kompletno stanje ekrana za igru. Jedan objekat umesto 24 zasebne `remember`
 * promenljive koliko ih je ChessScreen ranije držao.
 */
data class ChessUiState(
    val isLoading: Boolean = true,

    /** Sat se ne prikazuje, ali vreme i dalje teče i bodovanje je nepromenjeno. */
    val hideTimer: Boolean = false,

    // trenutna zagonetka
    val board: Board = Board(),
    val selectedSquare: Square? = null,
    val optimalMoves: Int = 0,
    val solutionMoveCount: Int = 0,

    // napredak kroz sesiju
    val sessionSize: Int = 0,
    val currentIndexInSession: Int = 0,
    val isLastPuzzle: Boolean = false,

    // brojači za bodovanje
    val elapsedSeconds: Int = 0,
    val playerMoveCount: Int = 0,
    val mistakes: Int = 0,

    // prikaz rešenja
    val showSolutionPath: Boolean = false,
    val isPlayingSolution: Boolean = false,
    val solutionMoveIndex: Int = -1,
    val usedSolution: Boolean = false,

    // hint
    val highlightedHintMove: Move? = null,

    // dijalozi
    val outcome: PuzzleOutcome? = null,
    val showNoMoreMovesDialog: Boolean = false,
    val showSessionEndDialog: Boolean = false,
    val totalXpAtSessionEnd: Int = 0,
    val defendedSquareBoard: Board? = null
) {
    /** Ponovno pokretanje nema smisla kad je rešenje već otkriveno ili je partija gotova. */
    val isRestartEnabled: Boolean
        get() = !usedSolution && outcome == null && !showNoMoreMovesDialog

    /** Dok traje hint ili prikaz rešenja, klik na tablu se ignoriše. */
    val isBoardLocked: Boolean
        get() = showSolutionPath || outcome != null || showSessionEndDialog ||
                showNoMoreMovesDialog || highlightedHintMove != null
}

/**
 * Ishod zagonetke. Namerno nosi podatke, a ne gotov tekst - formatiranje sa
 * `stringResource` je posao UI sloja, pa ViewModel ne mora da drži Context.
 */
sealed interface PuzzleOutcome {
    /** Rešeno samostalno. [premiumBonusXp] je 0 za korisnike bez premiuma. */
    data class Solved(val score: PuzzleScore, val premiumBonusXp: Int) : PuzzleOutcome

    /** Rešeno tek pošto je igrač pogledao rešenje - bez bodova. */
    data object SolvedWithHelp : PuzzleOutcome

    data object Surrendered : PuzzleOutcome

    /** Nema više legalnih poteza, zagonetka nije rešena. */
    data object Failed : PuzzleOutcome
}

/** Jednokratni događaji: zvuk i snackbar poruke. */
sealed interface ChessUiEvent {
    data object InvalidMove : ChessUiEvent
    data object Module1MustCapture : ChessUiEvent
    data object SolverFailed : ChessUiEvent
    data class PlaySound(val sound: GameSound) : ChessUiEvent

    /** Nosi samo ID - naziv dostignuća UI prevodi sam. */
    data class AchievementUnlocked(val id: AchievementId) : ChessUiEvent
}

enum class GameSound { SUCCESS, FAILURE }

/** Akcije koje UI prosleđuje ViewModel-u, grupisane da potpis ekrana ostane čitljiv. */
data class ChessActions(
    val onSquareClick: (Square) -> Unit,
    val onHintClick: () -> Unit,
    val onShowSolution: () -> Unit,
    val onNextPuzzle: () -> Unit,
    val onPreviousMove: () -> Unit,
    val onNextMove: () -> Unit,
    val onPlayPauseSolution: () -> Unit,
    val onSurrender: () -> Unit,
    val onRestart: () -> Unit,
    val onDismissDefendedSquare: () -> Unit
)
