package com.example.financetracker.budget

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/api/budgets")
@Tag(name = "Budgets", description = "Monthly spending limits by category")
class BudgetController(
    private val budgetService: BudgetService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: BudgetRequest): BudgetDto =
        budgetService.create(BudgetCommand(request.limitAmount, request.period, request.categoryId))

}

data class BudgetRequest(
    val limitAmount: BigDecimal,
    val categoryId: Long,
    @field:Size(min = 7, max = 7, message = "Period must be at 7 characters")
    val period: String

)