package com.example.financetracker.category

import org.springframework.stereotype.Service


@Service
class CategoryService(
    private val categoryRepository: CategoryRepository
) {

    fun getAll(): List<Category> =
        categoryRepository.findAll()

    fun getById(id: Long): Category =
        categoryRepository.findById(id)
            .orElseThrow { NoSuchElementException("Category not found with id: $id") }

    fun create(name: String, type: CategoryType): Category {
        val category = Category(name = name, type = type)
        return categoryRepository.save(category)
    }

    fun delete(id: Long) {
        if (!categoryRepository.existsById(id)) {
            throw NoSuchElementException("Category not found with id: $id")
        }
        categoryRepository.deleteById(id)
    }
}