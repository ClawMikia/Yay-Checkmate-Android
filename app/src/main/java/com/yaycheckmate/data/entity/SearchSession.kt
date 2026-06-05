package com.yaycheckmate.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "search_sessions")
data class SearchSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val objectId: Long,
    val objectName: String,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = 0L,
    val durationSeconds: Long = 0L,
    val result: String = "Unknown", // Found, Not Found, Cancelled
    val foundLocation: String = "",
    val confidenceScore: Int = 0, // 0-100
    val xpEarned: Int = 0
) : Parcelable
