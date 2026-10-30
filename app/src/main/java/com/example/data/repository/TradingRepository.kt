package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.data.api.AiTraderEngine
import com.example.data.db.AppDatabase
import com.example.data.db.SignalEntity
import com.example.data.pref.AppSettings
import com.example.model.ConfirmationType
import com.example.model.SignalType
import com.example.model.TradingSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TradingRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val appSettings: AppSettings,
    private val aiEngine: AiTraderEngine
) {
    companion object {
        private const val TAG = "TradingRepository"

        @Volatile
        private var INSTANCE: TradingRepository? = null

        fun getInstance(context: Context): TradingRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                val settings = AppSettings(context)
                val engine = AiTraderEngine(settings)
                val repo = TradingRepository(context.applicationContext, db, settings, engine)
                INSTANCE = repo
                repo
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Current live signal
    private val _currentSignal = MutableStateFlow<TradingSignal?>(null)
    val currentSignal: StateFlow<TradingSignal?> = _currentSignal.asStateFlow()

    // Screen capture service status
    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    // AI Analysis in-flight status
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    // Status message for UI
    private val _statusMessage = MutableStateFlow("Sistem siap. Silakan mulai analisis.")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // Floating window visibility state
    private val _isFloatingOverlayVisible = MutableStateFlow(false)
    val isFloatingOverlayVisible: StateFlow<Boolean> = _isFloatingOverlayVisible.asStateFlow()

    // Signals stream from Room
    val signalHistory: Flow<List<TradingSignal>> = database.signalDao().getAllSignals()
        .map { list -> list.map { it.toDomain() } }

    fun setCapturing(active: Boolean) {
        _isCapturing.value = active
        if (!active) {
            _statusMessage.value = "Screen capture dinonaktifkan."
        } else {
            _statusMessage.value = "Screen capture aktif. Menunggu frame chart MT5..."
        }
    }

    fun setFloatingOverlayVisible(visible: Boolean) {
        _isFloatingOverlayVisible.value = visible
    }

    /**
     * Called by ScreenCaptureService whenever a new chart frame is captured.
     */
    fun processCapturedFrame(bitmap: Bitmap) {
        if (_isAnalyzing.value) {
            // Drop frame if AI is still processing previous frame to prevent backlog
            return
        }

        scope.launch {
            _isAnalyzing.value = true
            _statusMessage.value = "Menganalisis chart dengan Gemini AI..."

            val result = aiEngine.analyzeChart(bitmap)

            result.onSuccess { signal ->
                _currentSignal.value = signal
                _statusMessage.value = "Sinyal diperbarui: ${signal.displaySignalText()} [${signal.pair} ${signal.timeframe}]"

                // Save to Room DB if not just empty WAIT or if it has meaningful reason
                if (signal.signal != SignalType.WAIT || signal.pair != "UNKNOWN") {
                    database.signalDao().insertSignal(SignalEntity.fromDomain(signal))
                    database.signalDao().trimOldSignals()
                }

                // Alert user on confirmed BUY or SELL
                if (signal.confirmation == ConfirmationType.CONFIRMED &&
                    (signal.signal == SignalType.BUY || signal.signal == SignalType.SELL)
                ) {
                    triggerAlert()
                }
            }.onFailure { error ->
                val errorMsg = error.localizedMessage ?: "Gagal menganalisis gambar"
                Log.e(TAG, "Analysis error: $errorMsg", error)
                _statusMessage.value = "Error Analisis: $errorMsg"

                // Update signal to WAIT on unrecoverable error
                _currentSignal.value = TradingSignal(
                    signal = SignalType.WAIT,
                    pair = "UNKNOWN",
                    timeframe = "M5",
                    entry = null,
                    stopLoss = null,
                    takeProfit = null,
                    riskReward = "1:2",
                    confidence = null,
                    trend = "UNKNOWN",
                    confirmation = ConfirmationType.UNCONFIRMED,
                    reason = "Kendala koneksi/analisis: $errorMsg",
                    timestamp = ""
                )
            }

            _isAnalyzing.value = false
        }
    }

    fun clearHistory() {
        scope.launch {
            database.signalDao().clearAllSignals()
        }
    }

    private fun triggerAlert() {
        val settings = appSettings.getSettings()

        if (settings.vibrationEnabled) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    val vibrator = vibratorManager?.defaultVibrator
                    vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    vibrator?.vibrate(300)
                }
            }
        }

        if (settings.soundEnabled) {
            runCatching {
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, notification)
                ringtone?.play()
            }
        }
    }
}
