# ZTE MC7500 ODU Signal Monitor & Widget

Aplikasi Android dan widget beranda (Jetpack Glance) untuk memantau dan mengontrol gateway Outdoor Unit (ODU) ZTE MC7500 secara langsung.

Versi saat ini: **v1.2.0**

---

## Fitur Utama

- **Telemetri Gateway Terotentikasi**: Memantau kecepatan unduh dan unggah seketika (Mbps), grafik tren throughput berkala, pemakaian kuota harian dan bulanan, WAN IP publik, nomor telepon kartu SIM, suhu prosesor, dan uptime gateway.
- **Engine JSON-RPC ubus**: Polling data efisien via protokol OpenWrt ubus dalam satu batch request HTTP untuk menghemat beban CPU router.
- **Alat Kendali Radio dan Band**:
  - Pilihan mode jaringan: Otomatis (5G NSA/SA & 4G), 5G SA Saja, atau 4G LTE Saja.
  - Preset operator seluler Indonesia: Telkomsel, XL Axiata, Indosat Ooredoo Hutchison, dan Smartfren.
  - Penguncian pita frekuensi (Band Locking) untuk 4G LTE dan 5G NR.
  - Penguncian sel pemancar (Cell Locking) berbasis PCI dan frekuensi (EARFCN/ARFCN) dengan dialog konfirmasi keamanan.
- **Notifikasi Sistem Real-Time**: Pemberitahuan otomatis ketika pita frekuensi berpindah, alamat WAN IP berganti, atau gateway ODU offline.
- **Widget Beranda (Jetpack Glance)**:
  - `OduCompactWidget` (2x2): Indikator ringkas RSRP dan SINR 5G.
  - `OduDetailedWidget` (4x2): Panel metrik ganda 5G & 4G, informasi pita frekuensi, dan waktu pembaruan.
- **Mode Demo**: Simulator offline untuk menguji seluruh antarmuka visual tanpa koneksi fisik ke perangkat.

---

## Sistem Versioning Otomatis

Aplikasi menggunakan skema Semantic Versioning (`MAJOR.MINOR.PATCH`) yang dikombinasikan dengan riwayat Git:

- **Konfigurasi Versi**: Diatur dalam file [version.properties](version.properties).
- **Penomoran Build Otomatis (`versionCode`)**: Dihitung secara dinamis dari jumlah commit Git (`git rev-list --count HEAD`). Setiap kali ada commit baru di repositori, nomor build (`versionCode`) otomatis naik tanpa perlu pengubahan manual.
- **Label Versi (`versionName`)**: Menggabungkan nilai `VERSION_MAJOR.VERSION_MINOR.VERSION_PATCH` dari `version.properties`.

### Perintah Gradle untuk Versioning

Untuk menaikkan nomor versi sebelum merilis pembaruan:

```bash
# Menampilkan versi aplikasi saat ini dan commit hash
./gradlew printVersion

# Menaikkan versi patch (contoh: 1.2.0 -> 1.2.1)
./gradlew bumpPatch

# Menaikkan versi minor dan mereset patch (contoh: 1.2.1 -> 1.3.0)
./gradlew bumpMinor

# Menaikkan versi major dan mereset minor serta patch (contoh: 1.3.0 -> 2.0.0)
./gradlew bumpMajor
```

---

## Build dan Rilis APK

### Persyaratan Lingkungan
- JDK 17
- Android SDK dengan platform API 34 (Build Tools 34.0.0+)
- Perangkat Android minimal versi 8.0 (API level 26)

### Mengompilasi APK Release

Jalankan perintah berikut pada terminal:

```bash
# Windows PowerShell
.\gradlew assembleRelease

# Linux / macOS
./gradlew assembleRelease
```

File APK siap pakai akan dihasilkan pada:
```
app/build/outputs/apk/release/app-release.apk
```

Skrip Gradle telah dikonfigurasi dengan penandatanganan rilis bawaan (fallback otomatis ke debug keystore lokal jika `release.keystore` belum disediakan), sehingga APK hasil rilis dapat langsung diinstal ke perangkat tanpa error sertifikat.

### Instalasi via ADB

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## Dokumentasi API Internal

Spesifikasi teknis protokol OpenWrt ubus JSON-RPC pada ZTE MC7500 didokumentasikan secara rinci di [docs/API.md](docs/API.md). Dokumen ini mencakup:
- Header HTTP wajib dan endpoint gateway
- Algoritma hashing kata sandi ganda (double-SHA256 dengan salt)
- Format JSON batch request untuk telemetri
- Perhitungan bitmask desimal untuk penguncian pita frekuensi 4G
- Rumus konversi standar 3GPP (TS 36.101 dan TS 38.104) dari kanal ke frekuensi MHz

---

## Struktur Direktori

```
ZTE-MC7500/
├── app/
│   ├── src/main/java/com/example/odumonitor/
│   │   ├── data/          # Model data, API service OkHttp, dan repositori
│   │   ├── receiver/      # Broadcast receiver untuk sistem dan widget
│   │   ├── ui/            # Antarmuka Jetpack Compose (Dashboard, Theme)
│   │   ├── util/          # Konverter frekuensi 3GPP dan helper format
│   │   ├── widget/        # Implementasi widget Glance (Compact & Detailed)
│   │   └── worker/        # WorkManager background sync
│   └── build.gradle.kts   # Konfigurasi modul aplikasi dan skrip rilis
├── docs/
│   └── API.md             # Dokumentasi lengkap protokol ubus JSON-RPC
├── version.properties     # Konfigurasi versi semantik (Major, Minor, Patch)
└── README.md              # Dokumentasi utama proyek
```
