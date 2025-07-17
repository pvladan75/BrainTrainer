package com.program.braintrainer

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.GameModeInfo
import com.program.braintrainer.chess.model.Module
import com.program.braintrainer.ui.screens.ChessScreen
import com.program.braintrainer.ui.theme.BrainTrainerTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gameModes = listOf(
            GameModeInfo(
                type = Module.Module1,
                title = "Ukloni sve crne figure",
                description = "Svaki potez - uklanja crnu figuru, bez straha da će te pojesti",
                color = Color(0xFFE57373), // Crvenkasta
                icon = R.drawable.ic_timer
            ),
            GameModeInfo(
                type = Module.Module2,
                title = "Ukloni sve crne figure bezbedno",
                description = "Ne smeš da staneš na polje koje crne figure napadaju",
                color = Color(0xFF64B5F6), // Plavkasta
                icon = R.drawable.ic_trap
            ),
            GameModeInfo(
                type = Module.Module3,
                title = "Pojedi crnog kralja",
                description = "Pazi da ne staneš na polje koje crne figure napadaju",
                color = Color(0xFF81C784), // Zelenkasta
                icon = R.drawable.ic_analysis
            )
        )

        setContent {
            BrainTrainerTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "main_screen") {
                    composable("main_screen") {
                        MainScreen(
                            gameModes = gameModes,
                            onModeAndDifficultySelected = { selectedMode, selectedDifficulty ->
                                navController.navigate("chess_screen/${selectedMode.type.name}/${selectedDifficulty.name}")
                            }
                        )
                    }
                    composable("chess_screen/{moduleType}/{difficultyType}") { backStackEntry ->
                        val moduleTypeString = backStackEntry.arguments?.getString("moduleType")
                        val difficultyTypeString = backStackEntry.arguments?.getString("difficultyType")

                        val module = moduleTypeString?.let { Module.valueOf(it) }
                        val difficulty = difficultyTypeString?.let { Difficulty.valueOf(it) }

                        if (module != null && difficulty != null) {
                            ChessScreen(module = module, difficulty = difficulty) {
                                navController.popBackStack("main_screen", inclusive = false)
                            }
                        } else {
                            Text("Greška: Nevažeći parametri igre.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    gameModes: List<GameModeInfo>,
    onModeAndDifficultySelected: (GameModeInfo, Difficulty) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // --- PROVERA ORIJENTACIJE ---
        val configuration = LocalConfiguration.current

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            WelcomeHeader()
            Spacer(modifier = Modifier.height(32.dp))

            // --- KONDICIONALNI PRIKAZ NA OSNOVU ORIJENTACIJE ---
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // LANDSCAPE: Horizontalni slajder
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(gameModes) { mode ->
                        GameModeCard(
                            modifier = Modifier.width(320.dp), // Fiksna širina za landscape
                            gameMode = mode,
                            onDifficultySelected = { difficulty ->
                                onModeAndDifficultySelected(mode, difficulty)
                            }
                        )
                    }
                }
            } else {
                // PORTRAIT: Vertikalna lista
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(gameModes) { mode ->
                        GameModeCard(
                            modifier = Modifier.fillMaxWidth(), // Puna širina za portrait
                            gameMode = mode,
                            onDifficultySelected = { difficulty ->
                                onModeAndDifficultySelected(mode, difficulty)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "♟ Vežbe uma kroz razonodu!",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tri vežbe na tri nivoa težine - treniraj um kroz zabavu",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun GameModeCard(
    modifier: Modifier = Modifier, // Modifikator se sada prosleđuje
    gameMode: GameModeInfo,
    onDifficultySelected: (Difficulty) -> Unit
) {
    Card(
        modifier = modifier, // Koristi se prosleđeni modifikator
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Gornji deo sa informacijama
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = gameMode.icon),
                        contentDescription = gameMode.title,
                        tint = gameMode.color,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = gameMode.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = gameMode.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Donji deo sa dugmićima za težinu
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                Text(
                    text = "Izaberi težinu:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Difficulty.entries.forEach { difficulty ->
                        OutlinedButton(onClick = { onDifficultySelected(difficulty) }) {
                            Text(text = difficulty.label)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "Portrait")
@Composable
fun MainScreenPreviewPortrait() {
    BrainTrainerTheme {
        MainScreen(
            gameModes = listOf(
                GameModeInfo(Module.Module1, "Modul 1", "Opis 1", Color.Red, R.drawable.ic_timer)
            ),
            onModeAndDifficultySelected = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 480, name = "Landscape")
@Composable
fun MainScreenPreviewLandscape() {
    BrainTrainerTheme {
        MainScreen(
            gameModes = listOf(
                GameModeInfo(Module.Module1, "Modul 1", "Opis 1", Color.Red, R.drawable.ic_timer)
            ),
            onModeAndDifficultySelected = { _, _ -> }
        )
    }
}