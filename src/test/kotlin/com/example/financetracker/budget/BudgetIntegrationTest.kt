package com.example.financetracker.budget

import com.example.financetracker.IntegrationTestBase
import com.example.financetracker.category.CategoryType
import com.example.financetracker.errorsOf
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BudgetIntegrationTest : IntegrationTestBase() {


    @Test
    fun `zero limitAmount`() {
        val category = addCategory("Food", CategoryType.EXPENSE)
        val request = BudgetRequest(
            limitAmount = BigDecimal("0"),
            categoryId = category.first.id,
            period = "2027-01"
        )

        val result =
            restTemplate.exchange(
                "/api/budgets",
                HttpMethod.POST,
                HttpEntity(request, headers),
                ProblemDetail::class.java
            )

        assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
        val errors = errorsOf(result.body)
        assertTrue(errors.any { it["field"] == "limitAmount" && it["message"] == "Limit amount must be positive" })
    }

    @Test
    fun `negative limitAmount`() {
        val category = addCategory("Food", CategoryType.EXPENSE)
        val request = BudgetRequest(
            limitAmount = BigDecimal("-10"),
            categoryId = category.first.id,
            period = "2027-01"
        )

        val result =
            restTemplate.exchange(
                "/api/budgets",
                HttpMethod.POST,
                HttpEntity(request, headers),
                ProblemDetail::class.java
            )

        assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
        val errors = errorsOf(result.body)
        assertTrue(errors.any { it["field"] == "limitAmount" && it["message"] == "Limit amount must be positive" })
    }

    @Test
    fun `wrong scale limitAmount`() {
        val category = addCategory("Food", CategoryType.EXPENSE)
        val request = BudgetRequest(
            limitAmount = BigDecimal("10.1234"),
            categoryId = category.first.id,
            period = "2027-01"
        )

        val result =
            restTemplate.exchange(
                "/api/budgets",
                HttpMethod.POST,
                HttpEntity(request, headers),
                ProblemDetail::class.java
            )

        assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
        val errors = errorsOf(result.body)
        assertTrue(errors.any { it["field"] == "limitAmount" && it["message"] == "LimitAmount scale must be less or equal 2" })
    }

    @Test
    fun `wrong period`() {
        val category = addCategory("Food", CategoryType.EXPENSE)
        val request = BudgetRequest(
            limitAmount = BigDecimal("10.12"),
            categoryId = category.first.id,
            period = "2027-01-01"
        )

        val result =
            restTemplate.exchange(
                "/api/budgets",
                HttpMethod.POST,
                HttpEntity(request, headers),
                ProblemDetail::class.java
            )

        assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
        val errors = errorsOf(result.body)
        assertTrue(errors.any { it["field"] == "period" && it["message"] == "Period must be YYYY-MM" })
    }

    @Test
    fun `create budget`() {
        val category = addCategory("Food", CategoryType.EXPENSE)
        val request = BudgetRequest(
            limitAmount = BigDecimal("10000"),
            categoryId = category.first.id,
            period = "2027-01"
        )

        val result =
            restTemplate.exchange("/api/budgets", HttpMethod.POST, HttpEntity(request, headers), BudgetDto::class.java)

        assertEquals(HttpStatus.CREATED, result.statusCode)
        assertEquals(request.limitAmount.setScale(2, RoundingMode.HALF_UP), result.body?.limitAmount)
    }

    @Test
    fun `create budget no token`() {
        val category = addCategory("Food", CategoryType.EXPENSE)
        val request = BudgetRequest(
            limitAmount = BigDecimal("10000"),
            categoryId = category.first.id,
            period = "2027-01"
        )

        val result =
            restTemplate.exchange("/api/budgets", HttpMethod.POST, HttpEntity(request), BudgetDto::class.java)

        assertEquals(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    fun `duplicate budget`() {
        val category = addCategory("Food", CategoryType.EXPENSE)
        val request = BudgetRequest(
            limitAmount = BigDecimal("10000"),
            categoryId = category.first.id,
            period = "2027-01"
        )

        restTemplate.exchange("/api/budgets", HttpMethod.POST, HttpEntity(request, headers), BudgetDto::class.java)

        val result =
            restTemplate.exchange(
                "/api/budgets",
                HttpMethod.POST,
                HttpEntity(request, headers),
                ProblemDetail::class.java
            )


        assertEquals(HttpStatus.CONFLICT, result.statusCode)
        assertEquals("Budget already exists for this category and period", result.body?.detail)
    }

    @Test
    fun `same budget for different users`() {
        val category = addCategory("Food", CategoryType.EXPENSE)
        val request = BudgetRequest(
            limitAmount = BigDecimal("10000"),
            categoryId = category.first.id,
            period = "2027-01"
        )

        val result1 =
            restTemplate.exchange(
                "/api/budgets",
                HttpMethod.POST,
                HttpEntity(request, headers),
                BudgetDto::class.java
            )

        val headers2 = registerUser("test2@mail.ru", "12345")
        val result2 =
            restTemplate.exchange(
                "/api/budgets",
                HttpMethod.POST,
                HttpEntity(request, headers2),
                BudgetDto::class.java
            )

        assertEquals(HttpStatus.CREATED, result1.statusCode, "Budget 1")
        assertEquals(HttpStatus.CREATED, result2.statusCode, "Budget 2")
    }

    @Test
    fun `wrong Category`() {
        val request = BudgetRequest(
            limitAmount = BigDecimal("10000"),
            categoryId = 123,
            period = "2027-01"
        )

        val result = restTemplate.exchange(
            "/api/budgets",
            HttpMethod.POST,
            HttpEntity(request, headers),
            ProblemDetail::class.java
        )

        assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
        assertEquals("Category not found with id: 123", result.body?.detail)
    }

    @Test
    fun `create budget, invalid body`() {
        val result =  restTemplate.exchange(
            "/api/budgets",
            HttpMethod.POST,
            HttpEntity<Void>( headers),
            ProblemDetail::class.java
        )
        assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
        assertEquals("Malformed or missing request body", result.body?.detail)
    }

    @Test
    fun `get budgets by userId and period`() {
        val categoryId = addCategory("Food", CategoryType.EXPENSE).first.id
        addBudget(categoryId, BigDecimal(1000), "2027-01")

        val result = restTemplate.exchange(
            "/api/budgets?period={period}",
            HttpMethod.GET,
            HttpEntity<Void>( headers),
            Array<BudgetDto>::class.java,
            mapOf("period" to "2027-01")
        )

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(1, result.body?.size)
        assertEquals("2027-01", result.body?.get(0)?.period)
    }

    @Test
    fun `get budgets by userId with no token`() {
        val result = restTemplate.exchange(
            "/api/budgets",
            HttpMethod.GET,
            HttpEntity.EMPTY,
            Array<BudgetDto>::class.java)

        assertEquals(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    fun `get no budgets by userId and period`() {
        val result = restTemplate.exchange(
            "/api/budgets?period={period}",
            HttpMethod.GET,
            HttpEntity<Void>( headers),
            Array<BudgetDto>::class.java,
            mapOf("period" to "2028-01")
        )

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(0, result.body?.size)
    }

    @Test
    fun `get budgets by userId`() {
        val categoryId = addCategory("Food", CategoryType.EXPENSE).first.id
        addBudget(categoryId, BigDecimal(1000), "2027-01")
        addBudget(categoryId, BigDecimal(1000), "2027-02")

        val result = restTemplate.exchange(
            "/api/budgets",
            HttpMethod.GET,
            HttpEntity<Void>( headers),
            Array<BudgetDto>::class.java
        )

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(2, result.body?.size)
        assertEquals("2027-02", result.body?.get(0)?.period)
        assertEquals("2027-01", result.body?.get(1)?.period)
    }

    @Test
    fun `get budgets isolates users`() {
        val categoryId = addCategory("Food", CategoryType.EXPENSE).first.id
        addBudget(categoryId, BigDecimal(1000), "2027-01")

        val headers2 = registerUser("test2@mail.ru", "12345")
        val result = restTemplate.exchange(
            "/api/budgets", HttpMethod.GET, HttpEntity<Void>(headers2), Array<BudgetDto>::class.java)
        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(0, result.body?.size)               // чужого не видно
    }

    @Test
    fun `get budgets invalid period`() {
        val result = restTemplate.exchange(
            "/api/budgets?period={period}", HttpMethod.GET,
            HttpEntity<Void>(headers), ProblemDetail::class.java, mapOf("period" to "2027-13"))
        assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
        assertEquals("Period must be YYYY-MM", result.body?.detail)
    }

}