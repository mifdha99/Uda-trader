package com.example.data.engine

/**
 * Standard OHLC Candlestick structure extracted from visual MT5 chart scanning.
 */
data class Candlestick(
    val index: Int,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val isBullish: Boolean = close >= open,
    val bodySize: Double = Math.abs(close - open),
    val totalRange: Double = (high - low).coerceAtLeast(0.00001),
    val upperWick: Double = (high - maxOf(open, close)).coerceAtLeast(0.0),
    val lowerWick: Double = (minOf(open, close) - low).coerceAtLeast(0.0)
) {
    val bodyRatio: Double get() = (bodySize / totalRange).coerceIn(0.0, 1.0)
    val upperWickRatio: Double get() = (upperWick / totalRange).coerceIn(0.0, 1.0)
    val lowerWickRatio: Double get() = (lowerWick / totalRange).coerceIn(0.0, 1.0)

    /**
     * Identifies candlestick patterns based on price action morphology.
     */
    fun isHammer(): Boolean {
        // Lower wick is at least 2x the body, small upper wick
        return lowerWick >= (bodySize * 1.8) && upperWick <= (bodySize * 0.8) && bodyRatio <= 0.4
    }

    fun isShootingStar(): Boolean {
        // Upper wick is at least 2x the body, small lower wick
        return upperWick >= (bodySize * 1.8) && lowerWick <= (bodySize * 0.8) && bodyRatio <= 0.4
    }

    fun isDoji(): Boolean {
        return bodyRatio < 0.1
    }

    fun isStrongMarubozu(): Boolean {
        return bodyRatio >= 0.75
    }
}
