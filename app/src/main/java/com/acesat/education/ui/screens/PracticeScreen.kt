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
import com.acesat.education.agent.GeneratedQuestionDto
import com.acesat.education.data.room.AppDatabase
import com.acesat.education.ui.components.NeobrutalistBox
import com.acesat.education.ui.components.NeobrutalistButton
import com.acesat.education.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                errorMessage = "Failed to fetch question from NVIDIA NIM. Check your API settings or network connection."
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
                    modifier = Modifier.align(Alignment.Center),
                    tint = BorderBlack
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = if (selectedCategory != null) "Focus: $selectedCategory" else "Adaptive Practice",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BorderBlack
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
                    Text("Fetching fresh SAT practice...", fontWeight = FontWeight.Bold, color = BorderBlack)
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
                        .fillMaxWidth()
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
                                fontWeight = FontWeight.Bold,
                                color = BorderBlack
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
                        lineHeight = 22.sp,
                        color = BorderBlack
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
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text("EXPLANATION", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BorderBlack)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(q.explanation, fontSize = 13.sp, lineHeight = 18.sp, color = BorderBlack)
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
                    backgroundColor = TealAccent,
                    enabled = selectedAnswer != null
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
