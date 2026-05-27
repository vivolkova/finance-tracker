package com.example.financetracker.transaction

import com.example.financetracker.category.CategoryRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

data class CreateTransactionCommand(
    val amount: BigDecimal,
    val description: String? = null,
    val date: LocalDate,
    val type: TransactionType,
    val categoryId: Long
)

@Service
class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {

    fun getAll(): List<Transaction> =
        transactionRepository.findAll()

    fun getById(id: Long): Transaction =
        transactionRepository.findById(id)
            .orElseThrow { NoSuchElementException("Transaction not found with id: $id") }

    fun create(command: CreateTransactionCommand): Transaction {
        val category = categoryRepository.findById(command.categoryId)
            .orElseThrow { NoSuchElementException("Category not found with id: ${command.categoryId}") }

        return transactionRepository.save(
            Transaction(
                amount = command.amount,
                description = command.description,
                date = command.date,
                type = command.type,
                category = category
            )
        )
    }

    fun delete(id: Long) {
        if (!transactionRepository.existsById(id))
            throw NoSuchElementException("Transaction not found with id: $id")
        transactionRepository.deleteById(id)
    }
}