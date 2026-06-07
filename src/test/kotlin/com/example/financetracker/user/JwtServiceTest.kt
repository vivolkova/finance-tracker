package com.example.financetracker.user

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.test.util.ReflectionTestUtils

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
}
