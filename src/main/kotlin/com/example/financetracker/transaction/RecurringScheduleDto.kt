package com.example.financetracker.transaction

import java.time.LocalDate

data class RecurringScheduleDto(
    val id: Long,
    val transactionId: Long,
    val frequency: Frequency,
    val dayOfMonth: Int?,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val active: Boolean
)

fun RecurringSchedule.toDto() = RecurringScheduleDto(
    id = id,
    transactionId = transactionId,
    frequency = frequency,
    dayOfMonth = dayOfMonth,
    startDate = startDate,
    endDate = endDate,
    active = active
)