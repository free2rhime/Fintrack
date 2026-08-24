package com.example.data.repository

import com.example.data.model.CategoryEntity
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    val allCategories: Flow<List<CategoryEntity>>
        get() = getCategories(null)

    fun getCategories(householdId: String? = null): Flow<List<CategoryEntity>>

    suspend fun getAllCategoriesList(): List<CategoryEntity>

    suspend fun ensureDefaultCategoriesSeeded(householdId: String? = null)

    suspend fun addCategory(name: String, type: String, subCategory: String, userId: String = "local_user", householdId: String? = null)

    suspend fun updateCategory(category: CategoryEntity)

    suspend fun deleteCategory(category: CategoryEntity)

    suspend fun updateCategoryGroup(oldName: String, newName: String, type: String, householdId: String? = null)

    suspend fun deleteCategoryGroup(name: String, type: String, householdId: String? = null)

    suspend fun updateSubcategory(id: String, newSubCategory: String)

    suspend fun deleteSubcategory(id: String)
}
