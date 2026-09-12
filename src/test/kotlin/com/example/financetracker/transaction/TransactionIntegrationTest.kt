package com.example.financetracker.transaction

import com.example.financetracker.IntegrationTestBase
import com.example.financetracker.category.CategoryType
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransactionIntegrationTest : IntegrationTestBase() {


    @Test
    fun `get by id, no token`() {
        val result = restTemplate.getForEntity("/api/transactions/{id}", ProblemDetail::class.java, mapOf("id" to 1))
        assertEquals(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    fun `get by id`() {
        val amount = BigDecimal(150)
        val id = addTransactionWithCategory("Groceries", CategoryType.EXPENSE, amount)

        val result = restTemplate.exchange(
            "/api/transactions/{id}",
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            TransactionDto::class.java, mapOf("id" to id)
        )
        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(0, result.body!!.amount.compareTo(amount))
        assertEquals(id, result.body!!.id)
    }

    @Test
    fun `get all`() {
        addTransactionWithCategory(
            "Groceries", CategoryType.EXPENSE, BigDecimal(150)
        )

        addTransactionWithCategory(
            "Salary", CategoryType.INCOME, BigDecimal(150)
        )

        val result = restTemplate.exchange(
            "/api/transactions",
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            Array<TransactionDto>::class.java
        )
        assertEquals(2, result.body?.size)
    }

    @Test
    fun `add transaction, no token`() {
        val request = CreateTransactionRequest(
            amount = BigDecimal(100),
            description = "test description",
            date = LocalDate.now(),
            type = TransactionType.EXPENSE,
            categoryId = 1
        )
        val result =
            restTemplate.postForEntity("/api/transactions", HttpEntity(request), ProblemDetail::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    fun `add transaction, wrong amount`() {
        val (categoryResponse, _) = addCategory("Groceries", CategoryType.EXPENSE)

        val request = CreateTransactionRequest(
            amount = BigDecimal(-100),
            description = "test description",
            date = LocalDate.now(),
            type = TransactionType.EXPENSE,
            categoryId = categoryResponse.id
        )
        val result =
            restTemplate.postForEntity("/api/transactions", HttpEntity(request, headers), ProblemDetail::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
    }

    @Test
    fun `delete, no token`() {
        val result = restTemplate.exchange(
            "/api/transactions/{id}",
            HttpMethod.DELETE,
            HttpEntity.EMPTY,
            ProblemDetail::class.java,
            mapOf("id" to 1)
        )
        assertEquals(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    fun `delete, wrong id`() {
        val result = restTemplate.exchange(
            "/api/transactions/{id}",
            HttpMethod.DELETE,
            HttpEntity<Void>(headers),
            ProblemDetail::class.java,
            mapOf("id" to 1)
        )
        assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    fun `delete by id`() {
        val id = addTransactionWithCategory(
            "Groceries", CategoryType.EXPENSE, BigDecimal(150)
        )
        val result = restTemplate.exchange(
            "/api/transactions/{id}",
            HttpMethod.DELETE,
            HttpEntity<Void>(headers),
            ProblemDetail::class.java,
            mapOf("id" to id)
        )
        assertEquals(HttpStatus.NO_CONTENT, result.statusCode)
    }

    @Test
    fun `get summary, no parameters`() {
        val result = restTemplate.exchange(
            "/api/transactions/summary",
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            ProblemDetail::class.java
        )
        assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
    }

    @Test
    fun `get summary`() {
        val incomeAmount = BigDecimal(200)
        val expenseAmount = BigDecimal(150)
        addTransactionWithCategory(
            "Groceries", CategoryType.EXPENSE, expenseAmount
        )

        addTransactionWithCategory(
            "Salary", CategoryType.INCOME, incomeAmount
        )

        val result = restTemplate.exchange(
            "/api/transactions/summary?year={year}&month={month}",
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            MonthlySummary::class.java,
            mapOf("year" to LocalDate.now().year, "month" to LocalDate.now().monthValue)
        )
        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(0, result.body!!.totalIncome.compareTo(incomeAmount), "Wrong IncomeAmount")
        assertEquals(0, result.body!!.totalExpense.compareTo(expenseAmount), "Wrong Expense Amount")
        assertEquals(2, result.body!!.byCategory.size)
    }

    @Test
    fun `update transaction`() {
        val id = addTransactionWithCategory(
            "Groceries", CategoryType.EXPENSE, BigDecimal(150)
        )
        val newAmount = BigDecimal(200)
        val transaction = restTemplate.exchange(
            "/api/transactions/{id}",
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            TransactionDto::class.java,
            mapOf("id" to id)
        )
        val request = transaction.body!!.copy(amount = newAmount).toUpdateTransactionRequest()
        val result = restTemplate.exchange(
            "/api/transactions/{id}",
            HttpMethod.PATCH,
            HttpEntity(request, headers),
            TransactionDto::class.java,
            mapOf("id" to id)
        )
        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(0, result.body!!.amount.compareTo(newAmount))
    }

    @Test
    fun `patch, version conflict`() {
        val id = addTransactionWithCategory(
            "Groceries", CategoryType.EXPENSE, BigDecimal(150)
        )
        val transaction = restTemplate.exchange(
            "/api/transactions/{id}",
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            TransactionDto::class.java,
            mapOf("id" to id)
        )
        val request = transaction.body!!.copy(amount = BigDecimal(200), version = 3).toUpdateTransactionRequest()
        val result = restTemplate.exchange(
            "/api/transactions/{id}",
            HttpMethod.PATCH,
            HttpEntity(request, headers),
            ProblemDetail::class.java,
            mapOf("id" to id)
        )
        assertEquals(HttpStatus.CONFLICT, result.statusCode)
    }

    @Test
    fun `limit exceeded`() {
        val categoryId = addCategory("Groceries", CategoryType.EXPENSE).first.id
        addBudget(categoryId, BigDecimal(1000), "2026-12")
        addTransaction(categoryId, BigDecimal(500), date = LocalDate.of(2026, 12, 10))
        addTransaction(categoryId, BigDecimal(200), date = LocalDate.of(2026, 12, 11))

        val request = CreateTransactionRequest(
            amount = BigDecimal(800),
            date = LocalDate.of(2026, 12, 12),
            categoryId = categoryId
        )
        val result = restTemplate.exchange(
            "/api/transactions", HttpMethod.POST,
            HttpEntity(request, headers), ProblemDetail::class.java
        )

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, result.statusCode)
        assertEquals("Limit Exceeded", result.body?.title)
        assertTrue(result.body?.detail?.contains("Transaction limit by") == true)
    }

    @Test
    fun `transaction within limit succeeds`() {
        val categoryId = addCategory("Groceries", CategoryType.EXPENSE).first.id
        addBudget(categoryId, BigDecimal(1000), "2026-12")
        addTransaction(categoryId, BigDecimal(500), date = LocalDate.of(2026, 12, 10))

        val request = CreateTransactionRequest(amount = BigDecimal(400), date = LocalDate.of(2026, 12, 12), categoryId = categoryId)
        val result = restTemplate.exchange("/api/transactions", HttpMethod.POST,
            HttpEntity(request, headers), TransactionDto::class.java)

        assertEquals(HttpStatus.CREATED, result.statusCode)
    }

    @Test
    fun `no budget means no limit check`() {
        val categoryId = addCategory("Entertainment", CategoryType.EXPENSE).first.id

        val request = CreateTransactionRequest(amount = BigDecimal(999999), date = LocalDate.now(), categoryId = categoryId)
        val result = restTemplate.exchange("/api/transactions", HttpMethod.POST,
            HttpEntity(request, headers), TransactionDto::class.java)

        assertEquals(HttpStatus.CREATED, result.statusCode)
    }

    @Test
    fun `spending exactly at limit succeeds`() {
        val categoryId = addCategory("Groceries", CategoryType.EXPENSE).first.id
        addBudget(categoryId, BigDecimal(1000), "2026-12")
        addTransaction(categoryId, BigDecimal(600), date = LocalDate.of(2026, 12, 10))

        val request = CreateTransactionRequest(amount = BigDecimal(400), date = LocalDate.of(2026, 12, 12), categoryId = categoryId) // 600+400=1000
        val result = restTemplate.exchange("/api/transactions", HttpMethod.POST,
            HttpEntity(request, headers), TransactionDto::class.java)

        assertEquals(HttpStatus.CREATED, result.statusCode)
    }

    @Test
    fun `transactions from other month do not count toward limit`() {
        val categoryId = addCategory("Groceries", CategoryType.EXPENSE).first.id
        addBudget(categoryId, BigDecimal(1000), "2026-12")
        addTransaction(categoryId, BigDecimal(900), date = LocalDate.of(2026, 11, 30))  // ноябрь — не считается

        val request = CreateTransactionRequest(amount = BigDecimal(900), date = LocalDate.of(2026, 12, 1), categoryId = categoryId)
        val result = restTemplate.exchange("/api/transactions", HttpMethod.POST,
            HttpEntity(request, headers), TransactionDto::class.java)

        assertEquals(HttpStatus.CREATED, result.statusCode)   // если бы ноябрь считался — было бы превышение
    }


    private fun TransactionDto.toUpdateTransactionRequest() = UpdateTransactionRequest(
        amount = amount,
        description = description,
        date = date,
        type = type,
        categoryId = categoryId,
        version = version
    )

}