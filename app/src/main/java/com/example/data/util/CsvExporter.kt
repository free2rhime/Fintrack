package com.example.data.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.CategoryEntity
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.TransactionEntity
import java.io.File

object CsvExporter {

    fun generateCsvContent(transactions: List<TransactionEntity>): String {
        val csvHeader = "Transaction_ID,Transaction_Date,Amount_RON,Amount_EUR,Exchange_Rate,Requested_Rate_Date,Effective_BNR_Rate_Date,Exchange_Rate_Source,Conversion_Status,Description,Type,Account,Category,SubCategory,Destination\n"
        val sb = StringBuilder(csvHeader)

        for (tx in transactions) {
            val line = listOf(
                tx.id,
                tx.date,
                tx.amountRON,
                tx.amountEUR,
                tx.exchangeRate,
                tx.date, // Requested rate date
                tx.exchangeRateDate, // Effective BNR rate date
                tx.exchangeRateSource,
                tx.conversionStatus,
                "\"${tx.description.replace("\"", "\"\"")}\"",
                tx.type,
                tx.account,
                "\"${tx.category.replace("\"", "\"\"")}\"",
                "\"${tx.subCategory.replace("\"", "\"\"")}\"",
                "\"${(tx.destination ?: "").replace("\"", "\"\"")}\""
            ).joinToString(",")
            sb.append(line).append("\n")
        }
        return sb.toString()
    }

    fun writeTransactionsToFile(file: File, transactions: List<TransactionEntity>): File {
        file.writeText(generateCsvContent(transactions))
        return file
    }

    fun generateCategoriesCsvContent(categories: List<CategoryEntity>): String {
        val header = "Category_ID,Name,Type,SubCategory,User_ID,Created_At,Updated_At,Is_Deleted,Sync_Status\n"
        val sb = StringBuilder(header)
        for (cat in categories) {
            val line = listOf(
                cat.id,
                "\"${cat.name.replace("\"", "\"\"")}\"",
                cat.type,
                "\"${cat.subCategory.replace("\"", "\"\"")}\"",
                cat.userId,
                cat.createdAt,
                cat.updatedAt,
                cat.isDeleted,
                cat.syncStatus
            ).joinToString(",")
            sb.append(line).append("\n")
        }
        return sb.toString()
    }

    fun writeCategoriesToFile(file: File, categories: List<CategoryEntity>): File {
        file.writeText(generateCategoriesCsvContent(categories))
        return file
    }

    fun generateExchangeRatesCsvContent(exchangeRates: List<ExchangeRateEntity>): String {
        val header = "Rate_Date,Requested_Date,Effective_Date,Rate,Source,Fetched_At,Status\n"
        val sb = StringBuilder(header)
        for (rate in exchangeRates) {
            val line = listOf(
                rate.date,
                rate.requestedDate,
                rate.effectiveDate,
                rate.rate,
                rate.source,
                rate.fetchedAt,
                rate.status
            ).joinToString(",")
            sb.append(line).append("\n")
        }
        return sb.toString()
    }

    fun writeExchangeRatesToFile(file: File, exchangeRates: List<ExchangeRateEntity>): File {
        file.writeText(generateExchangeRatesCsvContent(exchangeRates))
        return file
    }

    fun exportTransactionsToCsv(context: Context, transactions: List<TransactionEntity>) {
        try {
            val file = File(context.cacheDir, "fintrack_transactions.csv")
            writeTransactionsToFile(file, transactions)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "FinTrack Audited Transactions Export")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Export Transactions CSV"))
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
