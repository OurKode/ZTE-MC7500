# ZTE MC7500 ODU Signal Monitor & Widget

## Tech Stack
- **Language & Runtime**: Kotlin 1.9.22, Java 17, Android SDK (minSdk 26, targetSdk 34, compileSdk 34)
- **UI Framework**: Jetpack Compose (BOM 2024.02.00), Material 3
- **App Widgets**: Jetpack Glance 1.0.0 (`glance-appwidget`, `glance-material3`)
- **Networking & Serialization**: OkHttp 4.12.0, Kotlinx Serialization JSON 1.6.2 (OpenWrt JSON-RPC ubus)
- **Background & Storage**: AndroidX WorkManager 2.9.0, SQLite (`OduDatabaseHelper`), SharedPreferences / Glance preferences

## Key Gradle Commands
- Build Debug APK: `.\gradlew.bat assembleDebug`
- Build Release APK: `.\gradlew.bat assembleRelease`
- Print Version: `.\gradlew.bat printVersion`
- Bump Version: `.\gradlew.bat bumpPatch` (or `bumpMinor`, `bumpMajor`)
- Lint: `.\gradlew.bat lint`

## Architecture & Code Conventions
- **Package Root**: `com.example.odumonitor`
- **Data Flow**: `OduApiService` (JSON-RPC network) -> `OduRepository` -> `DashboardViewModel` -> `DashboardScreen` (Jetpack Compose).
- **Background Sync**: `OduMonitorService` (foreground service) + `OduSyncWorker` (WorkManager) updating widgets via `WidgetUpdateManager`.
- **Widgets**: Jetpack Glance implementations in `widget/` (`OduCompactWidget`, `OduDetailedWidget`).
- **Telemetry History**: Stored locally in `OduDatabaseHelper` with configurable retention (`HistoryRetention`).
- **Navigation / Code Understanding**: Use CodeGraph (`.codegraph/`) first for symbol resolution and call paths.
- **Router Protocol Source of Truth**: `docs/REFERENCE-SCRIPT.md` maps the upstream `zte-script-ng.js` (present in the repo root but gitignored) to Kotlin symbols. Read it before touching `OduApiService`, `OduRepository`, or `FrequencyConverter`. `docs/API.md` is the protocol spec; if it disagrees with the script, the script wins.

## Boundaries
- Never commit secrets, passwords, or keystores (`release.keystore`, `local.properties`).
- Router credentials must go through `SecureCredentialStore` (AES-256-GCM, Android keystore). Never write a password into `SharedPreferences` directly, never log one, and keep the plaintext migration path in `CredentialMigration` intact.
- Do not hardcode version codes in `build.gradle.kts` (derived dynamically from Git commits and `version.properties`).
- Always test widget update routines against both Glance widgets (`OduCompactWidget` and `OduDetailedWidget`).
- Radio write calls are never exercised against a live ODU; verify them statically and with `UbusPayloadsTest`.
