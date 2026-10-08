# ZTE MC7500 ODU Signal Monitor & Widget

A high-performance Android application & Homescreen Widget (Jetpack Glance) built for monitoring ZTE MC7500 4G/5G Outdoor Units (ODUs) in real-time.

## Features
- **JSON-RPC Telemetry Engine**: Polling unauthenticated maupun terotentikasi langsung ke gateway ZTE MC7500 via `/ubus/`.
- **Authenticated Gateway Telemetry**: Kecepatan unduh/unggah real-time (Mbps), grafik tren throughput, kuota hari ini dan bulan ini, WAN IP, nomor SIM, suhu prosesor, dan uptime gateway.
- **Alat Kendali Radio & Band**: Pilihan mode jaringan (Auto 5G/4G, 5G SA Saja, 4G LTE Saja), preset operator Indonesia (Telkomsel, XL, Indosat, Smartfren), penguncian pita frekuensi LTE/NR, dan One-Tap Cell Locking dengan dialog konfirmasi keamanan.
- **Notifikasi Sistem Real-Time**: Notifikasi otomatis saat pita frekuensi berubah, WAN IP diperbarui, atau gateway ODU terputus.
- **Homescreen Widgets (Jetpack Glance)**:
  - `OduCompactWidget` (2x2): Indikator utama RSRP dan SINR 5G.
  - `OduDetailedWidget` (4x2): Metrik ganda 5G & 4G, informasi pita frekuensi, dan waktu pembaruan.
- **Mode Demo**: Simulator offline bawaan untuk pengujian antarmuka tanpa koneksi fisik ke perangkat.

## Dokumentasi API
Spesifikasi lengkap protokol JSON-RPC ubus internal ZTE MC7500 (metode hashing sandi, batch query, format bitmask penguncian pita frekuensi, dan rumus konversi 3GPP) didokumentasikan di [docs/API.md](docs/API.md).

## Project Structure
- `app/src/main/java/com/example/odumonitor/data/`: Data model (`OduModels.kt`), HTTP service (`OduApiService.kt`), repository (`OduRepository.kt`).
- `app/src/main/java/com/example/odumonitor/ui/`: Jetpack Compose UI (`DashboardScreen.kt`, `DashboardViewModel.kt`, Tema).
- `app/src/main/java/com/example/odumonitor/widget/`: Glance widgets (`OduCompactWidget.kt`, `OduDetailedWidget.kt`).
- `app/src/main/java/com/example/odumonitor/worker/`: Background sync worker (`OduSyncWorker.kt`).
- `docs/API.md`: Spesifikasi protokol dan endpoint ubus ZTE MC7500.
