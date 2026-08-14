package com.acesat.education.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weak_areas")
data class WeakArea(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val section: String,
    val category: String, // e.g. "Linear Equations", "Grammar"
    val proficiencyScore: Int, // 0 - 100
    val totalAttempts: Int,
    val correctAttempts: Int,
    val lastUpdated: Long = System.currentTimeMillis()
)
