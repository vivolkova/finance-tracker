package com.example.financetracker.user

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.Optional

class RefreshTokenServiceTest {

    private val refreshTokenRepository = mockk<RefreshTokenRepository>()
    private val refreshTokenService = RefreshTokenService(refreshTokenRepository)

    private val validToken = RefreshToken(
        id = 1L,
        token = "valid-uuid-token",
        userId = 1L,
        expiresAt = LocalDateTime.now().plusDays(30)
    )

    private val expiredToken = RefreshToken(
        id = 2L,
        token = "expired-uuid-token",
        userId = 1L,
        expiresAt = LocalDateTime.now().minusDays(1)
    )

    @Test
    fun `createRefreshToken should delete old tokens and create new one`() {
        every { refreshTokenRepository.deleteByUserId(1L) } returns Unit
        every { refreshTokenRepository.save(any()) } returns validToken

        val result = refreshTokenService.createRefreshToken(1L)

        assert(result.userId == 1L)
        verify(exactly = 1) { refreshTokenRepository.deleteByUserId(1L) }
        verify(exactly = 1) { refreshTokenRepository.save(any()) }
    }

    @Test
    fun `verifyRefreshToken should return token when valid`() {
        every { refreshTokenRepository.findByToken("valid-uuid-token") } returns
                Optional.of(validToken)

        val result = refreshTokenService.verifyRefreshToken("valid-uuid-token")

        assert(result.token == "valid-uuid-token")
        assert(result.userId == 1L)
    }

    @Test
    fun `verifyRefreshToken should throw when token not found`() {
        every { refreshTokenRepository.findByToken("unknown-token") } returns
                Optional.empty()

        assertThrows<IllegalArgumentException> {
            refreshTokenService.verifyRefreshToken("unknown-token")
        }
    }

    @Test
    fun `verifyRefreshToken should throw when token expired`() {
        every { refreshTokenRepository.findByToken("expired-uuid-token") } returns
                Optional.of(expiredToken)

        assertThrows<IllegalArgumentException> {
            refreshTokenService.verifyRefreshToken("expired-uuid-token")
        }
    }
}