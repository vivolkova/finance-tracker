package com.example.financetracker.budget

import com.example.financetracker.category.CategoryRepository
import com.example.financetracker.user.User
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

data class BudgetCommand(
    val limit: BigDecimal,
    val period: String,
    val categoryId: Long
)

@Service
class BudgetService(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository
) {

    @Transactional
    fun create(budgetCommand: BudgetCommand): BudgetDto {
        val category = categoryRepository.findById(budgetCommand.categoryId)
            .orElseThrow { NoSuchElementException("Category not found with id: ${budgetCommand.categoryId}") }

        val currentUser = SecurityContextHolder.getContext().authentication?.principal as? User
            ?: throw NoSuchElementException("User is not defined")

        if (budgetRepository.existsByUserIdAndCategoryIdAndPeriod(
                currentUser.id,
                budgetCommand.categoryId,
                budgetCommand.period
            )
        )
            throw DuplicateBudgetException("Budget already exists for this category and period")


        return try {
            budgetRepository.save(
                Budget(
                    category = category,
                    user = currentUser,
                    limitAmount = budgetCommand.limit.setScale(2, RoundingMode.HALF_UP),
                    period = budgetCommand.period
                )
            ).toDto()
        } catch (ex: DataIntegrityViolationException) {
            throw DuplicateBudgetException("Budget already exists for this category and period")
        }
    }
}