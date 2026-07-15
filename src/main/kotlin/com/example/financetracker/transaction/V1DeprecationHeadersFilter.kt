package com.example.financetracker.transaction

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class V1DeprecationHeadersFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        req: HttpServletRequest,
        res: HttpServletResponse,
        chain: FilterChain
    ) {
        val p = req.requestURI
        if (p.startsWith("/api/v1/transactions") || p.startsWith("/api/transactions")) {
            res.setHeader("Deprecation", "true")
            res.setHeader("Sunset", "Wed, 16 Jul 2027 00:00:00 GMT")
            res.setHeader("Link", "</api/v2/transactions>; rel=\"successor-version\"")
        }
        chain.doFilter(req, res)
    }
}