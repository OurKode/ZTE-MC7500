# Changelog

All notable changes to **ZTE MC7500 ODU Monitor** are documented in this file.

## [1.4.0] - 2026-11-16

### Added
- **Retro field-instrument interface**: cream and charcoal skins, bevelled plates
  instead of elevation, burnt-orange accent, and seven-segment numerals drawn as canvas
  geometry (no font asset, so no missing glyphs).
- **Uniform motion system**: one easing curve and one duration set, applied to state
  changes only, disabled automatically by "Remove animations" and battery saver.
- **Instrument components**: `InstrumentToggle`, `InstrumentTabStrip`,
  `InstrumentChip` and `InstrumentSectionHeader` replace the stock Material pills and
  switches on Dashboard, Radio and Settings.
- **English copy**: every user-facing string moved to `res/values/strings*.xml`, with
  English as the default locale.
- **Payload unit tests** for every radio write call, plus an explicit 5G cell lock
  contract check against the vendor script.

### Changed
- **5G cell lock payload fixed**: `nwinfo_lock_nr_cell` now sends `lock_nr_pci`,
  `lock_nr_earfcn` and `lock_nr_cell_band` as strings, matching `zte-script-ng.js`.
  The previous `nr5g_*` keys were not accepted by the firmware.
- **Widgets restyled** to the same palette with flat 3dp plates; no animation, since
  Glance cannot run one usefully.
- **Launcher icon** replaced with the three-bar signal mark on a stamped plate.
- **Appearance settings simplified** to the two skins. The six Material 3 palettes,
  Material You dynamic colour, the AMOLED switch and the CRT overlay switch were
  removed. A legacy `DARK` theme choice migrates to Charcoal; everything else lands on
  Cream.
- **Carrier band presets kept** and documented in the README.

### Removed
- **CRT overlay**: the full-screen scanline pass is gone. It drew a horizontal hairline
  every 3dp across the whole viewport, which read as a grid over the interface rather
  than a period detail. The chart keeps its own grid, drawn only inside the chart
  canvas.
- **Chart grid switch**: the grid is now always on inside the charts, so the toggle and
  its stored preference were deleted.

### Fixed
- **Widgets rendered the launcher error layout**: both Glance widgets called
  `stringResource()` inside their composables. That helper reads Compose UI's
  `LocalContext`, which the Glance 1.0.0 runtime never provides, so every composition
  threw and the launcher showed its error box instead of the widget. All widget copy is
  now resolved in `WidgetText.kt` from the real `Context` before composition, and the
  composables only place strings they are handed.
- **A widget can no longer fail as a whole**: the telemetry read is wrapped in
  `loadWidgetSignal()`, which falls back to a resolved offline state, and widget
  preferences fall back to defaults, so a database or keystore fault degrades the
  content instead of replacing the widget with an error box.
- **Encrypted credentials no longer crash the app on start**: the cipher handed the
  keystore a caller-supplied IV, which Android rejects for a key generated with the
  default `setRandomizedEncryptionRequired(true)`. Encryption now lets the provider
  generate the IV and stores it with the ciphertext, so saving a credential works.
- **The 4G plate showed "4G LTLETE BAND 3"**: the router reports the band as
  `LTE BAND 3`, which repeated the plate label. Bands are reduced to their short
  identifier, so the plates read `5G NR n41` and `4G LTE B3`, and the band badge is
  right-aligned instead of colliding with the label.

### Security
- **Router credentials are now encrypted at rest**: the host, username and password
  moved out of `widget_preferences` into their own file, each value sealed with
  AES-256-GCM under an Android keystore key (random IV per value, authenticated
  ciphertext). The login switch travels with them as a plain boolean, since it is a
  setting rather than a secret. Existing plaintext values are migrated once on first
  read and then deleted from the old file.
- **Credential storage fails closed**: if the keystore is unavailable the credentials
  stay in memory for that session, the plaintext copy is left untouched rather than
  deleted, and nothing new is written in the clear.
- **No deprecated crypto dependency**: the storage is a small `AesGcmCipher` over
  `javax.crypto`, so there is no `androidx.security:security-crypto` (deprecated in
  1.1.0) in the dependency list.
- **No credential logging**: no log statement prints a password, token or request body.

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
