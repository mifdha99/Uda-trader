package com.example.ui.screens

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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AutoTradingState
import com.example.model.ConfirmationType
import com.example.model.DataSourceMode
import com.example.model.OlympAccountBalance
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
    activeDataSource: DataSourceMode,
    olympPrice: Double?,
    olympPair: String,
    olympBalance: OlympAccountBalance?,
    autoTradingState: AutoTradingState,
    onSelectDataSourceMode: (DataSourceMode) -> Unit,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onToggleFloatingOverlay: () -> Unit,
    onIntervalSelected: (Int) -> Unit,
    onTestSampleFrame: () -> Unit,
    onStartAutoTrading: () -> Unit,
    onStopAutoTrading: () -> Unit,
    onEmergencyStop: () -> Unit,
    onCopySignal: () -> Unit,
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
                    text = "Institutional Trading Cockpit",
                    color = CyanAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Mode Selector Toggle Bar
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate900)
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (activeDataSource == DataSourceMode.MT5_VISION) CyanAccent else Color.Transparent)
                        .clickable { onSelectDataSourceMode(DataSourceMode.MT5_VISION) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "MT5",
                        color = if (activeDataSource == DataSourceMode.MT5_VISION) Color.Black else Slate400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (activeDataSource == DataSourceMode.OLYMP_TRADE_WS) NeonGreen else Color.Transparent)
                        .clickable { onSelectDataSourceMode(DataSourceMode.OLYMP_TRADE_WS) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "OLYMP TRADE",
                        color = if (activeDataSource == DataSourceMode.OLYMP_TRADE_WS) Color.Black else Slate400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Live Telemetry Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (activeDataSource == DataSourceMode.OLYMP_TRADE_WS) "OLYMP TRADE LIVE FEED" else "STATUS SISTEM MT5",
                        color = Slate400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )

                    if (activeDataSource == DataSourceMode.OLYMP_TRADE_WS) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Demo: $${String.format("%.2f", olympBalance?.amount ?: 10000.0)}",
                                color = NeonGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (activeDataSource == DataSourceMode.MT5_VISION) {
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
                    } else {
                        StatusPill(
                            label = "WebSocket",
                            active = autoTradingState != AutoTradingState.DISCONNECTED,
                            activeText = autoTradingState.name,
                            inactiveText = "DISCONNECTED",
                            icon = Icons.Default.Wifi
                        )
                        if (olympPrice != null) {
                            StatusPill(
                                label = olympPair,
                                active = true,
                                activeText = String.format("%.5f", olympPrice),
                                inactiveText = "---",
                                icon = Icons.Default.Refresh
                            )
                        }
                    }

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
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (isCapturing || autoTradingState == AutoTradingState.ANALYZING) NeonGreen else Slate400, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusMessage,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        // Live Signal Hero Card (with COPY action)
        LiveSignalHeroCardWithCopy(
            signal = currentSignal,
            isAnalyzing = isAnalyzing,
            activeDataSource = activeDataSource,
            olympPrice = olympPrice,
            onCopySignal = onCopySignal
        )

        // Control Actions (MT5 vs Olymp Trade)
        if (activeDataSource == DataSourceMode.MT5_VISION) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "KONTROL ANALISIS MT5",
                        color = Slate400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onStartCapture,
                            enabled = !isCapturing,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("MULAI ANALISIS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = onStopCapture,
                            enabled = isCapturing,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRed, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("BERHENTI", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Interval Selection
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3, 5).forEach { seconds ->
                            val isSelected = selectedInterval == seconds
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CyanAccent else Slate900)
                                    .border(1.dp, if (isSelected) CyanAccent else DarkBorder, RoundedCornerShape(8.dp))
                                    .clickable { onIntervalSelected(seconds) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${seconds}s",
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onTestSampleFrame,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Uji Coba Analisis Frame Chart Sampel", color = CyanAccent, fontSize = 12.sp)
                    }
                }
            }
        } else {
            // Olymp Trade Auto Trading & Emergency Control Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "KONTROL OLYMP TRADE AUTO TRADING",
                            color = Slate400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x3310B981))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "DEMO ONLY", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Start & Stop Auto Trading
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val isAutoRunning = autoTradingState == AutoTradingState.ANALYZING ||
                            autoTradingState == AutoTradingState.ORDER_PENDING ||
                            autoTradingState == AutoTradingState.ORDER_CONFIRMED

                        Button(
                            onClick = onStartAutoTrading,
                            enabled = !isAutoRunning,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("START AUTO (DEMO)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = onStopAutoTrading,
                            enabled = isAutoRunning,
                            colors = ButtonDefaults.buttonColors(containerColor = Slate700, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("STOP ROBOT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Emergency Stop Button
                    Button(
                        onClick = onEmergencyStop,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D), contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Default.StopCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EMERGENCY STOP (HENTIKAN TOTAL)", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
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
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
            Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isFloatingOverlayVisible) "SEMBUNYIKAN FLOATING WINDOW" else "BUKA FLOATING WINDOW DI ATAS CHART",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun LiveSignalHeroCardWithCopy(
    signal: TradingSignal?,
    isAnalyzing: Boolean,
    activeDataSource: DataSourceMode,
    olympPrice: Double?,
    onCopySignal: () -> Unit
) {
    val signalType = signal?.signal ?: SignalType.WAIT
    val (signalColor, badgeBg, signalText) = when (signalType) {
        SignalType.BUY -> Triple(NeonGreen, NeonGreenGlow, if (activeDataSource == DataSourceMode.OLYMP_TRADE_WS) "🟢 UP / CALL" else "🟢 BUY NOW")
        SignalType.SELL -> Triple(NeonRed, NeonRedGlow, if (activeDataSource == DataSourceMode.OLYMP_TRADE_WS) "🔴 DOWN / PUT" else "🔴 SELL NOW")
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
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header: Pair & Source & Copy
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = signal?.pair ?: "EURUSD",
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

                // Copy Button
                Button(
                    onClick = onCopySignal,
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = CyanAccent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("COPY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                            ConfirmationType.CONFIRMED -> "✓ KONFLUENSI TERKONFIRMASI (> 70%)"
                            ConfirmationType.PENDING -> "⏳ PENDING / MENUNGGU CANDLE CLOSE"
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

            // Price Details Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate900)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PriceMetricColumn(
                    label = "HARGA / ENTRY",
                    value = if (activeDataSource == DataSourceMode.OLYMP_TRADE_WS && olympPrice != null) {
                        String.format("%.5f", olympPrice)
                    } else {
                        signal?.entry?.let { String.format("%.5f", it) } ?: "---"
                    },
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
                    label = "R:R / DURASI",
                    value = signal?.riskReward ?: "1:2",
                    valueColor = Color.White
                )
            }

            // Technical Rationale Text
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Slate900)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "CATATAN ANALISIS TEKNIKAL:",
                    color = Slate400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = signal?.reason ?: "Sistem siap menerima sinyal dari sumber aktif.",
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun PriceMetricColumn(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Slate400, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
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
        Icon(imageVector = icon, contentDescription = null, tint = if (active) NeonGreen else Slate400, modifier = Modifier.size(14.dp))
        Text(
            text = "$label: ${if (active) activeText else inactiveText}",
            color = if (active) NeonGreen else Slate400,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private val Slate700 = Color(0xFF334155)
