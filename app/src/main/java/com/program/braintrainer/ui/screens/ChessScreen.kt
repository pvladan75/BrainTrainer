package com.program.braintrainer.ui.screens

import androidx.compose.animation.ExperimentalAnimationApi
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
import androidx.compose.material3.ExperimentalMaterial3Api


@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
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
    val activePlayerColor = ChessColor.WHITE // Beli je uvek na potezu, prema novim pravilima

    // Ključna promena za trajnu selekciju
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
            // Odmah selektuj prvu belu figuru ako postoji, za trajno selektovanje
            selectedSquare = board.pieces.entries.firstOrNull { it.value.color == ChessColor.WHITE }?.key

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
                val startFromIndex = if (solutionMoveIndex == -1) 0 else solutionMoveIndex

                for (i in startFromIndex until solutionMoves.size) {
                    // Dodata provera isPlayingSolution u petlji i provera da li je indeks i dalje isti
                    if (!isPlayingSolution || i != solutionMoveIndex) {
                        break
                    }
                    val move = solutionMoves[i]
                    val parsedMove = FenParser.parseMove(move)
                    val start = parsedMove.first
                    val end = parsedMove.second
                    val newBoard = currentBoard.applyMove(start, end)
                    if (newBoard != null) {
                        currentBoard = newBoard
                        solutionMoveIndex = i + 1 // Povećaj indeks za sledeći potez rešenja
                        selectedSquare = end // Selekcija prelazi na novo polje figure
                    }
                    delay(1000L) // Pauza između poteza
                }
                isPlayingSolution = false // Zaustavi puštanje kada se završi
                // Postavi solutionMoveIndex na poslednji potez rešenja
                if (solutionMoveIndex > 0 && solutionMoveIndex == solutionMoves.size) {
                    solutionMoveIndex = solutionMoves.size - 1
                }
            } else {
                isPlayingSolution = false
            }
        }
    }


    val problemsInSession = remember(problems) {
        if (problems.size >= 10) problems.take(10) else problems
    }
    val currentSessionProblemIndex = currentProblemIndex % problemsInSession.size

    fun checkGameStatus() {
        when (module) {
            Module.Module1 -> { // Modul 1: Pojesti sve crne figure, svaki potez mora biti uzimanje
                if (!currentBoard.hasBlackPiecesRemaining()) {
                    gameResultMessage = "Čestitamo! Sve crne figure su pojedene. Zagonetka rešena!"
                    if (usedSolution) {
                        gameResultMessage += "\n(Niste osvojili poene jer ste koristili rešenje.)"
                    }
                    showGameResultDialog = true
                } else if (!currentBoard.hasAnyLegalCaptureMove(activePlayerColor)) {
                    // Ovo je kompleksnije jer hasAnyLegalCaptureMove sada proverava i šah.
                    // Za modul 1, ovo je validno stanje neuspeha.
                    gameResultMessage = "Promašaj! Nema više legalnih poteza uzimanja, a ostale su crne figure."
                    gameResultMessage += "\n(Niste osvojili poene jer zagonetka nije rešena.)"
                    showGameResultDialog = true
                }
            }
            Module.Module2 -> { // Modul 2: Pojesti sve crne figure (bez obzira na put, ne mora biti uzimanje)
                if (!currentBoard.hasBlackPiecesRemaining()) {
                    gameResultMessage = "Čestitamo! Sve crne figure su pojedene. Zagonetka rešena!"
                    if (usedSolution) {
                        gameResultMessage += "\n(Niste osvojili poene jer ste koristili rešenje.)"
                    }
                    showGameResultDialog = true
                } else if (!currentBoard.hasAnyLegalMove(activePlayerColor)) {
                    gameResultMessage = "Promašaj! Nema više legalnih poteza za belog, a ostale su crne figure."
                    gameResultMessage += "\n(Niste osvojili poene jer zagonetka nije rešena.)"
                    showGameResultDialog = true
                }
            }
            Module.Module3 -> { // Modul 3: Pojesti crnog kralja
                val blackKingExists = currentBoard.pieces.any { it.value == Piece(PieceType.KING, ChessColor.BLACK) }

                if (!blackKingExists) {
                    gameResultMessage = "Čestitamo! Crni kralj je pojeden. Zagonetka rešena!"
                    if (usedSolution) {
                        gameResultMessage += "\n(Niste osvojili poene jer ste koristili rešenje.)"
                    }
                    showGameResultDialog = true
                } else if (!currentBoard.hasAnyLegalMove(activePlayerColor)) { // Nema više poteza za belog, a kralj nije pojeden
                    gameResultMessage = "Promašaj! Nema više legalnih poteza za belog, a crni kralj nije pojeden."
                    gameResultMessage += "\n(Niste osvojili poene jer zagonetka nije rešena.)"
                    showGameResultDialog = true
                }
            }
        }
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
                text = "Vreme: 00:00", // Ova logika za vreme nije implementirana u ovom fajlu
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
                text = "Na potezu: Beli", // Uvek Beli, po novim pravilima
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
            // Prosleđujemo putanju rešenja samo ako je showSolutionPath true
            solutionPath = if (showSolutionPath) currentProblem?.solution?.moves else null,
            onSquareClick = { clickedSquare ->
                if (showSolutionPath) {
                    coroutineScope.launch { snackbarHostState.showSnackbar("Prikaz rešenja je aktivan. Pritisnite 'Sakrij rešenje' za igranje.") }
                    return@ChessBoardComposable
                }

                val pieceOnClickedSquare = currentBoard.getPiece(clickedSquare)

                // Logika za selekciju figure
                if (selectedSquare == null) {
                    // Ako nema selektovane figure (samo na početku ili nakon reseta)
                    if (pieceOnClickedSquare != null && pieceOnClickedSquare.color == activePlayerColor) {
                        selectedSquare = clickedSquare
                    } else {
                        coroutineScope.launch { snackbarHostState.showSnackbar("Selektujte belu figuru za početak.") }
                    }
                    return@ChessBoardComposable // Vrati se nakon selekcije ili neuspešne selekcije
                }

                val startSquare = selectedSquare!!
                val endSquare = clickedSquare

                // Ako je kliknuto na istu selektovanu figuru ili drugu belu figuru, promeni selekciju
                if (startSquare == endSquare || (pieceOnClickedSquare != null && pieceOnClickedSquare.color == activePlayerColor)) {
                    selectedSquare = clickedSquare
                    return@ChessBoardComposable // Vrati se nakon promene selekcije
                }

                // Ako smo došli dovde, korisnik pokušava da odigra potez sa selektovane figure na 'clickedSquare'
                val isCapture = pieceOnClickedSquare != null && pieceOnClickedSquare.color != activePlayerColor

                // Modul 1: Svaki potez mora biti uzimanje
                if (module == Module.Module1 && !isCapture) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("U modulu 'Čišćenje table' svaki potez mora biti uzimanje!")
                    }
                    usedSolution = true // Računaj kao grešku
                    // selectedSquare NE MENJAJ! Ostaje selektovan na originalnoj poziciji figure!
                    return@ChessBoardComposable
                }

                // Modul 2 i 3: Ciljno polje ne sme biti prazno i pod napadom crnih figura
                if (module == Module.Module2 || module == Module.Module3) {
                    val attackedByBlack = currentBoard.getAttackedSquares(ChessColor.BLACK)
                    // Ako je polje pod napadom crnih, i NIJE uzimanje (tj. polje je prazno)
                    if (attackedByBlack.contains(endSquare) && !isCapture) {
                        // Potez se ipak odigra, ali je zagonetka neuspešna
                        val tempBoardAfterMove = currentBoard.applyMove(startSquare, endSquare)
                        if (tempBoardAfterMove != null) {
                            currentBoard = tempBoardAfterMove
                            usedSolution = true // Označi kao neuspešno rešenje
                            gameResultMessage = "Pogrešan potez! Pomerili ste figuru na polje koje je pod napadom crnih figura, a na njemu nije bilo figure. Zagonetka je neuspešno rešena."
                            showGameResultDialog = true // Prikaži dijalog
                            selectedSquare = null // Poništi selekciju nakon što je zagonetka neuspešna
                            return@ChessBoardComposable
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Greška pri izvršavanju poteza (nevaljan potez za figuru).")
                            }
                            // selectedSquare NE MENJAJ! Ostaje selektovan na originalnoj poziciji figure!
                            return@ChessBoardComposable
                        }
                    }
                }

                // Standardna provera legalnosti poteza (po kretanju figure, bez šaha/mata)
                val isMoveLegalByPieceRules = currentBoard.isValidMove(startSquare, endSquare)

                if (isMoveLegalByPieceRules) {
                    val newBoard = currentBoard.applyMove(startSquare, endSquare)
                    if (newBoard != null) {
                        currentBoard = newBoard
                        println("Potez ${startSquare.toNotation()}${endSquare.toNotation()} odigran.")
                        selectedSquare = endSquare // KLJUČNA PROMENA: Selekcija se premešta na KRAJNJE POLJE POTEZA

                        // Provera statusa igre nakon svakog validnog poteza (uključujući uzimanja)
                        // Za Modul 3: Pojeli smo kralja
                        if (module == Module.Module3 && !newBoard.pieces.any { it.value == Piece(PieceType.KING, ChessColor.BLACK) }) {
                            gameResultMessage = "Čestitamo! Crni kralj je pojeden. Zagonetka rešena!"
                            showGameResultDialog = true
                        } else {
                            // Za module 1 i 2, pozovi checkGameStatus nakon validnog poteza
                            checkGameStatus()
                        }

                    } else {
                        println("Nevažeći potez (applyMove vratio null) ili nepoznat razlog).")
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Greška pri izvršavanju poteza.")
                        }
                        // selectedSquare NE MENJAJ! Ostaje selektovan na originalnoj poziciji figure!
                    }
                } else {
                    println("Nevažeći potez (prema pravilima kretanja figure).")
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Ne možete pomeriti figuru tako! Nelegalno kretanje za tu figuru.")
                    }
                    // selectedSquare NE MENJAJ! Ostaje selektovan na originalnoj poziciji figure!
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
                    isPlayingSolution = false // Zaustavi automatsko puštanje ako se prebacuje prikaz rešenja
                    // Resetuj stanje table na početnu poziciju kada se prebacuje prikaz rešenja
                    currentProblem?.let {
                        val (initialBoard, _) = FenParser.parseFenToBoard(it.fen)
                        currentBoard = initialBoard
                        // Nakon reseta, ponovo selektuj prvu belu figuru
                        selectedSquare = initialBoard.pieces.entries.firstOrNull { it.value.color == ChessColor.WHITE }?.key
                        solutionMoveIndex = -1 // Postavi na početak rešenja
                        usedSolution = true // Korišćenje rešenja
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(if (showSolutionPath) "Sakrij rešenje" else "Prikaži rešenje")
            }

            Button(
                onClick = {
                    if (problemsInSession.isNotEmpty() && currentProblemIndex + 1 < problems.size && currentSessionProblemIndex + 1 < problemsInSession.size) {
                        currentProblemIndex++
                    } else {
                        onGameFinished() // Završi sesiju ako nema više zagonetki
                    }
                },
                // Omogućeno samo ako ima sledećih zagonetki u sesiji
                enabled = problemsInSession.isNotEmpty() && currentSessionProblemIndex + 1 < problemsInSession.size,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text("Sledeća zagonetka")
            }
        }

        // DRUGI RED DUGMIĆA (vidljiv i omogućen samo kada je showSolutionPath == true)
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
                        isPlayingSolution = false // Zaustavi automatsko puštanje
                        val solutionMoves = currentProblem?.solution?.moves
                        if (solutionMoves != null) {
                            if (solutionMoveIndex > 0) {
                                solutionMoveIndex--
                                // Rekonstruiši tablu do trenutnog poteza rešenja
                                val (initialBoard, _) = FenParser.parseFenToBoard(currentProblem!!.fen)
                                var tempBoard = initialBoard
                                for (i in 0 until solutionMoveIndex) { // Iteriraj samo do novog solutionMoveIndex
                                    val move = solutionMoves[i]
                                    val parsedMove = FenParser.parseMove(move)
                                    val start = parsedMove.first
                                    val end = parsedMove.second
                                    tempBoard = tempBoard.applyMove(start, end) ?: tempBoard
                                }
                                currentBoard = tempBoard
                                // Nakon rekonstrukcije, selekcija se vraća na figuru koja je odigrala poslednji potez
                                // Ako je solutionMoveIndex -1 (početak), tada selektuj prvu belu
                                selectedSquare = if (solutionMoveIndex == -1) {
                                    initialBoard.pieces.entries.firstOrNull { it.value.color == ChessColor.WHITE }?.key
                                } else {
                                    // Pronađi krajnje polje poteza sa trenutnim solutionMoveIndex
                                    val lastMoveEndSquare = FenParser.parseMove(solutionMoves[solutionMoveIndex]).second
                                    lastMoveEndSquare
                                }

                            } else if (solutionMoveIndex == 0) { // Ako smo na prvom potezu, vrati se na početak zagonetke
                                val (initialBoard, _) = FenParser.parseFenToBoard(currentProblem!!.fen)
                                currentBoard = initialBoard
                                selectedSquare = initialBoard.pieces.entries.firstOrNull { it.value.color == ChessColor.WHITE }?.key
                                solutionMoveIndex = -1 // Pokaži da smo pre početka rešenja
                            }
                        }
                    },
                    // Omogućeno ako ima prethodnog poteza rešenja za prikaz (ako nismo na početnoj poziciji)
                    enabled = currentProblem != null && currentProblem?.solution?.moves?.isNotEmpty() == true && solutionMoveIndex > -1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                ) {
                    Text("Prethodni potez")
                }

                Button(
                    onClick = {
                        isPlayingSolution = !isPlayingSolution
                        // Ako počinjemo play i nismo još odigrali ni jedan potez rešenja
                        if (isPlayingSolution && solutionMoveIndex == -1) {
                            currentProblem?.let {
                                val (initialBoard, _) = FenParser.parseFenToBoard(it.fen)
                                currentBoard = initialBoard
                                // Ponovo selektuj prvu belu figuru
                                selectedSquare = initialBoard.pieces.entries.firstOrNull { it.value.color == ChessColor.WHITE }?.key
                                solutionMoveIndex = 0 // Postavi na prvi potez za play
                            }
                        }
                    },
                    // Omogućeno ako postoji rešenje i nismo na poslednjem potezu
                    enabled = currentProblem != null && currentProblem?.solution?.moves?.isNotEmpty() == true && solutionMoveIndex < (currentProblem?.solution?.moves?.size ?: 0),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    Text(if (isPlayingSolution) "Pauza" else "Play")
                }

                Button(
                    onClick = {
                        isPlayingSolution = false // Zaustavi automatsko puštanje
                        val solutionMoves = currentProblem?.solution?.moves
                        if (solutionMoves != null && solutionMoveIndex < solutionMoves.size) {
                            // Ako je -1, to znači da smo na početku, odigraj prvi potez (koji će biti indeks 0)
                            val nextMoveIndex = if (solutionMoveIndex == -1) 0 else solutionMoveIndex
                            val move = solutionMoves[nextMoveIndex]
                            val parsedMove = FenParser.parseMove(move)
                            val start = parsedMove.first
                            val end = parsedMove.second
                            val newBoard = currentBoard.applyMove(start, end)
                            if (newBoard != null) {
                                currentBoard = newBoard
                                solutionMoveIndex = nextMoveIndex + 1 // Povećaj indeks za sledeći potez
                                // Nakon odigranog poteza, selekcija prelazi na novo polje figure
                                selectedSquare = end
                            }
                        }
                    },
                    // Omogućeno ako postoji rešenje i nismo na poslednjem potezu
                    enabled = currentProblem != null && currentProblem?.solution?.moves?.isNotEmpty() == true && solutionMoveIndex < (currentProblem?.solution?.moves?.size ?: 0),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                ) {
                    Text("Sledeći potez")
                }
            }
        }

        Button(
            onClick = {
                gameResultMessage = "Predali ste se. Zagonetka nije rešena."
                showGameResultDialog = true
                usedSolution = true // Predaja se računa kao korišćenje rešenja
                isPlayingSolution = false // Zaustavi play
                currentProblem?.let {
                    val (initialBoard, _) = FenParser.parseFenToBoard(it.fen)
                    currentBoard = initialBoard
                    // Nakon reseta, ponovo selektuj prvu belu figuru
                    selectedSquare = initialBoard.pieces.entries.firstOrNull { it.value.color == ChessColor.WHITE }?.key
                    solutionMoveIndex = -1 // Resetuj indeks za rešenje
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
                        // Resetuj selekciju i stanje rešenja kada se završi dijalog
                        // SelectedSquare se ovde postavlja na null samo ako zagonetka ZAVRŠAVA.
                        // Ako se samo prikazuje greška (Modul 2/3 branjeno polje) ali zagonetka nastavlja,
                        // onda se ne resetuje.
                        // Po tvojoj logici, neuspeh znači kraj zagonetke.
                        selectedSquare = null // Poništi selekciju kada se zagonetka završi
                        showSolutionPath = false
                        solutionMoveIndex = -1
                        isPlayingSolution = false

                        // Pređi na sledeću zagonetku ili završi sesiju
                        if (problemsInSession.isNotEmpty() && currentProblemIndex + 1 < problems.size && currentSessionProblemIndex + 1 < problemsInSession.size) {
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
    solutionPath: List<String>?, // Sada prima listu stringova poteza
    onSquareClick: (Square) -> Unit
) {
    val squareSize = 40.dp // Povećao sam veličinu polja radi bolje vidljivosti
    val boardSize = squareSize * 8

    Column(
        modifier = Modifier
            .size(boardSize)
            .background(Color.DarkGray)
    ) {
        for (rank in 7 downTo 0) { // Odbrojavamo unazad za ispravan prikaz table
            Row(modifier = Modifier.fillMaxWidth()) {
                for (file in 0..7) {
                    val square = Square.fromCoordinates(file, rank)
                    val piece = board.getPiece(square)

                    val backgroundColor = if ((file + rank) % 2 == 0) {
                        Color(0xFFEEEED2) // Svetlo polje
                    } else {
                        Color(0xFF769656) // Tamno polje
                    }

                    val squareModifier = Modifier
                        .size(squareSize)
                        .background(
                            when {
                                square == selectedSquare -> Color.Yellow.copy(alpha = 0.5f) // Selektovano polje
                                !solutionPath.isNullOrEmpty() && isSquareInSolutionPath(square, solutionPath) -> Color.Blue.copy(alpha = 0.5f) // Polje u putanji rešenja
                                else -> backgroundColor // Podrazumevana boja polja
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

// Ova funkcija proverava da li je dato polje deo bilo kog poteza u listi poteza rešenja
fun isSquareInSolutionPath(square: Square, solutionMoves: List<String>): Boolean {
    for (move in solutionMoves) {
        val parsedMove = FenParser.parseMove(move)
        val start = parsedMove.first
        val end = parsedMove.second
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