package com.example.data.util

import com.example.data.dao.ExchangeRateDao
import com.example.data.repository.CategoryRepository
import com.example.data.repository.PreflightValidationResult
import com.example.data.repository.TransactionRepository
import com.example.ui.MigrationPreviewState
import kotlinx.coroutines.flow.first
import java.io.File

sealed interface PreflightBackupResult {
    data class Success(val backupBundleDir: File) : PreflightBackupResult
    data class Failure(val errorMessage: String) : PreflightBackupResult
}

class MigrationPreflightHelper(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val exchangeRateDao: ExchangeRateDao? = null,
    private val backupManager: CsvBackupManager = CsvBackupManager
) {

    suspend fun createPreflightBackup(filesDir: File): PreflightBackupResult {
        val backupsRootDir = File(filesDir, "migration_backups").apply { mkdirs() }
        val newBundleDir = File(backupsRootDir, "backup_${System.currentTimeMillis()}")

        val allTxs = transactionRepository.allTransactions.first()
        val allCats = categoryRepository.getAllCategoriesList()
        val allRates = exchangeRateDao?.getAllOfficialRates() ?: emptyList()

        val backupCreationResult = backupManager.createMigrationBackupBundle(
            bundleDir = newBundleDir,
            transactions = allTxs,
            categories = allCats,
            exchangeRates = allRates
        )

        return if (backupCreationResult.isValid) {
            PreflightBackupResult.Success(newBundleDir)
        } else {
            PreflightBackupResult.Failure(
                backupCreationResult.errorMessage ?: "Failed to generate mandatory preflight backup bundle."
            )
        }
    }

    fun extractManifestTimestamp(backupBundleDir: File): Long {
        return try {
            val manifestFile = File(backupBundleDir, CsvBackupManager.MANIFEST_FILE_NAME)
            if (manifestFile.exists()) {
                MigrationManifest.fromJson(manifestFile.readText())?.creationTimestamp
            } else null
        } catch (_: Exception) {
            null
        } ?: System.currentTimeMillis()
    }

    fun mapToPreviewState(
        result: PreflightValidationResult.Ready,
        resolvedHouseholdName: String,
        effectiveBackupDir: File
    ): MigrationPreviewState {
        val manifestTimestamp = extractManifestTimestamp(effectiveBackupDir)
        return MigrationPreviewState(
            householdId = result.householdId,
            householdName = resolvedHouseholdName,
            userUid = result.userUid,
            userRole = result.memberInfo.role,
            transactionsCount = result.localCounts.transactionsCount,
            categoriesCount = result.localCounts.categoriesCount,
            exchangeRatesCount = result.localCounts.exchangeRatesCount,
            totalRecords = result.localCounts.totalCount,
            backupBundlePath = result.backupBundlePath,
            backupTimestamp = manifestTimestamp,
            backupValidationStatus = "VALIDATED",
            preflightReadyData = result
        )
    }

    fun sanitizeError(rawError: String?): String {
        if (rawError.isNullOrBlank()) return "An unexpected error occurred during migration."
        val clean = rawError.lines()
            .map { it.replace(Regex("at [a-zA-Z0-9_$.]+\\(.*\\)"), "").trim() }
            .filter { it.isNotBlank() && !it.startsWith("java.") && !it.startsWith("kotlin.") && !it.startsWith("android.") }
            .firstOrNull() ?: "An unexpected error occurred during migration."
        return clean.take(150)
    }
}
