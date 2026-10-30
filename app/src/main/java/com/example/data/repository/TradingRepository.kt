package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.SignalEntity
import com.example.data.engine.UniversalSignalEngine
import com.example.data.pref.AppSettings
import com.example.model.ConfirmationType
import com.example.model.Mt5Timeframe
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
import kotlinx.coroutines.withContext

class TradingRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val appSettings: AppSettings,
    private val signalEngine: UniversalSignalEngine = UniversalSignalEngine()
) {
    companion object {
        private const val TAG = "TradingRepository"

        @Volatile
        private var INSTANCE: TradingRepository? = null

        fun getInstance(context: Context): TradingRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                val settings = AppSettings(context)
                val repo = TradingRepository(context.applicationContext, db, settings)
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

    // Engine Analysis in-flight status
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    // Status message for UI
    private val _statusMessage = MutableStateFlow("Universal Signal Engine siap. Aktifkan monitor MT5.")
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
            _statusMessage.value = "Pemantauan MT5 dihentikan."
        } else {
            _statusMessage.value = "Pemantauan aktif: Membaca chart MT5 secara real-time..."
        }
    }

    fun setFloatingOverlayVisible(visible: Boolean) {
        _isFloatingOverlayVisible.value = visible
    }

    /**
     * Analyzes captured MT5 frame on-device using the Universal Signal Engine.
     */
    fun processCapturedFrame(bitmap: Bitmap) {
        if (_isAnalyzing.value) {
            // Drop frame if engine is still processing previous frame to prevent backlog
            return
        }

        scope.launch {
            _isAnalyzing.value = true
            try {
                val settings = appSettings.getSettings()
                val activeTimeframe = settings.activeTimeframe

                val signal = withContext(Dispatchers.Default) {
                    signalEngine.processChartFrame(bitmap, activeTimeframe)
                }

                _currentSignal.value = signal

                if (signal.signal != SignalType.WAIT) {
                    _statusMessage.value = "${signal.displaySignalText()} [${signal.pair} ${signal.timeframe}] - Conf: ${signal.formattedConfidence()}"
                    // Save valid BUY/SELL signals to Room DB
                    database.signalDao().insertSignal(SignalEntity.fromDomain(signal))
                    database.signalDao().trimOldSignals()

                    if (signal.confirmation == ConfirmationType.CONFIRMED) {
                        triggerAlert()
                    }
                } else {
                    _statusMessage.value = signal.reason
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in Universal Signal Engine: ${e.message}", e)
                _statusMessage.value = "DATA CHART TIDAK VALID: ${e.localizedMessage ?: "Gagal memproses frame"}"
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun updateTimeframe(timeframe: Mt5Timeframe) {
        appSettings.updateTimeframe(timeframe)
        _statusMessage.value = "Timeframe acuan diubah ke ${timeframe.code} (${timeframe.displayName})"
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
