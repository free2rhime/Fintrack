package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["date"]),
        Index(value = ["category"]),
        Index(value = ["type"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "local_user",
    val date: String, // YYYY-MM-DD
    val description: String,
    val amountRON: Double,
    val amountEUR: Double,
    val exchangeRate: Double,
    val exchangeRateDate: String,
    val type: String, // "Income" or "Expense"
    val account: String, // "Card", "Cash", "Meal Tickets"
    val category: String,
    val subCategory: String,
    val destination: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
