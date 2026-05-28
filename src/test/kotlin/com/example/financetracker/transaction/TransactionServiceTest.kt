package com.example.financetracker.transaction

import com.example.financetracker.category.Category
import com.example.financetracker.category.CategoryRepository
import com.example.financetracker.category.CategoryType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class TransactionServiceTest {

    private val transactionRepository = mockk<TransactionRepository>()
    private val categoryRepository = mockk<CategoryRepository>()
    private val transactionService = TransactionService(transactionRepository, categoryRepository)

    private val category = Category(
        id = 1L,
        name = "Salary",
        type = CategoryType.INCOME
    )

    private val transaction = Transaction(
        id = 1L,
        amount = BigDecimal("5000"),
        description = "Monthly salary",
        date = LocalDate.of(2026, 5, 27),
        type = TransactionType.INCOME,
        category = category
    )

    @Test
    fun `getAll should return list of TransactionDto`() {
        // given
        every { transactionRepository.findAll() } returns listOf(transaction)

        // when
        val result = transactionService.getAll()

        // then
        assert(result.size == 1)
        assert(result[0].amount == BigDecimal("5000"))
        assert(result[0].categoryName == "Salary")
        verify(exactly = 1) { transactionRepository.findAll() }
    }

    @Test
    fun `getById should return TransactionDto when found`() {
        // given
        every { transactionRepository.findById(1L) } returns Optional.of(transaction)

        // when
        val result = transactionService.getById(1L)

        // then
        assert(result.id == 1L)
        assert(result.amount == BigDecimal("5000"))
        assert(result.categoryId == 1L)
    }

    @Test
    fun `getById should throw NoSuchElementException when not found`() {
        // given
        every { transactionRepository.findById(99L) } returns Optional.empty()

        // when/then
        assertThrows<NoSuchElementException> {
            transactionService.getById(99L)
        }
    }

    @Test
    fun `create should save and return TransactionDto`() {
        // given
        every { categoryRepository.findById(1L) } returns Optional.of(category)
        every { transactionRepository.save(any()) } returns transaction

        val command = CreateTransactionCommand(
            amount = BigDecimal("5000"),
            description = "Monthly salary",
            date = LocalDate.of(2026, 5, 27),
            type = TransactionType.INCOME,
            categoryId = 1L
        )

        // when
        val result = transactionService.create(command)

        // then
        assert(result.amount == BigDecimal("5000"))
        assert(result.categoryName == "Salary")
        verify(exactly = 1) { categoryRepository.findById(1L) }
        verify(exactly = 1) { transactionRepository.save(any()) }
    }

    @Test
    fun `create should throw NoSuchElementException when category not found`() {
        // given
        every { categoryRepository.findById(99L) } returns Optional.empty()

        val command = CreateTransactionCommand(
            amount = BigDecimal("5000"),
            description = null,
            date = LocalDate.of(2026, 5, 27),
            type = TransactionType.INCOME,
            categoryId = 99L
        )

        // when/then
        assertThrows<NoSuchElementException> {
            transactionService.create(command)
        }
        verify(exactly = 0) { transactionRepository.save(any()) }
    }

    @Test
    fun `delete should call deleteById when transaction exists`() {
        // given
        every { transactionRepository.existsById(1L) } returns true
        every { transactionRepository.deleteById(1L) } returns Unit

        // when
        transactionService.delete(1L)

        // then
        verify(exactly = 1) { transactionRepository.deleteById(1L) }
    }

    @Test
    fun `delete should throw NoSuchElementException when not found`() {
        // given
        every { transactionRepository.existsById(99L) } returns false

        // when/then
        assertThrows<NoSuchElementException> {
            transactionService.delete(99L)
        }
        verify(exactly = 0) { transactionRepository.deleteById(any()) }
    }
}