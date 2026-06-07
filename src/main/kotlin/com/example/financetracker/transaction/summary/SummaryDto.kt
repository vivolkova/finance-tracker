package com.example.financetracker.transaction

import com.example.financetracker.category.CategoryType
import java.math.BigDecimal

data class CategorySummary(
    val categoryName: String,
    val type: CategoryType,
    val total: BigDecimal
)

data class MonthlySummary(
    val month: String,
    val totalIncome: BigDecimal,
    val totalExpense: BigDecimal,
    val balance: BigDecimal,
    val byCategory: List<CategorySummary>
)