package com.example.financetracker.transaction

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "recurring_schedules")
class RecurringSchedule(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "transaction_id", nullable = false, unique = true)
    val transactionId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val frequency: Frequency,

    @Column(name = "day_of_month")
    val dayOfMonth: Int? = null,

    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate,

    @Column(name = "end_date")
    val endDate: LocalDate? = null,

    @Column(name = "last_run_date")
    val lastRunDate: LocalDate? = null,

    @Column(nullable = false)
    val active: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class Frequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}