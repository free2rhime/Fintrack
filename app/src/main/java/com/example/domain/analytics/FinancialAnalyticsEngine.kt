package com.example.domain.analytics

import com.example.data.model.FilterSettings
import com.example.data.model.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DashboardMetrics(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val savingsRate: Double = 0.0,
    val expensePressure: Double = 0.0,
    val topExpenseCategory: String = "N/A",
    val topExpenseCategoryAmount: Double = 0.0,
    val categoryConcentrationPercent: Double = 0.0,
    val currency: String = "RON",
    val periodLabel: String = "Last Month",
    val transactionCount: Int = 0
)

data class CategoryExpenseShare(
    val categoryName: String,
    val totalAmount: Double,
    val percentage: Double,
    val transactionCount: Int
)

data class MonthlyDataPoint(
    val monthYearLabel: String, // e.g., "2026-03" or "Mar 2026"
    val income: Double,
    val expense: Double,
    val balance: Double
)

data class SmartFinancialInsights(
    val avgMonthlyExpense: Double = 0.0,
    val avgMonthlyIncome: Double = 0.0,
    val largestExpenseMonth: String = "N/A",
    val largestExpenseMonthAmount: Double = 0.0,
    val largestIncomeMonth: String = "N/A",
    val largestIncomeMonthAmount: Double = 0.0,
    val monthOverMonthExpenseChangePercent: Double = 0.0,
    val savingsTrendText: String = "Stable"
)

object FinancialAnalyticsEngine {

    /**
     * Filters transactions according to FilterSettings period, date range, search query, and category filters.
     */
    fun filterTransactionsByPeriod(
        transactions: List<TransactionEntity>,
        settings: FilterSettings,
        ignoreCategoryFilter: Boolean = false
    ): List<TransactionEntity> {
        val dateBounds = calculateDateBounds(settings.selectedPeriod, settings.customStartDate, settings.customEndDate)
        val startDate = dateBounds.first
        val endDate = dateBounds.second

        val activeCategoryFilter = if (ignoreCategoryFilter) null else (settings.selectedExpenseCategory ?: settings.selectedIncomeCategory)

        return transactions.filter { tx ->
            var match = true
            if (startDate != null && startDate.isNotEmpty()) {
                match = match && tx.date >= startDate
            }
            if (endDate != null && endDate.isNotEmpty()) {
                match = match && tx.date <= endDate
            }
            if (settings.searchQuery.isNotBlank() && !ignoreCategoryFilter) {
                val q = settings.searchQuery.trim().lowercase(Locale.ROOT)
                val matchesText = tx.description.lowercase(Locale.ROOT).contains(q) ||
                        tx.category.lowercase(Locale.ROOT).contains(q) ||
                        tx.subCategory.lowercase(Locale.ROOT).contains(q) ||
                        tx.account.lowercase(Locale.ROOT).contains(q)
                match = match && matchesText
            }

            if (!activeCategoryFilter.isNullOrBlank()) {
                match = match && matchesCategory(tx, activeCategoryFilter)
            }

            match
        }
    }

    private fun matchesCategory(tx: TransactionEntity, filter: String): Boolean {
        if (tx.category.equals(filter, ignoreCase = true) || tx.subCategory.equals(filter, ignoreCase = true)) {
            return true
        }

        // Clean emojis & special symbols for robust fuzzy matching
        val filterClean = cleanCategoryString(filter)
        val catClean = cleanCategoryString(tx.category)
        val subClean = cleanCategoryString(tx.subCategory)

        if (filterClean.isBlank()) return true

        if (catClean.equals(filterClean, ignoreCase = true) || subClean.equals(filterClean, ignoreCase = true)) {
            return true
        }

        if (catClean.contains(filterClean, ignoreCase = true) || filterClean.contains(catClean, ignoreCase = true)) {
            return true
        }

        if (subClean.isNotBlank() && (subClean.contains(filterClean, ignoreCase = true) || filterClean.contains(subClean, ignoreCase = true))) {
            return true
        }

        return tx.category.contains(filter, ignoreCase = true) || tx.subCategory.contains(filter, ignoreCase = true)
    }

    private fun cleanCategoryString(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9\\s]"), "").trim().lowercase(Locale.ROOT)
    }

    /**
     * Calculates top card metrics, KPI insights, and ratios.
     */
    fun calculateMetrics(
        transactions: List<TransactionEntity>,
        currency: String,
        periodLabel: String
    ): DashboardMetrics {
        val useRon = currency == "RON"

        var incomeSum = 0.0
        var expenseSum = 0.0
        val categoryExpenses = mutableMapOf<String, Double>()

        for (tx in transactions) {
            val amount = if (useRon) tx.amountRON else tx.amountEUR
            if (tx.type == "Income") {
                incomeSum += amount
            } else if (tx.type == "Expense") {
                expenseSum += amount
                categoryExpenses[tx.category] = (categoryExpenses[tx.category] ?: 0.0) + amount
            }
        }

        val balance = incomeSum - expenseSum
        val savingsRate = if (incomeSum > 0.0) ((incomeSum - expenseSum) / incomeSum) * 100.0 else 0.0
        val expensePressure = if (incomeSum > 0.0) (expenseSum / incomeSum) * 100.0 else 0.0

        val topCategoryEntry = categoryExpenses.maxByOrNull { it.value }
        val topCategory = topCategoryEntry?.key ?: "N/A"
        val topCategoryAmt = topCategoryEntry?.value ?: 0.0
        val concentrationPct = if (expenseSum > 0.0) (topCategoryAmt / expenseSum) * 100.0 else 0.0

        return DashboardMetrics(
            totalIncome = roundTwoDecimals(incomeSum),
            totalExpense = roundTwoDecimals(expenseSum),
            balance = roundTwoDecimals(balance),
            savingsRate = roundOneDecimal(savingsRate),
            expensePressure = roundOneDecimal(expensePressure),
            topExpenseCategory = topCategory,
            topExpenseCategoryAmount = roundTwoDecimals(topCategoryAmt),
            categoryConcentrationPercent = roundOneDecimal(concentrationPct),
            currency = currency,
            periodLabel = periodLabel,
            transactionCount = transactions.size
        )
    }

    /**
     * Computes Category Distribution breakdown.
     */
    fun calculateCategoryShares(
        transactions: List<TransactionEntity>,
        currency: String,
        type: String = "Expense"
    ): List<CategoryExpenseShare> {
        val useRon = currency == "RON"
        val filtered = transactions.filter { it.type == type }
        val totalAmount = filtered.sumOf { if (useRon) it.amountRON else it.amountEUR }

        if (totalAmount <= 0.0) return emptyList()

        return filtered
            .groupBy { it.category }
            .map { (cat, txList) ->
                val catSum = txList.sumOf { if (useRon) it.amountRON else it.amountEUR }
                CategoryExpenseShare(
                    categoryName = cat,
                    totalAmount = roundTwoDecimals(catSum),
                    percentage = roundOneDecimal((catSum / totalAmount) * 100.0),
                    transactionCount = txList.size
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    /**
     * Computes Monthly Cash Flow (Income vs Expense vs Balance by Month).
     */
    fun calculateMonthlyDataPoints(
        transactions: List<TransactionEntity>,
        currency: String
    ): List<MonthlyDataPoint> {
        val useRon = currency == "RON"
        val grouped = transactions.groupBy {
            if (it.date.length >= 7) it.date.substring(0, 7) else it.date
        }

        return grouped.map { (yearMonth, txList) ->
            var inc = 0.0
            var exp = 0.0
            for (tx in txList) {
                val amt = if (useRon) tx.amountRON else tx.amountEUR
                if (tx.type == "Income") inc += amt else exp += amt
            }
            MonthlyDataPoint(
                monthYearLabel = formatYearMonthLabel(yearMonth),
                income = roundTwoDecimals(inc),
                expense = roundTwoDecimals(exp),
                balance = roundTwoDecimals(inc - exp)
            )
        }.sortedBy { it.monthYearLabel }
    }

    /**
     * Generates Smart Financial Insights.
     */
    fun calculateSmartInsights(
        transactions: List<TransactionEntity>,
        currency: String
    ): SmartFinancialInsights {
        val monthlyPoints = calculateMonthlyDataPoints(transactions, currency)
        if (monthlyPoints.isEmpty()) return SmartFinancialInsights()

        val avgExp = monthlyPoints.map { it.expense }.average()
        val avgInc = monthlyPoints.map { it.income }.average()

        val largestExp = monthlyPoints.maxByOrNull { it.expense }
        val largestInc = monthlyPoints.maxByOrNull { it.income }

        var momChange = 0.0
        if (monthlyPoints.size >= 2) {
            val current = monthlyPoints.last().expense
            val previous = monthlyPoints[monthlyPoints.size - 2].expense
            if (previous > 0.0) {
                momChange = ((current - previous) / previous) * 100.0
            }
        }

        val savingsTrend = when {
            avgInc > 0 && (avgInc - avgExp) / avgInc >= 0.20 -> "Strong Capital Growth"
            avgInc > 0 && (avgInc - avgExp) / avgInc >= 0.05 -> "Positive Savings Rate"
            else -> "High Expense Ratio"
        }

        return SmartFinancialInsights(
            avgMonthlyExpense = roundTwoDecimals(if (avgExp.isNaN()) 0.0 else avgExp),
            avgMonthlyIncome = roundTwoDecimals(if (avgInc.isNaN()) 0.0 else avgInc),
            largestExpenseMonth = largestExp?.monthYearLabel ?: "N/A",
            largestExpenseMonthAmount = largestExp?.expense ?: 0.0,
            largestIncomeMonth = largestInc?.monthYearLabel ?: "N/A",
            largestIncomeMonthAmount = largestInc?.income ?: 0.0,
            monthOverMonthExpenseChangePercent = roundOneDecimal(momChange),
            savingsTrendText = savingsTrend
        )
    }

    /**
     * Gets formatted label for period filters including dynamic current/previous month names.
     */
    fun getPeriodDisplayName(period: String): String {
        val sdfMonth = SimpleDateFormat("MMMM", Locale.US)
        return when (period) {
            "Last Month" -> {
                val currentMonthName = sdfMonth.format(Calendar.getInstance().time)
                "Last Month ($currentMonthName)"
            }
            "Previous Month" -> {
                val prevCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
                val prevMonthName = sdfMonth.format(prevCal.time)
                "Previous Month ($prevMonthName)"
            }
            else -> period
        }
    }

    /**
     * Computes start and end date strings based on selected period logic.
     */
    private fun calculateDateBounds(
        period: String,
        customStart: String,
        customEnd: String
    ): Pair<String?, String?> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayCal = Calendar.getInstance()
        val todayStr = sdf.format(todayCal.time)

        return when (period) {
            "All Time" -> Pair(null, null)

            "Last Month" -> { // Current Month (e.g., August 1 to August 31 / today)
                val calStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val calEnd = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                }
                Pair(sdf.format(calStart.time), sdf.format(calEnd.time))
            }

            "Previous Month" -> { // Previous Month (e.g., July 1 to July 31)
                val calStart = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -1)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val calEnd = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -1)
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                }
                Pair(sdf.format(calStart.time), sdf.format(calEnd.time))
            }

            "Last 3 Months" -> {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -3)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                Pair(sdf.format(cal.time), todayStr)
            }

            "Last 6 Months" -> {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -6)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                Pair(sdf.format(cal.time), todayStr)
            }

            "Last 12 Months" -> {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -12)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                Pair(sdf.format(cal.time), todayStr)
            }

            "Year To Date" -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_YEAR, 1)
                }
                Pair(sdf.format(cal.time), todayStr)
            }

            "Custom Range" -> {
                val start = customStart.ifEmpty { "2000-01-01" }
                val end = customEnd.ifEmpty { todayStr }
                Pair(start, end)
            }

            else -> Pair(null, null)
        }
    }

    private fun formatYearMonthLabel(yearMonth: String): String {
        return try {
            val parts = yearMonth.split("-")
            if (parts.size == 2) {
                val year = parts[0]
                val month = parts[1].toInt()
                val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                "${monthNames[month - 1]} $year"
            } else yearMonth
        } catch (e: Exception) {
            yearMonth
        }
    }

    private fun roundTwoDecimals(value: Double): Double {
        return Math.round(value * 100.0) / 100.0
    }

    private fun roundOneDecimal(value: Double): Double {
        return Math.round(value * 10.0) / 10.0
    }
}
