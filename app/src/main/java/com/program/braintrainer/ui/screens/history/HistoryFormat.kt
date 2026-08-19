package com.program.braintrainer.ui.screens.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.program.braintrainer.R
import com.program.braintrainer.stats.AttemptOutcome

/**
 * Trajanje u obliku koji se čita: sekunde do minuta, minuti do sata, pa sati.
 * Odvojeno od ekrana da bi granice mogle da se testiraju bez Android-a.
 */
internal fun durationParts(totalSeconds: Int): Triple<DurationUnit, Int, Int> {
    val seconds = totalSeconds.coerceAtLeast(0)
    return when {
        seconds < 60 -> Triple(DurationUnit.SECONDS, seconds, 0)
        seconds < 3600 -> Triple(DurationUnit.MINUTES, seconds / 60, seconds % 60)
        else -> Triple(DurationUnit.HOURS, seconds / 3600, (seconds % 3600) / 60)
    }
}

internal enum class DurationUnit { SECONDS, MINUTES, HOURS }

@Composable
internal fun formatDuration(totalSeconds: Int): String {
    val (unit, first, second) = durationParts(totalSeconds)
    return when (unit) {
        DurationUnit.SECONDS -> stringResource(R.string.duration_seconds, first)
        DurationUnit.MINUTES -> stringResource(R.string.duration_minutes, first, second)
        DurationUnit.HOURS -> stringResource(R.string.duration_hours, first, second)
    }
}

@Composable
internal fun outcomeLabel(outcome: AttemptOutcome): String = stringResource(
    when (outcome) {
        AttemptOutcome.SOLVED -> R.string.outcome_solved
        AttemptOutcome.SOLVED_WITH_HELP -> R.string.outcome_solved_with_help
        AttemptOutcome.SURRENDERED -> R.string.outcome_surrendered
        AttemptOutcome.FAILED -> R.string.outcome_failed
    }
)
