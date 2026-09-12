# A50 Font Studio

Aplikasi Android ringan untuk Itel A50 (Android 14 Go Edition) yang menyediakan pilihan gaya font, pratinjau, impor berkas TTF/OTF, serta pintasan aman ke XTheme/Tema Itel dan Pengaturan Tampilan.

> Android tidak mengizinkan aplikasi biasa mengganti `/system/fonts` tanpa izin sistem atau root. Aplikasi ini tidak melakukan modifikasi berisiko. Perubahan font seluruh sistem harus dilakukan lewat XTheme/Tema Itel bila fitur tersebut tersedia di firmware perangkat.

## Build APK otomatis di GitHub

Workflow `.github/workflows/build-apk.yml` berjalan otomatis saat ada perubahan pada branch `main`, saat Pull Request dibuat, atau ketika tombol **Run workflow** ditekan.

1. Unggah seluruh isi proyek ini ke repositori GitHub.
2. Buka tab **Actions**.
3. Pilih **Build APK Otomatis**.
4. Tekan **Run workflow** bila build belum berjalan otomatis.
5. Setelah tanda centang hijau muncul, buka proses build.
6. Di bagian **Artifacts**, download **A50-Font-Studio-APK**.
7. Ekstrak ZIP artifact lalu instal `A50-Font-Studio.apk` pada Itel A50.

Untuk membuat halaman Release dengan APK, buat tag versi dari GitHub, misalnya `v1.0.0`. Workflow akan menambahkan APK ke Release tersebut.

## Fitur

- Deteksi merek, model perangkat, dan versi Android.
- 10 gaya font sistem untuk pratinjau.
- Pengaturan ukuran pratinjau.
- Impor font TTF/OTF dari penyimpanan.
- Tombol membuka XTheme/Tema Itel jika tersedia.
- Fallback ke Pengaturan Tampilan Android.
- Tanpa root, tanpa izin internet, dan tanpa perubahan file sistem.

## Kompatibilitas

- Minimum Android 6 (API 23).
- Target Android 15 / API 35.
- Dirancang dan diuji secara konfigurasi untuk layar ponsel, termasuk Itel A50.
- Arsitektur CPU independen karena aplikasi hanya memakai Java/Android SDK.

## Build lokal

Buka folder proyek di Android Studio dengan JDK 17, tunggu Gradle Sync, lalu pilih **Build → Build APK(s)**.
