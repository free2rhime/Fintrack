package com.example.data.util

import com.example.data.model.TransactionEntity
import com.example.data.service.ExchangeRateService
import java.util.UUID

data class CsvImportResult(
    val insertedCount: Int,
    val updatedCount: Int,
    val skippedCount: Int,
    val errors: List<String>,
    val transactionsToInsert: List<TransactionEntity>
)

object CsvImporter {

    fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString())
        return tokens
    }

    suspend fun parseAndValidateCsv(
        csvContent: String,
        existingIds: Set<String>,
        exchangeRateService: ExchangeRateService
    ): CsvImportResult {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return CsvImportResult(0, 0, 0, listOf("CSV file is empty"), emptyList())
        }

        val headerRow = parseCsvLine(lines.first()).map { it.trim().lowercase() }
        val colMap = headerRow.withIndex().associate { it.value to it.index }

        fun getVal(row: List<String>, colName: String): String? {
            val idx = colMap[colName.lowercase()] ?: return null
            return if (idx < row.size) row[idx].trim() else null
        }

        var insertedCount = 0
        var updatedCount = 0
        var skippedCount = 0
        val errors = mutableListOf<String>()
        val txsToSave = mutableListOf<TransactionEntity>()

        val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")

        for ((lineIdx, line) in lines.drop(1).withIndex()) {
            val rowNum = lineIdx + 2
            val tokens = parseCsvLine(line)
            if (tokens.all { it.isBlank() }) continue

            val txIdRaw = getVal(tokens, "transaction_id")
            val dateRaw = getVal(tokens, "transaction_date") ?: getVal(tokens, "date")
            val amountRonRaw = getVal(tokens, "amount_ron") ?: getVal(tokens, "amountron")
            val typeRaw = getVal(tokens, "type")
            val descRaw = getVal(tokens, "description")
            val accountRaw = getVal(tokens, "account")
            val categoryRaw = getVal(tokens, "category")
            val subCategoryRaw = getVal(tokens, "subcategory")
            val destinationRaw = getVal(tokens, "destination")

            val amountEurRaw = getVal(tokens, "amount_eur") ?: getVal(tokens, "amounteur")
            val exchangeRateRaw = getVal(tokens, "exchange_rate") ?: getVal(tokens, "exchangerate")
            val exchangeRateDateRaw = getVal(tokens, "effective_bnr_rate_date") ?: getVal(tokens, "exchange_rate_date")
            val exchangeRateSourceRaw = getVal(tokens, "exchange_rate_source")
            val conversionStatusRaw = getVal(tokens, "conversion_status")

            if (dateRaw.isNullOrBlank() || !dateRegex.matches(dateRaw)) {
                skippedCount++
                errors.add("Row $rowNum: Invalid or missing date '$dateRaw'")
                continue
            }

            val amountRON = amountRonRaw?.toDoubleOrNull()
            if (amountRON == null || amountRON < 0.0) {
                skippedCount++
                errors.add("Row $rowNum: Invalid or missing amount_ron '$amountRonRaw'")
                continue
            }

            val type = when (typeRaw?.trim()?.lowercase()) {
                "income" -> "Income"
                "expense" -> "Expense"
                else -> null
            }
            if (type == null) {
                skippedCount++
                errors.add("Row $rowNum: Invalid transaction type '$typeRaw' (must be Income or Expense)")
                continue
            }

            if (descRaw.isNullOrBlank()) {
                skippedCount++
                errors.add("Row $rowNum: Missing description")
                continue
            }

            if (accountRaw.isNullOrBlank()) {
                skippedCount++
                errors.add("Row $rowNum: Missing account")
                continue
            }

            if (categoryRaw.isNullOrBlank()) {
                skippedCount++
                errors.add("Row $rowNum: Missing category")
                continue
            }

            val subCategory = if (!subCategoryRaw.isNullOrBlank()) subCategoryRaw else categoryRaw
            val destination = if (type == "Income" && !destinationRaw.isNullOrBlank()) destinationRaw else null

            val id = if (!txIdRaw.isNullOrBlank()) txIdRaw else UUID.randomUUID().toString()
            val isExisting = existingIds.contains(id)

            // Resolve EUR and exchange rate safely
            var finalRate = exchangeRateRaw?.toDoubleOrNull() ?: 0.0
            var finalAmountEUR = amountEurRaw?.toDoubleOrNull() ?: 0.0
            var finalRateDate = exchangeRateDateRaw ?: dateRaw
            var finalRateSource = if (!exchangeRateSourceRaw.isNullOrBlank()) exchangeRateSourceRaw else "BNR_OFFICIAL"
            var finalStatus = if (!conversionStatusRaw.isNullOrBlank()) conversionStatusRaw else "OFFICIAL"

            if (finalAmountEUR <= 0.0 || finalRate <= 0.0 || finalStatus != "OFFICIAL") {
                val bnrResult = exchangeRateService.getOfficialRate(dateRaw)
                if (bnrResult.status == "OFFICIAL" && bnrResult.rate > 0.0) {
                    finalRate = bnrResult.rate
                    finalRateDate = bnrResult.effectiveDate
                    finalAmountEUR = ExchangeRateService.calculateAmountEUR(amountRON, bnrResult.rate)
                    finalRateSource = "BNR_OFFICIAL"
                    finalStatus = "OFFICIAL"
                } else {
                    finalRate = 0.0
                    finalAmountEUR = 0.0
                    finalRateDate = dateRaw
                    finalRateSource = "BNR_OFFICIAL"
                    finalStatus = "PENDING"
                }
            }

            val now = System.currentTimeMillis()
            val tx = TransactionEntity(
                id = id,
                date = dateRaw,
                description = descRaw.trim(),
                amountRON = amountRON,
                amountEUR = finalAmountEUR,
                exchangeRate = finalRate,
                exchangeRateDate = finalRateDate,
                type = type,
                account = accountRaw.trim(),
                category = categoryRaw.trim(),
                subCategory = subCategory.trim(),
                destination = destination,
                exchangeRateSource = finalRateSource,
                conversionStatus = finalStatus,
                createdAt = now,
                updatedAt = now
            )

            txsToSave.add(tx)
            if (isExisting) {
                updatedCount++
            } else {
                insertedCount++
            }
        }

        return CsvImportResult(
            insertedCount = insertedCount,
            updatedCount = updatedCount,
            skippedCount = skippedCount,
            errors = errors,
            transactionsToInsert = txsToSave
        )
    }
}
