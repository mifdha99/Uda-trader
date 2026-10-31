package com.example.data.api

import android.util.Log
import com.example.model.AutoTradingConfig
import com.example.model.AutoTradingState
import com.example.model.OlympOrder
import com.example.model.SignalType
import com.example.model.TradingSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AutoTradingManager(
    private val wsClient: OlympTradeWsClient,
    private val signalEngine: UniversalSignalEngine
) {
    companion object {
        private const val TAG = "AutoTradingManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val orderMutex = Mutex()
    private var loopJob: Job? = null

    private val _config = MutableStateFlow(AutoTradingConfig())
    val config: StateFlow<AutoTradingConfig> = _config.asStateFlow()

    private val _state = MutableStateFlow(AutoTradingState.STOPPED)
    val state: StateFlow<AutoTradingState> = _state.asStateFlow()

    private val _dailyTradeCount = MutableStateFlow(0)
    val dailyTradeCount: StateFlow<Int> = _dailyTradeCount.asStateFlow()

    private val _dailyAccumulatedLoss = MutableStateFlow(0.0)
    val dailyAccumulatedLoss: StateFlow<Double> = _dailyAccumulatedLoss.asStateFlow()

    private val _statusMessage = MutableStateFlow("Auto Trading nonaktif.")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _recentOrders = MutableStateFlow<List<OlympOrder>>(emptyList())
    val recentOrders: StateFlow<List<OlympOrder>> = _recentOrders.asStateFlow()

    private var lastOrderTimestamp = 0L
    private var lastProcessedSignalId = ""

    fun updateConfig(newConfig: AutoTradingConfig) {
        _config.value = newConfig
    }

    /**
     * Starts Auto Trading loop after validating safety conditions.
     */
    fun startAutoTrading() {
        val currentConfig = _config.value

        // Stage validation
        if (currentConfig.requireDemoOnly && currentConfig.targetGroup != "demo") {
            _statusMessage.value = "PENOLAKAN KEAMANAN: Auto Trading hanya diizinkan pada akun DEMO."
            _state.value = AutoTradingState.STOPPED
            return
        }

        if (_dailyTradeCount.value >= currentConfig.dailyMaxTrades) {
            _statusMessage.value = "Batas transaksi harian tercapai (${_dailyTradeCount.value}/${currentConfig.dailyMaxTrades})."
            _state.value = AutoTradingState.STOPPED
            return
        }

        if (_dailyAccumulatedLoss.value >= currentConfig.dailyLossLimit) {
            _statusMessage.value = "Batas kerugian harian tercapai ($${String.format("%.2f", _dailyAccumulatedLoss.value)} >= $${currentConfig.dailyLossLimit})."
            _state.value = AutoTradingState.STOPPED
            return
        }

        loopJob?.cancel()
        _state.value = AutoTradingState.ANALYZING
        _statusMessage.value = "Auto Trading Aktif (Akun DEMO). Memantau sinyal konfluensi..."

        loopJob = scope.launch {
            while (isActive) {
                delay(2000)

                val pair = wsClient.currentPair.value
                val candles = wsClient.candles.value

                if (candles.size < 25) {
                    _statusMessage.value = "TAHAP A: Mengumpulkan data candle (${candles.size}/25)..."
                    continue
                }

                // TAHAP B: Signal Test
                val signal = signalEngine.evaluateOlympTradeCandles(candles, pair, "1M")
                val confidence = signal.confidence ?: 0.0

                if (signal.signal == SignalType.WAIT) {
                    _statusMessage.value = "Menunggu konfluensi teknikal: ${signal.reason}"
                    continue
                }

                if (confidence < currentConfig.minConfidenceScore) {
                    _statusMessage.value = "Sinyal ${signal.signal} diabaikan (Keyakinan ${String.format("%.0f", confidence * 100)}% < ${String.format("%.0f", currentConfig.minConfidenceScore * 100)}%)."
                    continue
                }

                // TAHAP C: Demo Execution with Mutex & Idempotency Lock
                val now = System.currentTimeMillis()
                val cooldownMs = currentConfig.cooldownSecondsBetweenTrades * 1000L

                if (now - lastOrderTimestamp < cooldownMs) {
                    continue
                }

                val signalKey = "${pair}_${signal.signal}_${signal.timestamp}"
                if (signalKey == lastProcessedSignalId) {
                    continue // Prevent double order
                }

                executeDemoOrder(signal, currentConfig, signalKey)
            }
        }
    }

    private suspend fun executeDemoOrder(
        signal: TradingSignal,
        currentConfig: AutoTradingConfig,
        signalKey: String
    ) = orderMutex.withLock {
        // Re-check safety limits under lock
        if (_dailyTradeCount.value >= currentConfig.dailyMaxTrades) {
            emergencyStop("Batas maksimum transaksi harian tercapai.")
            return
        }
        if (_dailyAccumulatedLoss.value >= currentConfig.dailyLossLimit) {
            emergencyStop("Batas kerugian harian tercapai.")
            return
        }

        _state.value = AutoTradingState.ORDER_PENDING
        _statusMessage.value = "Mengirim order demo ${signal.signal} $${currentConfig.tradeAmount}..."

        val direction = if (signal.signal == SignalType.BUY) "up" else "down"
        val order = wsClient.placeOrder(
            pair = signal.pair,
            direction = direction,
            amount = currentConfig.tradeAmount,
            durationSeconds = currentConfig.contractDurationSeconds
        )

        lastOrderTimestamp = System.currentTimeMillis()
        lastProcessedSignalId = signalKey
        _dailyTradeCount.value += 1

        val currentList = _recentOrders.value.toMutableList()
        currentList.add(0, order)
        _recentOrders.value = currentList.take(50)

        // TAHAP D: Result Verification
        _state.value = AutoTradingState.ORDER_CONFIRMED
        _statusMessage.value = "Order ${order.localId} terkonfirmasi [${order.status}]. Memantau hasil kontrak..."
    }

    /**
     * Immediate emergency stop of all trading loops and order dispatch.
     */
    fun emergencyStop(reason: String = "EMERGENCY STOP DITEKAN PENGGUNA") {
        loopJob?.cancel()
        loopJob = null
        _state.value = AutoTradingState.STOPPED
        _statusMessage.value = "⚠️ $reason. Pengiriman order dihentikan total."
        Log.w(TAG, "Emergency Stop triggered: $reason")
    }

    fun stopAutoTrading() {
        loopJob?.cancel()
        loopJob = null
        _state.value = AutoTradingState.STOPPED
        _statusMessage.value = "Auto Trading dinonaktifkan."
    }

    fun resetDailyStats() {
        _dailyTradeCount.value = 0
        _dailyAccumulatedLoss.value = 0.0
        _statusMessage.value = "Statistik harian direset."
    }
}
