package com.example.financetracker.category

import com.example.financetracker.IntegrationTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryIntegrationTest : IntegrationTestBase() {

    @Test
    fun `create category`() {
        val (_, resultStatus) =  addCategory("Groceries", CategoryType.EXPENSE)
        assertEquals(HttpStatus.CREATED, resultStatus)
    }

    @Test
    fun `create category, invalid body`() {
        val result =  restTemplate.exchange(
            "/api/categories",
            HttpMethod.POST,
            HttpEntity<Void>( headers),
            ProblemDetail::class.java
        )
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.statusCode)
    }

    @Test
    fun `create category, no token`() {
        val request = CreateCategoryRequest("Groceries", CategoryType.EXPENSE)
        val result = restTemplate.postForEntity("/api/categories", request, ProblemDetail::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    fun `get all categories`() {
        addCategory("Groceries", CategoryType.EXPENSE)

        val result = restTemplate.exchange(
            "/api/categories",
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            Array<CategoryDto>::class.java
        )
        assertEquals(HttpStatus.OK, result.statusCode)
        val categories = result.body!!
        assertEquals(1, categories.size )
        assertTrue(categories.any{it.name == "Groceries"})
    }

    @Test
    fun `get all categories, no token`() {
        val result = restTemplate.getForEntity("/api/categories", ProblemDetail::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    fun `get by id, not found`() {
        val result = restTemplate.exchange(
            "/api/categories{id}",
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            ProblemDetail::class.java,
            mapOf("id" to 100)
        )
        assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    fun `get by id`() {
        addCategory("Groceries", CategoryType.EXPENSE)
        val result = restTemplate.exchange(
            "/api/categories/{id}",
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            CategoryDto::class.java,
            mapOf("id" to 1)
        )
        assertEquals(HttpStatus.OK, result.statusCode)
    }

    @Test
    fun `get by id, no token`() {
        addCategory("Groceries", CategoryType.EXPENSE)
        val result = restTemplate.getForEntity(
            "/api/categories/{id}",
            ProblemDetail::class.java,
            mapOf("id" to 1)
        )
        assertEquals(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    fun `delete by id`() {
        val (response, _) = addCategory("Groceries", CategoryType.EXPENSE)
        val result = restTemplate.exchange(
            "/api/categories/{id}",
            HttpMethod.DELETE,
            HttpEntity<Void>(headers),
            CategoryDto::class.java,
            mapOf("id" to response.id)
        )
        assertEquals(HttpStatus.NO_CONTENT, result.statusCode)
    }

    @Test
    fun `delete by wrong id`() {
        val result = restTemplate.exchange(
            "/api/categories/{id}",
            HttpMethod.DELETE,
            HttpEntity<Void>(headers),
            ProblemDetail::class.java,
            mapOf("id" to 1000)
        )
        assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    fun `delete by id, no token`() {
        val result = restTemplate.exchange(
            "/api/categories/{id}",
            HttpMethod.DELETE,
            HttpEntity.EMPTY,
            ProblemDetail::class.java,
            mapOf("id" to 1),
        )
        assertEquals(HttpStatus.UNAUTHORIZED, result.statusCode)
    }
}