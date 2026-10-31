package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.AiTraderEngine
import com.example.data.pref.AppSettings
import com.example.data.repository.TradingRepository
import com.example.model.AutoTradingConfig
import com.example.model.AutoTradingState
import com.example.model.Candle
import com.example.model.DataSourceMode
import com.example.model.OlympAccountBalance
import com.example.model.OlympOrder
import com.example.model.RiskSettings
import com.example.model.SignalType
import com.example.model.TechnicalAnalysisResult
import com.example.model.TradingSignal
import com.example.service.FloatingWindowService
import com.example.service.OlympTradeBackgroundService
import com.example.service.ScreenCaptureService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TraderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TradingRepository.getInstance(application)
    private val appSettings = repository.appSettings
    private val aiEngine = AiTraderEngine(appSettings)

    // Data Source Mode
    val activeDataSource: StateFlow<DataSourceMode> = repository.activeDataSource

    // Signals and Live Analysis
    val currentSignal: StateFlow<TradingSignal?> = repository.currentSignal
    val isCapturing: StateFlow<Boolean> = repository.isCapturing
    val isAnalyzing: StateFlow<Boolean> = repository.isAnalyzing
    val statusMessage: StateFlow<String> = repository.statusMessage
    val isFloatingOverlayVisible: StateFlow<Boolean> = repository.isFloatingOverlayVisible

    // Indicator Engine State (Engineer View)
    val technicalResult: StateFlow<TechnicalAnalysisResult?> = repository.technicalResult

    // Olymp Trade Telemetry
    val olympPrice: StateFlow<Double?> = repository.olympPrice
    val olympPair: StateFlow<String> = repository.olympPair
    val olympBalance: StateFlow<OlympAccountBalance?> = repository.olympBalance
    val olympCandles: StateFlow<List<Candle>> = repository.olympCandles
    val autoTradingState: StateFlow<AutoTradingState> = repository.autoTradingState
    val autoTradingOrders: StateFlow<List<OlympOrder>> = repository.autoTradingOrders
    val autoTradingConfig: StateFlow<AutoTradingConfig> = repository.autoTradingManager.config

    // History and Settings
    val signalHistory: StateFlow<List<TradingSignal>> = repository.signalHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings.SettingsState> = appSettings.settingsFlow

    // Risk calculator state
    private val _riskSettings = MutableStateFlow(RiskSettings())
    val riskSettings: StateFlow<RiskSettings> = _riskSettings.asStateFlow()

    // Test connection feedback
    private val _connectionTestStatus = MutableStateFlow<String?>(null)
    val connectionTestStatus: StateFlow<String?> = _connectionTestStatus.asStateFlow()

    fun setDataSourceMode(mode: DataSourceMode) {
        repository.setDataSourceMode(mode)
    }

    fun connectOlympTrade(token: String, pair: String) {
        repository.connectOlympTrade(token, pair)
    }

    fun disconnectOlympTrade() {
        repository.disconnectOlympTrade()
    }

    fun startAutoTrading(context: Context) {
        val intent = Intent(context, OlympTradeBackgroundService::class.java).apply {
            action = OlympTradeBackgroundService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        repository.startAutoTrading()
    }

    fun stopAutoTrading(context: Context) {
        val intent = Intent(context, OlympTradeBackgroundService::class.java).apply {
            action = OlympTradeBackgroundService.ACTION_STOP
        }
        context.startService(intent)
        repository.stopAutoTrading()
    }

    fun emergencyStop(context: Context) {
        val intent = Intent(context, OlympTradeBackgroundService::class.java).apply {
            action = OlympTradeBackgroundService.ACTION_STOP
        }
        context.startService(intent)
        repository.emergencyStopAutoTrading()
        Toast.makeText(context, "⚠️ EMERGENCY STOP DIJALANKAN", Toast.LENGTH_LONG).show()
    }

    fun updateAutoTradingConfig(config: AutoTradingConfig) {
        repository.updateAutoTradingConfig(config)
    }

    fun copySignalToClipboard(context: Context) {
        val signal = currentSignal.value
        val pair = if (activeDataSource.value == DataSourceMode.OLYMP_TRADE_WS) olympPair.value else signal?.pair ?: "EURUSD"
        val dir = when (signal?.signal) {
            SignalType.BUY -> "BUY / CALL (UP)"
            SignalType.SELL -> "SELL / PUT (DOWN)"
            else -> "WAIT"
        }
        val text = "UDA AI TRADER\nPair: $pair\nSinyal: $dir\nEntry: ${signal?.entry ?: "Market"}\nKeyakinan: ${String.format("%.0f", (signal?.confidence ?: 0.0) * 100)}%\nWaktu: ${signal?.timestamp ?: "Realtime"}"

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("UDA AI Trader Signal", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Sinyal $dir berhasil disalin!", Toast.LENGTH_SHORT).show()
    }

    fun updateRiskBalance(balance: Double) {
        _riskSettings.value = _riskSettings.value.copy(accountBalance = balance)
    }

    fun updateRiskPercentage(pct: Double) {
        _riskSettings.value = _riskSettings.value.copy(riskPercentage = pct)
    }

    fun updateRiskStopLossPips(pips: Double) {
        _riskSettings.value = _riskSettings.value.copy(stopLossPips = pips)
    }

    fun updateRiskReward(rr: Double) {
        _riskSettings.value = _riskSettings.value.copy(riskRewardRatio = rr)
    }

    fun updateRiskInstrument(instrument: String) {
        _riskSettings.value = _riskSettings.value.copy(instrumentType = instrument)
    }

    fun startScreenCapture(resultCode: Int, data: Intent) {
        val context = getApplication<Application>()
        val serviceIntent = Intent(context, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_START
            putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    fun stopScreenCapture() {
        val context = getApplication<Application>()
        val serviceIntent = Intent(context, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_STOP
        }
        context.startService(serviceIntent)
    }

    fun canDrawOverlays(): Boolean {
        val context = getApplication<Application>()
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
    }

    fun toggleFloatingOverlay(context: Context, onPermissionRequired: () -> Unit) {
        if (!canDrawOverlays()) {
            onPermissionRequired()
            return
        }

        val serviceIntent = Intent(context, FloatingWindowService::class.java)
        if (isFloatingOverlayVisible.value) {
            serviceIntent.action = FloatingWindowService.ACTION_HIDE
        } else {
            serviceIntent.action = FloatingWindowService.ACTION_SHOW
        }
        context.startService(serviceIntent)
    }

    fun testConnection() {
        viewModelScope.launch {
            _connectionTestStatus.value = "Menghubungi endpoint..."
            val result = aiEngine.testConnection()
            result.onSuccess { msg ->
                _connectionTestStatus.value = "SUKSES: $msg"
            }.onFailure { err ->
                _connectionTestStatus.value = "GAGAL: ${err.localizedMessage}"
            }
        }
    }

    fun clearConnectionTestStatus() {
        _connectionTestStatus.value = null
    }

    fun updateOlympToken(token: String) = appSettings.updateOlympToken(token)
    fun updateOlympPair(pair: String) = appSettings.updateOlympPair(pair)
    fun clearOlympCredentials() = appSettings.clearOlympCredentials()

    fun updateBackendUrl(url: String) = appSettings.updateBackendUrl(url)
    fun updateGeminiApiKey(key: String) = appSettings.updateGeminiApiKey(key)
    fun updateModel(model: String) = appSettings.updateModel(model)
    fun updateInterval(seconds: Int) = appSettings.updateInterval(seconds)
    fun updateUseDirect(direct: Boolean) = appSettings.updateUseDirect(direct)
    fun updateVibration(enabled: Boolean) = appSettings.updateVibration(enabled)
    fun updateSound(enabled: Boolean) = appSettings.updateSound(enabled)

    fun clearHistory() = repository.clearHistory()

    fun analyzeSampleChart() {
        viewModelScope.launch {
            val sampleBitmap = createSyntheticMt5Chart()
            repository.processCapturedFrame(sampleBitmap)
        }
    }

    private fun createSyntheticMt5Chart(): Bitmap {
        val width = 800
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply { color = Color.rgb(16, 20, 29) }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val gridPaint = Paint().apply {
            color = Color.rgb(28, 35, 51)
            strokeWidth = 1f
        }
        for (y in 50 until height step 50) {
            canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)
        }
        for (x in 50 until width step 60) {
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
        }

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            isAntiAlias = true
        }
        canvas.drawText("EURUSD, M5 - MetaTrader 5 Institutional Chart", 30f, 45f, textPaint)

        val pricePaint = Paint().apply {
            color = Color.rgb(0, 200, 255)
            textSize = 22f
            isAntiAlias = true
        }
        canvas.drawText("Bid: 1.08520  Ask: 1.08530  Spread: 10", 30f, 80f, pricePaint)

        val greenCandle = Paint().apply { color = Color.rgb(16, 185, 129); strokeWidth = 8f }
        val redCandle = Paint().apply { color = Color.rgb(244, 63, 94); strokeWidth = 8f }
        val wickPaint = Paint().apply { color = Color.WHITE; strokeWidth = 2f }

        val candleData = listOf(
            Triple(280f, 320f, 250f to 340f),
            Triple(290f, 330f, 270f to 350f),
            Triple(320f, 280f, 260f to 330f),
            Triple(280f, 240f, 230f to 290f),
            Triple(240f, 260f, 230f to 270f),
            Triple(250f, 210f, 200f to 260f),
            Triple(210f, 180f, 170f to 220f)
        )

        var xOffset = 80f
        for (candle in candleData) {
            val (open, close, wicks) = candle
            val isBullish = close < open
            val paint = if (isBullish) greenCandle else redCandle

            canvas.drawLine(xOffset, wicks.first, xOffset, wicks.second, wickPaint)
            val top = minOf(open, close)
            val bottom = maxOf(open, close)
            canvas.drawRect(xOffset - 12f, top, xOffset + 12f, bottom, paint)
            xOffset += 85f
        }

        val srPaint = Paint().apply {
            color = Color.YELLOW
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(50f, 280f, 750f, 280f, srPaint)
        canvas.drawText("Support / Order Block Zone (1.08450)", 400f, 270f, Paint().apply {
            color = Color.YELLOW
            textSize = 16f
        })

        return bitmap
    }
}
