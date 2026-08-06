package com.example.data.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object NumberFormatter {
    fun formatAmount(amount: Double): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ' '
            decimalSeparator = '.'
        }
        val df = DecimalFormat("#,##0.0#", symbols)
        return df.format(amount)
    }

    fun formatCurrency(amount: Double, currency: String): String {
        return "${formatAmount(amount)} $currency"
    }
}
