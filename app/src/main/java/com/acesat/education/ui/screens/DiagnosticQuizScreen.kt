package com.acesat.education.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acesat.education.agent.AdaptiveAgent
import com.acesat.education.agent.DiagnosticResult
import com.acesat.education.agent.GeneratedQuestionDto
import com.acesat.education.ui.components.NeobrutalistBox
import com.acesat.education.ui.components.NeobrutalistButton
import com.acesat.education.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                errorMessage = "Failed to generate AI Diagnostic Quiz.\nPlease check your local Node proxy connection or enter your API key in Settings."
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
                    modifier = Modifier.align(Alignment.Center),
                    tint = BorderBlack
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("AI Diagnostic", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = BorderBlack)
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
                        "NVIDIA NIM is drafting authentic SAT questions...",
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = BorderBlack
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
                    Spacer(modifier = Modifier.height(8.dp))
                    NeobrutalistButton(onClick = onBack, backgroundColor = Color.Gray) {
                        Text("GO BACK", color = CardWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (quizQuestions.isNotEmpty()) {
            val currentQuestion = quizQuestions[currentQuestionIdx]

            Text(
                text = "Question ${currentQuestionIdx + 1} of ${quizQuestions.size}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = BorderBlack,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            NeobrutalistBox(
                modifier = Modifier.fillMaxWidth().weight(1f),
                backgroundColor = CardWhite
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
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
                        lineHeight = 22.sp,
                        color = BorderBlack
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
                            val mathQuestions = userAnswers.filter { it.section == "Math" }
                            val rwQuestions = userAnswers.filter { it.section == "Reading & Writing" }

                            val mathCorrectCount = mathQuestions.count { it.isCorrect }
                            val rwCorrectCount = rwQuestions.count { it.isCorrect }

                            val mathScore = 200 + ((mathCorrectCount.toFloat() / mathQuestions.size) * 600).toInt()
                            val rwScore = 200 + ((rwCorrectCount.toFloat() / rwQuestions.size) * 600).toInt()

                            onQuizComplete(mathScore, rwScore, userAnswers.toList())
                        }
                    }
                },
                backgroundColor = TealAccent,
                enabled = selectedAnswer != null
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
