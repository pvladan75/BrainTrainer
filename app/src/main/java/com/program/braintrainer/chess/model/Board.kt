package com.program.braintrainer.chess.model

data class Board(private val pieces: Map<Square, Piece> = emptyMap()) {
    /**
     * Vraća figuru na datom polju, ili null ako nema figure.
     */
    fun getPiece(square: Square): Piece? {
        return pieces[square]
    }

    /**
     * Postavlja figuru na dato polje i vraća novu Board instancu sa izmenjenom figurom.
     */
    fun setPiece(square: Square, piece: Piece?): Board {
        val newPieces = pieces.toMutableMap()
        if (piece == null) {
            newPieces.remove(square)
        } else {
            newPieces[square] = piece
        }
        return Board(newPieces) // Kreira novu instancu Board sa ažuriranom mapom
    }

    /**
     * Primeni potez na tabli i vrati novu Board instancu.
     * Ova metoda *ne proverava* validnost poteza prema šahovskim pravilima.
     * Samo pomera figuru ako je validan *transfer* sa jednog polja na drugo.
     */
    fun applyMove(start: Square, end: Square): Board? {
        val pieceToMove = pieces[start] ?: return null // Nema figure na početnom polju

        val newPieces = pieces.toMutableMap()
        newPieces.remove(start) // Ukloni figuru sa početnog polja
        newPieces[end] = pieceToMove // Postavi figuru na krajnje polje (prepisuje ako je bila neka figura)

        return Board(newPieces)
    }

    /**
     * Proverava da li je potez (start -> end) legalan za figuru na 'start' polju
     * prema osnovnim pravilima kretanja figura i bez provere šaha/mata.
     * Takođe proverava blokade za topove i lovce.
     */
    fun isValidMove(start: Square, end: Square): Boolean {
        val piece = getPiece(start) ?: return false // Nema figure na početnom polju
        val targetPiece = getPiece(end)

        // Ne može se pomeriti na polje gde je sopstvena figura
        if (targetPiece != null && targetPiece.color == piece.color) {
            return false
        }

        // Proveri pravila kretanja za svaku vrstu figure
        return when (piece.type) {
            PieceType.PAWN -> isValidPawnMove(start, end, piece.color, targetPiece != null)
            PieceType.ROOK -> isValidRookMove(start, end)
            PieceType.KNIGHT -> isValidKnightMove(start, end)
            PieceType.BISHOP -> isValidBishopMove(start, end)
            PieceType.QUEEN -> isValidQueenMove(start, end)
            PieceType.KING -> isValidKingMove(start, end)
        }
    }

    // --- Pomoćne funkcije za validaciju kretanja figura ---

    private fun isValidPawnMove(start: Square, end: Square, color: Color, isCapture: Boolean): Boolean {
        val deltaX = end.x - start.x
        val deltaY = end.y - start.y

        val direction = if (color == Color.WHITE) 1 else -1 // Beli ide gore (+Y), Crni ide dole (-Y)

        // Pešak ne može da se kreće horizontalno
        if (deltaX != 0 && !isCapture) {
            return false
        }

        // Normalan potez napred za jedno polje
        if (deltaX == 0 && deltaY == direction && !isCapture) {
            return getPiece(end) == null // Polje mora biti prazno
        }

        // Početni dvostruki potez
        if (deltaX == 0 && deltaY == 2 * direction && ((color == Color.WHITE && start.y == 1) || (color == Color.BLACK && start.y == 6))) {
            // Proveri da li su oba polja ispred pešaka prazna
            val intermediateSquare = Square.fromCoordinates(start.x, start.y + direction)
            return getPiece(end) == null && getPiece(intermediateSquare) == null
        }

        // Hvatanje dijagonalno
        if (Math.abs(deltaX) == 1 && deltaY == direction && isCapture) {
            return getPiece(end) != null && getPiece(end)!!.color != color // Mora postojati protivnička figura
        }
        return false
    }

    private fun isValidRookMove(start: Square, end: Square): Boolean {
        // Mora se kretati samo horizontalno ili vertikalno
        if (start.x != end.x && start.y != end.y) {
            return false
        }

        // Proveri blokade
        if (start.x == end.x) { // Vertikalno kretanje
            val startY = start.y
            val endY = end.y
            if (startY < endY) {
                for (y in (startY + 1) until endY) { // Idi gore
                    if (getPiece(Square.fromCoordinates(start.x, y)) != null) {
                        return false // Blokirano
                    }
                }
            } else { // startY > endY, Idi dole
                for (y in (startY - 1) downTo (endY + 1)) {
                    if (getPiece(Square.fromCoordinates(start.x, y)) != null) {
                        return false // Blokirano
                    }
                }
            }
        } else { // Horizontalno kretanje (start.y == end.y)
            val startX = start.x
            val endX = end.x
            if (startX < endX) {
                for (x in (startX + 1) until endX) { // Idi desno
                    if (getPiece(Square.fromCoordinates(x, start.y)) != null) {
                        return false // Blokirano
                    }
                }
            } else { // startX > endX, Idi levo
                for (x in (startX - 1) downTo (endX + 1)) {
                    if (getPiece(Square.fromCoordinates(x, start.y)) != null) {
                        return false // Blokirano
                    }
                }
            }
        }
        return true
    }

    private fun isValidKnightMove(start: Square, end: Square): Boolean {
        val deltaX = Math.abs(end.x - start.x)
        val deltaY = Math.abs(end.y - start.y)
        // L-oblik kretanja (2x1 ili 1x2)
        return (deltaX == 1 && deltaY == 2) || (deltaX == 2 && deltaY == 1)
    }

    private fun isValidBishopMove(start: Square, end: Square): Boolean {
        // Mora se kretati samo dijagonalno
        if (Math.abs(end.x - start.x) != Math.abs(end.y - start.y)) {
            return false
        }

        // Proveri blokade
        val stepX = if (end.x > start.x) 1 else -1
        val stepY = if (end.y > start.y) 1 else -1

        var currentX = start.x + stepX
        var currentY = start.y + stepY

        // Loop while not at the destination square
        while (currentX != end.x && currentY != end.y) {
            if (getPiece(Square.fromCoordinates(currentX, currentY)) != null) {
                return false // Blokirano
            }
            currentX += stepX
            currentY += stepY
        }
        return true
    }

    private fun isValidQueenMove(start: Square, end: Square): Boolean {
        // Kraljica je kombinacija topa i lovca
        return isValidRookMove(start, end) || isValidBishopMove(start, end)
    }

    private fun isValidKingMove(start: Square, end: Square): Boolean {
        val deltaX = Math.abs(end.x - start.x)
        val deltaY = Math.abs(end.y - start.y)
        // Kralj se kreće samo jedno polje u bilo kom smeru
        return deltaX <= 1 && deltaY <= 1
    }
}