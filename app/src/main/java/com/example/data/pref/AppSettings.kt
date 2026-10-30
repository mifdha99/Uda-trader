package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("uda_trader_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GEMINI_KEY = "key_gemini_key"
        private const val KEY_MODEL = "key_model"
        private const val KEY_INTERVAL = "key_interval"
        private const val KEY_VIBRATION = "key_vibration"
        private const val KEY_SOUND = "key_sound"

        const val DEFAULT_MODEL = "gemini-1.5-flash"
    }

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<SettingsState> = _settingsFlow.asStateFlow()

    data class SettingsState(
        val geminiApiKey: String,
        val selectedModel: String,
        val intervalSeconds: Int,
        val vibrationEnabled: Boolean,
        val soundEnabled: Boolean
    )

    private fun loadSettings(): SettingsState {
        // BuildConfig fallback or empty from Secrets Gradle Plugin
        val envKey = runCatching {
            BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String
        }.getOrNull()?.takeUnless { it == "your_api_key_here" } ?: ""

        val savedKey = prefs.getString(KEY_GEMINI_KEY, "") ?: ""
        val effectiveKey = if (savedKey.isNotBlank()) savedKey else envKey

        return SettingsState(
            geminiApiKey = effectiveKey,
            selectedModel = prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL,
            intervalSeconds = prefs.getInt(KEY_INTERVAL, 3).coerceIn(1, 10),
            vibrationEnabled = prefs.getBoolean(KEY_VIBRATION, true),
            soundEnabled = prefs.getBoolean(KEY_SOUND, true)
        )
    }

    fun updateGeminiApiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_KEY, key.trim()).apply()
        _settingsFlow.value = loadSettings()
    }

    fun updateModel(model: String) {
        prefs.edit().putString(KEY_MODEL, model).apply()
        _settingsFlow.value = loadSettings()
    }

    fun updateInterval(seconds: Int) {
        prefs.edit().putInt(KEY_INTERVAL, seconds.coerceIn(1, 10)).apply()
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

    fun getSettings(): SettingsState = _settingsFlow.value

    fun getMaskedApiKey(): String {
        val key = _settingsFlow.value.geminiApiKey
        return if (key.length > 8) {
            "••••••••" + key.takeLast(4)
        } else if (key.isNotBlank()) {
            "••••••••"
        } else {
            "Belum dikonfigurasi"
        }
    }
}
