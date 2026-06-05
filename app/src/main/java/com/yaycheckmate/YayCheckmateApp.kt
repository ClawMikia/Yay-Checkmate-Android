package com.yaycheckmate

import android.app.Application
import com.yaycheckmate.data.database.AppDatabase
import com.yaycheckmate.data.repository.ObjectRepository
import com.yaycheckmate.data.repository.SearchSessionRepository
import com.yaycheckmate.data.repository.UserStatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class YayCheckmateApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getInstance(this) }

    val objectRepository by lazy { ObjectRepository(database.objectItemDao(), this) }
    val searchSessionRepository by lazy { SearchSessionRepository(database.searchSessionDao()) }
    val userStatsRepository by lazy { UserStatsRepository(database.userStatsDao(), this) }
}
