package com.program.braintrainer.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.program.braintrainer.gamification.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBackPress: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Moj Profil i Rang") },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Nazad")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Prikazujemo sadržaj samo ako su podaci učitani
        uiState?.let { state ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Ikonica i naziv ranga
                Image(
                    painter = painterResource(id = state.currentRank.iconResId),
                    contentDescription = "Ikonica ranga: ${state.currentRank.title}",
                    modifier = Modifier.size(120.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = state.currentRank.title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ukupno XP: ${state.totalXp}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))

                // Progress bar i informacije o sledećem rangu
                if (state.nextRank != null) {
                    Text(
                        text = "Napredak do sledećeg ranga:",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Animirani progress bar
                    val progressAnimation by animateFloatAsState(
                        targetValue = state.xpProgress,
                        label = "XP Progress Animation"
                    )
                    LinearProgressIndicator(
                        progress = { progressAnimation },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(12.dp)
                            .clip(MaterialTheme.shapes.extraLarge),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${state.totalXp} / ${state.xpForNextRank} XP",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Sledeći rang: ${state.nextRank.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    // Poruka za igrače na najvišem rangu
                    Text(
                        text = "🎉 Čestitamo! Dostigli ste najviši rang! 🎉",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } ?: Box( // Prikazujemo indikator učitavanja dok se podaci ne spreme
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
