package com.example.financetracker.user

import com.example.financetracker.IntegrationTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthIntegrationTest : IntegrationTestBase() {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    val email = "test@mail.ru"
    val password = "1234"


    @BeforeAll
    fun `register test`() {
        val user = RegisterRequest(email, password)
        val registerResult = restTemplate.postForEntity("/api/auth/register", user, AuthResponse::class.java)
        assertEquals(HttpStatus.CREATED, registerResult.statusCode, "Wrong Status Code")
        assertNotNull(registerResult.body?.accessToken, "Wrong Access token")
        assertNotNull(registerResult.body?.refreshToken, "Wrong Refresh Token")
    }

    @Test
    fun `login test`() {
        val loginRequest = LoginRequest(email, password)
        val loginResult = restTemplate.postForEntity("/api/auth/login", loginRequest, AuthResponse::class.java)
        assertNotNull(loginResult.body?.accessToken, "Wrong Access token")
        assertNotNull(loginResult.body?.refreshToken, "Wrong Refresh Token")
    }

    @Test
    fun `wrong email, login test`() {
        val loginRequest = LoginRequest("email@email", password)
        val loginResult =
            restTemplate.postForEntity("/api/auth/login", loginRequest, ProblemDetail::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, loginResult.statusCode, "Wrong Status Code")
        assertEquals("Invalid email or password", loginResult.body?.detail)
    }
}