package com.example.financetracker.transaction

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/transactions/{transactionId}/schedule")
class RecurringScheduleController(
    private val scheduleService: RecurringScheduleService
) {

    @GetMapping
    fun get(@PathVariable transactionId: Long): RecurringScheduleDto =
        scheduleService.getByTransaction(transactionId)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable transactionId: Long,
        @RequestBody request: CreateScheduleRequest
    ): RecurringScheduleDto =
        scheduleService.create(
            transactionId,
            CreateScheduleCommand(
                frequency = request.frequency,
                dayOfMonth = request.dayOfMonth,
                startDate = request.startDate,
                endDate = request.endDate
            )
        )

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable transactionId: Long) =
        scheduleService.delete(transactionId)
}

data class CreateScheduleRequest(
    val frequency: Frequency,
    val dayOfMonth: Int? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null
)