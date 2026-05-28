package com.example.financetracker.transaction

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
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
}