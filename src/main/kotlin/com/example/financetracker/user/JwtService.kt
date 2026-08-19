package com.example.financetracker.user

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration}") private val expiration: Long
) {

    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    /* Token:
     * Header(algorithm HS256).
     * PayLoad(user info, for example: email, creation and exp date).
     * Signature based on header, payload and secret key*/
    fun generateToken(email: String): String {
        return Jwts.builder()
            .subject(email)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expiration))
            .signWith(key)
            .compact()
    }

    fun extractEmail(token: String): String {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
            .subject
    }

    // Проверяет токен и возвращает РЕЗУЛЬТАТ с причиной (sealed TokenValidation),
    // а не просто true/false — вызывающий код знает, токен истёк или невалиден.
    fun validate(token: String): TokenValidation = try {
        val email = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
            .subject
        TokenValidation.Valid(email)
    } catch (e: ExpiredJwtException) {
        TokenValidation.Expired
    } catch (e: Exception) {
        TokenValidation.Invalid
    }

    fun isTokenValid(token: String): Boolean = validate(token) is TokenValidation.Valid
}

// sealed class — закрытая иерархия: результат может быть ТОЛЬКО одним из этих типов.
// В отличие от enum, каждый вариант может нести свои данные (Valid хранит email).
sealed class TokenValidation {
    data class Valid(val email: String) : TokenValidation()
    object Expired : TokenValidation()
    object Invalid : TokenValidation()
}