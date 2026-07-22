# ZTE MC7500 ODU Signal Monitor & Widget

A high-performance Android application & Homescreen Widget (Jetpack Glance) built for monitoring ZTE MC7500 4G/5G Outdoor Units (ODUs) in real-time.

## Features
- **JSON-RPC Telemetry Engine**: Unauthenticated polling directly to `http://192.168.254.1/ubus/`.
- **Ethereal OLED Dark Aesthetics**: Double-Bezel nested architecture, high-contrast signal gauges for 5G NR and 4G LTE.
- **Configurable Refresh Intervals**: Dynamic 1s, 3s, and 5s polling options.
- **Glance AppWidgets**:
  - `OduCompactWidget` (2x2): Key 5G RSRP/SINR indicators.
  - `OduDetailedWidget` (4x2): Dual 5G/4G metrics, band information, and tower timestamps.
- **Live / Demo Mode Toggle**: Built-in simulator mode for testing visual states offline.

## Project Structure
- `app/src/main/java/com/example/odumonitor/data/`: Data models (`OduModels.kt`), OkHttp remote service (`OduApiService.kt`), repository (`OduRepository.kt`).
- `app/src/main/java/com/example/odumonitor/ui/`: Jetpack Compose UI (`DashboardScreen.kt`, `DashboardViewModel.kt`, Theme files).
- `app/src/main/java/com/example/odumonitor/widget/`: Glance widgets (`OduCompactWidget.kt`, `OduDetailedWidget.kt`).
- `app/src/main/java/com/example/odumonitor/worker/`: Background sync worker (`OduSyncWorker.kt`).
