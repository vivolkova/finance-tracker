package com.example.financetracker.transaction

import com.example.financetracker.common.loggerFor
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Обработка ОДНОГО расписания в СВОЕЙ транзакции.
 *
 * Почему это отдельный бин, а не приватный метод в scheduler:
 *  - @Transactional работает только через Spring-прокси, т.е. при вызове из ДРУГОГО бина;
 *    на приватных методах и при self-invocation (вызов своего же метода) он НЕ срабатывает;
 *  - fan-out гоняет обработку на разных потоках (Dispatchers.IO), а транзакция привязана
 *    к потоку (ThreadLocal). Поэтому нужна транзакция НА КАЖДЫЙ вызов process(), а не одна
 *    общая на весь джоб — иначе на потоках воркеров транзакции просто не будет;
 *  - бонус для батча: падение одного расписания откатывает ТОЛЬКО его, остальные проходят.
 *
 * Внутри process() блокирующий JDBC, и НЕТ suspend-точек — значит транзакция целиком
 * живёт на одном потоке воркера (правило: не приостанавливаться внутри транзакции).
 */
@Component
class RecurringScheduleProcessor(
    private val scheduleRepository: RecurringScheduleRepository,
    private val transactionRepository: TransactionRepository
) {
    private val logger = loggerFor<RecurringScheduleProcessor>()

    @Transactional
    fun deactivateExpired(today: LocalDate): Int =
        scheduleRepository.deactivateExpired(today)

    @Transactional
    fun process(schedule: RecurringSchedule, today: LocalDate) {
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
