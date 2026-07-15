package com.example.financetracker.transaction

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class TransactionV2Dto(
    val id: Long,
    val amount: BigDecimal,
    val description: String?,
    val date: LocalDate,
    val categoryId: Long,
    val categoryName: String,
    val userEmail: String?,
    val createdAt: LocalDateTime,
    val version: Long
)

data class UpdateTransactionV2Request(
    val amount: BigDecimal? = null,
    val description: String? = null,
    val date: LocalDate? = null,
    val categoryId: Long? = null,
    val version: Long
)

fun TransactionDto.toV2() = TransactionV2Dto(
    id = id,
    amount = amount,
    description = description,
    date = date,
    categoryId = categoryId,
    categoryName = categoryName,
    userEmail = userEmail,
    createdAt = createdAt,
    version = version
)