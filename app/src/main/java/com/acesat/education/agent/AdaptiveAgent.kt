package com.acesat.education.agent

import com.acesat.education.data.api.ChatRequest
import com.acesat.education.data.api.Message
import com.acesat.education.data.api.NvidiaService
import com.acesat.education.data.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.firstOrNull

/**
 * AdaptiveAgent — the AI brain of AceSAT.
 *
 * This agent:
 *  1. Diagnoses student weaknesses via an AI-generated diagnostic quiz
 *  2. Generates a personalized AI study plan
 *  3. Generates real SAT-level questions using NVIDIA Nemotron
 *  4. Adapts difficulty in real-time based on student performance
 *  5. Tracks progress and makes autonomous decisions about what to study next
 */
class AdaptiveAgent(
    private val db: AppDatabase,
    private val apiService: NvidiaService
) {
    private val gson = Gson()

    // All SAT subject domains
    companion object {
        val SAT_MATH_CATEGORIES = listOf(
            "Algebra" to "Linear equations, inequalities, systems of equations, absolute value",
            "Advanced Math" to "Quadratic equations, polynomial functions, exponential growth/decay, radical and rational equations",
            "Problem Solving & Data Analysis" to "Ratios, percentages, probability, statistics, scatterplots, linear/exponential growth models",
            "Geometry & Trigonometry" to "Area, volume, triangles, circles, trigonometric ratios, radians, complex figures"
        )
        val SAT_RW_CATEGORIES = listOf(
            "Information & Ideas" to "Central ideas, command of evidence (textual and quantitative), inferences from passages",
            "Craft & Structure" to "Words in context, text structure and purpose, cross-text connections",
            "Expression of Ideas" to "Rhetorical synthesis, transitions between ideas",
            "Standard English Conventions" to "Sentence boundaries, punctuation (commas, semicolons, colons, dashes), subject-verb agreement, pronoun clarity, verb forms, modifier placement, parallel structure"
        )
    }

    // ======================================================================
    // 1. AI DIAGNOSTIC QUIZ GENERATION
    // ======================================================================
    suspend fun generateDiagnosticQuiz(section: String): List<GeneratedQuestionDto> {
        val categories = if (section == "Math") SAT_MATH_CATEGORIES else SAT_RW_CATEGORIES

        val systemPrompt = """You are an expert SAT exam question writer. Generate a diagnostic quiz with exactly ${categories.size} questions — one question per category listed below.

For the "$section" section of the Digital SAT, create one question for each of these categories:
${categories.mapIndexed { i, (cat, desc) -> "${i+1}. $cat — $desc" }.joinToString("\n")}

CRITICAL REQUIREMENTS:
- Questions MUST be at genuine SAT difficulty level — the kind of questions that appear on the real College Board Digital SAT
- For Reading & Writing questions, include a SHORT passage (2-4 sentences) as context before the question, just like the real SAT
- Each question must have exactly 4 answer choices (A, B, C, D)
- Include the correct answer and a clear explanation

Output ONLY a valid JSON array. No markdown, no code blocks, no extra text.
[
  {
    "question": "Full question text (include passage if R&W)",
    "category": "Exact category name from the list above",
    "section": "$section",
    "difficulty": "Medium",
    "options": {"A": "...", "B": "...", "C": "...", "D": "..."},
    "correctAnswer": "A",
    "explanation": "Clear step-by-step explanation"
  }
]"""

        val userPrompt = "Generate a ${categories.size}-question diagnostic quiz for the SAT $section section. Make each question representative of real SAT difficulty."

        try {
            val response = apiService.getCompletions(
                ChatRequest(
                    messages = listOf(
                        Message("system", systemPrompt),
                        Message("user", userPrompt)
                    ),
                    temperature = 0.8,
                    max_tokens = 8000
                )
            )

            val rawJson = response.choices.firstOrNull()?.message?.content?.trim() ?: "[]"
            val cleanedJson = cleanJsonString(rawJson)
            val type = object : TypeToken<List<GeneratedQuestionDto>>() {}.type
            return gson.fromJson(cleanedJson, type)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    // ======================================================================
    // 2. DIAGNOSIS — Process quiz results and identify weak areas
    // ======================================================================
    suspend fun runDiagnosis(studentId: Int, sectionScores: Map<String, Int>) {
        val studentDao = db.studentDao()
        val weakAreaDao = db.weakAreaDao()

        val student = studentDao.getStudent().firstOrNull() ?: return

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
    }

    // Save weak areas from diagnostic results
    suspend fun saveDiagnosticWeakAreas(studentId: Int, results: List<DiagnosticResult>) {
        val weakAreaDao = db.weakAreaDao()

        for (result in results) {
            val proficiency = if (result.isCorrect) 70 else 30
            val existing = weakAreaDao.getWeakAreaByCategory(studentId, result.category)
            if (existing == null) {
                weakAreaDao.insertWeakArea(
                    WeakArea(
                        studentId = studentId,
                        section = result.section,
                        category = result.category,
                        proficiencyScore = proficiency,
                        totalAttempts = 1,
                        correctAttempts = if (result.isCorrect) 1 else 0
                    )
                )
            }
        }
    }

    // ======================================================================
    // 3. AI STUDY PLAN GENERATION
    // ======================================================================
    suspend fun generateStudyPlan(studentId: Int): List<StudyPlan> {
        val weakAreaDao = db.weakAreaDao()
        val studyPlanDao = db.studyPlanDao()

        val weakAreas = weakAreaDao.getWeakAreas(studentId)
        val weaknessList = weakAreas.joinToString("\n") {
            "- ${it.category} (${it.section}): Proficiency ${it.proficiencyScore}%, Attempts: ${it.totalAttempts}, Correct: ${it.correctAttempts}"
        }

        val systemPrompt = """You are an expert SAT curriculum designer creating a personalized study plan.

Analyze the student's weak areas below and create a focused study plan with EXACTLY 4 ordered steps.
Prioritize the areas with the LOWEST proficiency scores first.

Each step should be specific, actionable, and reference real SAT content domains.
Include specific strategies (e.g., "practice plugging in answer choices", "use process of elimination for inference questions").

Output ONLY a valid JSON array. No markdown, no code blocks.
[
  {
    "stepOrder": 1,
    "title": "Short descriptive title",
    "description": "Specific study instructions with real SAT strategies",
    "category": "The exact category name to focus on",
    "estimatedMinutes": 30
  }
]"""

        val userPrompt = "Student's weak areas:\n$weaknessList\n\nGenerate a personalized 4-step study plan targeting their weakest areas first."

        try {
            val response = apiService.getCompletions(
                ChatRequest(
                    messages = listOf(
                        Message("system", systemPrompt),
                        Message("user", userPrompt)
                    ),
                    temperature = 0.7,
                    max_tokens = 4000
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
            throw e
        }
    }

    // ======================================================================
    // 4. AI ADAPTIVE QUESTION GENERATION — Real SAT questions via NIM
    // ======================================================================
    suspend fun generateNextAdaptiveQuestion(studentId: Int, selectedCategory: String? = null, selectedSection: String? = null): GeneratedQuestionDto {
        val attempts = db.attemptDao().getAttempts(studentId)
        val weakAreas = db.weakAreaDao().getWeakAreas(studentId)

        // Agent decides what to practice: use selected category or pick weakest area
        val targetCategory: String
        val targetSection: String

        if (selectedCategory != null && selectedSection != null) {
            targetCategory = selectedCategory
            targetSection = selectedSection
        } else {
            // Autonomous agent decision: pick the weakest area
            val targetWeakArea = weakAreas.minByOrNull { it.proficiencyScore }
            targetCategory = targetWeakArea?.category ?: "Algebra"
            targetSection = targetWeakArea?.section ?: "Math"
        }

        // Adaptive difficulty based on recent performance history
        var targetDifficulty = "Medium"
        val recentAttempts = attempts.filter { it.category == targetCategory }.take(3)
        if (recentAttempts.isNotEmpty()) {
            val recentAccuracy = recentAttempts.count { it.isCorrect }.toFloat() / recentAttempts.size
            targetDifficulty = when {
                recentAccuracy >= 0.8 -> "Hard"
                recentAccuracy >= 0.5 -> "Medium"
                else -> "Easy"
            }
        }

        // Build context about what the student has already seen
        val previousQuestions = attempts
            .filter { it.category == targetCategory }
            .take(5)
            .joinToString("\n") { "- ${it.questionText.take(80)}..." }

        val categoryDesc = (SAT_MATH_CATEGORIES + SAT_RW_CATEGORIES)
            .firstOrNull { it.first == targetCategory }?.second ?: ""

        val systemPrompt = """You are an expert SAT question writer for the Digital SAT exam administered by College Board.

Generate ONE $targetDifficulty-level multiple-choice question for the "$targetCategory" category in the $targetSection section.

Category covers: $categoryDesc

CRITICAL REQUIREMENTS:
- The question MUST be at genuine SAT difficulty level — exactly like what appears on the real Digital SAT
- For Reading & Writing questions, you MUST include a realistic passage (3-5 sentences) before the question, drawn from topics like science, history, literature, or social studies
- For Math questions, present real-world scenarios or conceptual problems — NOT simple arithmetic
- $targetDifficulty difficulty means: ${when(targetDifficulty) {
            "Easy" -> "straightforward application of concepts, single-step reasoning"
            "Medium" -> "multi-step reasoning, requires understanding of underlying concepts"
            "Hard" -> "complex multi-step problems, requires advanced reasoning or combining multiple concepts"
            else -> "standard SAT level"
        }}
- Exactly 4 answer choices (A, B, C, D) with plausible distractors
- Include a thorough explanation

${if (previousQuestions.isNotEmpty()) "AVOID repeating these previously asked questions:\n$previousQuestions" else ""}

Output ONLY raw JSON. No markdown code blocks.
{
  "question": "Full question text (include passage for R&W)",
  "options": {"A": "...", "B": "...", "C": "...", "D": "..."},
  "correctAnswer": "A",
  "explanation": "Step-by-step explanation"
}"""

        val userPrompt = "Generate one $targetDifficulty SAT $targetSection question for the category: $targetCategory"

        try {
            val response = apiService.getCompletions(
                ChatRequest(
                    messages = listOf(
                        Message("system", systemPrompt),
                        Message("user", userPrompt)
                    ),
                    temperature = 0.9,
                    max_tokens = 4000
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
            throw e
        }
    }

    // ======================================================================
    // 5. RECORD ATTEMPT & ADAPT PROFICIENCY
    // ======================================================================
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

        // Update weak area proficiency
        val wa = weakAreaDao.getWeakAreaByCategory(studentId, question.category)
        if (wa != null) {
            val newTotal = wa.totalAttempts + 1
            val newCorrect = wa.correctAttempts + (if (isCorrect) 1 else 0)
            val change = if (isCorrect) {
                when (question.difficulty) { "Easy" -> 5; "Medium" -> 10; "Hard" -> 15; else -> 10 }
            } else {
                when (question.difficulty) { "Easy" -> -10; "Medium" -> -7; "Hard" -> -4; else -> -5 }
            }

            val newProficiency = (wa.proficiencyScore + change).coerceIn(5, 100)
            weakAreaDao.updateWeakArea(
                wa.copy(
                    totalAttempts = newTotal,
                    correctAttempts = newCorrect,
                    proficiencyScore = newProficiency,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        } else {
            // Create new weak area entry if it doesn't exist
            weakAreaDao.insertWeakArea(
                WeakArea(
                    studentId = studentId,
                    section = question.section,
                    category = question.category,
                    proficiencyScore = if (isCorrect) 60 else 30,
                    totalAttempts = 1,
                    correctAttempts = if (isCorrect) 1 else 0
                )
            )
        }

        // Update student scores
        val student = studentDao.getStudent().firstOrNull()
        if (student != null) {
            val scoreAdj = if (isCorrect) 10 else -10
            val (mathAdj, rwAdj) = if (question.section == "Math") Pair(scoreAdj, 0) else Pair(0, scoreAdj)
            studentDao.updateStudent(
                student.copy(
                    mathScore = (student.mathScore + mathAdj).coerceIn(200, 800),
                    readingWritingScore = (student.readingWritingScore + rwAdj).coerceIn(200, 800),
                    totalScore = ((student.mathScore + mathAdj).coerceIn(200, 800) +
                            (student.readingWritingScore + rwAdj).coerceIn(200, 800)).coerceIn(400, 1600)
                )
            )
        }
        return attempt
    }

    private fun cleanJsonString(raw: String): String {
        var clean = raw.trim()
        // Remove <think>...</think> blocks from reasoning models
        val thinkEnd = clean.indexOf("</think>")
        if (thinkEnd != -1) {
            clean = clean.substring(thinkEnd + 8).trim()
        }
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

data class DiagnosticResult(
    val category: String,
    val section: String,
    val isCorrect: Boolean
)
