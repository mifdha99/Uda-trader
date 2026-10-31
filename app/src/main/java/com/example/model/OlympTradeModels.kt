package com.example.model

enum class DataSourceMode {
    MT5_VISION,
    OLYMP_TRADE_WS
}

enum class AutoTradingState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DEMO_MODE,
    ANALYZING,
    ORDER_PENDING,
    ORDER_CONFIRMED,
    ORDER_FAILED,
    RECONNECTING,
    STOPPED
}

data class AutoTradingConfig(
    val isEnabled: Boolean = false,
    val requireDemoOnly: Boolean = true,
    val targetGroup: String = "demo", // strictly "demo" by default
    val tradeAmount: Double = 1.0, // default $1 demo
    val contractDurationSeconds: Int = 60, // 1 minute default
    val dailyMaxTrades: Int = 20,
    val dailyLossLimit: Double = 25.0, // max loss in $ before emergency halt
    val minConfidenceScore: Double = 0.70, // 70% minimum confluence
    val cooldownSecondsBetweenTrades: Int = 15
)

data class OlympOrder(
    val localId: String,
    val serverOrderId: String?,
    val pair: String,
    val direction: String, // "up" or "down"
    val amount: Double,
    val durationSeconds: Int,
    val group: String, // "demo"
    val status: String, // "PENDING", "CONFIRMED", "WON", "LOST", "FAILED"
    val entryPrice: Double?,
    val exitPrice: Double?,
    val profit: Double?,
    val timestamp: String,
    val errorReason: String? = null
)

data class OlympAccountBalance(
    val accountId: String,
    val group: String, // "demo" or "real"
    val currency: String,
    val amount: Double
)
