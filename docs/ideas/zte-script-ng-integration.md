# Integrating zte-script-ng.js into the ZTE MC7500 ODU Monitor

> **Status: implemented.** This document records the reasoning behind the radio and
> telemetry features that now ship in the app. Where it disagrees with the code, the
> code and `docs/REFERENCE-SCRIPT.md` win.

## Problem Statement

How do we bring the deep telemetry diagnostics and the radio frequency controls (band
and cell locking) from `zte-script-ng.js` into an Android app without risking a broken
network configuration or permanently dropping the ODU off the cell?

## Recommended Direction

The chosen approach is a **staged safety architecture** that combines safe read-only
deep telemetry with defensive radio control built on curated operator profiles:

1. **Stage 1: deep diagnostic telemetry (safe, non-destructive)**:
   - Add **router CPU temperature** monitoring (`zwrt_bsp.thermal` -> `get_cpu_temp`)
     and **device info / uptime** (`zwrt_mc.device.manager` -> `get_device_info`).
   - Implement arithmetic channel-to-frequency conversion in **MHz**:
     - 4G LTE: EARFCN to downlink frequency based on 3GPP TS 36.101.
     - 5G NR: ARFCN raster to frequency based on 3GPP TS 38.101.
   - Show the MHz value next to the band name on the main signal card (for example
     `Band n40 • 2300.0 MHz`, `Band 3 • 1805.0 MHz`).

2. **Stage 2: defensive frequency management (radio and band tools)**:
   - Placed on an isolated Radio tab so the controls cannot be reached by accident.
   - **Curated operator profiles** remove the risk of hand-built bitmasks by offering
     validated Indonesian carrier presets:
     - **Telkomsel**: 4G (B1, B3, B8, B40) + 5G (n40)
     - **XL Axiata**: 4G (B1, B3, B8)
     - **Indosat Ooredoo Hutchison**: 4G (B1, B3, B8)
     - **Smartfren**: 4G (B40, B28)
     - **Reset / Auto**: restores the full mask (`buildFullMask`) so every band is
       allowed again.
   - **One-tap current cell lock**:
     - Cell locking (PCI + EARFCN) only ever takes parameters from the *active cell
       that is currently connected and demonstrably working*; there is no free-text
       numeric entry.
     - Paired with an emergency **Unlock Cell** button that sends `(0, 0)` for 4G and
       `(0, 0, 0)` for 5G.

## Key Assumptions Validated

- [x] **UBUS RPC compatibility**: the `zwrt_bsp.thermal` (`get_cpu_temp`) and
  `zwrt_mc.device.manager` (`get_device_info`) endpoints succeed inside a batched RPC
  call and return real data (`53°C`, `23h 23m`).
- [x] **Band lock permission on the admin account**: `nwinfo_set_gwl_bandlock`,
  `nwinfo_set_netselect`, `nwinfo_set_nrbandlock` and `nwinfo_lock_lte_cell` work with
  the `Admin` credential validation in place.
- [x] **Responsiveness during band switching**: the ZTE MC7500 needs time to
  re-synchronise; the UI shows a non-blocking progress indicator and a confirmation
  dialog to prevent accidents.

## MVP Scope

### In Scope

- **Telemetry diagnostics (read-only)**:
  - ODU CPU temperature (°C) on the gateway status card.
  - Downlink frequency arithmetic (MHz) from the active EARFCN and ARFCN.
  - Router uptime.
  - Secondary carrier aggregation (SCC) detail including channel bandwidth (MHz).
- **Band and radio management (control layer)**:
  - A dedicated Radio tab.
  - Network mode (bearer): Auto (5G/4G/3G), 5G NSA/SA, 4G only.
  - Indonesian operator band presets (Telkomsel, XL Axiata, Indosat, Smartfren) plus
    per-band selection.
  - Emergency "Restore All Bands (Auto)" button.
  - "Lock to Current Cell" and "Unlock Cell".

## Not Doing (and Why)

- **Internal Wi-Fi parameter management (`setWifiTxPower`, `setWifiCountry`,
  `setWifiMaxClients`)**:
  - *Reason*: the ZTE MC7500 is an outdoor unit whose job is to capture the cellular
    signal as cleanly as possible. Internal Wi-Fi settings belong to an indoor router
    or mesh and are outside this app's focus.
- **Free-text bitmask or PCI entry**:
  - *Reason*: a mistyped or invented PCI/EARFCN can leave the ODU with no service and
    unable to attach to a cell.
- **Raw JavaScript execution or script evaluator**:
  - *Reason*: all logic is ported natively to **Kotlin** with typed data classes, error
    handling, and the regular Android toolchain.

## Technical Implementation Roadmap

```mermaid
flowchart TD
    subgraph Phase 1: Telemetry and Frequency (Non-Destructive)
        A["1.1 DTO and extra RPC: get_cpu_temp, get_device_info"] --> B["1.2 EARFCN/ARFCN to MHz module (pure Kotlin)"]
        B --> C["1.3 Show MHz and CPU temperature on the dashboard"]
    end

    subgraph Phase 2: Curated Radio Control (Safety First)
        C --> D["2.1 4G bitmask engine and 5G string format (curated presets)"]
        D --> E["2.2 RPC nwinfo_set_netselect and nwinfo_set_gwl_bandlock"]
        E --> F["2.3 One-tap cell lock and unlock"]
        F --> G["2.4 Dedicated Radio tab"]
    end
```
