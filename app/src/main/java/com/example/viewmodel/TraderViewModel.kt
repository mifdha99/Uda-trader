package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.pref.AppSettings
import com.example.data.repository.TradingRepository
import com.example.model.Mt5Timeframe
import com.example.model.RiskSettings
import com.example.model.TradingSignal
import com.example.service.FloatingWindowService
import com.example.service.ScreenCaptureService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TraderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TradingRepository.getInstance(application)
    private val appSettings = AppSettings(application)

    val currentSignal: StateFlow<TradingSignal?> = repository.currentSignal
    val isCapturing: StateFlow<Boolean> = repository.isCapturing
    val isAnalyzing: StateFlow<Boolean> = repository.isAnalyzing
    val statusMessage: StateFlow<String> = repository.statusMessage
    val isFloatingOverlayVisible: StateFlow<Boolean> = repository.isFloatingOverlayVisible

    val signalHistory: StateFlow<List<TradingSignal>> = repository.signalHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings.SettingsState> = appSettings.settingsFlow

    // Risk calculator state
    private val _riskSettings = MutableStateFlow(RiskSettings())
    val riskSettings: StateFlow<RiskSettings> = _riskSettings.asStateFlow()

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

    fun updateTimeframe(timeframe: Mt5Timeframe) {
        repository.updateTimeframe(timeframe)
    }

    fun updateInterval(seconds: Int) = appSettings.updateInterval(seconds)
    fun updatePreferredRiskReward(rr: String) = appSettings.updatePreferredRiskReward(rr)
    fun updateVibration(enabled: Boolean) = appSettings.updateVibration(enabled)
    fun updateSound(enabled: Boolean) = appSettings.updateSound(enabled)
    fun resetOverlayPosition() = appSettings.saveOverlayPosition(40, 200)

    fun clearHistory() = repository.clearHistory()

    /**
     * Generates a realistic synthetic MT5 candlestick chart bitmap to test the Universal Signal Engine locally.
     */
    fun analyzeSampleChart() {
        viewModelScope.launch {
            val sampleBitmap = createSyntheticMt5Chart()
            repository.processCapturedFrame(sampleBitmap)
        }
    }

    private fun createSyntheticMt5Chart(): Bitmap {
        val width = 900
        val height = 700
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Dark MT5 background
        val bgPaint = Paint().apply { color = Color.rgb(11, 15, 25) }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Subtle grid lines
        val gridPaint = Paint().apply {
            color = Color.rgb(28, 35, 51)
            strokeWidth = 1f
        }
        for (y in 80 until height - 60 step 60) {
            canvas.drawLine(30f, y.toFloat(), width - 110f, y.toFloat(), gridPaint)
        }
        for (x in 40 until width - 110 step 70) {
            canvas.drawLine(x.toFloat(), 80f, x.toFloat(), height - 60f, gridPaint)
        }

        // Header text "EURUSD, M5"
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 26f
            isAntiAlias = true
        }
        canvas.drawText("EURUSD, M5 - MetaTrader 5 Institutional Chart", 40f, 45f, textPaint)

        val pricePaint = Paint().apply {
            color = Color.rgb(0, 200, 255)
            textSize = 20f
            isAntiAlias = true
        }
        canvas.drawText("Bid: 1.08520  Ask: 1.08530  Spread: 10", 40f, 75f, pricePaint)

        // Price Axis on Right
        val axisPaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            textSize = 14f
            isAntiAlias = true
        }
        var axisPrice = 1.08750
        for (y in 100 until height - 60 step 70) {
            canvas.drawText(String.format(java.util.Locale.US, "%.5f", axisPrice), width - 95f, y.toFloat() + 5f, axisPaint)
            axisPrice -= 0.00050
        }

        // Draw 18 synthetic candlesticks establishing an institutional breakout
        val greenCandle = Paint().apply { color = Color.rgb(16, 185, 129); isAntiAlias = true }
        val redCandle = Paint().apply { color = Color.rgb(244, 63, 94); isAntiAlias = true }
        val wickPaint = Paint().apply { color = Color.rgb(200, 210, 220); strokeWidth = 2f }

        val candleData = listOf(
            Triple(420f, 440f, 410f to 455f),
            Triple(435f, 460f, 425f to 470f),
            Triple(460f, 440f, 430f to 475f),
            Triple(440f, 410f, 395f to 450f),
            Triple(410f, 425f, 400f to 435f),
            Triple(425f, 390f, 380f to 430f),
            Triple(390f, 370f, 360f to 400f),
            Triple(370f, 385f, 365f to 395f),
            Triple(385f, 360f, 350f to 390f),
            Triple(360f, 340f, 330f to 370f),
            Triple(340f, 310f, 300f to 350f),
            Triple(310f, 325f, 305f to 335f),
            Triple(325f, 290f, 280f to 330f),
            Triple(290f, 260f, 250f to 300f),
            Triple(260f, 240f, 230f to 270f),
            Triple(240f, 255f, 235f to 265f),
            Triple(255f, 210f, 200f to 260f), // Strong expansion breakout
            Triple(210f, 180f, 170f to 220f)  // Bullish continuation
        )

        var xOffset = 50f
        for (candle in candleData) {
            val (open, close, wicks) = candle
            val isBullish = close < open // in canvas coords, lower Y is higher price
            val paint = if (isBullish) greenCandle else redCandle

            // Draw wick
            canvas.drawLine(xOffset, wicks.first, xOffset, wicks.second, wickPaint)
            // Draw body
            val top = minOf(open, close)
            val bottom = maxOf(open, close)
            canvas.drawRect(xOffset - 10f, top, xOffset + 10f, bottom, paint)

            xOffset += 44f
        }

        // Support/Resistance line
        val srPaint = Paint().apply {
            color = Color.rgb(56, 189, 248)
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(40f, 340f, width - 110f, 340f, srPaint)

        return bitmap
    }
}
