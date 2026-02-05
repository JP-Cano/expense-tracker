package com.expensetracker.repository

import com.expensetracker.data.Currency
import com.expensetracker.data.ExpenseForm
import com.expensetracker.data.PaymentType
import com.expensetracker.db.ExchangeRateEntity
import com.expensetracker.db.ExpenseDao
import com.expensetracker.db.ExpenseEntity
import com.expensetracker.network.ExchangeRateApi
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

@Serializable
data class ExpenseExport(
    val expenses: List<ExpenseExportItem>
)

@Serializable
data class ExpenseExportItem(
    val paymentType: String,
    val cardBrand: String? = null,
    val amount: Double,
    val currency: String,
    val amountCop: Double,
    val rateUsed: Double? = null,
    val place: String,
    val date: String,
    val description: String
)

class ExpenseRepository(
    private val dao: ExpenseDao,
    private val api: ExchangeRateApi
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun getExpenses(): Flow<List<ExpenseEntity>> = dao.getAllExpenses()

    fun getTotalBetween(start: LocalDate, end: LocalDate): Flow<Double> =
        dao.getTotalBetween(start, end)

    suspend fun addExpense(form: ExpenseForm): Result<Unit> {
        val amount = form.amount.toDoubleOrNull()
            ?: return Result.failure(IllegalArgumentException("Invalid amount"))

        val cardBrand = normalizeCardBrand(form.paymentType, form.cardBrand?.name)

        val (amountCop, rateUsed) = when (form.currency) {
            Currency.COP -> amount to null
            Currency.USD -> {
                val manual = form.manualRate.toDoubleOrNull()
                val rate = manual ?: fetchRate(form.date).getOrNull()
                if (rate == null) {
                    return Result.failure(IllegalStateException("Missing exchange rate"))
                }
                amount * rate to rate
            }
        }

        val expense = ExpenseEntity(
            paymentType = form.paymentType.name,
            cardBrand = cardBrand,
            amount = amount,
            currency = form.currency.name,
            amountCop = amountCop,
            rateUsed = rateUsed,
            place = form.place.trim(),
            date = form.date,
            description = form.description.trim()
        )

        dao.insertExpense(expense)
        return Result.success(Unit)
    }

    suspend fun updateExpense(id: Long, form: ExpenseForm): Result<Unit> {
        val amount = form.amount.toDoubleOrNull()
            ?: return Result.failure(IllegalArgumentException("Invalid amount"))

        val cardBrand = normalizeCardBrand(form.paymentType, form.cardBrand?.name)

        val (amountCop, rateUsed) = when (form.currency) {
            Currency.COP -> amount to null
            Currency.USD -> {
                val manual = form.manualRate.toDoubleOrNull()
                val rate = manual ?: fetchRate(form.date).getOrNull()
                if (rate == null) {
                    return Result.failure(IllegalStateException("Missing exchange rate"))
                }
                amount * rate to rate
            }
        }

        val expense = ExpenseEntity(
            id = id,
            paymentType = form.paymentType.name,
            cardBrand = cardBrand,
            amount = amount,
            currency = form.currency.name,
            amountCop = amountCop,
            rateUsed = rateUsed,
            place = form.place.trim(),
            date = form.date,
            description = form.description.trim()
        )

        dao.updateExpense(expense)
        return Result.success(Unit)
    }

    suspend fun deleteExpense(id: Long) {
        dao.deleteExpense(id)
    }

    suspend fun exportJson(): String {
        val items = dao.getAllExpensesOnce().map { expense ->
            ExpenseExportItem(
                paymentType = expense.paymentType,
                cardBrand = expense.cardBrand,
                amount = expense.amount,
                currency = expense.currency,
                amountCop = expense.amountCop,
                rateUsed = expense.rateUsed,
                place = expense.place,
                date = expense.date.toString(),
                description = expense.description
            )
        }
        return json.encodeToString(ExpenseExport.serializer(), ExpenseExport(items))
    }

    suspend fun importJson(payload: String): Result<Int> {
        return try {
            val data = json.decodeFromString(ExpenseExport.serializer(), payload)
            val entities = data.expenses.map { item ->
                ExpenseEntity(
                    id = 0,
                    paymentType = item.paymentType,
                    cardBrand = item.cardBrand,
                    amount = item.amount,
                    currency = item.currency,
                    amountCop = item.amountCop,
                    rateUsed = item.rateUsed,
                    place = item.place,
                    date = LocalDate.parse(item.date),
                    description = item.description
                )
            }
            dao.insertExpenses(entities)
            Result.success(entities.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun normalizeCardBrand(paymentType: PaymentType, cardBrand: String?): String? {
        return if (paymentType == PaymentType.CARD) cardBrand else null
    }

    private suspend fun fetchRate(date: LocalDate): Result<Double> {
        return try {
            val response = api.getUsdCopRate(date.toString())
            val rate = response.rates["COP"]
            if (rate == null) {
                Result.failure(IllegalStateException("Rate missing"))
            } else {
                val responseDate = LocalDate.parse(response.date)
                dao.upsertRate(ExchangeRateEntity(responseDate, rate))
                if (responseDate != date) {
                    dao.upsertRate(ExchangeRateEntity(date, rate))
                }
                Result.success(rate)
            }
        } catch (e: Exception) {
            val cached = dao.getRate(date)
            if (cached != null) {
                Result.success(cached.usdToCop)
            } else {
                Result.failure(e)
            }
        }
    }
}
