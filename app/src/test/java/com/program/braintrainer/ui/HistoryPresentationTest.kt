package com.program.braintrainer.ui

import com.program.braintrainer.stats.DayTotals
import com.program.braintrainer.ui.screens.history.DurationUnit
import com.program.braintrainer.ui.screens.history.durationParts
import com.program.braintrainer.ui.screens.history.fillMissingDays
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HistoryPresentationTest {

    private val today = LocalDate.of(2026, 8, 19)

    @Test
    fun `dani bez vezbanja se popunjavaju nulama`() {
        val days = fillMissingDays(
            totals = listOf(DayTotals("2026-08-19", attempts = 4, solved = 3, earnedXp = 40, seconds = 200)),
            days = 3,
            today = today
        )

        assertEquals(listOf("2026-08-17", "2026-08-18", "2026-08-19"), days.map { it.day })
        assertEquals(listOf(0, 0, 4), days.map { it.attempts })
        assertEquals(3, days.last().solved)
    }

    @Test
    fun `poslednji dan je uvek danasnji`() {
        val days = fillMissingDays(emptyList(), days = 14, today = today)

        assertEquals(14, days.size)
        assertEquals("2026-08-19", days.last().day)
        assertEquals("2026-08-06", days.first().day)
    }

    @Test
    fun `dani van opsega se ne prikazuju`() {
        val days = fillMissingDays(
            totals = listOf(
                DayTotals("2026-08-01", attempts = 9, solved = 9, earnedXp = 90, seconds = 500),
                DayTotals("2026-08-18", attempts = 2, solved = 1, earnedXp = 10, seconds = 60)
            ),
            days = 3,
            today = today
        )

        assertEquals(listOf(0, 2, 0), days.map { it.attempts })
    }

    @Test
    fun `trajanje bira jedinicu po velicini`() {
        assertEquals(Triple(DurationUnit.SECONDS, 45, 0), durationParts(45))
        assertEquals(Triple(DurationUnit.MINUTES, 1, 0), durationParts(60))
        assertEquals(Triple(DurationUnit.MINUTES, 59, 59), durationParts(3599))
        assertEquals(Triple(DurationUnit.HOURS, 1, 0), durationParts(3600))
        assertEquals(Triple(DurationUnit.HOURS, 2, 30), durationParts(9000))
    }

    @Test
    fun `negativno trajanje se ne prikazuje kao negativno`() {
        assertEquals(Triple(DurationUnit.SECONDS, 0, 0), durationParts(-5))
    }
}
