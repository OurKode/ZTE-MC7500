# ZTE MC7500 ODU Signal Monitor & Widget

[![GitHub Release](https://img.shields.io/github/v/release/OurKode/ZTE-MC7500?style=flat-square&color=blue)](https://github.com/OurKode/ZTE-MC7500/releases)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-green?style=flat-square)](https://developer.android.com)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)
[![Android CI](https://github.com/OurKode/ZTE-MC7500/actions/workflows/android-ci.yml/badge.svg)](https://github.com/OurKode/ZTE-MC7500/actions/workflows/android-ci.yml)

An Android application and homescreen widget suite (Jetpack Glance) built for monitoring, diagnosing, and controlling ZTE MC7500 5G/4G Outdoor Units (ODUs) in real time.

---

## Key Features

- **Authenticated Gateway Telemetry**:
  - Live download and upload throughput rates displayed in Mbps.
  - Periodic dual-line throughput trend chart (download and upload history).
  - Daily and monthly data quota consumption tracking.
  - Public WAN IP address and SIM card phone number (MSISDN) display.
  - Gateway hardware health metrics, including CPU temperature and device uptime.
- **High-Efficiency JSON-RPC Engine**:
  - Direct HTTP/HTTPS communication with the OpenWrt ubus subsystem on ZTE MC7500.
  - Multi-query batch payloads bundled into a single HTTP request to minimize router CPU usage.
- **Radio & Band Control Tools**:
  - Bearer network selection: Auto (5G NSA/SA & 4G LTE), 5G SA Only, or 4G LTE Only.
  - Indonesian carrier presets: Telkomsel, XL Axiata, Indosat Ooredoo Hutchison, and Smartfren.
  - Decimal bitmask-based 4G LTE band locking.
  - List-based 5G NR band locking.
  - Active cell locking (PCI and EARFCN/ARFCN) with confirmation dialogs to prevent accidental signal loss.
- **Real-Time System Notifications**:
  - Immediate background notifications when the active cellular band changes.
  - Alerts when the public WAN IP address changes.
  - Gateway disconnect and offline detection.
- **Homescreen Widgets (Jetpack Glance)**:
  - `OduCompactWidget` (2x2): High-glanceability 5G RSRP and SINR metrics.
  - `OduDetailedWidget` (4x2): Dual 5G/4G indicators, active band tags, and tower timestamps.
- **Demo Mode Simulator**:
  - Built-in offline simulator to test all UI components and widget states without requiring physical hardware connection.

---

## Carrier Band Presets

The 4G LTE band lock card keeps four one-tap presets, so a carrier change does not
require rebuilding a bitmask by hand. Each preset only selects the bands; it does
not lock a cell, and it does not touch 5G.

| Preset | 4G LTE bands |
| --- | --- |
| Telkomsel | B1, B3, B8, B40 |
| XL Axiata | B1, B3, B8 |
| Indosat | B1, B3, B8 |
| Smartfren | B28, B40 |

Presets live in `RadioControlScreen.kt` (`operatorProfiles`) and are converted with
`FrequencyConverter.calculate4gBandMask`, which sets bit `n-1` for band `n`. The
"Custom" chip row and the "Auto / All" button remain available for any other
combination or to clear the lock. Every lock action goes through a confirmation
dialog first, because a wrong band mask drops the link until it is reset.

---

## Interface: Retro Field Instrument

The UI is deliberately styled as a 1970s bench instrument rather than a generic
Material app: cream shell, charcoal plate, burnt-orange accent, stamped 3dp corners,
bevelled hairlines instead of elevation, and seven-segment numerals drawn as real
canvas geometry (no font file required, so no missing glyphs).

- **Two skins**: `Cream` (default) and `Charcoal`, selectable in Settings. Both map
  the same token names, so every screen and both widgets look like one product.
- **No overlays, no filters.** There are no scanlines, no texture and no tint across the
  screen; the only ruled lines in the app are the chart grid inside the chart canvas.
- **Motion**: state changes only. One easing curve and one duration set, no ambient
  loops, no digit rolling. The app honours the system "Remove animations" setting and
  battery saver by dropping to instant transitions.
- **Widgets** cannot animate, so they carry the identity statically through the same
  palette and flat plates.

All user-facing copy lives in `res/values/strings*.xml` and is written in English by
default. No text is hardcoded in composables.

---

## Automated Git Versioning System

The repository uses a semantic versioning scheme (`MAJOR.MINOR.PATCH`) integrated directly with Git commit history:

- **Version Declaration**: Configured in [version.properties](version.properties).
- **Automated Build Number (`versionCode`)**: Derived dynamically on each build from the total Git commit count (`git rev-list --count HEAD`). Every commit increments the build number automatically.
- **Version Name (`versionName`)**: Formatted as `$VERSION_MAJOR.$VERSION_MINOR.$VERSION_PATCH`.
- **In-App Display**: The settings panel footer dynamically shows the active version, for example `ZTE ODU Monitor v1.2.0 (Build 15)`.

### Gradle Tasks for Version Management

```bash
# Print current version, code, and commit hash
./gradlew printVersion

# Increment patch version (e.g., 1.2.0 -> 1.2.1)
./gradlew bumpPatch

# Increment minor version and reset patch (e.g., 1.2.1 -> 1.3.0)
./gradlew bumpMinor

# Increment major version and reset minor and patch (e.g., 1.3.0 -> 2.0.0)
./gradlew bumpMajor
```

---

## Building and Installation

### Prerequisites
- JDK 17
- Android SDK with API 34 platform (Build Tools 34.0.0+)
- Android device running Android 8.0 (API level 26) or newer

### Compiling the Release APK

```bash
# Linux / macOS
./gradlew assembleRelease

# Windows PowerShell
.\gradlew assembleRelease
```

The compiled release package is written to:
```
app/build/outputs/apk/release/app-release.apk
```

The Gradle script includes default release signing fallback, allowing the output APK to be installed immediately without certificate errors.

### Installing via ADB

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## Privacy and Security

This app talks to one device: the ZTE MC7500 on your own network. It sends no
analytics, no crash reports and no telemetry anywhere else.

- **Credentials**: the router host, username, password and login switch are stored in a
  separate preferences file, each value encrypted with AES-256-GCM under a key that
  stays inside the Android keystore (`AesGcmCipher`, random IV per value, authenticated
  payload). Plaintext values from older builds are migrated on first read and deleted.
  If the keystore is unavailable, credentials are kept in memory for the session only
  and never written as plaintext.
- **No credential logging**: no log statement prints a password, hash or request body.
- **Backups are off** (`android:allowBackup="false"`), so preferences are not extractable
  through `adb backup`.
- **Cleartext traffic is limited** to the router's own address through
  `network_security_config.xml`; the rest of the app uses HTTPS.
- **Radio writes always confirm**: band and cell locks pass through a confirmation
  dialog, and every write payload is covered by unit tests.

If you find a security problem, please open a private report rather than a public issue
(see [SECURITY.md](SECURITY.md)).

---

## Technical API Documentation

Comprehensive technical specifications of the ZTE MC7500 internal OpenWrt ubus JSON-RPC protocol are documented in [docs/API.md](docs/API.md):
- Mandatory HTTP headers and gateway endpoints
- Double-SHA256 salted password hashing algorithm
- Batch telemetry JSON request and response payloads
- Decimal bitmask calculations for 4G LTE band locking
- 3GPP channel-to-frequency conversion formulas (TS 36.101 and TS 38.104)

[docs/REFERENCE-SCRIPT.md](docs/REFERENCE-SCRIPT.md) maps the vendor script
(`zte-script-ng.js`) to the Kotlin symbols that implement it, and records which device
fields are verified and which are still unused. When the two documents disagree, the
script is authoritative.

### Write payload contract

Radio write calls follow the vendor script (`zte-script-ng.js`), which is the
source of truth when it disagrees with older notes. The payload builders live in
`data/remote/UbusPayloads.kt` and are covered by unit tests:

| Action | ubus method | Keys |
| --- | --- | --- |
| Bearer mode | `nwinfo_set_netselect` | `net_select` |
| 4G band lock | `nwinfo_set_gwl_bandlock` | `is_gw_band`, `gw_band_mask`, `is_lte_band`, `lte_band_mask` |
| 5G band lock | `nwinfo_set_nrbandlock` | `nr5g_type`, `nr5g_band` |
| 4G cell lock | `nwinfo_lock_lte_cell` | `lock_lte_pci`, `lock_lte_earfcn` |
| 5G cell lock | `nwinfo_lock_nr_cell` | `lock_nr_pci`, `lock_nr_earfcn`, `lock_nr_cell_band` |

All values are sent as strings. Unlocking a cell is the same call with zeroes.
These payloads were verified statically against the script and the unit tests;
no write was executed against a live ODU during development.

---

## Project Structure

```
ZTE-MC7500/
├── .github/
│   ├── workflows/        # GitHub Actions CI and Release pipelines
│   └── ISSUE_TEMPLATE/   # Bug report and feature request templates
├── app/
│   ├── src/main/java/com/example/odumonitor/
│   │   ├── data/         # Data models, OkHttp API service, and repository
│   │   ├── receiver/     # System and widget broadcast receivers
│   │   ├── service/      # Foreground monitor service
│   │   ├── ui/           # Compose UI: theme, dashboard, radio, settings, common
│   │   ├── util/         # 3GPP frequency converter and formatters
│   │   ├── widget/       # Glance homescreen widgets
│   │   └── worker/       # WorkManager background sync workers
│   └── build.gradle.kts  # Application module build and release logic
├── docs/
│   ├── API.md            # Complete ubus JSON-RPC API specification
│   └── REFERENCE-SCRIPT.md  # Vendor script to Kotlin symbol map
├── CONTRIBUTING.md       # Contribution guidelines
├── SECURITY.md           # Supported versions and vulnerability reporting
├── LICENSE               # MIT License
├── version.properties    # Semantic version configuration
└── README.md             # Project documentation
```

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

The interface uses **Barlow**, provided by the Barlow Project Authors under the SIL
Open Font License 1.1. The full text ships with the app at
`app/src/main/assets/licenses/OFL_barlow.txt`.
