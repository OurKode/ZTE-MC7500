# Material 3 & Material You Redesign

## Problem Statement
Bagaimana mentransformasi ZTE ODU Monitor menjadi aplikasi Android Material 3 / Material You modern (Dynamic Color, Light/Dark mode adaptif, komponen M3 native) tanpa mengorbankan keterbacaan metrik RF teknis (RSRP/SINR/Throughput) dan kestabilan komunikasi gateway.

## Recommended Direction
Melakukan modernisasi menyeluruh secara bertahap:
1. **Dynamic Theming Core**: Mengaktifkan `dynamicLightColorScheme()` dan `dynamicDarkColorScheme()` di Android 12+ (API 31+), dengan fallback palet M3 kustom (Light & Dark) untuk Android 8.0–11 (API 26–30).
2. **Semantic Signal Color Matrix**: Mengatur warna sinyal (Excellent = Emerald, Fair = Amber, Poor = Coral) agar memiliki varian on-surface / container yang kontras tinggi dan memenuhi WCAG AA di mode terang maupun gelap.
3. **M3 Modular Navigation**: Memecah `DashboardScreen.kt` (2.273 baris) menjadi `Scaffold` + `NavigationBar` 3 tab:
   - **Monitor**: Live telemetry, dual gauge (RSRP/SINR), throughput chart, info WAN/SIM.
   - **Radio Control**: Bearer switch (`SegmentedButton`), band lock (`FilterChip`), cell lock.
   - **Settings & Stats**: Kuota, notifikasi, credentials router, widget preview.
4. **Widget Harmony**: Memperbarui `OduCompactWidget` dan `OduDetailedWidget` menggunakan `GlanceTheme.colors`.

## Key Assumptions to Validate
- [ ] Metrik sinyal RF tetap terbaca jelas di bawah terik matahari saat Light Mode aktif.
- [ ] Pemecahan `DashboardScreen.kt` menjadi 3 tab tidak merusak polling background dan state sharing `DashboardViewModel`.
- [ ] Dynamic Color pada widget Glance tidak menyebabkan render delay pada homescreen launcher pihak ketiga.

## MVP Scope (Tahap 1)
- **In Scope**:
  - Konfigurasi `Theme.kt` (Dynamic Color + Light & Dark ColorScheme).
  - Penyesuaian `Color.kt` untuk token semantic signal status.
  - Implementasi `Scaffold` dengan `TopAppBar` dan `NavigationBar` M3 dasar.
  - Migrasi kartu live telemetry utama ke `ElevatedCard` dan `OutlinedCard`.
- **Out of Scope (MVP)**:
  - Custom animation transisi antar tab (gunakan default M3 fade/slide).
  - Redesain total canvas grafik throughput (cukup sesuaikan warna garis dengan `colorScheme.primary`).

## Not Doing (and Why)
- **Menghapus Dark Mode murni**: Pengguna monitor jaringan di malam hari membutuhkan kontras minim silau; Dark Mode tetap menjadi prioritas kelas satu.
- **Membuat ulang logic polling ubus JSON-RPC**: Fokus hanya pada lapisan UI; data layer & repository tetap utuh.
- **Pustaka UI pihak ketiga tambahan**: Menggunakan murni pustaka resmi `androidx.compose.material3`.

## Open Questions
- Apakah tab aktif terakhir perlu disimpan di `DataStore` / `SharedPreferences` agar saat app dibuka kembali langsung ke tab tersebut?
