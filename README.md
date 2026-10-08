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

## Technical API Documentation

Comprehensive technical specifications of the ZTE MC7500 internal OpenWrt ubus JSON-RPC protocol are documented in [docs/API.md](docs/API.md):
- Mandatory HTTP headers and gateway endpoints
- Double-SHA256 salted password hashing algorithm
- Batch telemetry JSON request and response payloads
- Decimal bitmask calculations for 4G LTE band locking
- 3GPP channel-to-frequency conversion formulas (TS 36.101 and TS 38.104)

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
│   │   ├── ui/           # Jetpack Compose UI (Dashboard, Theme)
│   │   ├── util/         # 3GPP frequency converter and formatters
│   │   ├── widget/       # Glance homescreen widgets
│   │   └── worker/       # WorkManager background sync workers
│   └── build.gradle.kts  # Application module build and release logic
├── docs/
│   └── API.md            # Complete ubus JSON-RPC API specification
├── CONTRIBUTING.md       # Contribution guidelines
├── LICENSE               # MIT License
├── version.properties    # Semantic version configuration
└── README.md             # Project documentation
```

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
