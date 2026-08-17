package com.example.financetracker.budget

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface BudgetRepository : JpaRepository<Budget, Long>,
    JpaSpecificationExecutor<Budget> {

    fun existsByUserIdAndCategoryIdAndPeriod(
        userId: Long,
        categoryId: Long,
        period: String
    ): Boolean

    fun findByUserIdOrderByPeriodDesc(
        userId: Long
    ): List<Budget>

    fun findByUserIdAndPeriodOrderByPeriodDesc(
        userId: Long,
        period: String
    ): List<Budget>
}
