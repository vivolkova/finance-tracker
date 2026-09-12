package com.example.financetracker.transaction

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.example.financetracker.common.loggerFor
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate

@Component
class RecurringTransactionScheduler(
    private val scheduleRepository: RecurringScheduleRepository,
    private val processor: RecurringScheduleProcessor,
    private val clock: Clock
) {
    private val logger = loggerFor<RecurringTransactionScheduler>()

    private companion object {
        const val WORKERS = 4
    }

    @Scheduled(cron = "0 * * * * *")
    fun processRecurringTransactions() = runBlocking {
        logger.info("Processing recurring transactions...")
        val today = LocalDate.now(clock)

        val deactivated = processor.deactivateExpired(today)   // своя транзакция
        if (deactivated > 0) {
            logger.info("Deactivated $deactivated expired schedule(s)")
        }

        val schedules = scheduleRepository.findAllByActiveTrue()  // чтение — транзакция не нужна

        // ── fan-out: один производитель кладёт расписания в канал, N воркеров разбирают ──
        val channel = Channel<RecurringSchedule>()
        coroutineScope {
            // производитель: складывает все расписания и закрывает канал
            launch {
                schedules.forEach { channel.send(it) }
                channel.close()                       // сигнал воркерам: расписания кончились
            }
            // воркеры: каждое расписание достаётся ОДНОМУ (unicast), обработка идёт параллельно.
            // Балансировка динамическая — освободившийся воркер забирает следующее.
            repeat(WORKERS) {
                launch(Dispatchers.IO) {
                    for (schedule in channel) {
                        try {
                            processor.process(schedule, today)   // отдельная транзакция на расписание
                        } catch (e: Exception) {
                            logger.error("Failed to process schedule ${schedule.id}: ${e.message}")
                        }
                    }
                }
            }
        }

        logger.info("Recurring transactions processed: ${schedules.size} schedules checked")
    }
}
