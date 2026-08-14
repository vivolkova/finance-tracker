package com.example.financetracker.budget

import com.example.financetracker.category.CategoryRepository
import com.example.financetracker.user.User
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.math.BigDecimal

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

    fun create(budgetCommand: BudgetCommand): BudgetDto {
        val category = categoryRepository.findById(budgetCommand.categoryId)
            .orElseThrow { NoSuchElementException("Category not found with id: ${budgetCommand.categoryId}") }

        val currentUser = SecurityContextHolder.getContext().authentication?.principal as? User
            ?: throw NoSuchElementException("User is not defined")

        return budgetRepository.save(
            Budget(
                category = category,
                user = currentUser,
                limitAmount = budgetCommand.limit,
                period = budgetCommand.period
            )
        ).toDto()
    }
}