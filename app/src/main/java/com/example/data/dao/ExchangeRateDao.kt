package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ExchangeRateEntity

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rates WHERE (date = :date OR requestedDate = :date) AND source = 'BNR_OFFICIAL' AND status = 'OFFICIAL'")
    suspend fun getOfficialRateForDate(date: String): ExchangeRateEntity?

    @Query("SELECT * FROM exchange_rates WHERE date = :date OR requestedDate = :date")
    suspend fun getRateForDate(date: String): ExchangeRateEntity?

    @Query("SELECT * FROM exchange_rates WHERE source = 'BNR_OFFICIAL' AND status = 'OFFICIAL'")
    suspend fun getAllOfficialRates(): List<ExchangeRateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: ExchangeRateEntity)

    @Query("DELETE FROM exchange_rates WHERE (date = :date OR requestedDate = :date) AND (source != 'BNR_OFFICIAL' OR status != 'OFFICIAL')")
    suspend fun deleteUnverifiedRatesForDate(date: String): Int

    @Query("DELETE FROM exchange_rates")
    suspend fun deleteAllRates()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRates(rates: List<ExchangeRateEntity>)
}
