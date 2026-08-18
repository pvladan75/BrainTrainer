package com.program.braintrainer.chess

import com.program.braintrainer.chess.model.Color
import com.program.braintrainer.chess.model.PieceType
import com.program.braintrainer.chess.model.Square
import com.program.braintrainer.chess.parser.FenParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

private fun sq(name: String) = Square(name[0], name[1].digitToInt())

class FenParserTest {

    private val startPosition = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    @Test
    fun `pocetna pozicija ima 32 figure na ocekivanim poljima`() {
        val (board, activeColor) = FenParser.parseFenToBoard(startPosition)

        assertEquals(32, board.pieces.size)
        assertEquals(Color.WHITE, activeColor)

        assertEquals(PieceType.ROOK, board.getPiece(sq("a1"))!!.type)
        assertEquals(Color.WHITE, board.getPiece(sq("a1"))!!.color)
        assertEquals(PieceType.KING, board.getPiece(sq("e1"))!!.type)
        assertEquals(PieceType.QUEEN, board.getPiece(sq("d1"))!!.type)

        assertEquals(PieceType.KING, board.getPiece(sq("e8"))!!.type)
        assertEquals(Color.BLACK, board.getPiece(sq("e8"))!!.color)

        assertNull(board.getPiece(sq("e4")))
    }

    @Test
    fun `parsira poziciju iz asset zagonetke`() {
        // Prva zagonetka iz module3_easy_puzzles.json
        val (board, _) = FenParser.parseFenToBoard("8/8/5r2/8/7n/2k1q3/8/5N2 w - - 0 1")

        assertEquals(5, board.pieces.size)
        assertEquals(PieceType.KNIGHT, board.getPiece(sq("f1"))!!.type)
        assertEquals(Color.WHITE, board.getPiece(sq("f1"))!!.color)
        assertEquals(PieceType.KING, board.getPiece(sq("c3"))!!.type)
        assertEquals(Color.BLACK, board.getPiece(sq("c3"))!!.color)
        assertEquals(PieceType.ROOK, board.getPiece(sq("f6"))!!.type)
        assertEquals(PieceType.KNIGHT, board.getPiece(sq("h4"))!!.type)
        assertEquals(PieceType.QUEEN, board.getPiece(sq("e3"))!!.type)
    }

    @Test
    fun `cita crnog kao aktivnog igraca`() {
        val (_, activeColor) = FenParser.parseFenToBoard("8/8/8/8/8/8/8/K6k b - - 0 1")
        assertEquals(Color.BLACK, activeColor)
    }

    @Test
    fun `raspored figura prezivljava krug parsiranja i serijalizacije`() {
        val (board, color) = FenParser.parseFenToBoard(startPosition)
        val serialized = FenParser.toFenString(board, color)

        assertEquals(startPosition.substringBefore(' '), serialized.substringBefore(' '))
    }

    @Test
    fun `toFEN na Board klasi daje isti raspored kao FenParser`() {
        val (board, _) = FenParser.parseFenToBoard("8/8/5r2/8/7n/2k1q3/8/5N2 w - - 0 1")
        assertEquals("8/8/5r2/8/7n/2k1q3/8/5N2", board.toFEN().substringBefore(' '))
    }

    @Test
    fun `parseMove razlaze algebarski zapis na dva polja`() {
        val (start, end) = FenParser.parseMove("e2e4")
        assertEquals(sq("e2"), start)
        assertEquals(sq("e4"), end)
    }

    @Test
    fun `parseMove odbija zapis pogresne duzine`() {
        assertThrows(IllegalArgumentException::class.java) { FenParser.parseMove("e2e") }
        assertThrows(IllegalArgumentException::class.java) { FenParser.parseMove("e2e4e5") }
    }

    @Test
    fun `parseMove odbija polje van table`() {
        assertThrows(IllegalArgumentException::class.java) { FenParser.parseMove("e2e9") }
        assertThrows(IllegalArgumentException::class.java) { FenParser.parseMove("z2e4") }
    }

    @Test
    fun `nepoznat karakter figure se odbija`() {
        assertThrows(IllegalArgumentException::class.java) {
            FenParser.parseFenToBoard("8/8/8/8/8/8/8/X7 w - - 0 1")
        }
    }

    @Test
    fun `FEN bez polja za aktivnog igraca se cita kao beli na potezu`() {
        // Ranije je ovakav zapis rusio aplikaciju sa IndexOutOfBoundsException.
        val (board, activeColor) = FenParser.parseFenToBoard("8/8/8/8/8/8/8/K6k")

        assertEquals(Color.WHITE, activeColor)
        assertEquals(2, board.pieces.size)
        assertEquals(PieceType.KING, board.getPiece(sq("a1"))!!.type)
    }
}
