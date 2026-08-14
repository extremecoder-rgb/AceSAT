package com.acesat.education.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_plans")
data class StudyPlan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val stepOrder: Int,
    val title: String,
    val description: String,
    val category: String,
    val estimatedMinutes: Int,
    val isCompleted: Boolean = false,
    val createdTime: Long = System.currentTimeMillis()
)
