# ZTE MC7500 ubus JSON-RPC API Specification

Technical documentation of the internal OpenWrt ubus JSON-RPC interface on the ZTE MC7500 5G/4G Outdoor Unit (ODU). This protocol enables cellular telemetry collection, hardware monitoring, radio bearer selection, carrier band locking, and physical cell locking.

---

## 1. Protocol Overview

Communication with the ZTE MC7500 gateway utilizes JSON-RPC 2.0 transported over HTTP or HTTPS POST.

- **Endpoint URL:** `http://<host>/ubus/?t=<timestamp_ms>` or `https://<host>/ubus/?t=<timestamp_ms>`
- **Default Gateway IP:** `192.168.254.1`
- **HTTP Method:** `POST`
- **Payload Format:** JSON-RPC 2.0 (single call object or batch array)

### Mandatory HTTP Headers

Every request to `/ubus/` must provide the following HTTP headers:

```http
Content-Type: application/json; charset=utf-8
Accept: application/json, text/javascript, */*; q=0.01
Origin: http://<host>
Referer: http://<host>/
X-Requested-With: XMLHttpRequest
Z-Mode: 0
Z-Tag: <tag_method>
```

Header semantics:
- `Z-Mode`: Set to `0` for standard RPC calls, or `1` when performing encrypted certificate exchanges.
- `Z-Tag`: Identifies the target action. This usually matches the method name (e.g., `nwinfo_get_netinfo`, `web_login_info`, `router_get_status`).

---

## 2. Authentication & Session Management

Reading sensitive gateway parameters (public WAN IP, data consumption, radio locks) requires an active authenticated session token. Unauthenticated requests supply a 32-character zero string (`00000000000000000000000000000000`).

### Step 1: Request Login Salt

Before hashing the password, the client requests the gateway salt:

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

Response:
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

### Step 2: Password Hashing Algorithm

The ZTE MC7500 utilizes a double-SHA256 scheme formatted as uppercase hexadecimal:

$$\text{Hash} = \text{SHA256}(\text{SHA256}(\text{Password}).\text{toUpperCase}() + \text{Salt}).\text{toUpperCase}()$$

Kotlin reference implementation:
```kotlin
fun hashPassword(password: String, salt: String): String {
    val firstPass = sha256Hex(password).uppercase()
    return sha256Hex(firstPass + salt).uppercase()
}
```

### Step 3: Login Request

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
        "password": "<COMPUTED_HASH>"
      }
    ]
  }
]
```

Response:
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

- `result = 0`: Authentication successful. Store `ubus_rpc_session` as the active token for subsequent requests.
- `result != 0`: Authentication rejected (incorrect credentials or account locked).
- Error code `-32002`: Session expired. The client must re-authenticate starting from Step 1.

---

## 3. Cellular & Hardware Telemetry

### Radio Network Info (Unauthenticated)

Can be queried with a 32-zero token or an authenticated session token.

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

Key payload response fields:
- `network_type`: Active radio technology (`ENDC`, `LTE`, `NR5G_SA`, etc.).
- `network_provider`: Detected cellular carrier name.
- `signal_bar`: Signal bar integer (0 to 5).
- `lte_rsrp`: 4G LTE RSRP in dBm (e.g., `-95`).
- `lte_rsrq`: 4G LTE RSRQ in dB (e.g., `-12`).
- `lte_snr`: 4G LTE SINR in tenths of a dB (value `150` represents `15.0 dB`).
- `lte_pci`: 4G Physical Cell ID.
- `lte_cell_id`: 4G E-UTRAN Cell Global Identifier.
- `lte_ca_pcell_band`: 4G primary serving band (e.g., `LTE BAND 3`).
- `lte_ca_pcell_freq`: 4G primary serving downlink channel number (EARFCN).
- `lte_ca_pcell_bandwidth`: Primary carrier bandwidth (e.g., `20M`).
- `lte_ca_state`: Carrier Aggregation status (`1` when secondary carrier is active).
- `nr5g_action_band`: Active 5G band (e.g., `n40` or `n41`).
- `nr5g_action_channel`: Active 5G radio frequency channel number (ARFCN).
- `nr5g_pci`: 5G Physical Cell ID.
- `nr5g_cell_id`: 5G Cell Identifier.
- `nr5g_rsrp`: 5G RSRP in dBm.
- `nr5g_rsrq`: 5G RSRQ in dB.
- `nr5g_snr`: 5G SINR in tenths of a dB.
- `nr5g_bandwidth`: 5G carrier bandwidth.

### Batch Telemetry Query (Authenticated)

To maximize performance, the application bundles all required RPC calls into a single JSON array payload:

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

Services overview:

| ID | Service | Method | Target Telemetry |
|---|---|---|---|
| 1 | `zte_nwinfo_api` | `nwinfo_get_netinfo` | 4G & 5G radio signal metrics |
| 2 | `zwrt_router.api` | `router_get_status` | `mwan_wanlan1_wan_ipaddr` (Public WAN IP), connection status |
| 3 | `zwrt_data` | `get_wwandst` | `real_rx_speed` & `real_tx_speed` (Bps), daily and monthly bytes |
| 4 | `zwrt_zte_mdm.api` | `get_sim_info` | `msisdn` (SIM telephone number), `sim_iccid` |
| 6 | `zwrt_bsp.thermal` | `get_cpu_temp` | `cpuss_temp` (Gateway CPU temperature in Celsius) |
| 7 | `zwrt_mc.device.manager` | `get_device_info` | `device_uptime` (Uptime in seconds), software version |

---

## 4. Radio Controls & Frequency Locking

All modification endpoints require an active session token.

### Network Bearer Selection

- **Service:** `zte_nwinfo_api`
- **Method:** `nwinfo_set_netselect`
- **Z-Tag:** `nwinfo_set_netselect`

Supported `net_select` modes:
- `AUTO_AND_5G`: Auto (5G NSA / SA & 4G LTE)
- `ONLY_5G`: 5G SA Only
- `ONLY_LTE`: 4G LTE Only

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

### 4G LTE Band Locking

4G LTE band locking operates through decimal bitmask representation. Band $N$ corresponds to bit $2^{N-1}$. Multiple bands are combined via bitwise OR before converting to a decimal string.

- **Service:** `zte_nwinfo_api`
- **Method:** `nwinfo_set_gwl_bandlock`
- **Z-Tag:** `nwinfo_set_gwl_bandlock`

Decimal bitmask reference table:
- Band 1 (2100 MHz): $2^0 = 1$
- Band 3 (1800 MHz): $2^2 = 4$
- Band 5 (850 MHz): $2^4 = 16$
- Band 8 (900 MHz): $2^7 = 128$
- Band 40 (2300 MHz): $2^{39} = 549755813888$
- All Bands (Unlock): `0`

Example request to lock Band 1 + Band 3 (mask = $1 + 4 = 5$):
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

### 5G NR Band Locking

5G NR band locking takes a comma-separated list of band numbers.

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

Note: To unlock all 5G bands, send `"nr5g_band": "ALL"`.

### 4G LTE Cell Locking

Locks the modem to a specific tower using its Physical Cell ID (PCI) and EARFCN.

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

Note: To unlock 4G cell locking, send `"0"` for both `lock_lte_pci` and `lock_lte_earfcn`.

### 5G NR Cell Locking

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

Note: To unlock 5G cell locking, send `"0"` for all three parameters.

---

## 5. 3GPP Standard Frequency Calculations

The application converts raw channel numbers (EARFCN / ARFCN) into carrier frequencies (MHz) based on official 3GPP specifications:

### 4G LTE Downlink Frequency (3GPP TS 36.101)

$$F_{DL} = F_{DL\_low} + 0.1 \times (N_{DL} - N_{Offs-DL})$$

Representative band offsets in Indonesia:
- **Band 1:** $N_{Offs} = 0$, $F_{low} = 2110.0\text{ MHz}$
- **Band 3:** $N_{Offs} = 1200$, $F_{low} = 1805.0\text{ MHz}$
- **Band 8:** $N_{Offs} = 3450$, $F_{low} = 925.0\text{ MHz}$
- **Band 40:** $N_{Offs} = 38650$, $F_{low} = 2300.0\text{ MHz}$

### 5G NR Carrier Frequency (3GPP TS 38.104)

Global frequency raster $N_{REF}$ is converted using raster spacing $\Delta F_{global}$:

- Range $0 \le N_{REF} < 600000$ (Frequency $\le 3000\text{ MHz}$):
  $$F_{REF} = 0 + 5\text{ kHz} \times N_{REF}$$
- Range $600000 \le N_{REF} < 2016667$ (Frequency $3000 - 24250\text{ MHz}$):
  $$F_{REF} = 3000\text{ MHz} + 15\text{ kHz} \times (N_{REF} - 600000)$$
