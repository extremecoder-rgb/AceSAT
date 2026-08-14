package com.acesat.education

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.acesat.education.agent.AdaptiveAgent
import com.acesat.education.data.SettingsManager
import com.acesat.education.data.api.NvidiaService
import com.acesat.education.data.room.AppDatabase
import com.acesat.education.data.room.Student
import com.acesat.education.ui.screens.*
import com.acesat.education.ui.theme.AceSATTheme
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
    private lateinit var settingsManager: SettingsManager

    // Dynamically create agent based on whether an API key is provided
    private fun createAgent(): AdaptiveAgent {
        return AdaptiveAgent(database, NvidiaService.create(settingsManager))
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        database = AppDatabase.getDatabase(this)
        settingsManager = SettingsManager(this)

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
                    
                    // Agent state initialized dynamically
                    var agent by remember { mutableStateOf(createAgent()) }

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
                            settingsManager = settingsManager,
                            onStart = { name, targetScore ->
                                scope.launch {
                                    val newStudent = Student(name = name, gradeLevel = "11th Grade", targetScore = targetScore)
                                    val id = withContext(Dispatchers.IO) {
                                        database.studentDao().insertStudent(newStudent)
                                    }
                                    student = newStudent.copy(id = id.toInt())
                                    
                                    // Re-create agent in case they entered an API key
                                    agent = createAgent()
                                    
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
