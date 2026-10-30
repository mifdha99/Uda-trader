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

data class TradingSignal(
    val id: Long = 0,
    val signal: SignalType,
    val pair: String,
    val timeframe: String,
    val entry: Double?,
    val stopLoss: Double?,
    val takeProfit: Double?,
    val riskReward: String?,
    val confidence: Double?, // null if not calibrated
    val trend: String,
    val confirmation: ConfirmationType,
    val reason: String,
    val timestamp: String
) {
    fun displaySignalText(): String {
        return when (signal) {
            SignalType.BUY -> "BUY NOW"
            SignalType.SELL -> "SELL NOW"
            SignalType.WAIT -> "WAIT"
        }
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
     * Disclaimer: Assumes standard pip values (e.g. 1 pip = $10 per standard lot in EURUSD).
     * Instruments like Gold (XAUUSD), Indices, or Crypto have different pip values.
     */
    fun calculateEstimatedLot(): Double {
        if (stopLossPips <= 0) return 0.0
        // Forex standard: 1 pip = $10 / lot for standard accounts (EURUSD base)
        val pipValuePerLot = when (instrumentType) {
            "Gold (XAUUSD)" -> 10.0 // 0.10 move = $10 on 1 lot
            "Crypto (BTCUSD)" -> 1.0
            "Index (US30/NAS100)" -> 1.0
            else -> 10.0 // Forex
        }
        val totalRiskPerLot = stopLossPips * pipValuePerLot
        if (totalRiskPerLot <= 0) return 0.0
        val calculated = riskAmount / totalRiskPerLot
        return (Math.round(calculated * 100.0) / 100.0).coerceAtLeast(0.01)
    }
}
