package com.program.braintrainer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.program.braintrainer.R
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.GameModeInfo
import com.program.braintrainer.chess.model.Module
import com.program.braintrainer.gamification.AchievementsViewModelFactory
import com.program.braintrainer.gamification.ProfileViewModelFactory
import com.program.braintrainer.ui.screens.*
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
    const val CHESS_GAME = "chess_game/{moduleType}/{difficultyType}"

    /**
     * Pomoćna funkcija za kreiranje rute do ekrana igre sa konkretnim vrednostima.
     */
    fun createChessGameRoute(module: Module, difficulty: Difficulty): String {
        return "chess_game/${module.name}/${difficulty.name}"
    }
}

/**
 * Pomoćna funkcija za dobijanje prevedenih naziva modula.
 */
@Composable
private fun getLocalizedModuleTitle(module: Module): String {
    return when (module) {
        Module.Module1 -> stringResource(id = R.string.module_1_title)
        Module.Module2 -> stringResource(id = R.string.module_2_title)
        Module.Module3 -> stringResource(id = R.string.module_3_title)
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
            title = getLocalizedModuleTitle(module = Module.Module1), // IZMENA
            description = stringResource(R.string.modul1desc),
            color = Color(0xFFE57373),
            icon = R.drawable.ic_module1_target
        ),
        GameModeInfo(
            type = Module.Module2,
            title = getLocalizedModuleTitle(module = Module.Module2), // IZMENA
            description = stringResource(R.string.modul2desc),
            color = Color(0xFF64B5F6),
            icon = R.drawable.ic_module2_shield
        ),
        GameModeInfo(
            type = Module.Module3,
            title = getLocalizedModuleTitle(module = Module.Module3), // IZMENA
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
                onBackPress = { navController.popBackStack() }
            )
        }

        composable(Routes.ACHIEVEMENTS) {
            // ISPRAVKA: Uklonjen je 'viewModel' parametar iz poziva.
            // AchievementsScreen sada sam kreira svoj ViewModel.
            AchievementsScreen(
                navController = navController
            )
        }

        composable(Routes.CHESS_GAME) { backStackEntry ->
            val moduleType = backStackEntry.arguments?.getString("moduleType")?.let { Module.valueOf(it) }
            val difficultyType = backStackEntry.arguments?.getString("difficultyType")?.let { Difficulty.valueOf(it) }

            if (moduleType != null && difficultyType != null) {
                ChessScreen(
                    module = moduleType,
                    difficulty = difficultyType,
                    onGameFinished = {
                        navController.popBackStack(Routes.MAIN_MENU, inclusive = false)
                    }
                )
            }
        }
    }
}