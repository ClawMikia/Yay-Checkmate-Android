package com.yaycheckmate.viewmodel

import androidx.lifecycle.*
import com.yaycheckmate.YayCheckmateApp
import com.yaycheckmate.data.entity.ObjectItem
import com.yaycheckmate.data.entity.SearchSession
import com.yaycheckmate.utils.GameConstants
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel(private val app: YayCheckmateApp) : ViewModel() {

    private val _currentObject = MutableLiveData<ObjectItem?>()
    val currentObject: LiveData<ObjectItem?> = _currentObject

    private val _elapsedSeconds = MutableLiveData(0L)
    val elapsedSeconds: LiveData<Long> = _elapsedSeconds

    private val _detectionConfidence = MutableLiveData(0)
    val detectionConfidence: LiveData<Int> = _detectionConfidence

    private val _detectionLabel = MutableLiveData("Scanning...")
    val detectionLabel: LiveData<String> = _detectionLabel

    private val _mascotMessage = MutableLiveData("Scanning environment...")
    val mascotMessage: LiveData<String> = _mascotMessage

    private val _searchCompleted = MutableLiveData<SearchSession?>()
    val searchCompleted: LiveData<SearchSession?> = _searchCompleted

    private val _heatmapData = MutableLiveData<Map<String, Int>>()
    val heatmapData: LiveData<Map<String, Int>> = _heatmapData

    private var timerJob: Job? = null
    private var sessionId: Long = -1L
    private var searchStartTime: Long = 0L
    private var isSearchActive = false

    val mascotMessages = listOf(
        "Object may be nearby.",
        "Scanning environment...",
        "Potential match detected.",
        "Searching... stay focused.",
        "Check nearby surfaces.",
        "Look in usual spots first.",
        "Analyzing surroundings...",
        "Checkmate! Item found!"
    )

    fun loadObject(objectId: Long) {
        viewModelScope.launch {
            val obj = app.objectRepository.getObjectById(objectId)
            _currentObject.value = obj
            obj?.let {
                val percentages = app.objectRepository.getHeatmapPercentages(it.heatmapData)
                _heatmapData.value = percentages
            }
        }
    }

    fun startSearch() {
        isSearchActive = true
        searchStartTime = System.currentTimeMillis()
        _elapsedSeconds.value = 0L
        startTimer()
        rotateMascotMessages()
        viewModelScope.launch {
            val obj = _currentObject.value ?: return@launch
            app.objectRepository.recordSearch(obj.id)
            val session = SearchSession(
                objectId = obj.id,
                objectName = obj.name,
                startTime = searchStartTime
            )
            sessionId = app.searchSessionRepository.insertSession(session)
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isSearchActive) {
                delay(1000)
                _elapsedSeconds.value = (_elapsedSeconds.value ?: 0) + 1
            }
        }
    }

    private fun rotateMascotMessages() {
        viewModelScope.launch {
            val messages = mascotMessages.dropLast(1)
            var idx = 0
            while (isSearchActive) {
                _mascotMessage.value = messages[idx % messages.size]
                idx++
                delay(3000)
            }
        }
    }

    fun updateDetection(label: String, confidence: Int) {
        _detectionLabel.value = label
        _detectionConfidence.value = confidence
    }

    fun markFound(location: String) {
        isSearchActive = false
        timerJob?.cancel()
        _mascotMessage.value = "Checkmate! Item found!"
        val elapsed = _elapsedSeconds.value ?: 0L
        val obj = _currentObject.value ?: return
        viewModelScope.launch {
            val confidence = _detectionConfidence.value ?: 70
            app.objectRepository.recordFound(obj.id, location, obj.heatmapData)
            val xp = GameConstants.getXpForFind(obj.difficultyScore, elapsed)
            val coins = GameConstants.getCoinsForFind(obj.difficultyScore)
            app.userStatsRepository.addXp(xp)
            app.userStatsRepository.addCoins(coins)
            // Achievement checks
            val totalFound = app.searchSessionRepository.getTotalFound() + 1
            if (totalFound == 1) app.userStatsRepository.unlockAchievement("first_recovery")
            if (totalFound >= 10) app.userStatsRepository.unlockAchievement("ten_finds")
            if (totalFound >= 100) app.userStatsRepository.unlockAchievement("hundred_finds")
            if (elapsed < 30) app.userStatsRepository.unlockAchievement("speed_finder")
            val session = SearchSession(
                id = sessionId.coerceAtLeast(0),
                objectId = obj.id,
                objectName = obj.name,
                startTime = searchStartTime,
                endTime = System.currentTimeMillis(),
                durationSeconds = elapsed,
                result = "Found",
                foundLocation = location,
                confidenceScore = confidence,
                xpEarned = xp
            )
            if (sessionId > 0) app.searchSessionRepository.updateSession(session)
            else app.searchSessionRepository.insertSession(session)
            _searchCompleted.value = session
        }
    }

    fun cancelSearch() {
        isSearchActive = false
        timerJob?.cancel()
        val elapsed = _elapsedSeconds.value ?: 0L
        val obj = _currentObject.value ?: return
        viewModelScope.launch {
            if (sessionId > 0) {
                val session = SearchSession(
                    id = sessionId,
                    objectId = obj.id,
                    objectName = obj.name,
                    startTime = searchStartTime,
                    endTime = System.currentTimeMillis(),
                    durationSeconds = elapsed,
                    result = "Cancelled",
                    xpEarned = 0
                )
                app.searchSessionRepository.updateSession(session)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        isSearchActive = false
        timerJob?.cancel()
    }
}

class SearchViewModelFactory(private val app: YayCheckmateApp) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
