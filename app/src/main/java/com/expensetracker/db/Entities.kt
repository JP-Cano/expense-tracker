package com.expensetracker.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val paymentType: String,
    val cardBrand: String?,
    val amount: Double,
    val currency: String,
    val amountCop: Double,
    val rateUsed: Double?,
    val place: String,
    val date: LocalDate,
    val description: String
)

@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey val date: LocalDate,
    val usdToCop: Double
)
