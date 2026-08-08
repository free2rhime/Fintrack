package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.FilterSettings
import com.example.data.model.TransactionEntity
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.FinTrackTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun transactionsScreen_screenshot() {
    val sampleTx = TransactionEntity(
        id = "tx1",
        date = "2026-08-01",
        description = "Supermarket",
        amountRON = 150.0,
        amountEUR = 30.14,
        exchangeRate = 4.9765,
        exchangeRateDate = "2026-08-01",
        type = "Expense",
        account = "Checking",
        category = "Food",
        subCategory = "Groceries"
    )

    composeTestRule.setContent {
      FinTrackTheme {
        TransactionsScreen(
            transactions = listOf(sampleTx),
            categories = emptyList(),
            filterSettings = FilterSettings(),
            onPeriodSelected = {},
            onCurrencyChanged = {},
            onCategoryFilterSelected = { _, _ -> },
            onSearchQueryChanged = {},
            onAddTransactionClicked = {},
            onDuplicateClicked = {},
            onEditClicked = {},
            onDeleteClicked = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/transactions_screen.png")
  }
}
