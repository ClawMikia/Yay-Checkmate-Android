package com.yaycheckmate.viewmodel

import androidx.lifecycle.*
import com.yaycheckmate.YayCheckmateApp
import com.yaycheckmate.data.entity.SearchSession

class HistoryViewModel(private val app: YayCheckmateApp) : ViewModel() {

    val recentSessions: LiveData<List<SearchSession>> =
        app.searchSessionRepository.getRecentSessions(50).asLiveData()

    val allObjects = app.objectRepository.allActiveObjects.asLiveData()
}

class HistoryViewModelFactory(private val app: YayCheckmateApp) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
