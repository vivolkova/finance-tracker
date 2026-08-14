package com.example.financetracker.budget

import com.example.financetracker.category.Category
import java.math.BigDecimal

data class BudgetDto(
    val id: Long = 0,
    val category: Category,
    val userEmail: String?,
    val limitAmount: BigDecimal,
    val period: String,
)

fun Budget.toDto() =
    BudgetDto(
        id = this.id,
        category = this.category,
        userEmail = this.user.email,
        limitAmount = this.limitAmount,
        period = this.period
    )