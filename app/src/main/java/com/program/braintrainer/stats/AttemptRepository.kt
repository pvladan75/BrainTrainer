package com.program.braintrainer.stats

import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Jedini put do istorije odigranih zagonetki.
 *
 * Ekrani i ViewModel-i rade sa modelima iz [PuzzleAttempt], a ne sa Room
 * tipovima — da baza ne procuri u UI i da se kasnije može zameniti bez diranja
 * pozivalaca.
 */
class AttemptRepository(
    private val dao: AttemptDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun record(attempt: PuzzleAttempt) = withContext(ioDispatcher) {
        dao.insert(attempt.toEntity())
        Unit
    }

    suspend fun count(): Int = withContext(ioDispatcher) { dao.count() }

    suspend fun recent(limit: Int = DEFAULT_LIMIT): List<PuzzleAttempt> = withContext(ioDispatcher) {
        dao.recent(limit).mapNotNull { it.toAttempt() }
    }

    /** Dnevni zbir za poslednjih [days] dana, računato od početka današnjeg dana. */
    suspend fun dailyTotals(days: Int, now: Long = System.currentTimeMillis()): List<DayTotals> =
        withContext(ioDispatcher) {
            val since = now - days.toLong() * MILLIS_PER_DAY
            dao.dailyTotals(since).map {
                DayTotals(
                    day = it.day,
                    attempts = it.attempts,
                    solved = it.solved,
                    earnedXp = it.earnedXp,
                    seconds = it.seconds
                )
            }
        }

    /** Zagonetke koje čekaju revanš — poslednji pokušaj im nije bio uspešan. */
    suspend fun openMistakes(limit: Int = DEFAULT_LIMIT): List<OpenMistake> =
        withContext(ioDispatcher) {
            dao.openMistakes(limit).mapNotNull { row ->
                val module = enumOrNull<Module>(row.module) ?: return@mapNotNull null
                val difficulty = enumOrNull<Difficulty>(row.difficulty) ?: return@mapNotNull null

                OpenMistake(
                    puzzleId = row.puzzleId,
                    module = module,
                    difficulty = difficulty,
                    lastAttemptAt = row.lastAttemptAt,
                    attempts = row.attempts
                )
            }
        }

    /** Zbir za svaku kombinaciju modula i težine koja ima bar jedan pokušaj. */
    suspend fun summaries(): List<GroupSummary> = withContext(ioDispatcher) {
        dao.summaries().mapNotNull { row ->
            val module = enumOrNull<Module>(row.module) ?: return@mapNotNull null
            val difficulty = enumOrNull<Difficulty>(row.difficulty) ?: return@mapNotNull null

            GroupSummary(
                module = module,
                difficulty = difficulty,
                summary = ModuleSummary(
                    attempts = row.attempts,
                    solved = row.solved,
                    perfect = row.perfect,
                    bestSeconds = row.bestSeconds,
                    totalSeconds = row.totalSeconds
                )
            )
        }
    }

    suspend fun summary(module: Module, difficulty: Difficulty): ModuleSummary =
        withContext(ioDispatcher) {
            val row = dao.summary(module.name, difficulty.name)
            ModuleSummary(
                attempts = row.attempts,
                solved = row.solved,
                perfect = row.perfect,
                bestSeconds = row.bestSeconds,
                totalSeconds = row.totalSeconds
            )
        }

    private companion object {
        const val DEFAULT_LIMIT = 50
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    }
}
