package com.program.braintrainer.stats

import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module

/** Kako se zagonetka završila. */
enum class AttemptOutcome {
    /** Rešena samostalno. */
    SOLVED,

    /** Rešena tek pošto je igrač pogledao rešenje — bez bodova. */
    SOLVED_WITH_HELP,

    /** Igrač je predao. */
    SURRENDERED,

    /** Nema više poteza, zagonetka nije rešena. */
    FAILED
}

/**
 * Jedan odigran pokušaj. Zapisuje se za **svaki** ishod, ne samo za rešene —
 * dnevnik grešaka i grafici napretka žive od onoga što nije pošlo za rukom.
 */
data class PuzzleAttempt(
    val puzzleId: String,
    val module: Module,
    val difficulty: Difficulty,
    val finishedAt: Long,
    val outcome: AttemptOutcome,
    val elapsedSeconds: Int,
    val playerMoves: Int,
    val optimalMoves: Int,
    val mistakes: Int,
    val earnedXp: Int
) {
    /** Bez greške i bez poteza viška — isti kriterijum koji koristi bodovanje. */
    val isPerfect: Boolean
        get() = outcome == AttemptOutcome.SOLVED && mistakes == 0 && playerMoves <= optimalMoves
}

/** Jedan dan vežbanja, za grafik napretka. Dan je lokalni, u obliku `YYYY-MM-DD`. */
data class DayTotals(
    val day: String,
    val attempts: Int,
    val solved: Int,
    val earnedXp: Int,
    val seconds: Int
)

/**
 * Zagonetka čiji **poslednji** pokušaj nije bio rešen — kandidat za dnevnik
 * grešaka. Ranije rešena pa kasnije promašena zagonetka je i dalje otvorena;
 * obrnuto nije.
 */
data class OpenMistake(
    val puzzleId: String,
    val module: Module,
    val difficulty: Difficulty,
    val lastAttemptAt: Long,
    val attempts: Int
)

/** Zbir za jedan modul i težinu. */
data class ModuleSummary(
    val attempts: Int,
    val solved: Int,
    val perfect: Int,
    /** Najbrže rešavanje u sekundama, ili null ako nijedna nije rešena. */
    val bestSeconds: Int?,
    val totalSeconds: Int
)
