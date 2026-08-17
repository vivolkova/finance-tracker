package com.example.financetracker.budget

import com.example.financetracker.user.User
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Pattern
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
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
    @Operation(summary = "Set limit")
    fun create(@AuthenticationPrincipal user: User, @Valid @RequestBody request: BudgetRequest): BudgetDto =
        budgetService.create(user, BudgetCommand(request.limitAmount, request.period, request.categoryId))

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get limit by User")
    fun get(
        @AuthenticationPrincipal user: User,
        @RequestParam(required = false) period: String?
    ): List<BudgetDto> {
        require(period == null || Regex("\\d{4}-(0[1-9]|1[0-2])").matches(period)){ "Period must be YYYY-MM" }
        return budgetService.get(user.id, period)
    }
}

data class BudgetRequest(
    @field:DecimalMin("0.01", "Limit amount must be positive")
    @field:Digits(integer = 17, fraction = 2, message = "LimitAmount scale must be less or equal 2")
    val limitAmount: BigDecimal,
    val categoryId: Long,
    @field:Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "Period must be YYYY-MM")
    val period: String

)