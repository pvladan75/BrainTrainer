package com.program.braintrainer.stats

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Upiti nad pravim SQLite-om. Room proverava sintaksu pri prevođenju, ali ne i
 * da li upit znači ono što mislimo — a `openMistakes` i dnevni zbir su tačno
 * ona vrsta upita koja se lako napiše naopako.
 */
@RunWith(AndroidJUnit4::class)
class AttemptDaoTest {

    private lateinit var database: StatsDatabase
    private lateinit var dao: AttemptDao

    private val dan = 24L * 60 * 60 * 1000

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            StatsDatabase::class.java
        ).build()
        dao = database.attemptDao()
    }

    @After
    fun tearDown() = database.close()

    private fun attempt(
        puzzleId: String,
        outcome: String,
        finishedAt: Long,
        module: String = "Module1",
        difficulty: String = "EASY",
        elapsedSeconds: Int = 30,
        playerMoves: Int = 5,
        optimalMoves: Int = 5,
        mistakes: Int = 0,
        earnedXp: Int = 0
    ) = AttemptEntity(
        puzzleId = puzzleId,
        module = module,
        difficulty = difficulty,
        finishedAt = finishedAt,
        outcome = outcome,
        elapsedSeconds = elapsedSeconds,
        playerMoves = playerMoves,
        optimalMoves = optimalMoves,
        mistakes = mistakes,
        earnedXp = earnedXp
    )

    @Test
    fun najskoriji_pokusaji_dolaze_prvi() = runBlocking {
        val now = System.currentTimeMillis()
        dao.insert(attempt("1", "SOLVED", now - 2 * dan))
        dao.insert(attempt("2", "FAILED", now - dan))
        dao.insert(attempt("3", "SOLVED", now))

        assertEquals(3, dao.count())
        assertEquals(listOf("3", "2"), dao.recent(2).map { it.puzzleId })
    }

    @Test
    fun otvorena_greska_je_ona_ciji_je_poslednji_pokusaj_neuspesan() = runBlocking {
        val now = System.currentTimeMillis()
        // Prvo promašena pa rešena — zatvorena.
        dao.insert(attempt("zatvorena", "FAILED", now - 2 * dan))
        dao.insert(attempt("zatvorena", "SOLVED", now - dan))
        // Prvo rešena pa promašena — ponovo otvorena.
        dao.insert(attempt("otvorena", "SOLVED", now - 2 * dan))
        dao.insert(attempt("otvorena", "FAILED", now - dan))
        // Predaja se takođe računa kao otvorena.
        dao.insert(attempt("predata", "SURRENDERED", now))

        val open = dao.openMistakes(10)

        assertEquals(listOf("predata", "otvorena"), open.map { it.puzzleId })
        assertEquals(2, open.first { it.puzzleId == "otvorena" }.attempts)
    }

    @Test
    fun dnevni_zbir_grupise_po_danu() = runBlocking {
        val now = System.currentTimeMillis()
        dao.insert(attempt("1", "SOLVED", now, elapsedSeconds = 20, earnedXp = 10))
        dao.insert(attempt("2", "FAILED", now, elapsedSeconds = 40))
        dao.insert(attempt("3", "SOLVED", now - 3 * dan, elapsedSeconds = 15, earnedXp = 5))

        val totals = dao.dailyTotals(now - 5 * dan)

        assertEquals(2, totals.size)
        val danas = totals.last()
        assertEquals(2, danas.attempts)
        assertEquals(1, danas.solved)
        assertEquals(10, danas.earnedXp)
        assertEquals(60, danas.seconds)

        // Starije od granice se ne broji.
        assertEquals(1, dao.dailyTotals(now - dan).size)
    }

    @Test
    fun zbir_po_modulu_broji_savrsena_resenja_i_najbolje_vreme() = runBlocking {
        val now = System.currentTimeMillis()
        dao.insert(attempt("1", "SOLVED", now, elapsedSeconds = 30, mistakes = 0, playerMoves = 5, optimalMoves = 5))
        dao.insert(attempt("2", "SOLVED", now, elapsedSeconds = 12, mistakes = 2, playerMoves = 9, optimalMoves = 5))
        dao.insert(attempt("3", "FAILED", now, elapsedSeconds = 50))
        dao.insert(attempt("4", "SOLVED", now, difficulty = "HARD", elapsedSeconds = 5))

        val summary = dao.summary("Module1", "EASY")

        assertEquals(3, summary.attempts)
        assertEquals(2, summary.solved)
        assertEquals(1, summary.perfect)
        assertEquals(12, summary.bestSeconds)
        assertEquals(92, summary.totalSeconds)
    }

    @Test
    fun zbir_po_grupama_vraca_red_za_svaku_kombinaciju() = runBlocking {
        val now = System.currentTimeMillis()
        dao.insert(attempt("1", "SOLVED", now, elapsedSeconds = 30))
        dao.insert(attempt("2", "FAILED", now, elapsedSeconds = 10))
        dao.insert(attempt("3", "SOLVED", now, difficulty = "HARD", elapsedSeconds = 44))
        dao.insert(attempt("4", "SOLVED", now, module = "Module3", elapsedSeconds = 7))

        val byGroup = dao.summaries().associateBy { it.module to it.difficulty }

        assertEquals(3, byGroup.size)
        assertEquals(2, byGroup.getValue("Module1" to "EASY").attempts)
        assertEquals(1, byGroup.getValue("Module1" to "EASY").solved)
        assertEquals(44, byGroup.getValue("Module1" to "HARD").bestSeconds)
        assertEquals(7, byGroup.getValue("Module3" to "EASY").totalSeconds)
    }

    @Test
    fun bez_resene_zagonetke_nema_najboljeg_vremena() = runBlocking {
        dao.insert(attempt("1", "FAILED", System.currentTimeMillis()))

        val summary = dao.summary("Module1", "EASY")

        assertEquals(1, summary.attempts)
        assertEquals(0, summary.solved)
        assertNull(summary.bestSeconds)
        assertTrue(summary.totalSeconds > 0)
    }
}
