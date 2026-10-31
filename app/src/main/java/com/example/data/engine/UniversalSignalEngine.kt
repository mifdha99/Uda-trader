package com.example.data.engine

import android.graphics.Bitmap
import com.example.model.ConfirmationType
import com.example.model.DataVerificationLevel
import com.example.model.EngineState
import com.example.model.Mt5Timeframe
import com.example.model.SignalType
import com.example.model.TechnicalMetrics
import com.example.model.TradingSignal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Universal Signal Engine - 100% On-Device Quantitative Trading Analysis Engine.
 * Operates completely locally without any backend, server, or cloud AI.
 *
 * Implements:
 * - 10 Discrete Engine States
 * - EMA 9 / EMA 21 Trend & Dynamic Cross
 * - RSI 14 Momentum & Extremes Guard
 * - ATR 14 Volatility
 * - Support & Resistance Confluence
 * - Candlestick Price Action Confirmations
 * - Strict Data Integrity: Zero fabricated prices, zero default symbol assumptions.
 */
class UniversalSignalEngine {

    companion object {
        private const val FAST_EMA_PERIOD = 9
        private const val SLOW_EMA_PERIOD = 21
        private const val RSI_PERIOD = 14
        private const val ATR_PERIOD = 14
        private const val CONFIDENCE_THRESHOLD = 70.0 // Min 70% confidence for BUY/SELL
        const val DATA_EXPIRATION_THRESHOLD_MS = 15000L // 15 seconds expiration threshold
    }

    private val chartScanner = VisualChartScanner()

    // State stability & cooldown
    private var lastActionableSignal: TradingSignal? = null
    private var lastSignalTimeMs: Long = 0L
    private val signalCooldownMs = 8000L

    /**
     * Master processing pipeline for captured screen frames.
     */
    fun processChartFrame(
        bitmap: Bitmap,
        activeTimeframe: Mt5Timeframe
    ): TradingSignal {
        val now = getCurrentIsoTimestamp()
        val nowMs = System.currentTimeMillis()

        // 1. Scan image for candlesticks & layout
        val scanResult = chartScanner.scanChart(bitmap, activeTimeframe)

        if (scanResult is ChartScanResult.Invalid) {
            val isWaiting = scanResult.reason.contains("MENUNGGU CHART", ignoreCase = true)
            val state = if (isWaiting) EngineState.WAITING_CHART else EngineState.INVALID_DATA

            return TradingSignal(
                state = state,
                signal = SignalType.WAIT,
                pair = "TIDAK DIKENALI",
                timeframe = activeTimeframe.code,
                detectedTimeframe = null,
                isTimeframeMismatch = false,
                verificationLevel = DataVerificationLevel.INVALID,
                entry = null,
                stopLoss = null,
                takeProfit = null,
                riskReward = "1:2",
                confidence = 0.0,
                trend = "UNKNOWN",
                confirmation = ConfirmationType.UNCONFIRMED,
                reason = scanResult.reason,
                timestamp = now,
                timestampMillis = nowMs,
                metrics = TechnicalMetrics()
            )
        }

        val success = scanResult as ChartScanResult.Success
        val candles = success.candles
        val detectedTf = success.detectedTimeframe
        val detectedPair = success.detectedPair ?: "TIDAK DIKENALI"

        // 2. Timeframe Consistency Validation
        val isMismatch = detectedTf != null && detectedTf != activeTimeframe
        if (isMismatch) {
            return TradingSignal(
                state = EngineState.WAIT,
                signal = SignalType.WAIT,
                pair = detectedPair,
                timeframe = activeTimeframe.code,
                detectedTimeframe = detectedTf?.code,
                isTimeframeMismatch = true,
                verificationLevel = success.verificationLevel,
                entry = null,
                stopLoss = null,
                takeProfit = null,
                riskReward = "1:2",
                confidence = 0.0,
                trend = "MISMATCH",
                confirmation = ConfirmationType.UNCONFIRMED,
                reason = "PERINGATAN TIMEFRAME: Timeframe aplikasi (${activeTimeframe.code}) berbeda dengan chart visual (${detectedTf?.code}). Ubah timeframe agar sinkron.",
                timestamp = now,
                timestampMillis = nowMs,
                metrics = TechnicalMetrics(candleCount = candles.size)
            )
        }

        // 3. Technical Indicators Calculation
        val closePrices = candles.map { it.close }
        val fastEmaList = TechnicalIndicators.calculateEMA(closePrices, FAST_EMA_PERIOD)
        val slowEmaList = TechnicalIndicators.calculateEMA(closePrices, SLOW_EMA_PERIOD)
        val rsi = TechnicalIndicators.calculateRSI(closePrices, RSI_PERIOD)
        val atr = TechnicalIndicators.calculateATR(candles, ATR_PERIOD)
        val (support, resistance) = TechnicalIndicators.calculateSupportResistance(candles)

        val latestCandle = candles.last()
        val previousCandle = if (candles.size >= 2) candles[candles.size - 2] else latestCandle

        val latestFastEma = fastEmaList.lastOrNull() ?: latestCandle.close
        val latestSlowEma = slowEmaList.lastOrNull() ?: latestCandle.close
        val prevFastEma = if (fastEmaList.size >= 2) fastEmaList[fastEmaList.size - 2] else latestFastEma
        val prevSlowEma = if (slowEmaList.size >= 2) slowEmaList[slowEmaList.size - 2] else latestSlowEma

        // 4. Trend & Momentum Direction
        val isBullishTrend = latestFastEma > latestSlowEma && latestCandle.close >= latestSlowEma
        val isBearishTrend = latestFastEma < latestSlowEma && latestCandle.close <= latestSlowEma
        val isEmaGoldenCross = prevFastEma <= prevSlowEma && latestFastEma > latestSlowEma
        val isEmaDeathCross = prevFastEma >= prevSlowEma && latestFastEma < latestSlowEma

        val trendName = when {
            isBullishTrend -> "BULLISH"
            isBearishTrend -> "BEARISH"
            else -> "SIDEWAYS"
        }

        // 5. Candlestick Confirmation
        val isBullEngulf = TechnicalIndicators.isBullishEngulfing(previousCandle, latestCandle)
        val isBearEngulf = TechnicalIndicators.isBearishEngulfing(previousCandle, latestCandle)
        val isHammer = latestCandle.isHammer()
        val isShootingStar = latestCandle.isShootingStar()
        val isStrongBull = latestCandle.isBullish && latestCandle.isStrongMarubozu()
        val isStrongBear = !latestCandle.isBullish && latestCandle.isStrongMarubozu()

        val patternName = when {
            isBullEngulf -> "Bullish Engulfing"
            isBearEngulf -> "Bearish Engulfing"
            isHammer -> "Hammer / Rejection Wick"
            isShootingStar -> "Shooting Star"
            isStrongBull -> "Strong Bullish Marubozu"
            isStrongBear -> "Strong Bearish Marubozu"
            else -> "Price Action Normal"
        }

        // 6. Confluence & Deterministic Scoring (0.0 to 100.0%)
        var buyScore = 0.0
        var sellScore = 0.0

        // Trend Confluence (max 25 pts)
        if (isBullishTrend) buyScore += 20.0
        if (isEmaGoldenCross) buyScore += 5.0
        if (isBearishTrend) sellScore += 20.0
        if (isEmaDeathCross) sellScore += 5.0

        // RSI Momentum Confluence (max 25 pts)
        when {
            rsi in 50.0..68.0 -> buyScore += 25.0
            rsi in 45.0..50.0 -> buyScore += 15.0
            rsi in 32.0..50.0 -> sellScore += 25.0
            rsi in 50.0..55.0 -> sellScore += 15.0
            rsi > 70.0 -> buyScore -= 20.0 // Overbought penalty
            rsi < 30.0 -> sellScore -= 20.0 // Oversold penalty
        }

        // Candlestick Confirmation (max 25 pts)
        if (isBullEngulf || isHammer || isStrongBull) buyScore += 25.0
        else if (latestCandle.isBullish && latestCandle.lowerWickRatio > 0.3) buyScore += 15.0

        if (isBearEngulf || isShootingStar || isStrongBear) sellScore += 25.0
        else if (!latestCandle.isBullish && latestCandle.upperWickRatio > 0.3) sellScore += 15.0

        // Support / Resistance Proximity (max 15 pts)
        val distToSupport = Math.abs(latestCandle.close - support)
        val distToResistance = Math.abs(resistance - latestCandle.close)
        if (distToSupport < (atr * 1.2) && latestCandle.isBullish) buyScore += 15.0
        if (distToResistance < (atr * 1.2) && !latestCandle.isBullish) sellScore += 15.0

        // Visual Quality (max 10 pts)
        val qualityBonus = success.confidenceQuality * 10.0
        buyScore += qualityBonus
        sellScore += qualityBonus

        val finalBuyConfidence = buyScore.coerceIn(0.0, 98.0)
        val finalSellConfidence = sellScore.coerceIn(0.0, 98.0)

        val metrics = TechnicalMetrics(
            fastEma = latestFastEma,
            slowEma = latestSlowEma,
            rsi = rsi,
            atr = atr,
            supportLevel = support,
            resistanceLevel = resistance,
            candlePattern = patternName,
            candleCount = candles.size
        )

        // Enforce strict price integrity:
        // Absolute prices are ONLY provided if price scale is calibrated.
        // Otherwise null -> displays "TIDAK TERSEDIA".
        val entryPrice: Double? = success.currentPrice
        val (stopLoss: Double?, takeProfit: Double?) = if (entryPrice != null && atr > 0) {
            val slDist = atr * 1.5
            val tpDist = slDist * 2.0
            Pair(entryPrice - slDist, entryPrice + tpDist)
        } else {
            Pair(null, null)
        }

        val inCooldown = (nowMs - lastSignalTimeMs) < signalCooldownMs

        // 7. Determine Signal & Engine State
        if (finalBuyConfidence >= CONFIDENCE_THRESHOLD && isBullishTrend && rsi <= 68.0) {
            val reason = "Konfluensi BUY (${finalBuyConfidence.toInt()}%): Trend EMA Bullish, RSI ${String.format(Locale.US, "%.1f", rsi)}, $patternName."

            val signal = TradingSignal(
                state = EngineState.BUY,
                signal = SignalType.BUY,
                pair = detectedPair,
                timeframe = activeTimeframe.code,
                detectedTimeframe = detectedTf?.code,
                isTimeframeMismatch = false,
                verificationLevel = success.verificationLevel,
                entry = entryPrice,
                stopLoss = stopLoss,
                takeProfit = takeProfit,
                riskReward = "1:2",
                confidence = finalBuyConfidence,
                trend = trendName,
                confirmation = if (isBullEngulf || isHammer) ConfirmationType.CONFIRMED else ConfirmationType.PENDING,
                reason = reason,
                timestamp = now,
                timestampMillis = nowMs,
                metrics = metrics
            )
            lastActionableSignal = signal
            lastSignalTimeMs = nowMs
            return signal
        } else if (finalSellConfidence >= CONFIDENCE_THRESHOLD && isBearishTrend && rsi >= 32.0) {
            val reason = "Konfluensi SELL (${finalSellConfidence.toInt()}%): Trend EMA Bearish, RSI ${String.format(Locale.US, "%.1f", rsi)}, $patternName."

            val signal = TradingSignal(
                state = EngineState.SELL,
                signal = SignalType.SELL,
                pair = detectedPair,
                timeframe = activeTimeframe.code,
                detectedTimeframe = detectedTf?.code,
                isTimeframeMismatch = false,
                verificationLevel = success.verificationLevel,
                entry = entryPrice,
                stopLoss = stopLoss,
                takeProfit = takeProfit,
                riskReward = "1:2",
                confidence = finalSellConfidence,
                trend = trendName,
                confirmation = if (isBearEngulf || isShootingStar) ConfirmationType.CONFIRMED else ConfirmationType.PENDING,
                reason = reason,
                timestamp = now,
                timestampMillis = nowMs,
                metrics = metrics
            )
            lastActionableSignal = signal
            lastSignalTimeMs = nowMs
            return signal
        } else {
            val reasonWait = when {
                rsi > 70.0 -> "WAIT: RSI Overbought (${String.format(Locale.US, "%.1f", rsi)}) - Risiko koreksi tinggi, jangan kejar BUY."
                rsi < 30.0 -> "WAIT: RSI Oversold (${String.format(Locale.US, "%.1f", rsi)}) - Risiko rebound tinggi, jangan kejar SELL."
                trendName == "SIDEWAYS" -> "WAIT: Pasar Sideways (EMA 9 & 21 berhimpit). Tunggu breakout terkonfirmasi."
                finalBuyConfidence in 50.0..69.9 -> "WAIT: Setup Bullish belum matang (Confidence ${finalBuyConfidence.toInt()}% < 70%). Tunggu candle close."
                finalSellConfidence in 50.0..69.9 -> "WAIT: Setup Bearish belum matang (Confidence ${finalSellConfidence.toInt()}% < 70%). Tunggu candle close."
                else -> "WAIT: Kondisi pasar belum memenuhi konfluensi multi-indikator Universal Engine."
            }

            return TradingSignal(
                state = EngineState.WAIT,
                signal = SignalType.WAIT,
                pair = detectedPair,
                timeframe = activeTimeframe.code,
                detectedTimeframe = detectedTf?.code,
                isTimeframeMismatch = false,
                verificationLevel = success.verificationLevel,
                entry = null,
                stopLoss = null,
                takeProfit = null,
                riskReward = "1:2",
                confidence = maxOf(finalBuyConfidence, finalSellConfidence),
                trend = trendName,
                confirmation = ConfirmationType.UNCONFIRMED,
                reason = reasonWait,
                timestamp = now,
                timestampMillis = nowMs,
                metrics = metrics
            )
        }
    }

    private fun getCurrentIsoTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
}
