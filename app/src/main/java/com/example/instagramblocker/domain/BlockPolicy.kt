package com.example.instagramblocker.domain

/** A duração começa em zero e cresce em cinco minutos por entrada distinta. */
object BlockPolicy {
    const val STEP_MS = 5 * 60 * 1000L
    const val MAX_MS = 3 * 60 * 60 * 1000L

    fun nextDeadline(nowMs: Long, previousDeadlineMs: Long): Long {
        val remainingMs = remainingBetween(nowMs, previousDeadlineMs)
        val durationMs = if (remainingMs > MAX_MS - STEP_MS) {
            MAX_MS
        } else {
            (remainingMs + STEP_MS).coerceAtMost(MAX_MS)
        }
        // Keep the persisted deadline valid even if the clock value is close to
        // Long.MAX_VALUE (for example in a boundary-value test).
        return if (nowMs > Long.MAX_VALUE - durationMs) Long.MAX_VALUE else nowMs + durationMs
    }

    fun remainingMs(nowMs: Long, deadlineMs: Long): Long =
        remainingBetween(nowMs, deadlineMs)

    private fun remainingBetween(nowMs: Long, deadlineMs: Long): Long {
        if (deadlineMs <= nowMs) return 0L
        // Avoid overflow when callers provide boundary-value timestamps.
        return if (nowMs < 0L && deadlineMs > Long.MAX_VALUE + nowMs) {
            Long.MAX_VALUE
        } else {
            deadlineMs - nowMs
        }
    }
}
