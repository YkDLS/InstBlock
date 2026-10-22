package com.example.instagramblocker.data

import android.content.Context
import android.content.SharedPreferences
import com.example.instagramblocker.domain.BlockPolicy
import com.example.instagramblocker.domain.BlockState

class BlockStore(private val persistence: BlockStatePersistence) {
    constructor(context: Context) : this(
        SharedPreferencesBlockStatePersistence(
            context.applicationContext.getSharedPreferences("block_state", Context.MODE_PRIVATE),
        ),
    )

    fun read(): BlockState = persistence.read()

    fun restoreDeadline(deadlineMs: Long): Boolean = persistence.write(read().copy(deadlineMs = deadlineMs))

    /** Called from one service queue; persist before dismissing Instagram. */
    fun recordAttempt(nowMs: Long, dayKey: String = java.time.LocalDate.now().toString()): Boolean {
        val previous = read()
        val updatedDaily = previous.attemptsByDay.toMutableMap()
        val todayAttempts = updatedDaily[dayKey] ?: 0
        updatedDaily[dayKey] = if (todayAttempts == Int.MAX_VALUE) {
            Int.MAX_VALUE
        } else {
            todayAttempts + 1
        }
        val next = BlockState(
            deadlineMs = BlockPolicy.nextDeadline(nowMs, previous.deadlineMs),
            attempts = if (previous.attempts == Int.MAX_VALUE) {
                Int.MAX_VALUE
            } else {
                previous.attempts + 1
            },
            attemptsByDay = updatedDaily,
        )
        return persistence.write(next)
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
    )

    override fun write(state: BlockState): Boolean = preferences.edit()
        .putLong(KEY_DEADLINE, state.deadlineMs)
        .putInt(KEY_ATTEMPTS, state.attempts)
        .putString(
            KEY_DAILY,
            state.attemptsByDay.entries.joinToString("|") { "${it.key}=${it.value}" },
        )
        .commit()

    private companion object {
        const val KEY_DEADLINE = "deadline_ms"
        const val KEY_ATTEMPTS = "attempts"
        const val KEY_DAILY = "attempts_by_day"
    }
}
