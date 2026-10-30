package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConfirmationType
import com.example.model.SignalType
import com.example.model.TradingSignal
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonAmberGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenGlow
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonRedGlow
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CockpitScreen(
    currentSignal: TradingSignal?,
    isCapturing: Boolean,
    isAnalyzing: Boolean,
    statusMessage: String,
    isFloatingOverlayVisible: Boolean,
    selectedInterval: Int,
    useDirectGemini: Boolean,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onToggleFloatingOverlay: () -> Unit,
    onIntervalSelected: (Int) -> Unit,
    onTestSampleFrame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header Banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "UDA AI TRADER",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Institutional MT5 Chart Vision Engine",
                    color = CyanAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Engine Mode Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate900)
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (useDirectGemini) "DIRECT GEMINI" else "SECURE BACKEND",
                    color = if (useDirectGemini) CyanAccent else NeonGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Live Telemetry Status Chips
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "STATUS SISTEM REAL-TIME",
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPill(
                        label = "Screen Capture",
                        active = isCapturing,
                        activeText = "RECORDING",
                        inactiveText = "STOPPED",
                        icon = Icons.Default.CameraAlt
                    )

                    StatusPill(
                        label = "Gemini Engine",
                        active = isAnalyzing,
                        activeText = "ANALYZING",
                        inactiveText = "STANDBY",
                        icon = Icons.Default.AutoAwesome
                    )

                    StatusPill(
                        label = "Floating HUD",
                        active = isFloatingOverlayVisible,
                        activeText = "VISIBLE",
                        inactiveText = "HIDDEN",
                        icon = Icons.Default.Layers
                    )
                }

                // Live status text
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate900)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = CyanAccent,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isCapturing) NeonGreen else Slate400, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = statusMessage,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        // Main Live Signal Card
        LiveSignalHeroCard(signal = currentSignal, isAnalyzing = isAnalyzing)

        // Primary Control Actions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "KONTROL ANALISIS & OVERLAY",
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                // Start / Stop Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onStartCapture,
                        enabled = !isCapturing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = Color.Black,
                            disabledContainerColor = Slate800,
                            disabledContentColor = Slate400
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("start_analysis_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("MULAI ANALISIS", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = onStopCapture,
                        enabled = isCapturing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonRed,
                            contentColor = Color.White,
                            disabledContainerColor = Slate800,
                            disabledContentColor = Slate400
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("stop_analysis_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("BERHENTI", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Floating Overlay Toggle Button
                Button(
                    onClick = onToggleFloatingOverlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFloatingOverlayVisible) Slate700 else CyanAccent,
                        contentColor = if (isFloatingOverlayVisible) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("floating_window_button")
                ) {
                    Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isFloatingOverlayVisible) "SEMBUNYIKAN FLOATING WINDOW" else "BUKA FLOATING WINDOW DI ATAS MT5",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Interval Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Interval Pengambilan Layar:",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2, 3, 5).forEach { seconds ->
                            val isSelected = selectedInterval == seconds
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CyanAccent else Slate900)
                                    .border(
                                        1.dp,
                                        if (isSelected) CyanAccent else DarkBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onIntervalSelected(seconds) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${seconds}s",
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Test Sample Frame Action
                OutlinedButton(
                    onClick = onTestSampleFrame,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("test_sample_frame_button"),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Uji Coba Analisis Frame Chart Sampel",
                        color = CyanAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun LiveSignalHeroCard(signal: TradingSignal?, isAnalyzing: Boolean) {
    val signalType = signal?.signal ?: SignalType.WAIT
    val (signalColor, badgeBg, signalText) = when (signalType) {
        SignalType.BUY -> Triple(NeonGreen, NeonGreenGlow, "🟢 BUY NOW")
        SignalType.SELL -> Triple(NeonRed, NeonRedGlow, "🔴 SELL NOW")
        SignalType.WAIT -> Triple(NeonAmber, NeonAmberGlow, "🟡 WAIT")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            brush = Brush.linearGradient(listOf(signalColor, DarkBorder))
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Pair & Timeframe & Trend Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = signal?.pair ?: "MT5 SCANNER",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Slate800)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = signal?.timeframe ?: "M5",
                            color = CyanAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Trend badge
                val trend = signal?.trend ?: "UNKNOWN"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when (trend.uppercase()) {
                                "BULLISH" -> Color(0x3310B981)
                                "BEARISH" -> Color(0x33F43F5E)
                                else -> Slate800
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "TREND: $trend",
                        color = when (trend.uppercase()) {
                            "BULLISH" -> NeonGreen
                            "BEARISH" -> NeonRed
                            else -> Slate400
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Big Signal Visual Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(badgeBg)
                    .border(1.5.dp, signalColor.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = signalText,
                        color = signalColor,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )

                    val confirmation = signal?.confirmation ?: ConfirmationType.UNCONFIRMED
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (confirmation) {
                            ConfirmationType.CONFIRMED -> "✓ STRUKTUR TERKONFIRMASI (BOS / OB / FVG)"
                            ConfirmationType.PENDING -> "⏳ PENDING / MENUNGGU CLOSE CANDLE"
                            ConfirmationType.UNCONFIRMED -> "PEMANTAUAN PASAR"
                        },
                        color = when (confirmation) {
                            ConfirmationType.CONFIRMED -> Color(0xFF34D399)
                            ConfirmationType.PENDING -> Color(0xFFFBBF24)
                            ConfirmationType.UNCONFIRMED -> Slate400
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Price Details Grid (Entry, SL, TP, RR)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate900)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PriceMetricColumn(
                    label = "ENTRY",
                    value = signal?.entry?.let { String.format("%.5f", it) } ?: "---",
                    valueColor = CyanAccent
                )
                PriceMetricColumn(
                    label = "STOP LOSS",
                    value = signal?.stopLoss?.let { String.format("%.5f", it) } ?: "---",
                    valueColor = NeonRed
                )
                PriceMetricColumn(
                    label = "TAKE PROFIT",
                    value = signal?.takeProfit?.let { String.format("%.5f", it) } ?: "---",
                    valueColor = NeonGreen
                )
                PriceMetricColumn(
                    label = "R:R",
                    value = signal?.riskReward ?: "1:2",
                    valueColor = Color.White
                )
            }

            // Detailed Technical Reason from Gemini AI
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Slate900)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ANALISIS TEKNIKAL & STRUKTUR PASAR:",
                        color = Slate400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = signal?.reason ?: "Belum ada analisis yang dilakukan. Tekan 'MULAI ANALISIS' atau buka MetaTrader 5.",
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                if (!signal?.timestamp.isNullOrBlank()) {
                    Text(
                        text = "Waktu Analisis: ${signal?.timestamp}",
                        color = Slate400,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun PriceMetricColumn(
    label: String,
    value: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun StatusPill(
    label: String,
    active: Boolean,
    activeText: String,
    inactiveText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) Color(0x2210B981) else Slate900)
            .border(1.dp, if (active) NeonGreen else DarkBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) NeonGreen else Slate400,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "$label: ${if (active) activeText else inactiveText}",
            color = if (active) NeonGreen else Slate400,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private val Slate700 = Color(0xFF334155)
