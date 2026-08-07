package com.example.data.model

data class FilterSettings(
    val selectedPeriod: String = "Last Month", // "All Time", "Last Month", "Previous Month", "Last 3 Months", "Last 6 Months", "Last 12 Months", "Year To Date", "Custom Range"
    val selectedCurrency: String = "RON", // "RON" or "EUR"
    val customStartDate: String = "",
    val customEndDate: String = "",
    val selectedType: String = "All", // "All", "Income", "Expense"
    val selectedIncomeCategory: String? = null,
    val selectedExpenseCategory: String? = null,
    val searchQuery: String = ""
)
