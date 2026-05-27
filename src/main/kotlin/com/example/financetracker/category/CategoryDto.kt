package com.example.financetracker.category

data class CategoryDto(
    val id: Long,
    val name: String,
    val type: CategoryType
)

fun Category.toDto() = CategoryDto(
    id = id,
    name = name,
    type = type
)