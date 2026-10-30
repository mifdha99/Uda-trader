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
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Direct Gemini Vision trading analysis engine running completely client-side without any backend.
 */
class AiTraderEngine(
    private val appSettings: AppSettings
) {
    companion object {
        private const val TAG = "AiTraderEngine"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var previousSignal: TradingSignal? = null

    /**
     * Analyzes captured chart bitmap using Google Gemini Vision API directly.
     */
    suspend fun analyzeChart(bitmap: Bitmap): Result<TradingSignal> = withContext(Dispatchers.IO) {
        runCatching {
            val settings = appSettings.getSettings()
            val apiKey = settings.geminiApiKey.trim()

            if (apiKey.isBlank()) {
                throw IllegalStateException("API Key Gemini belum diset. Silakan masukkan API Key di menu Pengaturan.")
            }

            // 1. Optimize Bitmap to compact JPEG Base64
            val base64Image = bitmapToBase64(bitmap)

            // 2. Call Direct Gemini Vision API
            val rawJson = callDirectGeminiVision(base64Image, apiKey, settings.selectedModel)

            // 3. Parse & Validate
            val validatedResult = SignalValidator.parseAndValidate(rawJson)
            val currentSignal = validatedResult.getOrThrow()

            // 4. Compare with previous signal & apply State Machine logic
            val processedSignal = filterAndProcessSignal(currentSignal)
            previousSignal = processedSignal

            processedSignal
        }
    }

    /**
     * Converts bitmap to optimized JPEG Base64 string.
     * Scales down if larger than 1280px to save bandwidth while keeping candle wicks and text sharp.
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
     * Direct call to Google Gemini Vision API using the provided API key and model.
     * Never exposes or logs the API key.
     */
    private fun callDirectGeminiVision(base64Image: String, apiKey: String, model: String): String {
        val effectiveModel = if (model.isBlank()) "gemini-1.5-flash" else model
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$effectiveModel:generateContent?key=$apiKey"

        val rootJson = JSONObject().apply {
            // System instruction
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", GeminiPrompt.SYSTEM_INSTRUCTION)
                    })
                })
            })

            // Contents with prompt and inline image
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        // Instruction part
                        val textPart = JSONObject().apply {
                            val prevContext = previousSignal?.let {
                                "Sinyal sebelumnya: ${it.signal} pada ${it.pair} ${it.timeframe} (${it.confirmation})."
                            } ?: "Analisis awal / frame pertama."
                            put("text", "Analisis chart MetaTrader 5 berikut secara seksama. $prevContext Berikan output HANYA berupa JSON terstruktur.")
                        }
                        put(textPart)

                        // Image part
                        val inlineData = JSONObject().apply {
                            put("mimeType", "image/jpeg")
                            put("data", base64Image)
                        }
                        put(JSONObject().apply {
                            put("inlineData", inlineData)
                        })
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            // Generation config requesting structured JSON
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

        val response = try {
            httpClient.newCall(request).execute()
        } catch (e: SocketTimeoutException) {
            throw IOException("Koneksi timeout. Gemini API tidak merespons dalam 30 detik.", e)
        } catch (e: UnknownHostException) {
            throw IOException("Gagal terhubung ke internet. Periksa koneksi data seluler / Wi-Fi.", e)
        } catch (e: IOException) {
            throw IOException("Gangguan jaringan: ${e.localizedMessage ?: "Tidak dapat menghubungi server"}", e)
        }

        val responseBody = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            val friendlyError = parseGeminiError(response.code, responseBody)
            Log.e(TAG, "Gemini direct API error code: ${response.code}")
            throw IllegalStateException(friendlyError)
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

        throw IllegalStateException("Respons kosong dari Gemini API.")
    }

    /**
     * Translates Gemini API error codes into descriptive Indonesian error messages.
     */
    private fun parseGeminiError(statusCode: Int, responseBody: String): String {
        val serverMessage = runCatching {
            val obj = JSONObject(responseBody)
            obj.optJSONObject("error")?.optString("message", "") ?: ""
        }.getOrDefault("")

        return when (statusCode) {
            400 -> if (serverMessage.isNotBlank()) "Permintaan tidak valid: $serverMessage" else "Format request tidak valid (HTTP 400)."
            401, 403 -> "Gemini API Key tidak valid atau tidak memiliki izin akses (HTTP $statusCode). Periksa API Key di Pengaturan."
            404 -> "Model Gemini tidak ditemukan (HTTP 404). Silakan pilih model lain di Pengaturan."
            429 -> "Batas kuota penggunaan Gemini API tercapai (Rate Limit 429). Mohon tunggu beberapa saat sebelum mencoba lagi."
            500, 502, 503 -> "Server Google Gemini sedang mengalami gangguan sementara (HTTP $statusCode)."
            else -> "Terjadi kesalahan API (HTTP $statusCode): ${serverMessage.ifBlank { "Respons tidak berhasil" }}"
        }
    }

    /**
     * Differentiates pending vs confirmed, checks direction reversal, and maintains stability.
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

    /**
     * Test connection directly to Google Gemini API.
     */
    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val settings = appSettings.getSettings()
            val apiKey = settings.geminiApiKey.trim()

            if (apiKey.isBlank()) {
                throw IllegalStateException("API Key Gemini belum diisi. Masukkan API Key terlebih dahulu.")
            }

            val testModel = if (settings.selectedModel.isBlank()) "gemini-1.5-flash" else settings.selectedModel
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$testModel?key=$apiKey"
            val request = Request.Builder().url(url).get().build()

            val response = try {
                httpClient.newCall(request).execute()
            } catch (e: UnknownHostException) {
                throw IOException("Tidak dapat terhubung ke internet. Periksa koneksi Anda.", e)
            } catch (e: SocketTimeoutException) {
                throw IOException("Koneksi timeout saat menghubungi Google Gemini API.", e)
            }

            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                "Koneksi Google Gemini API Berhasil ($testModel siap digunakan)"
            } else {
                throw IllegalStateException(parseGeminiError(response.code, responseBody))
            }
        }
    }
}
