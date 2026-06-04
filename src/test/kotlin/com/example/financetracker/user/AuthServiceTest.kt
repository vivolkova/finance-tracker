package com.example.financetracker.user

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional

class AuthServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val jwtService = mockk<JwtService>()
    private val refreshTokenService = mockk<RefreshTokenService>()
    private val authService = AuthService(
        userRepository,
        passwordEncoder,
        jwtService,
        refreshTokenService
    )

    private val testUser = User(
        id = 1L,
        email = "test@mail.com",
        password = "\$2a\$10\$hashedpassword"
    )

    private val testRefreshToken = RefreshToken(
        id = 1L,
        token = "refresh-token-uuid",
        userId = 1L,
        expiresAt = java.time.LocalDateTime.now().plusDays(30)
    )

    @Test
    fun `register should create user and return tokens`() {
        every { userRepository.existsByEmail("test@mail.com") } returns false
        every { passwordEncoder.encode("password123") } returns "\$2a\$10\$hashedpassword"
        every { userRepository.save(any()) } returns testUser
        every { jwtService.generateToken("test@mail.com") } returns "access-token"
        every { refreshTokenService.createRefreshToken(1L) } returns testRefreshToken

        val result = authService.register(
            RegisterRequest(email = "test@mail.com", password = "password123")
        )

        assert(result.accessToken == "access-token")
        assert(result.refreshToken == "refresh-token-uuid")
        verify(exactly = 1) { userRepository.save(any()) }
    }

    @Test
    fun `register should throw when email already exists`() {
        every { userRepository.existsByEmail("test@mail.com") } returns true

        assertThrows<IllegalArgumentException> {
            authService.register(
                RegisterRequest(email = "test@mail.com", password = "password123")
            )
        }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `login should return tokens when credentials are valid`() {
        every { userRepository.findByEmail("test@mail.com") } returns Optional.of(testUser)
        every { passwordEncoder.matches("password123", testUser.password) } returns true
        every { jwtService.generateToken("test@mail.com") } returns "access-token"
        every { refreshTokenService.createRefreshToken(1L) } returns testRefreshToken

        val result = authService.login(
            LoginRequest(email = "test@mail.com", password = "password123")
        )

        assert(result.accessToken == "access-token")
        assert(result.refreshToken == "refresh-token-uuid")
    }

    @Test
    fun `login should throw when user not found`() {
        every { userRepository.findByEmail("unknown@mail.com") } returns Optional.empty()

        assertThrows<IllegalArgumentException> {
            authService.login(
                LoginRequest(email = "unknown@mail.com", password = "password123")
            )
        }
    }

    @Test
    fun `login should throw when password is wrong`() {
        every { userRepository.findByEmail("test@mail.com") } returns Optional.of(testUser)
        every { passwordEncoder.matches("wrongpassword", testUser.password) } returns false

        assertThrows<IllegalArgumentException> {
            authService.login(
                LoginRequest(email = "test@mail.com", password = "wrongpassword")
            )
        }
        verify(exactly = 0) { jwtService.generateToken(any()) }
    }

    @Test
    fun `refresh should return new tokens when refresh token is valid`() {
        every { refreshTokenService.verifyRefreshToken("refresh-token-uuid") } returns testRefreshToken
        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { jwtService.generateToken("test@mail.com") } returns "new-access-token"
        every { refreshTokenService.createRefreshToken(1L) } returns testRefreshToken

        val result = authService.refresh(RefreshRequest("refresh-token-uuid"))

        assert(result.accessToken == "new-access-token")
        verify(exactly = 1) { refreshTokenService.verifyRefreshToken("refresh-token-uuid") }
    }

    @Test
    fun `refresh should throw when refresh token is invalid`() {
        every { refreshTokenService.verifyRefreshToken("invalid-token") } throws
                IllegalArgumentException("Invalid refresh token")

        assertThrows<IllegalArgumentException> {
            authService.refresh(RefreshRequest("invalid-token"))
        }
        verify(exactly = 0) { userRepository.findById(any()) }
    }
}