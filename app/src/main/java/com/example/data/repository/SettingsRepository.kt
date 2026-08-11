package com.example.data.repository

import com.example.data.model.FilterSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    val filterSettingsFlow: Flow<FilterSettings>

    val themeModeFlow: Flow<String>

    suspend fun updateSelectedPeriod(period: String)

    suspend fun updateSelectedCurrency(currency: String)

    suspend fun updateCustomDateRange(startDate: String, endDate: String)

    suspend fun updateSelectedType(type: String)

    suspend fun updateCategoryFilter(type: String, categoryName: String?)

    suspend fun updateThemeMode(mode: String)
}
