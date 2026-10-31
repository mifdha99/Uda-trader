# UDA AI TRADER - Native Android Quantitative Trading Application

Aplikasi Android native institusional untuk analisis real-time chart **MetaTrader 5 (MT5)** dan integrasi streaming data serta auto-trading **Olymp Trade** (berbasis protokol WebSocket [ChipaDevTeam/OlympTradeAPI](https://github.com/ChipaDevTeam/OlympTradeAPI)).

---

## 🚀 Fitur Utama & Dua Mode Operasi

Aplikasi memiliki dua mode operasi data yang dapat beralih secara instan:

### 1. MODE MT5 (SCREEN CAPTURE + GEMINI VISION AI)
- Menangkap chart MetaTrader 5 secara berkala (1, 2, 3, atau 5 detik) menggunakan **Android MediaProjection API**.
- Berjalan pada **Foreground Service** dengan notifikasi kontrol transparan (`FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION`).
- Analisis struktur pasar kuantitatif dengan **Google Gemini Vision**:
  - Struktur Candlestick & Rejection Wicks.
  - Momentum Bullish / Bearish.
  - Support & Resistance, Supply & Demand.
  - Market Structure: Higher Highs (HH), Higher Lows (HL), Lower Highs (LH), Lower Lows (LL).
  - Breakout vs False Breakout (Liquidity Sweeps & Stop Hunts).
  - Order Blocks (OB) & Fair Value Gaps (FVG).
  - **Prinsip Anti-Halusinasi:** Tidak mengarang harga. Jika chart tidak terbaca atau pasar berkonsolidasi, sistem menghasilkan status **WAIT**.

### 2. MODE OLYMP TRADE (WEBSOCKET REAL-TIME + UNIVERSAL SIGNAL ENGINE)
- Menghubungkan langsung ke endpoint resmi WebSocket Olymp Trade (`wss://ws.olymptrade.com/otp`) menggunakan `access_token` session browser.
- **Arsitektur Tanpa Server Berbayar:** Berjalan 100% lokal di HP Android menggunakan koneksi WebSocket asinkron OkHttp (menghindari penambahan interpreter Python/Chaquopy yang tidak kompatibel dengan Android Gradle Plugin 9.1.1 dan memboroskan RAM/baterai).
- Streaming tick real-time dan rolling candlestick dinamis.
- Pembacaan saldo akun demo (`group == "demo"`).
- **Universal Signal Engine & Indikator Teknikal:**
  - **EMA 9 & EMA 21:** Deteksi tren arah dan perpotongan dinamis.
  - **RSI 14:** Deteksi kondisi overbought (>70), oversold (<30), dan momentum.
  - **ATR 14:** Pengukuran volatilitas pasar untuk menentukan jarak target.
  - **Support & Resistance:** Deteksi swing fractal otomatis.
  - **Pola Candlestick:** Deteksi Bullish/Bearish Engulfing, Hammer, Shooting Star, dan Doji.
  - **Skor Konfluensi Kuantitatif:** Menggabungkan 4 pilar aturan (EMA + RSI + Pola + S/R) menjadi skor matematis 0 - 100%. Sinyal UP/CALL atau DOWN/PUT hanya keluar jika skor $\ge$ 65%.

---

## 🤖 Sistem Auto Trading 4 Tahap (Olymp Trade)

Aplikasi menerapkan pengamanan ketat 4 tahap:

1. **TAHAP A — READ ONLY:**
   Menghubungkan akun demo dan mengalirkan data pasar tanpa mengirim order.
2. **TAHAP B — SIGNAL TEST:**
   Menganalisis data nyata dan menghasilkan sinyal UP/CALL, DOWN/PUT, atau WAIT tanpa mengeksekusi order.
3. **TAHAP C — DEMO EXECUTION:**
   Mengeksekusi order demo hanya setelah pengguna mengonfirmasi akun demo, instrumen, nominal ($), durasi kontrak (detik), dan ambang batas sinyal.
   - Dilengkapi **Mutex Lock & Idempotency Key** untuk mencegah order ganda.
   - Pengecekan ketat batas transaksi harian dan batas kerugian harian sebelum setiap order.
4. **TAHAP D — RESULT VERIFICATION:**
   Memverifikasi respons server dan status ID order (`serverOrderId`).

### Fitur Pengaman Auto Trading:
- **EMERGENCY STOP (Tombol Darurat):** Menghentikan seketika seluruh perulangan order, membatalkan transaksi pending, dan memutus koneksi.
- **Larangan Martingale:** Tidak ada penggandaan nominal modal setelah kerugian.
- **Wajib Akun Demo:** Fitur otomatis dibatasi secara ketat hanya untuk grup akun demo.

---

## 🪟 Floating Window (HUD Overlay di Atas Broker)

- Mengambang langsung di atas MetaTrader 5 atau Olymp Trade menggunakan izin `SYSTEM_ALERT_WINDOW`.
- Tampilan:
  - Indikator Sumber Data: `MT5 (VISION)` atau `OLYMP TRADE (WS)`.
  - Sinyal: 🟢 **BUY NOW / UP (CALL)**, 🔴 **SELL NOW / DOWN (PUT)**, 🟡 **WAIT**.
  - Metrik: **PAIR**, **TIMEFRAME**, **ENTRY / HARGA**, **R:R / DURASI**.
- Tombol Interaktif:
  - **COPY:** Menyalin sinyal langsung ke clipboard untuk eksekusi manual di aplikasi broker.
  - **STOP:** Tombol darurat cepat langsung dari floating card.
  - **Draggable:** Dapat digeser ke segala arah dan otomatis dibatasi tepi layar (*screen clamping*).
  - **Minimizable:** Dapat diperkecil menjadi floating bubble kecil agar tidak menghalangi chart.

---

## 📱 Berjalan Saat Layar Mati (POCO / Android Background Guide)

Aplikasi menyertakan `OlympTradeBackgroundService` dengan `FOREGROUND_SERVICE_TYPE_DATA_SYNC` dan `WakeLock` terkelola (maks. batas aman 30 menit).

### Panduan Khusus HP POCO / Xiaomi / HyperOS / MIUI:
1. Buka **Setelan** > **Aplikasi** > **Kelola Aplikasi** > **UDA AI TRADER**.
2. Pilih **Penghemat Baterai** > Ubah ke **Tidak Ada Pembatasan (No Restrictions)**.
3. Aktifkan sakelar **Mulai Otomatis (Autostart)**.
4. Buka **Recent Apps** (task manager), tahan kartu aplikasi UDA AI TRADER dan tekan tombol **Gembok (Lock)**.

---

## 🖥️ Struktur 5 Tab Antarmuka Aplikasi

1. **TRADING:** Cockpit utama pemantauan sinyal, grafik mini, toggle MT5 vs Olymp Trade, tombol COPY, dan kontrol Auto Trading.
2. **ENGINEER:** Diagnostik kuantitatif mendalam (nilai aktual EMA 9, EMA 21, RSI 14, ATR 14, Support & Resistance, Pola Candlestick, dan Skor Konfluensi).
3. **RISIKO:** Kalkulator risiko persentase (0.5% - 5%), kalkulasi lot instrumen, dan batas harian auto-trading.
4. **RIWAYAT:** Catatan sinyal dan order tersimpan dalam database Room lokal dengan filter BUY, SELL, WAIT.
5. **PENGATURAN:** Konfigurasi token sesi Olymp Trade, pasangan aset aktif, Gemini API key langsung, dan pengujian koneksi.

---

## ⚙️ Kompilasi & Build APK di GitHub Actions

Workflow GitHub Actions berada di `.github/workflows/android-build.yml`:
- Menggunakan JDK 17 dan Gradle Wrapper resmi.
- Menjalankan unit test (`./gradlew testDebugUnitTest`).
- Mengompilasi APK debug (`./gradlew assembleDebug`).
- Memverifikasi keberadaan file APK dan mengunggah artefak ke:
  `app/build/outputs/apk/debug/app-debug.apk`

---

## ⚠️ Disclaimer Risiko Trading

Trading Forex, Fixed Time Trades (FTT), dan instrumen derivatif memiliki risiko kerugian finansial yang signifikan. Aplikasi UDA AI TRADER berfungsi sebagai alat bantu analisis kuantitatif dan teknikal, bukan penasihat keuangan. Uji strategi Anda secara mendalam pada akun DEMO sebelum mengambil keputusan finansial.
