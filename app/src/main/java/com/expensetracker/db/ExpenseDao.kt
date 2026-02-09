package com.expensetracker.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insertExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: Long)

    @Query("DELETE FROM expenses WHERE pocketId = :pocketId")
    suspend fun deleteExpensesByPocket(pocketId: Long)

    @Query("UPDATE expenses SET pocketId = :newPocketId WHERE pocketId = :oldPocketId")
    suspend fun moveExpensesToPocket(oldPocketId: Long, newPocketId: Long)

    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE pocketId = :pocketId ORDER BY date DESC, id DESC")
    fun getExpensesByPocket(pocketId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    suspend fun getAllExpensesOnce(): List<ExpenseEntity>

    @Query("SELECT COALESCE(SUM(amountCop), 0) FROM expenses WHERE date BETWEEN :start AND :end")
    fun getTotalBetween(start: LocalDate, end: LocalDate): Flow<Double>

    @Query("SELECT COALESCE(SUM(amountCop), 0) FROM expenses WHERE pocketId = :pocketId AND date BETWEEN :start AND :end")
    fun getTotalByPocketBetween(pocketId: Long, start: LocalDate, end: LocalDate): Flow<Double>

    @Query("SELECT pocketId, COALESCE(SUM(amountCop), 0) as total FROM expenses WHERE date BETWEEN :start AND :end GROUP BY pocketId")
    fun getPocketTotalsBetween(start: LocalDate, end: LocalDate): Flow<List<PocketTotalEntity>>

    @Query("SELECT * FROM pockets ORDER BY isSystem DESC, name ASC")
    fun getAllPockets(): Flow<List<PocketEntity>>

    @Query("SELECT * FROM pockets ORDER BY isSystem DESC, name ASC")
    suspend fun getAllPocketsOnce(): List<PocketEntity>

    @Query("SELECT * FROM pockets WHERE id = :id LIMIT 1")
    suspend fun getPocketById(id: Long): PocketEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPocket(pocket: PocketEntity): Long

    @Update
    suspend fun updatePocket(pocket: PocketEntity)

    @Query("DELETE FROM pockets WHERE id = :id")
    suspend fun deletePocket(id: Long)

    @Query("SELECT * FROM global_budget WHERE id = 1 LIMIT 1")
    fun getGlobalBudget(): Flow<GlobalBudgetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGlobalBudget(budget: GlobalBudgetEntity)

    @Query("SELECT * FROM exchange_rates WHERE date = :date LIMIT 1")
    suspend fun getRate(date: LocalDate): ExchangeRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRate(rate: ExchangeRateEntity)
}

data class PocketTotalEntity(
    val pocketId: Long,
    val total: Double
)
