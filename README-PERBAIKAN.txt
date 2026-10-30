PERBAIKAN V3

Error V2 berasal dari ekspresi sed kedua yang salah sintaks. V3 menghapus ekspresi tersebut. Launcher hanya mengubah escape `\:` menjadi `:` pada distributionUrl, lalu menggunakan Gradle 9.3.1 dan folder hasil ekstraksi `gradle-9.3.1`.

Ganti file berikut di repository:
- gradlew (root)
- gradlew.bat (root)
- .github/workflows/android-build.yml

Jangan ubah gradle/wrapper/gradle-wrapper.properties. Commit perubahan dan jalankan ulang GitHub Actions.

Jika Verify Gradle lolos dan build selanjutnya gagal, kirim bagian log `FAILURE:` dan `What went wrong:`.
