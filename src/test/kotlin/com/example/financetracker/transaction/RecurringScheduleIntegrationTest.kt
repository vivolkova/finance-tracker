package com.example.financetracker.transaction

import com.example.financetracker.IntegrationTestBase
import com.example.financetracker.category.CategoryType
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals

class RecurringScheduleIntegrationTest : IntegrationTestBase() {

    @Test
    fun `get by transaction id`() {
        val transactionId = addTransaction("Groceries", CategoryType.EXPENSE, BigDecimal(100))
        val scheduleId = createSchedule(transactionId, Frequency.DAILY)

        val result = restTemplate.exchange(
            "/api/transactions/{transactionId}/schedule",
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            RecurringScheduleDto::class.java,
            mapOf("transactionId" to transactionId)
        )
        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(scheduleId, result.body!!.id)
    }

    @Test
    fun `delete by TransactionId`() {
        val transactionId = addTransaction("Groceries", CategoryType.EXPENSE, BigDecimal(100))
        createSchedule(transactionId, Frequency.DAILY)

        val result = restTemplate.exchange(
            "/api/transactions/{transactionId}/schedule",
            HttpMethod.DELETE,
            HttpEntity<Void>(headers),
            ProblemDetail::class.java,
            mapOf("transactionId" to transactionId)
        )
        assertEquals(HttpStatus.NO_CONTENT, result.statusCode)
   }

    @Test
    fun `create, no token`() {
        val transactionId = addTransaction("Groceries", CategoryType.EXPENSE, BigDecimal(100))
        val request = CreateScheduleRequest(Frequency.DAILY, null, LocalDate.now(), LocalDate.now().plusDays(30))

        val result = restTemplate.exchange(
            "/api/transactions/{transactionId}/schedule",
            HttpMethod.POST,
            HttpEntity(request),
            ProblemDetail::class.java,
            mapOf("transactionId" to transactionId)
        )
        assertEquals(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    private fun createSchedule(transactionId: Long, frequency: Frequency, dayOfMonth: Int? = null): Long {
        val request = CreateScheduleRequest(frequency, dayOfMonth, LocalDate.now(), LocalDate.now().plusDays(30))
        val result = restTemplate.exchange(
            "/api/transactions/{transactionId}/schedule",
            HttpMethod.POST,
            HttpEntity(request, headers),
            RecurringScheduleDto::class.java,
            mapOf("transactionId" to transactionId)
        )
        assertEquals(HttpStatus.CREATED, result.statusCode)
        return result.body!!.id
    }
}