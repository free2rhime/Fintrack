package com.example.data.util

import androidx.room.withTransaction
import com.example.data.db.FinTrackDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.SyncOutboxEntity
import com.example.data.model.TransactionEntity
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlin.math.abs

enum class CsvDuplicateMode {
    SKIP_EXISTING,
    UPDATE_EXISTING
}

data class CsvRowValidationError(
    val rowNumber: Int,
    val field: String,
    val message: String
)

data class MissingCategoryItem(
    val name: String,
    val subCategory: String,
    val type: String
)

data class CsvPreviewData(
    val totalRows: Int,
    val validRowsCount: Int,
    val invalidRowsCount: Int,
    val newIdsCount: Int,
    val existingIdsCount: Int,
    val proposedUpdatesCount: Int,
    val proposedSkipsCount: Int,
    val totalRonIncome: Double,
    val totalRonExpense: Double,
    val officialCount: Int,
    val unverifiedCount: Int,
    val pendingCount: Int,
    val missingCategories: List<MissingCategoryItem>,
    val rowErrors: List<CsvRowValidationError>,
    val validTransactionsToImport: List<TransactionEntity>,
    val duplicateMode: CsvDuplicateMode,
    val rawCsvContent: String
)

data class CsvImportFinalResult(
    val success: Boolean,
    val insertedCount: Int,
    val updatedCount: Int,
    val skippedCount: Int,
    val failedCount: Int,
    val categoriesCreatedCount: Int,
    val subcategoriesCreatedCount: Int,
    val pendingCount: Int,
    val unverifiedCount: Int,
    val backupFilePath: String? = null,
    val errorMessage: String? = null
)

data class CsvLineParseResult(
    val tokens: List<String>,
    val isUnclosedQuote: Boolean
)

object CsvImporter {

    fun parseCsvLine(line: String): CsvLineParseResult {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        val len = line.length
        while (i < len) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < len && line[i + 1] == '"') {
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
        return CsvLineParseResult(tokens, inQuotes)
    }

    fun parseAndValidate(
        csvContent: String,
        existingTransactions: List<TransactionEntity>,
        existingCategories: List<CategoryEntity>,
        duplicateMode: CsvDuplicateMode = CsvDuplicateMode.SKIP_EXISTING,
        householdId: String? = null,
        userId: String = "local_user",
        createdByUid: String? = null
    ): CsvPreviewData {
        val cleanCsv = csvContent.removePrefix("\uFEFF")
        val lines = cleanCsv.lines()

        val rawRows = lines.filterIndexed { idx, line ->
            idx == 0 || line.isNotBlank()
        }

        if (rawRows.isEmpty() || rawRows.first().isBlank()) {
            return CsvPreviewData(
                totalRows = 0,
                validRowsCount = 0,
                invalidRowsCount = 0,
                newIdsCount = 0,
                existingIdsCount = 0,
                proposedUpdatesCount = 0,
                proposedSkipsCount = 0,
                totalRonIncome = 0.0,
                totalRonExpense = 0.0,
                officialCount = 0,
                unverifiedCount = 0,
                pendingCount = 0,
                missingCategories = emptyList(),
                rowErrors = listOf(CsvRowValidationError(1, "Header", "CSV file is empty or unparseable")),
                validTransactionsToImport = emptyList(),
                duplicateMode = duplicateMode,
                rawCsvContent = csvContent
            )
        }

        val headerParse = parseCsvLine(rawRows.first())
        val headerRow = headerParse.tokens.map { it.trim().lowercase() }
        val colMap = headerRow.withIndex().associate { it.value to it.index }

        fun getVal(row: List<String>, vararg names: String): String? {
            for (name in names) {
                val idx = colMap[name.lowercase()] ?: continue
                if (idx < row.size) {
                    val value = row[idx].trim()
                    if (value.isNotEmpty()) return value
                }
            }
            return null
        }

        val existingIds = existingTransactions.map { it.id }.toSet()
        val validTransactions = mutableListOf<TransactionEntity>()
        val rowErrors = mutableListOf<CsvRowValidationError>()
        val missingCategoriesSet = mutableSetOf<MissingCategoryItem>()

        var officialCount = 0
        var unverifiedCount = 0
        var pendingCount = 0
        var newIdsCount = 0
        var existingIdsCount = 0
        var totalRonIncome = 0.0
        var totalRonExpense = 0.0

        val totalDataRows = rawRows.drop(1).count { it.isNotBlank() }

        for ((lineIdx, line) in rawRows.drop(1).withIndex()) {
            val rowNum = lineIdx + 2
            if (line.isBlank()) continue

            val parseResult = parseCsvLine(line)
            if (parseResult.isUnclosedQuote) {
                rowErrors.add(CsvRowValidationError(rowNum, "CSV", "Multiline quoted field or unclosed quote detected"))
                continue
            }

            val tokens = parseResult.tokens
            if (tokens.all { it.isBlank() }) continue

            var rowHasErrors = false

            // 1. Date Validation
            val dateRaw = getVal(tokens, "transaction_date", "date", "requested_rate_date")
            val txDate: LocalDate? = if (!dateRaw.isNullOrBlank()) {
                try {
                    LocalDate.parse(dateRaw)
                } catch (e: Exception) {
                    null
                }
            } else null

            if (txDate == null) {
                rowErrors.add(CsvRowValidationError(rowNum, "Date", "Invalid or missing LocalDate '$dateRaw' (expected YYYY-MM-DD)"))
                rowHasErrors = true
            }

            // 2. Amount RON Validation
            val amountRonRaw = getVal(tokens, "amount_ron", "amountron", "ron")
            val amountRON = amountRonRaw?.toDoubleOrNull()
            if (amountRON == null || amountRON <= 0.0) {
                rowErrors.add(CsvRowValidationError(rowNum, "Amount_RON", "Amount RON must be strictly > 0 (got '$amountRonRaw')"))
                rowHasErrors = true
            }

            // 3. Type Validation
            val typeRaw = getVal(tokens, "type")?.lowercase()
            val normType = when (typeRaw) {
                "income" -> "Income"
                "expense" -> "Expense"
                else -> null
            }
            if (normType == null) {
                rowErrors.add(CsvRowValidationError(rowNum, "Type", "Type must be Income or Expense (got '${getVal(tokens, "type")}')"))
                rowHasErrors = true
            }

            // 4. Account Validation
            val accountRaw = getVal(tokens, "account")?.lowercase()
            val normAccount = when (accountRaw) {
                "card" -> "Card"
                "cash" -> "Cash"
                "meal tickets", "mealtickets", "meal_tickets" -> "Meal Tickets"
                else -> null
            }
            if (normAccount == null) {
                rowErrors.add(CsvRowValidationError(rowNum, "Account", "Account must be Card, Cash, or Meal Tickets (got '${getVal(tokens, "account")}')"))
                rowHasErrors = true
            }

            // 5. Category & Subcategory Validation
            val categoryRaw = getVal(tokens, "category")?.trim()
            val subCategoryRaw = getVal(tokens, "subcategory", "sub_category")?.trim()

            if (categoryRaw.isNullOrBlank()) {
                rowErrors.add(CsvRowValidationError(rowNum, "Category", "Category is required"))
                rowHasErrors = true
            }
            if (subCategoryRaw.isNullOrBlank()) {
                rowErrors.add(CsvRowValidationError(rowNum, "Subcategory", "Subcategory is required"))
                rowHasErrors = true
            }

            if (!categoryRaw.isNullOrBlank() && !subCategoryRaw.isNullOrBlank() && normType != null) {
                // Check if subcategory already belongs to a DIFFERENT category or type in DB
                val conflictingCat = existingCategories.find {
                    it.subCategory.equals(subCategoryRaw, ignoreCase = true) &&
                    (!it.name.equals(categoryRaw, ignoreCase = true) || !it.type.equals(normType, ignoreCase = true))
                }
                if (conflictingCat != null) {
                    rowErrors.add(
                        CsvRowValidationError(
                            rowNum,
                            "Subcategory",
                            "Subcategory '$subCategoryRaw' belongs to category '${conflictingCat.name}' (${conflictingCat.type}), not '$categoryRaw' ($normType)"
                        )
                    )
                    rowHasErrors = true
                } else {
                    val categoryExistsInDb = existingCategories.any {
                        it.name.equals(categoryRaw, ignoreCase = true) &&
                        it.subCategory.equals(subCategoryRaw, ignoreCase = true) &&
                        it.type.equals(normType, ignoreCase = true)
                    }
                    if (!categoryExistsInDb) {
                        missingCategoriesSet.add(
                            MissingCategoryItem(
                                name = categoryRaw,
                                subCategory = subCategoryRaw,
                                type = normType
                            )
                        )
                    }
                }
            }

            // 6. Destination Validation
            val destinationRaw = getVal(tokens, "destination")?.trim()
            var normDestination: String? = null
            if (normType == "Expense") {
                if (!destinationRaw.isNullOrBlank()) {
                    rowErrors.add(CsvRowValidationError(rowNum, "Destination", "Expense transactions must not have a destination (got '$destinationRaw')"))
                    rowHasErrors = true
                }
            } else if (normType == "Income") {
                if (!destinationRaw.isNullOrBlank()) {
                    val destMatch = when (destinationRaw.lowercase()) {
                        "bubu" -> "Bubu"
                        "piticania" -> "Piticania"
                        else -> null
                    }
                    if (destMatch == null) {
                        rowErrors.add(CsvRowValidationError(rowNum, "Destination", "Income destination must be Bubu, Piticania, or blank (got '$destinationRaw')"))
                        rowHasErrors = true
                    } else {
                        normDestination = destMatch
                    }
                }
            }

            if (rowHasErrors) continue

            // 7. Exchange Rate Source, Status & EUR Validation
            val descRaw = getVal(tokens, "description", "desc") ?: ""
            val rawTxId = getVal(tokens, "transaction_id", "id")?.trim()
            val id = if (!rawTxId.isNullOrBlank()) rawTxId else UUID.randomUUID().toString()

            val rawSource = getVal(tokens, "exchange_rate_source", "rate_source", "source")?.trim()?.ifBlank { "UNVERIFIED" } ?: "UNVERIFIED"
            val rawStatus = getVal(tokens, "conversion_status", "status")?.trim()?.ifBlank { "UNVERIFIED" } ?: "UNVERIFIED"
            val rawRate = getVal(tokens, "exchange_rate", "exchangerate", "rate")?.toDoubleOrNull() ?: 0.0
            val rawAmountEur = getVal(tokens, "amount_eur", "amounteur", "eur")?.toDoubleOrNull() ?: 0.0
            val rawRateDate = getVal(tokens, "effective_bnr_rate_date", "exchange_rate_date", "rate_date")?.trim() ?: dateRaw!!

            val rateDateParsed: LocalDate? = try {
                LocalDate.parse(rawRateDate)
            } catch (e: Exception) {
                null
            }

            val isOfficialSource = rawSource == "BNR_OFFICIAL"
            val isOfficialStatus = rawStatus == "OFFICIAL"
            val isRateValid = rawRate > 0.0
            val isDatesValid = txDate != null && rateDateParsed != null
            val isDateOrderValid = isDatesValid && !rateDateParsed!!.isAfter(txDate!!)
            val expectedEur = if (isRateValid && amountRON != null) amountRON / rawRate else 0.0
            val isEurMatching = abs(rawAmountEur - expectedEur) <= 0.015

            var finalConversionStatus: String
            var finalExchangeRateSource: String
            var finalExchangeRate: Double
            var finalAmountEur: Double
            var finalExchangeRateDate: String

            if (isOfficialSource && isOfficialStatus && isRateValid && isDatesValid && isDateOrderValid && isEurMatching) {
                finalConversionStatus = "OFFICIAL"
                finalExchangeRateSource = "BNR_OFFICIAL"
                finalExchangeRate = rawRate
                finalAmountEur = rawAmountEur
                finalExchangeRateDate = rawRateDate
                officialCount++
            } else if (isRateValid) {
                finalConversionStatus = "UNVERIFIED"
                finalExchangeRateSource = if (rawSource != "NONE") rawSource else "UNVERIFIED"
                finalExchangeRate = rawRate
                finalAmountEur = if (rawAmountEur > 0.0) rawAmountEur else expectedEur
                finalExchangeRateDate = if (rateDateParsed != null) rawRateDate else dateRaw!!
                unverifiedCount++
            } else {
                finalConversionStatus = "PENDING"
                finalExchangeRateSource = "NONE"
                finalExchangeRate = 0.0
                finalAmountEur = 0.0
                finalExchangeRateDate = dateRaw!!
                pendingCount++
            }

            val isExisting = existingIds.contains(id)
            if (isExisting) {
                existingIdsCount++
            } else {
                newIdsCount++
            }

            if (normType == "Income" && amountRON != null) {
                totalRonIncome += amountRON
            } else if (normType == "Expense" && amountRON != null) {
                totalRonExpense += amountRON
            }

            val existingTx = existingTransactions.find { it.id == id }
            val effectiveUserId = if (existingTx != null && existingTx.userId.isNotBlank() && existingTx.userId != "local_user") {
                existingTx.userId
            } else if (userId.isNotBlank()) {
                userId
            } else {
                "local_user"
            }
            val effectiveHouseholdId = existingTx?.householdId ?: householdId
            val effectiveCreatedByUid = existingTx?.createdByUid
                ?: createdByUid?.takeIf { it.isNotBlank() }
                ?: if (effectiveUserId != "local_user") effectiveUserId else null

            val now = System.currentTimeMillis()
            val tx = TransactionEntity(
                id = id,
                date = dateRaw!!,
                description = descRaw,
                amountRON = amountRON!!,
                amountEUR = finalAmountEur,
                exchangeRate = finalExchangeRate,
                exchangeRateDate = finalExchangeRateDate,
                type = normType!!,
                account = normAccount!!,
                category = categoryRaw!!,
                subCategory = subCategoryRaw!!,
                destination = normDestination,
                exchangeRateSource = finalExchangeRateSource,
                conversionStatus = finalConversionStatus,
                userId = effectiveUserId,
                householdId = effectiveHouseholdId,
                createdByUid = effectiveCreatedByUid,
                createdAt = existingTx?.createdAt ?: now,
                updatedAt = now
            )

            validTransactions.add(tx)
        }

        val validRowsCount = validTransactions.size
        val invalidRowsCount = rowErrors.size
        val proposedUpdatesCount = if (duplicateMode == CsvDuplicateMode.UPDATE_EXISTING) existingIdsCount else 0
        val proposedSkipsCount = if (duplicateMode == CsvDuplicateMode.SKIP_EXISTING) existingIdsCount else 0

        return CsvPreviewData(
            totalRows = totalDataRows,
            validRowsCount = validRowsCount,
            invalidRowsCount = invalidRowsCount,
            newIdsCount = newIdsCount,
            existingIdsCount = existingIdsCount,
            proposedUpdatesCount = proposedUpdatesCount,
            proposedSkipsCount = proposedSkipsCount,
            totalRonIncome = totalRonIncome,
            totalRonExpense = totalRonExpense,
            officialCount = officialCount,
            unverifiedCount = unverifiedCount,
            pendingCount = pendingCount,
            missingCategories = missingCategoriesSet.toList(),
            rowErrors = rowErrors,
            validTransactionsToImport = validTransactions,
            duplicateMode = duplicateMode,
            rawCsvContent = csvContent
        )
    }

    suspend fun executeAtomicImport(
        database: FinTrackDatabase,
        previewData: CsvPreviewData,
        backupFile: File,
        allExistingTransactions: List<TransactionEntity>,
        householdId: String? = null,
        userId: String = "local_user",
        createdByUid: String? = null
    ): CsvImportFinalResult {
        // 1. OUTSIDE TRANSACTION: Create and Validate Backup
        val backupResult = CsvBackupManager.createAndValidateBackup(
            backupFile = backupFile,
            existingTransactions = allExistingTransactions
        )

        if (!backupResult.isValid) {
            return CsvImportFinalResult(
                success = false,
                insertedCount = 0,
                updatedCount = 0,
                skippedCount = 0,
                failedCount = previewData.validTransactionsToImport.size,
                categoriesCreatedCount = 0,
                subcategoriesCreatedCount = 0,
                pendingCount = 0,
                unverifiedCount = 0,
                backupFilePath = null,
                errorMessage = "Backup failure: ${backupResult.errorMessage}. Zero database writes were made."
            )
        }

        val effectiveHouseholdId = householdId ?: previewData.validTransactionsToImport.firstOrNull { it.householdId != null }?.householdId
        val effectiveUserId = if (userId.isNotBlank() && userId != "local_user") userId else (previewData.validTransactionsToImport.firstOrNull { it.userId.isNotBlank() && it.userId != "local_user" }?.userId ?: "local_user")
        val effectiveCreatedByUid = createdByUid?.takeIf { it.isNotBlank() }
            ?: previewData.validTransactionsToImport.firstOrNull { !it.createdByUid.isNullOrBlank() }?.createdByUid
            ?: if (effectiveUserId != "local_user") effectiveUserId else null

        val existingTxMap = allExistingTransactions.associateBy { it.id }
        val txsToInsert = mutableListOf<TransactionEntity>()
        val txsToUpdate = mutableListOf<TransactionEntity>()
        var skippedCount = 0

        val now = System.currentTimeMillis()
        for (tx in previewData.validTransactionsToImport) {
            val existingTx = existingTxMap[tx.id]
            val finalTx = if (existingTx != null) {
                tx.copy(
                    createdAt = existingTx.createdAt,
                    createdByUid = existingTx.createdByUid ?: tx.createdByUid ?: effectiveCreatedByUid,
                    householdId = existingTx.householdId ?: tx.householdId ?: effectiveHouseholdId,
                    userId = if (existingTx.userId.isNotBlank() && existingTx.userId != "local_user") existingTx.userId else (if (tx.userId.isNotBlank() && tx.userId != "local_user") tx.userId else effectiveUserId),
                    updatedAt = now
                )
            } else {
                tx.copy(
                    householdId = tx.householdId ?: effectiveHouseholdId,
                    userId = if (tx.userId.isNotBlank() && tx.userId != "local_user") tx.userId else effectiveUserId,
                    createdByUid = tx.createdByUid ?: effectiveCreatedByUid,
                    createdAt = if (tx.createdAt > 0L) tx.createdAt else now,
                    updatedAt = now
                )
            }

            if (existingTx != null) {
                if (previewData.duplicateMode == CsvDuplicateMode.UPDATE_EXISTING) {
                    txsToUpdate.add(finalTx)
                } else {
                    skippedCount++
                }
            } else {
                txsToInsert.add(finalTx)
            }
        }

        val categoryEntities = previewData.missingCategories.map {
            CategoryEntity(
                id = UUID.randomUUID().toString(),
                name = it.name,
                type = it.type,
                subCategory = it.subCategory,
                userId = effectiveUserId,
                householdId = effectiveHouseholdId,
                createdByUid = effectiveCreatedByUid
            )
        }

        var pendingCount = 0
        var unverifiedCount = 0
        (txsToInsert + txsToUpdate).forEach { tx ->
            if (tx.conversionStatus == "PENDING") pendingCount++
            if (tx.conversionStatus == "UNVERIFIED") unverifiedCount++
        }

        // 2. ATOMIC DATABASE TRANSACTION
        return try {
            database.withTransaction {
                val now = System.currentTimeMillis()
                if (categoryEntities.isNotEmpty()) {
                    database.categoryDao().insertAllCategories(categoryEntities)
                    val catOutbox = categoryEntities.map { cat ->
                        SyncOutboxEntity(
                            entityType = "CATEGORY",
                            entityId = cat.id,
                            operation = "UPSERT",
                            status = "PENDING",
                            createdAt = now,
                            updatedAt = now
                        )
                    }
                    database.syncOutboxDao().insertAllOutboxEntries(catOutbox)
                }
                if (txsToInsert.isNotEmpty()) {
                    database.transactionDao().insertAllTransactions(txsToInsert)
                    val txInsertOutbox = txsToInsert.map { tx ->
                        SyncOutboxEntity(
                            entityType = "TRANSACTION",
                            entityId = tx.id,
                            operation = "UPSERT",
                            status = "PENDING",
                            createdAt = now,
                            updatedAt = now
                        )
                    }
                    database.syncOutboxDao().insertAllOutboxEntries(txInsertOutbox)
                }
                if (txsToUpdate.isNotEmpty()) {
                    database.transactionDao().insertAllTransactions(txsToUpdate)
                    val txUpdateOutbox = txsToUpdate.map { tx ->
                        SyncOutboxEntity(
                            entityType = "TRANSACTION",
                            entityId = tx.id,
                            operation = "UPSERT",
                            status = "PENDING",
                            createdAt = now,
                            updatedAt = now
                        )
                    }
                    database.syncOutboxDao().insertAllOutboxEntries(txUpdateOutbox)
                }
            }

            CsvImportFinalResult(
                success = true,
                insertedCount = txsToInsert.size,
                updatedCount = txsToUpdate.size,
                skippedCount = skippedCount,
                failedCount = previewData.invalidRowsCount,
                categoriesCreatedCount = previewData.missingCategories.map { it.name }.distinct().size,
                subcategoriesCreatedCount = previewData.missingCategories.size,
                pendingCount = pendingCount,
                unverifiedCount = unverifiedCount,
                backupFilePath = backupFile.absolutePath,
                errorMessage = null
            )
        } catch (e: Exception) {
            CsvImportFinalResult(
                success = false,
                insertedCount = 0,
                updatedCount = 0,
                skippedCount = 0,
                failedCount = previewData.validTransactionsToImport.size + previewData.invalidRowsCount,
                categoriesCreatedCount = 0,
                subcategoriesCreatedCount = 0,
                pendingCount = 0,
                unverifiedCount = 0,
                backupFilePath = backupFile.absolutePath,
                errorMessage = "Database transaction failed: ${e.message}. Rolled back all writes."
            )
        }
    }
}
