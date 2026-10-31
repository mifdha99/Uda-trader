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
import com.example.model.DataVerificationLevel
import com.example.model.EngineState
import com.example.model.Mt5Timeframe
import com.example.model.SignalType
import com.example.model.TradingSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
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

    // 10-State Engine State Tracker
    private val _engineState = MutableStateFlow(EngineState.WAITING_CHART)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    // Screen capture service status
    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    // Screen capture pause status
    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    // Engine Analysis in-flight status
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    // Status message for UI
    private val _statusMessage = MutableStateFlow("Universal Signal Engine siap. Aktifkan monitor chart.")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // Floating window visibility state
    private val _isFloatingOverlayVisible = MutableStateFlow(false)
    val isFloatingOverlayVisible: StateFlow<Boolean> = _isFloatingOverlayVisible.asStateFlow()

    // Timestamp tracking for expiration (>15 seconds without fresh frames)
    private var lastFrameTimeMs: Long = 0L

    // Signals stream from Room
    val signalHistory: Flow<List<TradingSignal>> = database.signalDao().getAllSignals()
        .map { list -> list.map { it.toDomain() } }

    init {
        // Expiration monitor loop: checks every 3 seconds
        scope.launch {
            while (isActive) {
                delay(3000L)
                if (_isCapturing.value && !_isPaused.value && lastFrameTimeMs > 0L) {
                    val elapsed = System.currentTimeMillis() - lastFrameTimeMs
                    if (elapsed > UniversalSignalEngine.DATA_EXPIRATION_THRESHOLD_MS) {
                        if (_engineState.value != EngineState.DATA_EXPIRED) {
                            _engineState.value = EngineState.DATA_EXPIRED
                            _statusMessage.value = "DATA KEDALUWARSA: Tidak ada frame chart baru selama ${elapsed / 1000} detik."
                            _currentSignal.value = _currentSignal.value?.copy(
                                state = EngineState.DATA_EXPIRED,
                                reason = "DATA KEDALUWARSA: Tidak ada frame visual baru (>15s). Periksa apakah aplikasi chart tertutup atau latar belakang terhenti."
                            )
                        }
                    }
                }
            }
        }
    }

    fun setCapturing(active: Boolean) {
        _isCapturing.value = active
        if (!active) {
            _engineState.value = EngineState.WAITING_CHART
            _statusMessage.value = "Pemantauan dihentikan."
        } else {
            _engineState.value = EngineState.READING_CHART
            _statusMessage.value = "Pemantauan aktif: Membaca chart visual..."
        }
    }

    fun setPaused(paused: Boolean) {
        _isPaused.value = paused
        if (paused) {
            _statusMessage.value = "Pemantauan Dijeda (JEDA). Tekan Lanjut untuk membaca chart."
        } else {
            _statusMessage.value = "Pemantauan dilanjutkan (LANJUT)."
        }
    }

    fun notifyStreamDisconnected() {
        _isCapturing.value = false
        _isPaused.value = false
        _engineState.value = EngineState.STREAM_DISCONNECTED
        _statusMessage.value = "LIVE SCREEN TERPUTUS: Koneksi perekaman layar berhenti."
        _currentSignal.value = _currentSignal.value?.copy(
            state = EngineState.STREAM_DISCONNECTED,
            reason = "LIVE SCREEN TERPUTUS: Izin perekaman layar dihentikan oleh pengguna atau sistem."
        )
    }

    fun setFloatingOverlayVisible(visible: Boolean) {
        _isFloatingOverlayVisible.value = visible
    }

    /**
     * Processes live captured frame using Universal Signal Engine.
     */
    fun processCapturedFrame(bitmap: Bitmap) {
        if (_isAnalyzing.value || _isPaused.value) {
            // Drop frame if engine is busy or paused
            return
        }

        lastFrameTimeMs = System.currentTimeMillis()

        scope.launch {
            _isAnalyzing.value = true
            _engineState.value = EngineState.VALIDATING_DATA
            try {
                val settings = appSettings.getSettings()
                val activeTimeframe = settings.activeTimeframe

                _engineState.value = EngineState.ANALYZING

                val signal = withContext(Dispatchers.Default) {
                    signalEngine.processChartFrame(bitmap, activeTimeframe)
                }

                _currentSignal.value = signal
                _engineState.value = signal.state

                if (signal.state.isActionableSignal()) {
                    _statusMessage.value = "${signal.displaySignalText()} [${signal.formattedSymbol()} ${signal.timeframe}] - Conf: ${signal.formattedConfidence()}"
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
                _engineState.value = EngineState.INVALID_DATA
                _statusMessage.value = "DATA TIDAK VALID: ${e.localizedMessage ?: "Gagal memproses frame chart"}"
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
