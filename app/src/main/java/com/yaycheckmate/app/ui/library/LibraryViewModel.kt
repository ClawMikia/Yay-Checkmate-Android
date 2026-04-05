package com.yaycheckmate.app.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.yaycheckmate.app.YayCheckmateApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    val repository = (application as YayCheckmateApp).repository
    val objectsFlow = repository.observeAll()

    private val _isGrid = MutableStateFlow(true)
    val isGrid: StateFlow<Boolean> = _isGrid.asStateFlow()

    fun toggleGrid() {
        _isGrid.value = !_isGrid.value
    }
}
