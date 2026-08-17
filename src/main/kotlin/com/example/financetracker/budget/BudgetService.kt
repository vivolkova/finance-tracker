package com.example.financetracker.budget

import com.example.financetracker.category.CategoryRepository
import com.example.financetracker.user.User
import org.hibernate.exception.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
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
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun create(user: User, budgetCommand: BudgetCommand): BudgetDto {
        val category = categoryRepository.findById(budgetCommand.categoryId)
            .orElseThrow { NoSuchElementException("Category not found with id: ${budgetCommand.categoryId}") }

        if (budgetRepository.existsByUserIdAndCategoryIdAndPeriod(
                user.id, budgetCommand.categoryId, budgetCommand.period)) {
            logger.debug(
                "Duplicate budget rejected by pre-check: userId={}, categoryId={}, period={}",
                user.id, budgetCommand.categoryId, budgetCommand.period
            )
            throw DuplicateBudgetException("Budget already exists for this category and period")
        }

        return try {
            budgetRepository.save(
                Budget(
                    category = category,
                    user = user,
                    limitAmount = budgetCommand.limit.setScale(2, RoundingMode.HALF_UP),
                    period = budgetCommand.period
                )
            ).toDto()
        } catch (ex: DataIntegrityViolationException) {
            val constraint = (ex.cause as? ConstraintViolationException)?.constraintName
            if (constraint == "uq_budget_user_category_period") {
                logger.warn("Duplicate budget on insert during race: constraint={}, userId={}, categoryId={}, period={}",
                    constraint, user.id, budgetCommand.categoryId, budgetCommand.period)
                throw DuplicateBudgetException("Budget already exists for this category and period", ex)
            }

            logger.error("Data integrity violation: constraint={}, userId={}, categoryId={}, period={}",
                constraint, user.id, budgetCommand.categoryId, budgetCommand.period, ex)
            throw ex
        }
    }
}