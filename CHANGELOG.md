# Changelog

All notable changes to **ZTE MC7500 ODU Monitor** are documented in this file.

## [1.3.0] - 2026-10-23

### Added
- **Selectable Chart Time Ranges**: Switch live telemetry and RF charts between `1m`, `5m`, `15m`, `1h`, `6h`, and `24h` windows.
- **Persistent SQLite History & Time-Bucket Aggregation**: Upgraded local SQLite schema (`v2`) to persist real-time download/upload throughput alongside 4G/5G RF metrics, aggregated over fixed wall-clock buckets so historical points remain stable across range switches and app restarts.
- **Automatic UI Preference Persistence**: Automatically saves and restores polling interval, active chart time range, selected 5G/4G chart metric (`RSRP`/`SINR`/`RSRQ`), active navigation tab, and custom band selections.
- **Explicit ODU Disconnect Banner**: Immediate visual banner and state handling when the ZTE MC7500 Outdoor Unit becomes unreachable.
- **Material 3 / Dynamic Color & Adaptive Launcher Icon**: Full Material You dynamic theming with Android 13+ monochrome themed icon support.

### Changed
- **Utilitarian Minimalist Monitoring UI**: Redesigned the Monitoring tab with flat `1dp` bordered bento cards, tabular monospace telemetry readouts, proportional timestamp X-axes, precision linear chart segments, and ratcheted Y-axis scaling.
- **R8 Full Mode Optimization**: Enabled code minification, obfuscation, and resource shrinking with custom ProGuard rules for Kotlinx Serialization, OkHttp, and Jetpack Glance, reducing release APK size by ~84% (`11.83 MB` → `1.93 MB`).

### Security
- **Removed Hardcoded Default Router Password**: Replaced hardcoded fallback credentials with empty defaults and added pre-flight blank-password guards to prevent accidental router admin lockouts.

## [1.2.0] - 2026-10-23

### Added
- Automated Git semantic versioning (`MAJOR.MINOR.PATCH`) and commit-based `versionCode`.
- Safe radio controls: network bearer selection, Indonesian operator band presets, 4G decimal bitmask band locking, 5G NR band locking, and active PCI/EARFCN cell locking.
- Foreground monitoring service with real-time alerts for band changes, public WAN IP changes, and gateway offline events.
- ZTE MC7500 ubus JSON-RPC double-SHA256 challenge-response authentication and single-request batch telemetry polling.
