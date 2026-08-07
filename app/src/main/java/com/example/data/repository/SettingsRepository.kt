package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.FilterSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fintrack_settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val SELECTED_PERIOD = stringPreferencesKey("selected_period")
        val SELECTED_CURRENCY = stringPreferencesKey("selected_currency")
        val CUSTOM_START_DATE = stringPreferencesKey("custom_start_date")
        val CUSTOM_END_DATE = stringPreferencesKey("custom_end_date")
        val SELECTED_INCOME_CAT = stringPreferencesKey("selected_income_cat")
        val SELECTED_EXPENSE_CAT = stringPreferencesKey("selected_expense_cat")
        val SELECTED_TYPE = stringPreferencesKey("selected_type")
        val THEME_MODE = stringPreferencesKey("theme_mode") // "system", "dark", "light"
    }

    val filterSettingsFlow: Flow<FilterSettings> = context.dataStore.data.map { prefs ->
        FilterSettings(
            selectedPeriod = prefs[PreferencesKeys.SELECTED_PERIOD] ?: "Last Month",
            selectedCurrency = prefs[PreferencesKeys.SELECTED_CURRENCY] ?: "RON",
            customStartDate = prefs[PreferencesKeys.CUSTOM_START_DATE] ?: "",
            customEndDate = prefs[PreferencesKeys.CUSTOM_END_DATE] ?: "",
            selectedType = prefs[PreferencesKeys.SELECTED_TYPE] ?: "All",
            selectedIncomeCategory = prefs[PreferencesKeys.SELECTED_INCOME_CAT],
            selectedExpenseCategory = prefs[PreferencesKeys.SELECTED_EXPENSE_CAT]
        )
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.THEME_MODE] ?: "light" // Default to Clean Minimalism light theme
    }

    suspend fun updateSelectedPeriod(period: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SELECTED_PERIOD] = period
        }
    }

    suspend fun updateSelectedCurrency(currency: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SELECTED_CURRENCY] = currency
        }
    }

    suspend fun updateCustomDateRange(startDate: String, endDate: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.CUSTOM_START_DATE] = startDate
            prefs[PreferencesKeys.CUSTOM_END_DATE] = endDate
            prefs[PreferencesKeys.SELECTED_PERIOD] = "Custom Range"
        }
    }

    suspend fun updateSelectedType(type: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SELECTED_TYPE] = type
        }
    }

    suspend fun updateCategoryFilter(type: String, categoryName: String?) {
        context.dataStore.edit { prefs ->
            if (categoryName == null) {
                prefs.remove(PreferencesKeys.SELECTED_INCOME_CAT)
                prefs.remove(PreferencesKeys.SELECTED_EXPENSE_CAT)
            } else if (type == "Income") {
                prefs[PreferencesKeys.SELECTED_INCOME_CAT] = categoryName
                prefs.remove(PreferencesKeys.SELECTED_EXPENSE_CAT)
            } else {
                prefs[PreferencesKeys.SELECTED_EXPENSE_CAT] = categoryName
                prefs.remove(PreferencesKeys.SELECTED_INCOME_CAT)
            }
        }
    }

    suspend fun updateThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.THEME_MODE] = mode
        }
    }
}
