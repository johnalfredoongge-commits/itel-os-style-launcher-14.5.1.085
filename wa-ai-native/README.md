# WA AI Operator — itel A50

Aplikasi Android native ringan untuk operator WhatsApp Business API.

- Didesain untuk itel A50 / Android 14 Go Edition.
- minSdk 23, targetSdk 35.
- Sinkron percakapan melalui backend WA AI starter (`/api/chats`, `/api/send`).
- URL server dapat diubah dari ikon pengaturan di aplikasi.
- Mendukung HTTP LAN untuk pengujian dan HTTPS untuk penggunaan lintas jaringan.
- Tidak mencoba mengambil online/last seen/typing privat milik kontak WhatsApp.

## Setelah terpasang
Buka ikon ⚙ dan masukkan URL backend, misalnya `https://bot.domainanda.com` atau untuk LAN `http://192.168.1.10:8080`.
