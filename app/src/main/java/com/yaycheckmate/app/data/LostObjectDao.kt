package com.yaycheckmate.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LostObjectDao {

    @Query("SELECT * FROM lost_objects ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<LostObjectEntity>>

    @Query("SELECT * FROM lost_objects WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): LostObjectEntity?

    @Query("SELECT * FROM lost_objects WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<LostObjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LostObjectEntity): Long

    @Update
    suspend fun update(entity: LostObjectEntity)

    @Delete
    suspend fun delete(entity: LostObjectEntity)
}
