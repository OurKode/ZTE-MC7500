# Material 3 and Material You Redesign

> **Status: superseded.** The Material 3 / Material You pass shipped in 1.3.0 and was
> then replaced by the retro field-instrument interface described in the README. This
> document is kept as the record of that decision; dynamic colour, the six M3 palettes
> and the AMOLED switch no longer exist in the app.

## Problem Statement

How do we modernise the ZTE ODU Monitor into a current Android Material 3 / Material
You app (dynamic colour, adaptive light and dark mode, native M3 components) without
sacrificing the legibility of the technical RF metrics (RSRP, SINR, throughput) or the
stability of the gateway communication?

## Recommended Direction

Modernise in stages:

1. **Dynamic theming core**: enable `dynamicLightColorScheme()` and
   `dynamicDarkColorScheme()` on Android 12+ (API 31+), with a custom M3 palette
   fallback (light and dark) for Android 8.0-11 (API 26-30).
2. **Semantic signal colour matrix**: give the signal colours (excellent = emerald,
   fair = amber, poor = coral) on-surface and container variants with enough contrast to
   meet WCAG AA in both light and dark mode.
3. **M3 modular navigation**: split `DashboardScreen.kt` (2,273 lines) into a `Scaffold`
   plus a three-tab `NavigationBar`:
   - **Monitor**: live telemetry, dual gauges (RSRP/SINR), throughput chart, WAN/SIM
     info.
   - **Radio**: bearer switch (`SegmentedButton`), band lock (`FilterChip`), cell lock.
   - **Settings**: quota, notifications, router credentials, widget preview.
4. **Widget harmony**: update `OduCompactWidget` and `OduDetailedWidget` to use
   `GlanceTheme.colors`.

## Key Assumptions to Validate

- [ ] RF signal metrics stay readable in direct sunlight with light mode active.
- [ ] Splitting `DashboardScreen.kt` into three tabs does not break background polling
      or the shared `DashboardViewModel` state.
- [ ] Dynamic colour in Glance widgets does not add render delay on third-party
      launchers.

## MVP Scope (Stage 1)

- **In scope**:
  - `Theme.kt` configuration (dynamic colour plus light and dark `ColorScheme`).
  - `Color.kt` adjustments for the semantic signal status tokens.
  - A `Scaffold` with `TopAppBar` and a basic M3 `NavigationBar`.
  - Migrating the main live telemetry cards to `ElevatedCard` and `OutlinedCard`.
- **Out of scope**:
  - Custom tab transition animations (use the default M3 fade and slide).
  - A full redesign of the throughput chart canvas (match the line colour to
    `colorScheme.primary`).

## Not Doing (and Why)

- **Removing dark mode entirely**: people monitoring a network at night need low-glare
  contrast; dark mode stays a first-class priority.
- **Rewriting the ubus JSON-RPC polling logic**: this pass touches the UI layer only;
  the data layer and repository stay intact.
- **Additional third-party UI libraries**: use only the official
  `androidx.compose.material3` packages.

## Open Questions

- Should the last active tab be persisted in `DataStore` or `SharedPreferences` so the
  app reopens where the user left it?
