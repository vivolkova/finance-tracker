package com.example.financetracker.transaction

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class TransactionDto(
    val id: Long,
    val amount: BigDecimal,
    val description: String?,
    val date: LocalDate,
    val type: TransactionType,
    val categoryId: Long,
    val categoryName: String,
    val userEmail: String?,
    val createdAt: LocalDateTime
)

fun Transaction.toDto() = TransactionDto(
    id = id,
    amount = amount,
    description = description,
    date = date,
    type = type,
    categoryId = category.id,
    categoryName = category.name,
    userEmail = user?.email,
    createdAt = createdAt
)