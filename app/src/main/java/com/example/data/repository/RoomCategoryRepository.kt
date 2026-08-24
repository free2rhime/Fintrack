package com.example.data.repository

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.example.data.dao.CategoryDao
import com.example.data.dao.SyncOutboxDao
import com.example.data.db.FinTrackDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.SyncOutboxEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class RoomCategoryRepository(
    private val categoryDao: CategoryDao,
    private val syncOutboxDao: SyncOutboxDao? = null,
    private val database: RoomDatabase? = null,
    private val onOutboxMutated: (() -> Unit)? = null
) : CategoryRepository {

    override fun getCategories(householdId: String?): Flow<List<CategoryEntity>> {
        return categoryDao.getAllCategories(householdId)
    }

    override suspend fun getAllCategoriesList(): List<CategoryEntity> = allCategories.first()

    override suspend fun ensureDefaultCategoriesSeeded(householdId: String?) {
        val existing = categoryDao.getAllCategories(householdId).first()
        if (existing.isEmpty()) {
            val defaults = listOf(
                // Income Categories & Subcategories with Emojis
                CategoryEntity(name = "💼 Salary", type = "Income", subCategory = "🏢 Main Job", householdId = householdId),
                CategoryEntity(name = "💼 Salary", type = "Income", subCategory = "🎁 Bonus", householdId = householdId),
                CategoryEntity(name = "💻 Freelance", type = "Income", subCategory = "⚙️ Software & Consulting", householdId = householdId),
                CategoryEntity(name = "📈 Investments", type = "Income", subCategory = "📊 Stocks & Dividends", householdId = householdId),
                CategoryEntity(name = "💵 Other Income", type = "Income", subCategory = "🎉 Gifts & Side Jobs", householdId = householdId),

                // Expense Categories & Subcategories with Emojis
                CategoryEntity(name = "🍉 Food & Dining", type = "Expense", subCategory = "🛒 Groceries", householdId = householdId),
                CategoryEntity(name = "🍉 Food & Dining", type = "Expense", subCategory = "🍔 Restaurants & Cafes", householdId = householdId),
                CategoryEntity(name = "🍉 Food & Dining", type = "Expense", subCategory = "💳 Meal Tickets", householdId = householdId),
                CategoryEntity(name = "🏠 Housing & Utilities", type = "Expense", subCategory = "🔑 Rent / Mortgage", householdId = householdId),
                CategoryEntity(name = "🏠 Housing & Utilities", type = "Expense", subCategory = "⚡ Utilities & Internet", householdId = householdId),
                CategoryEntity(name = "🚗 Transportation", type = "Expense", subCategory = "⛽ Fuel", householdId = householdId),
                CategoryEntity(name = "🚗 Transportation", type = "Expense", subCategory = "🚌 Public Transit & Rides", householdId = householdId),
                CategoryEntity(name = "🛍️ Shopping & Personal", type = "Expense", subCategory = "👕 Apparel & Shoes", householdId = householdId),
                CategoryEntity(name = "🛍️ Shopping & Personal", type = "Expense", subCategory = "📱 Electronics", householdId = householdId),
                CategoryEntity(name = "🏥 Health & Wellness", type = "Expense", subCategory = "💊 Pharmacy & Medical", householdId = householdId),
                CategoryEntity(name = "🏥 Health & Wellness", type = "Expense", subCategory = "🏋️ Gym & Fitness", householdId = householdId),
                CategoryEntity(name = "🎬 Entertainment", type = "Expense", subCategory = "🍿 Subscriptions & Media", householdId = householdId),
                CategoryEntity(name = "🎬 Entertainment", type = "Expense", subCategory = "✈️ Travel & Vacations", householdId = householdId),
                CategoryEntity(name = "🏦 Financial & Taxes", type = "Expense", subCategory = "💳 Bank Fees & Insurance", householdId = householdId)
            )
            executeWithTransaction {
                categoryDao.insertAllCategories(defaults)
                if (householdId != null) {
                    for (cat in defaults) {
                        enqueueOutboxInternal(cat.id, "UPSERT")
                    }
                }
            }
        }
    }

    override suspend fun addCategory(name: String, type: String, subCategory: String, userId: String, householdId: String?) {
        val category = CategoryEntity(
            name = name.trim(),
            type = type,
            subCategory = subCategory.trim(),
            userId = userId,
            householdId = householdId
        )
        executeWithTransaction {
            categoryDao.insertCategory(category)
            enqueueOutboxInternal(category.id, "UPSERT")
        }
    }

    override suspend fun updateCategory(category: CategoryEntity) {
        executeWithTransaction {
            categoryDao.updateCategory(category)
            enqueueOutboxInternal(category.id, "UPSERT")
        }
    }

    override suspend fun deleteCategory(category: CategoryEntity) {
        executeWithTransaction {
            categoryDao.deleteCategory(category)
            enqueueOutboxInternal(category.id, "DELETE")
        }
    }

    override suspend fun updateCategoryGroup(oldName: String, newName: String, type: String, householdId: String?) {
        executeWithTransaction {
            val matching = categoryDao.getCategoriesGroup(oldName.trim(), type, householdId)
            categoryDao.updateCategoryGroup(oldName.trim(), newName.trim(), type, householdId)
            for (cat in matching) {
                enqueueOutboxInternal(cat.id, "UPSERT")
            }
        }
    }

    override suspend fun deleteCategoryGroup(name: String, type: String, householdId: String?) {
        executeWithTransaction {
            val matching = categoryDao.getCategoriesGroup(name.trim(), type, householdId)
            for (cat in matching) {
                enqueueOutboxInternal(cat.id, "DELETE")
            }
            categoryDao.deleteCategoryGroup(name.trim(), type, householdId)
        }
    }

    override suspend fun updateSubcategory(id: String, newSubCategory: String) {
        executeWithTransaction {
            categoryDao.updateSubcategory(id, newSubCategory.trim())
            enqueueOutboxInternal(id, "UPSERT")
        }
    }

    override suspend fun deleteSubcategory(id: String) {
        executeWithTransaction {
            enqueueOutboxInternal(id, "DELETE")
            categoryDao.deleteSubcategory(id)
        }
    }

    private suspend fun <T> executeWithTransaction(block: suspend () -> T): T {
        val result = if (database != null) {
            database.withTransaction { block() }
        } else {
            block()
        }
        onOutboxMutated?.invoke()
        return result
    }

    private suspend fun enqueueOutboxInternal(entityId: String, operation: String) {
        val dao = syncOutboxDao ?: (database as? FinTrackDatabase)?.syncOutboxDao() ?: return
        val now = System.currentTimeMillis()
        val existing = dao.getPendingEntryForEntity(entityId)
        if (existing != null && existing.operation == operation) {
            dao.updateOutboxEntry(existing.copy(updatedAt = now))
        } else {
            dao.insertOutboxEntry(
                SyncOutboxEntity(
                    entityType = "CATEGORY",
                    entityId = entityId,
                    operation = operation,
                    status = "PENDING",
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }
}
