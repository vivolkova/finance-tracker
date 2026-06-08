package com.example.financetracker.user

data class UserDto (
    val id: Long = 0,
    val email: String
)

fun User.toDto(): UserDto {
    return UserDto(this.id, this.email)
}