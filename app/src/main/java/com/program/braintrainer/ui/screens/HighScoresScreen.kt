package com.program.braintrainer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module
import com.program.braintrainer.score.ScoreManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighScoresScreen(navController: NavController) {
    val context = LocalContext.current
    // 'remember' osigurava da se ScoreManager i lista rezultata ne kreiraju ponovo pri svakoj rekompoziciji
    val scoreManager = remember { ScoreManager(context) }
    val highScores = remember { scoreManager.getAllHighScores() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Najbolji Rezultati") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Nazad")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Iteriramo kroz mapu rezultata. 'items' je ispravan način za prikazivanje liste u LazyColumn.
            items(highScores.entries.toList()) { (module, difficultyScores) ->
                ModuleScoreCard(module = module, scores = difficultyScores)
            }
        }
    }
}

@Composable
fun ModuleScoreCard(module: Module, scores: Map<Difficulty, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = module.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Prikazujemo rezultate za svaku težinu
            scores.entries.forEach { (difficulty, score) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = difficulty.label, // Ispravljeno sa 'name' na 'label'
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "$score XP",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
