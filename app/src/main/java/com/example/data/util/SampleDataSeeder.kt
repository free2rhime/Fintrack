package com.example.data.util

import com.example.data.model.TransactionEntity
import com.example.data.repository.TransactionRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

object SampleDataSeeder {

    suspend fun seedInitialTransactionsIfEmpty(
        repository: TransactionRepository,
        userId: String = "local_user"
    ) {
        val todayCal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val sampleTemplates = listOf(
            // Recent Month
            Triple("Tech Corp Salary", 7800.0, "Income" to ("Salary" to "Main Job")),
            Triple("Lidl Groceries", 345.50, "Expense" to ("Food & Dining" to "Groceries")),
            Triple("Mega Image Market", 128.90, "Expense" to ("Food & Dining" to "Groceries")),
            Triple("Uber Ride to Office", 38.00, "Expense" to ("Transportation" to "Public Transit & Rides")),
            Triple("Starbucks Coffee", 26.00, "Expense" to ("Food & Dining" to "Restaurants & Cafes")),
            Triple("OMV Fuel Station", 280.00, "Expense" to ("Transportation" to "Fuel")),
            Triple("Apartment Rent", 2200.00, "Expense" to ("Housing & Utilities" to "Rent / Mortgage")),
            Triple("Digi Fiber Internet & TV", 85.00, "Expense" to ("Housing & Utilities" to "Utilities & Internet")),
            Triple("Meal Tickets Allowance", 600.00, "Income" to ("Salary" to "Bonus")),
            Triple("Netflix Subscription", 55.00, "Expense" to ("Entertainment & Leisure" to "Subscriptions")),
            Triple("WorldClass Gym Pass", 220.00, "Expense" to ("Health & Wellness" to "Gym & Fitness")),
            Triple("Freelance Web Project", 3200.00, "Income" to ("Freelance & Consulting" to "Software")),
            Triple("Pharmacy Medicine", 94.00, "Expense" to ("Health & Wellness" to "Pharmacy & Medical")),
            Triple("EMAG Electronics", 450.00, "Expense" to ("Shopping & Personal" to "Electronics"))
        )

        // Seed across current month and past 2 months
        for (monthOffset in 0 downTo -2) {
            val monthCal = Calendar.getInstance().apply {
                add(Calendar.MONTH, monthOffset)
            }

            for ((index, item) in sampleTemplates.withIndex()) {
                val day = ((index * 2) % 25) + 1
                monthCal.set(Calendar.DAY_OF_MONTH, day)
                val dateStr = sdf.format(monthCal.time)

                val (desc, amt, typeCat) = item
                val (type, catSub) = typeCat
                val (cat, subCat) = catSub

                repository.saveTransaction(
                    id = UUID.randomUUID().toString(),
                    date = dateStr,
                    description = desc,
                    amountRON = amt,
                    type = type,
                    account = if (cat == "Meal Tickets") "Meal Tickets" else if (index % 3 == 0) "Cash" else "Card",
                    category = cat,
                    subCategory = subCat,
                    destination = if (type == "Income") "ING Bank Card" else null,
                    userId = userId
                )
            }
        }
    }
}
