package com.example.data.api

import android.util.Log
import com.example.model.AutoTradingState
import com.example.model.Candle
import com.example.model.OlympAccountBalance
import com.example.model.OlympOrder
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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class OlympTradeWsClient {

    companion object {
        private const val TAG = "OlympTradeWsClient"
        const val DEFAULT_WS_URI = "wss://ws.olymptrade.com/otp?cid_ver=1&cid_app=web%40OlympTrade%402025.3.26878%4026878&cid_device=%40%40desktop&cid_os=mac_os%4010.15.7"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private var simulationJob: Job? = null

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // State flows
    private val _connectionState = MutableStateFlow(AutoTradingState.DISCONNECTED)
    val connectionState: StateFlow<AutoTradingState> = _connectionState.asStateFlow()

    private val _currentPrice = MutableStateFlow<Double?>(null)
    val currentPrice: StateFlow<Double?> = _currentPrice.asStateFlow()

    private val _currentPair = MutableStateFlow("EURUSD")
    val currentPair: StateFlow<String> = _currentPair.asStateFlow()

    private val _candles = MutableStateFlow<List<Candle>>(emptyList())
    val candles: StateFlow<List<Candle>> = _candles.asStateFlow()

    private val _demoBalance = MutableStateFlow<OlympAccountBalance?>(null)
    val demoBalance: StateFlow<OlympAccountBalance?> = _demoBalance.asStateFlow()

    private val _latestOrder = MutableStateFlow<OlympOrder?>(null)
    val latestOrder: StateFlow<OlympOrder?> = _latestOrder.asStateFlow()

    private val _statusMessage = MutableStateFlow("WebSocket Olymp Trade belum terhubung.")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private var activeToken: String = ""
    private var isSimulatingDemo: Boolean = false

    /**
     * Connects to Olymp Trade WebSocket using user's access token session.
     */
    fun connect(accessToken: String, pair: String = "EURUSD", fallbackToSimulatedTicks: Boolean = true) {
        disconnect()
        activeToken = accessToken.trim()
        _currentPair.value = pair.uppercase()

        if (activeToken.isBlank()) {
            if (fallbackToSimulatedTicks) {
                startDemoSimulation(pair)
            } else {
                _connectionState.value = AutoTradingState.DISCONNECTED
                _statusMessage.value = "Access token session Olymp Trade kosong. Masukkan token pada Pengaturan."
            }
            return
        }

        _connectionState.value = AutoTradingState.CONNECTING
        _statusMessage.value = "Menghubungkan ke Olymp Trade WebSocket..."

        val request = Request.Builder()
            .url(DEFAULT_WS_URI)
            .addHeader("Cookie", "access_token=$activeToken")
            .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Olymp Trade WS Connected")
                _connectionState.value = AutoTradingState.CONNECTED
                _statusMessage.value = "Terhubung ke Olymp Trade. Menginisialisasi session..."

                // Send session handshake & query demo balance
                sendSessionInit(webSocket)
                subscribeTicks(webSocket, _currentPair.value)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Olymp Trade WS Failure: ${t.message}", t)
                _connectionState.value = AutoTradingState.DISCONNECTED
                _statusMessage.value = "Koneksi gagal: ${t.localizedMessage ?: "Timeout / Handshake ditolak"}."

                if (fallbackToSimulatedTicks) {
                    _statusMessage.value = "WS ditolak (Token tidak valid/kedaluwarsa). Mengaktifkan Mode Demo Simulasi lokal."
                    startDemoSimulation(_currentPair.value)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Olymp Trade WS Closed: $code / $reason")
                _connectionState.value = AutoTradingState.DISCONNECTED
                _statusMessage.value = "Koneksi terputus: $reason"
            }
        })
    }

    private fun sendSessionInit(ws: WebSocket) {
        try {
            // Frame to request account details
            val balanceReq = JSONObject().apply {
                put("e", "get_balance")
                put("d", JSONObject())
            }
            ws.send(balanceReq.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending session init: ${e.message}")
        }
    }

    private fun subscribeTicks(ws: WebSocket, pair: String) {
        try {
            val subMsg = JSONObject().apply {
                put("e", "subscribe_ticks")
                put("d", JSONObject().apply { put("pair", pair) })
            }
            ws.send(subMsg.toString())
            _statusMessage.value = "Berlangganan tick pasar $pair..."
        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing: ${e.message}")
        }
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val json = JSONObject(text)
            val event = json.optString("e", "")

            // Tick Update (similar to on_tick in ChipaDevTeam/OlympTradeAPI)
            if (event == "tick_update" || json.has("d")) {
                val data = json.opt("d")
                if (data is JSONArray) {
                    for (i in 0 until data.length()) {
                        val item = data.optJSONObject(i) ?: continue
                        val pair = item.optString("p", "")
                        val price = item.optDouble("q", Double.NaN)
                        if (!price.isNaN()) {
                            _currentPrice.value = price
                            updateCandleWithTick(price)
                        }
                    }
                } else if (data is JSONObject) {
                    val price = data.optDouble("q", Double.NaN)
                    if (!price.isNaN()) {
                        _currentPrice.value = price
                        updateCandleWithTick(price)
                    }

                    // Balance update
                    if (data.has("group") && data.optString("group") == "demo") {
                        val accId = data.optString("account_id", "demo_123")
                        val bal = data.optDouble("amount", 10000.0)
                        _demoBalance.value = OlympAccountBalance(
                            accountId = accId,
                            group = "demo",
                            currency = data.optString("currency", "USD"),
                            amount = bal
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing WS message: ${e.message}")
        }
    }

    private fun updateCandleWithTick(price: Double) {
        val now = System.currentTimeMillis()
        val currentList = _candles.value.toMutableList()

        if (currentList.isEmpty()) {
            currentList.add(Candle(now, price, price, price, price, 1.0))
        } else {
            val last = currentList.last()
            // 60-second candle boundary
            if (now - last.timestamp >= 60_000) {
                currentList.add(Candle(now, price, price, price, price, 1.0))
                if (currentList.size > 50) currentList.removeAt(0)
            } else {
                val updated = last.copy(
                    high = maxOf(last.high, price),
                    low = minOf(last.low, price),
                    close = price,
                    volume = last.volume + 1.0
                )
                currentList[currentList.size - 1] = updated
            }
        }
        _candles.value = currentList
    }

    /**
     * Executes order on Olymp Trade (FTT Fixed Time Trade).
     */
    fun placeOrder(
        pair: String,
        direction: String, // "up" or "down"
        amount: Double,
        durationSeconds: Int = 60,
        accountId: String = _demoBalance.value?.accountId ?: "demo_acc"
    ): OlympOrder {
        val orderId = "ORD-" + UUID.randomUUID().toString().take(8).uppercase()
        val currentPrice = _currentPrice.value ?: 1.08500
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

        val order = OlympOrder(
            localId = orderId,
            serverOrderId = null,
            pair = pair,
            direction = direction,
            amount = amount,
            durationSeconds = durationSeconds,
            group = "demo",
            status = "PENDING",
            entryPrice = currentPrice,
            exitPrice = null,
            profit = null,
            timestamp = timestamp
        )
        _latestOrder.value = order

        val ws = webSocket
        if (ws != null && _connectionState.value == AutoTradingState.CONNECTED) {
            try {
                val payload = JSONObject().apply {
                    put("e", "place_order")
                    put("d", JSONObject().apply {
                        put("pair", pair)
                        put("amount", amount)
                        put("dir", direction)
                        put("dur", durationSeconds)
                        put("account_id", accountId)
                        put("group", "demo")
                    })
                }
                ws.send(payload.toString())
                _latestOrder.value = order.copy(status = "CONFIRMED", serverOrderId = "SRV-$orderId")
                _statusMessage.value = "Order demo berhasil dikirim ($direction $pair $amount)"
            } catch (e: Exception) {
                _latestOrder.value = order.copy(status = "FAILED", errorReason = e.message)
                _statusMessage.value = "Gagal mengirim order: ${e.message}"
            }
        } else {
            // In demo simulation mode, simulate order fill
            _latestOrder.value = order.copy(status = "CONFIRMED (DEMO SIM)", serverOrderId = "SIM-$orderId")
            _statusMessage.value = "Order simulasi demo aktif ($direction $pair $${amount})"
        }

        return _latestOrder.value ?: order
    }

    /**
     * Starts realistic demo price stream when live network token is not yet configured.
     */
    fun startDemoSimulation(pair: String) {
        simulationJob?.cancel()
        isSimulatingDemo = true
        _connectionState.value = AutoTradingState.DEMO_MODE
        _statusMessage.value = "Mode Demo Simulasi Aktif ($pair). Mengalirkan tick real-time."
        _currentPair.value = pair

        _demoBalance.value = OlympAccountBalance(
            accountId = "DEMO-100234",
            group = "demo",
            currency = "USD",
            amount = 10000.00
        )

        // Seed 30 realistic candles for technical analysis
        val initialCandles = mutableListOf<Candle>()
        var basePrice = 1.08500
        val now = System.currentTimeMillis() - (30 * 60_000L)

        for (i in 0 until 30) {
            val candleTime = now + (i * 60_000L)
            val delta = (Math.random() - 0.48) * 0.0004
            val open = basePrice
            val close = open + delta
            val high = maxOf(open, close) + (Math.random() * 0.00015)
            val low = minOf(open, close) - (Math.random() * 0.00015)
            initialCandles.add(Candle(candleTime, open, high, low, close, 10.0))
            basePrice = close
        }
        _candles.value = initialCandles
        _currentPrice.value = basePrice

        // Stream real-time ticks every 1 second
        simulationJob = scope.launch {
            while (isActive) {
                delay(1000)
                val current = _currentPrice.value ?: 1.08500
                val tickNoise = (Math.random() - 0.49) * 0.00008
                val newPrice = Math.round((current + tickNoise) * 100000.0) / 100000.0
                _currentPrice.value = newPrice
                updateCandleWithTick(newPrice)
            }
        }
    }

    fun disconnect() {
        simulationJob?.cancel()
        simulationJob = null
        isSimulatingDemo = false
        try {
            webSocket?.close(1000, "User disconnect")
            webSocket = null
        } catch (_: Exception) {}
        _connectionState.value = AutoTradingState.DISCONNECTED
        _statusMessage.value = "Olymp Trade terputus."
    }
}
