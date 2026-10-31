package com.example

import com.example.data.engine.Candlestick
import com.example.data.engine.TechnicalIndicators
import com.example.model.ConfirmationType
import com.example.model.DataVerificationLevel
import com.example.model.EngineState
import com.example.model.Mt5Timeframe
import com.example.model.RiskSettings
import com.example.model.SignalType
import com.example.model.TradingSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testTechnicalIndicatorsEMA() {
        val prices = listOf(10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0)
        val ema = TechnicalIndicators.calculateEMA(prices, 5)
        assertEquals(prices.size, ema.size)
        // With rising prices, EMA should track upwards
        assertTrue(ema.last() > ema.first())
    }

    @Test
    fun testTechnicalIndicatorsRSI() {
        val bullPrices = (1..20).map { 1.08000 + (it * 0.00050) }
        val rsiBull = TechnicalIndicators.calculateRSI(bullPrices, 14)
        assertTrue("RSI should be high on strong bullish move, got $rsiBull", rsiBull > 70.0)

        val bearPrices = (1..20).map { 1.09000 - (it * 0.00050) }
        val rsiBear = TechnicalIndicators.calculateRSI(bearPrices, 14)
        assertTrue("RSI should be low on strong bearish move, got $rsiBear", rsiBear < 30.0)
    }

    @Test
    fun testCandlestickPatterns() {
        // Bullish Hammer: lower wick >= 2x body, small upper wick
        val hammer = Candlestick(
            index = 0,
            open = 1.08200,
            close = 1.08250,
            high = 1.08260,
            low = 1.08050
        )
        assertTrue("Candle should be recognized as hammer", hammer.isHammer())

        // Bullish Engulfing
        val prevRed = Candlestick(
            index = 0,
            open = 1.08500,
            close = 1.08400,
            high = 1.08520,
            low = 1.08380
        )
        val currGreen = Candlestick(
            index = 1,
            open = 1.08390,
            close = 1.08550,
            high = 1.08560,
            low = 1.08380
        )
        assertTrue("Should be bullish engulfing", TechnicalIndicators.isBullishEngulfing(prevRed, currGreen))
    }

    @Test
    fun testMt5TimeframeCodes() {
        assertEquals(Mt5Timeframe.M1, Mt5Timeframe.fromCode("M1"))
        assertEquals(Mt5Timeframe.M5, Mt5Timeframe.fromCode("m5"))
        assertEquals(Mt5Timeframe.H1, Mt5Timeframe.fromCode("H1"))
        assertEquals(Mt5Timeframe.D1, Mt5Timeframe.fromCode("D1"))
        assertEquals(Mt5Timeframe.M5, Mt5Timeframe.fromCode("UNKNOWN"))
    }

    @Test
    fun testRiskCalculatorStandardForexLot() {
        val risk = RiskSettings(
            accountBalance = 1000.0,
            riskPercentage = 1.0,
            stopLossPips = 20.0,
            instrumentType = "Forex (Standard Lot)"
        )
        assertEquals(10.0, risk.riskAmount, 0.001)
        assertEquals(0.05, risk.calculateEstimatedLot(), 0.001)
    }

    @Test
    fun testUncalibratedPriceFormattingStrictness() {
        val uncalibratedSignal = TradingSignal(
            state = EngineState.WAIT,
            signal = SignalType.WAIT,
            pair = "",
            timeframe = "M5",
            verificationLevel = DataVerificationLevel.VISUAL_ESTIMATION,
            entry = null,
            stopLoss = null,
            takeProfit = null,
            confidence = 55.0,
            timestamp = "2026-04-15 10:00:00"
        )

        assertEquals("TIDAK DIKENALI", uncalibratedSignal.formattedSymbol())
        assertEquals("TIDAK TERSEDIA", uncalibratedSignal.formattedEntry())
        assertEquals("TIDAK TERSEDIA", uncalibratedSignal.formattedStopLoss())
        assertEquals("TIDAK TERSEDIA", uncalibratedSignal.formattedTakeProfit())

        val copyText = uncalibratedSignal.formattedCopyText()
        assertTrue(copyText.contains("Simbol: TIDAK DIKENALI"))
        assertTrue(copyText.contains("Entry: TIDAK TERSEDIA"))
        assertTrue(copyText.contains("SL: TIDAK TERSEDIA"))
        assertTrue(copyText.contains("TP: TIDAK TERSEDIA"))
        assertTrue(copyText.contains("Status: WAIT"))
    }

    @Test
    fun test10EngineStatesExistence() {
        val allStates = EngineState.values()
        assertEquals(10, allStates.size)
        assertTrue(allStates.any { it.name == "WAITING_CHART" && it.displayName == "MENUNGGU CHART" })
        assertTrue(allStates.any { it.name == "READING_CHART" && it.displayName == "MEMBACA CHART" })
        assertTrue(allStates.any { it.name == "VALIDATING_DATA" && it.displayName == "MEMVALIDASI DATA" })
        assertTrue(allStates.any { it.name == "ANALYZING" && it.displayName == "MENGANALISIS" })
        assertTrue(allStates.any { it.name == "BUY" && it.displayName == "BUY NOW" })
        assertTrue(allStates.any { it.name == "SELL" && it.displayName == "SELL NOW" })
        assertTrue(allStates.any { it.name == "WAIT" && it.displayName == "WAIT" })
        assertTrue(allStates.any { it.name == "INVALID_DATA" && it.displayName == "DATA TIDAK VALID" })
        assertTrue(allStates.any { it.name == "STREAM_DISCONNECTED" && it.displayName == "LIVE SCREEN TERPUTUS" })
        assertTrue(allStates.any { it.name == "DATA_EXPIRED" && it.displayName == "DATA KEDALUWARSA" })
    }
}
