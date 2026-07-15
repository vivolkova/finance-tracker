package com.example.financetracker.user

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetails
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

// object — синглтон одной строкой: один экземпляр на всё приложение.
// Общие константы аутентификации в одном месте вместо «магических строк».
object AuthHeaders {
    const val HEADER = "Authorization"
    const val PREFIX = "Bearer "
}

@Component
class JwtAuthFilter(
    private val jwtService: JwtService,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader(AuthHeaders.HEADER)

        if (authHeader == null || !authHeader.startsWith(AuthHeaders.PREFIX)) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.substring(AuthHeaders.PREFIX.length)

        // sealed + when: обрабатываем все варианты результата (компилятор проверяет полноту).
        // Valid — берём email через smart cast; истёк/невалиден — пропускаем без аутентификации.
        val email = when (val result = jwtService.validate(token)) {
            is TokenValidation.Valid -> result.email
            TokenValidation.Expired, TokenValidation.Invalid -> {
                filterChain.doFilter(request, response)
                return
            }
        }

        val user = userRepository.findByEmail(email).orElse(null) ?: run {
            filterChain.doFilter(request, response)
            return
        }

        val authentication = UsernamePasswordAuthenticationToken(
            user, null, emptyList()
        )
        authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
        logger.debug("Authenticated user: $email, IP: ${(authentication.details as WebAuthenticationDetails).remoteAddress}")
        SecurityContextHolder.getContext().authentication = authentication

        filterChain.doFilter(request, response)
    }
}