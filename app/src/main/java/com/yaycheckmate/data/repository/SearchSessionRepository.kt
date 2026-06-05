package com.yaycheckmate.data.repository

import com.yaycheckmate.data.dao.SearchSessionDao
import com.yaycheckmate.data.entity.SearchSession
import kotlinx.coroutines.flow.Flow

class SearchSessionRepository(private val dao: SearchSessionDao) {

    val allSessions: Flow<List<SearchSession>> = dao.getAllSessions()

    fun getRecentSessions(limit: Int = 20): Flow<List<SearchSession>> = dao.getRecentSessions(limit)

    fun getSessionsForObject(objectId: Long): Flow<List<SearchSession>> = dao.getSessionsForObject(objectId)

    suspend fun insertSession(session: SearchSession): Long = dao.insertSession(session)

    suspend fun updateSession(session: SearchSession) = dao.updateSession(session)

    suspend fun getTotalFound(): Int = dao.getTotalFound()

    suspend fun getTotalSearches(): Int = dao.getTotalSearches()

    suspend fun getAverageDuration(): Double? = dao.getAverageDuration()

    suspend fun getMostCommonLocation(): String {
        return dao.getMostCommonLocation()?.foundLocation ?: ""
    }
}
