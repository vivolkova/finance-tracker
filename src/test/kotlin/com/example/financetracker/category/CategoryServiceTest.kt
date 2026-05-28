package com.example.financetracker.category

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional

class CategoryServiceTest {

    private val categoryRepository = mockk<CategoryRepository>()
    private val categoryService = CategoryService(categoryRepository)

    @Test
    fun `getAll should return list of CategoryDto`() {
        // given
        val categories = listOf(
            Category(id = 1L, name = "Salary", type = CategoryType.INCOME),
            Category(id = 2L, name = "Groceries", type = CategoryType.EXPENSE)
        )
        every { categoryRepository.findAll() } returns categories

        // when
        val result = categoryService.getAll()

        // then
        assert(result.size == 2)
        assert(result[0].name == "Salary")
        assert(result[1].type == CategoryType.EXPENSE)
        verify(exactly = 1) { categoryRepository.findAll() }
    }

    @Test
    fun `getById should return CategoryDto when found`() {
        // given
        val category = Category(id = 1L, name = "Salary", type = CategoryType.INCOME)
        every { categoryRepository.findById(1L) } returns Optional.of(category)

        // when
        val result = categoryService.getById(1L)

        // then
        assert(result.id == 1L)
        assert(result.name == "Salary")
    }

    @Test
    fun `getById should throw NoSuchElementException when not found`() {
        // given
        every { categoryRepository.findById(99L) } returns Optional.empty()

        // when/then
        assertThrows<NoSuchElementException> {
            categoryService.getById(99L)
        }
    }

    @Test
    fun `create should save and return CategoryDto`() {
        // given
        val category = Category(id = 1L, name = "Salary", type = CategoryType.INCOME)
        every { categoryRepository.save(any()) } returns category

        // when
        val result = categoryService.create("Salary", CategoryType.INCOME)

        // then
        assert(result.name == "Salary")
        assert(result.type == CategoryType.INCOME)
        verify(exactly = 1) { categoryRepository.save(any()) }
    }

    @Test
    fun `delete should call deleteById when category exists`() {
        // given
        every { categoryRepository.existsById(1L) } returns true
        every { categoryRepository.deleteById(1L) } returns Unit

        // when
        categoryService.delete(1L)

        // then
        verify(exactly = 1) { categoryRepository.deleteById(1L) }
    }

    @Test
    fun `delete should throw NoSuchElementException when not found`() {
        // given
        every { categoryRepository.existsById(99L) } returns false

        // when/then
        assertThrows<NoSuchElementException> {
            categoryService.delete(99L)
        }
    }
}