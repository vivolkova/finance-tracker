package com.example.financetracker

import com.example.financetracker.category.Category
import com.example.financetracker.category.CategoryRepository
import com.example.financetracker.category.CategoryType
import com.example.financetracker.transaction.Transaction
import com.example.financetracker.transaction.TransactionRepository
import com.example.financetracker.transaction.TransactionType
import com.example.financetracker.user.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test

class RepositoryTest : IntegrationTestBase() {

    @Autowired
    lateinit var transactionRepository: TransactionRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var categoryRepository: CategoryRepository

    @Test
    fun `sum of expenses in category and month`() {
        val user = userRepository.findByEmail(email).orElseThrow()
        val food = categoryRepository.save(Category(name = "Food", type = CategoryType.EXPENSE))
        val transport = categoryRepository.save(Category(name = "Transport", type = CategoryType.EXPENSE))

        transactionRepository.save(
            Transaction(
                amount = BigDecimal("1000.00"),
                date = LocalDate.of(2027, 1, 31), type = TransactionType.EXPENSE, category = food, user = user
            )
        )
        transactionRepository.save(
            Transaction(
                amount = BigDecimal("3000.00"),
                date = LocalDate.of(2027, 1, 30), type = TransactionType.EXPENSE, category = food, user = user
            )
        )
        transactionRepository.save(
            Transaction(
                amount = BigDecimal("5000.00"),
                date = LocalDate.of(2027, 1, 30), type = TransactionType.EXPENSE, category = transport, user = user
            )
        )
        transactionRepository.save(
            Transaction(
                amount = BigDecimal("4000.00"),
                date = LocalDate.of(2026, 12, 10), type = TransactionType.EXPENSE, category = food, user = user
            )
        )

        val sum = transactionRepository.getTotalAmount(
            LocalDate.of(2027, 1, 1),
            LocalDate.of(2027, 1, 31),
            user.id,
            food.id
        )

        assertEquals(0, sum.compareTo(BigDecimal("4000.0")))
    }
}