package com.yaycheckmate.data.dao

import androidx.room.*
import com.yaycheckmate.data.entity.ObjectItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ObjectItemDao {

    @Query("SELECT * FROM object_items WHERE isActive = 1 ORDER BY registeredAt DESC")
    fun getAllActiveObjects(): Flow<List<ObjectItem>>

    @Query("SELECT * FROM object_items ORDER BY registeredAt DESC")
    fun getAllObjects(): Flow<List<ObjectItem>>

    @Query("SELECT * FROM object_items WHERE id = :id")
    suspend fun getObjectById(id: Long): ObjectItem?

    @Query("SELECT * FROM object_items WHERE name LIKE '%' || :query || '%' AND isActive = 1")
    fun searchObjects(query: String): Flow<List<ObjectItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObject(item: ObjectItem): Long

    @Update
    suspend fun updateObject(item: ObjectItem)

    @Delete
    suspend fun deleteObject(item: ObjectItem)

    @Query("UPDATE object_items SET totalFinds = totalFinds + 1, totalSearches = totalSearches + 1, lastFoundLocation = :location, lastFoundTime = :time, heatmapData = :heatmap WHERE id = :id")
    suspend fun recordFound(id: Long, location: String, time: Long, heatmap: String)

    @Query("UPDATE object_items SET totalSearches = totalSearches + 1 WHERE id = :id")
    suspend fun recordSearch(id: Long)

    @Query("SELECT COUNT(*) FROM object_items WHERE isActive = 1")
    suspend fun getActiveCount(): Int

    @Query("SELECT * FROM object_items WHERE isActive = 1 ORDER BY totalSearches - totalFinds DESC LIMIT 1")
    suspend fun getMostLostObject(): ObjectItem?
}
