package com.yaycheckmate.viewmodel

import androidx.lifecycle.*
import com.yaycheckmate.YayCheckmateApp
import com.yaycheckmate.data.entity.ObjectItem
import com.yaycheckmate.data.entity.UserStats
import com.yaycheckmate.utils.GameConstants
import kotlinx.coroutines.launch

class DashboardViewModel(private val app: YayCheckmateApp) : ViewModel() {

    val allActiveObjects: LiveData<List<ObjectItem>> =
        app.objectRepository.allActiveObjects.asLiveData()

    val userStats: LiveData<UserStats?> =
        app.userStatsRepository.userStats.asLiveData()

    val xpProgress: LiveData<Pair<Int, Int>> = userStats.map { stats ->
        val level = stats?.level ?: 1
        val xp = stats?.totalXp ?: 0
        val currentLevelXp = GameConstants.getXpForLevel(level)
        val nextLevelXp = GameConstants.getXpForNextLevel(level)
        val progress = if (nextLevelXp > currentLevelXp) {
            ((xp - currentLevelXp).toFloat() / (nextLevelXp - currentLevelXp) * 100).toInt()
        } else 100
        Pair(progress, nextLevelXp - xp)
    }

    fun refreshStats() {
        viewModelScope.launch {
            try {
                val totalRegistered = app.objectRepository.getActiveCount()
                val totalFound = app.searchSessionRepository.getTotalFound()
                val totalSearches = app.searchSessionRepository.getTotalSearches()
                val avgDuration = app.searchSessionRepository.getAverageDuration()?.toLong() ?: 0L
                val mostLost = app.objectRepository.getMostLostObject()?.name ?: ""
                val mostLocation = app.searchSessionRepository.getMostCommonLocation()
                app.userStatsRepository.recalculateStats(
                    totalRegistered, totalFound, totalSearches, avgDuration, mostLost, mostLocation
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class DashboardViewModelFactory(private val app: YayCheckmateApp) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
