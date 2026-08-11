package com.example.data.repository

import com.example.data.model.CategoryEntity
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    val allCategories: Flow<List<CategoryEntity>>

    suspend fun getAllCategoriesList(): List<CategoryEntity>

    suspend fun ensureDefaultCategoriesSeeded()

    suspend fun addCategory(name: String, type: String, subCategory: String)

    suspend fun updateCategory(category: CategoryEntity)

    suspend fun deleteCategory(category: CategoryEntity)

    suspend fun updateCategoryGroup(oldName: String, newName: String, type: String)

    suspend fun deleteCategoryGroup(name: String, type: String)

    suspend fun updateSubcategory(id: String, newSubCategory: String)

    suspend fun deleteSubcategory(id: String)
}
