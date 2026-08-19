package com.program.braintrainer.ui.screens.history

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.program.braintrainer.R
import com.program.braintrainer.stats.DayTotals
import com.program.braintrainer.stats.GroupSummary
import com.program.braintrainer.stats.PuzzleAttempt
import com.program.braintrainer.ui.difficultyLabel
import com.program.braintrainer.ui.moduleTitle
import java.text.DateFormat
import java.util.Date

/**
 * Istorija napretka: dnevna aktivnost, zbir po modulima i poslednje zagonetke.
 * Sve dolazi iz lokalne baze pokušaja — ništa ne ide na mrežu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBackPress: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

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
                title = { Text(stringResource(R.string.history_title)) },
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
            state.isLoading -> Centered(Modifier.padding(padding)) { CircularProgressIndicator() }

            !state.isPremium -> LockedNotice(Modifier.padding(padding), onOpenSettings)

            !state.hasAnything -> Centered(Modifier.padding(padding)) {
                Text(
                    text = stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }

            else -> HistoryContent(state = state, modifier = Modifier.padding(padding))
        }
    }
}

@Composable
private fun HistoryContent(state: HistoryUiState, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { TotalsCard(state) }
        item { ActivityCard(state.days) }

        item {
            Text(
                text = stringResource(R.string.history_section_modules),
                style = MaterialTheme.typography.titleMedium
            )
        }
        items(state.summaries) { group -> GroupCard(group) }

        item {
            Text(
                text = stringResource(R.string.history_section_recent),
                style = MaterialTheme.typography.titleMedium
            )
        }
        items(state.recent) { attempt -> AttemptRow(attempt) }
    }
}

@Composable
private fun TotalsCard(state: HistoryUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.history_total_played, state.totalAttempts),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.history_total_solved, state.totalSolved),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.history_total_time, formatDuration(state.totalSeconds)),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun ActivityCard(days: List<DayTotals>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.history_section_activity),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(12.dp))
            ActivityChart(
                days = days,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.history_chart_max,
                    days.maxOfOrNull { it.attempts } ?: 0
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.history_chart_legend),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Stubić po danu: ceo stubić je odigrano, puni deo rešeno. Namerno bez brojeva
 * na osi — grafik odgovara na „da li vežbam redovno", ne na „koliko tačno".
 */
@Composable
private fun ActivityChart(days: List<DayTotals>, modifier: Modifier = Modifier) {
    val solvedColor = MaterialTheme.colorScheme.primary
    val playedColor = MaterialTheme.colorScheme.surfaceVariant
    val maxAttempts = (days.maxOfOrNull { it.attempts } ?: 0).coerceAtLeast(1)

    Canvas(modifier = modifier) {
        if (days.isEmpty()) return@Canvas

        val gap = size.width / (days.size * 5f)
        val barWidth = (size.width - gap * (days.size + 1)) / days.size

        days.forEachIndexed { index, day ->
            val left = gap + index * (barWidth + gap)
            val playedHeight = size.height * day.attempts / maxAttempts
            val solvedHeight =
                if (day.attempts == 0) 0f else playedHeight * day.solved / day.attempts

            // Prazan dan ostaje vidljiv kao tanka crta u dnu, da se pauza vidi.
            val baseHeight = if (day.attempts == 0) 2f else playedHeight

            drawRect(
                color = playedColor,
                topLeft = Offset(left, size.height - baseHeight),
                size = Size(barWidth, baseHeight)
            )
            if (solvedHeight > 0f) {
                drawRect(
                    color = solvedColor,
                    topLeft = Offset(left, size.height - solvedHeight),
                    size = Size(barWidth, solvedHeight)
                )
            }
        }
    }
}

@Composable
private fun GroupCard(group: GroupSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = moduleTitle(group.module),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = difficultyLabel(group.difficulty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.history_group_solved,
                    group.summary.solved,
                    group.summary.attempts
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.history_group_perfect, group.summary.perfect),
                style = MaterialTheme.typography.bodySmall
            )
            group.summary.bestSeconds?.let { best ->
                Text(
                    text = stringResource(R.string.history_group_best, formatDuration(best)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun AttemptRow(attempt: PuzzleAttempt) {
    val dateFormat = remember {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = moduleTitle(attempt.module) + " · " + difficultyLabel(attempt.difficulty),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = outcomeLabel(attempt.outcome),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatDuration(attempt.elapsedSeconds),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = dateFormat.format(Date(attempt.finishedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
    }
}

@Composable
private fun LockedNotice(modifier: Modifier = Modifier, onOpenSettings: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.premium_locked_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.history_locked_description),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onOpenSettings) {
            Text(stringResource(R.string.premium_locked_action))
        }
    }
}

@Composable
private fun Centered(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) { content() }
}
