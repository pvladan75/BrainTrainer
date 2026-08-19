package com.program.braintrainer.ui

import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Ruta partije nosi četiri stvari: modul, težinu, spisak zagonetki i podešavanja
 * sesije. Piše se na jednom mestu, a čita na drugom, pa je oblik vredan testa.
 */
class RoutesTest {

    @Test
    fun `obicna sesija ima prazne zagonetke i podrazumevanu duzinu`() {
        assertEquals(
            "chess_game/Module1/EASY?puzzleIds=&size=10&hideTimer=false",
            Routes.createChessGameRoute(Module.Module1, Difficulty.EASY)
        )
    }

    @Test
    fun `revans nosi spisak zagonetki razdvojen zarezom`() {
        assertEquals(
            "chess_game/Module3/HARD?puzzleIds=7,12,90&size=10&hideTimer=false",
            Routes.createChessGameRoute(
                Module.Module3,
                Difficulty.HARD,
                puzzleIds = listOf("7", "12", "90")
            )
        )
    }

    @Test
    fun `trening po meri nosi duzinu i skriven sat`() {
        assertEquals(
            "chess_game/Module2/MEDIUM?puzzleIds=&size=20&hideTimer=true",
            Routes.createChessGameRoute(
                Module.Module2,
                Difficulty.MEDIUM,
                sessionSize = 20,
                hideTimer = true
            )
        )
    }

    @Test
    fun `sablon rute pokriva sve parametre koje ruta postavlja`() {
        val template = Routes.CHESS_GAME

        listOf("{moduleType}", "{difficultyType}", "{puzzleIds}", "{size}", "{hideTimer}")
            .forEach { placeholder ->
                assert(template.contains(placeholder)) { "nedostaje $placeholder u $template" }
            }
    }
}
