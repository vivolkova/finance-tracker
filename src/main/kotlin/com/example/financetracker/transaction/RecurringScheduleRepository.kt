package com.example.financetracker.transaction

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface RecurringScheduleRepository : JpaRepository<RecurringSchedule, Long> {
    fun findByTransactionId(transactionId: Long): Optional<RecurringSchedule>
    fun findAllByActiveTrue(): List<RecurringSchedule>
}