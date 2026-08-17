package com.example.financetracker.budget

import java.math.BigDecimal

data class BudgetDto(
    val id: Long = 0,
    val category: Long,
    val userEmail: String?,
    val limitAmount: BigDecimal,
    val period: String,
)

fun Budget.toDto() =
    BudgetDto(
        id = this.id,
        category = this.category.id,
        userEmail = this.user.email,
        limitAmount = this.limitAmount,
        period = this.period
    )