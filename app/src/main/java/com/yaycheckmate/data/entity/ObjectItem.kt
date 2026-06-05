package com.yaycheckmate.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "object_items")
data class ObjectItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String,
    val difficulty: String, // Easy, Medium, Hard, Very Hard
    val difficultyScore: Int, // 1-5
    val description: String = "",
    val photoFrontPath: String = "",
    val photoBackPath: String = "",
    val photoLeftPath: String = "",
    val photoRightPath: String = "",
    val photoTopPath: String = "",
    val environmentDescription: String = "", // JSON of detected environment
    val heatmapData: String = "", // JSON: {"Bedroom":45,"Living Room":35}
    val lastFoundLocation: String = "",
    val lastFoundTime: Long = 0L,
    val totalFinds: Int = 0,
    val totalSearches: Int = 0,
    val isActive: Boolean = true,
    val registeredAt: Long = System.currentTimeMillis()
) : Parcelable
