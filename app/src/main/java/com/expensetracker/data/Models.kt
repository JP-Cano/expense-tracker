package com.expensetracker.data

import java.time.LocalDate

const val UNCATEGORIZED_POCKET_ID: Long = 1L

enum class Currency { COP, USD }

enum class PaymentType { CASH, CARD, TRANSFER, QR, OTHER }

enum class CardBrand { VISA, MASTERCARD, AMEX, RAPPI, GLOBAL66, OTHER }

data class ExpenseForm(
    val paymentType: PaymentType = PaymentType.CASH,
    val cardBrand: CardBrand? = null,
    val amount: String = "",
    val currency: Currency = Currency.COP,
    val place: String = "",
    val date: LocalDate = LocalDate.now(),
    val description: String = "",
    val manualRate: String = "",
    val pocketId: Long = UNCATEGORIZED_POCKET_ID
)

data class PocketForm(
    val name: String = "",
    val color: Int = 0xFF1F2937.toInt(),
    val icon: String = "Category",
    val monthlyBudget: String = ""
)
