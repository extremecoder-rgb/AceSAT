package com.acesat.education.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acesat.education.agent.AdaptiveAgent
import com.acesat.education.data.room.*
import com.acesat.education.ui.components.NeobrutalistBox
import com.acesat.education.ui.components.NeobrutalistButton
import com.acesat.education.ui.theme.*
import kotlinx.coroutines.launch

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
        launch { database.weakAreaDao().getWeakAreasFlow(student.id).collect { weakAreas = it } }
        launch { database.studyPlanDao().getStudyPlansFlow(student.id).collect { studyPlans = it } }
        launch { database.attemptDao().getAttemptsFlow(student.id).collect { scoreHistory = it } }
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
                modifier = Modifier.fillMaxWidth().padding(16.dp), // Added fillMaxWidth to prevent infinite layout height issue
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("WELCOME BACK,", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BorderBlack)
                    Text(student.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = BorderBlack)
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
                        fontWeight = FontWeight.ExtraBold,
                        color = BorderBlack
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
                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                    Text("MATH", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PurpleAccent)
                    Text("${student.mathScore}/800", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = BorderBlack)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            NeobrutalistBox(
                modifier = Modifier.weight(1f),
                backgroundColor = CardWhite
            ) {
                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                    Text("READING & WRITING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TealAccent)
                    Text("${student.readingWritingScore}/800", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = BorderBlack)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (student.diagnosticScore == 0) {
            NeobrutalistBox(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = CardWhite
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("TAKE AI DIAGNOSTIC ASSESSMENT", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = BorderBlack)
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
            NeobrutalistBox(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = CardWhite
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("START SAT PRACTICE", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = BorderBlack)
                    Spacer(modifier = Modifier.height(12.dp))

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

                    Text("Math Domains", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BorderBlack)
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
                        Divider(color = Color.LightGray)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Reading & Writing Domains", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BorderBlack)
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
                        Divider(color = Color.LightGray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("WEAK AREAS & PROFICIENCY", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = BorderBlack)
        Spacer(modifier = Modifier.height(8.dp))
        if (weakAreas.isEmpty()) {
            Text("No weak areas identified. Take the AI Diagnostic or start practicing!", fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = Color.DarkGray)
        } else {
            weakAreas.forEach { wa ->
                NeobrutalistBox(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    backgroundColor = CardWhite
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(wa.category, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BorderBlack)
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

        Text("AI PERSONALIZED STUDY PLAN", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = BorderBlack)
        Spacer(modifier = Modifier.height(8.dp))
        if (studyPlans.isEmpty()) {
            Text("Complete the diagnostic assessment to unlock your personalized plan.", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 13.sp, color = Color.DarkGray)
        } else {
            studyPlans.forEach { step ->
                NeobrutalistBox(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    backgroundColor = if (step.isCompleted) TealAccent else CardWhite
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(30.dp))
    }
}
