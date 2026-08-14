package com.acesat.education

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.acesat.education.agent.AdaptiveAgent
import com.acesat.education.agent.GeneratedQuestionDto
import com.acesat.education.data.api.NvidiaService
import com.acesat.education.data.room.*
import com.acesat.education.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class Screen {
    object Onboarding : Screen()
    object DiagnosticQuiz : Screen()
    object Dashboard : Screen()
    object Practice : Screen()
}

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var agent: AdaptiveAgent

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        database = AppDatabase.getDatabase(this)
        agent = AdaptiveAgent(database, NvidiaService.create())

        setContent {
            AceSATTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Onboarding) }
                    var student by remember { mutableStateOf<Student?>(null) }
                    val context = LocalContext.current

                    // Fetch student on start
                    LaunchedEffect(Unit) {
                        lifecycleScope.launch {
                            database.studentDao().getStudent().collect {
                                student = it
                                if (it != null) {
                                    currentScreen = Screen.Dashboard
                                }
                            }
                        }
                    }

                    when (currentScreen) {
                        is Screen.Onboarding -> OnboardingScreen(
                            onStartQuiz = { name, targetScore ->
                                lifecycleScope.launch {
                                    val newStudent = Student(name = name, gradeLevel = "11th Grade", targetScore = targetScore)
                                    val id = withContext(Dispatchers.IO) {
                                        database.studentDao().insertStudent(newStudent)
                                    }
                                    student = newStudent.copy(id = id.toInt())
                                    currentScreen = Screen.DiagnosticQuiz
                                }
                            }
                        )
                        is Screen.DiagnosticQuiz -> DiagnosticQuizScreen(
                            studentId = student?.id ?: 0,
                            onQuizComplete = { scores ->
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        agent.runDiagnosis(student?.id ?: 0, scores)
                                        agent.generateStudyPlan(student?.id ?: 0)
                                    }
                                    Toast.makeText(context, "Diagnosis complete! Study plan generated.", Toast.LENGTH_SHORT).show()
                                    currentScreen = Screen.Dashboard
                                }
                            }
                        )
                        is Screen.Dashboard -> DashboardScreen(
                            student = student!!,
                            database = database,
                            agent = agent,
                            onStartPractice = {
                                currentScreen = Screen.Practice
                            }
                        )
                        is Screen.Practice -> PracticeScreen(
                            studentId = student?.id ?: 0,
                            agent = agent,
                            database = database,
                            onBackToDashboard = {
                                currentScreen = Screen.Dashboard
                            }
                        )
                    }
                }
            }
        }
    }
}

// Custom Neobrutalist Container with border and drop shadow
@Composable
fun NeobrutalistBox(
    modifier: Modifier = Modifier,
    backgroundColor: Color = CardWhite,
    borderColor: Color = BorderBlack,
    shadowColor: Color = BorderBlack,
    borderWidth: Int = 3,
    shadowOffset: Int = 5,
    cornerRadius: Int = 12,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.padding(bottom = shadowOffset.dp, end = shadowOffset.dp)) {
        // Shadow layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset.dp, y = shadowOffset.dp)
                .background(shadowColor, shape = RoundedCornerShape(cornerRadius.dp))
        )
        // Foreground layer
        Box(
            modifier = Modifier
                .background(backgroundColor, shape = RoundedCornerShape(cornerRadius.dp))
                .border(borderWidth.dp, borderColor, shape = RoundedCornerShape(cornerRadius.dp))
                .clip(RoundedCornerShape(cornerRadius.dp)),
            content = content
        )
    }
}

@Composable
fun NeobrutalistButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = PurpleAccent,
    content: @Composable RowScope.() -> Unit
) {
    NeobrutalistBox(
        modifier = modifier.clickable { onClick() },
        backgroundColor = backgroundColor,
        cornerRadius = 8,
        shadowOffset = 4
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 18.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onStartQuiz: (String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var targetScoreText by remember { mutableStateOf("1400") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NeobrutalistBox(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = PinkAccent
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "AceSAT AI Tutor",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BorderBlack
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Personalized SAT prep that adapts to you in real-time.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = BorderBlack
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        NeobrutalistBox(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("STUDENT NAME", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Enter your name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = BorderBlack,
                        unfocusedBorderColor = BorderBlack
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("TARGET SAT SCORE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = targetScoreText,
                    onValueChange = { targetScoreText = it },
                    placeholder = { Text("e.g. 1400") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = BorderBlack,
                        unfocusedBorderColor = BorderBlack
                    ),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        NeobrutalistButton(
            onClick = {
                val target = targetScoreText.toIntOrNull() ?: 1200
                if (name.isNotBlank()) {
                    onStartQuiz(name, target)
                }
            },
            backgroundColor = PurpleAccent
        ) {
            Text("START DIAGNOSTIC QUIZ", fontWeight = FontWeight.ExtraBold, color = CardWhite)
        }
    }
}

@Composable
fun DiagnosticQuizScreen(studentId: Int, onQuizComplete: (Map<String, Int>) -> Unit) {
    var currentQuestionIdx by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    val answers = remember { mutableStateListOf<Boolean>() }

    val quizQuestions = listOf(
        DiagnosticQuestion(
            section = "Math",
            category = "Linear Equations",
            text = "If 3x - 4 = 11, what is the value of 2x + 5?",
            options = mapOf("A" to "10", "B" to "15", "C" to "12", "D" to "13"),
            correctAnswer = "B"
        ),
        DiagnosticQuestion(
            section = "Math",
            category = "Quadratic Equations",
            text = "For which of the following values of x is the equation x^2 - 5x + 6 = 0 true?",
            options = mapOf("A" to "1", "B" to "4", "C" to "3", "D" to "5"),
            correctAnswer = "C"
        ),
        DiagnosticQuestion(
            section = "Reading & Writing",
            category = "Inference",
            text = "A study shows that students who read fiction score higher on vocabulary tests. What is the most logical inference from this finding?",
            options = mapOf(
                "A" to "Reading fiction directly increases vocabulary size.",
                "B" to "There is a positive correlation between reading fiction and vocabulary strength.",
                "C" to "Fiction readers are generally better at math.",
                "D" to "Non-fiction has no impact on vocabulary development."
            ),
            correctAnswer = "B"
        )
    )

    val currentQuestion = quizQuestions[currentQuestionIdx]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream)
            .padding(24.dp)
    ) {
        Text(
            text = "Diagnostic Quiz (${currentQuestionIdx + 1}/${quizQuestions.size})",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Question Card
        NeobrutalistBox(
            modifier = Modifier.fillMaxWidth().weight(1f),
            backgroundColor = CardWhite
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = currentQuestion.section.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TealAccent
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentQuestion.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(24.dp))

                currentQuestion.options.forEach { (key, value) ->
                    val isSelected = selectedAnswer == key
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(
                                if (isSelected) PurpleAccent else CardWhite,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                2.dp,
                                BorderBlack,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedAnswer = key }
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "$key. $value",
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) CardWhite else BorderBlack
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        NeobrutalistButton(
            onClick = {
                if (selectedAnswer != null) {
                    val isCorrect = selectedAnswer == currentQuestion.correctAnswer
                    answers.add(isCorrect)
                    selectedAnswer = null

                    if (currentQuestionIdx < quizQuestions.size - 1) {
                        currentQuestionIdx++
                    } else {
                        // Diagnostic quiz completed, compile scores
                        var mathScore = 400
                        var rwScore = 400
                        
                        // Simple score mapping
                        if (answers[0]) mathScore += 100
                        if (answers[1]) mathScore += 100
                        if (answers[2]) rwScore += 200

                        onQuizComplete(mapOf("Math" to mathScore, "Reading & Writing" to rwScore))
                    }
                }
            },
            backgroundColor = TealAccent
        ) {
            Text(
                text = if (currentQuestionIdx == quizQuestions.size - 1) "FINISH ASSESSMENT" else "NEXT QUESTION",
                fontWeight = FontWeight.ExtraBold,
                color = CardWhite
            )
        }
    }
}

data class DiagnosticQuestion(
    val section: String,
    val category: String,
    val text: String,
    val options: Map<String, String>,
    val correctAnswer: String
)

@Composable
fun DashboardScreen(
    student: Student,
    database: AppDatabase,
    agent: AdaptiveAgent,
    onStartPractice: () -> Unit
) {
    var weakAreas by remember { mutableStateOf<List<WeakArea>>(emptyList()) }
    var studyPlans by remember { mutableStateOf<List<StudyPlan>>(emptyList()) }
    var scoreHistory by remember { mutableStateOf<List<Attempt>>(emptyList()) }

    LaunchedEffect(student.id) {
        lifecycleScope {
            database.weakAreaDao().getWeakAreasFlow(student.id).collect { weakAreas = it }
        }
        lifecycleScope {
            database.studyPlanDao().getStudyPlansFlow(student.id).collect { studyPlans = it }
        }
        lifecycleScope {
            database.attemptDao().getAttemptsFlow(student.id).collect { scoreHistory = it }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Welcome Header
        NeobrutalistBox(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = PinkAccent
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("WELCOME BACK,", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(student.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                }
                NeobrutalistBox(
                    backgroundColor = CardWhite,
                    cornerRadius = 6,
                    shadowOffset = 2,
                    borderWidth = 2
                ) {
                    Text(
                        text = "${student.totalScore}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid scores
        Row(modifier = Modifier.fillMaxWidth()) {
            NeobrutalistBox(
                modifier = Modifier.weight(1f),
                backgroundColor = CardWhite
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("MATH", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PurpleAccent)
                    Text("${student.mathScore}/800", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            NeobrutalistBox(
                modifier = Modifier.weight(1f),
                backgroundColor = CardWhite
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("READING & WRITING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TealAccent)
                    Text("${student.readingWritingScore}/800", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Start Adaptive Practice Button
        NeobrutalistButton(
            onClick = onStartPractice,
            backgroundColor = PurpleAccent
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = CardWhite)
            Spacer(modifier = Modifier.width(8.dp))
            Text("START ADAPTIVE PRACTICE", fontWeight = FontWeight.ExtraBold, color = CardWhite)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Weak Areas Card
        Text("WEAK AREAS & PROFICIENCY", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(8.dp))
        if (weakAreas.isEmpty()) {
            Text("No weak areas identified. Good job!", fontSize = 14.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        } else {
            weakAreas.forEach { wa ->
                NeobrutalistBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    backgroundColor = CardWhite
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(wa.category, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(wa.section, fontSize = 11.sp, color = Color.Gray)
                        }
                        Text(
                            text = "${wa.proficiencyScore}%",
                            fontWeight = FontWeight.ExtraBold,
                            color = if (wa.proficiencyScore < 50) Color.Red else TealAccent
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Study Plan Card
        Text("AI PERSONALIZED STUDY PLAN", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(8.dp))
        if (studyPlans.isEmpty()) {
            Text("Analyzing diagnostic and generating study steps...", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        } else {
            studyPlans.forEach { step ->
                NeobrutalistBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    backgroundColor = if (step.isCompleted) TealAccent else CardWhite
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NeobrutalistBox(
                                backgroundColor = PurpleAccent,
                                cornerRadius = 4,
                                borderWidth = 1,
                                shadowOffset = 1
                            ) {
                                Text(
                                    text = "Step ${step.stepOrder}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CardWhite
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = step.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (step.isCompleted) CardWhite else BorderBlack
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = step.description,
                            fontSize = 13.sp,
                            color = if (step.isCompleted) CardWhite else Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Estimated: ${step.estimatedMinutes} mins",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (step.isCompleted) CardWhite else TealAccent
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Progress History
        Text("SCORE PROGRESSION LOG", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(8.dp))
        if (scoreHistory.isEmpty()) {
            Text("Start practicing to see your attempt progression.", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        } else {
            scoreHistory.take(5).forEach { attempt ->
                NeobrutalistBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    backgroundColor = CardWhite
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (attempt.isCorrect) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = "Status",
                            tint = if (attempt.isCorrect) TealAccent else Color.Red
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(attempt.category, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Difficulty: ${attempt.difficulty}", fontSize = 11.sp, color = Color.Gray)
                        }
                        Text(
                            text = if (attempt.isCorrect) "+10 pts" else "-10 pts",
                            fontWeight = FontWeight.Bold,
                            color = if (attempt.isCorrect) TealAccent else Color.Red
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun PracticeScreen(
    studentId: Int,
    agent: AdaptiveAgent,
    database: AppDatabase,
    onBackToDashboard: () -> Unit
) {
    var currentQuestion by remember { mutableStateOf<GeneratedQuestionDto?>(null) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch next adaptive question
    fun loadNextQuestion() {
        isLoading = true
        selectedAnswer = null
        isSubmitted = false
        lifecycleScope {
            val q = withContext(Dispatchers.IO) {
                agent.generateNextAdaptiveQuestion(studentId)
            }
            currentQuestion = q
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadNextQuestion()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream)
            .padding(16.dp)
    ) {
        // Top Nav
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeobrutalistBox(
                modifier = Modifier
                    .clickable { onBackToDashboard() }
                    .size(40.dp),
                cornerRadius = 6,
                shadowOffset = 2
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Practice Room", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }

        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PurpleAccent
                )
            }
        } else if (currentQuestion != null) {
            val q = currentQuestion!!

            NeobrutalistBox(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                backgroundColor = CardWhite
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Badge Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        NeobrutalistBox(
                            backgroundColor = PinkAccent,
                            cornerRadius = 4,
                            borderWidth = 1,
                            shadowOffset = 1
                        ) {
                            Text(
                                text = q.category,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        NeobrutalistBox(
                            backgroundColor = TealAccent,
                            cornerRadius = 4,
                            borderWidth = 1,
                            shadowOffset = 1
                        ) {
                            Text(
                                text = q.difficulty,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CardWhite
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = q.question,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Options list
                    q.options.forEach { (key, value) ->
                        val isSelected = selectedAnswer == key
                        val optionBg = if (isSubmitted) {
                            if (key == q.correctAnswer) TealAccent
                            else if (isSelected) Color.Red
                            else CardWhite
                        } else {
                            if (isSelected) PurpleAccent else CardWhite
                        }
                        val optionText = if (isSelected || (isSubmitted && key == q.correctAnswer)) CardWhite else BorderBlack

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .background(optionBg, shape = RoundedCornerShape(8.dp))
                                .border(2.dp, BorderBlack, shape = RoundedCornerShape(8.dp))
                                .clickable(enabled = !isSubmitted) { selectedAnswer = key }
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "$key. $value",
                                fontWeight = FontWeight.SemiBold,
                                color = optionText
                            )
                        }
                    }

                    if (isSubmitted) {
                        Spacer(modifier = Modifier.height(20.dp))
                        NeobrutalistBox(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = PinkAccent
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("EXPLANATION", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(q.explanation, fontSize = 13.sp, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!isSubmitted) {
                NeobrutalistButton(
                    onClick = {
                        if (selectedAnswer != null) {
                            isSubmitted = true
                            lifecycleScope {
                                withContext(Dispatchers.IO) {
                                    agent.recordAttempt(studentId, q, selectedAnswer!!)
                                }
                            }
                        }
                    },
                    backgroundColor = TealAccent
                ) {
                    Text("SUBMIT ANSWER", fontWeight = FontWeight.ExtraBold, color = CardWhite)
                }
            } else {
                NeobrutalistButton(
                    onClick = {
                        loadNextQuestion()
                    },
                    backgroundColor = PurpleAccent
                ) {
                    Text("NEXT QUESTION", fontWeight = FontWeight.ExtraBold, color = CardWhite)
                }
            }
        }
    }
}

// Extension to trigger coroutines from Composables using lifecycleScope
@Composable
fun rememberLifecycleScope(): kotlinx.coroutines.CoroutineScope {
    val context = LocalContext.current
    return (context as ComponentActivity).lifecycleScope
}

inline fun ComponentActivity.lifecycleScope(crossinline block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {
    this.lifecycleScope.launch {
        block()
    }
}
