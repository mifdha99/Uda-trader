# UDA AI TRADER - Backend Deployment Guide

Backend ini berfungsi sebagai proxy terisolasi dan aman untuk menghubungkan aplikasi Android **UDA AI TRADER** dengan Google Gemini Vision API tanpa menanamkan API Key ke dalam paket APK.

---

## 1. Persiapan Environment

Dapatkan API Key Google Gemini dari [Google AI Studio](https://aistudio.google.com/).

Buat file `.env` di folder `backend/`:
```env
PORT=8080
GEMINI_API_KEY=AIzaSyYourGeminiApiKeyHere
GEMINI_MODEL=gemini-2.5-flash
```

---

## 2. Menjalankan Secara Lokal

```bash
cd backend
npm install
npm start
```
Server akan aktif di `http://localhost:8080`.
Endpoint uji kesehatan: `GET http://localhost:8080/health`.

---

## 3. Deployment Gratis / Cloud (Pilihan)

### Opsi A: Google Cloud Run (Direkomendasikan)
1. Install Google Cloud SDK: `gcloud auth login`
2. Jalankan perintah deploy langsung dari folder `backend/`:
   ```bash
   gcloud run deploy uda-ai-trader-backend \
     --source . \
     --region asia-southeast1 \
     --allow-unauthenticated \
     --set-env-vars GEMINI_API_KEY="AIzaSy..."
   ```
3. Salin URL output (misal: `https://uda-ai-trader-backend-xxx.run.app`) dan masukkan ke menu **Pengaturan** di aplikasi UDA AI TRADER.

### Opsi B: Render.com (Web Service)
1. Hubungkan repository GitHub Anda ke Render.com.
2. Pilih folder root: `backend`.
3. Build Command: `npm install`
4. Start Command: `node server.js`
5. Tambahkan Environment Variable: `GEMINI_API_KEY`.

---

## 4. Konfigurasi Pada Aplikasi Android

1. Buka aplikasi **UDA AI TRADER** di ponsel Anda.
2. Masuk ke tab **Pengaturan**.
3. Pastikan mode **UDA Backend** terpilih.
4. Masukkan URL server Anda: `https://your-backend-url.run.app/api/analyze`.
5. Tekan **UJI KONEKSI ENDPOINT** untuk memverifikasi respon server.
