package com.example.data.repository

import com.example.data.dao.CategoryDao
import com.example.data.model.CategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class RoomCategoryRepository(private val categoryDao: CategoryDao) : CategoryRepository {

    override val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    override suspend fun getAllCategoriesList(): List<CategoryEntity> = allCategories.first()

    override suspend fun ensureDefaultCategoriesSeeded() {
        val existing = allCategories.first()
        if (existing.isEmpty()) {
            val defaults = listOf(
                // Income Categories & Subcategories with Emojis
                CategoryEntity(name = "💼 Salary", type = "Income", subCategory = "🏢 Main Job"),
                CategoryEntity(name = "💼 Salary", type = "Income", subCategory = "🎁 Bonus"),
                CategoryEntity(name = "💻 Freelance", type = "Income", subCategory = "⚙️ Software & Consulting"),
                CategoryEntity(name = "📈 Investments", type = "Income", subCategory = "📊 Stocks & Dividends"),
                CategoryEntity(name = "💵 Other Income", type = "Income", subCategory = "🎉 Gifts & Side Jobs"),

                // Expense Categories & Subcategories with Emojis
                CategoryEntity(name = "🍉 Food & Dining", type = "Expense", subCategory = "🛒 Groceries"),
                CategoryEntity(name = "🍉 Food & Dining", type = "Expense", subCategory = "🍔 Restaurants & Cafes"),
                CategoryEntity(name = "🍉 Food & Dining", type = "Expense", subCategory = "💳 Meal Tickets"),
                CategoryEntity(name = "🏠 Housing & Utilities", type = "Expense", subCategory = "🔑 Rent / Mortgage"),
                CategoryEntity(name = "🏠 Housing & Utilities", type = "Expense", subCategory = "⚡ Utilities & Internet"),
                CategoryEntity(name = "🚗 Transportation", type = "Expense", subCategory = "⛽ Fuel"),
                CategoryEntity(name = "🚗 Transportation", type = "Expense", subCategory = "🚌 Public Transit & Rides"),
                CategoryEntity(name = "🛍️ Shopping & Personal", type = "Expense", subCategory = "👕 Apparel & Shoes"),
                CategoryEntity(name = "🛍️ Shopping & Personal", type = "Expense", subCategory = "📱 Electronics"),
                CategoryEntity(name = "🏥 Health & Wellness", type = "Expense", subCategory = "💊 Pharmacy & Medical"),
                CategoryEntity(name = "🏥 Health & Wellness", type = "Expense", subCategory = "🏋️ Gym & Fitness"),
                CategoryEntity(name = "🎬 Entertainment", type = "Expense", subCategory = "🍿 Subscriptions & Media"),
                CategoryEntity(name = "🎬 Entertainment", type = "Expense", subCategory = "✈️ Travel & Vacations"),
                CategoryEntity(name = "🏦 Financial & Taxes", type = "Expense", subCategory = "💳 Bank Fees & Insurance")
            )
            categoryDao.insertAllCategories(defaults)
        }
    }

    override suspend fun addCategory(name: String, type: String, subCategory: String) {
        val category = CategoryEntity(name = name.trim(), type = type, subCategory = subCategory.trim())
        categoryDao.insertCategory(category)
    }

    override suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(category)
    }

    override suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }

    override suspend fun updateCategoryGroup(oldName: String, newName: String, type: String) {
        categoryDao.updateCategoryGroup(oldName.trim(), newName.trim(), type)
    }

    override suspend fun deleteCategoryGroup(name: String, type: String) {
        categoryDao.deleteCategoryGroup(name, type)
    }

    override suspend fun updateSubcategory(id: String, newSubCategory: String) {
        categoryDao.updateSubcategory(id, newSubCategory.trim())
    }

    override suspend fun deleteSubcategory(id: String) {
        categoryDao.deleteSubcategory(id)
    }
}
