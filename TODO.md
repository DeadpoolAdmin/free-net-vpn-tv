# TODO — Production WireGuard integration & hardening

The MVP ships a **real** `VpnService` + a clean tunnel seam, but the actual encrypted tunnel is a
`SimulatedTunnel`. This file lists what's needed to make it a real, production tunnel and to harden
the app for release.

---

## 1. Wire the real WireGuard tunnel (the big one)

Today `AppContainer.tunnelManager` is chosen by `BuildConfig.USE_SIMULATED_TUNNEL`. To go real:

1. **Add the library.** Uncomment in `app/build.gradle.kts`:
   ```kotlin
   implementation(libs.wireguard.tunnel)   // com.wireguard.android:tunnel
   ```
2. **Flip the flag.** Set `USE_SIMULATED_TUNNEL = false` (build config field). `AppContainer` then
   uses `GoBackendTunnel`.
3. **Implement `GoBackendTunnel.up()/down()`** (`app/src/main/java/com/freevpn/androidtv/vpn/GoBackendTunnel.kt`).
   Pick **one** of these — never run two `VpnService`s at once:

   **Option A — let GoBackend own the tunnel (simplest):**
   - `GoBackend` bundles its **own** `com.wireguard.android.backend.GoBackend$VpnService`. Declare
     **that** service in `AndroidManifest.xml` (with `BIND_VPN_SERVICE` + the same `specialUse`
     FGS type) and **retire `MyVpnService`** (or keep it only as a thin controller). GoBackend then
     owns the TUN + its own foreground notification.
   - Bring up: `Config.parse(...)` the config string → `backend.setState(tunnel, Tunnel.State.UP, cfg)`.

   **Option B — keep `MyVpnService` (more control over notification/state):**
   - Parse the WireGuard config (`com.wireguard.config.Config.parse`).
   - Configure the passed-in `VpnService.Builder` from the `[Interface]` address/DNS and `[Peer]`
     `AllowedIPs` routes, then `builder.establish()` to get the TUN fd.
   - Hand that fd to the WireGuard Go userspace (`wgTurnOn`, the same path `GoBackend` uses
     internally). This keeps `MyVpnService` as the single tunnel owner.

4. **Remove the config fallback in real mode.** `VpnRepository.getConfig()` already re-throws when
   `USE_SIMULATED_TUNNEL == false` (a placeholder config would never parse). Verify `/vpn/config`
   returns a valid `wg-quick` config string.

5. **Update the manifest comment** that documents the two-VpnService rule.

## 2. Backend

- [ ] Replace the placeholder `BASE_URL` with the real HTTPS endpoint.
- [ ] Confirm the activation contract (does `POST /devices/activate` issue a code then return
      `ACTIVATED` on repeat calls? or is there a separate poll endpoint?). Adjust `DeviceRepository`
      / `ActivationViewModel` polling accordingly.
- [ ] Decide fail-open vs fail-closed for `/user/access` when the backend is down (currently
      fail-open = ALLOWED in `AccessRepository`).
- [ ] Confirm auth header names (`X-Device-Id` / `X-Api-Key`) match the backend.

## 3. Reconnect / connectivity (real-tunnel correctness)

- [ ] With a real tunnel, revisit the `ConnectivityManager` callback: a VPN is itself a network, so
      validate onLost/onAvailable behavior to avoid false reconnects. The bounded `ReconnectController`
      is in place; verify it interacts correctly with the real tunnel's own state.
- [ ] Add a real tunnel health check for `VpnManager.reconcile()` (currently relies on the
      `tunnelActive` flag) so sleep/wake recovery is accurate.
- [ ] Consider whether to auto-reconnect after `onRevoke()` (usually no — another VPN took over).

## 4. Notifications & settings

- [ ] The "Show connection status notification" setting can't fully remove a foreground-service
      notification on Android O+. Decide on minimal-notification behavior when the toggle is off
      (lower importance / generic text) vs. documenting the limitation.
- [ ] Localize all strings (currently English only in `res/values/strings.xml`).

## 5. Assets & branding

- [ ] Replace placeholder vector art: `res/drawable/app_banner.xml` (320×180 TV banner),
      `ic_launcher_foreground/background`, and `ic_vpn_status` notification icon.
- [ ] Provide a real privacy policy URL and wire the Settings "Privacy policy" entry to it.

## 6. Release hardening

- [ ] Add a release signing config (currently only debug).
- [ ] Verify R8/ProGuard release build (`./gradlew :app:assembleRelease`) — rules for
      kotlinx.serialization + Retrofit are in `proguard-rules.pro`; test end-to-end after shrinking.
- [ ] Re-evaluate `androidx.security:security-crypto` (deprecated). It's isolated behind the
      `*Storage` interfaces, so swapping to Tink or DataStore-with-manual-crypto is a one-file change.
- [ ] Confirm the Android 14+ `specialUse` FGS justification is submitted in Play Console along with
      the VPN declaration and privacy policy.

## 7. Tests

- [ ] Unit tests for `ReconnectController` (backoff caps, reset-on-success — no infinite loops).
- [ ] Unit tests for DTO → domain mapping incl. unknown enum coercion.
- [ ] Instrumented test for the connect/permission/notification flow against `SimulatedTunnel`.
