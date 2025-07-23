package com.program.braintrainer.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.program.braintrainer.R
import com.program.braintrainer.chess.model.*
import com.program.braintrainer.chess.model.data.AppSettings
import com.program.braintrainer.chess.model.data.ProblemLoader
import com.program.braintrainer.chess.model.data.SettingsManager
import com.program.braintrainer.chess.parser.FenParser
import com.program.braintrainer.chess.solver.UniversalPuzzleSolver
import com.program.braintrainer.gamification.AchievementManager
import com.program.braintrainer.gamification.PuzzleResultData
import com.program.braintrainer.rules.Module1Rules
import com.program.braintrainer.rules.Module2Rules
import com.program.braintrainer.rules.Module3Rules
import com.program.braintrainer.score.ScoreManager
import com.program.braintrainer.ui.theme.BrainTrainerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import com.program.braintrainer.chess.model.Color as ChessColor

// ===================================================================
// ===         CENTRALNO MESTO ZA PODEŠAVANJE BODOVANJA            ===
// ===================================================================
data class ScoringParams(
    val basePointsEasy: Int = 10,
    val basePointsMedium: Int = 20,
    val basePointsHard: Int = 30,
    val maxTimeForBonusEasy: Int = 30,
    val maxTimeForBonusMedium: Int = 60,
    val maxTimeForBonusHard: Int = 90,
    val penaltyPerMistake: Int = 5,
    val penaltyPerExtraMove: Int = 2,
    val streakBonusEasy: Int = 3,
    val streakBonusMedium: Int = 8,
    val streakBonusHard: Int = 20
)
// ===================================================================

// --- FUNKCIJE ZA UČITAVANJE REKLAMA ---

private fun loadRewardedAd(
    context: Context,
    adUnitId: String,
    onAdLoaded: (RewardedAd) -> Unit,
    onAdFailedToLoad: () -> Unit
) {
    val adRequest = AdRequest.Builder().build()
    RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
        override fun onAdFailedToLoad(adError: LoadAdError) {
            Log.d("AdMob", "Rewarded ad ($adUnitId) failed to load: ${adError.message}")
            onAdFailedToLoad()
        }
        override fun onAdLoaded(rewardedAd: RewardedAd) {
            Log.d("AdMob", "Rewarded ad ($adUnitId) was loaded.")
            onAdLoaded(rewardedAd)
        }
    })
}

private fun loadInterstitialAd(
    context: Context,
    onAdLoaded: (InterstitialAd) -> Unit,
    onAdFailedToLoad: () -> Unit
) {
    val adUnitId = "ca-app-pub-3940256099942544/1033173712" // Testni ID
    InterstitialAd.load(context, adUnitId, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
        override fun onAdFailedToLoad(adError: LoadAdError) {
            Log.d("AdMob", "Interstitial ad failed to load: ${adError.message}")
            onAdFailedToLoad()
        }
        override fun onAdLoaded(interstitialAd: InterstitialAd) {
            Log.d("AdMob", "Interstitial ad was loaded.")
            onAdLoaded(interstitialAd)
        }
    })
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ChessScreen(
    module: Module,
    difficulty: Difficulty,
    onGameFinished: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    val scoreManager = remember { ScoreManager(context) }
    val achievementManager = remember { AchievementManager(context, settingsManager) }
    val scoringParams = remember { ScoringParams() }

    // --- ČITANJE PREMIUM STATUSA ---
    val isPremium by settingsManager.settingsFlow.map { it.isPremiumUser }.collectAsState(initial = false)

    // --- STANJE ZA REKLAME ---
    var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }
    var isAdLoading by remember { mutableStateOf(false) }
    var hintRewardEarned by remember { mutableStateOf(false) }
    var doubleXpRewardEarned by remember { mutableStateOf(false) }
    var doubleXpButtonEnabled by remember { mutableStateOf(true) }


    // === Učitavanje pozicija u pozadini ===
    var problems by remember { mutableStateOf<List<Problem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(key1 = module, key2 = difficulty) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val loadedProblems = ProblemLoader(context).loadProblemsForModuleAndDifficulty(module, difficulty)
            problems = loadedProblems
        }
        isLoading = false
    }

    // --- USLOVNO UČITAVANJE REKLAMA ---
    LaunchedEffect(isPremium) {
        if (!isPremium) {
            loadInterstitialAd(context,
                onAdLoaded = { ad -> interstitialAd = ad },
                onAdFailedToLoad = { interstitialAd = null }
            )
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var currentProblemIndex by remember { mutableIntStateOf(0) }
    var currentProblem by remember { mutableStateOf<Problem?>(null) }
    var currentBoard by remember { mutableStateOf(Board()) }
    val activePlayerColor = ChessColor.WHITE
    var selectedSquare by remember { mutableStateOf<Square?>(null) }
    var showSolutionPath by remember { mutableStateOf(false) }
    var solutionMoveIndex by remember { mutableIntStateOf(-1) }
    var isPlayingSolution by remember { mutableStateOf(false) }
    var usedSolution by remember { mutableStateOf(false) }
    var showGameResultDialog by remember { mutableStateOf(false) }
    var showNoMoreMovesDialog by remember { mutableStateOf(false) }
    var gameResultMessage by remember { mutableStateOf("") }
    var showDefendedSquareDialog by remember { mutableStateOf(false) }
    var boardForDialog by remember { mutableStateOf<Board?>(null) }
    var showSessionEndDialog by remember { mutableStateOf(false) }
    var lastAwardedXp by remember { mutableIntStateOf(0) }
    var elapsedTimeInSeconds by remember { mutableIntStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var defendedSquareMistakes by remember { mutableIntStateOf(0) }
    var playerMoveCount by remember { mutableIntStateOf(0) }
    var correctStreak by remember { mutableIntStateOf(0) }
    var hintMoves by remember { mutableStateOf<List<Move>>(emptyList()) }
    var isShowingHint by remember { mutableStateOf(false) }
    var hintMoveIndex by remember { mutableIntStateOf(0) }
    val problemsInSession = remember(problems) {
        if (problems.size >= 10) problems.shuffled().take(10) else problems.shuffled()
    }
    val currentSessionProblemIndex = if (problemsInSession.isNotEmpty()) currentProblemIndex % problemsInSession.size else 0

    LaunchedEffect(problemsInSession, currentProblemIndex) {
        if (problemsInSession.isNotEmpty()) {
            val problem = problemsInSession[currentProblemIndex]
            currentProblem = problem
            val (board, _) = FenParser.parseFenToBoard(problem.fen)
            currentBoard = board
            selectedSquare = board.pieces.entries.firstOrNull { it.value.color == ChessColor.WHITE }?.key
            elapsedTimeInSeconds = 0
            isTimerRunning = true
            defendedSquareMistakes = 0
            playerMoveCount = 0
            isShowingHint = false
            hintMoves = emptyList()
            showSolutionPath = false
            solutionMoveIndex = -1
            isPlayingSolution = false
            usedSolution = false
            lastAwardedXp = 0
            showGameResultDialog = false
            showNoMoreMovesDialog = false
            gameResultMessage = ""
            doubleXpButtonEnabled = true
        }
    }


    fun stopTimer() { isTimerRunning = false }
    fun startTimer() { isTimerRunning = true }

    fun showHint() {
        coroutineScope.launch(Dispatchers.Default) {
            val rules = when (module) {
                Module.Module1 -> Module1Rules()
                Module.Module2 -> Module2Rules()
                Module.Module3 -> Module3Rules()
            }
            val solver = UniversalPuzzleSolver(rules)
            val solution = solver.solve(currentBoard)
            launch(Dispatchers.Main) {
                if (solution.isSolved) {
                    hintMoves = solution.path.take(3)
                    hintMoveIndex = 0
                    isShowingHint = true
                } else {
                    snackbarHostState.showSnackbar("Solver nije uspeo da pronađe rešenje.")
                    startTimer()
                }
            }
        }
    }

    val onHintClick: () -> Unit = {
        if (!isAdLoading) {
            if (!isPremium) {
                isAdLoading = true
                stopTimer()
                loadRewardedAd(context, "ca-app-pub-3940256099942544/5224354917",
                    onAdLoaded = { ad ->
                        isAdLoading = false
                        val activity = context as? Activity
                        if (activity != null) {
                            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    if (hintRewardEarned) {
                                        showHint()
                                        hintRewardEarned = false
                                    } else {
                                        startTimer()
                                    }
                                }
                                override fun onAdFailedToShowFullScreenContent(p0: AdError) { startTimer() }
                            }
                            ad.show(activity) { hintRewardEarned = true }
                        } else {
                            startTimer()
                        }
                    },
                    onAdFailedToLoad = {
                        isAdLoading = false
                        coroutineScope.launch { snackbarHostState.showSnackbar("Reklama nije dostupna.") }
                        startTimer()
                    }
                )
            } else {
                showHint()
            }
        }
    }

    val onDoubleXpClick: () -> Unit = {
        if (!isAdLoading) {
            isAdLoading = true
            loadRewardedAd(context, "ca-app-pub-3940256099942544/5224354917",
                onAdLoaded = { ad ->
                    isAdLoading = false
                    val activity = context as? Activity
                    if (activity != null) {
                        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                if (doubleXpRewardEarned) {
                                    scoreManager.addXp(lastAwardedXp)
                                    val bonusMessage = "\n\n🔥 Bonus reklama: +$lastAwardedXp"
                                    gameResultMessage += bonusMessage
                                    doubleXpButtonEnabled = false
                                    doubleXpRewardEarned = false
                                }
                            }
                        }
                        ad.show(activity) { doubleXpRewardEarned = true }
                    }
                },
                onAdFailedToLoad = {
                    isAdLoading = false
                    coroutineScope.launch { snackbarHostState.showSnackbar("Reklama nije dostupna.") }
                }
            )
        }
    }

    fun checkGameStatus(isSuccess: Boolean) {
        stopTimer()

        coroutineScope.launch {
            if (settingsManager.settingsFlow.first().isSoundEnabled) {
                val soundToPlay = if (isSuccess && !usedSolution) R.raw.succes else R.raw.failed
                MediaPlayer.create(context, soundToPlay).start()
            }
        }

        var detailedMessage = ""

        val isPerfect = defendedSquareMistakes == 0 && playerMoveCount <= (currentProblem?.solution?.moves?.size ?: playerMoveCount)

        if (isSuccess && !usedSolution) {
            scoreManager.incrementTotalPuzzlesSolved()
            scoreManager.incrementSolvedInModule(module)
            scoreManager.incrementSolvedCount(module, difficulty)

            if (isPerfect) {
                scoreManager.incrementPerfectStreak()
                scoreManager.incrementPerfectSolvedCount(module, difficulty)
            } else {
                scoreManager.resetPerfectStreak()
            }

            val resultData = PuzzleResultData(
                module = module,
                difficulty = difficulty,
                wasSuccess = true,
                mistakesMade = defendedSquareMistakes,
                timeTakenSeconds = elapsedTimeInSeconds,
                currentStreak = scoreManager.getPerfectStreak(),
                totalPuzzlesSolved = scoreManager.getTotalPuzzlesSolved(),
                totalSolvedInModule = scoreManager.getSolvedInModule(module)
            )
            coroutineScope.launch(Dispatchers.IO) {
                achievementManager.checkAndUnlockAchievements(resultData)
            }
        } else {
            scoreManager.resetPerfectStreak()
        }

        if (!isSuccess) {
            correctStreak = 0
            gameResultMessage = when (module) {
                Module.Module1 -> "Promašaj! Nema više legalnih poteza uzimanja."
                else -> "Promašaj! Nema više legalnih poteza."
            }
            showGameResultDialog = true
            return
        }

        detailedMessage = "Zagonetka rešena!\n\n"
        var finalPoints = 0
        if (!usedSolution) {
            val basePoints = when (difficulty) {
                Difficulty.EASY -> scoringParams.basePointsEasy
                Difficulty.MEDIUM -> scoringParams.basePointsMedium
                Difficulty.HARD -> scoringParams.basePointsHard
            }
            detailedMessage += "✅ Osnovni poeni: +$basePoints\n"
            val maxTimeForBonus = when (difficulty) {
                Difficulty.EASY -> scoringParams.maxTimeForBonusEasy
                Difficulty.MEDIUM -> scoringParams.maxTimeForBonusMedium
                Difficulty.HARD -> scoringParams.maxTimeForBonusHard
            }
            val timeBonus = max(0, maxTimeForBonus - elapsedTimeInSeconds)
            detailedMessage += "🕒 Bonus za vreme: +$timeBonus\n"
            val optimalMoves = currentProblem?.solution?.moves?.size ?: playerMoveCount
            val extraMoves = max(0, playerMoveCount - optimalMoves)
            val efficiencyPenalty = extraMoves * scoringParams.penaltyPerExtraMove
            detailedMessage += "🔻 Kazna za poteze: -$efficiencyPenalty ($extraMoves poteza viška)\n"
            val mistakePenalty = defendedSquareMistakes * scoringParams.penaltyPerMistake
            detailedMessage += "🔻 Kazna za greške: -$mistakePenalty ($defendedSquareMistakes grešaka)\n"

            if (isPerfect) {
                correctStreak++
                val bonusPerStreak = when(difficulty) {
                    Difficulty.EASY -> scoringParams.streakBonusEasy
                    Difficulty.MEDIUM -> scoringParams.streakBonusMedium
                    Difficulty.HARD -> scoringParams.streakBonusHard
                }
                val totalBonus = correctStreak * bonusPerStreak
                detailedMessage += "🔥 Bonus za niz: +$totalBonus ($correctStreak zagonetki zaredom)\n"
                finalPoints += totalBonus
            } else {
                correctStreak = 0
                detailedMessage += "🔥 Bonus za niz: +0 (niz prekinut)\n"
            }

            finalPoints += max(0, basePoints + timeBonus - efficiencyPenalty - mistakePenalty)

            scoreManager.addXp(finalPoints)
            lastAwardedXp = finalPoints

            detailedMessage += "\n🏆 Osvojeno XP: $finalPoints"

            if (isPremium && lastAwardedXp > 0) {
                scoreManager.addXp(lastAwardedXp)
                val bonusMessage = "\n\n🔥 Premium bonus: +$lastAwardedXp"
                detailedMessage += bonusMessage
            }
        } else {
            correctStreak = 0
            lastAwardedXp = 0
            detailedMessage = "Zagonetka rešena uz pomoć.\n\n(Niste osvojili XP poene jer ste koristili rešenje.)"
        }
        gameResultMessage = detailedMessage
        showGameResultDialog = true
    }

    fun showInterstitialAdAndFinish() {
        val activity = context as? Activity
        if (!isPremium && interstitialAd != null && activity != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() { onGameFinished() }
                override fun onAdFailedToShowFullScreenContent(p0: AdError) { onGameFinished() }
            }
            interstitialAd?.show(activity)
        } else {
            onGameFinished()
        }
    }

    val onNextPuzzle: () -> Unit = {
        showGameResultDialog = false
        showNoMoreMovesDialog = false
        if (currentProblemIndex + 1 < problemsInSession.size) {
            currentProblemIndex++
        } else {
            stopTimer()
            showSessionEndDialog = true
        }
    }

    val onShowSolution: () -> Unit = {
        if (!showSolutionPath) {
            stopTimer()
            usedSolution = true
            correctStreak = 0
        }
        showGameResultDialog = false
        showNoMoreMovesDialog = false
        showSolutionPath = !showSolutionPath
        isPlayingSolution = false
        currentProblem?.let {
            val (initialBoard, _) = FenParser.parseFenToBoard(it.fen)
            currentBoard = initialBoard
            selectedSquare = initialBoard.pieces.entries.firstOrNull { p -> p.value.color == ChessColor.WHITE }?.key
            solutionMoveIndex = -1
        }
    }

    val onPreviousMove: () -> Unit = {
        isPlayingSolution = false
        val solutionMoves = currentProblem?.solution?.moves
        if (solutionMoves != null) {
            if (solutionMoveIndex > 0) {
                solutionMoveIndex--
                val (initialBoard, _) = FenParser.parseFenToBoard(currentProblem!!.fen)
                var tempBoard = initialBoard
                for (i in 0 until solutionMoveIndex) {
                    val (start, end) = FenParser.parseMove(solutionMoves[i])
                    tempBoard = tempBoard.applyMove(start, end) ?: tempBoard
                }
                currentBoard = tempBoard
                selectedSquare = if (solutionMoveIndex > 0) {
                    FenParser.parseMove(solutionMoves[solutionMoveIndex - 1]).second
                } else {
                    initialBoard.pieces.entries.firstOrNull { it.value.color == ChessColor.WHITE }?.key
                }
            } else if (solutionMoveIndex <= 0) {
                val (initialBoard, _) = FenParser.parseFenToBoard(currentProblem!!.fen)
                currentBoard = initialBoard
                selectedSquare = initialBoard.pieces.entries.firstOrNull { it.value.color == ChessColor.WHITE }?.key
                solutionMoveIndex = -1
            }
        }
    }

    val onNextMove: () -> Unit = {
        isPlayingSolution = false
        val solutionMoves = currentProblem?.solution?.moves
        if (solutionMoves != null && solutionMoveIndex < solutionMoves.size) {
            val nextMoveIndex = if (solutionMoveIndex == -1) 0 else solutionMoveIndex
            val (start, end) = FenParser.parseMove(solutionMoves[nextMoveIndex])
            currentBoard.applyMove(start, end)?.let { newBoard ->
                currentBoard = newBoard
                solutionMoveIndex = nextMoveIndex + 1
                selectedSquare = end
            }
        }
    }

    val onSquareClick: (Square) -> Unit = click@{ clickedSquare ->
        if (showSolutionPath || showGameResultDialog || showSessionEndDialog || showNoMoreMovesDialog || isShowingHint) {
            return@click
        }
        val pieceOnClickedSquare = currentBoard.getPiece(clickedSquare)
        if (selectedSquare == null) {
            if (pieceOnClickedSquare != null && pieceOnClickedSquare.color == activePlayerColor) {
                selectedSquare = clickedSquare
            }
            return@click
        }
        val startSquare = selectedSquare!!
        if (startSquare == clickedSquare || (pieceOnClickedSquare != null && pieceOnClickedSquare.color == activePlayerColor)) {
            selectedSquare = clickedSquare
            return@click
        }
        if (currentBoard.isValidMove(startSquare, clickedSquare)) {
            if (module == Module.Module1 && currentBoard.getPiece(clickedSquare) == null) {
                coroutineScope.launch { snackbarHostState.showSnackbar("U ovom modulu svaki potez mora biti uzimanje!") }
                return@click
            }
            val newBoard = currentBoard.applyMove(startSquare, clickedSquare)
            if (newBoard != null) {
                if (module == Module.Module2 || module == Module.Module3) {
                    val attackedByBlackOnNewBoard = newBoard.getAttackedSquares(ChessColor.BLACK)
                    if (attackedByBlackOnNewBoard.contains(clickedSquare)) {
                        boardForDialog = newBoard
                        showDefendedSquareDialog = true
                        selectedSquare = startSquare
                        defendedSquareMistakes++
                        return@click
                    }
                }
                currentBoard = newBoard
                selectedSquare = clickedSquare
                playerMoveCount++
                when (module) {
                    Module.Module1 -> {
                        if (!currentBoard.hasBlackPiecesRemaining()) {
                            checkGameStatus(isSuccess = true)
                        } else if (!currentBoard.hasAnyLegalCaptureMove(activePlayerColor)) {
                            stopTimer()
                            correctStreak = 0
                            scoreManager.resetPerfectStreak()
                            showNoMoreMovesDialog = true
                        }
                    }
                    Module.Module2 -> {
                        if (!currentBoard.hasBlackPiecesRemaining()) checkGameStatus(isSuccess = true)
                        else if (!currentBoard.hasAnyLegalMove(activePlayerColor)) checkGameStatus(isSuccess = false)
                    }
                    Module.Module3 -> {
                        if (!currentBoard.pieces.any { it.value == Piece(PieceType.KING, ChessColor.BLACK) }) checkGameStatus(isSuccess = true)
                        else if (!currentBoard.hasAnyLegalMove(activePlayerColor)) checkGameStatus(isSuccess = false)
                    }
                }
            }
        } else {
            coroutineScope.launch { snackbarHostState.showSnackbar("Ne možete pomeriti figuru tako!") }
        }
    }

    val onSurrender: () -> Unit = {
        stopTimer()
        usedSolution = true
        correctStreak = 0
        gameResultMessage = "Predali ste se. Zagonetka nije rešena."
        showGameResultDialog = true
    }

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (true) {
                delay(1000L)
                elapsedTimeInSeconds++
            }
        }
    }

    LaunchedEffect(isShowingHint, hintMoveIndex) {
        if (isShowingHint && hintMoves.isNotEmpty() && hintMoveIndex < hintMoves.size) {
            delay(1500L)
            if (hintMoveIndex < hintMoves.size - 1) {
                hintMoveIndex++
            } else {
                isShowingHint = false
                hintMoves = emptyList()
                startTimer()
            }
        }
    }

    val currentlyHighlightedHintMove = if (isShowingHint) hintMoves.getOrNull(hintMoveIndex) else null

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            val configuration = LocalConfiguration.current
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                Row(modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    GameInfoPanel(module, difficulty, currentProblem, problemsInSession, currentSessionProblemIndex, Modifier.weight(1f), elapsedTimeInSeconds)
                    ChessBoardComposable(currentBoard, selectedSquare, onSquareClick, currentlyHighlightedHintMove, modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .aspectRatio(1f))
                    GameControlsPanel(showSolutionPath, isPlayingSolution, solutionMoveIndex, currentProblem, onShowSolution, onNextPuzzle, onPreviousMove, { isPlayingSolution = !isPlayingSolution }, onNextMove, onHintClick, onSurrender, modifier = Modifier.weight(1f))
                }
            } else {
                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceAround) {
                    GameInfoPanel(module, difficulty, currentProblem, problemsInSession, currentSessionProblemIndex, elapsedTime = elapsedTimeInSeconds)
                    ChessBoardComposable(currentBoard, selectedSquare, onSquareClick, currentlyHighlightedHintMove, modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .aspectRatio(1f))
                    GameControlsPanel(showSolutionPath, isPlayingSolution, solutionMoveIndex, currentProblem, onShowSolution, onNextPuzzle, onPreviousMove, { isPlayingSolution = !isPlayingSolution }, onNextMove, onHintClick, onSurrender)
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))

        if (showGameResultDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Status zagonetke") },
                text = { Text(gameResultMessage) },
                dismissButton = {
                    TextButton(onClick = onShowSolution) { Text("Rešenje") }
                },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (lastAwardedXp > 0 && !isPremium) {
                            TextButton(
                                onClick = onDoubleXpClick,
                                enabled = doubleXpButtonEnabled && !isAdLoading
                            ) {
                                Text("Dupliraj XP")
                            }
                        }
                        TextButton(onClick = onNextPuzzle) {
                            Text(if (currentProblemIndex + 1 < problemsInSession.size) "Sledeća" else "Kraj")
                        }
                    }
                }
            )
        }

        if (showNoMoreMovesDialog) {
            NoMoreMovesDialog(onShowSolution = onShowSolution, onNewGame = onNextPuzzle)
        }
        if (showSessionEndDialog) {
            val totalXp = scoreManager.getTotalXp()
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Kraj sesije") },
                text = {
                    Column {
                        Text("Završili ste sesiju.")
                        Text("Ukupno XP poena: $totalXp")
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showSessionEndDialog = false
                        showInterstitialAdAndFinish()
                    }) {
                        Text("Glavni Meni")
                    }
                }
            )
        }
        if (showDefendedSquareDialog && boardForDialog != null) {
            DefendedSquareDialog(board = boardForDialog!!, onDismiss = { showDefendedSquareDialog = false })
        }
    }
}

@Composable
fun NoMoreMovesDialog(onShowSolution: () -> Unit, onNewGame: () -> Unit) {
    AlertDialog(onDismissRequest = { }, title = { Text("Nema više poteza") }, text = { Text("Nažalost, ostali ste bez mogućih poteza kojima biste pojeli preostale crne figure.") }, dismissButton = { TextButton(onClick = onShowSolution) { Text("Pregled rešenja") } }, confirmButton = { TextButton(onClick = onNewGame) { Text("Nova zagonetka") } })
}

@SuppressLint("DefaultLocale")
@Composable
fun GameInfoPanel(module: Module, difficulty: Difficulty, currentProblem: Problem?, problemsInSession: List<Problem>, currentSessionProblemIndex: Int, modifier: Modifier = Modifier, elapsedTime: Int) {
    val minutes = elapsedTime / 60
    val seconds = elapsedTime % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Mod: ${module.title}", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(text = "Težina: ${difficulty.label}", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Vreme: $timeString", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (problemsInSession.isNotEmpty()) "Zagonetka: ${currentSessionProblemIndex + 1}/${problemsInSession.size}" else "Učitavanje...",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Na potezu: Beli", style = MaterialTheme.typography.bodyLarge)
        currentProblem?.let {
            Text(
                text = "Cilj: ${it.description}",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun GameControlsPanel(
    showSolutionPath: Boolean,
    isPlayingSolution: Boolean,
    solutionMoveIndex: Int,
    currentProblem: Problem?,
    onShowSolutionClick: () -> Unit,
    onNextPuzzleClick: () -> Unit,
    onPreviousMoveClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextMoveClick: () -> Unit,
    onHintClick: () -> Unit,
    onSurrenderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = onShowSolutionClick) { Text(if (showSolutionPath) "Sakrij" else "Rešenje") }
            Button(onClick = onHintClick) { Text("Hint") }
            Button(onClick = onNextPuzzleClick) { Text("Sledeća") }
        }

        if (showSolutionPath) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Rešenje iz JSON-a:", style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onPreviousMoveClick, enabled = solutionMoveIndex > 0) { Text("<<") }
                Button(onClick = onPlayPauseClick, enabled = currentProblem?.solution?.moves?.isNotEmpty() == true) { Text(if (isPlayingSolution) "||" else "Play") }
                Button(onClick = onNextMoveClick, enabled = currentProblem?.solution?.moves?.isNotEmpty() == true && solutionMoveIndex < (currentProblem.solution.moves.size)) { Text(">>") }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSurrenderClick, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Predajem se") }
    }
}

@Composable
fun ChessBoardComposable(
    board: Board,
    selectedSquare: Square?,
    onSquareClick: (Square) -> Unit,
    highlightedHintMove: Move?,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier
        .background(Color.DarkGray)
        .aspectRatio(1f)) {
        val squareSize = this.maxWidth / 8
        Column {
            for (rank in 7 downTo 0) {
                Row {
                    for (file in 0..7) {
                        val square = Square.fromCoordinates(file, rank)
                        val piece = board.getPiece(square)
                        val backgroundColor = if ((file + rank) % 2 == 0) Color(0xFFEEEED2) else Color(0xFF769656)

                        val isHintStart = highlightedHintMove?.start == square
                        val isHintEnd = highlightedHintMove?.end == square
                        val hintColor = Color.Cyan.copy(alpha = 0.7f)

                        val finalBackgroundColor = when {
                            isHintStart || isHintEnd -> hintColor
                            square == selectedSquare -> Color.Yellow.copy(alpha = 0.6f)
                            else -> backgroundColor
                        }

                        Box(
                            modifier = Modifier
                                .size(squareSize)
                                .background(finalBackgroundColor)
                                .border(
                                    width = if (isHintStart || isHintEnd) 2.dp else 0.dp,
                                    color = if (isHintStart || isHintEnd) Color.Blue else Color.Transparent
                                )
                                .clickable { onSquareClick(square) },
                            contentAlignment = Alignment.Center
                        ) {
                            piece?.let {
                                val drawableResId = getPieceDrawableResId(it)
                                Image(painter = painterResource(id = drawableResId), contentDescription = "${it.color} ${it.type}", modifier = Modifier.fillMaxSize(0.9f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DefendedSquareDialog(board: Board, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Branjeno Polje!", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                Text("Ne možete stati na polje koje napada protivnička figura.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 16.dp))
                ChessBoardComposable(board = board, onSquareClick = {}, selectedSquare = null, highlightedHintMove = null, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Pokušaj ponovo") }
            }
        }
    }
}

@SuppressLint("DiscouragedApi")
@Composable
fun getPieceDrawableResId(piece: Piece): Int {
    val context = LocalContext.current
    val colorPrefix = if (piece.color == ChessColor.WHITE) "w" else "b"
    val typeSuffix = when (piece.type) {
        PieceType.PAWN -> "p"
        PieceType.KNIGHT -> "n"
        PieceType.BISHOP -> "b"
        PieceType.ROOK -> "r"
        PieceType.QUEEN -> "q"
        PieceType.KING -> "k"
    }
    val resourceName = "${colorPrefix}${typeSuffix}"
    return context.resources.getIdentifier(resourceName, "drawable", context.packageName)
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740, name = "Portrait Preview")
@Composable
fun PreviewChessScreenPortrait() {
    BrainTrainerTheme(
        appSettings = AppSettings(
            isSoundEnabled = true,
            appTheme = SettingsManager.AppTheme.SYSTEM,
            isPremiumUser = false
        )
    ) {
        ChessScreen(module = Module.Module2, difficulty = Difficulty.EASY, onGameFinished = {})
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 480, name = "Landscape Preview")
@Composable
fun PreviewChessScreenLandscape() {
    BrainTrainerTheme(
        appSettings = AppSettings(
            isSoundEnabled = true,
            appTheme = SettingsManager.AppTheme.SYSTEM,
            isPremiumUser = false
        )
    ) {
        ChessScreen(module = Module.Module2, difficulty = Difficulty.EASY, onGameFinished = {})
    }
}
