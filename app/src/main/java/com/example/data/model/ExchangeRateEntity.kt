package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey
    val date: String, // YYYY-MM-DD
    val requestedDate: String = date,
    val effectiveDate: String = date,
    val rate: Double, // EUR to RON (e.g., 4.9765)
    val source: String = "BNR_OFFICIAL",
    val fetchedAt: Long = System.currentTimeMillis(),
    val status: String = "OFFICIAL" // "OFFICIAL", "PENDING", "FAILED", "UNVERIFIED"
)
