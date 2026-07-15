package com.example.financetracker.transaction

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate

@RestController
@RequestMapping(value = ["/api/transactions", "/api/v1/transactions"])
@Tag(
    name = "Transactions v1 (deprecated)",
    description = "Use /api/v2/transactions. Transactions v1 will be removed in v2.0"
)
@Deprecated("Use /api/v2/transactions instead. Will be removed in v2.0")
class TransactionController(
    private val transactionService: TransactionService
) {

    @GetMapping
    fun getAll(): List<TransactionDto> = transactionService.getAll()

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): TransactionDto = transactionService.getById(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateTransactionRequest): TransactionDto =
        transactionService.create(
            CreateTransactionCommand(
                amount = request.amount,
                description = request.description,
                date = request.date,
                categoryId = request.categoryId
            )
        )

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) =
        transactionService.delete(id)

    @GetMapping("/summary")
    fun getMonthlySummary(
        @RequestParam year: Int,
        @RequestParam month: Int
    ): MonthlySummary =
        transactionService.getMonthlySummary(year, month)

    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateTransactionRequest
    ): TransactionDto =
        transactionService.update(
            id,
            UpdateTransactionCommand(
                request.amount,
                request.description,
                request.date,
                request.categoryId,
                request.version
            )
        )
}

data class CreateTransactionRequest(
    @field:NotNull(message = "Amount cannot be null")
    @field:Positive(message = "Amount must be positive")
    val amount: BigDecimal,

    @field:Size(max = 255, message = "Description must be less than 255 characters")
    val description: String? = null,

    @field:NotNull(message = "Date cannot be null")
    val date: LocalDate,

    @field:NotNull(message = "Type cannot be null")
    val type: TransactionType,

    @field:NotNull(message = "Category id cannot be null")
    val categoryId: Long
)