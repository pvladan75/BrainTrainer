package com.program.braintrainer.stats

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Lokalna baza odigranih zagonetki. Jedina tabela je [AttemptEntity]; sve što
 * istorija, dnevnik grešaka i grafici pokazuju izvodi se iz nje.
 *
 * Šema se izvozi u `app/schemas` (vidi `ksp { arg("room.schemaLocation", ...) }`),
 * pa svaka naredna verzija ima sa čim da uporedi migraciju.
 */
@Database(entities = [AttemptEntity::class], version = 1, exportSchema = true)
abstract class StatsDatabase : RoomDatabase() {

    abstract fun attemptDao(): AttemptDao

    companion object {
        fun create(context: Context): StatsDatabase =
            Room.databaseBuilder(context, StatsDatabase::class.java, "stats.db").build()
    }
}
