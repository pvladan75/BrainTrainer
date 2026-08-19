package com.program.braintrainer.ui

import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module
import com.program.braintrainer.stats.OpenMistake
import com.program.braintrainer.ui.screens.mistakes.groupMistakes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MistakeGroupingTest {

    private fun mistake(
        id: String,
        module: Module,
        difficulty: Difficulty,
        at: Long
    ) = OpenMistake(id, module, difficulty, at, attempts = 1)

    @Test
    fun `greske se grupisu po modulu i tezini`() {
        val groups = groupMistakes(
            listOf(
                mistake("1", Module.Module1, Difficulty.EASY, 300),
                mistake("2", Module.Module1, Difficulty.HARD, 200),
                mistake("3", Module.Module1, Difficulty.EASY, 100)
            )
        )

        assertEquals(2, groups.size)
        val easy = groups.first { it.difficulty == Difficulty.EASY }
        assertEquals(listOf("1", "3"), easy.puzzleIds)
        assertEquals(300, easy.lastAttemptAt)
    }

    @Test
    fun `isti modul i tezina iz razlicitih modula ne padaju u istu grupu`() {
        val groups = groupMistakes(
            listOf(
                mistake("1", Module.Module1, Difficulty.EASY, 100),
                mistake("2", Module.Module3, Difficulty.EASY, 200)
            )
        )

        assertEquals(2, groups.size)
        assertTrue(groups.all { it.puzzleIds.size == 1 })
    }

    @Test
    fun `najskorija grupa je prva`() {
        val groups = groupMistakes(
            listOf(
                mistake("stara", Module.Module1, Difficulty.EASY, 100),
                mistake("nova", Module.Module2, Difficulty.MEDIUM, 900),
                mistake("srednja", Module.Module3, Difficulty.HARD, 500)
            )
        )

        assertEquals(listOf("nova", "srednja", "stara"), groups.map { it.puzzleIds.single() })
    }

    @Test
    fun `prazan spisak daje praznu listu grupa`() {
        assertEquals(emptyList<Any>(), groupMistakes(emptyList()))
    }
}
