package com.example.financetracker.user

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository
) {

    @Value("\${jwt.refresh-expiration}")
    private var refreshExpiration: Long = 0

    @Transactional
    fun createRefreshToken(userId: Long): RefreshToken {
        refreshTokenRepository.deleteByUserId(userId)

        val refreshToken = RefreshToken(
            token = UUID.randomUUID().toString(),
            userId = userId,
            expiresAt = LocalDateTime.now().plusNanos(refreshExpiration * 1_000_000)
        )
        return refreshTokenRepository.save(refreshToken)
    }

    @Transactional(readOnly = true)
    fun verifyRefreshToken(token: String): RefreshToken {
        val refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow { IllegalArgumentException("Invalid refresh token") }

        if (refreshToken.expiresAt.isBefore(LocalDateTime.now())) {
            throw IllegalArgumentException("Refresh token expired")
        }
        return refreshToken
    }
}