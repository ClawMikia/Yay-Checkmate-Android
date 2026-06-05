package com.yaycheckmate.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yaycheckmate.data.dao.UserStatsDao
import com.yaycheckmate.data.entity.UserStats
import com.yaycheckmate.utils.GameConstants
import kotlinx.coroutines.flow.Flow

class UserStatsRepository(
    private val dao: UserStatsDao,
    private val context: Context
) {
    private val gson = Gson()

    val userStats: Flow<UserStats?> = dao.getUserStats()

    suspend fun getUserStatsOnce(): UserStats? = dao.getUserStatsOnce()

    suspend fun addXp(xp: Int) {
        dao.addXp(xp)
        val stats = dao.getUserStatsOnce() ?: return
        val newXp = stats.totalXp + xp
        val newLevel = GameConstants.getLevelForXp(newXp)
        val newRank = GameConstants.getRankForLevel(newLevel)
        dao.updateStats(stats.copy(
            totalXp = newXp,
            level = newLevel,
            rank = newRank
        ))
    }

    suspend fun addCoins(coins: Int) = dao.addCoins(coins)

    suspend fun updateStats(stats: UserStats) = dao.updateStats(stats)

    suspend fun unlockAchievement(achievementId: String) {
        val stats = dao.getUserStatsOnce() ?: return
        val type = object : TypeToken<MutableList<String>>() {}.type
        val list: MutableList<String> = if (stats.achievementsUnlocked.isNotEmpty()) {
            try { gson.fromJson(stats.achievementsUnlocked, type) } catch (e: Exception) { mutableListOf() }
        } else mutableListOf()
        if (!list.contains(achievementId)) {
            list.add(achievementId)
            dao.updateStats(stats.copy(achievementsUnlocked = gson.toJson(list)))
        }
    }

    suspend fun getUnlockedAchievements(): List<String> {
        val stats = dao.getUserStatsOnce() ?: return emptyList()
        if (stats.achievementsUnlocked.isEmpty()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return try { gson.fromJson(stats.achievementsUnlocked, type) } catch (e: Exception) { emptyList() }
    }

    suspend fun recalculateStats(totalRegistered: Int, totalFound: Int, totalSearches: Int,
                                  avgSeconds: Long, mostLost: String, mostLocation: String) {
        val stats = dao.getUserStatsOnce() ?: UserStats()
        val successRate = if (totalSearches > 0) (totalFound.toFloat() / totalSearches) * 100f else 0f
        dao.updateStats(stats.copy(
            registeredObjects = totalRegistered,
            foundObjects = totalFound,
            totalSearches = totalSearches,
            successRate = successRate,
            averageSearchSeconds = avgSeconds,
            mostLostItem = mostLost,
            mostCommonLocation = mostLocation
        ))
    }
}
