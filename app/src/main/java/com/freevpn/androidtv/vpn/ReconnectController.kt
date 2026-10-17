package com.freevpn.androidtv.vpn

/**
 * Bounded exponential backoff for automatic reconnects. Hard caps the number of attempts so a
 * failing tunnel can NEVER produce an infinite reconnect loop. Call [reset] on a successful connect.
 *
 * Not thread-safe by design — it is only ever touched from [VpnManager]'s single coroutine scope.
 */
class ReconnectController(
    private val maxAttempts: Int = 3,
    private val baseDelayMs: Long = 2_000L,
    private val maxDelayMs: Long = 30_000L,
) {
    private var attempts = 0

    fun reset() {
        attempts = 0
    }

    /** @return the delay before the next attempt, or null if the attempt budget is exhausted. */
    fun nextDelayMs(): Long? {
        if (attempts >= maxAttempts) return null
        val delay = (baseDelayMs shl attempts).coerceAtMost(maxDelayMs)
        attempts++
        return delay
    }

    val isExhausted: Boolean get() = attempts >= maxAttempts
}
