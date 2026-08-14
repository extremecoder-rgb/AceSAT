package com.acesat.education.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.acesat.education.data.BackendDiscovery
import com.acesat.education.data.SettingsManager
import com.acesat.education.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    settingsManager: SettingsManager,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var ipText by remember { mutableStateOf(settingsManager.getBackendIp() ?: "") }
    var apiKeyText by remember { mutableStateOf(settingsManager.getApiKey() ?: "") }
    var discoveryStatus by remember { mutableStateOf("Ready to scan") }
    var isScanning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        NeobrutalistBox(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = CardWhite
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "AceSAT Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BorderBlack
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text("BACKEND PROXY SERVER IP", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = BorderBlack)
                Text("Your phone automatically connects to the server on this IP.", fontSize = 10.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = ipText,
                    onValueChange = { ipText = it },
                    placeholder = { Text("e.g. 192.168.0.104", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = BorderBlack,
                        unfocusedBorderColor = BorderBlack,
                        focusedTextColor = BorderBlack,
                        unfocusedTextColor = BorderBlack
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Auto Discovery Button
                NeobrutalistButton(
                    onClick = {
                        isScanning = true
                        discoveryStatus = "Scanning local subnet..."
                        scope.launch {
                            val discovered = BackendDiscovery.discoverServer(context)
                            if (discovered != null) {
                                ipText = discovered
                                discoveryStatus = "Discovered: $discovered!"
                            } else {
                                discoveryStatus = "Could not find server. Check firewall."
                            }
                            isScanning = false
                        }
                    },
                    backgroundColor = TealAccent,
                    enabled = !isScanning
                ) {
                    Text(
                        text = if (isScanning) "SCANNING..." else "AUTO-DISCOVER BACKEND IP",
                        fontWeight = FontWeight.Bold,
                        color = CardWhite,
                        fontSize = 12.sp
                    )
                }
                
                // Status Text
                Text(
                    text = discoveryStatus,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (discoveryStatus.contains("Discovered")) TealAccent else Color.DarkGray,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("NVIDIA NIM API KEY", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = BorderBlack)
                Text("Input key to bypass local server networking issues and call NVIDIA directly.", fontSize = 10.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = { apiKeyText = it },
                    placeholder = { Text("nvapi-... (Optional)", color = Color.Gray) },
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = BorderBlack, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(ipText, apiKeyText)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.border(2.dp, BorderBlack, shape = RoundedCornerShape(6.dp))
                    ) {
                        Text("SAVE & RESTART", color = CardWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
