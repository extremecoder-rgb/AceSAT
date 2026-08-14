package com.acesat.education.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    onSave: (String) -> Unit
) {
    var ipText by remember { mutableStateOf(settingsManager.getBackendIp() ?: "") }
    var discoveryStatus by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        NeobrutalistBox(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = CardWhite
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Connection Settings", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = BorderBlack)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Configure which local server IP your phone connects to.",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("BACKEND SERVER IP", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = BorderBlack)
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

                Spacer(modifier = Modifier.height(8.dp))

                NeobrutalistButton(
                    onClick = {
                        isScanning = true
                        discoveryStatus = "Scanning Wi-Fi network..."
                        scope.launch {
                            val discovered = BackendDiscovery.discoverServer(context)
                            if (discovered != null) {
                                ipText = discovered
                                discoveryStatus = "✅ Found server at $discovered"
                            } else {
                                discoveryStatus = "❌ No server found. Enter IP manually."
                            }
                            isScanning = false
                        }
                    },
                    backgroundColor = TealAccent,
                    enabled = !isScanning
                ) {
                    Text(
                        text = if (isScanning) "SCANNING..." else "AUTO-DETECT SERVER IP",
                        fontWeight = FontWeight.Bold,
                        color = CardWhite,
                        fontSize = 12.sp
                    )
                }

                if (discoveryStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = discoveryStatus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (discoveryStatus.startsWith("✅")) TealAccent else Color.Red
                    )
                }

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
                        onClick = { onSave(ipText.trim()) },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.border(2.dp, BorderBlack, RoundedCornerShape(6.dp))
                    ) {
                        Text("SAVE", color = CardWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
