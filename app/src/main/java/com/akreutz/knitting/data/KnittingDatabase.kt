package com.akreutz.knitting.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Project::class, Step::class], version = 20, exportSchema = false)
abstract class KnittingDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var instance: KnittingDatabase? = null

        /** Adds the free-text description used by Special steps. */
        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE steps ADD COLUMN description TEXT")
            }
        }

        fun get(context: Context): KnittingDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KnittingDatabase::class.java,
                    "knitting.db",
                )
                    // Only versions before 19 are wiped; from 19 on, schema changes need a migration.
                    .fallbackToDestructiveMigrationFrom(true, *IntArray(18) { it + 1 })
                    .addMigrations(MIGRATION_19_20)
                    .build()
                    .also { instance = it }
            }
    }
}
