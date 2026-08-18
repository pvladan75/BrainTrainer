package com.program.braintrainer.chess

import com.program.braintrainer.chess.model.data.ProblemSampler
import com.program.braintrainer.chess.parser.FenParser
import com.program.braintrainer.chess.solver.UniversalPuzzleSolver
import com.program.braintrainer.rules.Module1Rules
import com.program.braintrainer.rules.Module3Rules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class UniversalPuzzleSolverTest {

    private fun boardFromAsset(name: String, seed: Int) =
        ProblemSampler.sample(File("src/main/assets/${name}_puzzles.jsonl").inputStream(), 1, Random(seed))
            .first()
            .let { FenParser.parseFenToBoard(it.fen).first }

    @Test
    fun `resava lake zagonetke i vraca putanju do cilja`() {
        repeat(5) { seed ->
            val board = boardFromAsset("module3_easy", seed)
            val solution = UniversalPuzzleSolver(Module3Rules()).solve(board)

            assertTrue(solution.message, solution.isSolved)
            assertTrue(solution.path.isNotEmpty())

            // Putanja mora zaista da vodi od početne table do rešenja.
            var replayed = board
            for (move in solution.path) {
                replayed = replayed.applyMove(move.start, move.end)!!
            }
            assertEquals(solution.finalBoard, replayed)
            assertTrue(Module3Rules().isGoalReached(replayed))
        }
    }

    @Test
    fun `budzet vremena prekida pretragu umesto da traje neograniceno`() {
        val board = boardFromAsset("module1_hard", 3)
        val solver = UniversalPuzzleSolver(Module1Rules(), timeBudgetMillis = 50L)

        val elapsed = measureTimeMillis {
            val solution = solver.solve(board)
            if (!solution.isSolved) assertEquals("Pretraga je prekinuta.", solution.message)
        }

        // Bez budžeta bi teška pozicija mogla da traje sekundama.
        assertTrue("trajalo $elapsed ms", elapsed < 2_000)
    }

    @Test
    fun `budzet broja stanja prekida pretragu`() {
        val board = boardFromAsset("module1_hard", 3)
        val solution = UniversalPuzzleSolver(Module1Rules(), maxVisitedStates = 5).solve(board)

        assertFalse(solution.isSolved)
        assertEquals("Pretraga je prekinuta.", solution.message)
        assertTrue(solution.path.isEmpty())
    }
}
