package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.FinTrackDatabase
import com.example.data.repository.HouseholdVerificationResult
import com.example.data.repository.FirestoreSyncRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Stage2APreparationTest {

    private lateinit var db: FinTrackDatabase
    private lateinit var fakeSnapshotSource: FakeSnapshotSource
    private lateinit var syncRepository: FirestoreSyncRepository
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        fakeSnapshotSource = FakeSnapshotSource()
        syncRepository = FirestoreSyncRepository(
            database = db,
            snapshotSource = fakeSnapshotSource,
            coroutineScope = testScope
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testListenerSuppressionCanBeQueriedAndToggledReversibly() {
        assertFalse(syncRepository.isSuppressed)

        syncRepository.enableSuppression()
        assertTrue(syncRepository.isSuppressed)

        syncRepository.disableSuppression()
        assertFalse(syncRepository.isSuppressed)

        syncRepository.setSuppression(true)
        assertTrue(syncRepository.isSuppressed)

        syncRepository.setSuppression(false)
        assertFalse(syncRepository.isSuppressed)
    }

    @Test
    fun testSuppressedSnapshotsDoNotChangeRoom() = testScope.runTest {
        syncRepository.startSync("user_101", "hh_101")
        assertTrue(syncRepository.isListening)
        assertEquals(2, syncRepository.listenerCount)

        syncRepository.enableSuppression()
        assertTrue(syncRepository.isSuppressed)

        val txDoc = Pair(
            "tx_suppressed_1",
            mapOf<String, Any?>(
                "transactionId" to "tx_suppressed_1",
                "householdId" to "hh_101",
                "type" to "Expense",
                "account" to "Card",
                "destination" to null,
                "category" to "Bills",
                "amountRon" to 85.0,
                "amountEur" to 17.0,
                "exchangeRate" to 5.0,
                "transactionDate" to "2026-08-14",
                "createdByUid" to "user_101",
                "createdAt" to 1723636800000L,
                "isDeleted" to false
            )
        )

        val catDoc = Pair(
            "cat_suppressed_1",
            mapOf<String, Any?>(
                "categoryId" to "cat_suppressed_1",
                "householdId" to "hh_101",
                "name" to "Suppressed Utilities",
                "type" to "Expense",
                "color" to 4283585125L,
                "icon" to "bolt",
                "isDeleted" to false
            )
        )

        fakeSnapshotSource.emitTransactions(listOf(txDoc))
        fakeSnapshotSource.emitCategories(listOf(catDoc))
        testScheduler.advanceUntilIdle()

        // Also test direct invocation of processTransactionSnapshot / processCategorySnapshot during suppression
        syncRepository.processTransactionSnapshot(listOf(txDoc))
        syncRepository.processCategorySnapshot(listOf(catDoc))

        assertNull(db.transactionDao().getTransactionById("tx_suppressed_1"))
        assertNull(db.categoryDao().getCategoryById("cat_suppressed_1"))
        assertEquals(0, db.transactionDao().getAllTransactionsList().size)

        // Verify listeners remained attached throughout suppression
        assertTrue(syncRepository.isListening)
        assertEquals(2, syncRepository.listenerCount)
    }

    @Test
    fun testUnsuppressedSnapshotsBehaveNormally() = testScope.runTest {
        syncRepository.startSync("user_101", "hh_101")

        // First suppress, emit, verify no write
        syncRepository.enableSuppression()
        val txDoc = Pair(
            "tx_normal_1",
            mapOf<String, Any?>(
                "transactionId" to "tx_normal_1",
                "householdId" to "hh_101",
                "type" to "Income",
                "account" to "Card",
                "destination" to null,
                "category" to "Salary",
                "amountRon" to 5000.0,
                "amountEur" to 1000.0,
                "exchangeRate" to 5.0,
                "transactionDate" to "2026-08-14",
                "createdByUid" to "user_101",
                "createdAt" to 1723636800000L,
                "isDeleted" to false
            )
        )
        fakeSnapshotSource.emitTransactions(listOf(txDoc))
        syncRepository.processTransactionSnapshot(listOf(txDoc))
        assertNull(db.transactionDao().getTransactionById("tx_normal_1"))

        // Unsuppress and process again
        syncRepository.disableSuppression()
        assertFalse(syncRepository.isSuppressed)

        fakeSnapshotSource.emitTransactions(listOf(txDoc))
        syncRepository.processTransactionSnapshot(listOf(txDoc))

        val savedTx = db.transactionDao().getTransactionById("tx_normal_1")
        assertTrue(savedTx != null)
        assertEquals(5000.0, savedTx!!.amountRON, 0.001)
    }

    @Test
    fun testOwnerPassesVerification() = testScope.runTest {
        fakeSnapshotSource.setMember("hh_alpha", "user_owner_1", "OWNER", "ACTIVE")

        val result = syncRepository.verifyHouseholdAdminOrOwner("hh_alpha", "user_owner_1")
        assertTrue(result is HouseholdVerificationResult.Success)
        val success = result as HouseholdVerificationResult.Success
        assertEquals("OWNER", success.memberInfo.role)
        assertEquals("ACTIVE", success.memberInfo.status)
    }

    @Test
    fun testAdminPassesVerification() = testScope.runTest {
        fakeSnapshotSource.setMember("hh_alpha", "user_admin_1", "ADMIN", "ACTIVE")

        val result = syncRepository.verifyHouseholdAdminOrOwner("hh_alpha", "user_admin_1")
        assertTrue(result is HouseholdVerificationResult.Success)
        val success = result as HouseholdVerificationResult.Success
        assertEquals("ADMIN", success.memberInfo.role)
        assertEquals("ACTIVE", success.memberInfo.status)
    }

    @Test
    fun testMemberFailsVerification() = testScope.runTest {
        fakeSnapshotSource.setMember("hh_alpha", "user_member_1", "MEMBER", "ACTIVE")

        val result = syncRepository.verifyHouseholdAdminOrOwner("hh_alpha", "user_member_1")
        assertTrue(result is HouseholdVerificationResult.Failure)
        val failure = result as HouseholdVerificationResult.Failure
        assertTrue(failure.error.contains("Insufficient permissions"))
        assertTrue(failure.error.contains("MEMBER"))
    }

    @Test
    fun testInactiveMembershipFailsVerification() = testScope.runTest {
        fakeSnapshotSource.setMember("hh_alpha", "user_inactive_owner", "OWNER", "INACTIVE")

        val result = syncRepository.verifyHouseholdAdminOrOwner("hh_alpha", "user_inactive_owner")
        assertTrue(result is HouseholdVerificationResult.Failure)
        val failure = result as HouseholdVerificationResult.Failure
        assertTrue(failure.error.contains("User membership status is 'INACTIVE'"))
    }

    @Test
    fun testMissingMembershipFailsVerification() = testScope.runTest {
        val result = syncRepository.verifyHouseholdAdminOrOwner("hh_alpha", "user_nonexistent")
        assertTrue(result is HouseholdVerificationResult.Failure)
        val failure = result as HouseholdVerificationResult.Failure
        assertTrue(failure.error.contains("Membership record not found"))
    }

    @Test
    fun testBlankHouseholdOrUidFailsExplicitlyWithoutFallback() = testScope.runTest {
        val blankHh = syncRepository.verifyHouseholdAdminOrOwner("", "user_1")
        assertTrue(blankHh is HouseholdVerificationResult.Failure)
        assertEquals("Invalid household ID: Household ID must not be blank", (blankHh as HouseholdVerificationResult.Failure).error)

        val nullHh = syncRepository.verifyHouseholdAdminOrOwner(null, "user_1")
        assertTrue(nullHh is HouseholdVerificationResult.Failure)
        assertEquals("Invalid household ID: Household ID must not be blank", (nullHh as HouseholdVerificationResult.Failure).error)

        val blankUid = syncRepository.verifyHouseholdAdminOrOwner("hh_1", "")
        assertTrue(blankUid is HouseholdVerificationResult.Failure)
        assertEquals("Invalid user ID: User UID must not be blank", (blankUid as HouseholdVerificationResult.Failure).error)
    }

    @Test
    fun testSanitizedErrorsContainNoStackTraces() = testScope.runTest {
        val throwingSource = object : com.example.data.repository.FirestoreSnapshotSource {
            override fun listenToTransactions(householdId: String, onSnapshot: (List<Pair<String, Map<String, Any?>>>) -> Unit, onError: (Exception) -> Unit) =
                object : com.example.data.repository.ListenerRegistrationHandle { override fun remove() {} }
            override fun listenToCategories(householdId: String, onSnapshot: (List<Pair<String, Map<String, Any?>>>) -> Unit, onError: (Exception) -> Unit) =
                object : com.example.data.repository.ListenerRegistrationHandle { override fun remove() {} }
            override suspend fun resolveHouseholdId(userUid: String): String? = null
            override suspend fun getHouseholdMembership(householdId: String, userUid: String): Map<String, Any?>? {
                throw SecurityException("PERMISSION_DENIED: Missing security permissions at internal.network.FirebaseChannel.call(FirebaseChannel.kt:142)\n\tat com.google.internal.Stack.trace(Stack.kt:99)")
            }
        }

        val repo = FirestoreSyncRepository(
            database = db,
            snapshotSource = throwingSource,
            coroutineScope = testScope
        )

        val result = repo.verifyHouseholdAdminOrOwner("hh_secure", "user_1")
        assertTrue(result is HouseholdVerificationResult.Failure)
        val failure = result as HouseholdVerificationResult.Failure

        // Must be clean single-line error message without stack trace indicators
        assertFalse(failure.error.contains("\tat "))
        assertFalse(failure.error.contains("FirebaseChannel.kt"))
        assertFalse(failure.error.contains("Stack.kt"))
        assertTrue(failure.error.startsWith("Household verification error:"))
    }
}
