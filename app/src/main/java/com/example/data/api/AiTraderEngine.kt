package com.example.data.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.data.engine.UniversalSignalEngine as OnDeviceEngine
import com.example.data.pref.AppSettings
import com.example.model.ConfirmationType
import com.example.model.EngineState
import com.example.model.Mt5Timeframe
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
import java.util.concurrent.TimeUnit

/**
 * Direct Gemini Vision & Quantitative Engine for MT5 Chart Analysis.
 * Runs 100% without self-hosted backend, VPS, or proxy servers.
 */
class AiTraderEngine(
    private val appSettings: AppSettings,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val TAG = "AiTraderEngine"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val onDeviceEngine = OnDeviceEngine()

    /**
     * Tests connectivity directly to the Gemini API endpoint.
     */
    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        val settings = appSettings.getSettings()
        val apiKey = settings.geminiApiKey.trim()

        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                Exception("Gemini API Key belum diatur. Masukkan API Key di menu Pengaturan untuk koneksi AI.")
            )
        }

        try {
            val model = if (settings.selectedModel.isNotBlank()) settings.selectedModel else "gemini-2.5-flash"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            response.use { resp ->
                if (resp.isSuccessful) {
                    Result.success("Terhubung langsung ke Google Gemini API ($model)")
                } else {
                    val code = resp.code
                    val errorBody = resp.body?.string().orEmpty()
                    val errorMessage = extractApiErrorMessage(errorBody, code)
                    Result.failure(Exception("Gemini API Gagal (HTTP $code): $errorMessage"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed: ${e.message}", e)
            Result.failure(Exception("Koneksi gagal: ${e.localizedMessage ?: "Timeout / Masalah Jaringan"}"))
        }
    }

    /**
     * Analyzes captured MT5 chart bitmap.
     * Uses Direct Gemini Vision API when key is configured, or on-device quantitative engine as local fallback.
     */
    suspend fun analyzeChart(bitmap: Bitmap): Result<TradingSignal> = withContext(Dispatchers.IO) {
        val settings = appSettings.getSettings()
        val apiKey = settings.geminiApiKey.trim()

        // If Gemini API Key is configured and direct AI is active, query Gemini Vision directly
        if (apiKey.isNotBlank() && settings.useDirectGemini) {
            return@withContext analyzeWithGeminiVision(bitmap, apiKey, settings.selectedModel)
        }

        // On-Device Quantitative Engine Fallback (100% Local Scanner)
        return@withContext try {
            val signal = onDeviceEngine.processChartFrame(bitmap, Mt5Timeframe.M5)
            Result.success(signal)
        } catch (e: Exception) {
            Log.e(TAG, "On-device analysis error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Direct Gemini Vision API call via official REST endpoint.
     */
    private fun analyzeWithGeminiVision(
        bitmap: Bitmap,
        apiKey: String,
        selectedModel: String
    ): Result<TradingSignal> {
        return try {
            val model = if (selectedModel.isNotBlank()) selectedModel else "gemini-2.5-flash"
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            // 1. Compress bitmap to JPEG Base64
            val base64Image = compressBitmapToBase64(bitmap)

            // 2. Build structured prompt
            val prompt = """
                Anda adalah AI Quantitative Analyst untuk MetaTrader 5 (MT5). Analisis gambar screenshot chart ini secara presisi.
                Identifikasi:
                1. Simbol / Pair (misal EURUSD, GBPUSD, XAUUSD) yang tertera di chart.
                2. Timeframe yang aktif (M1, M5, M15, M30, H1, H4, D1).
                3. Harga saat ini (Bid/Ask/Last Close), Support, Resistance, dan Trend.
                4. Konfirmasi pola candlestick dan momentum.
                5. Tentukan sinyal: "BUY", "SELL", atau "WAIT".

                Aturan Ketat:
                - Jika chart tidak jelas, terpotong, atau tidak meyakinkan, sinyal WAJIB "WAIT" dan confirmation "UNCONFIRMED".
                - Jangan pernah mengarang harga Entry, Stop Loss, atau Take Profit jika tidak terbaca jelas pada skala harga chart.
                - Balas HANYA dengan JSON murni tanpa format markdown tambahan:
                {
                  "signal": "BUY" | "SELL" | "WAIT",
                  "pair": "SIMBOL",
                  "timeframe": "M5",
                  "entry": 1.08500,
                  "stop_loss": 1.08300,
                  "take_profit": 1.08900,
                  "risk_reward": "1:2",
                  "confidence": 85.0,
                  "trend": "BULLISH" | "BEARISH" | "SIDEWAYS",
                  "confirmation": "CONFIRMED" | "PENDING" | "UNCONFIRMED",
                  "reason": "Alasan analisis teknikal ringkas"
                }
            """.trimIndent()

            // 3. Construct JSON Payload
            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                            put(JSONObject().apply {
                                put("inline_data", JSONObject().apply {
                                    put("mime_type", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)

                val genConfig = JSONObject().apply {
                    put("temperature", 0.1)
                    put("response_mime_type", "application/json")
                }
                put("generationConfig", genConfig)
            }

            val requestBody = requestJson.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    val code = resp.code
                    val errorBody = resp.body?.string().orEmpty()
                    val errorMessage = extractApiErrorMessage(errorBody, code)
                    return Result.failure(Exception("Gemini API Error (HTTP $code): $errorMessage"))
                }

                val responseBody = resp.body?.string().orEmpty()
                val parsedText = extractCandidateText(responseBody)
                if (parsedText.isBlank()) {
                    return Result.failure(Exception("Respons Gemini kosong atau tidak memuat kandidat jawaban."))
                }

                SignalValidator.parseAndValidate(parsedText)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini Vision analysis failed: ${e.message}", e)
            Result.failure(Exception("Gagal menganalisis via Gemini: ${e.localizedMessage ?: "Timeout / Koneksi Terputus"}"))
        }
    }

    private fun compressBitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Resize if too large to conserve bandwidth & prevent latency
        val scaledBitmap = if (bitmap.width > 1280 || bitmap.height > 1280) {
            val scale = 1280f / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }

        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun extractCandidateText(jsonResponse: String): String {
        return runCatching {
            val root = JSONObject(jsonResponse)
            val candidates = root.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""
            if (parts.length() == 0) return ""
            parts.getJSONObject(0).optString("text", "")
        }.getOrDefault("")
    }

    private fun extractApiErrorMessage(responseBody: String, httpCode: Int): String {
        return runCatching {
            val json = JSONObject(responseBody)
            val error = json.optJSONObject("error")
            val message = error?.optString("message")
            if (!message.isNullOrBlank()) message else "Kode HTTP $httpCode"
        }.getOrDefault("Kode HTTP $httpCode")
    }
}
