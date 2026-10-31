package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TechnicalAnalysisResult
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
fun EngineerScreen(
    technicalResult: TechnicalAnalysisResult?,
    livePrice: Double?,
    pair: String,
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
        // Header
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ENGINEER QUANT DIAGNOSTICS",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "Telemetri matematika indikator dan scoring konfluensi real-time ($pair)",
                color = Slate400,
                fontSize = 12.sp
            )
        }

        // Confluence Score Card
        val score = technicalResult?.confidenceScore ?: 0.0
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CyanAccent.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SKOR KONFLUENSI SINYAL:",
                        color = Slate400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${String.format("%.0f", score * 100)}%",
                        color = if (score >= 0.70) NeonGreen else if (score >= 0.50) CyanAccent else Slate400,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                LinearProgressIndicator(
                    progress = { score.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (score >= 0.70) NeonGreen else CyanAccent,
                    trackColor = Slate800
                )

                Text(
                    text = if (score >= 0.70) "✓ Konfluensi Terkonfirmasi (Setup kuat > 70%)" else "⏳ Menunggu Konfluensi Terkonfirmasi (Ambang batas 65%)",
                    color = if (score >= 0.70) NeonGreen else Color(0xFFFBBF24),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Technical Indicators Grid
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
                    text = "NILAI INDIKATOR SAAT INI",
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IndicatorBox(
                        label = "EMA 9 (Cepat)",
                        value = technicalResult?.ema9?.let { String.format("%.5f", it) } ?: "---",
                        color = CyanAccent,
                        modifier = Modifier.weight(1f)
                    )
                    IndicatorBox(
                        label = "EMA 21 (Lambat)",
                        value = technicalResult?.ema21?.let { String.format("%.5f", it) } ?: "---",
                        color = Color(0xFFA855F7),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val rsi = technicalResult?.rsi14
                    val rsiColor = when {
                        rsi == null -> Slate400
                        rsi >= 70.0 -> NeonRed
                        rsi <= 30.0 -> NeonGreen
                        else -> Color.White
                    }
                    IndicatorBox(
                        label = "RSI 14",
                        value = rsi?.let { String.format("%.1f", it) } ?: "---",
                        subtext = when {
                            rsi == null -> ""
                            rsi >= 70.0 -> "Overbought"
                            rsi <= 30.0 -> "Oversold"
                            else -> "Netral (30-70)"
                        },
                        color = rsiColor,
                        modifier = Modifier.weight(1f)
                    )

                    IndicatorBox(
                        label = "ATR 14 (Volatilitas)",
                        value = technicalResult?.atr14?.let { String.format("%.5f", it) } ?: "---",
                        subtext = "Volatilitas range",
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IndicatorBox(
                        label = "Trend EMA",
                        value = technicalResult?.trend ?: "---",
                        color = when (technicalResult?.trend) {
                            "BULLISH" -> NeonGreen
                            "BEARISH" -> NeonRed
                            else -> Slate400
                        },
                        modifier = Modifier.weight(1f)
                    )

                    IndicatorBox(
                        label = "Pola Candlestick",
                        value = technicalResult?.detectedPattern?.name ?: "NONE",
                        color = CyanAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Support & Resistance Levels
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
                Text(
                    text = "ZONA KEY LEVELS (SUPPORT & RESISTANCE)",
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                val supports = technicalResult?.supportLevels ?: emptyList()
                val resistances = technicalResult?.resistanceLevels ?: emptyList()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Support:", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        if (supports.isEmpty()) {
                            Text(text = "Belum terdeteksi", color = Slate400, fontSize = 11.sp)
                        } else {
                            supports.forEach {
                                Text(text = "• ${String.format("%.5f", it)}", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Resistance:", color = NeonRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        if (resistances.isEmpty()) {
                            Text(text = "Belum terdeteksi", color = Slate400, fontSize = 11.sp)
                        } else {
                            resistances.forEach {
                                Text(text = "• ${String.format("%.5f", it)}", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        // Mathematical Formula Rationale
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PENJELASAN LOGIKA KUANTITATIF",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = technicalResult?.reason ?: "Data candle sedang dikumpulkan secara real-time dari koneksi aktif.",
                    color = Slate400,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun IndicatorBox(
    label: String,
    value: String,
    color: Color,
    subtext: String = "",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Slate900)
            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(text = label, color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = color,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            if (subtext.isNotBlank()) {
                Text(text = subtext, color = Slate400, fontSize = 9.sp)
            }
        }
    }
}
