package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.pref.AppSettings
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

@Composable
fun SettingsScreen(
    settings: AppSettings.SettingsState,
    connectionTestStatus: String?,
    onUpdateBackendUrl: (String) -> Unit,
    onUpdateGeminiKey: (String) -> Unit,
    onUpdateModel: (String) -> Unit,
    onUpdateUseDirect: (Boolean) -> Unit,
    onUpdateVibration: (Boolean) -> Unit,
    onUpdateSound: (Boolean) -> Unit,
    onTestConnection: () -> Unit,
    onClearTestStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    var backendUrlInput by remember(settings.backendUrl) { mutableStateOf(settings.backendUrl) }
    var geminiKeyInput by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    var isKeyVisible by remember { mutableStateOf(false) }

    val models = listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column {
            Text(
                text = "PENGATURAN & KONEKSI AI",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = "Konfigurasi backend proxy atau Gemini Vision API engine",
                color = Slate400,
                fontSize = 12.sp
            )
        }

        // Connection Mode Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ARSITEKTUR KONEKSI",
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!settings.useDirectGemini) CyanAccent else Slate900)
                            .border(1.dp, if (!settings.useDirectGemini) CyanAccent else DarkBorder, RoundedCornerShape(10.dp))
                            .clickable { onUpdateUseDirect(false) }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "UDA Backend",
                                color = if (!settings.useDirectGemini) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "(Paling Aman)",
                                color = if (!settings.useDirectGemini) Color.Black else NeonGreen,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (settings.useDirectGemini) CyanAccent else Slate900)
                            .border(1.dp, if (settings.useDirectGemini) CyanAccent else DarkBorder, RoundedCornerShape(10.dp))
                            .clickable { onUpdateUseDirect(true) }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Direct Gemini API",
                                color = if (settings.useDirectGemini) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "(Kunci Pribadi)",
                                color = if (settings.useDirectGemini) Color.Black else Slate400,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // Endpoint Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!settings.useDirectGemini) {
                    OutlinedTextField(
                        value = backendUrlInput,
                        onValueChange = {
                            backendUrlInput = it
                            onUpdateBackendUrl(it)
                        },
                        label = { Text("URL Server Backend UDA") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backend_url_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = DarkBorder,
                            focusedLabelColor = CyanAccent,
                            unfocusedLabelColor = Slate400,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                OutlinedTextField(
                    value = geminiKeyInput,
                    onValueChange = {
                        geminiKeyInput = it
                        onUpdateGeminiKey(it)
                    },
                    label = { Text(if (settings.useDirectGemini) "Google Gemini API Key (Wajib)" else "Gemini API Key (Opsional jika via Backend)") },
                    singleLine = true,
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            Icon(
                                imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Key",
                                tint = Slate400
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_key_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedLabelColor = CyanAccent,
                        unfocusedLabelColor = Slate400,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Model Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Pilih Model AI Vision:", color = Color.White, fontSize = 12.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        models.forEach { model ->
                            val isSelected = settings.selectedModel == model
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CyanAccent else Slate900)
                                    .border(1.dp, if (isSelected) CyanAccent else DarkBorder, RoundedCornerShape(8.dp))
                                    .clickable { onUpdateModel(model) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = model.removePrefix("gemini-"),
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Test Connection Button & Result
                Button(
                    onClick = onTestConnection,
                    colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = CyanAccent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_connection_button")
                ) {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("UJI KONEKSI ENDPOINT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                if (connectionTestStatus != null) {
                    val isSuccess = connectionTestStatus.startsWith("SUKSES")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSuccess) Color(0x2210B981) else Color(0x22F43F5E))
                            .border(1.dp, if (isSuccess) NeonGreen else NeonRed, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = connectionTestStatus,
                            color = if (isSuccess) NeonGreen else NeonRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Notification & Vibration Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "PEMBERITAHUAN SINYAL TERKONFIRMASI",
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Getaran Haptik saat BUY / SELL Terkonfirmasi", color = Color.White, fontSize = 13.sp)
                    Switch(
                        checked = settings.vibrationEnabled,
                        onCheckedChange = onUpdateVibration,
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent, checkedTrackColor = Slate800)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Suara Peringatan saat BUY / SELL Terkonfirmasi", color = Color.White, fontSize = 13.sp)
                    Switch(
                        checked = settings.soundEnabled,
                        onCheckedChange = onUpdateSound,
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent, checkedTrackColor = Slate800)
                    )
                }
            }
        }

        // MT5 Best Practices & Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PANDUAN OPTIMALISASI METATRADER 5",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "1. Buka aplikasi MetaTrader 5 pada ponsel Anda.\n" +
                        "2. Pilih chart candlestick (hindari bar chart atau line chart tipis).\n" +
                        "3. Gunakan skema warna 'Green on Black' atau 'Black on White' agar kontras candle jelas.\n" +
                        "4. Aktifkan Floating Window dari UDA AI TRADER lalu buka kembali MT5.\n" +
                        "5. Floating Window dapat digeser ke sudut layar agar tidak menutupi tombol transaksi MT5.",
                    color = Slate400,
                    fontSize = 11.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
