package com.acesat.education.agent

import com.acesat.education.data.api.ChatRequest
import com.acesat.education.data.api.Message
import com.acesat.education.data.api.NvidiaService
import com.acesat.education.data.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONArray
import org.json.JSONObject

class AdaptiveAgent(
    private val db: AppDatabase,
    private val apiService: NvidiaService
) {
    private val gson = Gson()

    // 1. Diagnosis Step
    // Evaluates diagnostic quiz answers and stores weak areas
    suspend fun runDiagnosis(studentId: Int, sectionScores: Map<String, Int>) {
        val studentDao = db.studentDao()
        val weakAreaDao = db.weakAreaDao()

        val student = studentDao.getStudent().firstOrNull() ?: return

        // Calculate scores
        val mathScore = sectionScores["Math"] ?: 200
        val rwScore = sectionScores["Reading & Writing"] ?: 200
        val totalScore = mathScore + rwScore

        val updatedStudent = student.copy(
            diagnosticScore = totalScore,
            mathScore = mathScore,
            readingWritingScore = rwScore,
            totalScore = totalScore
        )
        studentDao.updateStudent(updatedStudent)

        // Insert initial weak areas based on score threshold
        val weakMathCategories = if (mathScore < 550) {
            listOf("Linear Equations" to 40, "Quadratic Equations" to 30, "Geometry" to 50)
        } else {
            listOf("Quadratic Equations" to 70, "Trigonometry" to 75)
        }

        val weakRwCategories = if (rwScore < 550) {
            listOf("Inference" to 45, "Grammar & Punctuation" to 35, "Vocabulary in Context" to 50)
        } else {
            listOf("Inference" to 70, "Rhetorical Synthesis" to 72)
        }

        val allWeakness = weakMathCategories.map { (cat, prof) ->
            WeakArea(studentId = studentId, section = "Math", category = cat, proficiencyScore = prof, totalAttempts = 0, correctAttempts = 0)
        } + weakRwCategories.map { (cat, prof) ->
            WeakArea(studentId = studentId, section = "Reading & Writing", category = cat, proficiencyScore = prof, totalAttempts = 0, correctAttempts = 0)
        }

        for (wa in allWeakness) {
            val existing = weakAreaDao.getWeakAreaByCategory(studentId, wa.category)
            if (existing == null) {
                weakAreaDao.insertWeakArea(wa)
            }
        }
    }

    // 2. Study Plan Generation Step
    // Queries the model to generate a custom step-by-step study plan based on weak areas
    suspend fun generateStudyPlan(studentId: Int): List<StudyPlan> {
        val weakAreaDao = db.weakAreaDao()
        val studyPlanDao = db.studyPlanDao()

        val weakAreas = weakAreaDao.getWeakAreas(studentId)
        val weaknessList = weakAreas.joinToString { "${it.category} (${it.section}, Proficiency: ${it.proficiencyScore}%)" }

        val systemPrompt = """
            You are an expert SAT curriculum designer. Your job is to analyze a student's weak areas and output a structured study plan with EXACTLY 4 ordered steps in JSON format.
            The JSON must be an array of objects, each representing a step:
            [
              {
                "stepOrder": 1,
                "title": "Short title",
                "description": "Short explanation of what to review and practice",
                "category": "The specific category name",
                "estimatedMinutes": 30
              }
            ]
            ONLY output valid JSON. Do not include markdown code block syntax or extra text.
        """.trimIndent()

        val userPrompt = "The student has the following weak areas: $weaknessList. Generate a personalized 4-step study plan."

        try {
            val response = apiService.getCompletions(
                ChatRequest(
                    messages = listOf(
                        Message("system", systemPrompt),
                        Message("user", userPrompt)
                    )
                )
            )

            val rawJson = response.choices.firstOrNull()?.message?.content?.trim() ?: "[]"
            val cleanedJson = cleanJsonString(rawJson)
            val type = object : TypeToken<List<StudyPlanStepDto>>() {}.type
            val stepsDto: List<StudyPlanStepDto> = gson.fromJson(cleanedJson, type)

            val studyPlanSteps = stepsDto.map {
                StudyPlan(
                    studentId = studentId,
                    stepOrder = it.stepOrder,
                    title = it.title,
                    description = it.description,
                    category = it.category,
                    estimatedMinutes = it.estimatedMinutes
                )
            }

            studyPlanDao.deleteStudyPlan(studentId)
            studyPlanDao.insertStudyPlanSteps(studyPlanSteps)
            return studyPlanSteps
        } catch (e: Exception) {
            e.printStackTrace()
            // Return fallback steps if model fails
            val fallback = listOf(
                StudyPlan(studentId, 1, "Master Linear Equations", "Review slope-intercept form and system of linear equations.", "Linear Equations", 30),
                StudyPlan(studentId, 2, "Quadratic Foundations", "Practice factoring, quadratic formula, and graphing.", "Quadratic Equations", 45),
                StudyPlan(studentId, 3, "Inference Strategies", "Learn to identify logical conclusions in Reading passages.", "Inference", 30),
                StudyPlan(studentId, 4, "Grammar Rules Mastery", "Focus on punctuation, pronoun-antecedent agreement, and verb tense.", "Grammar & Punctuation", 30)
            )
            studyPlanDao.deleteStudyPlan(studentId)
            studyPlanDao.insertStudyPlanSteps(fallback)
            return fallback
        }
    }

    // 3. Question Adaptation Step
    // Determines the next topic and difficulty based on last attempt, then generates it from NIM
    suspend fun generateNextAdaptiveQuestion(studentId: Int): GeneratedQuestionDto {
        val attempts = db.attemptDao().getAttempts(studentId)
        val weakAreas = db.weakAreaDao().getWeakAreas(studentId)

        // Select target category
        // Pick the category with the lowest proficiency score, or fallback
        val targetWeakArea = weakAreas.firstOrNull()
        val targetCategory = targetWeakArea?.category ?: "Linear Equations"
        val targetSection = targetWeakArea?.section ?: "Math"

        // Determine adaptive difficulty based on history
        var targetDifficulty = "Medium"
        val recentAttempts = attempts.filter { it.category == targetCategory }
        if (recentAttempts.isNotEmpty()) {
            val lastCorrect = recentAttempts.first().isCorrect
            targetDifficulty = if (lastCorrect) {
                when (recentAttempts.first().difficulty) {
                    "Easy" -> "Medium"
                    "Medium" -> "Hard"
                    "Hard" -> "Hard"
                    else -> "Medium"
                }
            } else {
                when (recentAttempts.first().difficulty) {
                    "Easy" -> "Easy"
                    "Medium" -> "Easy"
                    "Hard" -> "Medium"
                    else -> "Medium"
                }
            }
        }

        val systemPrompt = """
            You are an adaptive SAT tutor generating practice questions.
            Generate a $targetDifficulty level multiple-choice question for the category "$targetCategory" in the $targetSection section.
            The question must contain exactly 4 options (A, B, C, D) and specify the correct answer and a brief explanation.
            Output your response in raw JSON format:
            {
              "question": "The question text",
              "options": {
                "A": "Option A text",
                "B": "Option B text",
                "C": "Option C text",
                "D": "Option D text"
              },
              "correctAnswer": "A",
              "explanation": "Brief explanation of why the correct answer is correct."
            }
            ONLY output raw JSON. Do not include markdown code block markers.
        """.trimIndent()

        val userPrompt = "Generate one $targetDifficulty level SAT practice question for category: $targetCategory"

        try {
            val response = apiService.getCompletions(
                ChatRequest(
                    messages = listOf(
                        Message("system", systemPrompt),
                        Message("user", userPrompt)
                    )
                )
            )

            val rawJson = response.choices.firstOrNull()?.message?.content?.trim() ?: ""
            val cleanedJson = cleanJsonString(rawJson)
            return gson.fromJson(cleanedJson, GeneratedQuestionDto::class.java).copy(
                category = targetCategory,
                section = targetSection,
                difficulty = targetDifficulty
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback question
            return GeneratedQuestionDto(
                question = "Solve the equation: 3x - 7 = 14. What is the value of x?",
                options = mapOf("A" to "5", "B" to "7", "C" to "6", "D" to "8"),
                correctAnswer = "B",
                explanation = "Add 7 to both sides: 3x = 21. Divide by 3: x = 7.",
                category = targetCategory,
                section = targetSection,
                difficulty = targetDifficulty
            )
        }
    }

    // Record an attempt and adapt the proficiency score locally
    suspend fun recordAttempt(
        studentId: Int,
        question: GeneratedQuestionDto,
        selectedAnswer: String
    ): Attempt {
        val attemptDao = db.attemptDao()
        val weakAreaDao = db.weakAreaDao()
        val studentDao = db.studentDao()

        val isCorrect = selectedAnswer == question.correctAnswer
        val attempt = Attempt(
            studentId = studentId,
            section = question.section,
            category = question.category,
            difficulty = question.difficulty,
            questionText = question.question,
            selectedAnswer = selectedAnswer,
            correctAnswer = question.correctAnswer,
            isCorrect = isCorrect
        )
        attemptDao.insertAttempt(attempt)

        // Update student proficiency
        val wa = weakAreaDao.getWeakAreaByCategory(studentId, question.category)
        if (wa != null) {
            val newTotal = wa.totalAttempts + 1
            val newCorrect = wa.correctAttempts + (if (isCorrect) 1 else 0)
            
            // Adjust score
            val change = if (isCorrect) {
                when (question.difficulty) {
                    "Easy" -> 5
                    "Medium" -> 10
                    "Hard" -> 15
                    else -> 10
                }
            } else {
                when (question.difficulty) {
                    "Easy" -> -10
                    "Medium" -> -7
                    "Hard" -> -4
                    else -> -5
                }
            }

            val newProficiency = (wa.proficiencyScore + change).coerceIn(10, 100)
            weakAreaDao.updateWeakArea(
                wa.copy(
                    totalAttempts = newTotal,
                    correctAttempts = newCorrect,
                    proficiencyScore = newProficiency,
                    lastUpdated = System.currentTimeMillis()
                )
            )

            // Adjust student score dynamic
            val student = studentDao.getStudent().firstOrNull()
            if (student != null) {
                val scoreAdjustment = if (isCorrect) 10 else -10
                val (mathAdj, rwAdj) = if (question.section == "Math") {
                    Pair(scoreAdjustment, 0)
                } else {
                    Pair(0, scoreAdjustment)
                }
                studentDao.updateStudent(
                    student.copy(
                        mathScore = (student.mathScore + mathAdj).coerceIn(200, 800),
                        readingWritingScore = (student.readingWritingScore + rwAdj).coerceIn(200, 800),
                        totalScore = ((student.mathScore + mathAdj).coerceIn(200, 800) + (student.readingWritingScore + rwAdj).coerceIn(200, 800)).coerceIn(400, 1600)
                    )
                )
            }
        }
        return attempt
    }

    private fun cleanJsonString(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.substring(7)
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3)
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length - 3)
        }
        return clean.trim()
    }
}

data class StudyPlanStepDto(
    val stepOrder: Int,
    val title: String,
    val description: String,
    val category: String,
    val estimatedMinutes: Int
)

data class GeneratedQuestionDto(
    val question: String,
    val options: Map<String, String>,
    val correctAnswer: String,
    val explanation: String,
    val category: String = "",
    val section: String = "",
    val difficulty: String = ""
)
