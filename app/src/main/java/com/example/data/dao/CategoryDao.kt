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
    @Query("""
        SELECT * FROM categories 
        WHERE ((:householdId IS NULL AND householdId IS NULL) OR householdId = :householdId)
        ORDER BY type ASC, name ASC
    """)
    fun getAllCategories(householdId: String? = null): Flow<List<CategoryEntity>>

    @Query("""
        SELECT * FROM categories 
        WHERE ((:householdId IS NULL AND householdId IS NULL) OR householdId = :householdId)
        ORDER BY type ASC, name ASC
    """)
    suspend fun getAllCategoriesList(householdId: String? = null): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCategories(categories: List<CategoryEntity>)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("""
        UPDATE categories SET name = :newName 
        WHERE ((:householdId IS NULL AND householdId IS NULL) OR householdId = :householdId)
        AND name = :oldName AND type = :type
    """)
    suspend fun updateCategoryGroup(oldName: String, newName: String, type: String, householdId: String? = null)

    @Query("""
        DELETE FROM categories 
        WHERE ((:householdId IS NULL AND householdId IS NULL) OR householdId = :householdId)
        AND name = :name AND type = :type
    """)
    suspend fun deleteCategoryGroup(name: String, type: String, householdId: String? = null)

    @Query("UPDATE categories SET subCategory = :newSubCategory WHERE id = :id")
    suspend fun updateSubcategory(id: String, newSubCategory: String)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteSubcategory(id: String)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)

    @Query("""
        SELECT * FROM categories 
        WHERE ((:householdId IS NULL AND householdId IS NULL) OR householdId = :householdId)
        AND name = :name AND type = :type
    """)
    suspend fun getCategoriesGroup(name: String, type: String, householdId: String? = null): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Query("""
        DELETE FROM categories 
        WHERE ((:householdId IS NULL AND householdId IS NULL) OR householdId = :householdId)
    """)
    suspend fun deleteCategoriesByHousehold(householdId: String? = null)

    @Query("""
        DELETE FROM categories 
        WHERE ((:householdId IS NULL AND householdId IS NULL) OR householdId = :householdId)
    """)
    suspend fun deleteAllCategories(householdId: String? = null)

    @Query("""
        SELECT * FROM categories
        WHERE ((:householdId IS NULL AND householdId IS NULL) OR householdId = :householdId)
        AND type = :type AND name = :name AND subCategory = :subCategory
    """)
    suspend fun getCategoriesByLogicalIdentity(
        householdId: String?,
        type: String,
        name: String,
        subCategory: String
    ): List<CategoryEntity>

    @Query("""
        DELETE FROM categories
        WHERE ((:householdId IS NULL AND householdId IS NULL) OR householdId = :householdId)
        AND type = :type AND name = :name AND subCategory = :subCategory
    """)
    suspend fun deleteCategoriesByLogicalIdentity(
        householdId: String?,
        type: String,
        name: String,
        subCategory: String
    )
}

