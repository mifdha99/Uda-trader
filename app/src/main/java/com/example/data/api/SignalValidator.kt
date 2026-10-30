package com.example.data.api

import com.example.model.ConfirmationType
import com.example.model.SignalType
import com.example.model.TradingSignal
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object SignalValidator {

    fun parseAndValidate(jsonString: String): Result<TradingSignal> {
        return runCatching {
            var cleaned = jsonString.trim()
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.removePrefix("```json").trim()
            }
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.removePrefix("```").trim()
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.removeSuffix("```").trim()
            }

            val json = JSONObject(cleaned)

            val rawSignal = json.optString("signal", "WAIT")
            val signalType = SignalType.fromString(rawSignal)

            val pair = json.optString("pair", "UNKNOWN").ifBlank { "UNKNOWN" }
            val timeframe = json.optString("timeframe", "M5").ifBlank { "M5" }

            val entry = if (json.isNull("entry")) null else json.optDouble("entry").takeUnless { it.isNaN() }
            val stopLoss = if (json.isNull("stop_loss")) null else json.optDouble("stop_loss").takeUnless { it.isNaN() }
            val takeProfit = if (json.isNull("take_profit")) null else json.optDouble("take_profit").takeUnless { it.isNaN() }

            val riskReward = if (json.isNull("risk_reward")) "1:2" else json.optString("risk_reward", "1:2")

            // Confidence MUST be null unless objectively calibrated
            val confidence: Double? = null

            val trend = json.optString("trend", "UNKNOWN").ifBlank { "UNKNOWN" }
            val rawConfirmation = json.optString("confirmation", "UNCONFIRMED")
            val confirmation = ConfirmationType.fromString(rawConfirmation)

            val reason = json.optString("reason", "Analisis chart real-time selesai.").ifBlank {
                "Analisis teknikal selesai."
            }

            var timestamp = json.optString("timestamp", "")
            if (timestamp.isBlank()) {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                timestamp = sdf.format(Date())
            }

            TradingSignal(
                signal = signalType,
                pair = pair,
                timeframe = timeframe,
                entry = entry,
                stopLoss = stopLoss,
                takeProfit = takeProfit,
                riskReward = riskReward,
                confidence = confidence,
                trend = trend,
                confirmation = confirmation,
                reason = reason,
                timestamp = timestamp
            )
        }
    }
}
