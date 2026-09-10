# FineLog Android

FineLog adalah aplikasi Android offline-first untuk catatan, tugas, kalender, dan sinkronisasi multi-perangkat.

## Status versi

Branch: `finelog-android`  
Package: `com.ronald.finelog`  
Minimum Android: Android 7.0 (API 24)  
Target SDK: 35  
Versi aplikasi: 1.14.0-beta

## Fitur yang sudah tersedia

- Catatan harian tersimpan lokal dengan SQLite.
- Tugas/checklist dengan tanggal jatuh tempo.
- Kalender untuk melihat tugas pada tanggal yang dipilih.
- Mode offline-first: aplikasi tetap dapat dipakai tanpa internet.
- WorkManager untuk sinkronisasi otomatis saat jaringan tersedia.
- Login/daftar akun Firebase Authentication (email + password).
- Sinkronisasi catatan dan tugas ke Cloud Firestore per akun pengguna.
- Konflik data dasar menggunakan `updatedAt` (last-write-wins).
- Navigasi Catatan, Tugas, Kalender, dan Profil.
- Build APK otomatis melalui GitHub Actions setiap push ke branch `finelog-android`.

## Mengaktifkan sinkronisasi cloud

Aplikasi tidak menyimpan kredensial Firebase di repository. Konfigurasi dimasukkan dari menu **Profil** agar source code tetap dapat dibagikan tanpa memasukkan kunci proyek ke Git.

1. Buat atau pilih project di Firebase Console.
2. Tambahkan aplikasi Android dengan package `com.ronald.finelog`.
3. Aktifkan **Authentication > Sign-in method > Email/Password**.
4. Buat database **Cloud Firestore**.
5. Terapkan aturan dari file `firebase/firestore.rules`.
6. Dari Firebase Project settings, ambil **Project ID**, **App ID**, dan **Web API Key**.
7. Buka FineLog > **Profil**, masukkan ketiga nilai tersebut lalu tekan **Simpan Konfigurasi Cloud**.
8. Daftar atau masuk menggunakan email dan password.
9. Tekan **Sinkronkan**. Sinkronisasi juga berjalan otomatis saat jaringan tersedia.

## Model data Firestore

Data setiap akun dipisahkan berdasarkan UID:

```text
users/{uid}/notes/{noteId}
users/{uid}/tasks/{taskId}
```

Aturan keamanan memastikan pengguna yang sudah login hanya dapat membaca dan menulis datanya sendiri.

## Build APK

GitHub Actions menjalankan `:app:assembleDebug` menggunakan Java 17, Android SDK 35, dan Gradle 8.7. Setelah workflow berhasil, APK tersedia sebagai artifact bernama `finelog-android-debug-apk`.

## Catatan pengembangan berikutnya

Tahap berikutnya: edit/hapus catatan, penghapusan tersinkron, notifikasi pengingat tugas, biometrik/PIN, penyempurnaan UI Material, backup/restore, dan build release bertanda tangan untuk distribusi.
