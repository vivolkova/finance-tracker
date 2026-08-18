package com.example.financetracker.category

import com.example.financetracker.common.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "categories")
class Category(
    @Column(nullable = false)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: CategoryType,
    id: Long = 0
): BaseEntity(id) {

    override fun toString(): String = "Category(id=$id, name=$name, type=$type)"
}

enum class CategoryType {
    INCOME,
    EXPENSE
}