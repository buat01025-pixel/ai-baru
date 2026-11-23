# AKSAA AI — Android

Aplikasi chat Android Kotlin + Jetpack Compose dengan endpoint AI online anonim LLMFaucet.

## Penting
- Tidak memerlukan API key pribadi atau akun penyedia AI.
- Memerlukan internet.
- Endpoint publik gratis dapat berubah, membatasi permintaan, antre, atau tidak tersedia.
- Percakapan disimpan lokal di SharedPreferences.
- Pesan dikirim ke penyedia layanan AI pihak ketiga. Jangan kirim password, OTP, atau data sensitif.
- Ini prototipe awal; untuk distribusi publik sebaiknya gunakan backend milik sendiri agar endpoint dapat diganti dan kebijakan privasi dapat dikendalikan.

## Build
Buka folder ini di Android Studio, biarkan Gradle sync, lalu pilih Build > Build Bundle(s) / APK(s) > Build APK(s).

Persyaratan: Android Studio dengan JDK 17 dan Android SDK 35.

## Catatan
Project ini belum menyertakan Gradle Wrapper. Android Studio akan menggunakan instalasi Gradle yang tersedia atau meminta konfigurasi Gradle.
