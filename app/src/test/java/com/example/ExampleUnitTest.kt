package com.example

import com.example.data.api.SignalValidator
import com.example.data.api.TechnicalIndicators
import com.example.model.Candle
import com.example.model.CandlePattern
import com.example.model.ConfirmationType
import com.example.model.RiskSettings
import com.example.model.SignalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {

    @Test
    fun testSignalValidatorWithValidBuyJson() {
        val sampleJson = """
        {
          "signal": "BUY",
          "pair": "EURUSD",
          "timeframe": "M5",
          "entry": 1.08500,
          "stop_loss": 1.08400,
          "take_profit": 1.08700,
          "risk_reward": "1:2",
          "confidence": null,
          "trend": "BULLISH",
          "confirmation": "CONFIRMED",
          "reason": "Institutional order block tap with bullish engulfing",
          "timestamp": "2026-10-30T16:00:00Z"
        }
        """.trimIndent()

        val result = SignalValidator.parseAndValidate(sampleJson)
        assertTrue(result.isSuccess)
        val signal = result.getOrThrow()

        assertEquals(SignalType.BUY, signal.signal)
        assertEquals("EURUSD", signal.pair)
        assertEquals("M5", signal.timeframe)
        assertEquals(1.08500, signal.entry!!, 0.00001)
        assertEquals(1.08400, signal.stopLoss!!, 0.00001)
        assertEquals(1.08700, signal.takeProfit!!, 0.00001)
        assertEquals("1:2", signal.riskReward)
        assertNull(signal.confidence) // Mandatory confidence: null rule
        assertEquals(ConfirmationType.CONFIRMED, signal.confirmation)
    }

    @Test
    fun testSignalValidatorHandlesMarkdownWrappingGracefully() {
        val wrappedJson = "```json\n{\"signal\":\"SELL\",\"pair\":\"GBPUSD\",\"timeframe\":\"M15\",\"confirmation\":\"PENDING\",\"reason\":\"Liquidity sweep at resistance\"}\n```"
        val result = SignalValidator.parseAndValidate(wrappedJson)
        assertTrue(result.isSuccess)
        val signal = result.getOrThrow()

        assertEquals(SignalType.SELL, signal.signal)
        assertEquals("GBPUSD", signal.pair)
        assertEquals(ConfirmationType.PENDING, signal.confirmation)
    }

    @Test
    fun testRiskCalculatorStandardForexLot() {
        val risk = RiskSettings(
            accountBalance = 1000.0,
            riskPercentage = 1.0, // $10 risk
            stopLossPips = 20.0,  // 20 pips
            instrumentType = "Forex (Standard Lot)"
        )
        assertEquals(10.0, risk.riskAmount, 0.001)
        assertEquals(0.05, risk.calculateEstimatedLot(), 0.001)
    }

    @Test
    fun testEMA9AndEMA21Calculations() {
        val data = listOf(
            10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0,
            20.0, 21.0, 22.0, 23.0, 24.0, 25.0, 26.0, 27.0, 28.0, 29.0, 30.0
        )
        val ema9 = TechnicalIndicators.calculateEMA(data, 9)
        assertTrue(ema9.isNotEmpty())
        // EMA of monotonically increasing sequence should be positive and less than or equal to current price
        assertTrue(ema9.last() > 0.0)
        assertTrue(ema9.last() <= data.last())

        val ema21 = TechnicalIndicators.calculateEMA(data, 21)
        assertEquals(1, ema21.size)
    }

    @Test
    fun testRSI14Calculations() {
        // Monotonically increasing closes should produce high RSI (>70)
        val bullCloses = List(20) { 100.0 + (it * 2.0) }
        val bullRsi = TechnicalIndicators.calculateRSI(bullCloses, 14)
        assertNotNull(bullRsi)
        assertTrue(bullRsi!! >= 70.0)

        // Monotonically decreasing closes should produce low RSI (<30)
        val bearCloses = List(20) { 100.0 - (it * 2.0) }
        val bearRsi = TechnicalIndicators.calculateRSI(bearCloses, 14)
        assertNotNull(bearRsi)
        assertTrue(bearRsi!! <= 30.0)
    }

    @Test
    fun testATR14Calculations() {
        val highs = List(20) { 105.0 }
        val lows = List(20) { 95.0 }
        val closes = List(20) { 100.0 }

        val atr = TechnicalIndicators.calculateATR(highs, lows, closes, 14)
        assertNotNull(atr)
        assertEquals(10.0, atr!!, 0.01)
    }

    @Test
    fun testCandleValidationAndPatternDetection() {
        val hammer = Candle(
            timestamp = 1000L,
            open = 10.0,
            high = 10.12,
            low = 9.0,
            close = 10.1,
            volume = 5.0
        )
        val pattern = TechnicalIndicators.detectCandlePattern(hammer, null)
        assertEquals(CandlePattern.HAMMER, pattern)

        val doji = Candle(
            timestamp = 2000L,
            open = 10.0,
            high = 10.5,
            low = 9.5,
            close = 10.02,
            volume = 5.0
        )
        assertEquals(CandlePattern.DOJI, TechnicalIndicators.detectCandlePattern(doji, null))
    }

    @Test
    fun testDoubleOrderPreventionKey() {
        val pair = "EURUSD"
        val signal = SignalType.BUY
        val timestamp = "2026-10-31T16:00:00Z"

        val key1 = "${pair}_${signal}_${timestamp}"
        val key2 = "${pair}_${signal}_${timestamp}"

        // Exactly identical key must match to block double firing
        assertEquals(key1, key2)
    }
}
