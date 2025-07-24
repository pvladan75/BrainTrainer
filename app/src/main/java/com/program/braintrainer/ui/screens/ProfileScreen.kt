package com.program.braintrainer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.program.braintrainer.R
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
                title = { Text(stringResource(id = R.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        // ISPRAVKA 1: Korišćenje AutoMirrored ikone
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.content_desc_back))
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
                item {
                    RankHeader(
                        rankTitle = state.currentRank.title,
                        totalXp = state.totalXp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                if (state.nextRank != null) {
                    item {
                        Text(
                            text = stringResource(id = R.string.profile_requirements_for_rank, state.nextRank.title),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        // ISPRAVKA 2: Divider je preimenovan u HorizontalDivider
                        HorizontalDivider()
                    }

                    item {
                        RequirementItem(
                            description = stringResource(id = R.string.profile_requirement_xp, state.nextRank.requiredXp),
                            isCompleted = state.totalXp >= state.nextRank.requiredXp,
                            progressText = stringResource(id = R.string.profile_progress_format, state.totalXp, state.nextRank.requiredXp)
                        )
                    }

                    items(state.requirementsForNextRank) { achievementProgress ->
                        RequirementItem(
                            description = achievementProgress.achievement.description,
                            isCompleted = achievementProgress.isUnlocked,
                            progressText = stringResource(id = R.string.profile_progress_format, achievementProgress.currentProgress, achievementProgress.target)
                        )
                    }
                } else {
                    item {
                        Text(
                            text = stringResource(id = R.string.profile_congrats_max_rank),
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
    Text(
        text = rankTitle,
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(id = R.string.profile_total_xp, totalXp),
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
            contentDescription = if (isCompleted) stringResource(id = R.string.content_desc_completed) else stringResource(id = R.string.content_desc_not_completed),
            tint = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = description,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        if (!isCompleted) {
            Text(
                text = progressText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    // ISPRAVKA 3: I ovde je Divider preimenovan u HorizontalDivider
    HorizontalDivider()
}