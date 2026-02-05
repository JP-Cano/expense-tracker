package com.expensetracker.data

import java.time.LocalDate

enum class Currency { COP, USD }

enum class PaymentType { CASH, CARD, TRANSFER, OTHER }

enum class CardBrand { VISA, MASTERCARD, AMEX, OTHER }

data class ExpenseForm(
    val paymentType: PaymentType = PaymentType.CASH,
    val cardBrand: CardBrand? = null,
    val amount: String = "",
    val currency: Currency = Currency.COP,
    val place: String = "",
    val date: LocalDate = LocalDate.now(),
    val description: String = "",
    val manualRate: String = ""
)
