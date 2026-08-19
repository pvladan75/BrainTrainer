package com.program.braintrainer.ui.screens.mistakes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.program.braintrainer.R
import com.program.braintrainer.ui.components.PremiumLockedNotice
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module
import com.program.braintrainer.ui.difficultyLabel
import com.program.braintrainer.ui.moduleTitle

/**
 * Dnevnik grešaka — zagonetke čiji poslednji pokušaj nije bio uspešan, spremne
 * za revanš. Sesija se pravi po modulu i težini, jer pravila i bodovanje ne
 * mogu da se mešaju unutar jedne partije.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MistakesScreen(
    viewModel: MistakesViewModel,
    onBackPress: () -> Unit,
    onPractice: (Module, Difficulty, List<String>) -> Unit,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    // Revanš menja spisak, pa se po povratku sa partije čita ponovo.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.mistakes_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_desc_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        when {
            state.isLoading -> CenteredColumn(Modifier.padding(padding)) {
                CircularProgressIndicator()
            }

            !state.isPremium -> PremiumLockedNotice(
                introText = stringResource(R.string.mistakes_locked_description),
                onOpenSettings = onOpenSettings,
                modifier = Modifier.padding(padding)
            )

            state.groups.isEmpty() -> CenteredColumn(Modifier.padding(padding)) {
                Text(
                    text = stringResource(R.string.mistakes_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = pluralStringResource(
                            R.plurals.mistakes_open_count,
                            state.totalOpen,
                            state.totalOpen
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                items(state.groups) { group ->
                    MistakeGroupCard(
                        group = group,
                        onPractice = { onPractice(group.module, group.difficulty, group.puzzleIds) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MistakeGroupCard(group: MistakeGroup, onPractice: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = moduleTitle(group.module),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = difficultyLabel(group.difficulty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = pluralStringResource(
                        R.plurals.mistakes_waiting,
                        group.puzzleIds.size,
                        group.puzzleIds.size
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(onClick = onPractice) { Text(stringResource(R.string.mistakes_practice)) }
        }
    }
}


@Composable
private fun CenteredColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) { content() }
}
