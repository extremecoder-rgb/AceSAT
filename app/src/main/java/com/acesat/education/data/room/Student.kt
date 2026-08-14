package com.acesat.education.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val gradeLevel: String,
    val targetScore: Int,
    val diagnosticScore: Int = 0,
    val mathScore: Int = 200,
    val readingWritingScore: Int = 200,
    val totalScore: Int = 400,
    val lastActive: Long = System.currentTimeMillis()
)
