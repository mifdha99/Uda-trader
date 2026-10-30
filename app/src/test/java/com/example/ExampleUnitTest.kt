package com.example

import com.example.data.engine.Candlestick
import com.example.data.engine.TechnicalIndicators
import com.example.model.ConfirmationType
import com.example.model.Mt5Timeframe
import com.example.model.RiskSettings
import com.example.model.SignalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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
        // Strong upward price momentum should have RSI > 60
        val bullPrices = (1..20).map { 1.08000 + (it * 0.00050) }
        val rsiBull = TechnicalIndicators.calculateRSI(bullPrices, 14)
        assertTrue("RSI should be high on strong bullish move, got $rsiBull", rsiBull > 70.0)

        // Strong downward price momentum should have RSI < 35
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
