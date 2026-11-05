package com.example.instagramblocker.data

import android.content.Context
import android.content.SharedPreferences
import com.example.instagramblocker.domain.BlockPolicy
import com.example.instagramblocker.domain.BlockPhase
import com.example.instagramblocker.domain.BlockState
import com.example.instagramblocker.domain.UNKNOWN_BOOT

class BlockStore(private val persistence: BlockStatePersistence) {
    constructor(context: Context) : this(
        SharedPreferencesBlockStatePersistence(
            context.applicationContext.getSharedPreferences("block_state", Context.MODE_PRIVATE),
        ),
    )

    fun read(): BlockState = persistence.read()

    fun current(
        nowElapsedMs: Long,
        bootCount: Int,
        legacyWallNowMs: Long = System.currentTimeMillis(),
    ): BlockState {
        val saved = read()
        val restored = when {
            saved.bootCount == bootCount -> normalized(saved, nowElapsedMs)
            saved.bootCount == UNKNOWN_BOOT -> migrateLegacy(saved, nowElapsedMs, bootCount, legacyWallNowMs)
            else -> restoreAfterBoot(saved, nowElapsedMs, bootCount)
        }
        if (restored != saved) persistence.write(restored)
        return restored
    }

    fun checkpoint(nowElapsedMs: Long, bootCount: Int): BlockState {
        val current = current(nowElapsedMs, bootCount)
        val snapshot = BlockPolicy.snapshot(nowElapsedMs, current.phase, current.deadlineMs)
        val checkpointed = current.copy(
            deadlineMs = snapshot.deadlineMs,
            phase = snapshot.phase,
            bootCount = bootCount,
            checkpointRemainingMs = snapshot.remainingMs,
        )
        if (checkpointed != current) persistence.write(checkpointed)
        return checkpointed
    }

    /** Called from one service queue; persist before dismissing Instagram. */
    fun recordAttempt(
        nowMs: Long,
        bootCount: Int = 0,
        dayKey: String = java.time.LocalDate.now().toString(),
    ): Boolean {
        val previous = current(nowMs, bootCount)
        if (previous.phase == BlockPhase.RELEASED) return false
        val updatedDaily = previous.attemptsByDay.toMutableMap()
        val todayAttempts = updatedDaily[dayKey] ?: 0
        updatedDaily[dayKey] = if (todayAttempts == Int.MAX_VALUE) {
            Int.MAX_VALUE
        } else {
            todayAttempts + 1
        }
        val nextDeadlineMs = BlockPolicy.nextDeadline(
            nowMs,
            previous.deadlineMs.takeIf { previous.phase == BlockPhase.BLOCKED } ?: 0L,
        )
        val next = BlockState(
            deadlineMs = nextDeadlineMs,
            attempts = if (previous.attempts == Int.MAX_VALUE) {
                Int.MAX_VALUE
            } else {
                previous.attempts + 1
            },
            attemptsByDay = updatedDaily,
            phase = BlockPhase.BLOCKED,
            bootCount = bootCount,
            checkpointRemainingMs = BlockPolicy.remainingMs(nowMs, nextDeadlineMs),
        )
        return persistence.write(next)
    }

    private fun normalized(state: BlockState, nowElapsedMs: Long): BlockState {
        val snapshot = BlockPolicy.snapshot(nowElapsedMs, state.phase, state.deadlineMs)
        val transitioned = snapshot.phase != state.phase || snapshot.deadlineMs != state.deadlineMs
        return state.copy(
            deadlineMs = snapshot.deadlineMs,
            phase = snapshot.phase,
            checkpointRemainingMs = if (transitioned) snapshot.remainingMs else state.checkpointRemainingMs,
        )
    }

    private fun restoreAfterBoot(state: BlockState, nowElapsedMs: Long, bootCount: Int): BlockState {
        val remainingMs = state.checkpointRemainingMs.coerceAtLeast(0L)
        val phase = if (remainingMs > 0L) state.phase else BlockPhase.READY
        return state.copy(
            deadlineMs = if (phase == BlockPhase.READY) 0L else BlockPolicy.restoredDeadline(nowElapsedMs, remainingMs),
            phase = phase,
            bootCount = bootCount,
            checkpointRemainingMs = remainingMs,
        )
    }

    private fun migrateLegacy(
        state: BlockState,
        nowElapsedMs: Long,
        bootCount: Int,
        wallNowMs: Long,
    ): BlockState {
        if (state.deadlineMs <= 0L) return state.copy(bootCount = bootCount)
        val blockRemainingMs = BlockPolicy.remainingMs(wallNowMs, state.deadlineMs)
        if (blockRemainingMs > 0L) {
            return state.copy(
                deadlineMs = BlockPolicy.restoredDeadline(nowElapsedMs, blockRemainingMs),
                phase = BlockPhase.BLOCKED,
                bootCount = bootCount,
                checkpointRemainingMs = blockRemainingMs,
            )
        }
        val releaseRemainingMs = BlockPolicy.releaseRemainingMs(wallNowMs, state.deadlineMs)
        return if (releaseRemainingMs > 0L) {
            state.copy(
                deadlineMs = BlockPolicy.restoredDeadline(nowElapsedMs, releaseRemainingMs),
                phase = BlockPhase.RELEASED,
                bootCount = bootCount,
                checkpointRemainingMs = releaseRemainingMs,
            )
        } else {
            state.copy(deadlineMs = 0L, phase = BlockPhase.READY, bootCount = bootCount)
        }
    }
}

private class SharedPreferencesBlockStatePersistence(
    private val preferences: SharedPreferences,
) : BlockStatePersistence {
    override fun read(): BlockState = BlockState(
        deadlineMs = preferences.getLong(KEY_DEADLINE, 0L),
        attempts = preferences.getInt(KEY_ATTEMPTS, 0),
        attemptsByDay = preferences.getString(KEY_DAILY, "").orEmpty()
            .split('|')
            .mapNotNull { entry ->
                val parts = entry.split('=', limit = 2)
                if (parts.size != 2) return@mapNotNull null
                val count = parts[1].toIntOrNull() ?: return@mapNotNull null
                parts[0] to count.coerceAtLeast(0)
            }
            .toMap(),
        phase = preferences.getString(KEY_PHASE, null)
            ?.let { stored -> BlockPhase.entries.firstOrNull { it.name == stored } }
            ?: BlockPhase.READY,
        bootCount = preferences.getInt(KEY_BOOT_COUNT, UNKNOWN_BOOT),
        checkpointRemainingMs = preferences.getLong(KEY_CHECKPOINT_REMAINING, 0L).coerceAtLeast(0L),
    )

    override fun write(state: BlockState): Boolean = preferences.edit()
        .putLong(KEY_DEADLINE, state.deadlineMs)
        .putInt(KEY_ATTEMPTS, state.attempts)
        .putString(KEY_PHASE, state.phase.name)
        .putInt(KEY_BOOT_COUNT, state.bootCount)
        .putLong(KEY_CHECKPOINT_REMAINING, state.checkpointRemainingMs)
        .putString(
            KEY_DAILY,
            state.attemptsByDay.entries.joinToString("|") { "${it.key}=${it.value}" },
        )
        .commit()

    private companion object {
        const val KEY_DEADLINE = "deadline_ms"
        const val KEY_ATTEMPTS = "attempts"
        const val KEY_DAILY = "attempts_by_day"
        const val KEY_PHASE = "timer_phase"
        const val KEY_BOOT_COUNT = "timer_boot_count"
        const val KEY_CHECKPOINT_REMAINING = "timer_checkpoint_remaining_ms"
    }
}
