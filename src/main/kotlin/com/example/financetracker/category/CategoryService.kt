package com.example.financetracker.category

import org.springframework.stereotype.Service


@Service
class CategoryService(
    private val categoryRepository: CategoryRepository
) {

    fun getAll(): List<CategoryDto> =
        categoryRepository.findAll().map { it.toDto() }

    fun getById(id: Long): CategoryDto =
        categoryRepository.findById(id)
            .orElseThrow { NoSuchElementException("Category not found with id: $id") }
            .toDto()

    fun create(name: String, type: CategoryType): CategoryDto =
        categoryRepository.save(Category(name = name, type = type)).toDto()

    fun delete(id: Long) {
        if (!categoryRepository.existsById(id)) {
            throw NoSuchElementException("Category not found with id: $id")
        }
        categoryRepository.deleteById(id)
    }
}