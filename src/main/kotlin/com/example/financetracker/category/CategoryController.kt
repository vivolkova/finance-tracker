package com.example.financetracker.category

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "Category control")
class CategoryController(
    private val categoryService: CategoryService
) {

    @GetMapping
    @Operation(summary = "Get all transactions")
    fun getAll(): List<CategoryDto> = categoryService.getAll()

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by Id")
    @ApiResponse(responseCode = "200", description = "Category found")
    @ApiResponse(responseCode = "404", description = "Category not found")

    fun getById(@PathVariable id: Long): CategoryDto = categoryService.getById(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateCategoryRequest): CategoryDto =
        categoryService.create(request.name, request.type)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) =
        categoryService.delete(id)
}

data class CreateCategoryRequest(
    val name: String,
    val type: CategoryType
)