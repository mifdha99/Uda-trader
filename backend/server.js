/**
 * UDA AI TRADER - Secure Gemini Vision Backend Server
 * 
 * Provides an isolated, secure gateway between the Android App and Google Gemini API.
 * Keeps API keys safely out of client binaries.
 */

const express = require('express');
const cors = require('cors');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 8080;

app.use(cors());
app.use(express.json({ limit: '15mb' }));

const SYSTEM_INSTRUCTION = `
You are UDA AI TRADER, an institutional quantitative trading AI and expert Computer Vision chart analyst for MetaTrader 5 (MT5).
Analyze the provided MetaTrader 5 chart screenshot with rigorous technical scrutiny.

### CRITICAL RULES:
1. Never hallucinate or invent prices, labels, or currency pairs not clearly visible on the chart.
2. If the image is unclear, blurry, shows a blank/loading screen, or does not clearly display an MT5 chart, output "signal": "WAIT" with "reason": "Tampilan chart MT5 tidak terbaca atau tidak memadai untuk analisis."
3. Never force a BUY or SELL. High-probability trading requires confluence. If market conditions are choppy, consolidated without clear range, or unviable, output "WAIT".
4. Do not base any decision solely on a single isolated candlestick without structural context.
5. "confidence" MUST be null unless you have an objectively calibrated statistical probability model.

### CORE ANALYSIS CRITERIA:
- Candlestick Structure & Price Action (pin bar, engulfing, rejection wicks).
- Bullish and Bearish momentum & volume flow.
- Support and Resistance key zones.
- Institutional Supply and Demand zones.
- Market Structure: Higher Highs (HH), Higher Lows (HL) for uptrend; Lower Highs (LH), Lower Lows (LL) for downtrend.
- Breakout vs False Breakout (Liquidity Sweeps, Hunt of stops).
- Institutional footprints: Order Blocks (OB) and Fair Value Gaps (FVG) / Imbalances.
- Overall Trend (BULLISH, BEARISH, SIDEWAYS, or UNKNOWN).
- Signal Confirmation: Mark as "CONFIRMED" only if price has tapped an OB/FVG/key level with structural break (BOS/CHoCH) and rejection confirmation. Mark as "PENDING" if setup is forming but lacks candle close confirmation. Mark as "WAIT" otherwise.

### MANDATORY OUTPUT FORMAT:
You MUST reply with a single valid JSON object only (no markdown, no backticks, no other text):
{
  "signal": "BUY" | "SELL" | "WAIT",
  "pair": "EURUSD",
  "timeframe": "M5",
  "entry": 1.08500,
  "stop_loss": 1.08400,
  "take_profit": 1.08700,
  "risk_reward": "1:2",
  "confidence": null,
  "trend": "BULLISH",
  "confirmation": "CONFIRMED",
  "reason": "Alasan analisis teknikal lengkap dalam bahasa Indonesia",
  "timestamp": "ISO-8601"
}
`;

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({
    status: 'online',
    service: 'UDA AI TRADER Backend',
    timestamp: new Date().toISOString()
  });
});

// Chart Vision Analysis endpoint
app.post('/api/analyze', async (req, res) => {
  try {
    const { image, apiKey } = req.body;

    if (!image) {
      return res.status(400).json({ error: 'Image base64 data is required.' });
    }

    const effectiveApiKey = apiKey || process.env.GEMINI_API_KEY;
    if (!effectiveApiKey) {
      return res.status(401).json({
        error: 'Gemini API Key is not configured on the backend server nor provided in request.'
      });
    }

    const modelName = process.env.GEMINI_MODEL || 'gemini-2.5-flash';
    const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${modelName}:generateContent?key=${effectiveApiKey}`;

    const payload = {
      contents: [
        {
          parts: [
            { text: `${SYSTEM_INSTRUCTION}\n\nAnalyze this MetaTrader 5 chart image now. Output strictly JSON.` },
            {
              inlineData: {
                mimeType: 'image/jpeg',
                data: image
              }
            }
          ]
        }
      ],
      generationConfig: {
        responseMimeType: 'application/json',
        temperature: 0.1
      }
    };

    const response = await fetch(geminiUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error('Gemini API Error:', response.status, errorText);
      return res.status(response.status).json({
        error: `Gemini API returned status ${response.status}`,
        details: errorText
      });
    }

    const data = await response.json();
    const candidateText = data.candidates?.[0]?.content?.parts?.[0]?.text;

    if (!candidateText) {
      return res.status(500).json({ error: 'No response generated from Gemini Vision.' });
    }

    // Clean backticks if any
    let cleanedJson = candidateText.trim();
    if (cleanedJson.startsWith('```json')) cleanedJson = cleanedJson.replace(/^```json/, '');
    if (cleanedJson.startsWith('```')) cleanedJson = cleanedJson.replace(/^```/, '');
    if (cleanedJson.endsWith('```')) cleanedJson = cleanedJson.replace(/```$/, '');
    cleanedJson = cleanedJson.trim();

    const parsedSignal = JSON.parse(cleanedJson);

    // Enforce confidence: null rule
    parsedSignal.confidence = null;

    if (!parsedSignal.timestamp) {
      parsedSignal.timestamp = new Date().toISOString();
    }

    return res.json(parsedSignal);
  } catch (err) {
    console.error('Analysis error:', err);
    return res.status(500).json({
      signal: 'WAIT',
      pair: 'UNKNOWN',
      timeframe: 'M5',
      entry: null,
      stop_loss: null,
      take_profit: null,
      risk_reward: '1:2',
      confidence: null,
      trend: 'UNKNOWN',
      confirmation: 'UNCONFIRMED',
      reason: `Server error: ${err.message}`,
      timestamp: new Date().toISOString()
    });
  }
});

app.listen(PORT, () => {
  console.log(`UDA AI TRADER Backend running on port ${PORT}`);
});
