package com.program.braintrainer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.program.braintrainer.R
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.GameModeInfo
import com.program.braintrainer.chess.model.Module
import com.program.braintrainer.ui.screens.ChessScreen
import com.program.braintrainer.ui.screens.HighScoresScreen
import com.program.braintrainer.ui.screens.MainScreen
import com.program.braintrainer.ui.screens.settings.SettingsScreen
import com.program.braintrainer.ui.screens.settings.SettingsViewModelFactory

/**
 * Objekat koji sadrži konstante za navigacione rute.
 * Ovo sprečava greške u kucanju i olakšava održavanje.
 */
object Routes {
    const val MAIN_MENU = "main_menu"
    const val SETTINGS = "settings"
    const val HIGH_SCORES = "high_scores"
    // Definišemo rutu sa placeholder-ima za argumente
    const val CHESS_GAME = "chess_game/{moduleType}/{difficultyType}"

    /**
     * Pomoćna funkcija za kreiranje rute do ekrana igre sa konkretnim vrednostima.
     * @param module Izabrani modul igre.
     * @param difficulty Izabrana težina.
     * @return String koji predstavlja kompletnu rutu, npr. "chess_game/Module1/EASY".
     */
    fun createChessGameRoute(module: Module, difficulty: Difficulty): String {
        return "chess_game/${module.name}/${difficulty.name}"
    }
}

/**
 * Glavna Composable funkcija koja upravlja celokupnom navigacijom u aplikaciji.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Lista modova igre, koja se prosleđuje glavnom ekranu.
    // Definisana je ovde da bi bila na centralnom mestu.
    val gameModes = listOf(
        GameModeInfo(
            type = Module.Module1,
            title = Module.Module1.title,
            description = "Crne figure spavaju, ali u svakom potezu - ukloni je bez straha da će te pojesti",
            color = Color(0xFFE57373), // Crvenkasta
            icon = R.drawable.ic_module1_target
        ),
        GameModeInfo(
            type = Module.Module2,
            title = Module.Module2.title,
            description = "Crne figure vrebaju- izbegavaj polja koje crne figure napadaju",
            color = Color(0xFF64B5F6), // Plavkasta
            icon = R.drawable.ic_module2_shield
        ),
        GameModeInfo(
            type = Module.Module3,
            title = Module.Module3.title,
            description = "Ukloni sve branioce crnog kralja, pa i njega ukloni. Pazi, crne figure vrebaju ",
            color = Color(0xFF81C784), // Zelenkasta
            icon = R.drawable.ic_module3_king
        )
    )

    // NavHost je kontejner koji prikazuje trenutno aktivni ekran.
    NavHost(navController = navController, startDestination = Routes.MAIN_MENU) {

        // Definicija ekrana za Glavni Meni
        composable(Routes.MAIN_MENU) {
            MainScreen(
                gameModes = gameModes,
                onModeAndDifficultySelected = { module, difficulty ->
                    // Kada korisnik izabere mod i težinu, navigiramo na ekran igre
                    navController.navigate(Routes.createChessGameRoute(module, difficulty))
                },
                onNavigateToSettings = {
                    // Navigacija na ekran sa podešavanjima
                    navController.navigate(Routes.SETTINGS)
                },
                onNavigateToHighScores = {
                    // Navigacija na ekran sa rezultatima
                    navController.navigate(Routes.HIGH_SCORES)
                }
            )
        }

        // Definicija ekrana za Podešavanja
        composable(Routes.SETTINGS) {
            SettingsScreen(
                // Kreiramo ViewModel koristeći našu fabriku
                viewModel = viewModel(factory = SettingsViewModelFactory(context)),
                // Prosleđujemo funkciju za povratak na prethodni ekran
                onBackPress = { navController.popBackStack() }
            )
        }

        // Definicija ekrana za Igru (Šah)
        composable(Routes.CHESS_GAME) { backStackEntry ->
            // Čitamo prosleđene argumente (modul i težinu) iz rute
            val moduleType = backStackEntry.arguments?.getString("moduleType")?.let { Module.valueOf(it) }
            val difficultyType = backStackEntry.arguments?.getString("difficultyType")?.let { Difficulty.valueOf(it) }

            // Prikazujemo ChessScreen samo ako su argumenti validni
            if (moduleType != null && difficultyType != null) {
                ChessScreen(
                    module = moduleType,
                    difficulty = difficultyType,
                    onGameFinished = {
                        // Kada se igra završi, vraćamo se na glavni meni
                        navController.popBackStack(Routes.MAIN_MENU, inclusive = false)
                    }
                )
            }
        }

        // Definicija ekrana za Najbolje Rezultate
        composable(Routes.HIGH_SCORES) {
            HighScoresScreen(navController = navController)
        }
    }
}
