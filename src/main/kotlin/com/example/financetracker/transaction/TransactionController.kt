package com.example.financetracker.transaction

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.jetbrains.annotations.NotNull
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Transactions")
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
                type = request.type,
                categoryId = request.categoryId
            )
        )

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) =
        transactionService.delete(id)
}

data class CreateTransactionRequest(
    @field:NotNull(value = "Amount cannot be null")
    @field:Positive(message = "Amount must be positive")
    val amount: BigDecimal,

    @field:Size(max = 255, message = "Description must be less than 255 characters")
    val description: String? = null,

    @field:NotNull(value = "Date cannot be null")
    val date: LocalDate,

    @field:NotNull(value = "Type cannot be null")
    val type: TransactionType,

    @field:NotNull(value = "Category id cannot be null")
    val categoryId: Long
)