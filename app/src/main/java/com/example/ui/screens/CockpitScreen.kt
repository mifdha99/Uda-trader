package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConfirmationType
import com.example.model.DataVerificationLevel
import com.example.model.EngineState
import com.example.model.Mt5Timeframe
import com.example.model.SignalType
import com.example.model.TradingSignal
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonAmberGlow
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenGlow
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonRedGlow
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CockpitScreen(
    currentSignal: TradingSignal?,
    engineState: EngineState,
    isCapturing: Boolean,
    isPaused: Boolean,
    isAnalyzing: Boolean,
    statusMessage: String,
    isFloatingOverlayVisible: Boolean,
    activeTimeframe: Mt5Timeframe,
    selectedInterval: Int,
    onTimeframeSelected: (Mt5Timeframe) -> Unit,
    onStartCapture: () -> Unit,
    onPauseCapture: () -> Unit,
    onResumeCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onToggleFloatingOverlay: () -> Unit,
    onIntervalSelected: (Int) -> Unit,
    onTestSampleFrame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val tfScrollState = rememberScrollState()

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
                    text = "Universal Signal Engine (On-Device MT4/MT5/TV)",
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
                    text = "LOCAL ENGINE",
                    color = NeonGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 10-State Engine Banner
        EngineStateBanner(engineState = engineState, isPaused = isPaused)

        // Timeframe Selector (9 Timeframes)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TIMEFRAME ACUAN ANALISIS",
                        color = Slate400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "AKTIF: ${activeTimeframe.code} (${activeTimeframe.displayName})",
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Horizontal scrollable chips for all 9 MT5 Timeframes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(tfScrollState),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Mt5Timeframe.values().forEach { tf ->
                        val isSelected = activeTimeframe == tf
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CyanAccent else Slate900)
                                .border(1.dp, if (isSelected) CyanAccent else DarkBorder, RoundedCornerShape(8.dp))
                                .clickable { onTimeframeSelected(tf) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tf.code,
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
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
                    text = "STATUS PEMANTAUAN REAL-TIME",
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
                        activeText = if (isPaused) "DIJEDA (JEDA)" else "AKTIF (${selectedInterval}s)",
                        inactiveText = "BERHENTI",
                        icon = Icons.Default.CameraAlt
                    )

                    StatusPill(
                        label = "Universal Engine",
                        active = isAnalyzing,
                        activeText = "SCANNING",
                        inactiveText = engineState.displayName,
                        icon = Icons.Default.ShowChart
                    )

                    StatusPill(
                        label = "Floating HUD",
                        active = isFloatingOverlayVisible,
                        activeText = "VISIBLE",
                        inactiveText = "HIDDEN",
                        icon = Icons.Default.Layers
                    )
                }

                // Status message bar
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
                                .background(if (isCapturing && !isPaused) NeonGreen else Slate400, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = statusMessage,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Main Live Signal Card
        LiveSignalHeroCard(signal = currentSignal, engineState = engineState)

        // Primary Control Actions: MULAI, JEDA/LANJUT, BERHENTI
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
                    text = "KONTROL PEMANTAUAN LAYAR (LIVE SCREEN)",
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                // 3 Action Buttons: MULAI, JEDA/LANJUT, BERHENTI
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // MULAI
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
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("MULAI", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // JEDA / LANJUT
                    Button(
                        onClick = {
                            if (isPaused) onResumeCapture() else onPauseCapture()
                        },
                        enabled = isCapturing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPaused) CyanAccent else NeonAmber,
                            contentColor = Color.Black,
                            disabledContainerColor = Slate800,
                            disabledContentColor = Slate400
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pause_resume_button")
                    ) {
                        Icon(
                            if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPaused) "LANJUT" else "JEDA", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // BERHENTI
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
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("BERHENTI", fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
                        text = if (isFloatingOverlayVisible) "SEMBUNYIKAN FLOATING WINDOW" else "BUKA FLOATING WINDOW DI ATAS CHART",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Interval Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Frekuensi Pemindaian Layar:",
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
            }
        }

        // Distinct DEMO / SIMULATION TEST Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MODE DEMO / SIMULASI (BUKAN LIVE MT5)",
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                }
                Text(
                    text = "Gunakan mode ini untuk menguji algoritma Universal Signal Engine secara lokal dengan dataset chart sintetis tanpa menghubungkan screen capture.",
                    color = Slate400,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                OutlinedButton(
                    onClick = onTestSampleFrame,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("test_sample_frame_button"),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent)
                ) {
                    Text(
                        text = "Jalankan Simulasi Chart Uji Coba",
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
fun EngineStateBanner(engineState: EngineState, isPaused: Boolean) {
    val (bgColor, borderColor, textColor, iconVector) = when (engineState) {
        EngineState.BUY -> Quad(Color(0x3310B981), NeonGreen, NeonGreen, Icons.Default.ShowChart)
        EngineState.SELL -> Quad(Color(0x33F43F5E), NeonRed, NeonRed, Icons.Default.ShowChart)
        EngineState.WAIT -> Quad(Color(0x33F59E0B), NeonAmber, NeonAmber, Icons.Default.Info)
        EngineState.WAITING_CHART -> Quad(Color(0x3364748B), Color(0xFF94A3B8), Color(0xFFCBD5E1), Icons.Default.CameraAlt)
        EngineState.READING_CHART -> Quad(Color(0x330284C7), Color(0xFF38BDF8), Color(0xFF38BDF8), Icons.Default.CameraAlt)
        EngineState.VALIDATING_DATA -> Quad(Color(0x338B5CF6), Color(0xFFA78BFA), Color(0xFFA78BFA), Icons.Default.ShowChart)
        EngineState.ANALYZING -> Quad(Color(0x3306B6D4), CyanAccent, CyanAccent, Icons.Default.ShowChart)
        EngineState.INVALID_DATA -> Quad(Color(0x33EF4444), NeonRed, NeonRed, Icons.Default.Warning)
        EngineState.STREAM_DISCONNECTED -> Quad(Color(0x33EF4444), NeonRed, NeonRed, Icons.Default.Warning)
        EngineState.DATA_EXPIRED -> Quad(Color(0x33F97316), Color(0xFFFB923C), Color(0xFFFB923C), Icons.Default.Warning)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = "STATUS ENGINE: ${engineState.displayName}",
                    color = textColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                )
                if (isPaused) {
                    Text(
                        text = "• Sesi pemantauan sedang dijeda",
                        color = NeonAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun LiveSignalHeroCard(signal: TradingSignal?, engineState: EngineState) {
    val context = LocalContext.current
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
                        text = signal?.formattedSymbol() ?: "TIDAK DIKENALI",
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

            // Big Signal Visual Banner + Confidence Score
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(badgeBg)
                    .border(1.5.dp, signalColor.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                    .padding(vertical = 14.dp),
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

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CONFIDENCE: ${signal?.formattedConfidence() ?: "0%"}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        val confirmation = signal?.confirmation ?: ConfirmationType.UNCONFIRMED
                        Text(
                            text = when (confirmation) {
                                ConfirmationType.CONFIRMED -> "• TERKONFIRMASI"
                                ConfirmationType.PENDING -> "• PENDING CLOSE"
                                ConfirmationType.UNCONFIRMED -> "• EVALUASI"
                            },
                            color = when (confirmation) {
                                ConfirmationType.CONFIRMED -> Color(0xFF34D399)
                                ConfirmationType.PENDING -> Color(0xFFFBBF24)
                                ConfirmationType.UNCONFIRMED -> Slate400
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
                    value = signal?.formattedEntry() ?: "TIDAK TERSEDIA",
                    valueColor = CyanAccent
                )
                PriceMetricColumn(
                    label = "STOP LOSS",
                    value = signal?.formattedStopLoss() ?: "TIDAK TERSEDIA",
                    valueColor = NeonRed
                )
                PriceMetricColumn(
                    label = "TAKE PROFIT",
                    value = signal?.formattedTakeProfit() ?: "TIDAK TERSEDIA",
                    valueColor = NeonGreen
                )
                PriceMetricColumn(
                    label = "R:R",
                    value = signal?.riskReward ?: "1:2",
                    valueColor = Color.White
                )
            }

            // Technical Indicators Breakdown (EMA, RSI, ATR, Pattern)
            signal?.metrics?.let { m ->
                if (m.rsi != null || m.fastEma != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Slate900)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        m.fastEma?.let {
                            IndicatorMiniBadge(label = "EMA 9", value = String.format(Locale.US, "%.1f", it))
                        }
                        m.slowEma?.let {
                            IndicatorMiniBadge(label = "EMA 21", value = String.format(Locale.US, "%.1f", it))
                        }
                        m.rsi?.let {
                            val rsiColor = when {
                                it > 70.0 -> NeonRed
                                it < 30.0 -> NeonAmber
                                else -> CyanAccent
                            }
                            IndicatorMiniBadge(label = "RSI 14", value = String.format(Locale.US, "%.1f", it), valueColor = rsiColor)
                        }
                        m.atr?.let {
                            IndicatorMiniBadge(label = "ATR", value = String.format(Locale.US, "%.1f", it))
                        }
                    }
                }
            }

            // Detailed Technical Reason & Section 10 COPY Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Slate900)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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
                            text = "ANALISIS UNIVERSAL ENGINE:",
                            color = Slate400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // COPY Button
                    Button(
                        onClick = {
                            val copyContent = signal?.formattedCopyText() ?: "[UDA AI TRADER]\nSinyal: WAIT\nStatus: MENUNGGU CHART"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("UdaTraderSignal", copyContent))
                            Toast.makeText(context, "Sinyal disalin ke Clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(26.dp)
                            .testTag("copy_signal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = Color.Black,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("COPY SINYAL", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }

                Text(
                    text = signal?.reason ?: "Universal Engine siap. Tekan 'MULAI' dan buka chart MetaTrader atau TradingView.",
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                if (!signal?.timestamp.isNullOrBlank()) {
                    Text(
                        text = "Waktu Pembaruan: ${signal?.timestamp} • ${signal?.verificationLevel?.label}",
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
private fun IndicatorMiniBadge(label: String, value: String, valueColor: Color = Color.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Slate400, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        Text(text = value, color = valueColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
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

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private val Slate700 = Color(0xFF334155)
