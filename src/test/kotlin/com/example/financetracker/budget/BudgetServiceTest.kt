package com.example.financetracker.budget

import com.example.financetracker.category.Category
import com.example.financetracker.category.CategoryRepository
import com.example.financetracker.category.CategoryType
import com.example.financetracker.user.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.hibernate.exception.ConstraintViolationException
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DataIntegrityViolationException
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals

class BudgetServiceTest {
    private val budgetRepository = mockk<BudgetRepository>()
    private val categoryRepository = mockk<CategoryRepository>()
    private val budgetService = BudgetService(budgetRepository, categoryRepository)

    private val category = Category(
        id = 1L,
        name = "Food",
        type = CategoryType.EXPENSE
    )

    private val user = User(id = 1L, email = "user@mail.ru", password = "password")

    @Test
    fun `budget created successful`() {
        val command = BudgetCommand(BigDecimal("1000.10"), "2026-09", category.id)
        val budget = Budget(
            id = 1L,
            category = category,
            user = user,
            limitAmount = command.limit,
            period = command.period
        )
        every { categoryRepository.findById(1L) } returns Optional.of(category)
        every {
            budgetRepository.existsByUserIdAndCategoryIdAndPeriod(
                user.id,
                category.id,
                period = command.period
            )
        } returns false

        val slot = slot<Budget>()
        every { budgetRepository.save(capture(slot)) } returns budget

        val result = budgetService.create(user, command)

        val saved = slot.captured
        assertEquals(user, saved.user)
        assertEquals(category, saved.category)
        assertEquals(command.limit, saved.limitAmount)
        assertEquals(budget.toDto(), result)
        verify(exactly = 1) { budgetRepository.save(any()) }
    }

    @Test
    fun `duplicate budget`() {
        val command = BudgetCommand(BigDecimal("1000.10"), "2026-09", category.id)

        every { categoryRepository.findById(1L) } returns Optional.of(category)
        every {
            budgetRepository.existsByUserIdAndCategoryIdAndPeriod(
                user.id,
                category.id,
                period = command.period
            )
        } returns true

        assertThrows<DuplicateBudgetException> { budgetService.create(user, command) }
        verify(exactly = 0) { budgetRepository.save(any()) }
    }

    @Test
    fun `duplicate budget with UK`() {
        val command = BudgetCommand(BigDecimal("1000.10"), "2026-09", category.id)
        every { categoryRepository.findById(1L) } returns Optional.of(category)
        every {
            budgetRepository.existsByUserIdAndCategoryIdAndPeriod(
                user.id,
                category.id,
                period = command.period
            )
        } returns false

        val cause = mockk<ConstraintViolationException>()
        every { cause.constraintName } returns "uq_budget_user_category_period"
        every { budgetRepository.save(any()) } throws DataIntegrityViolationException("dup", cause)

        assertThrows<DuplicateBudgetException> { budgetService.create(user, command) }
    }

    @Test
    fun `non-duplicate integrity violation is rethrown`() {
        val command = BudgetCommand(BigDecimal("1000.10"), "2026-09", category.id)
        every { categoryRepository.findById(1L) } returns Optional.of(category)
        every {
            budgetRepository.existsByUserIdAndCategoryIdAndPeriod(
                user.id,
                category.id,
                period = command.period
            )
        } returns false

        every { budgetRepository.save(any()) } throws DataIntegrityViolationException("some violation")
        assertThrows<DataIntegrityViolationException> { budgetService.create(user, command) }

    }

    @Test
    fun `category not found`() {
        val command = BudgetCommand(BigDecimal("1000.10"), "2026-09", category.id)
        every { categoryRepository.findById(1L) } returns Optional.empty()
        assertThrows<NoSuchElementException> { budgetService.create(user, command) }
        verify(exactly = 0) { budgetRepository.save(any()) }
    }

    @Test
    fun `scale of limit`() {
        val command = BudgetCommand(BigDecimal("1000.10"), "2026-09", category.id)
        val budget = Budget(
            id = 1L,
            category = category,
            user = user,
            limitAmount = command.limit.setScale(2, RoundingMode.HALF_UP),
            period = command.period
        )
        every { categoryRepository.findById(1L) } returns Optional.of(category)
        every {
            budgetRepository.existsByUserIdAndCategoryIdAndPeriod(
                user.id,
                category.id,
                period = command.period
            )
        } returns false

        val slot = slot<Budget>()
        every { budgetRepository.save(capture(slot)) } returns budget
        budgetService.create(user, command)
        assertEquals(command.limit.setScale(2), slot.captured.limitAmount)
        assertEquals(2, slot.captured.limitAmount.scale())
    }

    @Test
    fun `get by user`() {
        val category = Category(id = 1, name = "Food", type = CategoryType.EXPENSE)
        val budget = listOf(
            Budget(
                id = 1L,
                category = category,
                user = user,
                limitAmount = BigDecimal(1000),
                period = "2026-09"
            ),
            Budget(
                id = 2L,
                category = category,
                user = user,
                limitAmount = BigDecimal(1500),
                period = "2026-10"
            )
        )
        every{ budgetRepository.findByUserIdOrderByPeriodDesc(user.id)} returns budget
        val result = budgetService.get(user.id)
        assertEquals(2, result.size)
        verify(exactly = 1) { budgetRepository.findByUserIdOrderByPeriodDesc(1L) }
        verify(exactly = 0) { budgetRepository.findByUserIdAndPeriodOrderByPeriodDesc(any(), any()) }
    }

    @Test
    fun `get by user and period`() {
        val category = Category(id = 1, name = "Food", type = CategoryType.EXPENSE)
        val budget = listOf(
            Budget(
                id = 1L,
                category = category,
                user = user,
                limitAmount = BigDecimal(1000),
                period = "2026-09"
            )
        )
        every{ budgetRepository.findByUserIdAndPeriodOrderByPeriodDesc(user.id, "2026-09")} returns budget
        val result = budgetService.get(user.id, "2026-09")
        assertEquals(1, result.size)
        verify(exactly = 1) { budgetRepository.findByUserIdAndPeriodOrderByPeriodDesc(1L, "2026-09") }
        verify(exactly = 0) { budgetRepository.findByUserIdOrderByPeriodDesc(any()) }
    }

    @Test
    fun `get by wrong userId`(){
        every{ budgetRepository.findByUserIdOrderByPeriodDesc(123L)} returns emptyList()
        val result = budgetService.get(123L)
        assertEquals(emptyList(), result)
    }

}