package com.yaycheckmate.app.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yaycheckmate.app.YayCheckmateApp
import com.yaycheckmate.app.data.LostObjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class DetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as YayCheckmateApp).repository

    fun observe(id: Long): Flow<LostObjectEntity?> = repository.observeById(id)

    fun delete(entity: LostObjectEntity, onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(entity)
            kotlinx.coroutines.withContext(Dispatchers.Main) { onDone() }
        }
    }

    fun update(entity: LostObjectEntity, onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateObject(entity, null)
            kotlinx.coroutines.withContext(Dispatchers.Main) { onDone() }
        }
    }
}
