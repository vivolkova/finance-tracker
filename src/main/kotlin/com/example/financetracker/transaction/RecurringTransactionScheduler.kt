package com.example.financetracker.transaction

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class RecurringTransactionScheduler(
    private val scheduleRepository: RecurringScheduleRepository,
    private val transactionRepository: TransactionRepository
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    fun processRecurringTransactions() {
        logger.info("Processing recurring transactions...")
        val today = LocalDate.now()
        val schedules = scheduleRepository.findAllByActiveTrue()

        schedules.forEach { schedule ->
            try {
                processSchedule(schedule, today)
            } catch (e: Exception) {
                logger.error("Failed to process schedule ${schedule.id}: ${e.message}")
            }
        }
        logger.info("Recurring transactions processed: ${schedules.size} schedules checked")
    }

    private fun processSchedule(schedule: RecurringSchedule, today: LocalDate) {
        if (schedule.endDate != null && today.isAfter(schedule.endDate)) {
            logger.info("Schedule ${schedule.id} expired, skipping")
            return
        }

        if (!shouldCreateToday(schedule, today)) return

       val template = transactionRepository.findById(schedule.transactionId)
            .orElse(null) ?: return

        val newTransaction = Transaction(
            amount = template.amount,
            description = template.description,
            date = today,
            type = template.type,
            category = template.category,
            user = template.user
        )
        transactionRepository.save(newTransaction)

        val updatedSchedule = RecurringSchedule(
            id = schedule.id,
            transactionId = schedule.transactionId,
            frequency = schedule.frequency,
            dayOfMonth = schedule.dayOfMonth,
            startDate = schedule.startDate,
            endDate = schedule.endDate,
            lastRunDate = today,
            active = schedule.active,
            createdAt = schedule.createdAt
        )
        scheduleRepository.save(updatedSchedule)

        logger.info("Created recurring transaction for schedule ${schedule.id}")
    }

    private fun shouldCreateToday(schedule: RecurringSchedule, today: LocalDate): Boolean {
        val lastRunDate = schedule.lastRunDate ?: return today >= schedule.startDate

        return when (schedule.frequency) {
            Frequency.DAILY -> lastRunDate.plusDays(1) <= today
            Frequency.WEEKLY -> lastRunDate.plusWeeks(1) <= today
            Frequency.MONTHLY -> {
                val nextRun = lastRunDate.plusMonths(1)
                    .withDayOfMonth(schedule.dayOfMonth ?: lastRunDate.dayOfMonth)
                nextRun <= today
            }
            Frequency.YEARLY -> lastRunDate.plusYears(1) <= today
        }
    }
}