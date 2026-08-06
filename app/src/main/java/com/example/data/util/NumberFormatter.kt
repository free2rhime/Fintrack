package com.example.data.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToLong

object NumberFormatter {
    fun formatAmount(amount: Double): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ' '
            decimalSeparator = '.'
        }
        val df = DecimalFormat("#,##0", symbols)
        return df.format(amount.roundToLong())
    }

    fun formatCurrency(amount: Double, currency: String): String {
        return "${formatAmount(amount)} $currency"
    }
}
