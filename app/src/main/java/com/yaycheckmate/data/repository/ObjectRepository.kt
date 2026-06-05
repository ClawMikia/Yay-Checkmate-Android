package com.yaycheckmate.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yaycheckmate.data.dao.ObjectItemDao
import com.yaycheckmate.data.entity.ObjectItem
import kotlinx.coroutines.flow.Flow

class ObjectRepository(
    private val dao: ObjectItemDao,
    private val context: Context
) {
    private val gson = Gson()

    val allActiveObjects: Flow<List<ObjectItem>> = dao.getAllActiveObjects()
    val allObjects: Flow<List<ObjectItem>> = dao.getAllObjects()

    suspend fun getObjectById(id: Long): ObjectItem? = dao.getObjectById(id)

    fun searchObjects(query: String): Flow<List<ObjectItem>> = dao.searchObjects(query)

    suspend fun registerObject(item: ObjectItem): Long = dao.insertObject(item)

    suspend fun updateObject(item: ObjectItem) = dao.updateObject(item)

    suspend fun deleteObject(item: ObjectItem) = dao.deleteObject(item)

    suspend fun recordFound(objectId: Long, location: String, currentHeatmap: String): String {
        val type = object : TypeToken<MutableMap<String, Int>>() {}.type
        val heatmap: MutableMap<String, Int> = if (currentHeatmap.isNotEmpty()) {
            try { gson.fromJson(currentHeatmap, type) } catch (e: Exception) { mutableMapOf() }
        } else {
            mutableMapOf()
        }
        heatmap[location] = (heatmap[location] ?: 0) + 1
        val updatedJson = gson.toJson(heatmap)
        dao.recordFound(objectId, location, System.currentTimeMillis(), updatedJson)
        return updatedJson
    }

    suspend fun recordSearch(objectId: Long) = dao.recordSearch(objectId)

    suspend fun getActiveCount(): Int = dao.getActiveCount()

    suspend fun getMostLostObject(): ObjectItem? = dao.getMostLostObject()

    fun getHeatmapPercentages(heatmapJson: String): Map<String, Int> {
        if (heatmapJson.isEmpty()) return emptyMap()
        val type = object : TypeToken<Map<String, Int>>() {}.type
        val raw: Map<String, Int> = try { gson.fromJson(heatmapJson, type) } catch (e: Exception) { return emptyMap() }
        val total = raw.values.sum().toFloat()
        if (total == 0f) return emptyMap()
        return raw.mapValues { ((it.value / total) * 100).toInt() }
            .entries.sortedByDescending { it.value }
            .associate { it.key to it.value }
    }
}
