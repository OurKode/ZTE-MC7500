# ZTE-Script-NG: External Reference

Source of truth for every ubus JSON-RPC call this app makes. If `docs/API.md` and this
script disagree, **the script wins**, then `docs/API.md` gets corrected.

## Source and Pin

| Item | Value |
|---|---|
| Repository | https://github.com/tpoechtrager/ZTE-Web-Script |
| File | `ng/zte-script-ng.js` |
| Raw URL | https://raw.githubusercontent.com/tpoechtrager/ZTE-Web-Script/refs/heads/main/ng/zte-script-ng.js |
| Commit pin | `2c222d3f3ad5e5ee990ec397f6208ee0261537a9` (2025-10-17) |
| Commit message | `* Add Connection Duration under Network Info` |
| SHA256 | `F6A78574277AC401CF6529CB9CD3A50F90D5AB4975F69A7A0E610A6A3CD4A156` |
| Size | 71,506 bytes / 1,995 lines |

The local `zte-script-ng.js` copy at the repository root is byte-identical to upstream
HEAD (verified 2026-10-30). That file is **gitignored (line 64)**. It is a local
reference only and must never be `git add`ed.

## Device Compatibility

The upstream README separates two variants that are **not interchangeable**:

- **NG**: "ZTE G5TC and later"
- **Legacy**: MC7010, MC888, MC889 and others

**MC7500 is not listed explicitly under either.** This app uses the NG API surface
(`zwrt_*`, `nwinfo_*`, `zte_nwinfo_api`) and is proven to work on an MC7500. See the
validation notes in `docs/API.md` and `docs/ideas/zte-script-ng-integration.md`.
Treat MC7500 as part of the NG family.

Upstream also warns that ZTE has broken the locking API through firmware updates
before (the B19 case on MC889). If band locking suddenly fails after an MC7500
firmware update, suspect an API change first, not an app bug.

## Endpoint to Kotlin Symbol Map

| Script (`zte-script-ng.js`) | Service / Method | Android implementation |
|---|---|---|
| `updateDeviceInfo()`, lines 986-993 | batch of 5 calls | `OduApiService.fetchTelemetry()` |
| lines 1043-1044 | `zte_nwinfo_api` / `nwinfo_set_netselect` | `OduRepository.setNetworkSelect()` |
| lines 1051-1052 | `zte_nwinfo_api` / `nwinfo_set_gwl_bandlock` | `OduRepository.set4gBandLock()` |
| lines 1066-1067 | `zte_nwinfo_api` / `nwinfo_set_nrbandlock` | `OduRepository.set5gBandLock()` |
| lines 1078-1079 | `zte_nwinfo_api` / `nwinfo_lock_nr_cell` | `OduRepository.lock5gCell()` / `unlock5gCell()` |
| lines 1094-1095 | `zte_nwinfo_api` / `nwinfo_lock_lte_cell` | `OduRepository.lock4gCell()` / `unlock4gCell()` |
| EARFCN / ARFCN conversion | none (pure arithmetic) | `FrequencyConverter.convert4gEarfcnToMhz()` / `convert5gArfcnToMhz()` |
| 4G band bitmask | none | `FrequencyConverter.calculate4gBandMask()` / `parse4gActiveBands()` |
| Payload to state parsing | `LteSignal.parse()` / `NrSignal.parse()` | `OduRepository.mapPayloadToDomain()` |

The app adds **one call the script does not have**: `zwrt_zte_mdm.api` / `get_sim_info`
(id 4), used for `msisdn` and `sim_iccid`.

## Write Payload Contract (statically verified)

Every write payload is now built in one place, `data/remote/UbusPayloads.kt`, so key
names can be tested without touching a device.

| Action | Method | Payload |
|---|---|---|
| Network mode | `nwinfo_set_netselect` | `{"net_select":"<mode>"}` |
| 4G band lock | `nwinfo_set_gwl_bandlock` | `{"is_gw_band":"0","gw_band_mask":"0","is_lte_band":"1","lte_band_mask":"<decimal>"}` |
| 5G band lock | `nwinfo_set_nrbandlock` | `{"nr5g_type":"SA","nr5g_band":"1,3,28,40,78"}` |
| 4G cell lock | `nwinfo_lock_lte_cell` | `{"lock_lte_pci":"<pci>","lock_lte_earfcn":"<earfcn>"}` |
| 5G cell lock | `nwinfo_lock_nr_cell` | `{"lock_nr_pci":"<pci>","lock_nr_earfcn":"<arfcn>","lock_nr_cell_band":"<band>"}` |

Unlocking a cell is the same call with `0` values.

**Correction history**: an earlier build sent `nr5g_CellLockType` / `nr5g_pci` /
`nr5g_arfcn` / `nr5g_band` / `nr5g_scs` for the 5G cell lock. The script (lines
1076-1086) uses three `lock_nr_*` keys with string values, and that is what the
firmware accepts. The old keys are gone; `UbusPayloadsTest` fails if they return.

All values are sent as strings, numbers included, exactly as the script does
(`pci.toString()`).

## `get_wwandst` Fields

The app and the script use the same parameters:
`{"source_module":"web","cid":1,"type":4}`.

The script reads the following fields (lines 1373-1404):

| Group | Field | Read by script | Read by app |
|---|---|---|---|
| Realtime | `real_rx_speed`, `real_tx_speed` | yes | yes: `downloadSpeedBps` / `uploadSpeedBps` |
| Realtime | `real_time` (connection duration) | yes | no |
| Realtime | `real_rx_bytes`, `real_tx_bytes` | yes | no |
| Realtime | `real_rx_packets`, `real_tx_packets` | yes | no |
| Realtime | `real_rx_error_packets`, `real_tx_error_packets` | yes | no |
| Realtime | `real_rx_drop_packets`, `real_tx_drop_packets` | yes | no |
| Monthly | `month_rx_bytes` | yes | yes: `monthRxBytes` |
| Monthly | `month_tx_bytes` | yes | no |
| Monthly | `month_time`, `month_rx/tx_packets`, `_error_packets`, `_drop_packets` | yes | no |
| Total | `total_rx_bytes`, `total_tx_bytes`, `total_time`, `total_*_packets` | yes | no |

## Verified on MC7500 (read-only probe, 2026-10-30)

A direct probe of a real unit confirmed several things that were previously
assumptions. No write call was executed.

### Daily counters exist, and they mean "today"

`zwrt_data` / `get_wwandst` returns **four** counter groups, not two:

| Group | Fields (verified) |
|---|---|
| Realtime | `real_time`, `real_tx_bytes`, `real_rx_bytes`, `real_tx_packets`, `real_rx_packets`, `real_tx/rx_drop_packets`, `real_tx/rx_error_packets`, `real_tx_speed`, `real_rx_speed`, `real_max_tx_speed`, `real_max_rx_speed` |
| Daily | `day_time`, `day_tx_bytes`, `day_rx_bytes`, `day_tx/rx_packets`, `day_tx/rx_drop_packets`, `day_tx/rx_error_packets` |
| Monthly | `month_time`, `month_tx_bytes`, `month_rx_bytes`, `month_tx/rx_packets`, `month_tx/rx_drop_packets`, `month_tx/rx_error_packets` |
| Total | `total_time`, `total_tx_bytes`, `total_rx_bytes`, `total_tx/rx_packets`, `total_tx/rx_drop_packets`, `total_tx/rx_error_packets` |

Conclusions:

- **`day_rx_bytes` is real**: parsed by the app in `OduModels.kt:92`. The script does
  not use it, but the device provides it.
- **`day_tx_bytes` also exists**, and the app does **not** parse it. Daily upload is
  missing from the UI.
- **Daily counters survive a reboot.** At probe time `day_time` was 65,322 s (18.1 h)
  while `device_uptime` was 5,449 s (1.5 h). Because `day_time` is far larger than
  uptime, the counter does not start at boot; **it resets at calendar midnight**, not
  on a 24-hour cycle from boot.
- Packet, error and drop fields are **available for all three groups**, and none are
  used by the app yet.

### Monthly quota: unusable

`zwrt_data` / `get_wwandst_monthlimit` returns `result: [2]`, a non-zero status with
no payload, not an object holding a quota limit. On this firmware the quota limit is
**not readable** through that method. Treat "remaining quota" as **unavailable** until
there is evidence otherwise.

### Reboot: not `system.reboot`

`system` / `info` and `system` / `board` return `Access denied` (`-32002`) for the ZTE
web session: the object exists, but the ACL refuses it. Do not rely on
`system.reboot`.

What **is** available is the device's own reboot scheduler, readable through
`zwrt_mc.device.manager` / `get_device_info`:

```
reboot_schedule_enable, reboot_schedule_mode, reboot_dow,
reboot_hour1, reboot_min1, reboot_timeframe_hours1, reboot_random_min1,
reboot_dod, reboot_hour2, reboot_min2, ... (up to 3 schedules)
poweronoff_schedule_enable, poweron_schedule_hour/min,
poweroff_schedule_hour/min, WatchDog_Setting_Option
```

That is the safe reboot path: set a schedule, do not press a button.

### Actual `get_device_info` response shape

- `cpuss_temp` is returned as a **number** (`52`), not a string. The app model uses
  `String?`; kotlinx tree-based decoding tolerates this, but it is fragile.
- `device_uptime` is returned as a **string** (`"5449"`), while the app model uses
  `Long?`.
- **`hardware_version` and `software_version` are absent** from the response. What
  exists is `wa_inner_version` (empty) and `wa_version`. As a result
  `OduSignalState.softwareVersion` is always `null`.
- Unused extras: `meminfo` (total/free/available RAM), `flashinfo` (overlay partition
  usage), `reboot_schedule_*`, `WatchDog_Setting_Option`.

### Connected devices

`zwrt_router.api` / `router_get_user_list_num` works and returns `access_total_num`,
`lan_num`, `wireless_num`, `offline_num`, plus guest/2.4G/5G/6G variants. This can
replace the hardcoded `connectedDevicesCount = 1` in `OduRepository.kt:168`.

## Script Features Deliberately Not Implemented

| Endpoint | Reason |
|---|---|
| `zwrt_wlan` (internal Wi-Fi) | The ODU is an outdoor unit; Wi-Fi is the indoor router's job. See the "Not Doing" section of `docs/ideas/zte-script-ng-integration.md`. |
| `zwrt_wms` / `zwrt_wms_get_wms_capacity` (SMS) | No UI need; never evaluated. |

## Drift Check Against Upstream

```powershell
$u = 'https://raw.githubusercontent.com/tpoechtrager/ZTE-Web-Script/refs/heads/main/ng/zte-script-ng.js'
Invoke-WebRequest -Uri $u -OutFile zte-script-ng.js -UseBasicParsing
(Get-FileHash zte-script-ng.js -Algorithm SHA256).Hash
```

If the hash differs from the pin above: read the diff, check whether any endpoint or
field the app uses has changed, then update `docs/API.md` and the pin in this document.
