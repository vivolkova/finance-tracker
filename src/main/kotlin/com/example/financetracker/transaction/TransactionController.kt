package com.example.financetracker.transaction

import io.swagger.v3.oas.annotations.tags.Tag
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
    fun create(@RequestBody request: CreateTransactionRequest): TransactionDto =
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
    val amount: BigDecimal,
    val description: String? = null,
    val date: LocalDate,
    val type: TransactionType,
    val categoryId: Long
)