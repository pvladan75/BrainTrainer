package com.program.braintrainer.stats

import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttemptMappingTest {

    private val attempt = PuzzleAttempt(
        puzzleId = "42",
        module = Module.Module3,
        difficulty = Difficulty.HARD,
        finishedAt = 1_700_000_000_000,
        outcome = AttemptOutcome.SOLVED,
        elapsedSeconds = 33,
        playerMoves = 4,
        optimalMoves = 4,
        mistakes = 0,
        earnedXp = 120
    )

    @Test
    fun `zapis prezivi put kroz bazu nepromenjen`() {
        assertEquals(attempt, attempt.toEntity().toAttempt())
    }

    @Test
    fun `nepoznat modul ili ishod daje null umesto pada`() {
        val entity = attempt.toEntity()

        assertNull(entity.copy(module = "Module9").toAttempt())
        assertNull(entity.copy(difficulty = "IMPOSSIBLE").toAttempt())
        assertNull(entity.copy(outcome = "ABANDONED").toAttempt())
    }

    @Test
    fun `savrseno resenje je bez greske i bez poteza viska`() {
        assertTrue(attempt.isPerfect)
        assertFalse(attempt.copy(mistakes = 1).isPerfect)
        assertFalse(attempt.copy(playerMoves = 5).isPerfect)
        assertFalse(attempt.copy(outcome = AttemptOutcome.SOLVED_WITH_HELP).isPerfect)
    }

    @Test
    fun `manje poteza od optimalnog je i dalje savrseno`() {
        assertTrue(attempt.copy(playerMoves = 3).isPerfect)
    }
}
