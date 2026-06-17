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

class TransactionIntegrationTest : IntegrationTestBase() {


    @Test
    fun `get by id, no token`() {
        val result = restTemplate.getForEntity("/api/transactions/{id}", ProblemDetail::class.java, mapOf("id" to 1))
        assertEquals(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    fun `get by id`() {
        val id = addTransaction(
            "Groceries", CategoryType.EXPENSE, BigDecimal(150), description = "test transaction",
            TransactionType.EXPENSE
        )

        restTemplate.exchange(
            "/api/transactions/{id}",
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            TransactionDto::class.java, mapOf("id" to id)
        )
    }

    @Test
    fun `get all`() {
        addTransaction(
            "Groceries", CategoryType.EXPENSE, BigDecimal(150), description = "test transaction",
            TransactionType.EXPENSE
        )

        addTransaction(
            "Salary", CategoryType.INCOME, BigDecimal(150), description = "test transaction",
            TransactionType.INCOME
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
            "/api/transactions",
            HttpMethod.DELETE,
            HttpEntity.EMPTY,
            ProblemDetail::class.java
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
        val id = addTransaction(
            "Groceries", CategoryType.EXPENSE, BigDecimal(150), description = "test transaction",
            TransactionType.EXPENSE
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
            "/api/transactions/summary?year={year}&month={month}",
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            ProblemDetail::class.java
        )
        assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
    }
    @Test
    fun `get summary`() {
        addTransaction(
            "Groceries", CategoryType.EXPENSE, BigDecimal(150), description = "test transaction",
            TransactionType.EXPENSE
        )

        addTransaction(
            "Salary", CategoryType.INCOME, BigDecimal(150), description = "test transaction",
            TransactionType.INCOME
        )

        val result = restTemplate.exchange(
            "/api/transactions/summary?year={year}&month={month}",
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            MonthlySummary::class.java,
            mapOf("year" to LocalDate.now().year, "month" to LocalDate.now().monthValue)
        )
        assertEquals(HttpStatus.OK, result.statusCode)
    }

    @Test
    fun `update transaction`(){
        val id = addTransaction(
            "Groceries", CategoryType.EXPENSE, BigDecimal(150), description = "test transaction",
            TransactionType.EXPENSE
        )
        val transaction = restTemplate.exchange(
            "/api/transactions/{id}",
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            TransactionDto::class.java,
            mapOf("id" to id)
        )
        val request = transaction.body!!.copy(amount = BigDecimal(200))
        val result = restTemplate.exchange(
            "/api/transactions/{id}",
            HttpMethod.PATCH,
            HttpEntity(request, headers),
            TransactionDto::class.java,
            mapOf("id" to id)
        )
        assertEquals(BigDecimal(200), result.body!!.amount)
    }

    @Test
    fun `patch, version conflict`(){
        val id = addTransaction(
            "Groceries", CategoryType.EXPENSE, BigDecimal(150), description = "test transaction",
            TransactionType.EXPENSE
        )
        val transaction = restTemplate.exchange(
            "/api/transactions/{id}",
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            TransactionDto::class.java,
            mapOf("id" to id)
        )
        val request = transaction.body!!.copy(amount = BigDecimal(200), version = 3)
        val result = restTemplate.exchange(
            "/api/transactions/{id}",
            HttpMethod.PATCH,
            HttpEntity(request, headers),
            ProblemDetail::class.java,
            mapOf("id" to id)
        )
        assertEquals(HttpStatus.CONFLICT, result.statusCode)
    }

    fun addTransaction(
        categoryName: String,
        categoryType: CategoryType,
        amount: BigDecimal,
        description: String,
        transactionType: TransactionType
    ): Long {
        val (categoryResponse, _) = addCategory(categoryName, categoryType)

        val request = CreateTransactionRequest(
            amount = amount,
            description = description,
            date = LocalDate.now(),
            type = transactionType,
            categoryId = categoryResponse.id
        )

        val result = restTemplate.exchange(
            "/api/transactions", HttpMethod.POST, HttpEntity(request, headers),
            TransactionDto::class.java
        )

        assertEquals(HttpStatus.CREATED, result.statusCode)
        return result.body!!.id
    }
}