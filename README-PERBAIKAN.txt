PERBAIKAN V2 — PATH EKSTRAKSI GRADLE

Ganti gradlew dan gradlew.bat di root repository dengan versi dari paket ini.
Ganti juga .github/workflows/android-build.yml jika workflow sebelumnya belum sama.

Penyebab error sebelumnya:
Nama arsip adalah gradle-9.3.1-bin.zip, tetapi nama folder hasil ekstraksi di dalam ZIP adalah gradle-9.3.1. Launcher lama keliru menganggap foldernya bernama gradle-9.3.1-bin.

Versi ini memisahkan nama arsip dan nama folder hasil ekstraksi. File gradle-wrapper.properties tetap dipakai apa adanya; distributionUrl saat ini menunjuk Gradle 9.3.1.

Langkah:
1. Ekstrak ZIP ini.
2. Upload/ganti gradlew dan gradlew.bat di root repository.
3. Ganti .github/workflows/android-build.yml.
4. Commit ke main.
5. Jalankan ulang GitHub Actions.

Jika langkah Verify Gradle lolos tetapi assembleDebug gagal, kirim log mulai dari baris FAILURE: atau What went wrong. Itu berarti masalah berikutnya sudah berada pada konfigurasi/dependensi/kode proyek, bukan launcher Gradle.
