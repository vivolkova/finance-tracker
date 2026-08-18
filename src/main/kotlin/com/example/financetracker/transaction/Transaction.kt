package com.example.financetracker.transaction

import com.example.financetracker.category.Category
import com.example.financetracker.common.BaseEntity
import com.example.financetracker.user.User
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "transactions")
class Transaction(
    @Column(nullable = false)
    val amount: BigDecimal,

    @Column
    val description: String? = null,

    @Column(nullable = false)
    val date: LocalDate,

    @Deprecated("Use category.type instead. Will be removed in v2.0")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: TransactionType,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    val category: Category,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Version
    var version: Long = 0,

    id: Long = 0
): BaseEntity(id) {
    override fun toString(): String =
        "Transaction(id=$id, amount=$amount, date=$date)"
}

enum class TransactionType {
    INCOME,
    EXPENSE
}