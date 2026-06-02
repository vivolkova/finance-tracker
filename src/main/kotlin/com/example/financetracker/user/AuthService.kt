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

data class AuthResponse(
    val accessToken: String
)

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {

    fun register(request: RegisterRequest): AuthResponse {
        require(!userRepository.existsByEmail(request.email)) { "User with email ${request.email} already exists" }
        val user = User(
            email = request.email,
            password = passwordEncoder.encode(request.password)
                ?: throw IllegalArgumentException("Password encoding failed")
        )
        userRepository.save(user)
        return AuthResponse(jwtService.generateToken(user.email))
    }

    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { IllegalArgumentException("Invalid email or password") }

        require(!passwordEncoder.matches(request.password, user.password)) {
            "Invalid email or password"
        }
        return AuthResponse(jwtService.generateToken(user.email))
    }
}