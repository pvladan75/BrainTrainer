package com.program.braintrainer.stats

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/** Red koji vraća upit za dnevni zbir. */
data class DayTotalsRow(
    val day: String,
    val attempts: Int,
    val solved: Int,
    val earnedXp: Int,
    val seconds: Int
)

/** Red koji vraća upit za otvorene greške. */
data class OpenMistakeRow(
    val puzzleId: String,
    val module: String,
    val difficulty: String,
    val lastAttemptAt: Long,
    val attempts: Int
)

/** Red koji vraća zbirni upit; `bestSeconds` je null dok nijedna nije rešena. */
data class SummaryRow(
    val attempts: Int,
    val solved: Int,
    val perfect: Int,
    val bestSeconds: Int?,
    val totalSeconds: Int
)

@Dao
interface AttemptDao {

    @Insert
    suspend fun insert(attempt: AttemptEntity): Long

    @Query("SELECT COUNT(*) FROM puzzle_attempts")
    suspend fun count(): Int

    @Query("SELECT * FROM puzzle_attempts ORDER BY finished_at DESC, id DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<AttemptEntity>

    /**
     * Dnevni zbir. Dan se računa u **lokalnoj** zoni telefona, jer igrač deli
     * vežbanje po svojim danima, a ne po UTC-u.
     */
    @Query(
        """
        SELECT strftime('%Y-%m-%d', finished_at / 1000, 'unixepoch', 'localtime') AS day,
               COUNT(*) AS attempts,
               SUM(CASE WHEN outcome = 'SOLVED' THEN 1 ELSE 0 END) AS solved,
               SUM(earned_xp) AS earnedXp,
               SUM(elapsed_seconds) AS seconds
        FROM puzzle_attempts
        WHERE finished_at >= :since
        GROUP BY day
        ORDER BY day
        """
    )
    suspend fun dailyTotals(since: Long): List<DayTotalsRow>

    /**
     * Zagonetke čiji je poslednji pokušaj neuspešan. Poređenje ide po `id`, ne
     * po vremenu: dva pokušaja iste zagonetke u istoj sekundi bi inače oba ušla.
     */
    @Query(
        """
        SELECT a.puzzle_id AS puzzleId,
               a.module AS module,
               a.difficulty AS difficulty,
               a.finished_at AS lastAttemptAt,
               (SELECT COUNT(*) FROM puzzle_attempts t WHERE t.puzzle_id = a.puzzle_id) AS attempts
        FROM puzzle_attempts a
        JOIN (
            SELECT puzzle_id, MAX(id) AS last_id FROM puzzle_attempts GROUP BY puzzle_id
        ) latest ON a.id = latest.last_id
        WHERE a.outcome <> 'SOLVED'
        ORDER BY a.finished_at DESC
        LIMIT :limit
        """
    )
    suspend fun openMistakes(limit: Int): List<OpenMistakeRow>

    @Query(
        """
        SELECT COUNT(*) AS attempts,
               SUM(CASE WHEN outcome = 'SOLVED' THEN 1 ELSE 0 END) AS solved,
               SUM(CASE WHEN outcome = 'SOLVED' AND mistakes = 0 AND player_moves <= optimal_moves
                        THEN 1 ELSE 0 END) AS perfect,
               MIN(CASE WHEN outcome = 'SOLVED' THEN elapsed_seconds END) AS bestSeconds,
               SUM(elapsed_seconds) AS totalSeconds
        FROM puzzle_attempts
        WHERE module = :module AND difficulty = :difficulty
        """
    )
    suspend fun summary(module: String, difficulty: String): SummaryRow
}
