package com.example.financetracker.budget

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BudgetRepository : JpaRepository<Budget, Long> {

    fun existsByUserIdAndCategoryIdAndPeriod(
        userId: Long,
        categoryId: Long,
        period: String
    ): Boolean
}
