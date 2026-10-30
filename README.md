# UDA AI TRADER - Native Android Application

Aplikasi Android native institusional untuk analisis real-time chart **MetaTrader 5 (MT5)** menggunakan **Android MediaProjection API**, **SYSTEM_ALERT_WINDOW (Floating Window)**, dan **Google Gemini Vision AI Engine** langsung dari perangkat tanpa backend sendiri.

---

## 🚀 Fitur Utama

1. **Standalone Direct Gemini Vision AI (Tanpa Backend Sendiri)**
   - Mengirim screenshot chart MT5 langsung ke Google Generative Language REST API (`gemini-1.5-flash`, `gemini-2.0-flash`, `gemini-1.5-pro`).
   - Tidak memerlukan VPS, Node.js server, Cloud Functions, atau server perantara apapun.
   - Penanganan error komprehensif: deteksi offline, timeout jaringan, kuota rate limit (429), dan autentikasi (401/403).
   - Anti-Halusinasi: Menampilkan sinyal `WAIT` jika chart tidak terbaca atau kondisi market sideways tanpa konfirmasi. Tidak pernah mengarang harga Entry, SL, atau TP.

2. **Screen Capture Berkala (MediaProjection API)**
   - Mengambil tampilan layar MetaTrader 5 secara berkala (interval 1s, 2s, 3s, atau 5s).
   - Berjalan pada **Foreground Service** dengan notifikasi transparan sesuai standar Android 8 s/d Android 16 (`FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION`).
   - Kompresi JPEG dinamis (maksimum dimensi 1280px) untuk kecepatan analisis dan efisiensi kuota.

3. **Floating Window (HUD Overlay di Atas MetaTrader 5)**
   - Menggunakan izin `SYSTEM_ALERT_WINDOW`.
   - Mengambang langsung di atas MetaTrader 5:
     - 🟢 **BUY NOW**
     - 🔴 **SELL NOW**
     - 🟡 **WAIT**
     - Parameter: PAIR, TIMEFRAME, ENTRY, STOP LOSS, TAKE PROFIT, RISK REWARD.
   - Draggable (dapat digeser ke mana saja) dan Minimizable (dapat diciutkan ke pill ringkas agar tidak menutupi tombol MT5).
   - Tombol cepat **COPY** untuk menyalin parameter order ke clipboard.

4. **Sistem Sinyal Real-Time Cerdas**
   - Mendeteksi transisi arah (BUY $\leftrightarrow$ SELL).
   - Membedakan sinyal terkonfirmasi (`CONFIRMED`) vs pending (`PENDING`).
   - Mencegah duplikasi sinyal identik berulang.

5. **Manajemen Risiko & Kalkulator Lot**
   - Menghitung risiko modal berdasarkan persentase (0.5% - 5%).
   - Menghitung rasio Risk:Reward (1:1.5, 1:2, 1:3).
   - Menghitung estimasi ukuran lot berdasarkan instrumen kontrak (Forex Standard 100k, Gold XAUUSD 100oz, Crypto, Indeks).

6. **Penyimpanan Lokal Room Database**
   - Menyimpan seluruh riwayat sinyal secara offline di perangkat via Room SQLite.
   - Fitur filter BUY, SELL, WAIT, dan pembersihan riwayat.

7. **Konfigurasi API Key yang Fleksibel & Aman**
   - Pengguna dapat memasukkan Gemini API Key langsung melalui menu Pengaturan di aplikasi.
   - Mendukung injeksi melalui `.env` lokal atau GitHub Secrets (`GEMINI_API_KEY`) via Secrets Gradle Plugin (`BuildConfig.GEMINI_API_KEY`).
   - API Key tidak pernah dicetak di logcat atau ditampilkan secara terbuka di UI.

---

## 🛠️ Struktur Proyek

```
uda-ai-trader/
├── .github/workflows/
│   └── android-build.yml           # GitHub Actions CI/CD Build & Artifact Upload
├── app/
│   ├── src/main/java/com/example/
│   │   ├── UdaTraderApp.kt          # Application Context & Service Locator
│   │   ├── MainActivity.kt          # UI Container & Navigation Coordinator
│   │   ├── model/
│   │   │   └── TradingModels.kt     # Domain Signal & Risk Models
│   │   ├── data/
│   │   │   ├── db/                  # Local Room Database (SignalEntity, SignalDao, AppDatabase)
│   │   │   ├── api/                 # AiTraderEngine (Direct Gemini Vision), GeminiPrompt, SignalValidator
│   │   │   ├── pref/                # AppSettings (SharedPreferences & Secrets fallback)
│   │   │   └── repository/          # TradingRepository (Data Coordinator & Alerts)
│   │   ├── service/
│   │   │   ├── ScreenCaptureService.kt   # MediaProjection Foreground Service
│   │   │   └── FloatingWindowService.kt  # SYSTEM_ALERT_WINDOW Overlay Service
│   │   ├── viewmodel/
│   │   │   └── TraderViewModel.kt   # Jetpack StateFlow ViewModel
│   │   └── ui/
│   │       ├── screens/
│   │       │   ├── CockpitScreen.kt        # Real-time Cockpit & Live Telemetry
│   │       │   ├── HistoryScreen.kt        # Offline Database Signal Logs
│   │       │   ├── RiskCalculatorScreen.kt # Position Sizing & Lot Calculator
│   │       │   └── SettingsScreen.kt       # Direct Gemini API Configuration
│   │       └── theme/                      # Cyber Dark Fintech Material 3 Theme
│   └── AndroidManifest.xml          # Permissions & Service Declarations
├── gradle/wrapper/                  # Gradle 9.3.1 Wrapper & Jar
├── .env.example                     # Secrets template
└── README.md
```

---

## 📋 Izin Android yang Digunakan

1. `android.permission.INTERNET` & `ACCESS_NETWORK_STATE`: Komunikasi langsung ke Google Gemini API.
2. `android.permission.SYSTEM_ALERT_WINDOW`: Menampilkan Floating Window di atas MetaTrader 5.
3. `android.permission.FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PROJECTION`: Menjalankan perekaman layar berkelanjutan sesuai spesifikasi Android 14+.
4. `android.permission.POST_NOTIFICATIONS`: Notifikasi foreground service pada Android 13+.
5. `android.permission.VIBRATE`: Umpan balik haptik ketika sinyal terkonfirmasi terbentuk.

---

## 📱 Panduan Penggunaan dengan MetaTrader 5 (MT5)

1. **Jalankan Aplikasi UDA AI TRADER**.
2. Masuk ke tab **Pengaturan**:
   - Masukkan Google Gemini API Key Anda.
   - Pilih model (direkomendasikan: `gemini-1.5-flash`).
   - Tekan **UJI KONEKSI GEMINI API SEKARANG** untuk memverifikasi kuota dan koneksi.
3. Kembali ke tab **Cockpit**:
   - Tekan **BUKA FLOATING WINDOW DI ATAS MT5** (izinkan "Tampilan di atas aplikasi lain" pada setelan perangkat jika pertama kali).
   - Tekan **MULAI ANALISIS** dan izinkan perekaman layar pada dialog konfirmasi Android.
4. **Buka aplikasi MetaTrader 5**:
   - Floating Window akan tetap tampil di atas chart MT5.
   - Posisikan chart candlestick MT5 dengan kontras yang jelas.
   - Sinyal **BUY NOW**, **SELL NOW**, atau **WAIT** akan diperbarui secara otomatis.
   - Gunakan tombol **COPY** untuk menyalin level entry, SL, dan TP ke clipboard.

---

## ⚙️ Kompilasi & Build APK di GitHub Actions

Proyek ini telah dikonfigurasi agar dapat dibuild secara otomatis di **GitHub Actions**:
- Workflow: `.github/workflows/android-build.yml`
- Menggunakan JDK 21 (Temurin), Gradle 9.3.1 Wrapper, dan Android SDK 36.
- Menyiapkan keystore debug dan environment file secara otomatis di runner CI.
- Menghasilkan artifact APK Debug yang dapat diunduh langsung: `app/build/outputs/apk/debug/app-debug.apk`.

Untuk build manual di komputer lokal:
```bash
./gradlew assembleDebug
```

---

## 🔒 Peringatan Keamanan API Key

API Key disimpan secara lokal pada perangkat Anda atau disuntikkan saat kompilasi via `BuildConfig.GEMINI_API_KEY`. Perlu diperhatikan bahwa binary Android APK dapat didekompilasi dan secret di dalamnya dapat diekstrak oleh pihak yang memiliki file APK. Jangan membagikan file APK atau source code yang mengandung API Key aktif kepada publik.

---

## ⚠️ Disclaimer Risiko Trading

Trading instrumen Forex, Komoditas, Indeks, dan Kripto memiliki tingkat risiko kerugian yang tinggi. Aplikasi UDA AI TRADER berfungsi sebagai asisten analisis teknikal berbantuan kecerdasan buatan, bukan penasihat keuangan atau sistem eksekusi transaksi otomatis. Selalu gunakan manajemen risiko yang ketat.
