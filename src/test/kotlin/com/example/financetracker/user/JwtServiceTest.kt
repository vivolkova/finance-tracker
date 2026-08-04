package com.example.financetracker.user

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.assertEquals
import kotlin.test.assertIs

class JwtServiceTest {

    private val jwtService = JwtService("test-secret-key-that-is-long-enough-256bits!", 3_600_000L)

    @Test
    fun `generateToken then extractEmail returns the same email`() {
        val token = jwtService.generateToken("user@example.com")
        val email = jwtService.extractEmail(token)
        assert(email == "user@example.com")
    }

    @Test
    fun `generateToken produces a non-blank three-part JWT`() {
        val token = jwtService.generateToken("user@example.com")
        assert(token.isNotBlank())
        assert(token.split(".").size == 3)
    }

    @Test
    fun `isTokenValid returns true for a freshly generated token`() {
        val token = jwtService.generateToken("user@example.com")
        assert(jwtService.isTokenValid(token))
    }

    @Test
    fun `isTokenValid returns false for a malformed token`() {
        assert(!jwtService.isTokenValid("not-a-jwt"))
    }

    @Test
    fun `isTokenValid returns false for a token signed with another secret`() {
        val otherService = JwtService("another-secret-key-that-is-also-256-bits-long!", 3_600_000L)
        val foreignToken = otherService.generateToken("user@example.com")
        assert(!jwtService.isTokenValid(foreignToken))
    }

    @Test
    fun `isTokenValid returns false for an expired token`() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1_000L)
        val expiredToken = jwtService.generateToken("user@example.com")
        assert(!jwtService.isTokenValid(expiredToken))
    }

    @Test
    fun `extractEmail throws for a token signed with another secret`() {
        val otherService = JwtService("another-secret-key-that-is-also-256-bits-long!", 3_600_000L)
        val foreignToken = otherService.generateToken("user@example.com")

        assertThrows<Exception> {
            jwtService.extractEmail(foreignToken)
        }
    }

    // ── sealed-результат validate(): три ветки вместо true/false ──────────────
    // validate() НЕ бросает исключение, а возвращает типизированную причину.
    // Вызывающий (JwtAuthFilter) обязан разобрать все случаи через when.

    @Test
    fun `validate returns Valid with email for a fresh token`() {
        val token = jwtService.generateToken("user@example.com")

        val result = jwtService.validate(token)

        assertIs<TokenValidation.Valid>(result)          // smart-cast к Valid ниже
        assertEquals("user@example.com", result.email)
    }

    @Test
    fun `validate returns Invalid for a malformed token`() {
        val result = jwtService.validate("not-a-jwt")

        assertIs<TokenValidation.Invalid>(result)
    }

    @Test
    fun `validate returns Invalid for a token signed with another secret`() {
        val otherService = JwtService("another-secret-key-that-is-also-256-bits-long!", 3_600_000L)
        val foreignToken = otherService.generateToken("user@example.com")

        val result = jwtService.validate(foreignToken)   // подпись не сходится

        assertIs<TokenValidation.Invalid>(result)
    }

    @Test
    fun `validate returns Expired for an already expired token`() {
        // тот же секрет, но время жизни в ПРОШЛОМ -> подпись валидна, но exp истёк
        val expiredService = JwtService("test-secret-key-that-is-long-enough-256bits!", -1_000L)
        val token = expiredService.generateToken("user@example.com")

        val result = jwtService.validate(token)

        assertIs<TokenValidation.Expired>(result)
    }
}
