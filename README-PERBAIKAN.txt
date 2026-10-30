PERBAIKAN BUILD UDA AI TRADER

Isi ZIP:
- gradlew: launcher Gradle untuk Linux/GitHub Actions.
- gradlew.bat: launcher untuk Windows.
- .github/workflows/android-build.yml: workflow build APK.

PENTING:
Repository saat diperiksa sudah memiliki gradle/wrapper/gradle-wrapper.properties dengan:
distributionUrl=https\\://services.gradle.org/distributions/gradle-9.3.1-bin.zip

Namun gradlew dan gradle-wrapper.jar tidak ada. Paket ini memakai launcher pengunduh Gradle sederhana sehingga workflow tidak membutuhkan gradle-wrapper.jar. Ini solusi sementara yang valid untuk menjalankan build, bukan Gradle Wrapper resmi yang dihasilkan oleh task `gradle wrapper`.

CARA MEMASANG:
1. Ekstrak ZIP.
2. Upload file gradlew ke root repository (sejajar dengan settings.gradle.kts).
3. Upload gradlew.bat ke root repository.
4. Ganti file .github/workflows/android-build.yml dengan file dari ZIP.
5. Jangan menghapus atau mengubah gradle/wrapper/gradle-wrapper.properties.
6. Commit ke branch main.
7. Buka tab Actions di GitHub dan jalankan workflow jika belum berjalan.

Setelah workflow berhasil, unduh artifact bernama UDA-AI-TRADER-debug-apk.

Catatan: paket ini memperbaiki kegagalan karena file `gradlew` hilang. Belum menjamin keseluruhan proyek pasti berhasil dikompilasi; error dependency/kode berikutnya perlu ditangani dari log Actions.
