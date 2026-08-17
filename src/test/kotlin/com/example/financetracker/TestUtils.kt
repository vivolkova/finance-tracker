package com.example.financetracker

import org.springframework.http.ProblemDetail

@Suppress("UNCHECKED_CAST")
fun errorsOf(body: ProblemDetail?): List<Map<String, String?>> =
    body?.properties?.get("errors") as? List<Map<String, String?>> ?: emptyList()