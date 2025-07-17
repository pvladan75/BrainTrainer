package com.program.braintrainer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.program.braintrainer.chess.model.Board
import com.program.braintrainer.chess.model.Color as ChessColor
import com.program.braintrainer.chess.model.Piece
import com.program.braintrainer.chess.model.PieceType
import com.program.braintrainer.chess.model.Square
import com.program.braintrainer.chess.parser.FenParser
import com.program.braintrainer.ui.theme.BrainTrainerTheme
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.program.braintrainer.R
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module
import com.program.braintrainer.chess.data.ProblemLoader
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay


@Composable
fun ChessScreen(
    module: Module,
    difficulty: Difficulty,
    onGameFinished: () -> Unit
) {
    val context = LocalContext.current
    val problemLoader = remember { ProblemLoader(context) }

    val problems = remember(module, difficulty) {
        problemLoader.loadProblemsForModuleAndDifficulty(module, difficulty)
    }

    var currentProblemIndex by remember { mutableStateOf(0) }
    var currentProblem by remember { mutableStateOf<com.program.braintrainer.chess.model.Problem?>(null) }
    var currentBoard by remember { mutableStateOf(Board()) }
    val activePlayerColor = ChessColor.WHITE // Beli je uvek na potezu

    var selectedSquare by remember { mutableStateOf<Square?>(null) }
    var showSolutionPath by remember { mutableStateOf(false) } // Kontroliše vidljivost dugmića za rešenje

    // Novo stanje za navigaciju kroz rešenje
    var solutionMoveIndex by remember { mutableStateOf(-1) } // -1 znači da smo na početnoj poziciji zagonetke
    var isPlayingSolution by remember { mutableStateOf(false) } // Da li je play mod aktivan
    var usedSolution by remember { mutableStateOf(false) } // Da li je korisnik koristio opciju prikaza rešenja

    var showGameResultDialog by remember { mutableStateOf(false) }
    var gameResultMessage by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()


    LaunchedEffect(currentProblemIndex, problems) {
        if (problems.isNotEmpty()) {
            val problem = problems[currentProblemIndex]
            currentProblem = problem
            val (board, _) = FenParser.parseFenToBoard(problem.fen)
            currentBoard = board
            selectedSquare = null // Resetuj selekciju pri učitavanju nove zagonetke
            showSolutionPath = false // Uvek počni sa skrivenim dugmićima za rešenje
            solutionMoveIndex = -1 // Resetuj indeks poteza rešenja
            isPlayingSolution = false // Resetuj play mod
            usedSolution = false // Resetuj zastavicu korišćenja rešenja za novu zagonetku
            showGameResultDialog = false // Reset dialog state
            gameResultMessage = "" // Reset message
        } else {
            currentProblem = null
            currentBoard = Board()
            selectedSquare = null
            showSolutionPath = false
            solutionMoveIndex = -1
            isPlayingSolution = false
            usedSolution = false
            showGameResultDialog = false
            gameResultMessage = ""
        }
    }

    // LaunchedEffect za automatsko puštanje rešenja
    LaunchedEffect(isPlayingSolution) {
        if (isPlayingSolution) {
            val solutionMoves = currentProblem?.solution?.moves
            if (solutionMoves != null && solutionMoves.isNotEmpty()) {
                // Ako je tek pokrenut play, kreni od trenutnog solutionMoveIndex-a
                // ili od 0 ako je -1 (početna pozicija)
                val startFromIndex = if (solutionMoveIndex == -1) 0 else solutionMoveIndex

                for (i in startFromIndex until solutionMoves.size) {
                    // Proveri da li je play i dalje aktivan i da li smo na dobrom indeksu
                    if (!isPlayingSolution || i != solutionMoveIndex) {
                        break
                    }
                    val move = solutionMoves[i]
                    val (start, end) = FenParser.parseMove(move)
                    val newBoard = currentBoard.applyMove(start, end)
                    if (newBoard != null) {
                        currentBoard = newBoard
                        solutionMoveIndex = i + 1 // Pomeri na sledeći indeks za prikaz
                    }
                    delay(1000L) // Pauza od 1 sekunde između poteza
                }
                isPlayingSolution = false // Zaustavi play kada se završi
                // Postavi solutionMoveIndex na poslednji potez rešenja ako je Play završio
                if (solutionMoveIndex > 0 && solutionMoveIndex == solutionMoves.size) {
                    solutionMoveIndex = solutionMoves.size -1
                }
            } else {
                isPlayingSolution = false // Nema rešenja, zaustavi play
            }
        }
    }


    val problemsInSession = remember(problems) {
        if (problems.size >= 10) problems.take(10) else problems
    }
    val currentSessionProblemIndex = currentProblemIndex % problemsInSession.size

    fun checkGameStatus() {
        if (module == Module.Module1) {
            if (!currentBoard.hasBlackPiecesRemaining()) {
                // SVE CRNE FIGURE POJEDENE - POBEDA U MODULU 1
                gameResultMessage = "Čestitamo! Sve crne figure su pojedene. Zagonetka rešena!"
                if (usedSolution) {
                    gameResultMessage += "\n(Niste osvojili poene jer ste koristili rešenje.)"
                }
                showGameResultDialog = true
            } else if (!currentBoard.hasAnyLegalCaptureMove(activePlayerColor)) {
                // IMA CRNIH FIGURA, A NEMA MOGUĆNOSTI UZIMANJA - PROMAŠAJ
                gameResultMessage = "Promašaj! Nema više legalnih poteza uzimanja, a ostale su crne figure."
                gameResultMessage += "\n(Niste osvojili poene jer zagonetka nije rešena.)" // Nije bitno da li je korišteno rešenje, jer nije rešeno
                showGameResultDialog = true
            }
        }
        // Za ostale module (2 i 3) nema specifičnih uslova pobede/poraza na osnovu uzimanja
        // Njihova logika pobede/poraza će biti definisana kasnije (npr. mat, osvojen broj poena itd.)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Mod: ${module.title}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Težina: ${difficulty.label}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Vreme: 00:00",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (problemsInSession.isNotEmpty()) "Zagonetka: ${currentSessionProblemIndex + 1}/${problemsInSession.size}" else "Nema zagonetki",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Na potezu: Beli",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            currentProblem?.let {
                Text(
                    text = "Cilj: ${it.description}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        ChessBoardComposable(
            board = currentBoard,
            selectedSquare = selectedSquare,
            solutionPath = if (showSolutionPath) currentProblem?.solution?.moves else null, // I dalje šaljemo putanju za highlight
            onSquareClick = { clickedSquare ->
                // Ako je prikaz rešenja aktivan, onemogući interakciju igrača sa tablom
                if (showSolutionPath) {
                    coroutineScope.launch { snackbarHostState.showSnackbar("Prikaz rešenja je aktivan. Pritisnite 'Sakrij rešenje' za igranje.") }
                    return@ChessBoardComposable
                }

                val pieceOnClickedSquare = currentBoard.getPiece(clickedSquare)

                if (selectedSquare == null) {
                    // Prvi klik: Selektujemo belu figuru (ako postoji)
                    if (pieceOnClickedSquare != null && pieceOnClickedSquare.color == activePlayerColor) {
                        selectedSquare = clickedSquare
                    }
                } else {
                    // Drugi klik: Povezujemo sa prethodno selektovanom figurom
                    val startSquare = selectedSquare!!
                    val endSquare = clickedSquare

                    // Slučaj 1: Klik na istu belu figuru (ništa se ne menja, ostaje selektovana)
                    if (startSquare == endSquare) {
                        // selectedSquare ostaje nepromenjen
                        return@ChessBoardComposable
                    }

                    // Slučaj 2: Klik na drugu belu figuru (prebacujemo selekciju)
                    if (pieceOnClickedSquare != null && pieceOnClickedSquare.color == activePlayerColor) {
                        selectedSquare = clickedSquare
                        return@ChessBoardComposable
                    }

                    // Slučaj 3: Pokušaj poteza (na prazno polje ili na protivničku figuru)
                    val isCapture = pieceOnClickedSquare != null && pieceOnClickedSquare.color != activePlayerColor

                    // Pravilo specifično za Modul 1: Potez mora biti uzimanje
                    if (module == Module.Module1 && !isCapture) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("U modulu 'Čišćenje table' svaki potez mora biti uzimanje!")
                        }
                        // selectedSquare ostaje selektovan jer potez nije validan za ovaj modul
                        return@ChessBoardComposable
                    }

                    // Provera da li je potez legalan po pravilima kretanja figura
                    if (currentBoard.isValidMove(startSquare, endSquare)) {
                        val newBoard = currentBoard.applyMove(startSquare, endSquare)
                        if (newBoard != null) {
                            currentBoard = newBoard
                            println("Potez ${startSquare.toString()}${endSquare.toString()} odigran.")
                            selectedSquare = endSquare // Figura ostaje selektovana na novoj poziciji

                            // Proveri status igre nakon poteza
                            checkGameStatus()

                        } else {
                            // Ovo bi se trebalo retko desiti ako je isValidMove istinit
                            println("Nevažeći potez (applyMove vratio null) ili nepoznat razlog).")
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Greška pri izvršavanju poteza.")
                            }
                            // selectedSquare ostaje selektovan jer potez nije uspešno primenjen
                        }
                    } else {
                        println("Nevažeći potez (prema pravilima kretanja figure).")
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Ne možete pomeriti figuru tako!")
                        }
                        // selectedSquare ostaje selektovan jer potez nije validan po pravilima šaha
                    }
                }
            }
        )

        // PRVI RED DUGMIĆA
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    showSolutionPath = !showSolutionPath
                    // Kada se prikaže rešenje
                    if (showSolutionPath) {
                        usedSolution = true // Postavi zastavicu da je rešenje korišćeno
                        currentProblem?.let {
                            val (initialBoard, _) = FenParser.parseFenToBoard(it.fen)
                            currentBoard = initialBoard
                            selectedSquare = null
                            solutionMoveIndex = -1 // Postavi na početnu poziciju rešenja
                            isPlayingSolution = false // Zaustavi play mod
                        }
                    } else {
                        // Kada se sakrije rešenje
                        // Resetuj na početnu poziciju zagonetke
                        currentProblem?.let {
                            val (initialBoard, _) = FenParser.parseFenToBoard(it.fen)
                            currentBoard = initialBoard
                            selectedSquare = null
                            solutionMoveIndex = -1
                            isPlayingSolution = false
                        }
                    }
                },
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                Text(if (showSolutionPath) "Sakrij rešenje" else "Prikaži rešenje")
            }

            Button(
                onClick = {
                    // Logika za prelazak na sledeću zagonetku
                    if (problems.isNotEmpty() && currentProblemIndex < problems.size - 1 && currentSessionProblemIndex < problemsInSession.size - 1) {
                        currentProblemIndex++
                    } else {
                        onGameFinished() // Završi sesiju ako nema više zagonetki
                    }
                },
                enabled = problemsInSession.isNotEmpty() && currentSessionProblemIndex < problemsInSession.size - 1,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            ) {
                Text("Sledeća zagonetka")
            }
        }

        // DRUGI RED DUGMIĆA (vidljiv samo kada je showSolutionPath == true)
        if (showSolutionPath) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        isPlayingSolution = false // Zaustavi play mod
                        val solutionMoves = currentProblem?.solution?.moves
                        if (solutionMoves != null && solutionMoveIndex > 0) { // Promenjeno na > 0 da ostane na -1
                            solutionMoveIndex-- // Idi na prethodni potez
                            // Rekonstruiši tablu do ovog poteza
                            val (initialBoard, _) = FenParser.parseFenToBoard(currentProblem!!.fen)
                            var tempBoard = initialBoard
                            for (i in 0 until solutionMoveIndex) { // Petlja ide do solutionMoveIndex - 1
                                val move = solutionMoves[i]
                                val (start, end) = FenParser.parseMove(move)
                                tempBoard = tempBoard.applyMove(start, end) ?: tempBoard
                            }
                            currentBoard = tempBoard
                        } else if (solutionMoveIndex == 0) { // Ako je na prvom potezu, vrati na -1 (početna pozicija)
                            val (initialBoard, _) = FenParser.parseFenToBoard(currentProblem!!.fen)
                            currentBoard = initialBoard
                            selectedSquare = null
                            solutionMoveIndex = -1
                        }
                    },
                    // Omogući samo ako postoji rešenje i nismo već na početnoj poziciji
                    enabled = currentProblem != null && currentProblem?.solution?.moves?.isNotEmpty() == true && solutionMoveIndex > -1,
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                ) {
                    Text("Prethodni potez")
                }

                Button(
                    onClick = {
                        isPlayingSolution = !isPlayingSolution // Prebacivanje play/pauza
                        // Ako se pokreće play i nismo na početku, postavi index na početak rešenja
                        if (isPlayingSolution && solutionMoveIndex == -1) {
                            currentProblem?.let {
                                val (initialBoard, _) = FenParser.parseFenToBoard(it.fen)
                                currentBoard = initialBoard
                                selectedSquare = null
                                solutionMoveIndex = 0 // Kreni od prvog poteza
                            }
                        }
                    },
                    // Omogući samo ako postoji rešenje i nismo na kraju rešenja
                    enabled = currentProblem != null && currentProblem?.solution?.moves?.isNotEmpty() == true && solutionMoveIndex < (currentProblem?.solution?.moves?.size ?: 0),
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                ) {
                    Text(if (isPlayingSolution) "Pauza" else "Play")
                }

                Button(
                    onClick = {
                        isPlayingSolution = false // Zaustavi play mod
                        val solutionMoves = currentProblem?.solution?.moves
                        if (solutionMoves != null) {
                            // Ako smo na početnoj poziciji (-1), kreni od 0
                            if (solutionMoveIndex == -1 && solutionMoves.isNotEmpty()) {
                                solutionMoveIndex = 0
                                val move = solutionMoves[0]
                                val (start, end) = FenParser.parseMove(move)
                                val newBoard = currentBoard.applyMove(start, end)
                                if (newBoard != null) {
                                    currentBoard = newBoard
                                }
                                solutionMoveIndex++ // Priprema za sledeći klik
                            } else if (solutionMoveIndex < solutionMoves.size) {
                                val move = solutionMoves[solutionMoveIndex]
                                val (start, end) = FenParser.parseMove(move)
                                val newBoard = currentBoard.applyMove(start, end)
                                if (newBoard != null) {
                                    currentBoard = newBoard
                                    solutionMoveIndex++ // Idi na sledeći potez
                                }
                            }
                        }
                    },
                    // Omogući samo ako postoji rešenje i nismo na kraju rešenja
                    enabled = currentProblem != null && currentProblem?.solution?.moves?.isNotEmpty() == true && solutionMoveIndex < (currentProblem?.solution?.moves?.size ?: 0),
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                ) {
                    Text("Sledeći potez")
                }
            }
        }

        // Dugme za predaju (pozicionirano ispod drugog reda ili prvog ako drugi nije vidljiv)
        Button(
            onClick = {
                gameResultMessage = "Predali ste se. Pokušajte ponovo!"
                showGameResultDialog = true
                usedSolution = true // Predaja se računa kao korišćenje rešenja
                showSolutionPath = true // Automatski prikaži rešenje kada se preda
                isPlayingSolution = false // Zaustavi play mod
                // Resetuj na početnu poziciju rešenja
                currentProblem?.let {
                    val (initialBoard, _) = FenParser.parseFenToBoard(it.fen)
                    currentBoard = initialBoard
                    selectedSquare = null
                    solutionMoveIndex = -1
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Predajem se / Završi sesiju")
        }


        Spacer(modifier = Modifier.height(16.dp))
        SnackbarHost(hostState = snackbarHostState)


        if (showGameResultDialog) {
            AlertDialog(
                onDismissRequest = { showGameResultDialog = false },
                title = { Text("Status Zagonetke") },
                text = { Text(gameResultMessage) },
                confirmButton = {
                    TextButton(onClick = {
                        showGameResultDialog = false
                        // Pređi na sledeću zagonetku ili završi sesiju
                        if (problems.isNotEmpty() && currentProblemIndex < problems.size - 1 && currentSessionProblemIndex < problemsInSession.size - 1) {
                            currentProblemIndex++
                        } else {
                            onGameFinished()
                        }
                    }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
fun ChessBoardComposable(
    board: Board,
    selectedSquare: Square?,
    solutionPath: List<String>?,
    onSquareClick: (Square) -> Unit
) {
    val squareSize = 40.dp
    val boardSize = squareSize * 8

    Column(
        modifier = Modifier
            .size(boardSize)
            .background(Color.DarkGray)
    ) {
        for (rank in 7 downTo 0) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (file in 0..7) {
                    val square = Square.fromCoordinates(file, rank)
                    val piece = board.getPiece(square)

                    val backgroundColor = if ((file + rank) % 2 == 0) {
                        Color(0xFFEEEED2)
                    } else {
                        Color(0xFF769656)
                    }

                    val squareModifier = Modifier
                        .size(squareSize)
                        .background(
                            when {
                                square == selectedSquare -> Color.Yellow.copy(alpha = 0.5f)
                                // Obojite samo početno i krajnje polje sledećeg poteza rešenja
                                solutionPath != null && isSquareInSolutionPath(square, solutionPath) -> Color.Red.copy(alpha = 0.5f)
                                else -> backgroundColor
                            }
                        )
                        .clickable { onSquareClick(square) }

                    Box(
                        modifier = squareModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        piece?.let {
                            val drawableResId = getPieceDrawableResId(it)
                            Image(
                                painter = painterResource(id = drawableResId),
                                contentDescription = "${it.color} ${it.type}",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

fun isSquareInSolutionPath(square: Square, solutionMoves: List<String>): Boolean {
    // Ova funkcija će sada označavati SVE poteze rešenja na tabli,
    // što može biti konfuzno ako želimo samo trenutni potez.
    // Za highlight trenutnog poteza trebaće nam dodatna logika.
    // Za sada, neka ostane ovako ako je cilj da se svi potezi rešenja markiraju.
    for (move in solutionMoves) {
        val (start, end) = FenParser.parseMove(move)
        if (square == start || square == end) {
            return true
        }
    }
    return false
}


@Composable
fun getPieceDrawableResId(piece: Piece): Int {
    val colorPrefix = if (piece.color == ChessColor.WHITE) "w" else "b"
    val typeSuffix = when (piece.type) {
        PieceType.PAWN -> "p"
        PieceType.KNIGHT -> "n"
        PieceType.BISHOP -> "b"
        PieceType.ROOK -> "r"
        PieceType.QUEEN -> "q"
        PieceType.KING -> "k"
    }
    val resourceName = "$colorPrefix$typeSuffix"
    return LocalContext.current.resources.getIdentifier(resourceName, "drawable", LocalContext.current.packageName)
}


@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PreviewChessScreen() {
    BrainTrainerTheme {
        ChessScreen(module = Module.Module1, difficulty = Difficulty.EASY, onGameFinished = {})
    }
}