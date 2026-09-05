package com.example

import com.example.data.model.CategoryDto
import com.example.data.model.CategoryEntity
import com.example.data.model.ExchangeRateDto
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.TransactionDto
import com.example.data.model.TransactionEntity
import com.example.data.model.toFirestoreMap
import com.example.data.repository.FirestoreSnapshotSource
import com.example.data.repository.HouseholdResolutionResult
import com.example.data.repository.ListenerRegistrationHandle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RecordingSnapshotSource : FirestoreSnapshotSource {
    val transactionUpserts = mutableListOf<Pair<String, Pair<String, Map<String, Any?>>>>()
    val transactionDeletes = mutableListOf<Pair<String, String>>()
    val categoryUpserts = mutableListOf<Pair<String, Pair<String, Map<String, Any?>>>>()
    val categoryDeletes = mutableListOf<Pair<String, String>>()
    val exchangeRateUpserts = mutableListOf<Pair<String, Pair<String, Map<String, Any?>>>>()
    val exchangeRateDeletes = mutableListOf<Pair<String, String>>()

    var shouldThrowException: Exception? = null

    override fun listenToTransactions(
        householdId: String,
        onSnapshot: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistrationHandle = object : ListenerRegistrationHandle { override fun remove() {} }

    override fun listenToCategories(
        householdId: String,
        onSnapshot: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistrationHandle = object : ListenerRegistrationHandle { override fun remove() {} }

    override suspend fun resolveHouseholdId(userUid: String): HouseholdResolutionResult {
        return HouseholdResolutionResult.Success("hh_alpha")
    }

    override suspend fun upsertTransaction(householdId: String, transactionId: String, data: Map<String, Any?>) {
        shouldThrowException?.let { throw it }
        transactionUpserts.add(Pair(householdId, Pair(transactionId, data)))
    }

    override suspend fun deleteTransaction(householdId: String, transactionId: String) {
        shouldThrowException?.let { throw it }
        transactionDeletes.add(Pair(householdId, transactionId))
    }

    override suspend fun upsertCategory(householdId: String, categoryId: String, data: Map<String, Any?>) {
        shouldThrowException?.let { throw it }
        categoryUpserts.add(Pair(householdId, Pair(categoryId, data)))
    }

    override suspend fun deleteCategory(householdId: String, categoryId: String) {
        shouldThrowException?.let { throw it }
        categoryDeletes.add(Pair(householdId, categoryId))
    }

    override suspend fun upsertExchangeRate(householdId: String, exchangeRateId: String, data: Map<String, Any?>) {
        shouldThrowException?.let { throw it }
        exchangeRateUpserts.add(Pair(householdId, Pair(exchangeRateId, data)))
    }

    override suspend fun deleteExchangeRate(householdId: String, exchangeRateId: String) {
        shouldThrowException?.let { throw it }
        exchangeRateDeletes.add(Pair(householdId, exchangeRateId))
    }
}

class FirestoreOutboundTransportTest {

    @Test
    fun testTransactionUpsertSerializationAndTransport() = runTest {
        val source = RecordingSnapshotSource()
        val tx = TransactionEntity(
            id = "tx_001",
            userId = "user_bubu",
            date = "2026-08-15",
            description = "Groceries",
            amountRON = 245.50,
            amountEUR = 49.10,
            exchangeRate = 5.0000,
            exchangeRateDate = "2026-08-14",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "Market",
            destination = null,
            createdAt = 1700000000000L,
            updatedAt = 1700001000000L,
            exchangeRateSource = "BNR_OFFICIAL",
            conversionStatus = "OFFICIAL"
        )

        val payload = tx.toFirestoreMap("hh_123")
        assertEquals("tx_001", payload["transactionId"])
        assertEquals("hh_123", payload["householdId"])
        assertEquals("user_bubu", payload["createdByUid"])
        assertEquals(245.50, payload["amountRon"])
        assertEquals(49.10, payload["amountEur"])
        assertEquals("Expense", payload["type"])
        assertEquals("Card", payload["account"])
        assertEquals(false, payload["isDeleted"])

        source.upsertTransaction("hh_123", tx.id, payload)
        assertEquals(1, source.transactionUpserts.size)
        val recorded = source.transactionUpserts.first()
        assertEquals("hh_123", recorded.first)
        assertEquals("tx_001", recorded.second.first)
        assertEquals(payload, recorded.second.second)
    }

    @Test
    fun testTransactionDeletePathTransport() = runTest {
        val source = RecordingSnapshotSource()
        source.deleteTransaction("hh_123", "tx_001")
        assertEquals(1, source.transactionDeletes.size)
        assertEquals(Pair("hh_123", "tx_001"), source.transactionDeletes.first())
    }

    @Test
    fun testCategoryUpsertSerializationAndTransport() = runTest {
        val source = RecordingSnapshotSource()
        val cat = CategoryEntity(
            id = "cat_001",
            name = "Salary",
            type = "Income",
            subCategory = "Primary",
            userId = "user_admin",
            createdAt = 1700000000000L,
            updatedAt = 1700001000000L,
            isDeleted = false
        )

        val payload = cat.toFirestoreMap("hh_123")
        assertEquals("cat_001", payload["categoryId"])
        assertEquals("hh_123", payload["householdId"])
        assertEquals("Salary", payload["name"])
        assertEquals("Income", payload["type"])
        assertEquals("Primary", payload["subCategory"])
        assertEquals("user_admin", payload["createdByUid"])
        assertFalse(payload["isDeleted"] as Boolean)

        source.upsertCategory("hh_123", cat.id, payload)
        assertEquals(1, source.categoryUpserts.size)
        val recorded = source.categoryUpserts.first()
        assertEquals("hh_123", recorded.first)
        assertEquals("cat_001", recorded.second.first)
        assertEquals(payload, recorded.second.second)
    }

    @Test
    fun testCategoryDeletePathTransport() = runTest {
        val source = RecordingSnapshotSource()
        source.deleteCategory("hh_123", "cat_001")
        assertEquals(1, source.categoryDeletes.size)
        assertEquals(Pair("hh_123", "cat_001"), source.categoryDeletes.first())
    }

    @Test
    fun testExchangeRateUpsertSerializationAndTransport() = runTest {
        val source = RecordingSnapshotSource()
        val rate = ExchangeRateEntity(
            date = "2026-08-15",
            requestedDate = "2026-08-15",
            effectiveDate = "2026-08-14",
            rate = 4.9750,
            source = "BNR_OFFICIAL",
            fetchedAt = 1700000000000L,
            status = "OFFICIAL"
        )

        val payload = rate.toFirestoreMap("hh_123")
        assertEquals("2026-08-15", payload["requestedDate"])
        assertEquals("2026-08-14", payload["effectiveDate"])
        assertEquals(4.9750, payload["rate"])
        assertEquals("BNR_OFFICIAL", payload["source"])
        assertEquals("OFFICIAL", payload["status"])
        assertEquals("hh_123", payload["householdId"])
        @Suppress("UNCHECKED_CAST")
        val ratesMap = payload["rates"] as Map<String, Double>
        assertEquals(4.9750, ratesMap["EUR"] ?: 0.0, 0.0001)

        source.upsertExchangeRate("hh_123", rate.date, payload)
        assertEquals(1, source.exchangeRateUpserts.size)
        val recorded = source.exchangeRateUpserts.first()
        assertEquals("hh_123", recorded.first)
        assertEquals("2026-08-15", recorded.second.first)
        assertEquals(payload, recorded.second.second)
    }

    @Test
    fun testExchangeRateDeletePathTransport() = runTest {
        val source = RecordingSnapshotSource()
        source.deleteExchangeRate("hh_123", "2026-08-15")
        assertEquals(1, source.exchangeRateDeletes.size)
        assertEquals(Pair("hh_123", "2026-08-15"), source.exchangeRateDeletes.first())
    }

    @Test
    fun testErrorPropagationDoesNotSwallowExceptions() = runTest {
        val source = RecordingSnapshotSource()
        source.shouldThrowException = SecurityException("PERMISSION_DENIED: User not in household")

        try {
            source.upsertTransaction("hh_123", "tx_001", emptyMap())
            fail("Expected SecurityException to be thrown")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("PERMISSION_DENIED"))
        }

        try {
            source.deleteCategory("hh_123", "cat_001")
            fail("Expected SecurityException to be thrown")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("PERMISSION_DENIED"))
        }
    }

    @Test
    fun testIdempotentRepeatedExecutionProducesIdenticalState() = runTest {
        val source = RecordingSnapshotSource()
        val tx = TransactionEntity(
            id = "tx_idem_001",
            userId = "user_bubu",
            date = "2026-08-15",
            description = "Subscription",
            amountRON = 50.0,
            amountEUR = 10.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-15",
            type = "Expense",
            account = "Card",
            category = "Utilities",
            subCategory = "",
            createdAt = 1700000000000L,
            updatedAt = 1700001000000L
        )

        val payload1 = tx.toFirestoreMap("hh_123")
        val payload2 = tx.toFirestoreMap("hh_123")

        // 10 repeated upserts
        for (i in 1..10) {
            source.upsertTransaction("hh_123", tx.id, payload1)
        }

        assertEquals(10, source.transactionUpserts.size)
        // All recorded payloads must be identical
        for (recorded in source.transactionUpserts) {
            assertEquals("hh_123", recorded.first)
            assertEquals("tx_idem_001", recorded.second.first)
            assertEquals(payload1, recorded.second.second)
            assertEquals(payload2, recorded.second.second)
        }
    }

    @Test
    fun testTransactionAndCategoryHardDeleteOutboundSemantics() = runTest {
        val source = RecordingSnapshotSource()

        // Verify transaction delete records householdId and transactionId for hard delete
        source.deleteTransaction("hh_household_99", "tx_del_99")
        assertEquals(1, source.transactionDeletes.size)
        assertEquals("hh_household_99", source.transactionDeletes.first().first)
        assertEquals("tx_del_99", source.transactionDeletes.first().second)

        // Verify category delete records householdId and categoryId for hard delete
        source.deleteCategory("hh_household_99", "cat_del_99")
        assertEquals(1, source.categoryDeletes.size)
        assertEquals("hh_household_99", source.categoryDeletes.first().first)
        assertEquals("cat_del_99", source.categoryDeletes.first().second)
    }
}
