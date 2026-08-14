package com.acesat.education.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acesat.education.ui.components.NeobrutalistBox
import com.acesat.education.ui.components.NeobrutalistButton
import com.acesat.education.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onStart: (String, Int) -> Unit
) {
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
        // Header Card
        NeobrutalistBox(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = PinkAccent,
            fillMaxWidth = true
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AceSAT",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BorderBlack
                )
                Text(
                    text = "AI-POWERED SAT TUTOR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = BorderBlack
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Personalized practice. Real SAT questions. Adaptive learning powered by NVIDIA AI.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = BorderBlack
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        NeobrutalistBox(
            modifier = Modifier.fillMaxWidth(),
            fillMaxWidth = true
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    "YOUR NAME",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = BorderBlack
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Enter your name", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = BorderBlack,
                        unfocusedBorderColor = BorderBlack,
                        focusedTextColor = BorderBlack,
                        unfocusedTextColor = BorderBlack
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    "TARGET SAT SCORE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = BorderBlack
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = targetScoreText,
                    onValueChange = { targetScoreText = it },
                    placeholder = { Text("e.g. 1400", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = BorderBlack,
                        unfocusedBorderColor = BorderBlack,
                        focusedTextColor = BorderBlack,
                        unfocusedTextColor = BorderBlack
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
                    onStart(name.trim(), target)
                }
            },
            backgroundColor = PurpleAccent
        ) {
            Text(
                "ENTER DASHBOARD →",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = CardWhite
            )
        }
    }
}
