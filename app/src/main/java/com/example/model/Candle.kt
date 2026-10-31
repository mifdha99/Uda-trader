package com.example.model

data class Candle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double = 0.0
) {
    val isBullish: Boolean get() = close > open
    val isBearish: Boolean get() = close < open
    val bodySize: Double get() = Math.abs(close - open)
    val upperWick: Double get() = if (isBullish) high - close else high - open
    val lowerWick: Double get() = if (isBullish) open - low else close - low
    val totalRange: Double get() = high - low
}

enum class CandlePattern {
    BULLISH_ENGULFING,
    BEARISH_ENGULFING,
    HAMMER,
    SHOOTING_STAR,
    DOJI,
    NONE
}

data class TechnicalAnalysisResult(
    val ema9: Double?,
    val ema21: Double?,
    val rsi14: Double?,
    val atr14: Double?,
    val supportLevels: List<Double>,
    val resistanceLevels: List<Double>,
    val detectedPattern: CandlePattern,
    val trend: String, // "BULLISH", "BEARISH", "SIDEWAYS"
    val confidenceScore: Double, // 0.0 to 1.0
    val signal: SignalType, // BUY (UP), SELL (DOWN), WAIT
    val recommendation: String, // "UP / CALL", "DOWN / PUT", "WAIT"
    val durationSeconds: Int, // e.g. 60s
    val reason: String
)
