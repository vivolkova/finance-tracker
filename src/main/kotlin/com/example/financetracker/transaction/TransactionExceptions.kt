package com.example.financetracker.transaction

class LimitExceeded(message: String, cause: Throwable? = null): RuntimeException(message, cause) {
}