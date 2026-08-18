package com.program.braintrainer.score

import com.program.braintrainer.chess.model.Difficulty
import kotlin.math.max

/**
 * Centralno mesto za podešavanje bodovanja.
 */
data class ScoringParams(
    val basePointsEasy: Int = 10,
    val basePointsMedium: Int = 20,
    val basePointsHard: Int = 30,
    val maxTimeForBonusEasy: Int = 30,
    val maxTimeForBonusMedium: Int = 60,
    val maxTimeForBonusHard: Int = 90,
    val penaltyPerMistake: Int = 5,
    val penaltyPerExtraMove: Int = 2,
    val streakBonusEasy: Int = 3,
    val streakBonusMedium: Int = 8,
    val streakBonusHard: Int = 20
)

/**
 * Razložen rezultat jedne rešene zagonetke. Svaka stavka postoji zasebno da bi UI
 * mogao da je prikaže, a da se sama računica ne meša sa formatiranjem teksta.
 */
data class PuzzleScore(
    val basePoints: Int,
    val timeBonus: Int,
    val extraMoves: Int,
    val efficiencyPenalty: Int,
    val mistakes: Int,
    val mistakePenalty: Int,
    val isPerfect: Boolean,
    /** Niz uzastopnih savršenih rešenja POSLE ove zagonetke. */
    val streak: Int,
    val streakBonus: Int,
    val totalXp: Int
)

/**
 * Čista računica bodova - bez Android zavisnosti, bez stringova, bez stanja.
 * Izdvojena iz ChessScreen-a da bi mogla da se testira.
 */
object ScoreCalculator {

    fun calculate(
        difficulty: Difficulty,
        elapsedSeconds: Int,
        playerMoveCount: Int,
        optimalMoves: Int,
        mistakes: Int,
        isPerfect: Boolean,
        previousStreak: Int,
        params: ScoringParams = ScoringParams()
    ): PuzzleScore {
        val basePoints = when (difficulty) {
            Difficulty.EASY -> params.basePointsEasy
            Difficulty.MEDIUM -> params.basePointsMedium
            Difficulty.HARD -> params.basePointsHard
        }
        val maxTimeForBonus = when (difficulty) {
            Difficulty.EASY -> params.maxTimeForBonusEasy
            Difficulty.MEDIUM -> params.maxTimeForBonusMedium
            Difficulty.HARD -> params.maxTimeForBonusHard
        }
        val bonusPerStreak = when (difficulty) {
            Difficulty.EASY -> params.streakBonusEasy
            Difficulty.MEDIUM -> params.streakBonusMedium
            Difficulty.HARD -> params.streakBonusHard
        }

        val timeBonus = max(0, maxTimeForBonus - elapsedSeconds)
        val extraMoves = max(0, playerMoveCount - optimalMoves)
        val efficiencyPenalty = extraMoves * params.penaltyPerExtraMove
        val mistakePenalty = mistakes * params.penaltyPerMistake

        val streak = if (isPerfect) previousStreak + 1 else 0
        val streakBonus = if (isPerfect) streak * bonusPerStreak else 0

        // Streak bonus se dodaje IZVAN max(0, ...) - kazne mogu da ponište osnovne
        // poene i vremenski bonus, ali ne i nagradu za niz.
        val totalXp = streakBonus + max(0, basePoints + timeBonus - efficiencyPenalty - mistakePenalty)

        return PuzzleScore(
            basePoints = basePoints,
            timeBonus = timeBonus,
            extraMoves = extraMoves,
            efficiencyPenalty = efficiencyPenalty,
            mistakes = mistakes,
            mistakePenalty = mistakePenalty,
            isPerfect = isPerfect,
            streak = streak,
            streakBonus = streakBonus,
            totalXp = totalXp
        )
    }
}
