package com.example.financetracker.budget

import com.example.financetracker.category.Category
import com.example.financetracker.common.BaseEntity
import com.example.financetracker.user.User
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "budgets")
class Budget (
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    val category: Category,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "limit_amount", nullable = false, precision = 19, scale = 2)
    val limitAmount: BigDecimal,

    @Column(nullable = false, length = 7)
    val period: String,
    id: Long = 0
): BaseEntity(id) {

    override fun toString(): String =
        "Budget(id=$id, amount=$limitAmount, period=$period, type=${category.id})"
}