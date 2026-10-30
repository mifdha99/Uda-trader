# UDA AI TRADER - Native Android Application

Aplikasi Android native institusional untuk analisis real-time chart **MetaTrader 5 (MT5)** menggunakan **Android MediaProjection API**, **SYSTEM_ALERT_WINDOW (Floating Window)**, dan **Universal Signal Engine** yang berjalan 100% secara lokal pada perangkat tanpa Gemini AI dan tanpa backend.

---

## 🚀 Fitur Utama

1. **Universal Signal Engine (100% Lokal di Android)**
   - Algoritma analisis teknikal kuantitatif on-device:
     - **EMA 9 & EMA 21**: Menentukan arah tren utama dan deteksi Golden / Death Cross.
     - **RSI 14 (Wilder Smoothed)**: Membaca momentum pasar dan memfilter overbought (>70) serta oversold (<30).
     - **ATR 14 (Average True Range)**: Mengukur volatilitas dinamis untuk penentuan Stop Loss dan Take Profit.
     - **Support & Resistance**: Deteksi zona pantulan dari swing high/low pivots.
     - **Price Action & Candle Confirmation**: Pengenalan pola Hammer, Shooting Star, Bullish/Bearish Engulfing, dan Strong Marubozu.
     - **Deterministic Confidence Scoring**: Skor keyakinan terkalibrasi (0% - 100%) berdasarkan konfluensi multi-indikator terukur (bukan angka acak).
     - **Sinyal**: BUY NOW, SELL NOW, atau WAIT (jika kondisi pasar tidak memenuhi kriteria konfluensi $\ge 70\%$). Tidak pernah mengarang harga.

2. **Screen Capture Real-Time MT5 (MediaProjection API)**
   - Membaca chart MetaTrader 5 secara otomatis setiap 1 detik melalui Android Foreground Service (`FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION`).
   - Modul **Visual Chart Recognition**:
     - Memindai candlestick bullish (hijau) dan bearish (merah).
     - Menentukan posisi Open, High, Low, Close, wick, dan body candle.
     - Memetakan skala harga sumbu vertikal.
     - Deteksi validitas chart: jika layar bukan chart MT5 (misal menu, loading, blank), sistem mengeluarkan status `WAIT: DATA CHART TIDAK VALID`.

3. **Pilihan Timeframe Standar MT5**
   - 9 pilihan timeframe standar: **M1, M5, M15, M30, H1, H4, D1, W1, MN1**.
   - Pengguna dapat memilih langsung dari UI Cockpit maupun Pengaturan.
   - Pilihan timeframe tersimpan otomatis di preferensi lokal.
   - Peringatan otomatis jika timeframe aplikasi berbeda dengan tampilan MT5.

4. **Floating Window (HUD Overlay di Atas MetaTrader 5)**
   - Mengambang langsung di atas MetaTrader 5 (`SYSTEM_ALERT_WINDOW`).
   - Menampilkan:
     - **TIMEFRAME**: M1, M5, dll.
     - **SIGNAL**: BUY NOW / SELL NOW / WAIT
     - **CONFIDENCE**: XX%
     - **ENTRY**: Harga Entry
     - **STOP LOSS**: Harga SL
     - **TAKE PROFIT**: Harga TP
     - **RISK REWARD**: 1:2 atau 1:3
     - **WAKTU PEMBARUAN**: Waktu analisis terakhir
   - Tombol cepat: **COPY** (menyalin order ke clipboard), **MINIMIZE** (menciutkan ke pill ringkas), dan **CLOSE**.
   - Dapat digeser bebas (draggable) dengan pembatas layar (tidak keluar dari batas tampilan) dan menyimpan posisi terakhir.

5. **Kalkulator Risiko & Manajemen Lot**
   - Menghitung risiko akun berdasarkan persentase modal (0.5% - 5%).
   - Menghitung ukuran lot posisi sesuai instrumen (Forex Standard 100k, Gold XAUUSD, Crypto, Indeks).

6. **Penyimpanan Offline Room Database**
   - Riwayat sinyal disimpan di SQLite lokal via Room.
   - Fitur filter dan pembersihan riwayat.

7. **Bebas API Key & Tanpa Server Eksternal**
   - Tidak memerlukan Gemini API Key, Firebase AI Logic, Node.js, VPS, atau cloud service apapun.
   - Bebas error "API KEY BELUM DI SET".

---

## 🛠️ Struktur Proyek

```
uda-ai-trader/
├── .github/workflows/
│   └── android-build.yml           # GitHub Actions CI/CD Build & Artifact Upload
├── app/
│   ├── src/main/java/com/example/
│   │   ├── UdaTraderApp.kt          # Application Context
│   │   ├── MainActivity.kt          # Navigation & Lifecycle Coordinator
│   │   ├── model/
│   │   │   └── TradingModels.kt     # Signal, Timeframe, Metrics & Risk Models
│   │   ├── data/
│   │   │   ├── db/                  # Local Room Database (SignalEntity, SignalDao, AppDatabase)
│   │   │   ├── engine/              # Universal Signal Engine & Visual Chart Scanner
│   │   │   │   ├── Candlestick.kt
│   │   │   │   ├── TechnicalIndicators.kt (EMA, RSI, ATR, S&R)
│   │   │   │   ├── VisualChartScanner.kt (CV Chart Recognition)
│   │   │   │   └── UniversalSignalEngine.kt (Master Analysis Brain)
│   │   │   ├── pref/                # AppSettings (Local SharedPreferences)
│   │   │   └── repository/          # TradingRepository
│   │   ├── service/
│   │   │   ├── ScreenCaptureService.kt   # MediaProjection Foreground Service
│   │   │   └── FloatingWindowService.kt  # SYSTEM_ALERT_WINDOW Overlay Service
│   │   ├── viewmodel/
│   │   │   └── TraderViewModel.kt   # Jetpack StateFlow ViewModel
│   │   └── ui/
│   │       ├── screens/
│   │       │   ├── CockpitScreen.kt        # Real-time Cockpit & Timeframe Selector
│   │       │   ├── HistoryScreen.kt        # Room Database Signal Logs
│   │       │   ├── RiskCalculatorScreen.kt # Position Sizing & Lot Calculator
│   │       │   └── SettingsScreen.kt       # Universal Engine Technical Settings
│   │       └── theme/                      # Cyber Dark Fintech Material 3 Theme
│   └── AndroidManifest.xml          # Permissions & Service Declarations
├── gradle/wrapper/                  # Gradle 9.3.1 Wrapper & Jar
└── README.md
```

---

## 📱 Panduan Penggunaan dengan MetaTrader 5 (MT5)

1. **Jalankan Aplikasi UDA AI TRADER**.
2. **Pilih Timeframe** pada baris selector di atas (M1, M5, M15, M30, H1, H4, D1, W1, MN1) sesuai chart MT5 yang ingin dianalisis.
3. Tekan **BUKA FLOATING WINDOW DI ATAS MT5** (izinkan izin overlay Android jika baru pertama kali).
4. Tekan **MULAI PEMANTAUAN** dan berikan izin perekaman layar.
5. **Buka aplikasi MetaTrader 5**:
   - Floating Window akan tetap tampil di atas chart MT5.
   - Universal Signal Engine membaca chart secara real-time setiap 1 detik.
   - Sinyal **BUY NOW**, **SELL NOW**, atau **WAIT** beserta harga Entry, SL, TP, dan Confidence diperbarui secara otomatis.
   - Tekan tombol **COPY** pada Floating Window untuk menyalin parameter order ke clipboard.

---

## ⚙️ Kompilasi & Build APK di GitHub Actions

Workflow `.github/workflows/android-build.yml` otomatis membangun APK tanpa membutuhkan API key atau file rahasia:
- Menggunakan JDK 21 (Temurin) dan Gradle 9.3.1.
- Menghasilkan artifact APK Debug:
  `app/build/outputs/apk/debug/app-debug.apk`
- Dapat diunduh langsung dari tab **Actions** di GitHub.

Untuk build lokal:
```bash
./gradlew assembleDebug
```
