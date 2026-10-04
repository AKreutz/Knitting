package com.akreutz.knitting.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Project::class, Step::class], version = 12, exportSchema = false)
abstract class KnittingDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var instance: KnittingDatabase? = null

        /** Inserts a sample project whenever the database is created, in every build type. */
        private fun seedCallback() = object : Callback() {
            private var seedOnOpen = false

            override fun onCreate(db: SupportSQLiteDatabase) = seed(db)

            // A destructive migration recreates the tables without calling onCreate, and this
            // callback runs before they exist again, so the seed waits for onOpen.
            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                seedOnOpen = true
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                if (!seedOnOpen) return
                seedOnOpen = false
                seed(db)
            }

            private fun seed(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "INSERT INTO projects (name, description, status, rowCount) " +
                        "VALUES ('Sample scarf', 'Seed project for development', 'Created', 0)",
                )
            }
        }

        fun get(context: Context): KnittingDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KnittingDatabase::class.java,
                    "knitting.db",
                )
                    // Schema changes wipe the data instead of migrating it.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(seedCallback())
                    .build()
                    .also { instance = it }
            }
    }
}
