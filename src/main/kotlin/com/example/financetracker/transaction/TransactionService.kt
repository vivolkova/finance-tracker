package com.example.financetracker.transaction

import com.example.financetracker.category.CategoryRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
    private val logger = LoggerFactory.getLogger(TransactionService::class.java)

    @Transactional(readOnly = true)
    fun getAll(): List<TransactionDto> =
        transactionRepository.findAll().map { it.toDto() }

    @Transactional(readOnly = true)
    fun getById(id: Long): TransactionDto =
        transactionRepository.findById(id)
            .orElseThrow { NoSuchElementException("Transaction not found with id: $id") }
            .toDto()

    @Transactional
    fun create(command: CreateTransactionCommand): TransactionDto {
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
        ).toDto()
    }

    @Transactional
    fun delete(id: Long) {
        logger.info("Deleting transaction with id: $id")
        if (!transactionRepository.existsById(id))
            throw NoSuchElementException("Transaction not found with id: $id")
        transactionRepository.deleteById(id)
        logger.info("Transaction deleted successfully: $id")
    }

    @Transactional(readOnly = true)
    fun getMonthlySummary(year: Int, month: Int): MonthlySummary {
        val date = LocalDate.of(year, month, 1)
        val transactions = transactionRepository.findAllByMonth(date)

        val totalIncome = transactions
            .filter { it.type == TransactionType.INCOME }
            .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }

        val totalExpense = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }

        val byCategory = transactions
            .groupBy { it.category }
            .map { (category, txs) ->
                CategorySummary(
                    categoryName = category.name,
                    type = category.type,
                    total = txs.fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }
                )
            }
            .sortedByDescending { it.total }

        return MonthlySummary(
            month = "$year-${month.toString().padStart(2, '0')}",
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            balance = totalIncome - totalExpense,
            byCategory = byCategory
        )
    }
}