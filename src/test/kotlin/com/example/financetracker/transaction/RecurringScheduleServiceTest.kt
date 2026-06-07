package com.example.financetracker.transaction

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.Optional

class RecurringScheduleServiceTest {

    private val scheduleRepository = mockk<RecurringScheduleRepository>()
    private val transactionRepository = mockk<TransactionRepository>()
    private val scheduleService = RecurringScheduleService(
        scheduleRepository,
        transactionRepository
    )

    private val schedule = RecurringSchedule(
        id = 1L,
        transactionId = 1L,
        frequency = Frequency.MONTHLY,
        dayOfMonth = 15,
        startDate = LocalDate.of(2026, 1, 1)
    )

    @Test
    fun `getByTransaction should return schedule when found`() {
        every { scheduleRepository.findByTransactionId(1L) } returns Optional.of(schedule)

        val result = scheduleService.getByTransaction(1L)

        assert(result.transactionId == 1L)
        assert(result.frequency == Frequency.MONTHLY)
        assert(result.dayOfMonth == 15)
    }

    @Test
    fun `getByTransaction should throw when not found`() {
        every { scheduleRepository.findByTransactionId(99L) } returns Optional.empty()

        assertThrows<NoSuchElementException> {
            scheduleService.getByTransaction(99L)
        }
    }

    @Test
    fun `create should save schedule when transaction exists and no schedule yet`() {
        every { transactionRepository.existsById(1L) } returns true
        every { scheduleRepository.findByTransactionId(1L) } returns Optional.empty()
        every { scheduleRepository.save(any()) } returns schedule

        val command = CreateScheduleCommand(
            frequency = Frequency.MONTHLY,
            dayOfMonth = 15,
            startDate = LocalDate.of(2026, 1, 1)
        )

        val result = scheduleService.create(1L, command)

        assert(result.frequency == Frequency.MONTHLY)
        assert(result.dayOfMonth == 15)
        verify(exactly = 1) { scheduleRepository.save(any()) }
    }

    @Test
    fun `create should throw when transaction not found`() {
        every { transactionRepository.existsById(99L) } returns false

        val command = CreateScheduleCommand(
            frequency = Frequency.MONTHLY,
            startDate = LocalDate.of(2026, 1, 1)
        )

        assertThrows<NoSuchElementException> {
            scheduleService.create(99L, command)
        }
        verify(exactly = 0) { scheduleRepository.save(any()) }
    }

    @Test
    fun `create should throw when schedule already exists`() {
        every { transactionRepository.existsById(1L) } returns true
        every { scheduleRepository.findByTransactionId(1L) } returns Optional.of(schedule)

        val command = CreateScheduleCommand(
            frequency = Frequency.MONTHLY,
            startDate = LocalDate.of(2026, 1, 1)
        )

        assertThrows<IllegalArgumentException> {
            scheduleService.create(1L, command)
        }
        verify(exactly = 0) { scheduleRepository.save(any()) }
    }

    @Test
    fun `delete should remove schedule when found`() {
        every { scheduleRepository.findByTransactionId(1L) } returns Optional.of(schedule)
        every { scheduleRepository.delete(schedule) } returns Unit

        scheduleService.delete(1L)

        verify(exactly = 1) { scheduleRepository.delete(schedule) }
    }

    @Test
    fun `delete should throw when schedule not found`() {
        every { scheduleRepository.findByTransactionId(99L) } returns Optional.empty()

        assertThrows<NoSuchElementException> {
            scheduleService.delete(99L)
        }
        verify(exactly = 0) { scheduleRepository.delete(any()) }
    }
}