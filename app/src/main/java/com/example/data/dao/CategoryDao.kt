package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY type ASC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCategories(categories: List<CategoryEntity>)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("UPDATE categories SET name = :newName WHERE name = :oldName AND type = :type")
    suspend fun updateCategoryGroup(oldName: String, newName: String, type: String)

    @Query("DELETE FROM categories WHERE name = :name AND type = :type")
    suspend fun deleteCategoryGroup(name: String, type: String)

    @Query("UPDATE categories SET subCategory = :newSubCategory WHERE id = :id")
    suspend fun updateSubcategory(id: String, newSubCategory: String)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteSubcategory(id: String)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)

    @Query("SELECT * FROM categories WHERE name = :name AND type = :type")
    suspend fun getCategoriesGroup(name: String, type: String): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()
}
