package com.yaycheckmate.app.ui.add

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yaycheckmate.app.YayCheckmateApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class AddObjectViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as YayCheckmateApp).repository

    sealed interface SaveState {
        data object Idle : SaveState
        data object Saving : SaveState
        data class Done(val id: Long) : SaveState
        data class Error(val message: String) : SaveState
    }

    private val _saveState = MutableLiveData<SaveState>(SaveState.Idle)
    val saveState: LiveData<SaveState> = _saveState

    fun save(
        tempImageFile: File,
        name: String,
        description: String,
        tags: String,
        latitude: Double?,
        longitude: Double?,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _saveState.postValue(SaveState.Saving)
            try {
                val id = repository.saveNewObject(
                    name = name,
                    description = description,
                    tags = tags,
                    tempImageFile = tempImageFile,
                    latitude = latitude,
                    longitude = longitude,
                )
                _saveState.postValue(SaveState.Done(id))
            } catch (e: Exception) {
                _saveState.postValue(SaveState.Error(e.message ?: "Save failed"))
            }
        }
    }
}
