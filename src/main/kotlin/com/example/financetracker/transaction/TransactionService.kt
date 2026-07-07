package com.example.financetracker.transaction

import com.example.financetracker.category.CategoryRepository
import com.example.financetracker.user.User
import jakarta.persistence.OptimisticLockException
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.core.context.SecurityContextHolder
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

data class UpdateTransactionCommand(
    val amount: BigDecimal? = null,
    val description: String? = null,
    val date: LocalDate? = null,
    val type: TransactionType? = null,
    val categoryId: Long? = null,
    val version: Long
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

    @CacheEvict(value = ["monthlySummary"], allEntries = true)
    @Transactional
    fun create(command: CreateTransactionCommand): TransactionDto {
        val category = categoryRepository.findById(command.categoryId)
            .orElseThrow { NoSuchElementException("Category not found with id: ${command.categoryId}") }

        val currentUser = SecurityContextHolder.getContext().authentication?.principal as? User

        return transactionRepository.save(
            Transaction(
                amount = command.amount,
                description = command.description,
                date = command.date,
                type = command.type,
                category = category,
                user = currentUser
            )
        ).toDto()
    }

    @CacheEvict(value = ["monthlySummary"], allEntries = true)
    @Transactional
    fun delete(id: Long) {
        logger.info("Deleting transaction with id: $id")
        if (!transactionRepository.existsById(id))
            throw NoSuchElementException("Transaction not found with id: $id")
        transactionRepository.deleteById(id)
        logger.info("Transaction deleted successfully: $id")
    }

    @Cacheable(value = ["monthlySummary"], key = "#year + '-' + #month")
    @Transactional(readOnly = true)
    fun getMonthlySummary(year: Int, month: Int): MonthlySummary {
        logger.info("SUMMARY MISS: computing $year-$month")
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

    @CacheEvict(value = ["monthlySummary"], allEntries = true)
    @Transactional
    fun update(id: Long, command: UpdateTransactionCommand): TransactionDto {
        val transaction = transactionRepository.findById(id)
            .orElseThrow { NoSuchElementException("Transaction not found with id: $id") }

        if (transaction.version != command.version) {
            throw OptimisticLockException(
                "Transaction was modified by another user. Please refresh and try again."
            )
        }

        val category = if (command.categoryId != null) {
            categoryRepository.findById(command.categoryId)
                .orElseThrow { NoSuchElementException("Category not found with id: ${command.categoryId}") }
        } else {
            transaction.category
        }

        val updated = Transaction(
            id = transaction.id,
            amount = command.amount ?: transaction.amount,
            description = command.description ?: transaction.description,
            date = command.date ?: transaction.date,
            type = command.type ?: transaction.type,
            category = category,
            user = transaction.user,
            createdAt = transaction.createdAt,
            version = transaction.version
        )

        // saveAndFlush forces the SQL UPDATE immediately so Hibernate can write
        // back the incremented version to the entity before toDto() is called.
        return transactionRepository.saveAndFlush(updated).toDto()
    }
}