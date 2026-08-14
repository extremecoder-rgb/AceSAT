package com.acesat.education.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attempts")
data class Attempt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val section: String, // "Math" or "Reading & Writing"
    val category: String, // e.g. "Quadratic Equations", "Inference"
    val difficulty: String, // "Easy", "Medium", "Hard"
    val questionText: String,
    val selectedAnswer: String,
    val correctAnswer: String,
    val isCorrect: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
