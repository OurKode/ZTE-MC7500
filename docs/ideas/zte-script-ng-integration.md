# Integrasi Fitur zte-script-ng.js ke Aplikasi ZTE MC7500 ODU Monitor

## Problem Statement
Bagaimana kita bisa menghadirkan kemampuan diagnostik telemetri mendalam dan kendali optimasi frekuensi radio (Band & Cell Locking) dari `zte-script-ng.js` ke dalam aplikasi Android tanpa risiko merusak konfigurasi jaringan atau memutuskan koneksi ODU secara permanen?

---

## Recommended Direction

Pendekatan yang dipilih adalah **Staged Safety Architecture** (Arsitektur Keselamatan Bertahap) yang memadukan integrasi telemetri mendalam yang aman (read-only) dengan kendali radio defensif berbasis profil operator terkurasi (*Curated Operator Profiles*):

1. **Tahap 1 — Telemetri Diagnostik Mendalam (Aman & Non-Destruktif)**:
   - Menambahkan pemantauan **Suhu CPU Router** (`zwrt_bsp.thermal` -> `get_cpu_temp`) dan **Info Perangkat/Uptime** (`zwrt_mc.device.manager` -> `get_device_info`).
   - Mengimplementasikan konverter matematis channel radio ke frekuensi nyata dalam satuan **MHz**:
     - 4G LTE: Rumus konversi EARFCN $\rightarrow$ Frekuensi downlink (MHz) berdasarkan 3GPP TS 36.101.
     - 5G NR: Rumus konversi ARFCN rasters $\rightarrow$ Frekuensi MHz berdasarkan 3GPP TS 38.101.
   - Menampilkan frekuensi MHz secara kontekstual di samping penamaan Band pada kartu sinyal utama (misal: `Band n40 • 2300.0 MHz`, `Band 3 • 1805.0 MHz`).

2. **Tahap 2 — Manajemen Frekuensi Defensif (Alat Radio & Band)**:
   - Ditempatkan pada tab terisolasi di `SettingsBottomSheet` bertajuk **"Alat Radio & Band"** agar terlindung dari sentuhan tidak sengaja.
   - **Curated Operator Profiles**: Menghapus risiko kesalahan bitmask manual dengan menyediakan tombol preset band operator Indonesia yang telah divalidasi:
     - **Telkomsel**: 4G (B1, B3, B8, B40) + 5G (n40)
     - **XL Axiata**: 4G (B1, B3, B8)
     - **Indosat Ooredoo Hutchison**: 4G (B1, B3, B8)
     - **Smartfren**: 4G (B40, B28)
     - **Reset / Auto**: Memulihkan mask penuh (`buildFullMask`) untuk mengizinkan semua band.
   - **One-Tap Current Cell Lock**:
     - Fitur Cell Locking (PCI + EARFCN) hanya mengambil parameter dari *sel aktif yang sedang terhubung dan terbukti berjalan baik* (tanpa input teks angka bebas).
     - Dilengkapi tombol darurat **"Buka Kunci Sel (Unlock)"** yang memanggil payload `(0, 0)` untuk 4G dan `(0, 0, 0)` untuk 5G.

---

## Key Assumptions Validated

- [x] **Kompatibilitas UBUS RPC**: Endpoint `zwrt_bsp.thermal` (`get_cpu_temp`) dan `zwrt_mc.device.manager` (`get_device_info`) terbukti sukses dieksekusi dalam batch call RPC dengan data aktual (`53°C`, `23j 23m`).
- [x] **Izin Bandlock pada Akun Admin**: Endpoint `nwinfo_set_gwl_bandlock`, `nwinfo_set_netselect`, `nwinfo_set_nrbandlock`, dan `nwinfo_lock_lte_cell` terintegrasi dengan validasi kredensial `Admin`.
- [x] **Responsiveness saat Band Switching**: Router ZTE MC7500 membutuhkan jeda waktu re-sinkronisasi; UI menerapkan indikator progres non-blocking dan modal konfirmasi pencegahan insiden.

---

## MVP Scope

### Yang Masuk dalam Lingkup (In-Scope):
- **Diagnostik Telemetri (Read-Only)**:
  - Pembacaan suhu CPU perangkat ODU (°C) di kartu status gateway.
  - Perhitungan matematis frekuensi downlink (MHz) dari EARFCN & ARFCN aktif.
  - Uptime router (format `X hari Y jam Z menit`).
  - Parsing detail Carrier Aggregation (CA) sekunder (SCC) beserta bandwidth kanal (MHz).
- **Manajemen Band & Radio (Control Layer)**:
  - Tab "Alat Radio & Band" di bottom sheet pengaturan.
  - Mode Jaringan (Bearer): Auto (5G/4G/3G), 5G NSA/SA, 4G Only.
  - Preset Band Operator Indonesia (Telkomsel, XL, Indosat, Smartfren) + Toggle per-Band resmi.
  - Tombol Darurat "Pulihkan Semua Band (Auto)".
  - "Kunci ke Sel Saat Ini" (One-Tap Lock Current Cell) & "Buka Kunci Sel".

---

## Not Doing (dan Alasan)

- **Manajemen Parameter Wi-Fi Internal (`setWifiTxPower`, `setWifiCountry`, `setWifiMaxClients`)**:
  - *Alasan*: ODU ZTE MC7500 adalah perangkat antena luar ruang (Outdoor Unit) yang fokus utamanya adalah penangkapan sinyal seluler selancar mungkin. Pengaturan Wi-Fi internal adalah ranah router indoor/mesh dan keluar dari fokus aplikasi.
- **Input Angka Bitmask / PCI Manual Bebas**:
  - *Alasan*: Menghindari risiko salah ketik angka PCI/EARFCN fiktif yang dapat membuat ODU *no-service* dan gagal terhubung ke menara seluler.
- **Eksekusi Raw JavaScript / Evaluator Script**:
  - *Alasan*: Seluruh logika dikonversi secara native ke bahasa **Kotlin** murni dengan typed data class, error handling, dan protokol Android yang aman.

---

## Roadmap Implementasi Teknis

```mermaid
flowchart TD
    subgraph Fase 1: Telemetri & Frekuensi (Non-Destruktif)
        A["1.1 DTO & RPC Tambahan: get_cpu_temp, get_device_info"] --> B["1.2 Modul Konversi EARFCN/ARFCN ke MHz (Pure Kotlin)"]
        B --> C["1.3 Integrasi Visual MHz & Suhu CPU ke Dashboard"]
    end

    subgraph Fase 2: Kendali Radio Terkurasi (Safety-First)
        C --> D["2.1 Engine Bitmask 4G & Format String 5G (Curated Presets)"]
        D --> E["2.2 RPC nwinfo_set_netselect & nwinfo_set_gwl_bandlock"]
        E --> F["2.3 One-Tap Cell Lock (Lock Current Cell & Unlock)"]
        F --> G["2.4 Tab 'Alat Radio & Band' di SettingsBottomSheet"]
    end
```
