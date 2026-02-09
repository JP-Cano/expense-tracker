package com.expensetracker.db

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ExpenseEntity::class, ExchangeRateEntity::class, PocketEntity::class, GlobalBudgetEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense-tracker.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN cardBrand TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS pockets (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "color INTEGER NOT NULL, " +
                        "icon TEXT NOT NULL, " +
                        "monthlyBudget REAL, " +
                        "isSystem INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS global_budget (" +
                        "id INTEGER PRIMARY KEY NOT NULL, " +
                        "monthlyBudget REAL)"
                )
                db.execSQL("INSERT OR IGNORE INTO global_budget (id, monthlyBudget) VALUES (1, NULL)")
                db.execSQL(
                    "INSERT OR IGNORE INTO pockets (id, name, color, icon, monthlyBudget, isSystem) " +
                        "VALUES (1, 'Uncategorized', 0xFF1F2937, 'Category', NULL, 1)"
                )
                db.execSQL("ALTER TABLE expenses ADD COLUMN pocketId INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}
