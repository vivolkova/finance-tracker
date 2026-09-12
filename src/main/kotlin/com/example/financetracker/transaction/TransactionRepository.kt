package com.example.financetracker.transaction

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

@Repository
interface TransactionRepository : JpaRepository<Transaction, Long> {

    @Query("""
        SELECT t FROM Transaction t 
        JOIN FETCH t.category 
        WHERE FUNCTION('DATE_TRUNC', 'month', t.date) = 
              FUNCTION('DATE_TRUNC', 'month', CAST(:date AS date))
    """)
    fun findAllByMonth(@Param("date") date: LocalDate): List<Transaction>

    @Query("""
        SELECT coalesce(sum(t.amount),0) FROM Transaction t
        WHERE t.date between :dateFrom and :dateTo and t.user.id = :userId and t.category.id = :categoryId
    """)
    fun getTotalAmount(@Param("dateFrom") dateFrom: LocalDate,
                       @Param("dateTo") dateTo: LocalDate,
                       @Param("userId") userId: Long,
                       @Param("categoryId") categoryId: Long): BigDecimal

}