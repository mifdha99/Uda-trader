package com.example.data.engine

import kotlin.math.abs
import kotlin.math.max

object TechnicalIndicators {

    /**
     * Calculates Exponential Moving Average (EMA) for a series of closing prices.
     */
    fun calculateEMA(prices: List<Double>, period: Int): List<Double> {
        if (prices.isEmpty() || period <= 0) return emptyList()
        val emaList = ArrayList<Double>(prices.size)
        val k = 2.0 / (period + 1.0)

        // Simple Moving Average for initial seed
        val seedCount = minOf(period, prices.size)
        var sum = 0.0
        for (i in 0 until seedCount) {
            sum += prices[i]
            emaList.add(sum / (i + 1))
        }

        var currentEma = emaList.last()
        for (i in seedCount until prices.size) {
            currentEma = (prices[i] * k) + (currentEma * (1.0 - k))
            emaList.add(currentEma)
        }

        return emaList
    }

    /**
     * Calculates Relative Strength Index (RSI) using standard Wilder's smoothing.
     * Returns a value between 0.0 and 100.0.
     */
    fun calculateRSI(prices: List<Double>, period: Int = 14): Double {
        if (prices.size < period + 1) {
            return 50.0 // Default neutral if insufficient samples
        }

        var avgGain = 0.0
        var avgLoss = 0.0

        for (i in 1..period) {
            val change = prices[i] - prices[i - 1]
            if (change > 0) {
                avgGain += change
            } else {
                avgLoss += abs(change)
            }
        }

        avgGain /= period
        avgLoss /= period

        for (i in (period + 1) until prices.size) {
            val change = prices[i] - prices[i - 1]
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) abs(change) else 0.0

            avgGain = ((avgGain * (period - 1)) + gain) / period
            avgLoss = ((avgLoss * (period - 1)) + loss) / period
        }

        if (avgLoss == 0.0) {
            return if (avgGain == 0.0) 50.0 else 100.0
        }

        val rs = avgGain / avgLoss
        return (100.0 - (100.0 / (1.0 + rs))).coerceIn(0.0, 100.0)
    }

    /**
     * Calculates Average True Range (ATR) over candles.
     */
    fun calculateATR(candles: List<Candlestick>, period: Int = 14): Double {
        if (candles.size < 2) return 0.00100 // fallback 10 pips

        val trueRanges = ArrayList<Double>(candles.size)
        trueRanges.add(candles[0].high - candles[0].low)

        for (i in 1 until candles.size) {
            val current = candles[i]
            val prevClose = candles[i - 1].close
            val tr = max(
                current.high - current.low,
                max(
                    abs(current.high - prevClose),
                    abs(current.low - prevClose)
                )
            )
            trueRanges.add(tr)
        }

        val effPeriod = minOf(period, trueRanges.size)
        var atr = 0.0
        for (i in 0 until effPeriod) {
            atr += trueRanges[i]
        }
        atr /= effPeriod

        for (i in effPeriod until trueRanges.size) {
            atr = ((atr * (period - 1)) + trueRanges[i]) / period
        }

        return atr.coerceAtLeast(0.00010)
    }

    /**
     * Identifies nearest Support & Resistance levels from recent swing pivots.
     */
    fun calculateSupportResistance(candles: List<Candlestick>): Pair<Double, Double> {
        if (candles.size < 5) {
            val lastClose = candles.lastOrNull()?.close ?: 1.08500
            return Pair(lastClose - 0.00200, lastClose + 0.00200)
        }

        val highs = candles.map { it.high }
        val lows = candles.map { it.low }
        val currentPrice = candles.last().close

        val swingHighs = ArrayList<Double>()
        val swingLows = ArrayList<Double>()

        for (i in 2 until candles.size - 2) {
            if (highs[i] >= highs[i - 1] && highs[i] >= highs[i - 2] &&
                highs[i] >= highs[i + 1] && highs[i] >= highs[i + 2]
            ) {
                swingHighs.add(highs[i])
            }
            if (lows[i] <= lows[i - 1] && lows[i] <= lows[i - 2] &&
                lows[i] <= lows[i + 1] && lows[i] <= lows[i + 2]
            ) {
                swingLows.add(lows[i])
            }
        }

        val resistance = swingHighs.filter { it > currentPrice }.minOrNull()
            ?: (highs.maxOrNull() ?: (currentPrice + 0.00200))
        val support = swingLows.filter { it < currentPrice }.maxOrNull()
            ?: (lows.minOrNull() ?: (currentPrice - 0.00200))

        return Pair(support, resistance)
    }

    /**
     * Checks if the latest candle forms a Bullish Engulfing pattern.
     */
    fun isBullishEngulfing(prev: Candlestick, current: Candlestick): Boolean {
        return !prev.isBullish && current.isBullish &&
            current.close > prev.open && current.open <= prev.close &&
            current.bodySize > (prev.bodySize * 1.1)
    }

    /**
     * Checks if the latest candle forms a Bearish Engulfing pattern.
     */
    fun isBearishEngulfing(prev: Candlestick, current: Candlestick): Boolean {
        return prev.isBullish && !current.isBullish &&
            current.close < prev.open && current.open >= prev.close &&
            current.bodySize > (prev.bodySize * 1.1)
    }
}
