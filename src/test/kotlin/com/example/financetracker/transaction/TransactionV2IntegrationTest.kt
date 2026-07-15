package com.example.financetracker.transaction

import com.example.financetracker.IntegrationTestBase
import com.example.financetracker.category.CategoryType
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals

class TransactionV2IntegrationTest : IntegrationTestBase() {

    @Test
    fun `create and get transaction via v2`() {
        val (category, _) = addCategory("Salary", CategoryType.INCOME)

        val createReq = CreateTransactionV2Request(
            amount = BigDecimal(200),
            description = "salary",
            date = LocalDate.now(),
            categoryId = category.id
        )
        val created = restTemplate.exchange(
            "/api/v2/transactions", HttpMethod.POST,
            HttpEntity(createReq, headers), TransactionV2Dto::class.java
        )
        assertEquals(HttpStatus.CREATED, created.statusCode)
        val id = created.body!!.id

        val fetched = restTemplate.exchange(
            "/api/v2/transactions/{id}", HttpMethod.GET,
            HttpEntity<Void>(headers), TransactionV2Dto::class.java, mapOf("id" to id)
        )
        assertEquals(HttpStatus.OK, fetched.statusCode)
        assertEquals(0, fetched.body!!.amount.compareTo(BigDecimal(200)))
        assertEquals(category.id, fetched.body!!.categoryId)
    }

    @Test
    fun `summary via v2`() {
        addTransaction("Salary", CategoryType.INCOME, BigDecimal(200))
        addTransaction("Groceries", CategoryType.EXPENSE, BigDecimal(150))

        val result = restTemplate.exchange(
            "/api/v2/transactions/summary?year={y}&month={m}", HttpMethod.GET,
            HttpEntity<Void>(headers), MonthlySummary::class.java,
            mapOf("y" to LocalDate.now().year, "m" to LocalDate.now().monthValue)
        )
        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(0, result.body!!.totalIncome.compareTo(BigDecimal(200)))
        assertEquals(0, result.body!!.totalExpense.compareTo(BigDecimal(150)))
    }
}