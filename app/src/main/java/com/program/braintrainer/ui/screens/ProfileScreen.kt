package com.program.braintrainer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.program.braintrainer.gamification.AchievementProgress
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
        uiState?.let { state ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Zaglavlje sa rangom i XP poenima
                item {
                    RankHeader(
                        rankTitle = state.currentRank.title,
                        totalXp = state.totalXp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Sekcija sa uslovima za sledeći rang
                if (state.nextRank != null) {
                    item {
                        Text(
                            text = "Uslovi za rang: ${state.nextRank.title}",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Divider()
                    }

                    // Prikaz potrebnih XP poena
                    item {
                        RequirementItem(
                            description = "Sakupi ${state.nextRank.requiredXp} XP poena",
                            isCompleted = state.totalXp >= state.nextRank.requiredXp,
                            progressText = "${state.totalXp} / ${state.nextRank.requiredXp}"
                        )
                    }

                    // Prikaz potrebnih dostignuća
                    items(state.requirementsForNextRank) { achievementProgress ->
                        RequirementItem(
                            description = achievementProgress.achievement.description,
                            isCompleted = achievementProgress.isUnlocked,
                            progressText = "${achievementProgress.currentProgress} / ${achievementProgress.target}"
                        )
                    }
                } else {
                    item {
                        Text(
                            text = "🎉 Čestitamo! Dostigli ste najviši rang! 🎉",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun RankHeader(rankTitle: String, totalXp: Int) {
    // Ikonica je za sada uklonjena
    Text(
        text = rankTitle,
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Ukupno XP: $totalXp",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun RequirementItem(
    description: String,
    isCompleted: Boolean,
    progressText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = if (isCompleted) "Završeno" else "Nije završeno",
            tint = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = description,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        // Prikazujemo napredak samo ako zadatak nije završen
        if (!isCompleted) {
            Text(
                text = progressText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider()
}
