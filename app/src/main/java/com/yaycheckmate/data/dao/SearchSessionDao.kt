package com.yaycheckmate.data.dao

import androidx.room.*
import com.yaycheckmate.data.entity.SearchSession
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchSessionDao {

    @Query("SELECT * FROM search_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SearchSession>>

    @Query("SELECT * FROM search_sessions ORDER BY startTime DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<SearchSession>>

    @Query("SELECT * FROM search_sessions WHERE objectId = :objectId ORDER BY startTime DESC")
    fun getSessionsForObject(objectId: Long): Flow<List<SearchSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SearchSession): Long

    @Update
    suspend fun updateSession(session: SearchSession)

    @Query("SELECT COUNT(*) FROM search_sessions WHERE result = 'Found'")
    suspend fun getTotalFound(): Int

    @Query("SELECT COUNT(*) FROM search_sessions")
    suspend fun getTotalSearches(): Int

    @Query("SELECT AVG(durationSeconds) FROM search_sessions WHERE result = 'Found'")
    suspend fun getAverageDuration(): Double?

    @Query("SELECT foundLocation, COUNT(*) as cnt FROM search_sessions WHERE result = 'Found' AND foundLocation != '' GROUP BY foundLocation ORDER BY cnt DESC LIMIT 1")
    suspend fun getMostCommonLocation(): LocationCount?

    @Query("SELECT * FROM search_sessions WHERE result = 'Found' ORDER BY startTime DESC LIMIT 50")
    suspend fun getRecentFoundSessions(): List<SearchSession>
}

data class LocationCount(val foundLocation: String, val cnt: Int)
