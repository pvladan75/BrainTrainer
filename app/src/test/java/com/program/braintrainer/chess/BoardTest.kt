package com.program.braintrainer.chess

import com.program.braintrainer.chess.model.Board
import com.program.braintrainer.chess.model.Color
import com.program.braintrainer.chess.model.Piece
import com.program.braintrainer.chess.model.PieceType
import com.program.braintrainer.chess.model.Square
import com.program.braintrainer.chess.parser.FenParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tabla iz FEN zapisa - čitljivije od ručnog sastavljanja mape figura. */
private fun board(fen: String): Board = FenParser.parseFenToBoard(fen).first

private fun s(name: String) = Square(name[0], name[1].digitToInt())

class BoardTest {

    // ---------- kretanje figura ----------

    @Test
    fun `top se krece po redu i koloni`() {
        val b = board("8/8/8/8/8/8/8/R7 w - - 0 1")
        assertTrue(b.isValidMove(s("a1"), s("a8")))
        assertTrue(b.isValidMove(s("a1"), s("h1")))
        assertFalse(b.isValidMove(s("a1"), s("b2")))
    }

    @Test
    fun `top ne preskace figuru`() {
        // Beli top a1, beli pešak a3 - a4 je iza pešaka
        val b = board("8/8/8/8/8/P7/8/R7 w - - 0 1")
        assertTrue(b.isValidMove(s("a1"), s("a2")))
        assertFalse(b.isValidMove(s("a1"), s("a4")))
    }

    @Test
    fun `lovac se krece dijagonalno`() {
        val b = board("8/8/8/8/8/8/8/B7 w - - 0 1")
        assertTrue(b.isValidMove(s("a1"), s("h8")))
        assertFalse(b.isValidMove(s("a1"), s("a5")))
    }

    @Test
    fun `lovac ne preskace figuru`() {
        // Beli lovac a1, crni pešak c3 - d4 je iza njega
        val b = board("8/8/8/8/8/2p5/8/B7 w - - 0 1")
        assertTrue(b.isValidMove(s("a1"), s("c3")))
        assertFalse(b.isValidMove(s("a1"), s("d4")))
    }

    @Test
    fun `skakac preskace figure`() {
        val b = board("8/8/8/8/8/8/PPPPPPPP/1N6 w - - 0 1")
        assertTrue(b.isValidMove(s("b1"), s("a3")))
        assertTrue(b.isValidMove(s("b1"), s("c3")))
        assertFalse(b.isValidMove(s("b1"), s("b3")))
    }

    @Test
    fun `kraljica kombinuje topa i lovca`() {
        val b = board("8/8/8/3Q4/8/8/8/8 w - - 0 1")
        assertTrue(b.isValidMove(s("d5"), s("d1")))
        assertTrue(b.isValidMove(s("d5"), s("h5")))
        assertTrue(b.isValidMove(s("d5"), s("h1")))
        assertFalse(b.isValidMove(s("d5"), s("e3")))
    }

    @Test
    fun `kralj se krece samo jedno polje`() {
        val b = board("8/8/8/3K4/8/8/8/8 w - - 0 1")
        assertTrue(b.isValidMove(s("d5"), s("d6")))
        assertTrue(b.isValidMove(s("d5"), s("e6")))
        assertFalse(b.isValidMove(s("d5"), s("d7")))
    }

    // ---------- pešak ----------

    @Test
    fun `pesak ide jedno polje napred`() {
        val b = board("8/8/8/8/8/8/4P3/8 w - - 0 1")
        assertTrue(b.isValidMove(s("e2"), s("e3")))
    }

    @Test
    fun `pesak moze dva polja sa pocetne pozicije`() {
        val b = board("8/8/8/8/8/8/4P3/8 w - - 0 1")
        assertTrue(b.isValidMove(s("e2"), s("e4")))
    }

    @Test
    fun `pesak ne moze dva polja van pocetne pozicije`() {
        val b = board("8/8/8/8/8/4P3/8/8 w - - 0 1")
        assertFalse(b.isValidMove(s("e3"), s("e5")))
    }

    @Test
    fun `pesak ne moze dva polja preko prepreke`() {
        val b = board("8/8/8/8/8/4p3/4P3/8 w - - 0 1")
        assertFalse(b.isValidMove(s("e2"), s("e4")))
    }

    @Test
    fun `pesak ne uzima pravolinijski`() {
        val b = board("8/8/8/8/8/4p3/4P3/8 w - - 0 1")
        assertFalse(b.isValidMove(s("e2"), s("e3")))
    }

    @Test
    fun `pesak uzima dijagonalno`() {
        val b = board("8/8/8/8/8/3p4/4P3/8 w - - 0 1")
        assertTrue(b.isValidMove(s("e2"), s("d3")))
    }

    @Test
    fun `pesak ne ide unazad`() {
        val b = board("8/8/8/8/8/4P3/8/8 w - - 0 1")
        assertFalse(b.isValidMove(s("e3"), s("e2")))
    }

    @Test
    fun `crni pesak se krece u suprotnom smeru`() {
        val b = board("8/4p3/8/8/8/8/8/8 w - - 0 1")
        assertTrue(b.isValidMove(s("e7"), s("e6")))
        assertTrue(b.isValidMove(s("e7"), s("e5")))
        assertFalse(b.isValidMove(s("e7"), s("e8")))
    }

    // ---------- osnovna pravila ----------

    @Test
    fun `ne moze se uzeti sopstvena figura`() {
        val b = board("8/8/8/8/8/8/P7/R7 w - - 0 1")
        assertFalse(b.isValidMove(s("a1"), s("a2")))
    }

    @Test
    fun `potez sa praznog polja nije validan`() {
        val b = board("8/8/8/8/8/8/8/R7 w - - 0 1")
        assertFalse(b.isValidMove(s("d4"), s("d5")))
        assertNull(b.applyMove(s("d4"), s("d5")))
    }

    // ---------- šah i vezivanje ----------

    @Test
    fun `isKingInCheck prepoznaje napad na kralja`() {
        val b = board("4r3/8/8/8/8/8/8/4K3 w - - 0 1")
        assertTrue(b.isKingInCheck(Color.WHITE))
        assertFalse(b.isKingInCheck(Color.BLACK))
    }

    @Test
    fun `potez koji otvara kralja je nelegalan`() {
        // Beli top e2 je vezan: iza njega kralj e1, ispred crni top e8
        val b = board("4r3/8/8/8/8/8/4R3/4K3 w - - 0 1")
        assertFalse(b.isValidMove(s("e2"), s("a2")))
    }

    @Test
    fun `potez duz linije vezivanja ostaje legalan`() {
        val b = board("4r3/8/8/8/8/8/4R3/4K3 w - - 0 1")
        assertTrue(b.isValidMove(s("e2"), s("e3")))
        assertTrue(b.isValidMove(s("e2"), s("e8")))
    }

    // ---------- napadnuta polja (osnova za Modul 2 i 3) ----------

    @Test
    fun `napadnuta polja ukljucuju i sopstvene branjene figure`() {
        val b = board("8/8/8/8/8/8/4R3/4K3 w - - 0 1")
        assertTrue(b.getAttackedSquares(Color.WHITE).contains(s("e1")))
    }

    @Test
    fun `klizeca figura napada do prve prepreke ukljucujuci je`() {
        val b = board("8/8/8/8/8/P7/8/R7 w - - 0 1")
        val attacked = b.getAttackedSquares(Color.WHITE)
        assertTrue(attacked.contains(s("a2")))
        assertTrue(attacked.contains(s("a3")))
        assertFalse(attacked.contains(s("a4")))
    }

    @Test
    fun `pesak napada dijagonalno a ne polje ispred sebe`() {
        val b = board("8/8/8/8/8/8/4P3/8 w - - 0 1")
        val attacked = b.getAttackedSquares(Color.WHITE)
        assertTrue(attacked.contains(s("d3")))
        assertTrue(attacked.contains(s("f3")))
        assertFalse(attacked.contains(s("e3")))
    }

    // ---------- stanje table ----------

    @Test
    fun `applyMove vraca novu tablu i ne menja original`() {
        val b = board("8/8/8/8/8/8/8/R7 w - - 0 1")
        val after = b.applyMove(s("a1"), s("a5"))

        assertNotNull(after)
        assertNull(after!!.getPiece(s("a1")))
        assertEquals(PieceType.ROOK, after.getPiece(s("a5"))!!.type)
        assertEquals(PieceType.ROOK, b.getPiece(s("a1"))!!.type)
        assertNull(b.getPiece(s("a5")))
    }

    @Test
    fun `uzimanje uklanja protivnicku figuru`() {
        val b = board("8/8/8/8/8/2p5/8/B7 w - - 0 1")
        val after = b.applyMove(s("a1"), s("c3"))!!

        assertEquals(1, after.pieces.size)
        assertEquals(Piece(PieceType.BISHOP, Color.WHITE), after.getPiece(s("c3")))
        assertFalse(after.hasBlackPiecesRemaining())
    }

    @Test
    fun `hasBlackPiecesRemaining prati crne figure`() {
        assertTrue(board("8/8/8/8/8/2p5/8/B7 w - - 0 1").hasBlackPiecesRemaining())
        assertFalse(board("8/8/8/8/8/8/8/B7 w - - 0 1").hasBlackPiecesRemaining())
    }

    @Test
    fun `hasAnyLegalCaptureMove razlikuje uzimanje od obicnog poteza`() {
        assertTrue(board("8/8/8/8/8/2p5/8/B7 w - - 0 1").hasAnyLegalCaptureMove(Color.WHITE))
        assertFalse(board("8/8/8/8/8/3p4/8/B7 w - - 0 1").hasAnyLegalCaptureMove(Color.WHITE))
    }

    @Test
    fun `hasAnyLegalMove prepoznaje stranu bez figura`() {
        assertTrue(board("8/8/8/8/8/8/8/R7 w - - 0 1").hasAnyLegalMove(Color.WHITE))
        assertFalse(board("8/8/8/8/8/8/8/R7 w - - 0 1").hasAnyLegalMove(Color.BLACK))
    }

    @Test
    fun `getLegalMoves daje sva odredista skakaca u uglu`() {
        val b = board("8/8/8/8/8/8/8/N7 w - - 0 1")
        val moves = b.getLegalMoves(s("a1")).toSet()
        assertEquals(setOf(s("b3"), s("c2")), moves)
    }

    @Test
    fun `prazna tabla nema legalnih poteza`() {
        val b = Board()
        assertFalse(b.hasAnyLegalMove(Color.WHITE))
        assertTrue(b.getLegalMoves(s("e4")).isEmpty())
    }

    // ---------- poznato ograničenje ----------

    @Test
    fun `promocija pesaka nije implementirana`() {
        // Dokumentuje trenutno ponašanje: pešak koji stigne na 8. red ostaje pešak.
        // Ako se promocija ikada doda, ovaj test treba da padne i da se ažurira.
        val b = board("8/4P3/8/8/8/8/8/8 w - - 0 1")
        val after = b.applyMove(s("e7"), s("e8"))!!
        assertEquals(PieceType.PAWN, after.getPiece(s("e8"))!!.type)
    }
}
