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
import com.acesat.education.data.BackendDiscovery
import com.acesat.education.data.SettingsManager
import com.acesat.education.data.api.NvidiaService
import com.acesat.education.data.room.AppDatabase
import com.acesat.education.data.room.Student
import com.acesat.education.ui.components.SettingsDialog
import com.acesat.education.ui.screens.*
import com.acesat.education.ui.theme.AceSATTheme
import kotlinx.coroutines.Dispatchers
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
                    var agent by remember { mutableStateOf(createAgent()) }

                    LaunchedEffect(Unit) {
                        // Run backend auto-discovery in background — non-blocking
                        launch {
                            try {
                                val discoveredIp = BackendDiscovery.discoverServer(context)
                                if (discoveredIp != null && discoveredIp != settingsManager.getBackendIp()) {
                                    settingsManager.setBackendIp(discoveredIp)
                                    agent = createAgent()
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "Connected to AI server at $discoveredIp",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } catch (e: Exception) {
                                // Never crash on discovery failure — just continue
                            }
                        }

                        // Load saved student
                        launch {
                            try {
                                database.studentDao().getStudent().collect { savedStudent ->
                                    if (savedStudent != null) {
                                        student = savedStudent
                                        currentScreen = Screen.Dashboard
                                    }
                                }
                            } catch (e: Exception) {
                                // Ignore DB error on first launch
                            }
                        }
                    }

                    when (val screen = currentScreen) {
                        is Screen.Onboarding -> OnboardingScreen(
                            onStart = { name, targetScore ->
                                scope.launch {
                                    try {
                                        val newStudent = Student(
                                            name = name,
                                            gradeLevel = "11th Grade",
                                            targetScore = targetScore
                                        )
                                        val id = withContext(Dispatchers.IO) {
                                            database.studentDao().insertStudent(newStudent)
                                        }
                                        student = newStudent.copy(id = id.toInt())
                                        currentScreen = Screen.Dashboard
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error saving student: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )

                        is Screen.DiagnosticQuiz -> DiagnosticQuizScreen(
                            studentId = student?.id ?: 0,
                            agent = agent,
                            onQuizComplete = { mathScore, rwScore, results ->
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        agent.runDiagnosis(
                                            student?.id ?: 0,
                                            mapOf("Math" to mathScore, "Reading & Writing" to rwScore)
                                        )
                                        agent.saveDiagnosticWeakAreas(student?.id ?: 0, results)
                                        agent.generateStudyPlan(student?.id ?: 0)
                                    }
                                    Toast.makeText(context, "AI Diagnostic complete!", Toast.LENGTH_SHORT).show()
                                    currentScreen = Screen.Dashboard
                                }
                            },
                            onBack = { currentScreen = Screen.Dashboard }
                        )

                        is Screen.Dashboard -> DashboardScreen(
                            student = student!!,
                            database = database,
                            agent = agent,
                            onStartPractice = { cat, sec -> currentScreen = Screen.Practice(cat, sec) },
                            onStartDiagnostic = { currentScreen = Screen.DiagnosticQuiz }
                        )

                        is Screen.Practice -> PracticeScreen(
                            studentId = student?.id ?: 0,
                            selectedCategory = screen.category,
                            selectedSection = screen.section,
                            agent = agent,
                            database = database,
                            onBackToDashboard = { currentScreen = Screen.Dashboard }
                        )
                    }
                }
            }
        }
    }
}
