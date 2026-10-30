package com.example.data.engine

import android.graphics.Bitmap
import android.util.Log
import com.example.model.ConfirmationType
import com.example.model.Mt5Timeframe
import com.example.model.SignalType
import com.example.model.TechnicalMetrics
import com.example.model.TradingSignal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Universal Signal Engine - Core on-device quantitative technical analysis brain for UDA AI TRADER.
 * Analyzes chart data strictly locally using EMA, RSI, ATR, Price Action, and Confluence Scoring.
 */
class UniversalSignalEngine {

    companion object {
        private const val TAG = "UniversalSignalEngine"
        private const val FAST_EMA_PERIOD = 9
        private const val SLOW_EMA_PERIOD = 21
        private const val RSI_PERIOD = 14
        private const val ATR_PERIOD = 14
        private const val CONFIDENCE_THRESHOLD = 70.0 // Minimum 70% confidence for BUY/SELL
    }

    private val chartScanner = VisualChartScanner()

    // State machine for signal stability & cooldown
    private var lastSignal: TradingSignal? = null
    private var lastSignalTimeMs: Long = 0L
    private val signalCooldownMs = 8000L // 8-second cooldown between directional flips

    /**
     * Master analysis entry point: processes captured MT5 bitmap into a TradingSignal.
     */
    fun processChartFrame(
        bitmap: Bitmap,
        activeTimeframe: Mt5Timeframe
    ): TradingSignal {
        val now = getCurrentIsoTimestamp()

        // 1. Scan image for candlesticks & scale
        val scanResult = chartScanner.scanChart(bitmap, activeTimeframe)

        if (scanResult is ChartScanResult.Invalid) {
            return TradingSignal(
                signal = SignalType.WAIT,
                pair = "MT5 CHART",
                timeframe = activeTimeframe.code,
                entry = null,
                stopLoss = null,
                takeProfit = null,
                riskReward = "1:2",
                confidence = 0.0,
                trend = "UNKNOWN",
                confirmation = ConfirmationType.UNCONFIRMED,
                reason = scanResult.reason,
                timestamp = now,
                metrics = TechnicalMetrics()
            )
        }

        val success = scanResult as ChartScanResult.Success
        val candles = success.candles
        val detectedTf = success.detectedTimeframe

        // 2. Timeframe Consistency Check
        if (detectedTf != null && detectedTf != activeTimeframe) {
            return TradingSignal(
                signal = SignalType.WAIT,
                pair = success.detectedPair,
                timeframe = activeTimeframe.code,
                entry = null,
                stopLoss = null,
                takeProfit = null,
                riskReward = "1:2",
                confidence = 0.0,
                trend = "MISMATCH",
                confirmation = ConfirmationType.UNCONFIRMED,
                reason = "PERINGATAN TIMEFRAME: Timeframe aplikasi (${activeTimeframe.code}) berbeda dengan chart MT5 (${detectedTf.code}). Sesuaikan pilihan timeframe Anda.",
                timestamp = now,
                metrics = TechnicalMetrics(candleCount = candles.size)
            )
        }

        // 3. Calculate Technical Indicators
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

        // 4. Identify Trend & Momentum Direction
        val isBullishTrend = latestFastEma > latestSlowEma && latestCandle.close >= latestSlowEma
        val isBearishTrend = latestFastEma < latestSlowEma && latestCandle.close <= latestSlowEma
        val isEmaGoldenCross = prevFastEma <= prevSlowEma && latestFastEma > latestSlowEma
        val isEmaDeathCross = prevFastEma >= prevSlowEma && latestFastEma < latestSlowEma

        val trendName = when {
            isBullishTrend -> "BULLISH"
            isBearishTrend -> "BEARISH"
            else -> "SIDEWAYS"
        }

        // 5. Candle Pattern Confirmation
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

        // 6. Confluence & Deterministic Confidence Scoring (0 - 100)
        var buyScore = 0.0
        var sellScore = 0.0

        // Factor A: Trend Confluence (max 25 pts)
        if (isBullishTrend) buyScore += 20.0
        if (isEmaGoldenCross) buyScore += 5.0
        if (isBearishTrend) sellScore += 20.0
        if (isEmaDeathCross) sellScore += 5.0

        // Factor B: RSI Momentum Confluence (max 25 pts)
        when {
            // Healthy Bullish Momentum: 50 <= RSI <= 68
            rsi in 50.0..68.0 -> buyScore += 25.0
            rsi in 45.0..50.0 -> buyScore += 15.0
            // Healthy Bearish Momentum: 32 <= RSI <= 50
            rsi in 32.0..50.0 -> sellScore += 25.0
            rsi in 50.0..55.0 -> sellScore += 15.0
            // Overbought / Oversold Penalties
            rsi > 70.0 -> buyScore -= 20.0 // Danger of buying at the top
            rsi < 30.0 -> sellScore -= 20.0 // Danger of selling at the bottom
        }

        // Factor C: Candlestick Confirmation (max 25 pts)
        if (isBullEngulf || isHammer || isStrongBull) buyScore += 25.0
        else if (latestCandle.isBullish && latestCandle.lowerWickRatio > 0.3) buyScore += 15.0

        if (isBearEngulf || isShootingStar || isStrongBear) sellScore += 25.0
        else if (!latestCandle.isBullish && latestCandle.upperWickRatio > 0.3) sellScore += 15.0

        // Factor D: Key Level Proximity / S&R (max 15 pts)
        val distToSupport = Math.abs(latestCandle.close - support)
        val distToResistance = Math.abs(resistance - latestCandle.close)
        if (distToSupport < (atr * 1.2) && latestCandle.isBullish) buyScore += 15.0
        if (distToResistance < (atr * 1.2) && !latestCandle.isBullish) sellScore += 15.0

        // Factor E: Quality of visual capture (max 10 pts)
        val qualityBonus = success.confidenceQuality * 10.0
        buyScore += qualityBonus
        sellScore += qualityBonus

        val finalBuyConfidence = buyScore.coerceIn(0.0, 98.0)
        val finalSellConfidence = sellScore.coerceIn(0.0, 98.0)

        // 7. Signal Determination
        val entryPrice = latestCandle.close
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

        val currentTimeMs = System.currentTimeMillis()
        val inCooldown = (currentTimeMs - lastSignalTimeMs) < signalCooldownMs

        if (finalBuyConfidence >= CONFIDENCE_THRESHOLD && isBullishTrend && rsi <= 68.0) {
            val slDistance = (atr * 1.5).coerceAtLeast(0.00060)
            val tpDistance = slDistance * 2.0 // 1:2 R:R
            val stopLoss = entryPrice - slDistance
            val takeProfit = entryPrice + tpDistance

            val reason = "Konfluensi BUY (${finalBuyConfidence.toInt()}%): Trend EMA Bullish, RSI ${String.format(Locale.US, "%.1f", rsi)}, $patternName. Pantulan Support ${String.format(Locale.US, "%.5f", support)}."

            val signal = TradingSignal(
                signal = SignalType.BUY,
                pair = success.detectedPair,
                timeframe = activeTimeframe.code,
                entry = entryPrice,
                stopLoss = stopLoss,
                takeProfit = takeProfit,
                riskReward = "1:2",
                confidence = finalBuyConfidence,
                trend = trendName,
                confirmation = if (isBullEngulf || isHammer) ConfirmationType.CONFIRMED else ConfirmationType.PENDING,
                reason = reason,
                timestamp = now,
                metrics = metrics
            )
            lastSignal = signal
            lastSignalTimeMs = currentTimeMs
            return signal
        } else if (finalSellConfidence >= CONFIDENCE_THRESHOLD && isBearishTrend && rsi >= 32.0) {
            val slDistance = (atr * 1.5).coerceAtLeast(0.00060)
            val tpDistance = slDistance * 2.0 // 1:2 R:R
            val stopLoss = entryPrice + slDistance
            val takeProfit = entryPrice - tpDistance

            val reason = "Konfluensi SELL (${finalSellConfidence.toInt()}%): Trend EMA Bearish, RSI ${String.format(Locale.US, "%.1f", rsi)}, $patternName. Penolakan Resistance ${String.format(Locale.US, "%.5f", resistance)}."

            val signal = TradingSignal(
                signal = SignalType.SELL,
                pair = success.detectedPair,
                timeframe = activeTimeframe.code,
                entry = entryPrice,
                stopLoss = stopLoss,
                takeProfit = takeProfit,
                riskReward = "1:2",
                confidence = finalSellConfidence,
                trend = trendName,
                confirmation = if (isBearEngulf || isShootingStar) ConfirmationType.CONFIRMED else ConfirmationType.PENDING,
                reason = reason,
                timestamp = now,
                metrics = metrics
            )
            lastSignal = signal
            lastSignalTimeMs = currentTimeMs
            return signal
        } else {
            // WAIT Conditions
            val reasonWait = when {
                rsi > 70.0 -> "WAIT: RSI Overbought (${String.format(Locale.US, "%.1f", rsi)}) - Risiko koreksi tinggi, jangan kejar BUY."
                rsi < 30.0 -> "WAIT: RSI Oversold (${String.format(Locale.US, "%.1f", rsi)}) - Risiko rebound tinggi, jangan kejar SELL."
                trendName == "SIDEWAYS" -> "WAIT: Pasar Sideways/Konsolidasi (EMA 9 & 21 saling berhimpit). Tunggu breakout terkonfirmasi."
                finalBuyConfidence in 50.0..69.9 -> "WAIT: Setup Bullish belum matang (Confidence ${finalBuyConfidence.toInt()}% < 70%). Menunggu candle close."
                finalSellConfidence in 50.0..69.9 -> "WAIT: Setup Bearish belum matang (Confidence ${finalSellConfidence.toInt()}% < 70%). Menunggu candle close."
                else -> "WAIT: Kondisi pasar belum memenuhi konfluensi multi-indikator Universal Engine."
            }

            return TradingSignal(
                signal = SignalType.WAIT,
                pair = success.detectedPair,
                timeframe = activeTimeframe.code,
                entry = null,
                stopLoss = null,
                takeProfit = null,
                riskReward = "1:2",
                confidence = maxOf(finalBuyConfidence, finalSellConfidence),
                trend = trendName,
                confirmation = ConfirmationType.UNCONFIRMED,
                reason = reasonWait,
                timestamp = now,
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
