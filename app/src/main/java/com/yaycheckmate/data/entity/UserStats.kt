package com.yaycheckmate.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey
    val id: Int = 1,
    val totalXp: Int = 0,
    val level: Int = 1,
    val rank: String = "Rookie Detective",
    val registeredObjects: Int = 0,
    val foundObjects: Int = 0,
    val totalSearches: Int = 0,
    val successRate: Float = 0f,
    val averageSearchSeconds: Long = 0L,
    val mostLostItem: String = "",
    val mostCommonLocation: String = "",
    val coinsEarned: Int = 0,
    val achievementsUnlocked: String = "", // JSON list of achievement IDs
    val dailyStreakDays: Int = 0,
    val lastActiveDate: Long = 0L
)
