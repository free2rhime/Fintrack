package com.example.data.repository

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.example.data.dao.CategoryDao
import com.example.data.dao.SyncOutboxDao
import com.example.data.dao.TransactionDao
import com.example.data.db.FinTrackDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.SyncOutboxEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class RoomCategoryRepository(
    private val categoryDao: CategoryDao,
    private val syncOutboxDao: SyncOutboxDao? = null,
    private val database: RoomDatabase? = null,
    private val transactionDao: TransactionDao? = null,
    private val onOutboxMutated: (() -> Unit)? = null
) : CategoryRepository {

    override fun getCategories(householdId: String?): Flow<List<CategoryEntity>> {
        return categoryDao.getAllCategories(householdId)
    }

    override suspend fun getAllCategoriesList(householdId: String?): List<CategoryEntity> =
        categoryDao.getAllCategoriesList(householdId)

    override suspend fun ensureDefaultCategoriesSeeded(householdId: String?, enqueueOutbox: Boolean) {
        val existing = categoryDao.getAllCategories(householdId).first()
        if (existing.isEmpty()) {
            val defaults = createDefaultCategories(householdId)
            executeWithTransaction {
                categoryDao.insertAllCategories(defaults)
                if (enqueueOutbox && householdId != null) {
                    for (cat in defaults) {
                        enqueueOutboxInternal(cat.id, "UPSERT", "CATEGORY")
                    }
                }
            }
        }
    }

    override suspend fun addCategory(name: String, type: String, subCategory: String, userId: String, householdId: String?) {
        val id = UUID.randomUUID().toString()
        val category = CategoryEntity(
            id = id,
            name = name.trim(),
            type = type,
            subCategory = subCategory.trim(),
            userId = userId,
            householdId = householdId
        )
        executeWithTransaction {
            categoryDao.insertCategory(category)
            enqueueOutboxInternal(category.id, "UPSERT", "CATEGORY")
        }
    }

    override suspend fun updateCategory(category: CategoryEntity) {
        executeWithTransaction {
            categoryDao.updateCategory(category)
            enqueueOutboxInternal(category.id, "UPSERT", "CATEGORY")
        }
    }

    override suspend fun deleteCategory(category: CategoryEntity) {
        executeWithTransaction {
            categoryDao.deleteCategory(category)
            enqueueOutboxInternal(category.id, "DELETE", "CATEGORY")
        }
    }

    override suspend fun updateCategoryGroup(oldName: String, newName: String, type: String, householdId: String?) {
        executeWithTransaction {
            val cleanOldName = oldName.trim()
            val cleanNewName = newName.trim()
            val matching = categoryDao.getCategoriesGroup(cleanOldName, type, householdId)
            val now = System.currentTimeMillis()
            for (cat in matching) {
                val updated = cat.copy(name = cleanNewName, updatedAt = now)
                categoryDao.updateCategory(updated)
                enqueueOutboxInternal(cat.id, "UPSERT", "CATEGORY")
            }

            // Atomically propagate rename to historical transactions in the same household & type
            val txDao = transactionDao ?: (database as? FinTrackDatabase)?.transactionDao()
            if (txDao != null && cleanOldName != cleanNewName) {
                val affectedTxs = txDao.getTransactionsByCategory(cleanOldName, type, householdId)
                if (affectedTxs.isNotEmpty()) {
                    txDao.updateCategoryName(
                        oldName = cleanOldName,
                        newName = cleanNewName,
                        type = type,
                        updatedAt = now,
                        householdId = householdId
                    )
                    for (tx in affectedTxs) {
                        enqueueOutboxInternal(tx.id, "UPSERT", "TRANSACTION")
                    }
                }
            }
        }
    }

    override suspend fun deleteCategoryGroup(name: String, type: String, householdId: String?) {
        executeWithTransaction {
            val matching = categoryDao.getCategoriesGroup(name.trim(), type, householdId)
            for (cat in matching) {
                enqueueOutboxInternal(cat.id, "DELETE", "CATEGORY")
                categoryDao.deleteCategoryById(cat.id)
            }
            categoryDao.deleteCategoryGroup(name.trim(), type, householdId)
        }
    }

    override suspend fun updateSubcategory(id: String, newSubCategory: String) {
        executeWithTransaction {
            val target = categoryDao.getCategoryById(id)
            if (target != null) {
                val oldSub = target.subCategory
                val newSub = newSubCategory.trim()
                val now = System.currentTimeMillis()
                val updatedCat = target.copy(
                    subCategory = newSub,
                    updatedAt = now
                )
                categoryDao.updateCategory(updatedCat)
                enqueueOutboxInternal(id, "UPSERT", "CATEGORY")

                // Atomically propagate rename to historical transactions in the same household & type
                val txDao = transactionDao ?: (database as? FinTrackDatabase)?.transactionDao()
                if (txDao != null && oldSub.isNotBlank() && oldSub != newSub) {
                    val affectedTxs = txDao.getTransactionsBySubcategory(
                        categoryName = target.name,
                        subCategoryName = oldSub,
                        type = target.type,
                        householdId = target.householdId
                    )
                    if (affectedTxs.isNotEmpty()) {
                        txDao.updateSubcategoryName(
                            oldSubCategory = oldSub,
                            newSubCategory = newSub,
                            categoryName = target.name,
                            type = target.type,
                            updatedAt = now,
                            householdId = target.householdId
                        )
                        for (tx in affectedTxs) {
                            enqueueOutboxInternal(tx.id, "UPSERT", "TRANSACTION")
                        }
                    }
                }
            } else {
                categoryDao.updateSubcategory(id, newSubCategory.trim())
                enqueueOutboxInternal(id, "UPSERT", "CATEGORY")
            }
        }
    }

    override suspend fun deleteSubcategory(id: String) {
        executeWithTransaction {
            categoryDao.deleteSubcategory(id)
            enqueueOutboxInternal(id, "DELETE", "CATEGORY")
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

    private suspend fun enqueueOutboxInternal(entityId: String, operation: String, entityType: String = "CATEGORY") {
        val dao = syncOutboxDao ?: (database as? FinTrackDatabase)?.syncOutboxDao() ?: return
        val now = System.currentTimeMillis()
        val existing = dao.getPendingEntry(entityType, entityId)
        if (existing != null && existing.operation == operation) {
            dao.updateOutboxEntry(existing.copy(updatedAt = now))
        } else {
            dao.insertOutboxEntry(
                SyncOutboxEntity(
                    entityType = entityType,
                    entityId = entityId,
                    operation = operation,
                    status = "PENDING",
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    companion object {
        fun createDefaultCategories(householdId: String?): List<CategoryEntity> {
            fun createCat(name: String, type: String, subCategory: String): CategoryEntity {
                return CategoryEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    type = type,
                    subCategory = subCategory,
                    householdId = householdId
                )
            }

            return listOf(
                // Income Categories & Subcategories with Emojis
                createCat(name = "💼 Salary", type = "Income", subCategory = "🏢 Main Job"),
                createCat(name = "💼 Salary", type = "Income", subCategory = "🎁 Bonus"),
                createCat(name = "💻 Freelance", type = "Income", subCategory = "⚙️ Software & Consulting"),
                createCat(name = "📈 Investments", type = "Income", subCategory = "📊 Stocks & Dividends"),
                createCat(name = "💵 Other Income", type = "Income", subCategory = "🎉 Gifts & Side Jobs"),

                // Expense Categories & Subcategories with Emojis
                createCat(name = "🍉 Food & Dining", type = "Expense", subCategory = "🛒 Groceries"),
                createCat(name = "🍉 Food & Dining", type = "Expense", subCategory = "🍔 Restaurants & Cafes"),
                createCat(name = "🍉 Food & Dining", type = "Expense", subCategory = "💳 Meal Tickets"),
                createCat(name = "🏠 Housing & Utilities", type = "Expense", subCategory = "🔑 Rent / Mortgage"),
                createCat(name = "🏠 Housing & Utilities", type = "Expense", subCategory = "⚡ Utilities & Internet"),
                createCat(name = "🚗 Transportation", type = "Expense", subCategory = "⛽ Fuel"),
                createCat(name = "🚗 Transportation", type = "Expense", subCategory = "🚌 Public Transit & Rides"),
                createCat(name = "🛍️ Shopping & Personal", type = "Expense", subCategory = "👕 Apparel & Shoes"),
                createCat(name = "🛍️ Shopping & Personal", type = "Expense", subCategory = "📱 Electronics"),
                createCat(name = "🏥 Health & Wellness", type = "Expense", subCategory = "💊 Pharmacy & Medical"),
                createCat(name = "🏥 Health & Wellness", type = "Expense", subCategory = "🏋️ Gym & Fitness"),
                createCat(name = "🎬 Entertainment", type = "Expense", subCategory = "🍿 Subscriptions & Media"),
                createCat(name = "🎬 Entertainment", type = "Expense", subCategory = "✈️ Travel & Vacations"),
                createCat(name = "🏦 Financial & Taxes", type = "Expense", subCategory = "💳 Bank Fees & Insurance")
            )
        }
    }
}
