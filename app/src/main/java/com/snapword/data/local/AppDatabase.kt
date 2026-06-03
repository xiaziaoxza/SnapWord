package com.snapword.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WordEntity::class, ReviewRecordEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun reviewDao(): ReviewDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE word_entries ADD COLUMN lastReviewedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE word_entries ADD COLUMN forgettingDays INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE word_entries ADD COLUMN reviewCount INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
