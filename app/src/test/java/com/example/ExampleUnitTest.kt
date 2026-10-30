package com.example

import com.example.data.api.SignalValidator
import com.example.model.ConfirmationType
import com.example.model.RiskSettings
import com.example.model.SignalType
import org.junit.Assert.assertEquals
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
        // 20 pips * $10/pip/lot = $200 risk per standard lot.
        // $10 risk / $200 = 0.05 lot
        assertEquals(0.05, risk.calculateEstimatedLot(), 0.001)
    }
}
