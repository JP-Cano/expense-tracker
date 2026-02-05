package com.expensetracker

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.Currency
import com.expensetracker.data.CardBrand
import com.expensetracker.data.ExpenseForm
import com.expensetracker.data.PaymentType
import com.expensetracker.db.ExpenseEntity
import com.expensetracker.repository.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val formState = MutableStateFlow(ExpenseForm())
    private val errorState = MutableStateFlow<String?>(null)
    private val messageState = MutableStateFlow<String?>(null)
    private val editingIdState = MutableStateFlow<Long?>(null)
    private val editFormState = MutableStateFlow(ExpenseForm())
    private val rateState = MutableStateFlow<RateUiState>(RateUiState.Idle)

    private val startDateState = MutableStateFlow(LocalDate.now())
    private val endDateState = MutableStateFlow(LocalDate.now())

    val form: StateFlow<ExpenseForm> = formState.asStateFlow()
    val errorMessage: StateFlow<String?> = errorState.asStateFlow()
    val message: StateFlow<String?> = messageState.asStateFlow()
    val editingId: StateFlow<Long?> = editingIdState.asStateFlow()
    val editForm: StateFlow<ExpenseForm> = editFormState.asStateFlow()
    val usdRateState: StateFlow<RateUiState> = rateState.asStateFlow()

    val expenses = repository.getExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val dateRange: Flow<Pair<LocalDate, LocalDate>> =
        combine(startDateState, endDateState) { start, end -> start to end }

    val totalBetween: StateFlow<Double> = dateRange
        .flatMapLatest { (start, end) -> repository.getTotalBetween(start, end) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val startDate: StateFlow<LocalDate> = startDateState.asStateFlow()
    val endDate: StateFlow<LocalDate> = endDateState.asStateFlow()

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

    fun setDateRange(start: LocalDate, end: LocalDate) {
        if (start.isAfter(end)) {
            startDateState.value = end
            endDateState.value = start
        } else {
            startDateState.value = start
            endDateState.value = end
        }
    }

    fun quickToday() {
        val today = LocalDate.now()
        setDateRange(today, today)
    }

    fun quickMonth() {
        val now = LocalDate.now()
        setDateRange(now.withDayOfMonth(1), now)
    }

    fun quickYear() {
        val now = LocalDate.now()
        setDateRange(now.withDayOfYear(1), now)
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
            manualRate = expense.rateUsed?.toString() ?: ""
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
            }
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExpense(id)
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

    fun clearError() {
        errorState.value = null
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
