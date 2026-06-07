package com.example.financetracker.transaction

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional

@Repository
interface RecurringScheduleRepository : JpaRepository<RecurringSchedule, Long> {
    fun findByTransactionId(transactionId: Long): Optional<RecurringSchedule>
    fun findAllByActiveTrue(): List<RecurringSchedule>

    /**
     * Deactivates every active schedule whose end date is already in the past.
     * Returns the number of rows updated.
     */
    @Modifying
    @Query(
        """
        UPDATE RecurringSchedule s
        SET s.active = false
        WHERE s.active = true AND s.endDate IS NOT NULL AND s.endDate < :today
        """
    )
    fun deactivateExpired(@Param("today") today: LocalDate): Int
}