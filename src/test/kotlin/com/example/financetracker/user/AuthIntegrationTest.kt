package com.example.financetracker.user

import com.example.financetracker.IntegrationTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class AuthIntegrationTest : IntegrationTestBase() {

    @Test
    fun `register test`() {
        val user = RegisterRequest("user1@mail.ru", "1234")
        val registerResult = restTemplate.postForEntity("/api/auth/register", user, AuthResponse::class.java)
        assertEquals(HttpStatus.CREATED, registerResult.statusCode, "Wrong Status Code")
        assertNotNull(registerResult.body?.accessToken, "Wrong Access token")
        assertNotNull(registerResult.body?.refreshToken, "Wrong Refresh Token")
    }

    @Test
    fun `register test with duplicate email`() {
        val userDupl = RegisterRequest(email, password)
        val registerResult = restTemplate.postForEntity("/api/auth/register", userDupl, ProblemDetail::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, registerResult.statusCode, "Wrong Status Code")
        assertEquals("User with email $email already exists", registerResult.body?.detail, "Wrong result message")
    }

    @Test
    fun `login test with wrong password`() {
        val user = LoginRequest(email, "password")
        val loginResult = restTemplate.postForEntity("/api/auth/login", user, ProblemDetail::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, loginResult.statusCode, "Wrong Status Code")
        assertEquals("Invalid email or password", loginResult.body?.detail, "Wrong result message")
    }

    @Test
    fun `login test`() {
        val loginRequest = LoginRequest(email, password)
        val loginResult = restTemplate.postForEntity("/api/auth/login", loginRequest, AuthResponse::class.java)
        assertNotNull(loginResult.body?.accessToken, "Wrong Access token")
        assertNotNull(loginResult.body?.refreshToken, "Wrong Refresh Token")
    }

    @Test
    fun `login test with wrong email`() {
        val loginRequest = LoginRequest("email@email", password)
        val loginResult =
            restTemplate.postForEntity("/api/auth/login", loginRequest, ProblemDetail::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, loginResult.statusCode, "Wrong Status Code")
        assertEquals("Invalid email or password", loginResult.body?.detail, "Wrong result message")
    }

    @Test
    fun `refresh token`() {
        val loginResult =
            restTemplate.postForEntity("/api/auth/login", LoginRequest(email, password), AuthResponse::class.java)
        assertNotNull(loginResult.body?.refreshToken, "Refresh token is empty")
        assertNotNull(loginResult.body?.accessToken, "Access token is empty")

        val refreshResult = restTemplate.postForEntity(
            "/api/auth/refresh",
            RefreshRequest(loginResult.body!!.refreshToken),
            AuthResponse::class.java
        )
        assertEquals(HttpStatus.OK, refreshResult.statusCode)
        assertNotNull(refreshResult.body?.refreshToken, "No refresh token")
        assertNotNull(refreshResult.body?.accessToken, "No access token")
        assertNotEquals(loginResult.body!!.refreshToken, refreshResult.body!!.refreshToken)
    }

    @Test
    fun `invalid refresh token`() {
        val refreshResult = restTemplate.postForEntity(
            "/api/auth/refresh",
            RefreshRequest("invalid refresh token"),
            ProblemDetail::class.java
        )
        assertEquals(HttpStatus.BAD_REQUEST, refreshResult.statusCode)
        assertEquals("Invalid refresh token", refreshResult.body?.detail)
    }

}