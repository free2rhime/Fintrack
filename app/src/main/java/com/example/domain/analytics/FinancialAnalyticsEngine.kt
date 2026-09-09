package com.example.domain.analytics

import com.example.data.model.FilterSettings
import com.example.data.model.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
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
    val transactionCount: Int = 0,
    val excludedNonOfficialCount: Int = 0,
    val hasIncompleteEurData: Boolean = false,
    val secondaryCurrency: String = if (currency == "RON") "EUR" else "RON",
    val secondaryCurrencyBalance: Double? = null,
    val latestBnrRate: Double? = null,
    val effectiveBnrDate: String? = null,
    val bnrStatus: String? = null
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
    val savingsTrendText: String = "Stable",
    val savingsRatio: Double? = null,
    val expenseVelocity: Double? = null
)

data class SingleSeriesDataPoint(
    val yearMonth: String, // e.g., "2026-03"
    val monthYearLabel: String, // e.g., "Mar 2026"
    val value: Double
)

data class SingleSeriesAnalyticsResult(
    val dataPoints: List<SingleSeriesDataPoint>,
    val total: Double,
    val monthlyAverage: Double,
    val monthCount: Int,
    val currency: String
)

data class CategoryRankingItem(
    val categoryName: String,
    val totalAmount: Double,
    val percentage: Double,
    val transactionCount: Int
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

            if (!ignoreCategoryFilter && settings.selectedType != "All") {
                match = match && tx.type.equals(settings.selectedType, ignoreCase = true)
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
        periodLabel: String,
        latestBnrRate: Double? = null,
        effectiveBnrDate: String? = null,
        bnrStatus: String? = null
    ): DashboardMetrics {
        val useRon = currency == "RON"

        var incomeRon = 0.0
        var expenseRon = 0.0
        var incomeEur = 0.0
        var expenseEur = 0.0
        val categoryExpenses = mutableMapOf<String, Double>()
        var nonOfficialEurCount = 0

        for (tx in transactions) {
            val isOfficialEur = tx.conversionStatus == "OFFICIAL" && tx.exchangeRateSource == "BNR_OFFICIAL" && tx.exchangeRate > 0.0
            val amtRon = tx.amountRON

            if (tx.type == "Income") {
                incomeRon += amtRon
            } else if (tx.type == "Expense") {
                expenseRon += amtRon
                if (useRon) {
                    categoryExpenses[tx.category] = (categoryExpenses[tx.category] ?: 0.0) + amtRon
                }
            }

            if (isOfficialEur) {
                val amtEur = tx.amountEUR
                if (tx.type == "Income") {
                    incomeEur += amtEur
                } else if (tx.type == "Expense") {
                    expenseEur += amtEur
                    if (!useRon) {
                        categoryExpenses[tx.category] = (categoryExpenses[tx.category] ?: 0.0) + amtEur
                    }
                }
            } else {
                nonOfficialEurCount++
            }
        }

        val incomeSum = if (useRon) incomeRon else incomeEur
        val expenseSum = if (useRon) expenseRon else expenseEur
        val balance = incomeSum - expenseSum
        val savingsRate = if (incomeSum > 0.0) ((incomeSum - expenseSum) / incomeSum) * 100.0 else 0.0
        val expensePressure = if (incomeSum > 0.0) (expenseSum / incomeSum) * 100.0 else 0.0

        val topCategoryEntry = categoryExpenses.maxByOrNull { it.value }
        val topCategory = topCategoryEntry?.key ?: "N/A"
        val topCategoryAmt = topCategoryEntry?.value ?: 0.0
        val concentrationPct = if (expenseSum > 0.0) (topCategoryAmt / expenseSum) * 100.0 else 0.0

        val secondaryCurrency = if (useRon) "EUR" else "RON"
        val secondaryCurrencyBalance = if (nonOfficialEurCount == 0) {
            roundTwoDecimals(if (useRon) (incomeEur - expenseEur) else (incomeRon - expenseRon))
        } else {
            null
        }

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
            transactionCount = transactions.size,
            excludedNonOfficialCount = if (useRon) 0 else nonOfficialEurCount,
            hasIncompleteEurData = (!useRon) && (nonOfficialEurCount > 0),
            secondaryCurrency = secondaryCurrency,
            secondaryCurrencyBalance = secondaryCurrencyBalance,
            latestBnrRate = latestBnrRate,
            effectiveBnrDate = effectiveBnrDate,
            bnrStatus = bnrStatus
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
        val filtered = if (useRon) {
            transactions.filter { it.type == type }
        } else {
            transactions.filter { it.type == type && it.conversionStatus == "OFFICIAL" && it.exchangeRateSource == "BNR_OFFICIAL" && it.exchangeRate > 0.0 }
        }
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

        return grouped.entries
            .sortedBy { it.key }
            .map { (yearMonth, txList) ->
                var inc = 0.0
                var exp = 0.0
                for (tx in txList) {
                    if (useRon) {
                        val amt = tx.amountRON
                        if (tx.type == "Income") inc += amt else exp += amt
                    } else {
                        if (tx.conversionStatus == "OFFICIAL" && tx.exchangeRateSource == "BNR_OFFICIAL" && tx.exchangeRate > 0.0) {
                            val amt = tx.amountEUR
                            if (tx.type == "Income") inc += amt else exp += amt
                        }
                    }
                }
                MonthlyDataPoint(
                    monthYearLabel = formatYearMonthLabel(yearMonth),
                    income = roundTwoDecimals(inc),
                    expense = roundTwoDecimals(exp),
                    balance = roundTwoDecimals(inc - exp)
                )
            }
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

            "Last Month" -> { // Current Month
                val calStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val calEnd = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                }
                Pair(sdf.format(calStart.time), sdf.format(calEnd.time))
            }

            "Previous Month" -> { // Previous Month
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

    fun formatYearMonthLabel(yearMonth: String): String {
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

    /**
     * Generates a contiguous, chronological list of "yyyy-MM" strings representing the full temporal axis
     * of the specified filter period.
     */
    fun generateContiguousMonthsForPeriod(
        period: String,
        customStart: String = "",
        customEnd: String = "",
        referenceDates: List<String> = emptyList(),
        referenceCal: Calendar = Calendar.getInstance()
    ): List<String> {
        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.US)
        val currentYearMonth = sdfMonth.format(referenceCal.time)

        when (period) {
            "Last Month" -> return listOf(currentYearMonth)
            "Previous Month" -> {
                val prevCal = (referenceCal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                return listOf(sdfMonth.format(prevCal.time))
            }
            "Last 3 Months" -> {
                val startCal = (referenceCal.clone() as Calendar).apply { add(Calendar.MONTH, -2) }
                return generateMonthsBetween(sdfMonth.format(startCal.time), currentYearMonth)
            }
            "Last 6 Months" -> {
                val startCal = (referenceCal.clone() as Calendar).apply { add(Calendar.MONTH, -5) }
                return generateMonthsBetween(sdfMonth.format(startCal.time), currentYearMonth)
            }
            "Last 12 Months" -> {
                val startCal = (referenceCal.clone() as Calendar).apply { add(Calendar.MONTH, -11) }
                return generateMonthsBetween(sdfMonth.format(startCal.time), currentYearMonth)
            }
            "Year To Date" -> {
                val calStart = (referenceCal.clone() as Calendar).apply { set(Calendar.MONTH, Calendar.JANUARY) }
                return generateMonthsBetween(sdfMonth.format(calStart.time), currentYearMonth)
            }
            "Custom Range" -> {
                val startYm = if (customStart.length >= 7) customStart.substring(0, 7) else {
                    val minDate = referenceDates.minOrNull()
                    if (minDate != null && minDate.length >= 7) minDate.substring(0, 7) else currentYearMonth
                }
                val endYm = if (customEnd.length >= 7) customEnd.substring(0, 7) else currentYearMonth
                return generateMonthsBetween(startYm, endYm)
            }
            "All Time" -> {
                val validDates = referenceDates.filter { it.length >= 7 }
                if (validDates.isEmpty()) return listOf(currentYearMonth)
                val minYm = validDates.minOrNull()!!.substring(0, 7)
                val maxYm = validDates.maxOrNull()!!.substring(0, 7)
                return generateMonthsBetween(minYm, maxYm)
            }
            else -> return listOf(currentYearMonth)
        }
    }

    /**
     * Generates all year-month ("yyyy-MM") strings between start and end (inclusive).
     */
    fun generateMonthsBetween(startYearMonth: String, endYearMonth: String): List<String> {
        if (startYearMonth > endYearMonth) return listOf(startYearMonth)
        val result = mutableListOf<String>()
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val cal = Calendar.getInstance().apply {
            time = try { sdf.parse(startYearMonth) } catch (e: Exception) { null } ?: return listOf(startYearMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val endCal = Calendar.getInstance().apply {
            time = try { sdf.parse(endYearMonth) } catch (e: Exception) { null } ?: return listOf(endYearMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        while (!cal.after(endCal)) {
            result.add(sdf.format(cal.time))
            cal.add(Calendar.MONTH, 1)
        }
        return result
    }

    /**
     * Computes a single series (Income, Expense, specific Category, or specific Income Source)
     * over a complete, contiguous temporal axis of months. Missing months are populated with 0.0.
     * Monthly Average is calculated strictly as total / contiguousMonths.size.
     */
    fun calculateSingleSeries(
        transactions: List<TransactionEntity>,
        contiguousYearMonths: List<String>,
        currency: String,
        typeFilter: String? = null,
        categoryFilter: String? = null
    ): SingleSeriesAnalyticsResult {
        val useRon = currency == "RON"

        // Pre-filter candidate transactions
        val filtered = transactions.filter { tx ->
            val matchesType = typeFilter == null || tx.type.equals(typeFilter, ignoreCase = true)
            val matchesCategory = categoryFilter == null || tx.category.equals(categoryFilter, ignoreCase = true)
            val matchesCurrency = if (useRon) true else {
                tx.conversionStatus == "OFFICIAL" && tx.exchangeRateSource == "BNR_OFFICIAL" && tx.exchangeRate > 0.0
            }
            matchesType && matchesCategory && matchesCurrency
        }

        // Group by year-month ("yyyy-MM")
        val groupedByYm = filtered.groupBy {
            if (it.date.length >= 7) it.date.substring(0, 7) else it.date
        }

        val dataPoints = contiguousYearMonths.map { ym ->
            val txsInMonth = groupedByYm[ym] ?: emptyList()
            val monthSum = txsInMonth.sumOf { if (useRon) it.amountRON else it.amountEUR }
            SingleSeriesDataPoint(
                yearMonth = ym,
                monthYearLabel = formatYearMonthLabel(ym),
                value = roundTwoDecimals(monthSum)
            )
        }

        val total = roundTwoDecimals(dataPoints.sumOf { it.value })
        val monthCount = contiguousYearMonths.size
        val avg = if (monthCount > 0) roundTwoDecimals(total / monthCount) else 0.0

        return SingleSeriesAnalyticsResult(
            dataPoints = dataPoints,
            total = total,
            monthlyAverage = avg,
            monthCount = monthCount,
            currency = currency
        )
    }

    /**
     * Calculates rankings of Expense categories sorted descending by their percentage share
     * of total expenses in the period.
     */
    fun calculateExpenseCategoryRankings(
        transactions: List<TransactionEntity>,
        currency: String
    ): List<CategoryRankingItem> {
        val useRon = currency == "RON"
        val filtered = if (useRon) {
            transactions.filter { it.type == "Expense" }
        } else {
            transactions.filter {
                it.type == "Expense" &&
                it.conversionStatus == "OFFICIAL" &&
                it.exchangeRateSource == "BNR_OFFICIAL" &&
                it.exchangeRate > 0.0
            }
        }

        val totalExpense = filtered.sumOf { if (useRon) it.amountRON else it.amountEUR }
        if (totalExpense <= 0.0 && filtered.isEmpty()) return emptyList()

        return filtered
            .groupBy { it.category }
            .map { (cat, txList) ->
                val catSum = txList.sumOf { if (useRon) it.amountRON else it.amountEUR }
                val pct = if (totalExpense > 0.0) roundOneDecimal((catSum / totalExpense) * 100.0) else 0.0
                CategoryRankingItem(
                    categoryName = cat,
                    totalAmount = roundTwoDecimals(catSum),
                    percentage = pct,
                    transactionCount = txList.size
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    /**
     * Calculates rankings of Income sources (which are Income categories) sorted descending by their percentage share
     * of total income in the period.
     */
    fun calculateIncomeSourceRankings(
        transactions: List<TransactionEntity>,
        currency: String
    ): List<CategoryRankingItem> {
        val useRon = currency == "RON"
        val filtered = if (useRon) {
            transactions.filter { it.type == "Income" }
        } else {
            transactions.filter {
                it.type == "Income" &&
                it.conversionStatus == "OFFICIAL" &&
                it.exchangeRateSource == "BNR_OFFICIAL" &&
                it.exchangeRate > 0.0
            }
        }

        val totalIncome = filtered.sumOf { if (useRon) it.amountRON else it.amountEUR }
        if (totalIncome <= 0.0 && filtered.isEmpty()) return emptyList()

        return filtered
            .groupBy { it.category }
            .map { (cat, txList) ->
                val catSum = txList.sumOf { if (useRon) it.amountRON else it.amountEUR }
                val pct = if (totalIncome > 0.0) roundOneDecimal((catSum / totalIncome) * 100.0) else 0.0
                CategoryRankingItem(
                    categoryName = cat,
                    totalAmount = roundTwoDecimals(catSum),
                    percentage = pct,
                    transactionCount = txList.size
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    private fun roundTwoDecimals(value: Double): Double {
        return Math.round(value * 100.0) / 100.0
    }

    private fun roundOneDecimal(value: Double): Double {
        return Math.round(value * 10.0) / 10.0
    }
}
