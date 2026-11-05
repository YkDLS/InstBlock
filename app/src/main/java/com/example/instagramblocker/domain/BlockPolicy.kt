package com.example.instagramblocker.domain

/** A duração começa em zero e cresce em cinco minutos por entrada distinta. */
object BlockPolicy {
    const val STEP_MS = 5 * 60 * 1000L
    const val MAX_MS = 3 * 60 * 60 * 1000L
    const val RELEASE_MS = 10 * 60 * 1000L

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

    fun releaseRemainingMs(nowMs: Long, deadlineMs: Long): Long {
        if (deadlineMs <= 0L || nowMs < deadlineMs) return 0L
        val releaseDeadlineMs = safeAdd(deadlineMs, RELEASE_MS)
        return remainingBetween(nowMs, releaseDeadlineMs)
    }

    fun phase(nowMs: Long, deadlineMs: Long): BlockPhase = when {
        remainingMs(nowMs, deadlineMs) > 0L -> BlockPhase.BLOCKED
        releaseRemainingMs(nowMs, deadlineMs) > 0L -> BlockPhase.RELEASED
        else -> BlockPhase.READY
    }

    fun snapshot(nowMs: Long, phase: BlockPhase, phaseDeadlineMs: Long): CycleSnapshot {
        val remainingMs = remainingBetween(nowMs, phaseDeadlineMs)
        if (remainingMs > 0L) return CycleSnapshot(phase, phaseDeadlineMs, remainingMs)

        if (phase == BlockPhase.BLOCKED) {
            val releaseDeadlineMs = safeAdd(phaseDeadlineMs, RELEASE_MS)
            val releaseRemainingMs = remainingBetween(nowMs, releaseDeadlineMs)
            if (releaseRemainingMs > 0L) {
                return CycleSnapshot(BlockPhase.RELEASED, releaseDeadlineMs, releaseRemainingMs)
            }
        }
        return CycleSnapshot(BlockPhase.READY, 0L, 0L)
    }

    fun restoredDeadline(nowMs: Long, remainingMs: Long): Long =
        safeAdd(nowMs, remainingMs.coerceAtLeast(0L))

    private fun remainingBetween(nowMs: Long, deadlineMs: Long): Long {
        if (deadlineMs <= nowMs) return 0L
        // Avoid overflow when callers provide boundary-value timestamps.
        return if (nowMs < 0L && deadlineMs > Long.MAX_VALUE + nowMs) {
            Long.MAX_VALUE
        } else {
            deadlineMs - nowMs
        }
    }

    private fun safeAdd(valueMs: Long, durationMs: Long): Long =
        if (valueMs > Long.MAX_VALUE - durationMs) Long.MAX_VALUE else valueMs + durationMs
}

enum class BlockPhase {
    BLOCKED,
    RELEASED,
    READY,
}

data class CycleSnapshot(
    val phase: BlockPhase,
    val deadlineMs: Long,
    val remainingMs: Long,
)
