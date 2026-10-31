package com.example.model

/**
 * 10 Explicit Universal Engine States as specified in project requirements:
 * 1. MENUNGGU CHART: Waiting for trading chart on screen
 * 2. MEMBACA CHART: Capturing and reading screen pixels
 * 3. MEMVALIDASI DATA: Validating candlestick structure and visual layout
 * 4. MENGANALISIS: Computing indicators, momentum, and confluence
 * 5. BUY: High-probability Bullish confluence (>= 70%)
 * 6. SELL: High-probability Bearish confluence (>= 70%)
 * 7. WAIT: No conclusive setup, sideways, or overbought/oversold
 * 8. DATA TIDAK VALID: Screen is not a chart, resolution too low, or corrupted
 * 9. LIVE SCREEN TERPUTUS: MediaProjection disconnected or permission revoked
 * 10. DATA KEDALUWARSA: Data older than threshold (>15s without fresh frames)
 */
enum class EngineState(val displayName: String) {
    WAITING_CHART("MENUNGGU CHART"),
    READING_CHART("MEMBACA CHART"),
    VALIDATING_DATA("MEMVALIDASI DATA"),
    ANALYZING("MENGANALISIS"),
    BUY("BUY NOW"),
    SELL("SELL NOW"),
    WAIT("WAIT"),
    INVALID_DATA("DATA TIDAK VALID"),
    STREAM_DISCONNECTED("LIVE SCREEN TERPUTUS"),
    DATA_EXPIRED("DATA KEDALUWARSA");

    fun isActionableSignal(): Boolean = this == BUY || this == SELL
}

enum class DataVerificationLevel(val label: String) {
    VERIFIED("TERVERIFIKASI"),
    VISUAL_ESTIMATION("ESTIMASI VISUAL"),
    INVALID("TIDAK VALID")
}

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
 * Standard 9 MT5 Timeframes
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
    val state: EngineState = EngineState.WAIT,
    val signal: SignalType = SignalType.WAIT,
    val pair: String = "TIDAK DIKENALI",
    val timeframe: String = "M5",
    val detectedTimeframe: String? = null,
    val isTimeframeMismatch: Boolean = false,
    val verificationLevel: DataVerificationLevel = DataVerificationLevel.VISUAL_ESTIMATION,
    val entry: Double? = null,
    val stopLoss: Double? = null,
    val takeProfit: Double? = null,
    val riskReward: String? = "1:2",
    val confidence: Double? = 0.0, // 0.0 - 100.0%
    val trend: String = "SIDEWAYS",
    val confirmation: ConfirmationType = ConfirmationType.UNCONFIRMED,
    val reason: String = "",
    val timestamp: String = "",
    val timestampMillis: Long = System.currentTimeMillis(),
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

    fun formattedSymbol(): String {
        return if (pair.isBlank() || pair.equals("MT5", ignoreCase = true) || pair.equals("EURUSD", ignoreCase = true) && verificationLevel != DataVerificationLevel.VERIFIED) {
            "TIDAK DIKENALI"
        } else {
            pair
        }
    }

    fun formattedEntry(): String {
        return entry?.let { String.format(java.util.Locale.US, "%.5f", it) } ?: "TIDAK TERSEDIA"
    }

    fun formattedStopLoss(): String {
        return stopLoss?.let { String.format(java.util.Locale.US, "%.5f", it) } ?: "TIDAK TERSEDIA"
    }

    fun formattedTakeProfit(): String {
        return takeProfit?.let { String.format(java.util.Locale.US, "%.5f", it) } ?: "TIDAK TERSEDIA"
    }

    fun formattedTimeframeStatus(): String {
        return when {
            isTimeframeMismatch -> "TIDAK TERKONFIRMASI (Beda: ${detectedTimeframe ?: "?"})"
            detectedTimeframe != null -> "TERKONFIRMASI ($detectedTimeframe)"
            else -> "TIDAK TERKONFIRMASI"
        }
    }

    /**
     * Formats signal copy text strictly adhering to Section 10 rules.
     */
    fun formattedCopyText(): String {
        return """
            [UDA AI TRADER]
            Sinyal: ${displaySignalText()}
            Simbol: ${formattedSymbol()}
            Timeframe: $timeframe (${formattedTimeframeStatus()})
            Entry: ${formattedEntry()}
            SL: ${formattedStopLoss()}
            TP: ${formattedTakeProfit()}
            Risk/Reward: ${riskReward ?: "1:2"}
            Confidence: ${formattedConfidence()}
            Status: ${state.displayName}
            Waktu: $timestamp
        """.trimIndent()
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
