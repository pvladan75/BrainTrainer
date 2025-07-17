package com.program.braintrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- NOVI IMPORTI ZA NAVIGACIJU ---
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.runtime.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
// --- KRAJ NOVIH IMPORTI ZA NAVIGACIJU ---

import com.program.braintrainer.chess.model.Module
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.GameModeInfo

// ISPRAVLJEN IMPORT ZA BrainTrainerTheme - sada je direktno iz ui.theme
import com.program.braintrainer.ui.theme.BrainTrainerTheme

// ISPRAVLJEN IMPORT ZA ChessScreen - sada je iz ui.screens
import com.program.braintrainer.ui.screens.ChessScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gameModes = listOf(
            GameModeInfo(
                type = Module.Module1,
                title = "Brza Taktika",
                description = "Reši što više zagonetki u ograničenom vremenu.",
                color = Color(0xFFE57373), // Crvenkasta
                icon = R.drawable.ic_timer // VAŽNO: Moraš dodati ikonice u res/drawable folder
            ),
            GameModeInfo(
                type = Module.Module2,
                title = "Skrivena Zamka",
                description = "Pronađi najbolji potez u poziciji sa skrivenom pretnjom.",
                color = Color(0xFF64B5F6), // Plavkasta
                icon = R.drawable.ic_trap
            ),
            GameModeInfo(
                type = Module.Module3,
                title = "Analiza Poteza",
                description = "Analiziraj poziciju bez pritiska i odigraj savršen potez.",
                color = Color(0xFF81C784), // Zelenkasta
                icon = R.drawable.ic_analysis
            )
        )

        setContent {
            BrainTrainerTheme {
                // Inicijalizacija NavController-a
                val navController = rememberNavController()

                // NavHost definiše graf navigacije
                NavHost(navController = navController, startDestination = "main_screen") {
                    // Glavni ekran sa izborom modula
                    composable("main_screen") {
                        MainScreen(gameModes = gameModes) { selectedMode ->
                            // Kada se izabere mod, idemo na ekran za odabir težine,
                            // prosleđujući tip modula kao argument
                            navController.navigate("difficulty_selection_screen/${selectedMode.type.name}")
                        }
                    }

                    // Ekran za odabir težine
                    composable("difficulty_selection_screen/{moduleType}") { backStackEntry ->
                        val moduleTypeString = backStackEntry.arguments?.getString("moduleType")
                        val module = moduleTypeString?.let { Module.valueOf(it) }

                        // Prikazujemo dijalog za odabir težine
                        // Za demo, koristimo AlertDialog, ali može biti i poseban Composable ekran
                        if (module != null) {
                            DifficultySelectionDialog(
                                onDismiss = { navController.popBackStack() }, // Nazad na glavni ekran
                                onDifficultySelected = { difficulty ->
                                    // Kada se izabere težina, navigiramo na ChessScreen
                                    // Prosleđujemo i mod i težinu
                                    navController.navigate("chess_screen/${module.name}/${difficulty.name}")
                                },
                                module = module // Prosleđujemo modul dijalogu
                            )
                        } else {
                            // Upravljanje greškom ako modul nije pronađen
                            Text("Greška: Modul nije pronađen.")
                        }
                    }

                    // Ekran za igru šaha
                    composable("chess_screen/{moduleType}/{difficultyType}") { backStackEntry ->
                        val moduleTypeString = backStackEntry.arguments?.getString("moduleType")
                        val difficultyTypeString = backStackEntry.arguments?.getString("difficultyType")

                        val module = moduleTypeString?.let { Module.valueOf(it) }
                        val difficulty = difficultyTypeString?.let { Difficulty.valueOf(it) }

                        if (module != null && difficulty != null) {
                            ChessScreen(module = module, difficulty = difficulty) {
                                // Šta se dešava kada se igra završi, npr. povratak na glavni ekran
                                navController.popBackStack("main_screen", inclusive = false)
                            }
                        } else {
                            // Upravljanje greškom ako argumenti nedostaju
                            Text("Greška: Nevažeći parametri igre.")
                        }
                    }
                }
            }
        }
    }
}

// --- Glavne Komponente Ekrana (Composables) ---

@Composable
fun MainScreen(gameModes: List<GameModeInfo>, onModeSelected: (GameModeInfo) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            WelcomeHeader()
            Spacer(modifier = Modifier.height(32.dp))
            gameModes.forEach { mode ->
                GameModeCard(gameMode = mode, onPlayClicked = { onModeSelected(mode) })
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun WelcomeHeader() {
    Text(
        text = "♟ Dobrodošao u Šah Zagonetke!",
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Vežbaj taktiku kroz tri različita izazova – izaberi stil koji ti najviše odgovara.",
        fontSize = 16.sp,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun GameModeCard(gameMode: GameModeInfo, onPlayClicked: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = gameMode.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 56.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onPlayClicked,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(containerColor = gameMode.color)
            ) {
                Text(text = "Igraj", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- NOVO: Komponenta za odabir težine ---
@Composable
fun DifficultySelectionDialog(
    onDismiss: () -> Unit,
    onDifficultySelected: (Difficulty) -> Unit,
    module: Module // Prosleđujemo odabrani modul dijalogu
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Izaberi Težinu za ${module.title}",
                fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text("Odabrali ste ${module.title}. Sada izaberite nivo težine:")
                Spacer(modifier = Modifier.height(16.dp))
                // Lista svih težina iz enuma
                Difficulty.entries.forEach { difficulty ->
                    Button(
                        onClick = { onDifficultySelected(difficulty) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(text = difficulty.label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Odustani")
            }
        }
    )
}


// --- Pregled (Preview) za Android Studio ---

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
fun MainScreenPreview() {
    val previewGameModes = listOf(
        GameModeInfo(Module.Module1, "Brza Taktika", "Reši što više zagonetki u ograničenom vremenu.", Color(0xFFE57373), R.drawable.ic_timer),
        GameModeInfo(Module.Module2, "Skrivena Zamka", "Pronađi najbolji potez u poziciji sa skrivenom pretnjom.", Color(0xFF64B5F6), R.drawable.ic_trap),
        GameModeInfo(Module.Module3, "Analiza Poteza", "Analiziraj poziciju bez pritiska i odigraj savršen potez.", Color(0xFF81C784), R.drawable.ic_analysis)
    )
    BrainTrainerTheme {
        MainScreen(gameModes = previewGameModes, onModeSelected = {})
    }
}

@Preview(showBackground = true)
@Composable
fun DifficultySelectionDialogPreview() {
    BrainTrainerTheme {
        DifficultySelectionDialog(
            onDismiss = {},
            onDifficultySelected = {},
            module = Module.Module1
        )
    }
}