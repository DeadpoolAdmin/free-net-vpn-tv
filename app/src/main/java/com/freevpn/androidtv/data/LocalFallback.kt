package com.freevpn.androidtv.data

import com.freevpn.androidtv.model.FreeAccessStatus
import com.freevpn.androidtv.model.ServerStatus
import com.freevpn.androidtv.model.VpnConfig
import com.freevpn.androidtv.model.VpnProtocol
import com.freevpn.androidtv.model.VpnServer

/**
 * Thin local data source used when the backend is unreachable so every screen still renders during
 * development. This is NOT a cache and NOT a substitute for the backend — it returns a fixed sample
 * set. The placeholder config is inert (the SimulatedTunnel ignores its contents).
 */
object LocalFallback {

    fun servers(): List<VpnServer> = listOf(
        // Multiple cities per country so the Locations screen shows real country -> sub-location groups.
        VpnServer("de-frankfurt-1", "Germany", "Frankfurt", isRecommended = true, status = ServerStatus.AVAILABLE, loadPercent = 42, pingMs = 38),
        VpnServer("de-berlin-1", "Germany", "Berlin", status = ServerStatus.AVAILABLE, loadPercent = 51, pingMs = 41),
        VpnServer("de-munich-1", "Germany", "Munich", status = ServerStatus.BUSY, loadPercent = 84, pingMs = 46),
        VpnServer("nl-amsterdam-1", "Netherlands", "Amsterdam", status = ServerStatus.AVAILABLE, loadPercent = 55, pingMs = 44),
        VpnServer("nl-rotterdam-1", "Netherlands", "Rotterdam", status = ServerStatus.AVAILABLE, loadPercent = 33, pingMs = 48),
        VpnServer("us-newyork-1", "United States", "New York", status = ServerStatus.BUSY, loadPercent = 88, pingMs = 120),
        VpnServer("us-losangeles-1", "United States", "Los Angeles", status = ServerStatus.AVAILABLE, loadPercent = 61, pingMs = 155),
        VpnServer("us-miami-1", "United States", "Miami", status = ServerStatus.AVAILABLE, loadPercent = 47, pingMs = 138),
        VpnServer("gb-london-1", "United Kingdom", "London", status = ServerStatus.AVAILABLE, loadPercent = 60, pingMs = 52),
        VpnServer("gb-manchester-1", "United Kingdom", "Manchester", status = ServerStatus.AVAILABLE, loadPercent = 39, pingMs = 58),
        VpnServer("fr-paris-1", "France", "Paris", status = ServerStatus.AVAILABLE, loadPercent = 35, pingMs = 47),
        VpnServer("fr-marseille-1", "France", "Marseille", status = ServerStatus.BUSY, loadPercent = 82, pingMs = 53),
        VpnServer("se-stockholm-1", "Sweden", "Stockholm", status = ServerStatus.UNAVAILABLE, loadPercent = 100, pingMs = null),
    )

    fun access(): FreeAccessStatus = FreeAccessStatus.ALLOWED

    /** Inert placeholder config. Only valid for the SimulatedTunnel. */
    fun config(serverId: String): VpnConfig = VpnConfig(
        serverId = serverId,
        protocol = VpnProtocol.WIREGUARD,
        configContent = "# SIMULATED PLACEHOLDER CONFIG — not a real tunnel",
    )
}
