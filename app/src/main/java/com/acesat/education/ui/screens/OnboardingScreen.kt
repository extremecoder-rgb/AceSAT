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
import com.acesat.education.data.SettingsManager
import com.acesat.education.ui.components.NeobrutalistBox
import com.acesat.education.ui.components.NeobrutalistButton
import com.acesat.education.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    settingsManager: SettingsManager,
    onStart: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetScoreText by remember { mutableStateOf("1400") }
    var apiKey by remember { mutableStateOf(settingsManager.getApiKey() ?: "") }

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
                Text("STUDENT NAME", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BorderBlack)
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

                Spacer(modifier = Modifier.height(16.dp))

                Text("TARGET SAT SCORE", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BorderBlack)
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
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("NVIDIA API KEY (Optional for proxy)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BorderBlack)
                Text("Paste your free NIM API key here to bypass network issues and connect directly.", fontSize = 10.sp, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    placeholder = { Text("nvapi-...", color = Color.Gray) },
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
                    settingsManager.setApiKey(apiKey.trim())
                    onStart(name, target)
                }
            },
            backgroundColor = PurpleAccent
        ) {
            Text("ENTER DASHBOARD", fontWeight = FontWeight.ExtraBold, color = CardWhite)
        }
    }
}
