# Dokumentasi API ZTE MC7500 (ubus JSON-RPC)

Dokumentasi teknis protokol internal OpenWrt ubus JSON-RPC pada perangkat Outdoor Unit (ODU) ZTE MC7500. Protokol ini digunakan aplikasi untuk memantau sinyal seluler, kecepatan jaringan, metrik hardware, serta mengatur penguncian pita frekuensi (band locking) dan sel pemancar (cell locking).

---

## 1. Spesifikasi Protokol Dasar

Komunikasi dengan gateway ZTE MC7500 menggunakan JSON-RPC 2.0 melalui HTTP atau HTTPS POST.

- **Endpoint URL:** `http://<host>/ubus/?t=<timestamp_ms>` atau `https://<host>/ubus/?t=<timestamp_ms>`
- **IP Default:** `192.168.254.1`
- **Metode HTTP:** `POST`
- **Format Payload:** JSON-RPC 2.0 (dapat berupa objek tunggal atau array batch)

### Header HTTP Wajib

Setiap request ke endpoint `/ubus/` harus menyertakan header berikut:

```http
Content-Type: application/json; charset=utf-8
Accept: application/json, text/javascript, */*; q=0.01
Origin: http://<host>
Referer: http://<host>/
X-Requested-With: XMLHttpRequest
Z-Mode: 0
Z-Tag: <tag_method>
```

Keterangan header:
- `Z-Mode`: Bernilai `0` untuk pemanggilan RPC standar, atau `1` untuk transaksi terenkripsi sertifikat web.
- `Z-Tag`: Identifier pemanggilan yang biasanya disamakan dengan nama method target (contoh: `nwinfo_get_netinfo`, `web_login_info`, `router_get_status`).

---

## 2. Autentikasi dan Manajemen Sesi

Akses ke data router sensitif (IP publik, penggunaan kuota, pengaturan pita frekuensi) membutuhkan token sesi aktif. Jika belum login, parameter token diisi dengan 32 karakter nol (`00000000000000000000000000000000`).

### Tahap 1: Mengambil Garam Acak (Salt)

Sebelum menghitung hash kata sandi, aplikasi meminta salt acak dari router:

- **Service:** `zwrt_web`
- **Method:** `web_login_info`
- **Z-Tag:** `web_login_info`

Request:
```json
[
  {
    "jsonrpc": "2.0",
    "id": 1,
    "method": "call",
    "params": [
      "00000000000000000000000000000000",
      "zwrt_web",
      "web_login_info",
      {}
    ]
  }
]
```

Respons:
```json
[
  {
    "jsonrpc": "2.0",
    "id": 1,
    "result": [
      0,
      {
        "zte_web_sault": "98765432",
        "login_fail_num": 0,
        "login_fail_lock_lefttime": 0
      }
    ]
  }
]
```

### Tahap 2: Algoritma Hashing Kata Sandi

ZTE MC7500 menggunakan skema double-SHA256 dengan format heksadesimal huruf kapital:

$$\text{Hash} = \text{SHA256}(\text{SHA256}(\text{Password}).\text{toUpperCase}() + \text{Salt}).\text{toUpperCase}()$$

Contoh implementasi Kotlin:
```kotlin
fun hashPassword(password: String, salt: String): String {
    val firstPass = sha256Hex(password).uppercase()
    return sha256Hex(firstPass + salt).uppercase()
}
```

### Tahap 3: Login Pengguna

- **Service:** `zwrt_web`
- **Method:** `web_login`
- **Z-Tag:** `web_login`

Request:
```json
[
  {
    "jsonrpc": "2.0",
    "id": 2,
    "method": "call",
    "params": [
      "00000000000000000000000000000000",
      "zwrt_web",
      "web_login",
      {
        "username": "Admin",
        "password": "<HASIL_HASH>"
      }
    ]
  }
]
```

Respons:
```json
[
  {
    "jsonrpc": "2.0",
    "id": 2,
    "result": [
      0,
      {
        "result": 0,
        "ubus_rpc_session": "9a56cf95e2e3d6800599aaecc87ea6bd",
        "timeout": 300
      }
    ]
  }
]
```

- `result = 0`: Berhasil. Nilai `ubus_rpc_session` disimpan sebagai token untuk request selanjutnya.
- `result != 0`: Autentikasi gagal (sandi salah atau akun terkunci).
- Kode error `-32002`: Sesi kedaluwarsa. Klien harus mengulang alur login dari Tahap 1.

---

## 3. Telemetri dan Status Jaringan

### Netinfo Seluler (Tanpa Autentikasi)

Dapat diakses menggunakan token nol atau token login.

- **Service:** `zte_nwinfo_api`
- **Method:** `nwinfo_get_netinfo`
- **Z-Tag:** `nwinfo_get_netinfo`

Request:
```json
[
  {
    "jsonrpc": "2.0",
    "id": 1,
    "method": "call",
    "params": [
      "00000000000000000000000000000000",
      "zte_nwinfo_api",
      "nwinfo_get_netinfo",
      {}
    ]
  }
]
```

Field utama dalam respons:
- `network_type`: Tipe jaringan radio aktif (`ENDC`, `LTE`, `NR5G_SA`, dll.).
- `network_provider`: Nama operator seluler terdeteksi.
- `signal_bar`: Skala bar sinyal (0 sampai 5).
- `lte_rsrp`: Nilai RSRP 4G LTE dalam dBm (contoh: `-95`).
- `lte_rsrq`: Nilai RSRQ 4G LTE dalam dB (contoh: `-12`).
- `lte_snr`: Nilai SINR 4G LTE dalam satuan persepuluh dB (nilai `150` berarti `15.0 dB`).
- `lte_pci`: Physical Cell ID LTE.
- `lte_cell_id`: Identitas global sel LTE.
- `lte_ca_pcell_band`: Pita frekuensi pembawa utama LTE (contoh: `LTE BAND 3`).
- `lte_ca_pcell_freq`: Nilai EARFCN downlink sel utama LTE.
- `lte_ca_pcell_bandwidth`: Lebar pita frekuensi sel utama (contoh: `20M`).
- `lte_ca_state`: Indikator agregasi operator (Carrier Aggregation), bernilai `1` jika aktif.
- `nr5g_action_band`: Pita frekuensi 5G aktif (contoh: `n40` atau `n41`).
- `nr5g_action_channel`: Nomor kanal frekuensi radio (ARFCN) 5G aktif.
- `nr5g_pci`: Physical Cell ID 5G.
- `nr5g_cell_id`: Identitas sel 5G.
- `nr5g_rsrp`: Nilai RSRP 5G dalam dBm.
- `nr5g_rsrq`: Nilai RSRQ 5G dalam dB.
- `nr5g_snr`: Nilai SINR 5G dalam satuan persepuluh dB.
- `nr5g_bandwidth`: Lebar pita frekuensi 5G.

### Batch Telemetri Lengkap (Membutuhkan Login)

Aplikasi mengirim batch RPC tunggal setiap siklus polling untuk mengambil seluruh metrik jaringan dan sistem secara simultan:

Request:
```json
[
  {"jsonrpc":"2.0","id":1,"method":"call","params":["<TOKEN>","zte_nwinfo_api","nwinfo_get_netinfo",{}]},
  {"jsonrpc":"2.0","id":2,"method":"call","params":["<TOKEN>","zwrt_router.api","router_get_status",{}]},
  {"jsonrpc":"2.0","id":3,"method":"call","params":["<TOKEN>","zwrt_data","get_wwandst",{"source_module":"web","cid":1,"type":4}]},
  {"jsonrpc":"2.0","id":4,"method":"call","params":["<TOKEN>","zwrt_zte_mdm.api","get_sim_info",{}]},
  {"jsonrpc":"2.0","id":6,"method":"call","params":["<TOKEN>","zwrt_bsp.thermal","get_cpu_temp",{}]},
  {"jsonrpc":"2.0","id":7,"method":"call","params":["<TOKEN>","zwrt_mc.device.manager","get_device_info",{}]}
]
```

Detail service dan fungsi masing-masing:

| ID | Service | Method | Data yang Diambil |
|---|---|---|---|
| 1 | `zte_nwinfo_api` | `nwinfo_get_netinfo` | Parameter radio seluler 4G & 5G |
| 2 | `zwrt_router.api` | `router_get_status` | `mwan_wanlan1_wan_ipaddr` (WAN IP publik), `current_wan_status` |
| 3 | `zwrt_data` | `get_wwandst` | `real_rx_speed` & `real_tx_speed` (Bps), `day_rx_bytes`, `month_rx_bytes` |
| 4 | `zwrt_zte_mdm.api` | `get_sim_info` | `msisdn` (Nomor telepon SIM), `sim_iccid` |
| 6 | `zwrt_bsp.thermal` | `get_cpu_temp` | `cpuss_temp` (Suhu prosesor gateway dalam derajat Celsius) |
| 7 | `zwrt_mc.device.manager` | `get_device_info` | `device_uptime` (Detik aktif sejak boot), versi software |

---

## 4. Kendali Radio dan Penguncian Frekuensi

Perintah di bawah memerlukan sesi autentikasi aktif. Parameter dikirim sebagai string berformat JSON di dalam parameter `params`.

### Mode Jaringan (Bearer Selection)

- **Service:** `zte_nwinfo_api`
- **Method:** `nwinfo_set_netselect`
- **Z-Tag:** `nwinfo_set_netselect`

Parameter `net_select`:
- `AUTO_AND_5G`: Otomatis (5G NSA / SA & 4G LTE)
- `ONLY_5G`: 5G SA Saja
- `ONLY_LTE`: 4G LTE Saja

Request:
```json
[
  {
    "jsonrpc": "2.0",
    "id": 10,
    "method": "call",
    "params": [
      "<TOKEN>",
      "zte_nwinfo_api",
      "nwinfo_set_netselect",
      {
        "net_select": "AUTO_AND_5G"
      }
    ]
  }
]
```

### Penguncian Band 4G LTE (LTE Band Lock)

Penguncian pita 4G menggunakan representasi bitmask desimal. Pita nomor $N$ diwakili oleh bit $2^{N-1}$. Beberapa band digabungkan menggunakan operasi bitwise OR sebelum dikonversi ke string desimal.

- **Service:** `zte_nwinfo_api`
- **Method:** `nwinfo_set_gwl_bandlock`
- **Z-Tag:** `nwinfo_set_gwl_bandlock`

Tabel nilai bitmask pita 4G:
- Band 1 (2100 MHz): $2^0 = 1$
- Band 3 (1800 MHz): $2^2 = 4$
- Band 5 (850 MHz): $2^4 = 16$
- Band 8 (900 MHz): $2^7 = 128$
- Band 40 (2300 MHz): $2^{39} = 549755813888$
- Semua Band (Reset / Unlock): `0`

Contoh request mengunci ke Band 1 + Band 3 (nilai mask = $1 + 4 = 5$):
```json
[
  {
    "jsonrpc": "2.0",
    "id": 11,
    "method": "call",
    "params": [
      "<TOKEN>",
      "zte_nwinfo_api",
      "nwinfo_set_gwl_bandlock",
      {
        "is_gw_band": "0",
        "gw_band_mask": "0",
        "is_lte_band": "1",
        "lte_band_mask": "5"
      }
    ]
  }
]
```

### Penguncian Band 5G NR (NR Band Lock)

Penguncian pita 5G menggunakan daftar nomor band dipisahkan tanda koma.

- **Service:** `zte_nwinfo_api`
- **Method:** `nwinfo_set_nrbandlock`
- **Z-Tag:** `nwinfo_set_nrbandlock`

Request:
```json
[
  {
    "jsonrpc": "2.0",
    "id": 12,
    "method": "call",
    "params": [
      "<TOKEN>",
      "zte_nwinfo_api",
      "nwinfo_set_nrbandlock",
      {
        "nr5g_type": "SA",
        "nr5g_band": "1,3,40"
      }
    ]
  }
]
```

Catatan: Untuk membuka kunci seluruh pita 5G, parameter `nr5g_band` diisi dengan `ALL`.

### Penguncian Sel 4G LTE (Cell Lock)

Mengunci modem ke pemancar spesifik berdasarkan Physical Cell ID (PCI) dan EARFCN kanal frekuensi.

- **Service:** `zte_nwinfo_api`
- **Method:** `nwinfo_lock_lte_cell`
- **Z-Tag:** `nwinfo_lock_lte_cell`

Request:
```json
[
  {
    "jsonrpc": "2.0",
    "id": 13,
    "method": "call",
    "params": [
      "<TOKEN>",
      "zte_nwinfo_api",
      "nwinfo_lock_lte_cell",
      {
        "lock_lte_pci": "342",
        "lock_lte_earfcn": "1825"
      }
    ]
  }
]
```

Catatan: Untuk melepas kunci sel 4G, kirim nilai `0` pada `lock_lte_pci` dan `lock_lte_earfcn`.

### Penguncian Sel 5G NR (NR Cell Lock)

- **Service:** `zte_nwinfo_api`
- **Method:** `nwinfo_lock_nr_cell`
- **Z-Tag:** `nwinfo_lock_nr_cell`

Request:
```json
[
  {
    "jsonrpc": "2.0",
    "id": 14,
    "method": "call",
    "params": [
      "<TOKEN>",
      "zte_nwinfo_api",
      "nwinfo_lock_nr_cell",
      {
        "lock_nr_pci": "120",
        "lock_nr_earfcn": "472000",
        "lock_nr_cell_band": "40"
      }
    ]
  }
]
```

Catatan: Untuk melepas kunci sel 5G, kirim nilai `0` pada ketiga parameter di atas.

---

## 5. Rumus Konversi Frekuensi Standar 3GPP

Aplikasi mengonversi nomor kanal (EARFCN / ARFCN) menjadi frekuensi kerja riil (MHz) berdasarkan spesifikasi resmi 3GPP:

### 4G LTE Downlink Frequency (3GPP TS 36.101)

$$F_{DL} = F_{DL\_low} + 0.1 \times (N_{DL} - N_{Offs-DL})$$

Contoh offset untuk pita frekuensi umum di Indonesia:
- **Band 1:** $N_{Offs} = 0$, $F_{low} = 2110.0\text{ MHz}$
- **Band 3:** $N_{Offs} = 1200$, $F_{low} = 1805.0\text{ MHz}$
- **Band 8:** $N_{Offs} = 3450$, $F_{low} = 925.0\text{ MHz}$
- **Band 40:** $N_{Offs} = 38650$, $F_{low} = 2300.0\text{ MHz}$

### 5G NR Carrier Frequency (3GPP TS 38.104)

Nomor raster global $N_{REF}$ dikonversi ke frekuensi carrier menggunakan interval raster frekuensi $\Delta F_{global}$:

- Rentang $0 \le N_{REF} < 600000$ (Frekuensi $\le 3000\text{ MHz}$):
  $$F_{REF} = 0 + 5\text{ kHz} \times N_{REF}$$
- Rentang $600000 \le N_{REF} < 2016667$ (Frekuensi $3000 - 24250\text{ MHz}$):
  $$F_{REF} = 3000\text{ MHz} + 15\text{ kHz} \times (N_{REF} - 600000)$$
