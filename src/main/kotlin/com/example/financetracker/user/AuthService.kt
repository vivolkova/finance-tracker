package com.example.financetracker.user

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

data class RegisterRequest(
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RefreshRequest(
    val refreshToken: String
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String
)

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val refreshTokenService: RefreshTokenService
) {

    fun register(request: RegisterRequest): AuthResponse {
        require(!userRepository.existsByEmail(request.email)) {
            "User with email ${request.email} already exists" }
        val user = userRepository.save(
            User(
                email = request.email,
                password = passwordEncoder.encode(request.password)
                    ?: throw IllegalArgumentException("Password encoding failed")
            )
        )
        return buildAuthResponse(user)
    }

    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { throw IllegalArgumentException("Invalid email or password") }

        require(passwordEncoder.matches(request.password, user.password)) {
            "Invalid email or password"
        }
        return buildAuthResponse(user)
    }

    fun refresh(request: RefreshRequest): AuthResponse {
        val refreshToken = refreshTokenService.verifyRefreshToken(request.refreshToken)
        val user = userRepository.findById(refreshToken.userId)
            .orElseThrow { IllegalArgumentException("User not found") }
        return buildAuthResponse(user)
    }

    private fun buildAuthResponse(user: User): AuthResponse {
        val accessToken = jwtService.generateToken(user.email)
        val refreshToken = refreshTokenService.createRefreshToken(user.id)
        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken.token
        )
    }
}