package com.example.data.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.data.pref.AppSettings
import com.example.model.ConfirmationType
import com.example.model.SignalType
import com.example.model.TradingSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class AiTraderEngine(
    private val appSettings: AppSettings
) {
    companion object {
        private const val TAG = "AiTraderEngine"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var previousSignal: TradingSignal? = null

    /**
     * Analyzes captured bitmap using either the secure backend or direct Gemini API.
     */
    suspend fun analyzeChart(bitmap: Bitmap): Result<TradingSignal> = withContext(Dispatchers.IO) {
        runCatching {
            val settings = appSettings.getSettings()

            // 1. Optimize Bitmap to compact JPEG Base64
            val base64Image = bitmapToBase64(bitmap)

            val rawJson: String = if (settings.useDirectGemini && settings.geminiApiKey.isNotBlank()) {
                callDirectGeminiVision(base64Image, settings.geminiApiKey, settings.selectedModel)
            } else {
                callBackendServer(base64Image, settings.backendUrl, settings.geminiApiKey)
            }

            // 2. Parse & Validate
            val validatedResult = SignalValidator.parseAndValidate(rawJson)
            val currentSignal = validatedResult.getOrThrow()

            // 3. Compare with previous signal & apply State Machine logic
            val processedSignal = filterAndProcessSignal(currentSignal)
            previousSignal = processedSignal

            processedSignal
        }
    }

    /**
     * Converts bitmap to optimized JPEG Base64 string.
     * Scales down if larger than 1280px to save bandwidth and compute while maintaining crisp candle details.
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val maxDim = 1280
        val width = bitmap.width
        val height = bitmap.height
        val scaledBitmap = if (width > maxDim || height > maxDim) {
            val ratio = width.toFloat() / height.toFloat()
            val newWidth = if (width >= height) maxDim else (maxDim * ratio).toInt()
            val newHeight = if (height > width) maxDim else (maxDim / ratio).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Calls the self-hosted secure backend server.
     */
    private fun callBackendServer(base64Image: String, backendUrl: String, optionalApiKey: String): String {
        val requestJson = JSONObject().apply {
            put("image", base64Image)
            put("timestamp", getCurrentIsoTimestamp())
            put("system_prompt", GeminiPrompt.SYSTEM_INSTRUCTION)
            if (optionalApiKey.isNotBlank()) {
                put("apiKey", optionalApiKey)
            }
            previousSignal?.let {
                val prevObj = JSONObject().apply {
                    put("signal", it.signal.name)
                    put("pair", it.pair)
                    put("timeframe", it.timeframe)
                    put("confirmation", it.confirmation.name)
                }
                put("previous_signal", prevObj)
            }
        }

        val body = requestJson.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(backendUrl)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            Log.e(TAG, "Backend error: ${response.code} $responseBody")
            throw IllegalStateException("Backend HTTP ${response.code}: $responseBody")
        }

        // Backend can return raw json signal or wrapped in { "data": {...} }
        return try {
            val obj = JSONObject(responseBody)
            if (obj.has("signal")) {
                responseBody
            } else if (obj.has("data")) {
                obj.getJSONObject("data").toString()
            } else {
                responseBody
            }
        } catch (_: Exception) {
            responseBody
        }
    }

    /**
     * Direct call to Google Gemini Vision API using API key from settings or BuildConfig.
     */
    private fun callDirectGeminiVision(base64Image: String, apiKey: String, model: String): String {
        val effectiveModel = if (model.isBlank()) "gemini-2.5-flash" else model
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$effectiveModel:generateContent?key=$apiKey"

        val rootJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        // Instruction part
                        val textPart = JSONObject().apply {
                            put("text", "${GeminiPrompt.SYSTEM_INSTRUCTION}\n\nAnalyze this MetaTrader 5 chart image now. Output strictly valid JSON.")
                        }
                        put(textPart)

                        // Image part
                        val inlineData = JSONObject().apply {
                            put("mimeType", "image/jpeg")
                            put("data", base64Image)
                        }
                        val imagePart = JSONObject().apply {
                            put("inlineData", inlineData)
                        }
                        put(imagePart)
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            // Generation config requesting JSON
            val genConfig = JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
            }
            put("generationConfig", genConfig)
        }

        val body = rootJson.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            Log.e(TAG, "Gemini direct API error: ${response.code} $responseBody")
            throw IllegalStateException("Gemini API HTTP ${response.code}: $responseBody")
        }

        val jsonResponse = JSONObject(responseBody)
        val candidates = jsonResponse.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                return parts.getJSONObject(0).optString("text", "{}")
            }
        }

        throw IllegalStateException("Empty or unparsable response from Gemini API")
    }

    /**
     * Differentiates pending vs confirmed, checks direction reversal, and suppresses spam duplicates.
     */
    private fun filterAndProcessSignal(newSignal: TradingSignal): TradingSignal {
        val prev = previousSignal ?: return newSignal

        // If signal flips direction (e.g. from BUY to SELL or vice-versa), log clear transition
        if (prev.signal != newSignal.signal && newSignal.signal != SignalType.WAIT) {
            return newSignal.copy(
                reason = "Perubahan arah terdeteksi (${prev.signal} -> ${newSignal.signal}). ${newSignal.reason}"
            )
        }

        // If same signal and same entry, maintain stability unless confirmation changed
        if (prev.signal == newSignal.signal &&
            prev.pair.equals(newSignal.pair, ignoreCase = true) &&
            prev.confirmation == newSignal.confirmation &&
            newSignal.signal != SignalType.WAIT
        ) {
            return newSignal.copy(
                reason = "Sinyal konsisten: ${newSignal.reason}"
            )
        }

        return newSignal
    }

    private fun getCurrentIsoTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    /**
     * Test connection to configured backend or Gemini.
     */
    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val settings = appSettings.getSettings()
            if (settings.useDirectGemini) {
                if (settings.geminiApiKey.isBlank()) {
                    throw IllegalStateException("API Key Gemini belum diisi.")
                }
                val url = "https://generativelanguage.googleapis.com/v1beta/models?key=${settings.geminiApiKey}"
                val request = Request.Builder().url(url).get().build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    "Koneksi Google Gemini API Berhasil (${response.code} OK)"
                } else {
                    throw IllegalStateException("Gemini API Error: ${response.code} ${response.message}")
                }
            } else {
                val pingUrl = if (settings.backendUrl.endsWith("/api/analyze")) {
                    settings.backendUrl.removeSuffix("/api/analyze") + "/health"
                } else {
                    settings.backendUrl
                }
                val request = Request.Builder().url(pingUrl).get().build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful || response.code == 404 || response.code == 405) {
                    "Koneksi Backend Berhasil dijangkau (${response.code})"
                } else {
                    throw IllegalStateException("Backend Error: ${response.code} ${response.message}")
                }
            }
        }
    }
}
