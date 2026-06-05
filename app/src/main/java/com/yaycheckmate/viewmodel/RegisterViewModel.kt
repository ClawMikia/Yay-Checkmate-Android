package com.yaycheckmate.viewmodel

import androidx.lifecycle.*
import com.yaycheckmate.YayCheckmateApp
import com.yaycheckmate.data.entity.ObjectItem
import com.yaycheckmate.utils.GameConstants
import kotlinx.coroutines.launch

class RegisterViewModel(private val app: YayCheckmateApp) : ViewModel() {

    private val _registrationResult = MutableLiveData<Result<Long>>()
    val registrationResult: LiveData<Result<Long>> = _registrationResult

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Photo paths collected during registration
    var photoFrontPath: String = ""
    var photoBackPath: String = ""
    var photoLeftPath: String = ""
    var photoRightPath: String = ""
    var photoTopPath: String = ""
    var environmentDescription: String = ""

    fun registerObject(
        name: String,
        category: String,
        description: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val (difficulty, difficultyScore) = GameConstants.getDifficultyForName(name)
                val item = ObjectItem(
                    name = name,
                    category = category,
                    difficulty = difficulty,
                    difficultyScore = difficultyScore,
                    description = description,
                    photoFrontPath = photoFrontPath,
                    photoBackPath = photoBackPath,
                    photoLeftPath = photoLeftPath,
                    photoRightPath = photoRightPath,
                    photoTopPath = photoTopPath,
                    environmentDescription = environmentDescription
                )
                val id = app.objectRepository.registerObject(item)
                // Check achievements
                val count = app.objectRepository.getActiveCount()
                if (count == 1) app.userStatsRepository.unlockAchievement("first_register")
                if (count >= 10) app.userStatsRepository.unlockAchievement("treasure_hunter")
                app.userStatsRepository.addXp(25)
                _registrationResult.value = Result.success(id)
            } catch (e: Exception) {
                _registrationResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetPhotos() {
        photoFrontPath = ""
        photoBackPath = ""
        photoLeftPath = ""
        photoRightPath = ""
        photoTopPath = ""
        environmentDescription = ""
    }
}

class RegisterViewModelFactory(private val app: YayCheckmateApp) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegisterViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
