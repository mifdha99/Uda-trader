# UDA AI TRADER - Native Android Application

Aplikasi Android native institusional untuk analisis real-time chart **MetaTrader 5 (MT5)** menggunakan **Android MediaProjection API**, **SYSTEM_ALERT_WINDOW (Floating Window)**, dan **Google Gemini Vision AI Engine**.

---

## 🚀 Fitur Utama

1. **Screen Capture Berkala (MediaProjection API)**
   - Mengambil tampilan layar MetaTrader 5 secara berkala (interval 1s, 2s, 3s, atau 5s).
   - Berjalan pada **Foreground Service** dengan notifikasi transparan sesuai standar Android 8 s/d Android 16 (`FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION`).
   - Optimasi resolusi dan kompresi JPEG dinamis guna meminimalkan latensi dan konsumsi data/token API.

2. **Gemini Vision AI Engine (Institutional Market Structure)**
   - Menganalisis aksi harga (Price Action) chart MT5 secara mendalam:
     - Struktur Candlestick & Rejection Wicks.
     - Momentum Bullish / Bearish.
     - Key Support, Resistance, Supply & Demand Zones.
     - Market Structure: Higher Highs (HH), Higher Lows (HL), Lower Highs (LH), Lower Lows (LL).
     - Breakout vs False Breakout (Liquidity Sweeps / Stop Hunts).
     - Footprint institusional: Order Blocks (OB) & Fair Value Gaps (FVG).
     - Trend & Volatilitas pasar.
   - **Anti-Halusinasi:** Tidak mengarang harga yang tidak terbaca pada chart. Jika tampilan tidak jelas atau pasar sideways tidak layak ditradingkan, sistem secara otomatis mengeluarkan status **WAIT**.
   - Output terstruktur JSON ketat dengan `confidence: null` (tanpa simulasi probabilitas palsu).

3. **Floating Window (HUD Overlay di Atas MetaTrader 5)**
   - Menggunakan izin `SYSTEM_ALERT_WINDOW`.
   - Mengambang langsung di atas MetaTrader 5 sehingga trader dapat melihat sinyal tanpa berpindah aplikasi:
     - 🟢 **BUY NOW**
     - 🔴 **SELL NOW**
     - 🟡 **WAIT**
     - PAIR, TIMEFRAME, ENTRY, STOP LOSS, TAKE PROFIT, RISK REWARD.
   - Dapat digeser (draggable) ke segala posisi layar.
   - Dapat diperkecil (minimize ke pill ringkas) agar tidak menghalangi tombol order MT5.
   - Dapat diperbesar (expand) atau ditutup kapan saja.

4. **Sistem Sinyal Real-Time Cerdas**
   - Mendeteksi transisi arah (BUY $\leftrightarrow$ SELL).
   - Membedakan sinyal terkonfirmasi (`CONFIRMED`) vs pending (`PENDING`).
   - Mencegah spam sinyal berulang yang identik.

5. **Manajemen Risiko & Kalkulator Lot**
   - Menghitung risiko modal berdasarkan persentase (0.5% - 5%).
   - Menghitung rasio Risk:Reward (1:1.5, 1:2, 1:3).
   - Menghitung estimasi ukuran lot berdasarkan instrumen kontrak (Forex Standard 100k, Gold XAUUSD 100oz, Crypto, Indeks) tanpa mengasumsikan nilai kontrak broker secara buta.

6. **Penyimpanan Lokal Room Database**
   - Menyimpan seluruh riwayat sinyal secara lokal di perangkat.
   - Fitur filter BUY, SELL, WAIT, dan pembersihan riwayat.

7. **Keamanan Maksimal**
   - Tidak menanamkan API key di dalam binary APK.
   - Mendukung proxy backend mandiri (`backend/`) untuk melindungi API key di sisi server.
   - Mendukung opsi direct Gemini API jika pengguna ingin menggunakan API Key milik sendiri secara lokal melalui pengaturan aplikasi.

---

## 🛠️ Struktur Arsitektur Proyek

```
uda-ai-trader/
├── .github/workflows/
│   └── android-build.yml           # GitHub Actions CI/CD Build APK
├── app/
│   ├── src/main/java/com/example/
│   │   ├── UdaTraderApp.kt          # Application Context & Service Locator
│   │   ├── MainActivity.kt          # UI Container & Lifecycle Permission Coordinator
│   │   ├── model/
│   │   │   └── TradingModels.kt     # Signal, Confirmation, Risk Data Classes
│   │   ├── data/
│   │   │   ├── db/                  # Room Database (SignalEntity, SignalDao, AppDatabase)
│   │   │   ├── api/                 # AiTraderEngine, GeminiPrompt, SignalValidator
│   │   │   ├── pref/                # AppSettings (SharedPreferences / Preferences)
│   │   │   └── repository/          # TradingRepository (Data Coordinator & Alerts)
│   │   ├── service/
│   │   │   ├── ScreenCaptureService.kt   # MediaProjection Foreground Service
│   │   │   └── FloatingWindowService.kt  # SYSTEM_ALERT_WINDOW Overlay Service
│   │   ├── viewmodel/
│   │   │   └── TraderViewModel.kt   # Central StateFlow ViewModel
│   │   └── ui/
│   │       ├── screens/
│   │       │   ├── CockpitScreen.kt        # Real-time Cockpit & Live Hero Card
│   │       │   ├── HistoryScreen.kt        # Local Database Signal Logs
│   │       │   ├── RiskCalculatorScreen.kt # Risk & Contract Lot Calculator
│   │       │   └── SettingsScreen.kt       # API, Backend & Model Configuration
│   │       └── theme/                      # Cyber Dark Fintech Material 3 Theme
│   └── AndroidManifest.xml          # Permissions & Service Declarations
├── backend/                         # Node.js Express Secure Gemini Proxy
│   ├── server.js
│   ├── package.json
│   ├── Dockerfile
│   └── README_BACKEND.md
└── README.md
```

---

## 📋 Izin Android yang Digunakan

1. `android.permission.INTERNET` & `ACCESS_NETWORK_STATE`: Komunikasi API ke Backend / Gemini.
2. `android.permission.SYSTEM_ALERT_WINDOW`: Menampilkan Floating Window di atas MetaTrader 5.
3. `android.permission.FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PROJECTION`: Menjalankan perekaman layar berkelanjutan dengan kepatuhan Android 14+.
4. `android.permission.POST_NOTIFICATIONS`: Notifikasi sistem foreground pada Android 13+.
5. `android.permission.VIBRATE`: Umpan balik haptik ketika sinyal terkonfirmasi terbentuk.

---

## 📱 Panduan Penggunaan dengan MetaTrader 5 (MT5)

1. **Jalankan Aplikasi UDA AI TRADER**.
2. Masuk ke tab **Pengaturan**:
   - Jika menggunakan server backend Anda, masukkan URL backend pada kolom yang tersedia.
   - Atau aktifkan **Direct Gemini API** dan masukkan API key Gemini Anda.
   - Tekan **UJI KONEKSI ENDPOINT** untuk memastikan status online.
3. Kembali ke tab **Cockpit**:
   - Tekan **BUKA FLOATING WINDOW DI ATAS MT5** (izinkan "Tampilan di atas aplikasi lain" di menu setelan ponsel jika pertama kali).
   - Tekan **MULAI ANALISIS** dan berikan izin perekaman layar pada dialog konfirmasi Android.
4. **Buka aplikasi MetaTrader 5**:
   - Floating Window akan tetap tampil di atas chart MT5.
   - Posisikan chart candlestick MT5 dengan kontras yang jelas (misal: Green on Black).
   - Geser Floating Window ke pojok yang nyaman.
   - Sinyal **BUY NOW**, **SELL NOW**, atau **WAIT** akan diperbarui secara otomatis sesuai interval yang dipilih!

---

## ⚙️ Kompilasi & Build APK

Untuk mengompilasi APK debug di komputer lokal:
```bash
./gradlew assembleDebug
```
Output APK akan dihasilkan pada:
`app/build/outputs/apk/debug/app-debug.apk`

---

## ⚠️ Disclaimer Trading & Risiko

Trading instrumen Forex, Komoditas, Indeks, dan Kripto memiliki tingkat risiko kerugian yang tinggi. Aplikasi UDA AI TRADER berfungsi sebagai asisten analisis teknikal berbantuan kecerdasan buatan, bukan penasihat keuangan atau sistem eksekusi otomatis. Selalu gunakan manajemen risiko yang ketat dan verifikasi spesifikasi kontrak broker sebelum melakukan transaksi.
