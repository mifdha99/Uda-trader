package com.example.data.api

import com.example.model.ConfirmationType
import com.example.model.DataVerificationLevel
import com.example.model.EngineState
import com.example.model.SignalType
import com.example.model.TradingSignal
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SignalValidator {

    /**
     * Cleans markdown blocks (e.g. ```json ... ```) from LLM output.
     */
    fun cleanMarkdown(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```json")) {
            text = text.removePrefix("```json")
        } else if (text.startsWith("```")) {
            text = text.removePrefix("```")
        }
        if (text.endsWith("```")) {
            text = text.removeSuffix("```")
        }
        return text.trim()
    }

    /**
     * Parses and validates a JSON response string into a TradingSignal.
     */
    fun parseAndValidate(jsonString: String): Result<TradingSignal> {
        return runCatching {
            val cleaned = cleanMarkdown(jsonString)
            val json = JSONObject(cleaned)

            val rawSignal = json.optString("signal", "WAIT")
            val signalType = SignalType.fromString(rawSignal)

            val pair = json.optString("pair", "TIDAK DIKENALI").ifBlank { "TIDAK DIKENALI" }
            val timeframe = json.optString("timeframe", "M5").ifBlank { "M5" }

            val entry = if (json.has("entry") && !json.isNull("entry")) {
                json.optDouble("entry")
            } else null

            val stopLoss = when {
                json.has("stop_loss") && !json.isNull("stop_loss") -> json.optDouble("stop_loss")
                json.has("stopLoss") && !json.isNull("stopLoss") -> json.optDouble("stopLoss")
                else -> null
            }

            val takeProfit = when {
                json.has("take_profit") && !json.isNull("take_profit") -> json.optDouble("take_profit")
                json.has("takeProfit") && !json.isNull("takeProfit") -> json.optDouble("takeProfit")
                else -> null
            }

            val riskReward = when {
                json.has("risk_reward") && !json.isNull("risk_reward") -> json.optString("risk_reward")
                json.has("riskReward") && !json.isNull("riskReward") -> json.optString("riskReward")
                else -> "1:2"
            }

            val confidence = if (json.has("confidence") && !json.isNull("confidence")) {
                json.optDouble("confidence")
            } else null

            val trend = json.optString("trend", "UNKNOWN")
            val rawConfirmation = json.optString("confirmation", "UNCONFIRMED")
            val confirmation = ConfirmationType.fromString(rawConfirmation)
            val reason = json.optString("reason", "")
            val timestamp = if (json.has("timestamp") && !json.isNull("timestamp")) {
                json.optString("timestamp")
            } else {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            }

            val state = when (signalType) {
                SignalType.BUY -> EngineState.BUY
                SignalType.SELL -> EngineState.SELL
                SignalType.WAIT -> EngineState.WAIT
            }

            TradingSignal(
                state = state,
                signal = signalType,
                pair = pair,
                timeframe = timeframe,
                detectedTimeframe = timeframe,
                isTimeframeMismatch = false,
                verificationLevel = if (entry != null) DataVerificationLevel.VERIFIED else DataVerificationLevel.VISUAL_ESTIMATION,
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
