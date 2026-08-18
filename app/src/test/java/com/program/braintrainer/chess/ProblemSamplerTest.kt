package com.program.braintrainer.chess

import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module
import com.program.braintrainer.chess.model.data.ProblemSampler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class ProblemSamplerTest {

    private fun stream(vararg lines: String) = lines.joinToString("\n").byteInputStream()

    private fun puzzleLine(id: Int) =
        """{"id":"$id","module":"Module3","difficulty":"EASY","fen":"8/8/5r2/8/7n/2k1q3/8/5N2 w - - 0 1",""" +
            """"description":"Pojedi bezbedno crnog kralja","solution":{"moves":["f1e3"]}}"""

    private fun lines(count: Int) = Array(count) { puzzleLine(it + 1) }

    @Test
    fun `vraca tacno trazeni broj zagonetki bez ponavljanja`() {
        val sample = ProblemSampler.sample(stream(*lines(500)), 10, Random(1))

        assertEquals(10, sample.size)
        assertEquals(10, sample.map { it.id }.toSet().size)
        assertTrue(sample.all { it.id.toInt() in 1..500 })
    }

    @Test
    fun `kad ima manje zagonetki nego trazeno vraca sve`() {
        val sample = ProblemSampler.sample(stream(*lines(4)), 10, Random(1))

        assertEquals(4, sample.size)
        assertEquals(setOf("1", "2", "3", "4"), sample.map { it.id }.toSet())
    }

    @Test
    fun `uzorak nije uvek isti i ne dolazi samo sa pocetka fajla`() {
        val seenIds = (1..20).flatMap { seed ->
            ProblemSampler.sample(stream(*lines(500)), 10, Random(seed)).map { it.id }
        }.toSet()

        // Deterministički uzorak bi dao istih deset; rezervoar mora da zahvati
        // znatno više različitih zagonetki, i to iz celog fajla.
        assertTrue("različitih zagonetki: ${seenIds.size}", seenIds.size > 50)
        assertTrue(seenIds.any { it.toInt() > 250 })
    }

    @Test
    fun `prazni i neispravni redovi se preskacu`() {
        val sample = ProblemSampler.sample(
            stream(puzzleLine(1), "", "{ ovo nije JSON", puzzleLine(2), ""),
            10,
            Random(1)
        )

        assertEquals(setOf("1", "2"), sample.map { it.id }.toSet())
    }

    @Test
    fun `nula zagonetki daje praznu listu`() {
        assertEquals(emptyList<Any>(), ProblemSampler.sample(stream(*lines(10)), 0, Random(1)))
    }

    @Test
    fun `selectByIds vraca zagonetke redosledom kojim su trazene`() {
        val selected = ProblemSampler.selectByIds(stream(*lines(300)), listOf("42", "7", "299"))

        assertEquals(listOf("42", "7", "299"), selected.map { it.id })
    }

    @Test
    fun `selectByIds preskace nepoznate ID-jeve i prazan zahtev`() {
        val selected = ProblemSampler.selectByIds(stream(*lines(10)), listOf("3", "nepostojeci", "9"))

        assertEquals(listOf("3", "9"), selected.map { it.id })
        assertEquals(emptyList<Any>(), ProblemSampler.selectByIds(stream(*lines(10)), emptyList()))
    }

    @Test
    fun `selectByIds radi nad stvarnim assets fajlom`() {
        val file = File("src/main/assets/module1_hard_puzzles.jsonl")
        val sample = ProblemSampler.sample(file.inputStream(), 5, Random(11))
        val ids = sample.map { it.id }.reversed()

        val selected = ProblemSampler.selectByIds(file.inputStream(), ids)

        assertEquals(ids, selected.map { it.id })
        assertEquals(sample.sortedBy { it.id }, selected.sortedBy { it.id })
    }

    @Test
    fun `svaki red u assets fajlu je ispravna zagonetka`() {
        val file = File("src/main/assets/module3_hard_puzzles.jsonl")
        assertTrue("nedostaje ${file.absolutePath}", file.exists())

        val total = file.readLines().count { it.isNotEmpty() }
        val sample = ProblemSampler.sample(file.inputStream(), total, Random(1))

        assertEquals(total, sample.size)
        assertTrue(sample.all { it.module == Module.Module3 && it.difficulty == Difficulty.HARD })
        assertTrue(sample.all { it.fen.isNotEmpty() && it.solution.moves.isNotEmpty() })
    }
}
