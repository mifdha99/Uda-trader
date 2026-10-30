package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences
import com.example.model.Mt5Timeframe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("uda_trader_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TIMEFRAME = "key_active_timeframe"
        private const val KEY_INTERVAL = "key_interval"
        private const val KEY_VIBRATION = "key_vibration"
        private const val KEY_SOUND = "key_sound"
        private const val KEY_OVERLAY_X = "key_overlay_x"
        private const val KEY_OVERLAY_Y = "key_overlay_y"
        private const val KEY_RISK_REWARD = "key_risk_reward"

        val DEFAULT_TIMEFRAME = Mt5Timeframe.M5
    }

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<SettingsState> = _settingsFlow.asStateFlow()

    data class SettingsState(
        val activeTimeframe: Mt5Timeframe,
        val intervalSeconds: Int,
        val vibrationEnabled: Boolean,
        val soundEnabled: Boolean,
        val overlayX: Int,
        val overlayY: Int,
        val preferredRiskReward: String
    )

    private fun loadSettings(): SettingsState {
        val tfCode = prefs.getString(KEY_TIMEFRAME, DEFAULT_TIMEFRAME.code)
        val timeframe = Mt5Timeframe.fromCode(tfCode)

        return SettingsState(
            activeTimeframe = timeframe,
            intervalSeconds = prefs.getInt(KEY_INTERVAL, 1).coerceIn(1, 5),
            vibrationEnabled = prefs.getBoolean(KEY_VIBRATION, true),
            soundEnabled = prefs.getBoolean(KEY_SOUND, true),
            overlayX = prefs.getInt(KEY_OVERLAY_X, 40),
            overlayY = prefs.getInt(KEY_OVERLAY_Y, 200),
            preferredRiskReward = prefs.getString(KEY_RISK_REWARD, "1:2") ?: "1:2"
        )
    }

    fun updateTimeframe(timeframe: Mt5Timeframe) {
        prefs.edit().putString(KEY_TIMEFRAME, timeframe.code).apply()
        _settingsFlow.value = loadSettings()
    }

    fun updateInterval(seconds: Int) {
        prefs.edit().putInt(KEY_INTERVAL, seconds.coerceIn(1, 5)).apply()
        _settingsFlow.value = loadSettings()
    }

    fun updateVibration(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply()
        _settingsFlow.value = loadSettings()
    }

    fun updateSound(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
        _settingsFlow.value = loadSettings()
    }

    fun saveOverlayPosition(x: Int, y: Int) {
        prefs.edit().putInt(KEY_OVERLAY_X, x).putInt(KEY_OVERLAY_Y, y).apply()
        _settingsFlow.value = loadSettings()
    }

    fun updatePreferredRiskReward(rr: String) {
        prefs.edit().putString(KEY_RISK_REWARD, rr).apply()
        _settingsFlow.value = loadSettings()
    }

    fun getSettings(): SettingsState = _settingsFlow.value
}
