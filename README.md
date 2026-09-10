# FineLog Android

FineLog adalah aplikasi Android untuk mencatat aktivitas harian, tugas, dan catatan pribadi dengan dukungan offline-first serta fondasi sinkronisasi multi-perangkat.

## Status branch
Branch pengembangan: `finelog-android`

## Fitur tahap awal
- Catatan lokal berbasis SQLite
- UI responsif untuk berbagai ukuran layar Android
- Tombol sinkronisasi menggunakan WorkManager
- Dukungan Android 7.0+ (`minSdk 24`)
- Target Android SDK 35
- Build APK otomatis melalui GitHub Actions

## Sinkronisasi multi-perangkat
Struktur worker sinkronisasi sudah tersedia. Agar data benar-benar tersinkron antar-smartphone, tahap berikutnya adalah menghubungkan backend cloud, autentikasi pengguna, dan API sinkronisasi.

## Build
Jalankan workflow **Build FineLog APK** atau gunakan Gradle:

```bash
gradle :app:assembleDebug
```

APK debug akan tersedia di:

`app/build/outputs/apk/debug/app-debug.apk`
