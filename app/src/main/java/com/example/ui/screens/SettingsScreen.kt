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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
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
    onUpdateOlympToken: (String) -> Unit,
    onUpdateOlympPair: (String) -> Unit,
    onClearOlympCredentials: () -> Unit,
    onConnectOlymp: (String, String) -> Unit,
    onDisconnectOlymp: () -> Unit,
    onTestConnection: () -> Unit,
    onClearTestStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    var geminiKeyInput by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    var olympTokenInput by remember(settings.olympToken) { mutableStateOf(settings.olympToken) }
    var olympPairInput by remember(settings.olympPair) { mutableStateOf(settings.olympPair) }

    var isGeminiKeyVisible by remember { mutableStateOf(false) }
    var isOlympTokenVisible by remember { mutableStateOf(false) }

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
                text = "PENGATURAN & KONEKSI",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = "Konfigurasi autentikasi broker Olymp Trade dan Google Gemini API",
                color = Slate400,
                fontSize = 12.sp
            )
        }

        // Section 1: Olymp Trade API Configuration
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AUTENTIKASI OLYMP TRADE (CHIPADEVTEAM API)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Aplikasi berkomunikasi langsung via WebSocket resmi Olymp Trade (wss://ws.olymptrade.com) tanpa server perantara berbayar.",
                    color = Slate400,
                    fontSize = 11.sp
                )

                // Access Token Input
                OutlinedTextField(
                    value = olympTokenInput,
                    onValueChange = {
                        olympTokenInput = it
                        onUpdateOlympToken(it)
                    },
                    label = { Text("Olymp Trade Access Token / Session Cookie") },
                    singleLine = true,
                    visualTransformation = if (isOlympTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isOlympTokenVisible = !isOlympTokenVisible }) {
                            Icon(
                                imageVector = if (isOlympTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle",
                                tint = Slate400
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = DarkBorder,
                        focusedLabelColor = NeonGreen,
                        unfocusedLabelColor = Slate400,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Pair Input
                OutlinedTextField(
                    value = olympPairInput,
                    onValueChange = {
                        olympPairInput = it.uppercase()
                        onUpdateOlympPair(it.uppercase())
                    },
                    label = { Text("Instrumen / Pasangan Aktif (cth: EURUSD, LATAM_X)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = DarkBorder,
                        focusedLabelColor = NeonGreen,
                        unfocusedLabelColor = Slate400,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Action Buttons (Connect, Disconnect, Clear)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onConnectOlymp(olympTokenInput, olympPairInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("HUBUNGKAN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onDisconnectOlymp,
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PUTUSKAN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            olympTokenInput = ""
                            onClearOlympCredentials()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Section 2: Google Gemini Vision Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "GOOGLE GEMINI VISION API (MT5 ENGINE)",
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                OutlinedTextField(
                    value = geminiKeyInput,
                    onValueChange = {
                        geminiKeyInput = it
                        onUpdateGeminiKey(it)
                    },
                    label = { Text("Gemini API Key") },
                    singleLine = true,
                    visualTransformation = if (isGeminiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isGeminiKeyVisible = !isGeminiKeyVisible }) {
                            Icon(
                                imageVector = if (isGeminiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle",
                                tint = Slate400
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

                // Test Connection Button
                Button(
                    onClick = onTestConnection,
                    colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = CyanAccent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(38.dp)
                ) {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("UJI KONEKSI GEMINI API", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                if (connectionTestStatus != null) {
                    Text(
                        text = connectionTestStatus,
                        color = if (connectionTestStatus.startsWith("SUKSES")) NeonGreen else NeonRed,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Section 3: POCO & Android Battery Optimization Guide
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PANDUAN LATAR BELAKANG PERANGKAT POCO / MIUI",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Agar robot trading dapat berjalan saat layar mati atau berpindah aplikasi pada POCO/Xiaomi/HyperOS:\n" +
                        "1. Buka Setelan HP > Aplikasi > Kelola Aplikasi > UDA AI TRADER.\n" +
                        "2. Buka 'Penghemat Baterai' > Pilih 'Tidak Ada Pembatasan' (No Restrictions).\n" +
                        "3. Aktifkan sakelar 'Mulai Otomatis' (Autostart).\n" +
                        "4. Buka menu Recent Apps, tahan ikon aplikasi dan tekan ikon Gembok (Lock).",
                    color = Slate400,
                    fontSize = 11.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}
