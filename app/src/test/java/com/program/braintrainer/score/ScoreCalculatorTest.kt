package com.program.braintrainer.score

import com.program.braintrainer.chess.model.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreCalculatorTest {

    private val params = ScoringParams()

    @Test
    fun `savrseno resenje na lakom nivou daje osnovne poene, vremenski bonus i streak`() {
        val score = ScoreCalculator.calculate(
            difficulty = Difficulty.EASY,
            elapsedSeconds = 10,
            playerMoveCount = 3,
            optimalMoves = 3,
            mistakes = 0,
            isPerfect = true,
            previousStreak = 0,
            params = params
        )
        assertEquals(10, score.basePoints)
        assertEquals(20, score.timeBonus)          // 30 - 10
        assertEquals(0, score.efficiencyPenalty)
        assertEquals(0, score.mistakePenalty)
        assertEquals(1, score.streak)
        assertEquals(3, score.streakBonus)         // streak 1 * 3
        assertEquals(33, score.totalXp)            // 3 + max(0, 10 + 20 - 0 - 0)
    }

    @Test
    fun `vremenski bonus ne moze biti negativan`() {
        val score = ScoreCalculator.calculate(
            difficulty = Difficulty.EASY,
            elapsedSeconds = 500,
            playerMoveCount = 1,
            optimalMoves = 1,
            mistakes = 0,
            isPerfect = true,
            previousStreak = 0,
            params = params
        )
        assertEquals(0, score.timeBonus)
    }

    @Test
    fun `visak poteza se kaznjava po potezu`() {
        val score = ScoreCalculator.calculate(
            difficulty = Difficulty.MEDIUM,
            elapsedSeconds = 0,
            playerMoveCount = 7,
            optimalMoves = 4,
            mistakes = 0,
            isPerfect = false,
            previousStreak = 0,
            params = params
        )
        assertEquals(3, score.extraMoves)
        assertEquals(6, score.efficiencyPenalty)   // 3 * 2
    }

    @Test
    fun `greske se kaznjavaju ali ukupan skor ne ide ispod nule`() {
        val score = ScoreCalculator.calculate(
            difficulty = Difficulty.EASY,
            elapsedSeconds = 30,                   // nema vremenskog bonusa
            playerMoveCount = 1,
            optimalMoves = 1,
            mistakes = 100,                        // kazna 500, daleko preko osnovnih 10
            isPerfect = false,
            previousStreak = 0,
            params = params
        )
        assertEquals(500, score.mistakePenalty)
        assertEquals(0, score.totalXp)
    }

    @Test
    fun `streak bonus prezivljava kaznu koja bi inace ponistila sve poene`() {
        val score = ScoreCalculator.calculate(
            difficulty = Difficulty.HARD,
            elapsedSeconds = 90,                   // nema vremenskog bonusa
            playerMoveCount = 1,
            optimalMoves = 1,
            mistakes = 50,                         // kazna 250 > osnovnih 30
            isPerfect = true,
            previousStreak = 1,
            params = params
        )
        assertEquals(2, score.streak)
        assertEquals(40, score.streakBonus)        // streak 2 * 20
        assertEquals(40, score.totalXp)            // 40 + max(0, 30 + 0 - 0 - 250)
    }

    @Test
    fun `neuspeh niza resetuje streak na nulu`() {
        val score = ScoreCalculator.calculate(
            difficulty = Difficulty.MEDIUM,
            elapsedSeconds = 5,
            playerMoveCount = 4,
            optimalMoves = 4,
            mistakes = 1,
            isPerfect = false,
            previousStreak = 7,
            params = params
        )
        assertEquals(0, score.streak)
        assertEquals(0, score.streakBonus)
        assertFalse(score.isPerfect)
    }

    @Test
    fun `streak raste kroz uzastopna savrsena resenja`() {
        var streak = 0
        repeat(3) {
            streak = ScoreCalculator.calculate(
                difficulty = Difficulty.EASY,
                elapsedSeconds = 0,
                playerMoveCount = 2,
                optimalMoves = 2,
                mistakes = 0,
                isPerfect = true,
                previousStreak = streak,
                params = params
            ).streak
        }
        assertEquals(3, streak)
    }

    @Test
    fun `tezina odredjuje osnovne poene i bonus za niz`() {
        val easy = ScoreCalculator.calculate(Difficulty.EASY, 0, 1, 1, 0, true, 0, params)
        val medium = ScoreCalculator.calculate(Difficulty.MEDIUM, 0, 1, 1, 0, true, 0, params)
        val hard = ScoreCalculator.calculate(Difficulty.HARD, 0, 1, 1, 0, true, 0, params)

        assertEquals(10, easy.basePoints)
        assertEquals(20, medium.basePoints)
        assertEquals(30, hard.basePoints)

        assertEquals(3, easy.streakBonus)
        assertEquals(8, medium.streakBonus)
        assertEquals(20, hard.streakBonus)

        assertTrue(hard.totalXp > medium.totalXp && medium.totalXp > easy.totalXp)
    }
}
