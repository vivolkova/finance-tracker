package com.example.financetracker.transaction

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

data class CreateScheduleCommand(
    val frequency: Frequency,
    val dayOfMonth: Int? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null
)

@Service
class RecurringScheduleService(
    private val scheduleRepository: RecurringScheduleRepository,
    private val transactionRepository: TransactionRepository
) {

    @Transactional(readOnly = true)
    fun getByTransaction(transactionId: Long): RecurringScheduleDto {
        return scheduleRepository.findByTransactionId(transactionId)
            .orElseThrow { NoSuchElementException("Schedule not found for transaction $transactionId") }
            .toDto()
    }

    @Transactional
    fun create(transactionId: Long, command: CreateScheduleCommand): RecurringScheduleDto {
        if (!transactionRepository.existsById(transactionId)) {
            throw NoSuchElementException("Transaction not found with id: $transactionId")
        }
        require(!scheduleRepository.findByTransactionId(transactionId).isPresent) {
            "Schedule already exists for transaction $transactionId"
        }

        val schedule = RecurringSchedule(
            transactionId = transactionId,
            frequency = command.frequency,
            dayOfMonth = command.dayOfMonth,
            startDate = command.startDate,
            endDate = command.endDate
        )
        return scheduleRepository.save(schedule).toDto()
    }

    @Transactional
    fun delete(transactionId: Long) {
        val schedule = scheduleRepository.findByTransactionId(transactionId)
            .orElseThrow { NoSuchElementException("Schedule not found for transaction $transactionId") }
        scheduleRepository.delete(schedule)
    }
}