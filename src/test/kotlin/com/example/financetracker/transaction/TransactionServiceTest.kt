package com.example.financetracker.transaction

import com.example.financetracker.category.Category
import com.example.financetracker.category.CategoryRepository
import com.example.financetracker.category.CategoryType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
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

    // Recreated before each test — prevents shared mutable state via var version.
    private lateinit var transaction: Transaction

    @BeforeEach
    fun setUp() {
        transaction = Transaction(
            id = 1L,
            amount = BigDecimal("5000"),
            description = "Monthly salary",
            date = LocalDate.of(2026, 5, 27),
            type = TransactionType.INCOME,
            category = category,
            version = 0
        )
    }

    @Test
    fun `getAll should return list of TransactionDto`() {
        every { transactionRepository.findAll() } returns listOf(transaction)

        val result = transactionService.getAll()

        assert(result.size == 1)
        assert(result[0].amount == BigDecimal("5000"))
        assert(result[0].categoryName == "Salary")
        verify(exactly = 1) { transactionRepository.findAll() }
    }

    @Test
    fun `getById should return TransactionDto when found`() {
        every { transactionRepository.findById(1L) } returns Optional.of(transaction)

        val result = transactionService.getById(1L)

        assert(result.id == 1L)
        assert(result.amount == BigDecimal("5000"))
        assert(result.categoryId == 1L)
    }

    @Test
    fun `getById should throw NoSuchElementException when not found`() {
        every { transactionRepository.findById(99L) } returns Optional.empty()

        assertThrows<NoSuchElementException> {
            transactionService.getById(99L)
        }
    }

    @Test
    fun `create should save and return TransactionDto`() {
        every { categoryRepository.findById(1L) } returns Optional.of(category)
        every { transactionRepository.save(any()) } returns transaction

        val command = CreateTransactionCommand(
            amount = BigDecimal("5000"),
            description = "Monthly salary",
            date = LocalDate.of(2026, 5, 27),
            categoryId = 1L
        )

        val result = transactionService.create(command)

        assert(result.amount == BigDecimal("5000"))
        assert(result.categoryName == "Salary")
        verify(exactly = 1) { categoryRepository.findById(1L) }
        verify(exactly = 1) { transactionRepository.save(any()) }
    }

    @Test
    fun `create should throw NoSuchElementException when category not found`() {
        every { categoryRepository.findById(99L) } returns Optional.empty()

        val command = CreateTransactionCommand(
            amount = BigDecimal("5000"),
            description = null,
            date = LocalDate.of(2026, 5, 27),
            categoryId = 99L
        )

        assertThrows<NoSuchElementException> {
            transactionService.create(command)
        }
        verify(exactly = 0) { transactionRepository.save(any()) }
    }

    @Test
    fun `delete should call deleteById when transaction exists`() {
        every { transactionRepository.existsById(1L) } returns true
        every { transactionRepository.deleteById(1L) } returns Unit

        transactionService.delete(1L)

        verify(exactly = 1) { transactionRepository.deleteById(1L) }
    }

    @Test
    fun `delete should throw NoSuchElementException when not found`() {
        every { transactionRepository.existsById(99L) } returns false

        assertThrows<NoSuchElementException> {
            transactionService.delete(99L)
        }
        verify(exactly = 0) { transactionRepository.deleteById(any()) }
    }

    @Test
    fun `update should change provided fields and keep the others`() {
        every { transactionRepository.findById(1L) } returns Optional.of(transaction)
        every { transactionRepository.saveAndFlush(any()) } answers { firstArg() }

        val request = UpdateTransactionCommand(
            amount = BigDecimal("7000"),
            description = "Raise",
            version = transaction.version
        )

        val result = transactionService.update(1L, request)

        assert(result.amount == BigDecimal("7000"))
        assert(result.description == "Raise")
        assert(result.date == LocalDate.of(2026, 5, 27)) // unchanged
        assert(result.categoryName == "Salary")           // unchanged
        verify(exactly = 1) { transactionRepository.saveAndFlush(any()) }
        verify(exactly = 0) { categoryRepository.findById(any()) }
    }

    @Test
    fun `update should change category when categoryId is provided`() {
        val newCategory = Category(id = 2L, name = "Bonus", type = CategoryType.INCOME)
        every { transactionRepository.findById(1L) } returns Optional.of(transaction)
        every { categoryRepository.findById(2L) } returns Optional.of(newCategory)
        every { transactionRepository.saveAndFlush(any()) } answers { firstArg() }

        val result =
            transactionService.update(1L, UpdateTransactionCommand(categoryId = 2L, version = transaction.version))

        assert(result.categoryId == 2L)
        assert(result.categoryName == "Bonus")
        verify(exactly = 1) { transactionRepository.saveAndFlush(any()) }
    }

    @Test
    fun `update should throw NoSuchElementException when transaction not found`() {
        every { transactionRepository.findById(99L) } returns Optional.empty()

        assertThrows<NoSuchElementException> {
            transactionService.update(99L, UpdateTransactionCommand(amount = BigDecimal("1"), version = 0))
        }
        verify(exactly = 0) { transactionRepository.saveAndFlush(any()) }
    }

    @Test
    fun `getMonthlySummary aggregates totals, balance and groups by category`() {
        val expenseCategory = Category(id = 2L, name = "Rent", type = CategoryType.EXPENSE)
        val expense1 = Transaction(
            id = 2L,
            amount = BigDecimal("1500"),
            date = LocalDate.of(2026, 5, 10),
            type = TransactionType.EXPENSE,
            category = expenseCategory
        )
        val expense2 = Transaction(
            id = 3L,
            amount = BigDecimal("500"),
            date = LocalDate.of(2026, 5, 20),
            type = TransactionType.EXPENSE,
            category = expenseCategory
        )
        every { transactionRepository.findAllByMonth(LocalDate.of(2026, 5, 1)) } returns
                listOf(transaction, expense1, expense2)

        val result = transactionService.getMonthlySummary(2026, 5)

        assert(result.month == "2026-05")
        assert(result.totalIncome == BigDecimal("5000"))
        assert(result.totalExpense == BigDecimal("2000")) // 1500 + 500, same category
        assert(result.balance == BigDecimal("3000"))
        assert(result.byCategory.size == 2)
        // sorted by total descending: Salary (5000) before Rent (2000)
        assert(result.byCategory[0].categoryName == "Salary")
        assert(result.byCategory[1].categoryName == "Rent")
        assert(result.byCategory[1].total == BigDecimal("2000"))
    }

    @Test
    fun `update should succeed when version matches and return incremented version`() {
        every { transactionRepository.findById(1L) } returns Optional.of(transaction)
        every { transactionRepository.saveAndFlush(any()) } answers {
            firstArg<Transaction>().apply { version++ }
        }

        val result = transactionService.update(
            1L,
            UpdateTransactionCommand(amount = BigDecimal("999"), version = transaction.version)
        )

        verify(exactly = 1) { transactionRepository.saveAndFlush(any()) }
        assert(result.amount == BigDecimal("999"))
        assert(result.version == 1L)
    }

    @Test
    fun `update should throw OptimisticLockException when version does not match`() {
        every { transactionRepository.findById(1L) } returns Optional.of(transaction) // version=0

        assertThrows<jakarta.persistence.OptimisticLockException> {
            transactionService.update(
                1L,
                UpdateTransactionCommand(amount = BigDecimal("999"), version = 99L) // wrong version
            )
        }
        verify(exactly = 0) { transactionRepository.saveAndFlush(any()) }
    }
}