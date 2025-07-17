package com.program.braintrainer.chess.model

import kotlin.math.abs

data class Board(val pieces: Map<Square, Piece> = emptyMap()) {
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
        // Ukloni figuru sa početnog polja
        newPieces.remove(start)
        // Postavi figuru na krajnje polje (prepisuje ako je bila neka figura)
        newPieces[end] = pieceToMove

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
        val isValidBasicMove = when (piece.type) {
            PieceType.PAWN -> isValidPawnMove(start, end, piece.color, targetPiece != null)
            PieceType.ROOK -> isValidRookMove(start, end)
            PieceType.KNIGHT -> isValidKnightMove(start, end)
            PieceType.BISHOP -> isValidBishopMove(start, end)
            PieceType.QUEEN -> isValidQueenMove(start, end)
            PieceType.KING -> isValidKingMove(start, end)
        }

        if (!isValidBasicMove) return false

        // Dodatna provera: da li ovaj potez ostavlja sopstvenog kralja u šahu?
        val tempBoard = this.applyMove(start, end)
        // Ako tempBoard može biti null (npr. ako nema figure na startu, što je već provereno),
        // ali pošto smo već proverili pieceToMove, tempBoard ne bi trebalo biti null ovde.
        // Ipak, dobra praksa je da se proveri nullability.
        return tempBoard != null && !tempBoard.isKingInCheck(piece.color)
    }

    /**
     * Vraća listu svih legalnih polja na koja data figura može da se pomeri.
     * Uključuje proveru šaha.
     */
    fun getLegalMoves(start: Square): List<Square> {
        val piece = getPiece(start) ?: return emptyList()
        val possibleMoves = mutableListOf<Square>()

        // Iteriramo kroz sva polja na tabli i proveravamo da li je potez validan za tu figuru
        for (x in 0..7) {
            for (y in 0..7) {
                val end = Square.fromCoordinates(x, y)
                if (start == end) continue // Ne može se pomeriti na isto polje

                // Ako je potez validan prema pravilima kretanja figure i ne rezultira šahom
                if (isValidMove(start, end)) { // isValidMove sada uključuje proveru šaha
                    possibleMoves.add(end)
                }
            }
        }
        return possibleMoves
    }

    /**
     * Proverava da li bilo koja figura date boje ima legalan potez koji rezultira uzimanjem.
     */
    fun hasAnyLegalCaptureMove(color: Color): Boolean {
        for (startSquareEntry in pieces) {
            val start = startSquareEntry.key
            val piece = startSquareEntry.value

            if (piece.color == color) {
                // Proveri sva moguća legalna odredišta za ovu figuru
                for (end in getLegalMoves(start)) { // Koristimo getLegalMoves koje proverava šah
                    val targetPiece = getPiece(end)
                    // Ako na odredištu postoji protivnička figura, to je potez uzimanja
                    if (targetPiece != null && targetPiece.color != color) {
                        return true // Pronašli smo bar jedan potez uzimanja
                    }
                }
            }
        }
        return false
    }

    /**
     * Proverava da li na tabli postoje crne figure.
     */
    fun hasBlackPiecesRemaining(): Boolean {
        return pieces.any { it.value.color == Color.BLACK }
    }

    /**
     * Proverava da li data boja ima bilo kakvih legalnih poteza.
     */
    fun hasAnyLegalMove(color: Color): Boolean {
        for ((startSquare, piece) in pieces) {
            if (piece.color == color) {
                if (getLegalMoves(startSquare).isNotEmpty()) {
                    return true
                }
            }
        }
        return false
    }

    // --- NOVA FUNKCIJA: Pronalazi sva polja koja su napadnuta od figura određene boje ---
    fun getAttackedSquares(attackingColor: Color): Set<Square> {
        val attackedSquares = mutableSetOf<Square>()
        for ((square, piece) in pieces) {
            if (piece.color == attackingColor) {
                // Za svaku figuru, simuliraj sve POTENCIJALNE mete napada
                // (bez obzira na to da li je na meti prijateljska figura)
                val potentialTargets = when (piece.type) {
                    PieceType.PAWN -> getPawnAttackTargets(square, attackingColor)
                    // Knights don't have color-specific attack targets in terms of movement,
                    // but we pass attackingColor to getAttackedSquares for consistency.
                    PieceType.KNIGHT -> getKnightAttackTargets(square)
                    PieceType.BISHOP -> getSlidingAttackTargets(square, BishopDirections)
                    PieceType.ROOK -> getSlidingAttackTargets(square, RookDirections)
                    PieceType.QUEEN -> getSlidingAttackTargets(square, QueenDirections)
                    PieceType.KING -> getKingAttackTargets(square)
                }
                attackedSquares.addAll(potentialTargets)
            }
        }
        return attackedSquares
    }

    // --- NOVA FUNKCIJA: Proverava da li je kralj date boje u šahu ---
    fun isKingInCheck(kingColor: Color): Boolean {
        // Pronađi poziciju kralja date boje
        val kingSquare = pieces.entries.find { it.value == Piece(PieceType.KING, kingColor) }?.key
            ?: return false // Ako kralj ne postoji, ne može biti u šahu

        val opponentColor = kingColor.opposite()
        val attackedByOpponent = getAttackedSquares(opponentColor) // Polja napadnuta od strane protivnika
        return attackedByOpponent.contains(kingSquare) // Da li je kraljevo polje među napadnutim?
    }

    // --- Pomoćne funkcije za generisanje POTENCIJALNIH meta napada (za getAttackedSquares) ---
    // OVE FUNKCIJE SU DOBIJE GLAVNE IZMENE!
    private fun getPawnAttackTargets(start: Square, color: Color): List<Square> {
        val targets = mutableListOf<Square>()
        val direction = if (color == Color.WHITE) 1 else -1

        // Levi dijagonalni napad
        val targetXLeft = start.x - 1
        val targetYLeft = start.y + direction
        if (targetXLeft in 0..7 && targetYLeft in 0..7) { // PROVERA GRANICA
            targets.add(Square.fromCoordinates(targetXLeft, targetYLeft))
        }

        // Desni dijagonalni napad
        val targetXRight = start.x + 1
        val targetYRight = start.y + direction
        if (targetXRight in 0..7 && targetYRight in 0..7) { // PROVERA GRANICA
            targets.add(Square.fromCoordinates(targetXRight, targetYRight))
        }
        return targets
    }

    private fun getKnightAttackTargets(start: Square): List<Square> {
        val targets = mutableListOf<Square>()
        val knightMoves = listOf(
            Pair(2, 1), Pair(2, -1), Pair(-2, 1), Pair(-2, -1),
            Pair(1, 2), Pair(1, -2), Pair(-1, 2), Pair(-1, -2)
        )
        for ((dx, dy) in knightMoves) {
            val targetX = start.x + dx
            val targetY = start.y + dy
            if (targetX in 0..7 && targetY in 0..7) { // PROVERA GRANICA
                targets.add(Square.fromCoordinates(targetX, targetY))
            }
        }
        return targets
    }

    private fun getKingAttackTargets(start: Square): List<Square> {
        val targets = mutableListOf<Square>()
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue // Kralj se ne kreće na isto polje
                val targetX = start.x + dx
                val targetY = start.y + dy
                if (targetX in 0..7 && targetY in 0..7) { // PROVERA GRANICA
                    targets.add(Square.fromCoordinates(targetX, targetY))
                }
            }
        }
        return targets
    }

    // Pomoćne varijable za smerove kretanja klizećih figura
    private val BishopDirections = listOf(Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1))
    private val RookDirections = listOf(Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0))
    private val QueenDirections = BishopDirections + RookDirections

    private fun getSlidingAttackTargets(start: Square, directions: List<Pair<Int, Int>>): List<Square> {
        val targets = mutableListOf<Square>()
        for ((dx, dy) in directions) {
            var currentX = start.x
            var currentY = start.y
            while (true) {
                currentX += dx
                currentY += dy
                if (currentX !in 0..7 || currentY !in 0..7) break // PROVERA GRANICA

                val currentSquare = Square.fromCoordinates(currentX, currentY)
                targets.add(currentSquare)
                // Ako naiđemo na figuru (bilo koju), zaustavljamo se jer blokira liniju napada
                if (getPiece(currentSquare) != null) break
            }
        }
        return targets
    }

    // --- Postojeće pomoćne funkcije za validaciju kretanja figura (nepromenjene u logici kretanja) ---
    // (Ove funkcije su ostale iste, samo je isValidMove sada koristi i za proveru šaha)

    private fun isValidPawnMove(start: Square, end: Square, color: Color, isCapture: Boolean): Boolean {
        val deltaX = end.x - start.x
        val deltaY = end.y - start.y

        val direction = if (color == Color.WHITE) 1 else -1 // Beli ide gore (+Y), Crni ide dole (-Y)

        // Pešak ne može da se kreće horizontalno osim kod uzimanja
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
            val intermediateX = start.x
            val intermediateY = start.y + direction
            // Dodata provera granica za intermediateSquare, iako bi trebalo da bude unutar granica
            // s obzirom na start.y uslove (1 i 6). Ipak, radi robusnosti.
            if (intermediateX !in 0..7 || intermediateY !in 0..7) return false // Ovo bi trebalo da bude redundantno, ali za svaki slučaj
            val intermediateSquare = Square.fromCoordinates(intermediateX, intermediateY)
            return getPiece(end) == null && getPiece(intermediateSquare) == null
        }

        // Hvatanje dijagonalno
        if (abs(deltaX) == 1 && deltaY == direction && isCapture) {
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
            val stepY = if (start.y < end.y) 1 else -1
            var currentY = start.y + stepY
            while (currentY != end.y) {
                // Dodata provera granica, iako bi fromCoordinates to već uhvatio.
                // Ali ovo je bolje jer izbegava bacanje izuzetka.
                if (start.x !in 0..7 || currentY !in 0..7) return false
                if (getPiece(Square.fromCoordinates(start.x, currentY)) != null) {
                    return false // Blokirano
                }
                currentY += stepY
            }
        } else { // Horizontalno kretanje (start.y == end.y)
            val stepX = if (start.x < end.x) 1 else -1
            var currentX = start.x + stepX
            while (currentX != end.x) {
                // Dodata provera granica
                if (currentX !in 0..7 || start.y !in 0..7) return false
                if (getPiece(Square.fromCoordinates(currentX, start.y)) != null) {
                    return false // Blokirano
                }
                currentX += stepX
            }
        }
        return true
    }

    private fun isValidKnightMove(start: Square, end: Square): Boolean {
        val deltaX = abs(end.x - start.x)
        val deltaY = abs(end.y - start.y)
        // L-oblik kretanja (2x1 ili 1x2)
        return (deltaX == 1 && deltaY == 2) || (deltaX == 2 && deltaY == 1)
    }

    private fun isValidBishopMove(start: Square, end: Square): Boolean {
        // Mora se kretati samo dijagonalno
        if (abs(end.x - start.x) != abs(end.y - start.y)) {
            return false
        }

        // Proveri blokade
        val stepX = if (end.x > start.x) 1 else -1
        val stepY = if (end.y > start.y) 1 else -1

        var currentX = start.x + stepX
        var currentY = start.y + stepY

        // Loop dok ne dođemo do krajnjeg polja
        while (currentX != end.x && currentY != end.y) {
            // Dodata provera granica
            if (currentX !in 0..7 || currentY !in 0..7) return false
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
        val deltaX = abs(end.x - start.x)
        val deltaY = abs(end.y - start.y)
        // Kralj se kreće samo jedno polje u bilo kom smeru
        return deltaX <= 1 && deltaY <= 1
    }
}