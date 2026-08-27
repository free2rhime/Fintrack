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
import java.util.UUID

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

    override suspend fun ensureDefaultCategoriesSeeded(householdId: String?, enqueueOutbox: Boolean) {
        val existing = categoryDao.getAllCategories(householdId).first()
        if (existing.isEmpty()) {
            val defaults = createDefaultCategories(householdId)
            executeWithTransaction {
                categoryDao.insertAllCategories(defaults)
                if (enqueueOutbox && householdId != null) {
                    for (cat in defaults) {
                        enqueueOutboxInternal(cat.id, "UPSERT")
                    }
                }
            }
        }
    }

    override suspend fun addCategory(name: String, type: String, subCategory: String, userId: String, householdId: String?) {
        val canonicalId = generateDefaultCategoryId(
            householdId = householdId,
            type = type,
            name = name.trim(),
            subCategory = subCategory.trim()
        )
        val category = CategoryEntity(
            id = canonicalId,
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
            val allIds = matching.map { it.id }.toMutableSet()
            for (cat in matching) {
                val canonicalId = generateDefaultCategoryId(cat.householdId, cat.type, cat.name, cat.subCategory)
                allIds.add(canonicalId)
            }
            for (catId in allIds) {
                enqueueOutboxInternal(catId, "DELETE")
                categoryDao.deleteCategoryById(catId)
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
            val target = categoryDao.getCategoryById(id)
            if (target != null) {
                val canonicalId = generateDefaultCategoryId(target.householdId, target.type, target.name, target.subCategory)
                val matching = categoryDao.getCategoriesByLogicalIdentity(
                    householdId = target.householdId,
                    type = target.type,
                    name = target.name,
                    subCategory = target.subCategory
                )
                val allIdsToDelete = (matching.map { it.id } + listOf(id, canonicalId)).distinct()
                for (catId in allIdsToDelete) {
                    enqueueOutboxInternal(catId, "DELETE")
                    categoryDao.deleteCategoryById(catId)
                }
            } else {
                enqueueOutboxInternal(id, "DELETE")
                categoryDao.deleteSubcategory(id)
            }
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

    companion object {
        fun generateDefaultCategoryId(householdId: String?, type: String, name: String, subCategory: String): String {
            val rawKey = "${householdId ?: "global"}_${type.trim()}_${name.trim()}_${subCategory.trim()}"
            return UUID.nameUUIDFromBytes(rawKey.toByteArray(Charsets.UTF_8)).toString()
        }

        fun createDefaultCategories(householdId: String?): List<CategoryEntity> {
            fun createCat(name: String, type: String, subCategory: String): CategoryEntity {
                val id = generateDefaultCategoryId(householdId, type, name, subCategory)
                return CategoryEntity(
                    id = id,
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
