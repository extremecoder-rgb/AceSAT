package com.acesat.education.ui.screens

import android.graphics.Paint
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
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
    onStartDiagnostic: () -> Unit,
    onOpenSettings: () -> Unit
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
        // Welcome Header & Settings button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeobrutalistBox(
                modifier = Modifier.weight(1f),
                backgroundColor = PinkAccent,
                fillMaxWidth = true // Correctly stretches foreground to match parent width
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("WELCOME BACK,", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BorderBlack)
                        Text(student.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = BorderBlack)
                    }
                    NeobrutalistBox(
                        backgroundColor = CardWhite,
                        cornerRadius = 6,
                        shadowOffset = 2,
                        borderWidth = 2,
                        fillMaxWidth = false // Wraps height/width correctly!
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
            
            Spacer(modifier = Modifier.width(8.dp))
            
            NeobrutalistBox(
                modifier = Modifier
                    .clickable { onOpenSettings() }
                    .size(54.dp),
                backgroundColor = CardWhite,
                cornerRadius = 10,
                shadowOffset = 2,
                borderWidth = 2,
                fillMaxWidth = false
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = BorderBlack,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid scores
        Row(modifier = Modifier.fillMaxWidth()) {
            NeobrutalistBox(
                modifier = Modifier.weight(1f),
                backgroundColor = CardWhite,
                fillMaxWidth = true
            ) {
                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                    Text("MATH SCORE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PurpleAccent)
                    Text("${student.mathScore}/800", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = BorderBlack)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            NeobrutalistBox(
                modifier = Modifier.weight(1f),
                backgroundColor = CardWhite,
                fillMaxWidth = true
            ) {
                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                    Text("READING & WRITING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TealAccent)
                    Text("${student.readingWritingScore}/800", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = BorderBlack)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // TELEMETRY & ANALYTICS DASHBOARD
        Text("PERFORMANCE TELEMETRY", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = BorderBlack)
        Spacer(modifier = Modifier.height(8.dp))
        
        NeobrutalistBox(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = CardWhite,
            fillMaxWidth = true
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Key KPI Metrics
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("TOTAL SOLVED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text("${scoreHistory.size}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = BorderBlack)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        val accuracy = if (scoreHistory.isNotEmpty()) {
                            (scoreHistory.count { it.isCorrect }.toFloat() / scoreHistory.size * 100).toInt()
                        } else 0
                        Text("ACCURACY RATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text("$accuracy%", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = if (accuracy >= 70) TealAccent else Color.Red)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        val mathCorrect = scoreHistory.filter { it.section == "Math" }.count { it.isCorrect }
                        val mathTotal = scoreHistory.filter { it.section == "Math" }.size
                        val mathAcc = if (mathTotal > 0) (mathCorrect * 100) / mathTotal else 0
                        Text("MATH ACCURACY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text("$mathAcc%", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = PurpleAccent)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("SCORE EVOLUTION (OVER TIME)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BorderBlack)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Canvas Line Chart
                val density = LocalDensity.current
                val paint = remember {
                    Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = with(density) { 10.sp.toPx() }
                        textAlign = Paint.Align.RIGHT
                    }
                }
                
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(BackgroundCream)
                        .border(2.dp, BorderBlack)
                ) {
                    val width = size.width
                    val height = size.height
                    
                    // Draw horizontal grid lines (e.g. 400, 800, 1200, 1600 scale)
                    val scaleValues = listOf(1600, 1200, 800, 400)
                    scaleValues.forEachIndexed { idx, valText ->
                        val y = 15.dp.toPx() + (height - 30.dp.toPx()) * idx / 3
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(40.dp.toPx(), y),
                            end = Offset(width - 10.dp.toPx(), y),
                            strokeWidth = 1.dp.toPx()
                        )
                        // Label text
                        drawContext.canvas.nativeCanvas.drawText(
                            valText.toString(),
                            35.dp.toPx(),
                            y + 4.dp.toPx(),
                            paint
                        )
                    }
                    
                    // Plot scores
                    // Build dummy points if empty, else build from actual scoreHistory
                    val points = mutableListOf<Float>()
                    if (student.diagnosticScore > 0) {
                        points.add(student.diagnosticScore.toFloat())
                    }
                    // Cumulative mock estimation over history
                    var currentEst = student.diagnosticScore.toFloat()
                    if (currentEst == 0f) currentEst = 800f // default mid-level start
                    
                    scoreHistory.forEach { attempt ->
                        val weight = if (attempt.isCorrect) 40f else -30f
                        currentEst = (currentEst + weight).coerceIn(400f, 1600f)
                        points.add(currentEst)
                    }
                    
                    if (points.size > 1) {
                        val path = Path()
                        val stepX = (width - 60.dp.toPx()) / (points.size - 1)
                        
                        points.forEachIndexed { i, pScore ->
                            val x = 45.dp.toPx() + i * stepX
                            // Map 400..1600 to chart height
                            val minScore = 400f
                            val maxScore = 1600f
                            val ratio = (pScore - minScore) / (maxScore - minScore)
                            val y = height - 15.dp.toPx() - ratio * (height - 30.dp.toPx())
                            
                            if (i == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                            
                            // Draw circular nodes
                            drawCircle(
                                color = YellowAccent,
                                radius = 4.dp.toPx(),
                                center = Offset(x, y)
                            )
                            drawCircle(
                                color = BorderBlack,
                                radius = 4.dp.toPx(),
                                center = Offset(x, y),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                        
                        // Draw Path line
                        drawPath(
                            path = path,
                            color = PurpleAccent,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    } else {
                        // Display message if not enough history
                        drawContext.canvas.nativeCanvas.drawText(
                            "Complete diagnostic quiz to draw telemetry progress",
                            width / 2f + 15.dp.toPx(),
                            height / 2f + 5.dp.toPx(),
                            Paint().apply {
                                color = android.graphics.Color.DKGRAY
                                textSize = with(density) { 11.sp.toPx() }
                                textAlign = Paint.Align.CENTER
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))

        if (student.diagnosticScore == 0) {
            NeobrutalistBox(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = CardWhite,
                fillMaxWidth = true
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
                backgroundColor = CardWhite,
                fillMaxWidth = true
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
                    backgroundColor = CardWhite,
                    fillMaxWidth = true
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
                    backgroundColor = if (step.isCompleted) TealAccent else CardWhite,
                    fillMaxWidth = true
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NeobrutalistBox(
                                backgroundColor = PurpleAccent,
                                cornerRadius = 4,
                                borderWidth = 1,
                                shadowOffset = 1,
                                fillMaxWidth = false
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
