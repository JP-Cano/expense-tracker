package com.expensetracker.repository

import com.expensetracker.data.Currency
import com.expensetracker.data.ExpenseForm
import com.expensetracker.data.PaymentType
import com.expensetracker.data.PocketForm
import com.expensetracker.data.UNCATEGORIZED_POCKET_ID
import com.expensetracker.db.ExchangeRateEntity
import com.expensetracker.db.ExpenseDao
import com.expensetracker.db.ExpenseEntity
import com.expensetracker.db.GlobalBudgetEntity
import com.expensetracker.db.PocketEntity
import com.expensetracker.db.PocketTotalEntity
import com.expensetracker.network.ExchangeRateApi
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

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
    val description: String,
    val pocketId: Long? = null,
    val pocketName: String? = null
)

class ExpenseRepository(
    private val dao: ExpenseDao,
    private val api: ExchangeRateApi
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun getExpenses(): Flow<List<ExpenseEntity>> = dao.getAllExpenses()

    fun getExpensesByPocket(pocketId: Long): Flow<List<ExpenseEntity>> =
        dao.getExpensesByPocket(pocketId)

    fun getPockets(): Flow<List<PocketEntity>> = dao.getAllPockets()

    fun getGlobalBudget(): Flow<GlobalBudgetEntity?> = dao.getGlobalBudget()

    suspend fun getPocketById(id: Long): PocketEntity? = dao.getPocketById(id)

    suspend fun ensureDefaultPocket() {
        val existing = dao.getPocketById(UNCATEGORIZED_POCKET_ID)
        if (existing == null) {
            dao.insertPocket(
                PocketEntity(
                    id = UNCATEGORIZED_POCKET_ID,
                    name = "Uncategorized",
                    color = 0xFF1F2937.toInt(),
                    icon = "Category",
                    monthlyBudget = null,
                    isSystem = true
                )
            )
        }
    }

    fun getTotalBetween(start: LocalDate, end: LocalDate): Flow<Double> =
        dao.getTotalBetween(start, end)

    fun getPocketTotalBetween(pocketId: Long, start: LocalDate, end: LocalDate): Flow<Double> =
        dao.getTotalByPocketBetween(pocketId, start, end)

    fun getPocketTotalsBetween(start: LocalDate, end: LocalDate): Flow<List<PocketTotalEntity>> =
        dao.getPocketTotalsBetween(start, end)

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
            description = form.description.trim(),
            pocketId = form.pocketId
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
            description = form.description.trim(),
            pocketId = form.pocketId
        )

        dao.updateExpense(expense)
        return Result.success(Unit)
    }

    suspend fun deleteExpense(id: Long) {
        dao.deleteExpense(id)
    }

    suspend fun upsertGlobalBudget(amount: Double?) {
        dao.upsertGlobalBudget(GlobalBudgetEntity(1, amount))
    }

    suspend fun createPocket(form: PocketForm): Long {
        val pocket = PocketEntity(
            name = form.name.trim(),
            color = form.color,
            icon = form.icon,
            monthlyBudget = form.monthlyBudget.toDoubleOrNull(),
            isSystem = false
        )
        return dao.insertPocket(pocket)
    }

    suspend fun updatePocket(pocket: PocketEntity, form: PocketForm) {
        val updated = pocket.copy(
            name = form.name.trim(),
            color = form.color,
            icon = form.icon,
            monthlyBudget = form.monthlyBudget.toDoubleOrNull()
        )
        dao.updatePocket(updated)
    }

    enum class DeletePocketMode { DELETE_EXPENSES, MOVE_TO_UNCATEGORIZED, REASSIGN }

    suspend fun deletePocket(pocketId: Long, mode: DeletePocketMode, targetPocketId: Long? = null) {
        if (pocketId == UNCATEGORIZED_POCKET_ID) return
        when (mode) {
            DeletePocketMode.DELETE_EXPENSES -> dao.deleteExpensesByPocket(pocketId)
            DeletePocketMode.MOVE_TO_UNCATEGORIZED ->
                dao.moveExpensesToPocket(pocketId, UNCATEGORIZED_POCKET_ID)
            DeletePocketMode.REASSIGN -> {
                val target = targetPocketId ?: UNCATEGORIZED_POCKET_ID
                dao.moveExpensesToPocket(pocketId, target)
            }
        }
        dao.deletePocket(pocketId)
    }

    suspend fun exportJson(): String {
        val pockets = dao.getAllPocketsOnce().associateBy { it.id }
        val items = dao.getAllExpensesOnce().map { expense ->
            val pocketName = pockets[expense.pocketId]?.name
            ExpenseExportItem(
                paymentType = expense.paymentType,
                cardBrand = expense.cardBrand,
                amount = expense.amount,
                currency = expense.currency,
                amountCop = expense.amountCop,
                rateUsed = expense.rateUsed,
                place = expense.place,
                date = expense.date.toString(),
                description = expense.description,
                pocketId = expense.pocketId,
                pocketName = pocketName
            )
        }
        return json.encodeToString(ExpenseExport.serializer(), ExpenseExport(items))
    }

    suspend fun importJson(payload: String): Result<Int> {
        return try {
            val data = json.decodeFromString(ExpenseExport.serializer(), payload)
            val pocketMap = dao.getAllPocketsOnce().associateBy { it.name.lowercase() }.toMutableMap()
            val entities = data.expenses.map { item ->
                val pocketId = when {
                    item.pocketId != null -> item.pocketId
                    !item.pocketName.isNullOrBlank() -> {
                        val pocketName = item.pocketName!!
                        val key = pocketName.lowercase()
                        val existing = pocketMap[key]
                        if (existing != null) {
                            existing.id
                        } else {
                            val newId = dao.insertPocket(
                                PocketEntity(
                                    name = pocketName,
                                    color = 0xFF1F2937.toInt(),
                                    icon = "Category",
                                    monthlyBudget = null,
                                    isSystem = false
                                )
                            )
                            val created = PocketEntity(
                                id = newId,
                                name = pocketName,
                                color = 0xFF1F2937.toInt(),
                                icon = "Category",
                                monthlyBudget = null,
                                isSystem = false
                            )
                            pocketMap[key] = created
                            newId
                        }
                    }
                    else -> UNCATEGORIZED_POCKET_ID
                }
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
                    description = item.description,
                    pocketId = pocketId ?: UNCATEGORIZED_POCKET_ID
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

    suspend fun getUsdCopRate(date: LocalDate): Result<Double> {
        return fetchRate(date)
    }

    private suspend fun fetchRate(date: LocalDate): Result<Double> {
        return try {
            val response = api.latestRates("USD")
            if (response.result != "success") {
                val reason = response.errorType ?: "unknown"
                Result.failure(IllegalStateException("API error: $reason"))
            } else {
                val rate = response.conversionRates["COP"] ?: response.rates["COP"]
                if (rate == null) {
                    Result.failure(IllegalStateException("Rate missing"))
                } else {
                    val responseDate = Instant.ofEpochSecond(response.timeLastUpdateUnix)
                        .atZone(ZoneOffset.UTC).toLocalDate()
                    dao.upsertRate(ExchangeRateEntity(responseDate, rate))
                    if (responseDate != date) {
                        dao.upsertRate(ExchangeRateEntity(date, rate))
                    }
                    Result.success(rate)
                }
            }
        } catch (e: Exception) {
            val cached = dao.getRate(date)
            if (cached != null) {
                Result.success(cached.usdToCop)
            } else {
                Result.failure(e)
            }
        } as Result<Double>
    }
}
