# Traffic Telemetry and ODU Diagnostics

Read-only probe results from an MC7500 (2026-10-30): most of the data this app needs
**already exists on the device**; the app simply does not read it yet.

## Problem Statement

How should the app answer "how much have I used today" and "why does the connection
feel slow" without opening the router web UI, by using fields the device already
sends but the data model discards?

## Recommended Direction

Three layers, ordered cheapest and safest first. Every endpoint here is verified live
on a real unit; there are no new endpoint discoveries and no write operation is
required.

**Layer 1: Complete the traffic counters.** `get_wwandst` returns four groups
(`real_`, `day_`, `month_`, `total_`), each with bytes, packets, errors and drops for
RX and TX. The model currently keeps 4 of those dozens of fields. Biggest impact for
the smallest cost: daily upload is completely invisible even though `day_tx_bytes` is
available.

**Layer 2: Diagnose from packet counters.** `day_rx_error_packets` and
`_drop_packets` are metrics no web UI shows. High retransmission explains "speed drops
while RSRP looks fine", a cause that will never appear in a signal chart.

**Layer 3: Connected devices and reboot schedule.** `router_get_user_list_num`
returns real counts (verified: 1 LAN, 0 wireless), replacing the hardcoded
`connectedDevicesCount = 1` in `OduRepository.kt:168`. The built-in reboot schedule
(`reboot_schedule_*`) is displayed read-only as information, with no execute button.

## Key Assumptions to Validate

- [ ] **Daily counters reset at calendar midnight.** Strong evidence: `day_time`
  65,322 s versus `device_uptime` 5,449 s, so it is not since boot. Not yet proven
  whether it follows the NITZ timezone (`nitz_timezone: 7.0`) or the router's internal
  zone. Test: one observation across midnight, compare `day_time` before and after.
- [ ] **`day_rx_bytes` is not a cumulative counter that wraps.** Observed value
  45.2 GB in 18 hours, consistent with `month_rx_bytes` 499.8 GB in 9.7 days. Test:
  two samples 10 minutes apart, confirm the value grows plausibly.
- [ ] **`router_get_user_list_num` exists on every MC7500 firmware variant.** Verified
  on one unit only. Test: fall back to an empty value when the method fails.
- [ ] **The data model tolerates the real types.** `cpuss_temp` arrives as a number
  (`52`) even though the model uses `String?`; `device_uptime` arrives as a string
  (`"5449"`) even though the model uses `Long?`. Test: deserialisation unit tests using
  real device responses.

## MVP Scope

In:

- Extend `OduTrafficPayload` with the `day_`, `month_` and `total_` groups, plus
  `real_time` and `real_max_*_speed`.
- Extend `OduSignalState` with daily traffic fields (RX and TX), error/drop, and
  connection duration.
- Daily traffic card: separate DL and UL, plus an error/drop indicator past a threshold.
- Replace hardcoded `connectedDevicesCount` with `router_get_user_list_num`.
- Show the built-in reboot schedule (read-only) in Settings.
- Deserialisation unit tests using real device responses.

Out of MVP: anything that writes configuration.

## Not Doing (and Why)

- **Direct reboot button**: `system.info` and `system.board` return `Access denied`
  (-32002) for a ZTE web session. The object exists; the ACL refuses. The correct path
  is the built-in reboot schedule.
- **Remaining quota**: `get_wwandst_monthlimit` returns `result: [2]`, a non-zero
  status with no payload. The plan limit is not readable on this firmware.
- **Firmware upgrade / factory reset**: `sysupgrade` lives in the `system` object that
  is already refused, and losing the factory carrier configuration cannot be undone
  from the app.
- **SIM slot switch** (`sim_switch_slot`): it can leave the ODU with no service.
- **Per-device traffic**: `router_get_clients_traffic` returns `[0]` with no payload;
  the required parameters are unknown.
- **DHCP lease list**: `luci-rpc.getDHCPLeases` does not respond on this device.
- **Wi-Fi**: the ODU reports `wireless_num: 0`; there are no wireless clients to
  manage.

## Open Questions

- Do `day_*` counters follow the NITZ timezone, or a separate internal router zone?
- Should the daily counter read as an absolute "today", or as "since the device's
  midnight"?
- Is support for two devices (ODU plus indoor router) needed in this iteration, or
  deferred?
- Should the reboot schedule be **editable** from the app, or is displaying it enough?
