package com.example.data.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.TransactionEntity
import java.io.File

object CsvExporter {
    fun exportTransactionsToCsv(context: Context, transactions: List<TransactionEntity>) {
        try {
            val csvHeader = "ID,Date,Description,Amount_RON,Amount_EUR,ExchangeRate,Type,Account,Category,SubCategory,Destination\n"
            val sb = StringBuilder(csvHeader)

            for (tx in transactions) {
                val line = listOf(
                    tx.id,
                    tx.date,
                    "\"${tx.description.replace("\"", "\"\"")}\"",
                    tx.amountRON,
                    tx.amountEUR,
                    tx.exchangeRate,
                    tx.type,
                    tx.account,
                    "\"${tx.category.replace("\"", "\"\"")}\"",
                    "\"${tx.subCategory.replace("\"", "\"\"")}\"",
                    "\"${(tx.destination ?: "").replace("\"", "\"\"")}\""
                ).joinToString(",")
                sb.append(line).append("\n")
            }

            val file = File(context.cacheDir, "fintrack_transactions.csv")
            file.writeText(sb.toString())

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "FinTrack Transactions Export")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Export Transactions CSV"))
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
