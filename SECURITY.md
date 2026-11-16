# Security Policy

## Supported Versions

Only the latest release receives fixes. The app has no server component, so a fix ships
as a new APK.

| Version | Supported |
| --- | --- |
| 1.3.x and newer | yes |
| older | no |

## Reporting a Vulnerability

Please use GitHub's private vulnerability reporting:

1. Open the repository's **Security** tab.
2. Choose **Report a vulnerability**.

Do not open a public issue for a security problem, and do not attach a real router
password, serial number or SIM identifier to a report. A sanitised request/response pair
is enough.

Expect an initial reply within a few days. This is a single-maintainer project, so there
is no formal SLA.

## What the App Stores

| Data | Where | Protection |
| --- | --- | --- |
| Router host, username, password, login switch | `secure_router_credentials` | AES-256-GCM per value, key in the Android keystore, random IV, authenticated ciphertext |
| Skin, chart toggles, polling interval, tab, band selections | `widget_preferences` | Ordinary app-private storage, no secrets |
| Telemetry history (throughput, RSRP/SINR/RSRQ) | Local SQLite | App-private storage, retention is user-configurable |
| Cached widget data | Glance state | App-private storage |

The app performs no analytics, crash reporting or third-party network calls. The only
network peer is the ZTE MC7500 on the local network, or `192.168.254.1` by default.

## Known Limitations

- **Cleartext HTTP to the router.** The device exposes a local web API over plain HTTP.
  `network_security_config.xml` restricts cleartext to the gateway address, but traffic
  on the LAN is unencrypted by design of the firmware.
- **Router-side session token.** Login returns a session token held in memory only. It
  is never persisted.
- **Radio write calls** are authenticated with the configured router credential. A
  compromised router means compromised band configuration; rotate the ODU admin password
  if the device has ever been exposed.
- **Backups are disabled**, so a device migration requires re-entering credentials.

## Out of Scope

- Vulnerabilities in the ZTE MC7500 firmware itself. Report those to the vendor.
- Physical access to an unlocked device.
- Band or cell lock payloads being able to disconnect the ODU. That is the documented
  behaviour of the hardware, and the UI already asks for confirmation.
