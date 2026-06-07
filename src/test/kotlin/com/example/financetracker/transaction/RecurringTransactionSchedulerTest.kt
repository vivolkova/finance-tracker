package com.example.financetracker.transaction

import com.example.financetracker.category.Category
import com.example.financetracker.category.CategoryType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Optional

class RecurringTransactionSchedulerTest {

    private val today = LocalDate.of(2026, 6, 15)

    private val clock: Clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneId.of("UTC"))

    private val scheduleRepository = mockk<RecurringScheduleRepository>()
    private val transactionRepository = mockk<TransactionRepository>()
    private val scheduler =
        RecurringTransactionScheduler(scheduleRepository, transactionRepository, clock)

    private val category = Category(id = 1L, name = "Rent", type = CategoryType.EXPENSE)

    private val template = Transaction(
        id = 1L,
        amount = BigDecimal("1000"),
        description = "Monthly rent",
        date = LocalDate.of(2026, 1, 1),
        type = TransactionType.EXPENSE,
        category = category
    )

    @BeforeEach
    fun setUp() {
        every { transactionRepository.findById(1L) } returns Optional.of(template)
        every { transactionRepository.save(any()) } answers { firstArg() }
        every { scheduleRepository.deactivateExpired(any()) } returns 0
        every { scheduleRepository.save(any()) } answers { firstArg() }
    }

    private fun schedule(
        frequency: Frequency,
        startDate: LocalDate = LocalDate.of(2026, 1, 1),
        endDate: LocalDate? = null,
        lastRunDate: LocalDate? = null,
        dayOfMonth: Int? = null,
        transactionId: Long = 1L
    ) = RecurringSchedule(
        id = 1L,
        transactionId = transactionId,
        frequency = frequency,
        dayOfMonth = dayOfMonth,
        startDate = startDate,
        endDate = endDate,
        lastRunDate = lastRunDate
    )

    @Test
    fun `creates transaction for DAILY schedule due today`() {
        every { scheduleRepository.findAllByActiveTrue() } returns
                listOf(schedule(Frequency.DAILY, lastRunDate = today.minusDays(1)))

        scheduler.processRecurringTransactions()

        verify(exactly = 1) { transactionRepository.save(any()) }
        verify(exactly = 1) { scheduleRepository.save(any()) }
    }

    @Test
    fun `does not create for DAILY schedule that already ran today`() {
        every { scheduleRepository.findAllByActiveTrue() } returns
                listOf(schedule(Frequency.DAILY, lastRunDate = today))

        scheduler.processRecurringTransactions()

        verify(exactly = 0) { transactionRepository.save(any()) }
        verify(exactly = 0) { scheduleRepository.save(any()) }
    }

    @Test
    fun `creates transaction for WEEKLY schedule due today`() {
        every { scheduleRepository.findAllByActiveTrue() } returns
                listOf(schedule(Frequency.WEEKLY, lastRunDate = today.minusWeeks(1)))

        scheduler.processRecurringTransactions()

        verify(exactly = 1) { transactionRepository.save(any()) }
    }

    @Test
    fun `does not create for WEEKLY schedule before the week elapsed`() {
        every { scheduleRepository.findAllByActiveTrue() } returns
                listOf(schedule(Frequency.WEEKLY, lastRunDate = today.minusDays(3)))

        scheduler.processRecurringTransactions()

        verify(exactly = 0) { transactionRepository.save(any()) }
    }

    @Test
    fun `creates transaction for MONTHLY schedule on the configured day`() {
        every { scheduleRepository.findAllByActiveTrue() } returns
                listOf(
                    schedule(
                        Frequency.MONTHLY,
                        lastRunDate = LocalDate.of(2026, 5, 15),
                        dayOfMonth = 15
                    )
                )

        scheduler.processRecurringTransactions()

        verify(exactly = 1) { transactionRepository.save(any()) }
    }

    @Test
    fun `creates transaction for YEARLY schedule due today`() {
        every { scheduleRepository.findAllByActiveTrue() } returns
                listOf(schedule(Frequency.YEARLY, lastRunDate = today.minusYears(1)))

        scheduler.processRecurringTransactions()

        verify(exactly = 1) { transactionRepository.save(any()) }
    }

    @Test
    fun `creates transaction when never run before and start date has arrived`() {
        every { scheduleRepository.findAllByActiveTrue() } returns
                listOf(schedule(Frequency.DAILY, startDate = today, lastRunDate = null))

        scheduler.processRecurringTransactions()

        verify(exactly = 1) { transactionRepository.save(any()) }
    }

    @Test
    fun `does not create when start date is in the future`() {
        every { scheduleRepository.findAllByActiveTrue() } returns
                listOf(schedule(Frequency.DAILY, startDate = today.plusDays(5), lastRunDate = null))

        scheduler.processRecurringTransactions()

        verify(exactly = 0) { transactionRepository.save(any()) }
    }

    @Test
    fun `skips expired schedule whose end date has passed`() {
        every { scheduleRepository.findAllByActiveTrue() } returns
                listOf(
                    schedule(
                        Frequency.DAILY,
                        endDate = today.minusDays(1),
                        lastRunDate = today.minusDays(1)
                    )
                )

        scheduler.processRecurringTransactions()

        verify(exactly = 0) { transactionRepository.save(any()) }
    }

    @Test
    fun `does not create when template transaction is missing`() {
        every { transactionRepository.findById(1L) } returns Optional.empty()
        every { scheduleRepository.findAllByActiveTrue() } returns
                listOf(schedule(Frequency.DAILY, lastRunDate = today.minusDays(1)))

        scheduler.processRecurringTransactions()

        verify(exactly = 0) { transactionRepository.save(any()) }
        verify(exactly = 0) { scheduleRepository.save(any()) }
    }

    @Test
    fun `failure in one schedule does not stop processing the others`() {
        val failing = schedule(Frequency.DAILY, lastRunDate = today.minusDays(1), transactionId = 2L)
        val healthy = schedule(Frequency.DAILY, lastRunDate = today.minusDays(1), transactionId = 1L)
        every { transactionRepository.findById(2L) } throws RuntimeException("DB down")
        every { scheduleRepository.findAllByActiveTrue() } returns listOf(failing, healthy)

        scheduler.processRecurringTransactions()

        // The healthy schedule (transactionId = 1L) is still processed and saved.
        verify(exactly = 1) { transactionRepository.save(any()) }
    }
}
