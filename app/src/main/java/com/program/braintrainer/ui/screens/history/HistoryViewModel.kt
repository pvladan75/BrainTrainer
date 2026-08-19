package com.program.braintrainer.ui.screens.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.program.braintrainer.brainTrainerApp
import com.program.braintrainer.chess.model.data.SettingsManager
import com.program.braintrainer.stats.AttemptRepository
import com.program.braintrainer.stats.DayTotals
import com.program.braintrainer.stats.GroupSummary
import com.program.braintrainer.stats.PuzzleAttempt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class HistoryUiState(
    val isLoading: Boolean = true,
    val isPremium: Boolean = false,
    val days: List<DayTotals> = emptyList(),
    val summaries: List<GroupSummary> = emptyList(),
    val recent: List<PuzzleAttempt> = emptyList()
) {
    val hasAnything: Boolean get() = summaries.isNotEmpty()
    val totalAttempts: Int get() = summaries.sumOf { it.summary.attempts }
    val totalSolved: Int get() = summaries.sumOf { it.summary.solved }
    val totalSeconds: Int get() = summaries.sumOf { it.summary.totalSeconds }
}

/**
 * Istorija napretka: koliko je vežbano po danima, koliko je rešeno po modulu i
 * težini, i spisak poslednjih pokušaja.
 */
class HistoryViewModel(
    private val attemptRepository: AttemptRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val isPremium = settingsManager.settingsFlow.first().isPremiumUser
            val days = fillMissingDays(attemptRepository.dailyTotals(CHART_DAYS), CHART_DAYS)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isPremium = isPremium,
                    days = days,
                    summaries = attemptRepository.summaries(),
                    recent = attemptRepository.recent(RECENT_LIMIT)
                )
            }
        }
    }

    companion object {
        /** Dve nedelje staju na ekran a da stubići ostanu čitljivi. */
        const val CHART_DAYS = 14
        const val RECENT_LIMIT = 30
    }
}

/**
 * Dopunjava dane bez ijednog pokušaja nulama, da grafik ne sabije nedelju dana
 * pauze u jedan razmak. Vraća tačno [days] unosa, poslednji je današnji.
 */
internal fun fillMissingDays(
    totals: List<DayTotals>,
    days: Int,
    today: LocalDate = LocalDate.now()
): List<DayTotals> {
    val byDay = totals.associateBy { it.day }
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    return (days - 1 downTo 0).map { back ->
        val key = today.minusDays(back.toLong()).format(formatter)
        byDay[key] ?: DayTotals(day = key, attempts = 0, solved = 0, earnedXp = 0, seconds = 0)
    }
}

class HistoryViewModelFactory(context: Context) : ViewModelProvider.Factory {

    private val app = context.brainTrainerApp

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(app.attemptRepository, app.settingsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
