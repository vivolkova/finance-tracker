package com.example.financetracker.budget

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
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

    fun findByUserIdAndPeriodAndCategoryId(
        userId: Long,
        period: String,
        categoryId: Long
    ): Budget?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select b from Budget b
    where b.user.id = :userId and b.period = :period and b.category.id = :categoryId
""")
    fun findForUpdate(userId: Long, period: String, categoryId: Long): Budget?
}
