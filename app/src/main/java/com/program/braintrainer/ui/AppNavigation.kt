package com.program.braintrainer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.program.braintrainer.R
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.GameModeInfo
import com.program.braintrainer.chess.model.Module
import com.program.braintrainer.gamification.AchievementsViewModelFactory
import com.program.braintrainer.gamification.ProfileViewModelFactory
import com.program.braintrainer.ui.screens.*
import com.program.braintrainer.ui.screens.chess.ChessScreen
import com.program.braintrainer.ui.screens.chess.ChessViewModelFactory
import com.program.braintrainer.ui.screens.history.HistoryScreen
import com.program.braintrainer.ui.screens.history.HistoryViewModelFactory
import com.program.braintrainer.ui.screens.mistakes.MistakesScreen
import com.program.braintrainer.ui.screens.mistakes.MistakesViewModelFactory
import com.program.braintrainer.ui.screens.settings.SettingsScreen
import com.program.braintrainer.ui.screens.settings.SettingsViewModelFactory

/**
 * Objekat koji sadrži konstante za navigacione rute.
 */
object Routes {
    const val MAIN_MENU = "main_menu"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val ACHIEVEMENTS = "achievements"
    const val MISTAKES = "mistakes"
    const val HISTORY = "history"
    const val CHESS_GAME = "chess_game/{moduleType}/{difficultyType}?puzzleIds={puzzleIds}"

    /**
     * Ruta do ekrana igre. [puzzleIds] je prazno za običnu sesiju, a popunjeno
     * kad se vežbaju baš određene zagonetke (revanš iz dnevnika grešaka).
     */
    fun createChessGameRoute(
        module: Module,
        difficulty: Difficulty,
        puzzleIds: List<String> = emptyList()
    ): String {
        return "chess_game/${module.name}/${difficulty.name}?puzzleIds=${puzzleIds.joinToString(",")}"
    }
}

/**
 * Glavna Composable funkcija koja upravlja celokupnom navigacijom u aplikaciji.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    val gameModes = listOf(
        GameModeInfo(
            type = Module.Module1,
            title = moduleTitle(Module.Module1),
            description = stringResource(R.string.modul1desc),
            color = Color(0xFFE57373),
            icon = R.drawable.ic_module1_target
        ),
        GameModeInfo(
            type = Module.Module2,
            title = moduleTitle(Module.Module2),
            description = stringResource(R.string.modul2desc),
            color = Color(0xFF64B5F6),
            icon = R.drawable.ic_module2_shield
        ),
        GameModeInfo(
            type = Module.Module3,
            title = moduleTitle(Module.Module3),
            description = stringResource(R.string.modul3desc),
            color = Color(0xFF81C784),
            icon = R.drawable.ic_module3_king
        )
    )

    NavHost(navController = navController, startDestination = Routes.MAIN_MENU) {

        composable(Routes.MAIN_MENU) {
            MainScreen(
                gameModes = gameModes,
                onModeAndDifficultySelected = { module, difficulty ->
                    navController.navigate(Routes.createChessGameRoute(module, difficulty))
                },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToMistakes = { navController.navigate(Routes.MISTAKES) },
                onNavigateToProfile = { navController.navigate(Routes.PROFILE) },
                onNavigateToAchievements = { navController.navigate(Routes.ACHIEVEMENTS) }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel(factory = SettingsViewModelFactory(context)),
                onBackPress = { navController.popBackStack() }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                viewModel = viewModel(factory = ProfileViewModelFactory(context)),
                onBackPress = { navController.popBackStack() },
                onNavigateToHistory = { navController.navigate(Routes.HISTORY) }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                viewModel = viewModel(factory = HistoryViewModelFactory(context)),
                onBackPress = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.ACHIEVEMENTS) {
            // ISPRAVKA: Uklonjen je 'viewModel' parametar iz poziva.
            // AchievementsScreen sada sam kreira svoj ViewModel.
            AchievementsScreen(
                navController = navController
            )
        }

        composable(Routes.MISTAKES) {
            MistakesScreen(
                viewModel = viewModel(factory = MistakesViewModelFactory(context)),
                onBackPress = { navController.popBackStack() },
                onPractice = { module, difficulty, puzzleIds ->
                    navController.navigate(Routes.createChessGameRoute(module, difficulty, puzzleIds))
                },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            Routes.CHESS_GAME,
            // Bez podrazumevane vrednosti ruta bez upitnog dela ne bi bila pogođena.
            arguments = listOf(
                navArgument("puzzleIds") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val moduleType = backStackEntry.arguments?.getString("moduleType")?.let { Module.valueOf(it) }
            val difficultyType = backStackEntry.arguments?.getString("difficultyType")?.let { Difficulty.valueOf(it) }
            val puzzleIds = backStackEntry.arguments?.getString("puzzleIds")
                ?.split(",")
                ?.filter { it.isNotBlank() }
                .orEmpty()

            if (moduleType != null && difficultyType != null) {
                ChessScreen(
                    module = moduleType,
                    difficulty = difficultyType,
                    viewModel = viewModel(
                        factory = ChessViewModelFactory(context, moduleType, difficultyType, puzzleIds)
                    ),
                    onGameFinished = {
                        // Nazad na ekran sa kog je partija pokrenuta — glavni meni
                        // ili dnevnik grešaka, koji se time i osveži.
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}