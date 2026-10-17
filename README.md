# Free VPN for Android TV

A **free** Android TV VPN client built with Kotlin and Jetpack Compose for TV, on a **real**
`android.net.VpnService` foundation. No subscription, no payment screen, no premium plans, no
browsing-history collection — just: open app → (activate device if required) → choose server →
connect for free.

> **MVP status:** the entire connect/disconnect/state/notification flow is real and works on-device.
> The encrypted WireGuard tunnel is stubbed by a **`SimulatedTunnel`** so you can run and test the
> whole app without a WireGuard server. See [TODO.md](TODO.md) to switch on the production tunnel.

---

## Features

- **Welcome → Activation (optional) → Home → Servers → Settings** flow, fully remote/D-pad driven.
- Real `MyVpnService` (`VpnService`): establishes a TUN interface, triggers the **system VPN
  permission dialog** (`VpnService.prepare()`), runs as a **foreground service** with a persistent
  status notification, and exposes the five connection states.
- **Connection states:** `DISCONNECTED · CONNECTING · CONNECTED · DISCONNECTING · ERROR`, surfaced
  as a sealed class and observed by the UI via a single process-wide `StateFlow`.
- **Free-access gating** (`ALLOWED / LIMITED / BLOCKED / SERVER_UNAVAILABLE / MAINTENANCE`) for fair
  usage / abuse protection — **never** a payment prompt.
- **Server selection** with country, city, recommended badge, status, load %, and ping.
- **Connection timer**, selected-location display, and a clear **FREE** badge on the main screen.
- **Privacy-safe operational logging** only (device id, server, timestamps, success/failure, app
  version). No browsing history, DNS, or traffic content is ever collected.
- **Bounded auto-reconnect** (max 3 attempts, exponential backoff) — no infinite loops — plus
  sleep/wake state reconciliation.



## Architecture

```
app/
  ui/{welcome,activation,home,servers,settings}   # Compose screens + ViewModels
  vpn/                                             # MyVpnService, VpnManager, tunnel seam, state
  network/                                         # ApiClient, AuthInterceptor, VpnApi, DTOs
  data/                                            # Server/Device/Vpn/Access repositories + LocalFallback
  storage/                                         # DeviceStorage, TokenStorage, ConfigStorage
  model/                                           # VpnServer, VpnConfig, DeviceActivation, FreeAccessStatus
  AppContainer.kt / AppViewModelFactory.kt / VpnApp.kt / MainActivity.kt
```

State flows one way:
`UI → ViewModel → VpnManager → startForegroundService → MyVpnService`, and back via
`MyVpnService → VpnManager → VpnStateHolder (StateFlow) → UI`. The raw VPN config is handed to the
service **in-process** (never through an Intent) so it can't leak.

---

## Build & run

### Requirements / environment notes (important on this machine)

- Build with **Android Studio's bundled JBR 21**, *not* the system JDK 25 (AGP isn't certified on 25).
  This is pinned in [gradle.properties](gradle.properties) via `org.gradle.java.home`.
- Always use the **Gradle wrapper** (`./gradlew`) — it's pinned to Gradle **8.13**. Do **not** invoke
  the system `gradle` (9.5.1), which isn't certified with AGP 8.7.
- `local.properties` points at `~/Library/Android/sdk`. It is git-ignored; adjust if your SDK differs.

### Commands

```bash
# Build the debug APK (already verified to assemble cleanly)
./gradlew :app:assembleDebug

# Install on a connected Android TV device / emulator
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Or build + install in one step
./gradlew :app:installDebug
```

Open in **Android Studio** for the smoothest experience (it uses its bundled JBR automatically). If
you build from the CLI and see a toolchain/JDK error, confirm `./gradlew --version` reports JVM 21.

### Configuration knobs (`app/build.gradle.kts`)

- `BASE_URL` — backend HTTPS base URL. Currently a **placeholder**
  (`https://api.freevpn.example.com/`). The app is in real-backend mode but degrades to
  `LocalFallback` sample data when the backend is unreachable, so all screens render during dev.
- `USE_SIMULATED_TUNNEL` — `true` (default) uses `SimulatedTunnel`; flip to `false` only after
  wiring `GoBackendTunnel` (see [TODO.md](TODO.md)).
- Auth: requests carry `X-Device-Id` (per-install UUID) and optional `X-Api-Key` via `AuthInterceptor`.

---

## ⚠️ Simulated tunnel — read this

With `USE_SIMULATED_TUNNEL = true`, the app establishes a **real** TUN interface (so the system VPN
key icon, permission dialog, and notification behave exactly like production) **but black-holes all
traffic** — i.e. while "connected" the device has **no internet**. This is intentional: it lets you
exercise the full flow without a WireGuard server. A debug banner on the Home screen says so. Replace
it with the real tunnel before any real-world use ([TODO.md](TODO.md)).

---

## Backend API

| Method & path | Purpose |
|---|---|
| `POST /devices/activate` | Activate device by code (optional; anonymous-free by default) |
| `GET /vpn/servers` | Free server list |
| `POST /vpn/config` | VPN config for the selected server |
| `GET /user/access` | Free access / fair-usage status |
| `POST /vpn/connect-log` | Privacy-safe connect log (fire-and-forget) |
| `POST /vpn/disconnect-log` | Privacy-safe disconnect log (fire-and-forget) |

Unknown enum values from the backend are coerced to safe defaults, so a new server status or access
state won't crash the client.

---

## Testing checklist (device / emulator)

Test on a real Android TV box, the Android TV emulator, and a low-end box if available. Drive
everything with the **remote/D-pad** — confirm focus highlights on every button and card.

1. First launch → Welcome → Get Started.
2. Activation screen appears only if the backend returns a pending code; otherwise straight to Home.
3. Server list loads (falls back to samples if backend down).
4. Select the **Recommended** server.
5. Connect → **system VPN permission dialog** appears → accept.
6. State goes `CONNECTING → CONNECTED`, timer runs, "Free VPN is connected" notification shows.
7. Disconnect → notification removed, state `DISCONNECTED`.
8. Reconnect.
9. Restart the app while connected → state reconciles on resume.
10. Sleep/wake the TV → state reconciles.
11. Backend unavailable → screens still render (fallback), friendly behavior.
12. Force each access state (`LIMITED/BLOCKED/SERVER_UNAVAILABLE/MAINTENANCE`) → correct message,
    connect disabled where required, **no payment prompts**.
13. Deny the VPN permission dialog → friendly error + "Try again".

To force access/server states locally, edit [`Lo calFallback`](app/src/main/java/com/freevpn/androidtv/data/LocalFallback.kt).

---

## Privacy & Play policy

- **No** browsing history, visited sites, DNS queries, or traffic content is collected.
- Only operational metadata is logged (device id, server, connection times, success/failure, app
  version, optional approximate bandwidth for fair usage).
- The VPN service is declared with `foregroundServiceType="specialUse"` + a `vpn` justification
  property (required on Android 14+). Submit the VPN declaration and a clear privacy policy in the
  Play Console. No traffic manipulation, no ads in the connection flow.

See [TODO.md](TODO.md) for the production WireGuard integration steps and remaining hardening.
