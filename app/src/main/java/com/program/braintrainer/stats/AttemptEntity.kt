package com.program.braintrainer.stats

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module

/**
 * Red u tabeli odigranih zagonetki.
 *
 * Modul, težina i ishod se čuvaju kao tekst (`Module.name` i slično), a ne kao
 * redni broj enum-a: dodavanje četvrtog modula ne sme da premesti značenje već
 * upisanih redova.
 */
@Entity(
    tableName = "puzzle_attempts",
    indices = [
        Index("finished_at"),
        Index("puzzle_id"),
        Index("module", "difficulty")
    ]
)
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "puzzle_id") val puzzleId: String,
    @ColumnInfo(name = "module") val module: String,
    @ColumnInfo(name = "difficulty") val difficulty: String,
    @ColumnInfo(name = "finished_at") val finishedAt: Long,
    @ColumnInfo(name = "outcome") val outcome: String,
    @ColumnInfo(name = "elapsed_seconds") val elapsedSeconds: Int,
    @ColumnInfo(name = "player_moves") val playerMoves: Int,
    @ColumnInfo(name = "optimal_moves") val optimalMoves: Int,
    @ColumnInfo(name = "mistakes") val mistakes: Int,
    @ColumnInfo(name = "earned_xp") val earnedXp: Int
)

internal fun PuzzleAttempt.toEntity() = AttemptEntity(
    puzzleId = puzzleId,
    module = module.name,
    difficulty = difficulty.name,
    finishedAt = finishedAt,
    outcome = outcome.name,
    elapsedSeconds = elapsedSeconds,
    playerMoves = playerMoves,
    optimalMoves = optimalMoves,
    mistakes = mistakes,
    earnedXp = earnedXp
)

/**
 * Red iz baze u model. Vraća null kada zapis nosi modul, težinu ili ishod koji
 * ova verzija aplikacije ne poznaje — bolje preskočiti jedan red nego srušiti
 * ceo spisak zbog podatka koji je upisala novija verzija.
 */
internal fun AttemptEntity.toAttempt(): PuzzleAttempt? {
    val module = enumOrNull<Module>(module) ?: return null
    val difficulty = enumOrNull<Difficulty>(difficulty) ?: return null
    val outcome = enumOrNull<AttemptOutcome>(outcome) ?: return null

    return PuzzleAttempt(
        puzzleId = puzzleId,
        module = module,
        difficulty = difficulty,
        finishedAt = finishedAt,
        outcome = outcome,
        elapsedSeconds = elapsedSeconds,
        playerMoves = playerMoves,
        optimalMoves = optimalMoves,
        mistakes = mistakes,
        earnedXp = earnedXp
    )
}

internal inline fun <reified T : Enum<T>> enumOrNull(name: String): T? =
    enumValues<T>().firstOrNull { it.name == name }
