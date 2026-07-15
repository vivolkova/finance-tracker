package com.example.financetracker.transaction

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate

@RestController
@RequestMapping("/api/v2/transactions")
@Tag(name = "Transactions v2", description = "Transactions (v2, без поля type)")
class TransactionV2Controller(
    private val transactionService: TransactionService
) {

    @GetMapping
    fun getAll(): List<TransactionV2Dto> =
        transactionService.getAll().map { it.toV2() }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): TransactionV2Dto =
        transactionService.getById(id).toV2()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateTransactionV2Request): TransactionV2Dto =
        transactionService.create(
            CreateTransactionCommand(
                amount = request.amount,
                description = request.description,
                date = request.date,
                categoryId = request.categoryId
            )
        ).toV2()

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
        @Valid @RequestBody request: UpdateTransactionV2Request
    ): TransactionV2Dto =
        transactionService.update(
            id,
            UpdateTransactionCommand(
                request.amount,
                request.description,
                request.date,
                request.categoryId,
                request.version
            )
        ).toV2()
}

data class CreateTransactionV2Request(
    @field:NotNull(message = "Amount cannot be null")
    @field:Positive(message = "Amount must be positive")
    val amount: BigDecimal,

    @field:Size(max = 255, message = "Description must be less than 255 characters")
    val description: String? = null,

    @field:NotNull(message = "Date cannot be null")
    val date: LocalDate,

    @field:NotNull(message = "Category id cannot be null")
    val categoryId: Long
)