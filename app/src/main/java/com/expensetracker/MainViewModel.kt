package com.expensetracker

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.CardBrand
import com.expensetracker.data.Currency
import com.expensetracker.data.ExpenseForm
import com.expensetracker.data.PaymentType
import com.expensetracker.data.PocketForm
import com.expensetracker.db.ExpenseEntity
import com.expensetracker.db.GlobalBudgetEntity
import com.expensetracker.db.PocketEntity
import com.expensetracker.repository.ExpenseRepository
import com.expensetracker.repository.ExpenseRepository.DeletePocketMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureDefaultPocket()
        }
    }

    private val formState = MutableStateFlow(ExpenseForm())
    private val errorState = MutableStateFlow<String?>(null)
    private val messageState = MutableStateFlow<String?>(null)
    private val editingIdState = MutableStateFlow<Long?>(null)
    private val editFormState = MutableStateFlow(ExpenseForm())
    private val rateState = MutableStateFlow<RateUiState>(RateUiState.Idle)

    private val editingPocketIdState = MutableStateFlow<Long?>(null)
    private val pocketFormState = MutableStateFlow(PocketForm())

    private val monthRangeState = MutableStateFlow(currentMonthRange())

    val form: StateFlow<ExpenseForm> = formState.asStateFlow()
    val errorMessage: StateFlow<String?> = errorState.asStateFlow()
    val message: StateFlow<String?> = messageState.asStateFlow()
    val editingId: StateFlow<Long?> = editingIdState.asStateFlow()
    val editForm: StateFlow<ExpenseForm> = editFormState.asStateFlow()
    val usdRateState: StateFlow<RateUiState> = rateState.asStateFlow()

    val pocketForm: StateFlow<PocketForm> = pocketFormState.asStateFlow()

    val pockets = repository.getPockets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val globalBudget = repository.getGlobalBudget()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GlobalBudgetEntity(1, null))

    val expenses = repository.getExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val pocketTotals = monthRangeState
        .flatMapLatest { (start, end) -> repository.getPocketTotalsBetween(start, end) }
        .map { list -> list.associate { it.pocketId to it.total } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val globalMonthlyTotal = monthRangeState
        .flatMapLatest { (start, end) -> repository.getTotalBetween(start, end) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun expensesByPocket(pocketId: Long): Flow<List<ExpenseEntity>> =
        repository.getExpensesByPocket(pocketId)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun pocketMonthlyTotal(pocketId: Long): Flow<Double> = monthRangeState
        .flatMapLatest { (start, end) -> repository.getPocketTotalBetween(pocketId, start, end) }

    fun updatePaymentType(type: PaymentType) {
        val brand = if (type == PaymentType.CARD) formState.value.cardBrand else null
        formState.value = formState.value.copy(paymentType = type, cardBrand = brand)
    }

    fun updateCardBrand(brand: CardBrand) {
        formState.value = formState.value.copy(cardBrand = brand)
    }

    fun updateAmount(value: String) {
        formState.value = formState.value.copy(amount = value)
    }

    fun updateCurrency(currency: Currency) {
        formState.value = formState.value.copy(currency = currency)
        refreshRateIfNeeded()
    }

    fun updatePlace(value: String) {
        formState.value = formState.value.copy(place = value)
    }

    fun updateDate(value: LocalDate) {
        formState.value = formState.value.copy(date = value)
        refreshRateIfNeeded()
    }

    fun updateDescription(value: String) {
        formState.value = formState.value.copy(description = value)
    }

    fun updateManualRate(value: String) {
        formState.value = formState.value.copy(manualRate = value)
    }

    fun updatePocketId(value: Long) {
        formState.value = formState.value.copy(pocketId = value)
    }

    fun startNewExpenseForPocket(pocketId: Long) {
        formState.value = ExpenseForm(pocketId = pocketId)
        rateState.value = RateUiState.Idle
    }

    fun refreshRateIfNeeded() {
        val form = formState.value
        if (form.currency != Currency.USD) {
            rateState.value = RateUiState.Idle
            return
        }
        rateState.value = RateUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.getUsdCopRate(form.date)
            rateState.value = if (result.isSuccess) {
                RateUiState.Success(result.getOrNull() ?: 0.0)
            } else {
                RateUiState.Error(result.exceptionOrNull()?.message ?: "Rate unavailable")
            }
        }
    }

    fun updateEditPaymentType(type: PaymentType) {
        val brand = if (type == PaymentType.CARD) editFormState.value.cardBrand else null
        editFormState.value = editFormState.value.copy(paymentType = type, cardBrand = brand)
    }

    fun updateEditCardBrand(brand: CardBrand) {
        editFormState.value = editFormState.value.copy(cardBrand = brand)
    }

    fun updateEditAmount(value: String) {
        editFormState.value = editFormState.value.copy(amount = value)
    }

    fun updateEditCurrency(currency: Currency) {
        editFormState.value = editFormState.value.copy(currency = currency)
    }

    fun updateEditPlace(value: String) {
        editFormState.value = editFormState.value.copy(place = value)
    }

    fun updateEditDate(value: LocalDate) {
        editFormState.value = editFormState.value.copy(date = value)
    }

    fun updateEditDescription(value: String) {
        editFormState.value = editFormState.value.copy(description = value)
    }

    fun updateEditManualRate(value: String) {
        editFormState.value = editFormState.value.copy(manualRate = value)
    }

    fun updateEditPocketId(value: Long) {
        editFormState.value = editFormState.value.copy(pocketId = value)
    }

    fun saveExpense() {
        viewModelScope.launch {
            val result = repository.addExpense(formState.value)
            if (result.isFailure) {
                errorState.value = result.exceptionOrNull()?.message
            } else {
                errorState.value = null
                formState.value = ExpenseForm()
                rateState.value = RateUiState.Idle
                messageState.value = "expense_saved"
            }
        }
    }

    fun startEdit(expense: ExpenseEntity) {
        editingIdState.value = expense.id
        editFormState.value = ExpenseForm(
            paymentType = parsePaymentType(expense.paymentType),
            cardBrand = parseCardBrand(expense.cardBrand),
            amount = expense.amount.toString(),
            currency = parseCurrency(expense.currency),
            place = expense.place,
            date = expense.date,
            description = expense.description,
            manualRate = expense.rateUsed?.toString() ?: "",
            pocketId = expense.pocketId
        )
    }

    fun cancelEdit() {
        editingIdState.value = null
        editFormState.value = ExpenseForm()
    }

    fun saveEdit() {
        val id = editingIdState.value ?: return
        viewModelScope.launch {
            val result = repository.updateExpense(id, editFormState.value)
            if (result.isFailure) {
                errorState.value = result.exceptionOrNull()?.message
            } else {
                errorState.value = null
                cancelEdit()
                messageState.value = "expense_updated"
            }
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExpense(id)
            messageState.value = "expense_deleted"
        }
    }

    fun startCreatePocket() {
        editingPocketIdState.value = null
        pocketFormState.value = PocketForm()
    }

    fun startEditPocket(pocket: PocketEntity) {
        if (pocket.isSystem) return
        editingPocketIdState.value = pocket.id
        pocketFormState.value = PocketForm(
            name = pocket.name,
            color = pocket.color,
            icon = pocket.icon,
            monthlyBudget = pocket.monthlyBudget?.toString() ?: ""
        )
    }

    fun updatePocketName(value: String) {
        pocketFormState.value = pocketFormState.value.copy(name = value)
    }

    fun updatePocketColor(value: Int) {
        pocketFormState.value = pocketFormState.value.copy(color = value)
    }

    fun updatePocketIcon(value: String) {
        pocketFormState.value = pocketFormState.value.copy(icon = value)
    }

    fun updatePocketBudget(value: String) {
        pocketFormState.value = pocketFormState.value.copy(monthlyBudget = value)
    }

    fun savePocket(globalBudgetValue: Double?) {
        val form = pocketFormState.value
        val budget = form.monthlyBudget.toDoubleOrNull()
        if (form.name.isBlank()) {
            errorState.value = "Pocket name required"
            return
        }
        if (globalBudgetValue != null && budget != null && budget > globalBudgetValue) {
            errorState.value = "Pocket budget exceeds global budget"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val pocketId = editingPocketIdState.value
            if (pocketId == null) {
                repository.createPocket(form)
                messageState.value = "pocket_created"
            } else {
                val current = repository.getPocketById(pocketId)
                if (current != null) {
                    repository.updatePocket(current, form)
                    messageState.value = "pocket_updated"
                }
            }
            editingPocketIdState.value = null
        }
    }

    fun deletePocket(pocketId: Long, mode: DeletePocketMode, targetPocketId: Long? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePocket(pocketId, mode, targetPocketId)
            messageState.value = "pocket_deleted"
        }
    }

    fun saveGlobalBudget(value: String?) {
        val amount = value?.toDoubleOrNull()
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertGlobalBudget(amount)
        }
    }

    fun exportToUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val payload = repository.exportJson()
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(payload.toByteArray())
            }
            messageState.value = "Exported"
        }
    }

    fun importFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            } ?: ""
            val result = repository.importJson(text)
            messageState.value = if (result.isSuccess) {
                "Imported ${result.getOrNull()}"
            } else {
                "Import failed"
            }
        }
    }

    fun clearMessage() {
        messageState.value = null
    }

    private fun parsePaymentType(raw: String): PaymentType {
        return try {
            PaymentType.valueOf(raw)
        } catch (_: Exception) {
            PaymentType.OTHER
        }
    }

    private fun parseCurrency(raw: String): Currency {
        return try {
            Currency.valueOf(raw)
        } catch (_: Exception) {
            Currency.COP
        }
    }

    private fun parseCardBrand(raw: String?): CardBrand? {
        return if (raw.isNullOrBlank()) {
            null
        } else {
            try {
                CardBrand.valueOf(raw)
            } catch (_: Exception) {
                CardBrand.OTHER
            }
        }
    }

    class Factory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}

sealed class RateUiState {
    data object Idle : RateUiState()
    data object Loading : RateUiState()
    data class Success(val rate: Double) : RateUiState()
    data class Error(val message: String) : RateUiState()
}

private fun currentMonthRange(): Pair<LocalDate, LocalDate> {
    val now = LocalDate.now()
    return now.withDayOfMonth(1) to now.withDayOfMonth(now.lengthOfMonth())
}
