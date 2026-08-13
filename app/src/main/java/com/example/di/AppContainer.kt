package com.example.di

import android.content.Context
import com.example.data.db.FinTrackDatabase
import com.example.data.repository.CategoryRepository
import com.example.data.repository.DataStoreSettingsRepository
import com.example.data.repository.RoomCategoryRepository
import com.example.data.repository.RoomTransactionRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TransactionRepository
import com.example.data.service.ExchangeRateService

import com.example.data.repository.AuthRepository
import com.example.data.repository.FirebaseAuthRepository

import com.example.data.repository.FirestoreSyncRepository

interface AppContainer {
    val transactionRepository: TransactionRepository
    val categoryRepository: CategoryRepository
    val settingsRepository: SettingsRepository
    val authRepository: AuthRepository
    val database: FinTrackDatabase
    val syncRepository: FirestoreSyncRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val syncRepository: FirestoreSyncRepository by lazy {
        FirestoreSyncRepository(database = database)
    }

    override val authRepository: AuthRepository by lazy {
        FirebaseAuthRepository()
    }

    override val database: FinTrackDatabase by lazy {
        FinTrackDatabase.getDatabase(context)
    }

    private val exchangeRateService: ExchangeRateService by lazy {
        ExchangeRateService(database.exchangeRateDao())
    }

    override val transactionRepository: TransactionRepository by lazy {
        RoomTransactionRepository(
            transactionDao = database.transactionDao(),
            exchangeRateService = exchangeRateService,
            exchangeRateDao = database.exchangeRateDao(),
            database = database
        )
    }

    override val categoryRepository: CategoryRepository by lazy {
        RoomCategoryRepository(
            categoryDao = database.categoryDao(),
            syncOutboxDao = database.syncOutboxDao(),
            database = database
        )
    }

    override val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(context)
    }
}
