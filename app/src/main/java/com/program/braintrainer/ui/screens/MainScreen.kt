package com.program.braintrainer.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.program.braintrainer.R
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.GameModeInfo
import com.program.braintrainer.chess.model.Module
import com.program.braintrainer.gamification.RankManager
import com.program.braintrainer.score.ScoreManager

@Composable
private fun getLocalizedDifficultyLabel(difficulty: Difficulty): String {
    return when (difficulty) {
        Difficulty.EASY -> stringResource(id = R.string.difficulty_easy)
        Difficulty.MEDIUM -> stringResource(id = R.string.difficulty_medium)
        Difficulty.HARD -> stringResource(id = R.string.difficulty_hard)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    gameModes: List<GameModeInfo>,
    onModeAndDifficultySelected: (Module, Difficulty) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAchievements: () -> Unit
) {
    val context = LocalContext.current
    val scoreManager = remember { ScoreManager(context) }
    val currentRank = remember { RankManager.getRankForXp(scoreManager.getTotalXp()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                actions = {
                    IconButton(onClick = onNavigateToAchievements) { Icon(Icons.Default.EmojiEvents, stringResource(id = R.string.content_desc_achievements)) }
                    IconButton(onClick = onNavigateToProfile) { Icon(Icons.Default.Person, stringResource(id = R.string.content_desc_profile)) }
                    IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, stringResource(id = R.string.content_desc_settings)) }
                }
            )
        }
    ) { paddingValues ->
        val configuration = LocalConfiguration.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WelcomeHeader()
            Spacer(modifier = Modifier.height(24.dp))

            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(gameModes) { mode ->
                        GameModeCard(
                            modifier = Modifier.width(320.dp),
                            gameMode = mode,
                            currentRank = currentRank,
                            onDifficultySelected = { difficulty ->
                                onModeAndDifficultySelected(mode.type, difficulty)
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(gameModes) { mode ->
                        GameModeCard(
                            modifier = Modifier.fillMaxWidth(),
                            gameMode = mode,
                            currentRank = currentRank,
                            onDifficultySelected = { difficulty ->
                                onModeAndDifficultySelected(mode.type, difficulty)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameModeCard(
    modifier: Modifier = Modifier,
    gameMode: GameModeInfo,
    currentRank: com.program.braintrainer.gamification.Rank,
    onDifficultySelected: (Difficulty) -> Unit
) {
    val availableDifficulties = remember(currentRank, gameMode.type) {
        RankManager.getAvailableDifficultiesFor(currentRank, gameMode.type)
    }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(
                    text = stringResource(id = R.string.main_select_difficulty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Difficulty.values().forEach { difficulty ->
                        val isEnabled = availableDifficulties.contains(difficulty)
                        OutlinedButton(
                            onClick = { onDifficultySelected(difficulty) },
                            enabled = isEnabled
                        ) {
                            Text(text = getLocalizedDifficultyLabel(difficulty))
                        }
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
            text = stringResource(id = R.string.main_welcome_header),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.main_welcome_subheader),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}