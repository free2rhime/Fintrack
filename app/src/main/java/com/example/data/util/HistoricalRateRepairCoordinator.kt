package com.example.data.util

import com.example.data.repository.PreparedRepairItem
import com.example.data.repository.TransactionRepository
import com.example.data.service.ExchangeRateService
import com.example.ui.DiscrepancyItem
import com.example.ui.DiscrepancyReport
import java.io.File

sealed interface RepairExecutionResult {
    data class Success(val updatedCount: Int) : RepairExecutionResult
    data class ValidationFailed(val message: String = "CSV backup validation failed.") : RepairExecutionResult
    data class Failure(val message: String) : RepairExecutionResult
}

class HistoricalRateRepairCoordinator(
    private val transactionRepository: TransactionRepository
) {
    suspend fun generateDiscrepancyReport(cacheDir: File): DiscrepancyReport {
        val unverified = transactionRepository.getUnverifiedTransactions()
        val items = mutableListOf<DiscrepancyItem>()

        for (tx in unverified) {
            val bnrResult = transactionRepository.getOfficialRate(tx.date)
            if (bnrResult.status == "OFFICIAL" && bnrResult.rate > 0.0) {
                val correctEUR = ExchangeRateService.calculateAmountEUR(tx.amountRON, bnrResult.rate)
                val diff = kotlin.math.abs(correctEUR - tx.amountEUR)
                items.add(
                    DiscrepancyItem(
                        transactionId = tx.id,
                        date = tx.date,
                        description = tx.description,
                        amountRON = tx.amountRON,
                        oldRate = tx.exchangeRate,
                        correctRate = bnrResult.rate,
                        effectiveBnrDate = bnrResult.effectiveDate,
                        oldAmountEUR = tx.amountEUR,
                        correctAmountEUR = correctEUR,
                        differenceEUR = Math.round(diff * 100.0) / 100.0
                    )
                )
            }
        }

        // Create backup CSV before applying corrections
        val allTxs = transactionRepository.getAllTransactionsList()
        val backupFile = File(cacheDir, "fintrack_backup_before_repair.csv")
        CsvExporter.writeTransactionsToFile(backupFile, allTxs)

        val totalDiff = items.sumOf { it.differenceEUR }
        return DiscrepancyReport(
            items = items,
            totalDiscrepancyEUR = Math.round(totalDiff * 100.0) / 100.0,
            backupFilePath = backupFile.absolutePath
        )
    }

    suspend fun confirmAndApplyRepair(report: DiscrepancyReport, cacheDir: File): RepairExecutionResult {
        return try {
            val allTxs = transactionRepository.getAllTransactionsList()
            val backupFile = File(cacheDir, "fintrack_backup_before_repair.csv")

            CsvExporter.writeTransactionsToFile(backupFile, allTxs)

            // Perform strict 6-point backup validation
            if (!validateBackupFile(backupFile, allTxs.size)) {
                return RepairExecutionResult.ValidationFailed("CSV backup validation failed.")
            }

            val preparedItems = mutableListOf<PreparedRepairItem>()
            for (item in report.items) {
                val tx = transactionRepository.getTransactionById(item.transactionId) ?: continue
                preparedItems.add(PreparedRepairItem(tx, item.correctRate, item.effectiveBnrDate))
            }

            val updatedCount = transactionRepository.applyRepairBatch(preparedItems)
            RepairExecutionResult.Success(updatedCount)
        } catch (e: Exception) {
            RepairExecutionResult.Failure(e.message ?: "Unknown error applying repair")
        }
    }

    fun validateBackupFile(file: File, expectedCount: Int): Boolean {
        try {
            if (!file.exists()) return false
            if (!file.canRead()) return false
            if (file.length() <= 0) return false

            val lines = file.readLines()
            if (lines.isEmpty()) return false

            val header = lines.first()
            if (!header.startsWith("Transaction_ID,Transaction_Date,Amount_RON,Amount_EUR")) return false

            val dataRows = lines.drop(1).filter { it.isNotBlank() }
            if (dataRows.size < expectedCount) return false

            return true
        } catch (e: Exception) {
            return false
        }
    }
}
