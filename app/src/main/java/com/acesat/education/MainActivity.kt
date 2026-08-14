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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acesat.education.agent.AdaptiveAgent
import com.acesat.education.agent.GeneratedQuestionDto
import com.acesat.education.agent.DiagnosticResult
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
    data class Practice(val category: String? = null, val section: String? = null) : Screen()
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
                    val scope = rememberCoroutineScope()

                    // Fetch student on start
                    LaunchedEffect(Unit) {
                        launch {
                            database.studentDao().getStudent().collect {
                                student = it
                                if (it != null) {
                                    currentScreen = Screen.Dashboard
                                }
                            }
                        }
                    }

                    when (val screen = currentScreen) {
                        is Screen.Onboarding -> OnboardingScreen(
                            onStart = { name, targetScore ->
                                scope.launch {
                                    val newStudent = Student(name = name, gradeLevel = "11th Grade", targetScore = targetScore)
                                    val id = withContext(Dispatchers.IO) {
                                        database.studentDao().insertStudent(newStudent)
                                    }
                                    student = newStudent.copy(id = id.toInt())
                                    // Go directly to Dashboard first!
                                    currentScreen = Screen.Dashboard
                                }
                            }
                        )
                        is Screen.DiagnosticQuiz -> DiagnosticQuizScreen(
                            studentId = student?.id ?: 0,
                            agent = agent,
                            onQuizComplete = { mathScore, rwScore, results ->
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        agent.runDiagnosis(student?.id ?: 0, mapOf("Math" to mathScore, "Reading & Writing" to rwScore))
                                        agent.saveDiagnosticWeakAreas(student?.id ?: 0, results)
                                        agent.generateStudyPlan(student?.id ?: 0)
                                    }
                                    Toast.makeText(context, "AI Diagnostic evaluation complete!", Toast.LENGTH_SHORT).show()
                                    currentScreen = Screen.Dashboard
                                }
                            },
                            onBack = { currentScreen = Screen.Dashboard }
                        )
                        is Screen.Dashboard -> DashboardScreen(
                            student = student!!,
                            database = database,
                            agent = agent,
                            onStartPractice = { cat, sec ->
                                currentScreen = Screen.Practice(cat, sec)
                            },
                            onStartDiagnostic = {
                                currentScreen = Screen.DiagnosticQuiz
                            }
                        )
                        is Screen.Practice -> PracticeScreen(
                            studentId = student?.id ?: 0,
                            selectedCategory = screen.category,
                            selectedSection = screen.section,
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

// Neobrutalist UI Card with shadow fix
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
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset.dp, y = shadowOffset.dp)
                .background(shadowColor, shape = RoundedCornerShape(cornerRadius.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth() // Fixes the background/shadow alignment issue
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
fun OnboardingScreen(onStart: (String, Int) -> Unit) {
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
                    onStart(name, target)
                }
            },
            backgroundColor = PurpleAccent
        ) {
            Text("ENTER DASHBOARD", fontWeight = FontWeight.ExtraBold, color = CardWhite)
        }
    }
}

@Composable
fun DiagnosticQuizScreen(
    studentId: Int,
    agent: AdaptiveAgent,
    onQuizComplete: (mathScore: Int, rwScore: Int, results: List<DiagnosticResult>) -> Unit,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val quizQuestions = remember { mutableStateListOf<GeneratedQuestionDto>() }
    var currentQuestionIdx by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    val userAnswers = remember { mutableStateListOf<DiagnosticResult>() }
    val scope = rememberCoroutineScope()

    fun loadQuiz() {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val mathQs = withContext(Dispatchers.IO) { agent.generateDiagnosticQuiz("Math") }
                val rwQs = withContext(Dispatchers.IO) { agent.generateDiagnosticQuiz("Reading & Writing") }
                quizQuestions.clear()
                quizQuestions.addAll(mathQs + rwQs)
                isLoading = false
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Failed to generate AI Diagnostic Quiz. Make sure your local Node proxy is running and configured with your NVIDIA API key."
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadQuiz()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream)
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeobrutalistBox(
                modifier = Modifier.clickable { onBack() }.size(40.dp),
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
            Text("AI Diagnostic", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }

        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = PurpleAccent)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "NVIDIA NIM is writing authentic SAT questions...",
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(errorMessage!!, textAlign = TextAlign.Center, color = Color.Red, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    NeobrutalistButton(onClick = { loadQuiz() }, backgroundColor = TealAccent) {
                        Text("RETRY", color = CardWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (quizQuestions.isNotEmpty()) {
            val currentQuestion = quizQuestions[currentQuestionIdx]

            Text(
                text = "Question ${currentQuestionIdx + 1} of ${quizQuestions.size}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

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
                        text = "${currentQuestion.section.uppercase()} — ${currentQuestion.category.uppercase()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealAccent
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentQuestion.question,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    currentQuestion.options.forEach { (key, value) ->
                        val isSelected = selectedAnswer == key
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .background(if (isSelected) PurpleAccent else CardWhite, shape = RoundedCornerShape(8.dp))
                                .border(2.dp, BorderBlack, shape = RoundedCornerShape(8.dp))
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
                        userAnswers.add(
                            DiagnosticResult(
                                category = currentQuestion.category,
                                section = currentQuestion.section,
                                isCorrect = isCorrect
                            )
                        )
                        selectedAnswer = null

                        if (currentQuestionIdx < quizQuestions.size - 1) {
                            currentQuestionIdx++
                        } else {
                            // Calculate scores
                            val mathQuestions = userAnswers.filter { it.section == "Math" }
                            val rwQuestions = userAnswers.filter { it.section == "Reading & Writing" }

                            val mathCorrectCount = mathQuestions.count { it.isCorrect }
                            val rwCorrectCount = rwQuestions.count { it.isCorrect }

                            // Map correct ratio to 200-800 scale
                            val mathScore = 200 + ((mathCorrectCount.toFloat() / mathQuestions.size) * 600).toInt()
                            val rwScore = 200 + ((rwCorrectCount.toFloat() / rwQuestions.size) * 600).toInt()

                            onQuizComplete(mathScore, rwScore, userAnswers.toList())
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
}

@Composable
fun DashboardScreen(
    student: Student,
    database: AppDatabase,
    agent: AdaptiveAgent,
    onStartPractice: (category: String?, section: String?) -> Unit,
    onStartDiagnostic: () -> Unit
) {
    var weakAreas by remember { mutableStateOf<List<WeakArea>>(emptyList()) }
    var studyPlans by remember { mutableStateOf<List<StudyPlan>>(emptyList()) }
    var scoreHistory by remember { mutableStateOf<List<Attempt>>(emptyList()) }

    LaunchedEffect(student.id) {
        launch {
            database.weakAreaDao().getWeakAreasFlow(student.id).collect { weakAreas = it }
        }
        launch {
            database.studyPlanDao().getStudyPlansFlow(student.id).collect { studyPlans = it }
        }
        launch {
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
            // First item
            NeobrutalistBox(
                modifier = Modifier.weight(1f),
                backgroundColor = CardWhite
            ) {
                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                    Text("MATH", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PurpleAccent)
                    Text("${student.mathScore}/800", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            // Second item
            NeobrutalistBox(
                modifier = Modifier.weight(1f),
                backgroundColor = CardWhite
            ) {
                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                    Text("READING & WRITING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TealAccent)
                    Text("${student.readingWritingScore}/800", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (student.diagnosticScore == 0) {
            // Un-diagnosed Prompt
            NeobrutalistBox(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = CardWhite
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("TAKE AI DIAGNOSTIC ASSESSMENT", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Let NVIDIA NIM generate a diagnostic test to identify your specific SAT strengths and weaknesses.",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    NeobrutalistButton(onClick = onStartDiagnostic, backgroundColor = PurpleAccent) {
                        Text("START AI DIAGNOSTIC", fontWeight = FontWeight.Bold, color = CardWhite)
                    }
                }
            }
        } else {
            // Practice Picker
            NeobrutalistBox(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = CardWhite
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("START SAT PRACTICE", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Adaptive button
                    NeobrutalistButton(
                        onClick = { onStartPractice(null, null) },
                        backgroundColor = PurpleAccent
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = CardWhite)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ADAPTIVE PRACTICE (AI DECIDES)", fontWeight = FontWeight.Bold, color = CardWhite)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("PRACTICE SPECIFIC DOMAINS:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Math domain buttons
                    Text("Math Domains", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    AdaptiveAgent.SAT_MATH_CATEGORIES.forEach { (cat, _) ->
                        Text(
                            text = cat,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStartPractice(cat, "Math") }
                                .padding(vertical = 8.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = PurpleAccent
                        )
                        Divider()
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // R&W domain buttons
                    Text("Reading & Writing Domains", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    AdaptiveAgent.SAT_RW_CATEGORIES.forEach { (cat, _) ->
                        Text(
                            text = cat,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStartPractice(cat, "Reading & Writing") }
                                .padding(vertical = 8.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TealAccent
                        )
                        Divider()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Weak Areas Card
        Text("WEAK AREAS & PROFICIENCY", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(8.dp))
        if (weakAreas.isEmpty()) {
            Text("No weak areas identified. Take the AI Diagnostic or start practicing!", fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        } else {
            weakAreas.forEach { wa ->
                NeobrutalistBox(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    backgroundColor = CardWhite
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
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
            Text("Complete the diagnostic assessment to unlock your personalized plan.", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 13.sp)
        } else {
            studyPlans.forEach { step ->
                NeobrutalistBox(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
                            text = "Estimated: ${step.estimatedMinutes} mins | Focus: ${step.category}",
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
            Text("Start practicing to log attempt history.", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 13.sp)
        } else {
            scoreHistory.take(5).forEach { attempt ->
                NeobrutalistBox(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    backgroundColor = CardWhite
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
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
    selectedCategory: String?,
    selectedSection: String?,
    agent: AdaptiveAgent,
    database: AppDatabase,
    onBackToDashboard: () -> Unit
) {
    var currentQuestion by remember { mutableStateOf<GeneratedQuestionDto?>(null) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadNextQuestion() {
        isLoading = true
        errorMessage = null
        selectedAnswer = null
        isSubmitted = false
        scope.launch {
            try {
                val q = withContext(Dispatchers.IO) {
                    agent.generateNextAdaptiveQuestion(studentId, selectedCategory, selectedSection)
                }
                currentQuestion = q
                isLoading = false
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Failed to fetch question from NVIDIA NIM. Check your server proxy connection."
                isLoading = false
            }
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
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeobrutalistBox(
                modifier = Modifier.clickable { onBackToDashboard() }.size(40.dp),
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
            Text(
                text = if (selectedCategory != null) "Focus: $selectedCategory" else "Adaptive Practice",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = PurpleAccent)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Fetching fresh SAT practice...", fontWeight = FontWeight.Bold)
                }
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(errorMessage!!, textAlign = TextAlign.Center, color = Color.Red, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    NeobrutalistButton(onClick = { loadNextQuestion() }, backgroundColor = PurpleAccent) {
                        Text("RETRY", color = CardWhite, fontWeight = FontWeight.Bold)
                    }
                }
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp
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
                            scope.launch {
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
