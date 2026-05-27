package com.example.financetracker.category

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/categories")
class CategoryController(
    private val categoryService: CategoryService
) {

    @GetMapping
    fun getAll(): List<Category> =
        categoryService.getAll()

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): Category =
        categoryService.getById(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateCategoryRequest): Category =
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