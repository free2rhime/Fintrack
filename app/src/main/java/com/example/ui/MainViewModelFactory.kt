package com.example.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.di.AppContainer

class MainViewModelFactory(
    private val appContainer: AppContainer,
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(
                transactionRepository = appContainer.transactionRepository,
                categoryRepository = appContainer.categoryRepository,
                settingsRepository = appContainer.settingsRepository,
                authRepository = appContainer.authRepository,
                syncRepository = appContainer.syncRepository,
                application = application
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
