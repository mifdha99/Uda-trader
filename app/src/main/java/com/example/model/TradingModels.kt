package com.example.model

enum class SignalType {
    BUY,
    SELL,
    WAIT;

    companion object {
        fun fromString(value: String?): SignalType {
            return when (value?.trim()?.uppercase()) {
                "BUY", "BUY NOW" -> BUY
                "SELL", "SELL NOW" -> SELL
                else -> WAIT
            }
        }
    }
}

enum class ConfirmationType {
    CONFIRMED,
    PENDING,
    UNCONFIRMED;

    companion object {
        fun fromString(value: String?): ConfirmationType {
            return when (value?.trim()?.uppercase()) {
                "CONFIRMED" -> CONFIRMED
                "PENDING" -> PENDING
                else -> UNCONFIRMED
            }
        }
    }
}

/**
 * Standard MT5 Timeframes
 */
enum class Mt5Timeframe(val code: String, val displayName: String, val seconds: Long) {
    M1("M1", "1 Menit", 60),
    M5("M5", "5 Menit", 300),
    M15("M15", "15 Menit", 900),
    M30("M30", "30 Menit", 1800),
    H1("H1", "1 Jam", 3600),
    H4("H4", "4 Jam", 14400),
    D1("D1", "Harian", 86400),
    W1("W1", "Mingguan", 604800),
    MN1("MN1", "Bulanan", 2592000);

    companion object {
        fun fromCode(code: String?): Mt5Timeframe {
            val clean = code?.trim()?.uppercase() ?: return M5
            return values().firstOrNull { it.code == clean } ?: M5
        }
    }
}

data class TechnicalMetrics(
    val fastEma: Double? = null,
    val slowEma: Double? = null,
    val rsi: Double? = null,
    val atr: Double? = null,
    val supportLevel: Double? = null,
    val resistanceLevel: Double? = null,
    val candlePattern: String = "Normal",
    val candleCount: Int = 0
)

data class TradingSignal(
    val id: Long = 0,
    val signal: SignalType,
    val pair: String,
    val timeframe: String,
    val entry: Double?,
    val stopLoss: Double?,
    val takeProfit: Double?,
    val riskReward: String?,
    val confidence: Double?, // 0.0 - 100.0%
    val trend: String,
    val confirmation: ConfirmationType,
    val reason: String,
    val timestamp: String,
    val metrics: TechnicalMetrics = TechnicalMetrics()
) {
    fun displaySignalText(): String {
        return when (signal) {
            SignalType.BUY -> "BUY NOW"
            SignalType.SELL -> "SELL NOW"
            SignalType.WAIT -> "WAIT"
        }
    }

    fun formattedConfidence(): String {
        return confidence?.let { String.format("%.0f%%", it) } ?: "0%"
    }
}

data class RiskSettings(
    val accountBalance: Double = 1000.0,
    val riskPercentage: Double = 1.0,
    val riskRewardRatio: Double = 2.0,
    val stopLossPips: Double = 20.0,
    val instrumentType: String = "Forex (Standard Lot)",
    val customContractSize: Double = 100000.0
) {
    val riskAmount: Double
        get() = (accountBalance * riskPercentage) / 100.0

    /**
     * Calculates estimated lot size based on risk and stop loss distance.
     */
    fun calculateEstimatedLot(): Double {
        if (stopLossPips <= 0) return 0.0
        val pipValuePerLot = when (instrumentType) {
            "Gold (XAUUSD)" -> 10.0
            "Crypto (BTCUSD)" -> 1.0
            "Index (US30/NAS100)" -> 1.0
            else -> 10.0
        }
        val totalRiskPerLot = stopLossPips * pipValuePerLot
        if (totalRiskPerLot <= 0) return 0.0
        val calculated = riskAmount / totalRiskPerLot
        return (Math.round(calculated * 100.0) / 100.0).coerceAtLeast(0.01)
    }
}
