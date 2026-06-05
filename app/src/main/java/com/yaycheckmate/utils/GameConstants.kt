package com.yaycheckmate.utils

object GameConstants {

    // XP thresholds per level
    private val LEVEL_XP = listOf(
        0, 100, 250, 450, 700, 1000, 1400, 1900, 2500, 3200,
        4000, 5000, 6200, 7600, 9200, 11000, 13000, 15500, 18500, 22000,
        26000, 30500, 35500, 41000, 47000, 54000, 62000, 71000, 81000, 92000,
        104000, 117000, 131000, 146000, 162000, 179000, 197000, 216000, 236000, 257000,
        280000, 305000, 332000, 361000, 392000, 425000, 460000, 497000, 536000, 577000,
        620000
    )

    private val RANKS = mapOf(
        1 to "Rookie Detective",
        5 to "Investigator",
        10 to "Senior Investigator",
        15 to "Tracker",
        20 to "Expert Tracker",
        25 to "Field Agent",
        30 to "Cyber Detective",
        40 to "Master Detective",
        50 to "Checkmate Master",
        75 to "Elite Finder",
        100 to "Legendary Finder"
    )

    fun getLevelForXp(xp: Int): Int {
        for (i in LEVEL_XP.indices.reversed()) {
            if (xp >= LEVEL_XP[i]) return (i + 1).coerceAtMost(100)
        }
        return 1
    }

    fun getXpForLevel(level: Int): Int {
        val idx = (level - 1).coerceIn(0, LEVEL_XP.lastIndex)
        return LEVEL_XP[idx]
    }

    fun getXpForNextLevel(level: Int): Int {
        val idx = level.coerceIn(0, LEVEL_XP.lastIndex)
        return LEVEL_XP[idx]
    }

    fun getRankForLevel(level: Int): String {
        var rank = "Rookie Detective"
        for ((minLevel, rankName) in RANKS) {
            if (level >= minLevel) rank = rankName
        }
        return rank
    }

    fun getXpForFind(difficultyScore: Int, durationSeconds: Long): Int {
        val base = when (difficultyScore) {
            1 -> 10
            2 -> 20
            3 -> 35
            4 -> 55
            5 -> 80
            else -> 10
        }
        val timeBonus = when {
            durationSeconds < 30 -> 20
            durationSeconds < 60 -> 10
            durationSeconds < 120 -> 5
            else -> 0
        }
        return base + timeBonus
    }

    fun getCoinsForFind(difficultyScore: Int): Int = difficultyScore * 5

    // Difficulty definitions
    val DIFFICULTY_LABELS = mapOf(
        "Wallet" to Pair("Easy", 1),
        "Backpack" to Pair("Easy", 1),
        "Phone" to Pair("Easy", 1),
        "Bag" to Pair("Easy", 1),
        "Shoes" to Pair("Easy", 1),
        "Keys" to Pair("Medium", 3),
        "Remote" to Pair("Medium", 3),
        "Glasses" to Pair("Medium", 3),
        "Headphones" to Pair("Medium", 3),
        "Book" to Pair("Medium", 3),
        "USB Drive" to Pair("Hard", 4),
        "Charger" to Pair("Hard", 4),
        "Earbuds" to Pair("Hard", 4),
        "Pen" to Pair("Hard", 4),
        "Coin" to Pair("Very Hard", 5),
        "Ring" to Pair("Very Hard", 5),
        "SD Card" to Pair("Very Hard", 5)
    )

    fun getDifficultyForName(name: String): Pair<String, Int> {
        val upper = name.trim()
        for ((key, value) in DIFFICULTY_LABELS) {
            if (upper.contains(key, ignoreCase = true)) return value
        }
        return Pair("Medium", 3)
    }

    fun getDifficultyColor(difficulty: String): Int {
        return when (difficulty) {
            "Easy" -> 0xFF00FF88.toInt()
            "Medium" -> 0xFFFFD700.toInt()
            "Hard" -> 0xFFFF6B35.toInt()
            "Very Hard" -> 0xFFFF0055.toInt()
            else -> 0xFF00FFFF.toInt()
        }
    }

    // Achievement definitions
    data class Achievement(
        val id: String,
        val title: String,
        val description: String,
        val icon: String,
        val xpReward: Int,
        val coinReward: Int
    )

    val ACHIEVEMENTS = listOf(
        Achievement("first_register", "First Registration", "Register your first object", "🔍", 50, 20),
        Achievement("first_recovery", "First Recovery", "Find your first object", "🎯", 100, 50),
        Achievement("lost_no_more", "Lost No More", "Find an object on first search", "⚡", 75, 30),
        Achievement("ten_finds", "10 Successful Finds", "Successfully find 10 objects", "🏆", 200, 100),
        Achievement("hundred_finds", "100 Successful Finds", "Successfully find 100 objects", "💎", 1000, 500),
        Achievement("treasure_hunter", "Treasure Hunter", "Register 10 different objects", "🗝️", 150, 75),
        Achievement("master_investigator", "Master Investigator", "Reach level 30", "🔮", 500, 250),
        Achievement("checkmate_champion", "Checkmate Champion", "Reach level 50", "👑", 2000, 1000),
        Achievement("speed_finder", "Speed Finder", "Find object in under 30 seconds", "⚡", 100, 50),
        Achievement("streak_3", "On a Roll", "3-day search streak", "🔥", 75, 30),
        Achievement("streak_7", "Week Warrior", "7-day search streak", "🌟", 200, 100)
    )
}
