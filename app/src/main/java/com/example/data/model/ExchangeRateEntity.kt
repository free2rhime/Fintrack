package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey
    val date: String, // YYYY-MM-DD
    val rate: Double, // EUR to RON (e.g., 4.9750)
    val cachedAt: Long = System.currentTimeMillis()
)
