package com.example.data.api

import com.example.model.Candle
import com.example.model.ConfirmationType
import com.example.model.DataSourceMode
import com.example.model.SignalType
import com.example.model.TechnicalAnalysisResult
import com.example.model.TradingSignal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UniversalSignalEngine {

    private val _activeDataSource = MutableStateFlow(DataSourceMode.MT5_VISION)
    val activeDataSource: StateFlow<DataSourceMode> = _activeDataSource.asStateFlow()

    private val _latestTechnicalResult = MutableStateFlow<TechnicalAnalysisResult?>(null)
    val latestTechnicalResult: StateFlow<TechnicalAnalysisResult?> = _latestTechnicalResult.asStateFlow()

    fun setDataSourceMode(mode: DataSourceMode) {
        _activeDataSource.value = mode
    }

    /**
     * Evaluates Olymp Trade candles using pure technical indicators.
     */
    fun evaluateOlympTradeCandles(candles: List<Candle>, pair: String, timeframe: String): TradingSignal {
        val analysis = TechnicalIndicators.evaluateMarketData(candles, pair, timeframe)
        _latestTechnicalResult.value = analysis

        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val latestClose = candles.lastOrNull()?.close

        val confirmation = if (analysis.confidenceScore >= 0.70) {
            ConfirmationType.CONFIRMED
        } else if (analysis.confidenceScore >= 0.50) {
            ConfirmationType.PENDING
        } else {
            ConfirmationType.UNCONFIRMED
        }

        // SL & TP estimates based on ATR
        val atr = analysis.atr14 ?: 0.0010
        val stopLoss = when (analysis.signal) {
            SignalType.BUY -> latestClose?.minus(atr * 1.5)
            SignalType.SELL -> latestClose?.plus(atr * 1.5)
            SignalType.WAIT -> null
        }
        val takeProfit = when (analysis.signal) {
            SignalType.BUY -> latestClose?.plus(atr * 3.0)
            SignalType.SELL -> latestClose?.minus(atr * 3.0)
            SignalType.WAIT -> null
        }

        return TradingSignal(
            signal = analysis.signal,
            pair = pair,
            timeframe = timeframe,
            entry = latestClose,
            stopLoss = stopLoss,
            takeProfit = takeProfit,
            riskReward = "1:2 (Durasi ${analysis.durationSeconds}s)",
            confidence = analysis.confidenceScore,
            trend = analysis.trend,
            confirmation = confirmation,
            reason = "[Olymp Trade Mode] ${analysis.reason}",
            timestamp = timestamp
        )
    }
}
