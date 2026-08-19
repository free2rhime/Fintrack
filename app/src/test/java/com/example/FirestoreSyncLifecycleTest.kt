package com.example

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.FinTrackDatabase
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthState
import com.example.data.repository.FirestoreSyncRepository
import com.example.ui.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class FakeAuthRepositoryForSync : AuthRepository {
    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override suspend fun signInWithGoogleCredential(idToken: String): Result<String> {
        val uid = "user_$idToken"
        _authState.value = AuthState.SignedIn(uid, "test@example.com")
        return Result.success(uid)
    }

    override suspend fun signInWithTestUid(testUid: String, email: String?, displayName: String?): Result<String> {
        _authState.value = AuthState.SignedIn(testUid, email ?: "$testUid@example.com", displayName)
        return Result.success(testUid)
    }

    override suspend fun signOut() {
        _authState.value = AuthState.SignedOut
    }

    override fun getCurrentUserUid(): String? {
        return (authState.value as? AuthState.SignedIn)?.userUid
    }

    override fun clearError() {
        if (_authState.value is AuthState.AuthError) {
            _authState.value = AuthState.SignedOut
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FirestoreSyncLifecycleTest {

    private lateinit var db: FinTrackDatabase
    private lateinit var fakeSnapshotSource: FakeSnapshotSource
    private lateinit var syncRepository: FirestoreSyncRepository
    private lateinit var fakeAuthRepo: FakeAuthRepositoryForSync
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        fakeSnapshotSource = FakeSnapshotSource()
        fakeSnapshotSource.setMember("hh_stage3", "user_stage3", "OWNER", "ACTIVE")
        syncRepository = FirestoreSyncRepository(
            database = db,
            snapshotSource = fakeSnapshotSource,
            coroutineScope = testScope
        )
        fakeAuthRepo = FakeAuthRepositoryForSync()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testSignInStartsInboundSyncAndUpdatesStatus() = testScope.runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(
            transactionRepository = FakeTransactionRepository(),
            categoryRepository = FakeCategoryRepository(),
            settingsRepository = FakeSettingsRepository(),
            authRepository = fakeAuthRepo,
            syncRepository = syncRepository,
            application = app
        )
        backgroundScope.launch { viewModel.syncStatus.collect {} }

        assertEquals("Signed out", viewModel.syncStatus.value)

        fakeAuthRepo.signInWithTestUid("user_stage3")
        testScheduler.advanceUntilIdle()

        assertTrue(syncRepository.isListening)
        assertEquals("user_stage3", syncRepository.activeUserUid)
        assertEquals("Synced", viewModel.syncStatus.value)
    }

    @Test
    fun testSignOutStopsSynchronizationAndResetsStatus() = testScope.runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(
            transactionRepository = FakeTransactionRepository(),
            categoryRepository = FakeCategoryRepository(),
            settingsRepository = FakeSettingsRepository(),
            authRepository = fakeAuthRepo,
            syncRepository = syncRepository,
            application = app
        )
        backgroundScope.launch { viewModel.syncStatus.collect {} }

        fakeAuthRepo.signInWithTestUid("user_stage3")
        testScheduler.advanceUntilIdle()
        assertTrue(syncRepository.isListening)

        fakeAuthRepo.signOut()
        testScheduler.advanceUntilIdle()

        assertFalse(syncRepository.isListening)
        assertEquals("Signed out", viewModel.syncStatus.value)
    }

    @Test
    fun testRepeatedSignedInStateDoesNotCreateDuplicateListeners() = testScope.runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(
            transactionRepository = FakeTransactionRepository(),
            categoryRepository = FakeCategoryRepository(),
            settingsRepository = FakeSettingsRepository(),
            authRepository = fakeAuthRepo,
            syncRepository = syncRepository,
            application = app
        )
        backgroundScope.launch { viewModel.syncStatus.collect {} }

        fakeAuthRepo.signInWithTestUid("user_stage3")
        testScheduler.advanceUntilIdle()
        val removeCountFirst = fakeSnapshotSource.txListenerRemoveCount

        // Emitting signed-in state again for the same user
        fakeAuthRepo.signInWithTestUid("user_stage3")
        testScheduler.advanceUntilIdle()

        assertEquals(removeCountFirst, fakeSnapshotSource.txListenerRemoveCount)
        assertEquals(2, syncRepository.listenerCount)
    }
}
