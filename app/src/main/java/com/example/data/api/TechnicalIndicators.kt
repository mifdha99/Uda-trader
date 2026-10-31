package com.example.data.api

import com.example.model.Candle
import com.example.model.CandlePattern
import com.example.model.SignalType
import com.example.model.TechnicalAnalysisResult

object TechnicalIndicators {

    /**
     * Calculates Exponential Moving Average (EMA) for a given series.
     */
    fun calculateEMA(data: List<Double>, period: Int): List<Double> {
        if (data.size < period || period <= 0) return emptyList()

        val multiplier = 2.0 / (period + 1.0)
        val result = mutableListOf<Double>()

        // Initial EMA is the Simple Moving Average (SMA) of first 'period' elements
        var sma = 0.0
        for (i in 0 until period) {
            sma += data[i]
        }
        sma /= period
        result.add(sma)

        // Calculate subsequent EMAs
        for (i in period until data.size) {
            val prevEma = result.last()
            val currentEma = (data[i] - prevEma) * multiplier + prevEma
            result.add(currentEma)
        }

        return result
    }

    /**
     * Calculates Relative Strength Index (RSI) for closing prices over a specified period (default 14).
     */
    fun calculateRSI(closes: List<Double>, period: Int = 14): Double? {
        if (closes.size <= period || period <= 0) return null

        var gainSum = 0.0
        var lossSum = 0.0

        for (i in 1..period) {
            val change = closes[i] - closes[i - 1]
            if (change > 0) gainSum += change else lossSum += Math.abs(change)
        }

        var avgGain = gainSum / period
        var avgLoss = lossSum / period

        // Wilder's smoothing for subsequent data
        for (i in (period + 1) until closes.size) {
            val change = closes[i] - closes[i - 1]
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) Math.abs(change) else 0.0

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
        }

        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    /**
     * Calculates Average True Range (ATR) over period (default 14).
     */
    fun calculateATR(
        highs: List<Double>,
        lows: List<Double>,
        closes: List<Double>,
        period: Int = 14
    ): Double? {
        val size = minOf(highs.size, lows.size, closes.size)
        if (size <= period || period <= 0) return null

        val trueRanges = mutableListOf<Double>()
        for (i in 1 until size) {
            val high = highs[i]
            val low = lows[i]
            val prevClose = closes[i - 1]

            val tr = maxOf(
                high - low,
                Math.abs(high - prevClose),
                Math.abs(low - prevClose)
            )
            trueRanges.add(tr)
        }

        if (trueRanges.size < period) return null
        var trSum = 0.0
        for (i in 0 until period) {
            trSum += trueRanges[i]
        }
        var atr = trSum / period

        for (i in period until trueRanges.size) {
            atr = (atr * (period - 1) + trueRanges[i]) / period
        }

        return atr
    }

    /**
     * Identifies key support and resistance zones using pivot swing highs and lows.
     */
    fun detectSupportResistance(candles: List<Candle>): Pair<List<Double>, List<Double>> {
        val supports = mutableListOf<Double>()
        val resistances = mutableListOf<Double>()
        if (candles.size < 5) return Pair(supports, resistances)

        // Swing highs & lows over 5-candle window
        for (i in 2 until (candles.size - 2)) {
            val current = candles[i]
            val isSwingHigh = current.high > candles[i - 1].high &&
                current.high > candles[i - 2].high &&
                current.high > candles[i + 1].high &&
                current.high > candles[i + 2].high

            val isSwingLow = current.low < candles[i - 1].low &&
                current.low < candles[i - 2].low &&
                current.low < candles[i + 1].low &&
                current.low < candles[i + 2].low

            if (isSwingHigh) resistances.add(current.high)
            if (isSwingLow) supports.add(current.low)
        }

        return Pair(supports.takeLast(3), resistances.takeLast(3))
    }

    /**
     * Identifies Japanese Candlestick Patterns on latest closed candles.
     */
    fun detectCandlePattern(current: Candle, previous: Candle?): CandlePattern {
        if (current.totalRange <= 0.0) return CandlePattern.NONE

        // Hammer (lower wick >= 2x body and >= 50% total range, small upper wick)
        if (current.lowerWick >= (2.0 * current.bodySize) &&
            current.lowerWick >= (0.5 * current.totalRange) &&
            current.upperWick <= (0.35 * current.bodySize)
        ) {
            return CandlePattern.HAMMER
        }

        // Shooting Star (upper wick >= 2x body and >= 50% total range, small lower wick)
        if (current.upperWick >= (2.0 * current.bodySize) &&
            current.upperWick >= (0.5 * current.totalRange) &&
            current.lowerWick <= (0.35 * current.bodySize)
        ) {
            return CandlePattern.SHOOTING_STAR
        }

        // Doji (body is 10% or less of total range and neither wick constitutes a hammer)
        if (current.bodySize / current.totalRange <= 0.10) {
            return CandlePattern.DOJI
        }

        // Engulfing patterns require previous candle
        if (previous != null) {
            if (previous.isBearish && current.isBullish &&
                current.open <= previous.close && current.close >= previous.open
            ) {
                return CandlePattern.BULLISH_ENGULFING
            }

            if (previous.isBullish && current.isBearish &&
                current.open >= previous.close && current.close <= previous.open
            ) {
                return CandlePattern.BEARISH_ENGULFING
            }
        }

        return CandlePattern.NONE
    }

    /**
     * Quantitative Institutional Technical Analysis confluence evaluation.
     */
    fun evaluateMarketData(
        candles: List<Candle>,
        pairName: String,
        timeframe: String
    ): TechnicalAnalysisResult {
        if (candles.size < 25) {
            return TechnicalAnalysisResult(
                ema9 = null,
                ema21 = null,
                rsi14 = null,
                atr14 = null,
                supportLevels = emptyList(),
                resistanceLevels = emptyList(),
                detectedPattern = CandlePattern.NONE,
                trend = "INSUFFICIENT_DATA",
                confidenceScore = 0.0,
                signal = SignalType.WAIT,
                recommendation = "WAIT",
                durationSeconds = 60,
                reason = "Data candle tidak mencukupi untuk indikator teknikal (dibutuhkan min. 25 candle, tersedia ${candles.size})."
            )
        }

        val closes = candles.map { it.close }
        val highs = candles.map { it.high }
        val lows = candles.map { it.low }

        val ema9Series = calculateEMA(closes, 9)
        val ema21Series = calculateEMA(closes, 21)

        val latestEma9 = ema9Series.lastOrNull()
        val latestEma21 = ema21Series.lastOrNull()
        val latestRsi = calculateRSI(closes, 14)
        val latestAtr = calculateATR(highs, lows, closes, 14)

        val (supports, resistances) = detectSupportResistance(candles)
        val currentCandle = candles.last()
        val prevCandle = candles.getOrNull(candles.size - 2)
        val pattern = detectCandlePattern(currentCandle, prevCandle)

        var trend = "SIDEWAYS"
        if (latestEma9 != null && latestEma21 != null) {
            val diff = (latestEma9 - latestEma21) / latestEma21
            if (diff > 0.0001) trend = "BULLISH"
            else if (diff < -0.0001) trend = "BEARISH"
        }

        // Confluence Scoring (0.0 to 1.0)
        var upConfluence = 0.0
        var downConfluence = 0.0

        // 1. EMA Rule (+0.30)
        if (trend == "BULLISH" && currentCandle.close >= (latestEma9 ?: 0.0)) {
            upConfluence += 0.30
        } else if (trend == "BEARISH" && currentCandle.close <= (latestEma9 ?: 0.0)) {
            downConfluence += 0.30
        }

        // 2. RSI Rule (+0.25)
        if (latestRsi != null) {
            if (latestRsi in 45.0..68.0 && trend == "BULLISH") {
                upConfluence += 0.25
            } else if (latestRsi in 32.0..55.0 && trend == "BEARISH") {
                downConfluence += 0.25
            } else if (latestRsi <= 30.0) {
                // Oversold bounce potential
                upConfluence += 0.15
            } else if (latestRsi >= 70.0) {
                // Overbought rejection potential
                downConfluence += 0.15
            }
        }

        // 3. Candlestick Pattern Rule (+0.25)
        when (pattern) {
            CandlePattern.BULLISH_ENGULFING, CandlePattern.HAMMER -> upConfluence += 0.25
            CandlePattern.BEARISH_ENGULFING, CandlePattern.SHOOTING_STAR -> downConfluence += 0.25
            CandlePattern.DOJI -> {
                // Indecision reduces both
                upConfluence -= 0.10
                downConfluence -= 0.10
            }
            else -> {}
        }

        // 4. Support / Resistance Proximity (+0.20)
        val nearestSupport = supports.minByOrNull { Math.abs(it - currentCandle.close) }
        val nearestResistance = resistances.minByOrNull { Math.abs(it - currentCandle.close) }

        if (nearestSupport != null && Math.abs(currentCandle.close - nearestSupport) <= (latestAtr ?: 0.0010)) {
            if (currentCandle.close >= nearestSupport) upConfluence += 0.20
        }
        if (nearestResistance != null && Math.abs(currentCandle.close - nearestResistance) <= (latestAtr ?: 0.0010)) {
            if (currentCandle.close <= nearestResistance) downConfluence += 0.20
        }

        val clampedUp = upConfluence.coerceIn(0.0, 1.0)
        val clampedDown = downConfluence.coerceIn(0.0, 1.0)

        val duration = when (timeframe.uppercase()) {
            "5S", "15S" -> 30
            "1M", "M1" -> 60
            "5M", "M5" -> 300
            else -> 60
        }

        val threshold = 0.65

        return if (clampedUp >= threshold && clampedUp > clampedDown) {
            TechnicalAnalysisResult(
                ema9 = latestEma9,
                ema21 = latestEma21,
                rsi14 = latestRsi,
                atr14 = latestAtr,
                supportLevels = supports,
                resistanceLevels = resistances,
                detectedPattern = pattern,
                trend = trend,
                confidenceScore = Math.round(clampedUp * 100.0) / 100.0,
                signal = SignalType.BUY,
                recommendation = "UP / CALL",
                durationSeconds = duration,
                reason = "Konfluensi Bullish: EMA 9 > EMA 21, RSI=${latestRsi?.let { String.format("%.1f", it) } ?: "N/A"}, Pola=${pattern.name}."
            )
        } else if (clampedDown >= threshold && clampedDown > clampedUp) {
            TechnicalAnalysisResult(
                ema9 = latestEma9,
                ema21 = latestEma21,
                rsi14 = latestRsi,
                atr14 = latestAtr,
                supportLevels = supports,
                resistanceLevels = resistances,
                detectedPattern = pattern,
                trend = trend,
                confidenceScore = Math.round(clampedDown * 100.0) / 100.0,
                signal = SignalType.SELL,
                recommendation = "DOWN / PUT",
                durationSeconds = duration,
                reason = "Konfluensi Bearish: EMA 9 < EMA 21, RSI=${latestRsi?.let { String.format("%.1f", it) } ?: "N/A"}, Pola=${pattern.name}."
            )
        } else {
            TechnicalAnalysisResult(
                ema9 = latestEma9,
                ema21 = latestEma21,
                rsi14 = latestRsi,
                atr14 = latestAtr,
                supportLevels = supports,
                resistanceLevels = resistances,
                detectedPattern = pattern,
                trend = trend,
                confidenceScore = Math.round(maxOf(clampedUp, clampedDown) * 100.0) / 100.0,
                signal = SignalType.WAIT,
                recommendation = "WAIT",
                durationSeconds = duration,
                reason = "Kondisi pasar belum memenuhi ambang batas konfluensi (${String.format("%.0f", maxOf(clampedUp, clampedDown) * 100)}% < 65%). Menunggu sinyal terkonfirmasi."
            )
        }
    }
}
