package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ConfirmationType
import com.example.model.DataVerificationLevel
import com.example.model.EngineState
import com.example.model.SignalType
import com.example.model.TradingSignal

@Entity(tableName = "signals")
data class SignalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val signal: String,
    val pair: String,
    val timeframe: String,
    val entry: Double?,
    val stopLoss: Double?,
    val takeProfit: Double?,
    val riskReward: String?,
    val confidence: Double?,
    val trend: String,
    val confirmation: String,
    val reason: String,
    val timestamp: String
) {
    fun toDomain(): TradingSignal {
        val signalType = SignalType.fromString(signal)
        val state = when (signalType) {
            SignalType.BUY -> EngineState.BUY
            SignalType.SELL -> EngineState.SELL
            SignalType.WAIT -> EngineState.WAIT
        }
        return TradingSignal(
            id = id,
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
            confirmation = ConfirmationType.fromString(confirmation),
            reason = reason,
            timestamp = timestamp
        )
    }

    companion object {
        fun fromDomain(domain: TradingSignal): SignalEntity {
            return SignalEntity(
                id = domain.id,
                signal = domain.signal.name,
                pair = domain.pair,
                timeframe = domain.timeframe,
                entry = domain.entry,
                stopLoss = domain.stopLoss,
                takeProfit = domain.takeProfit,
                riskReward = domain.riskReward,
                confidence = domain.confidence,
                trend = domain.trend,
                confirmation = domain.confirmation.name,
                reason = domain.reason,
                timestamp = domain.timestamp
            )
        }
    }
}
