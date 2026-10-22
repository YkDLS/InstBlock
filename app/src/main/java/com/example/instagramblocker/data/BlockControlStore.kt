package com.example.instagramblocker.data

import android.content.Context
import com.example.instagramblocker.domain.BlockPolicy
import com.example.instagramblocker.domain.BlockingState

/** Preferência local para pausar o bloqueio sem desligar a acessibilidade do Android. */
class BlockControlStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "block_control",
        Context.MODE_PRIVATE,
    )

    fun isEnabled(): Boolean = preferences.getBoolean(KEY_ENABLED, true)

    fun read(): BlockingState = BlockingState(
        enabled = isEnabled(),
        pausedRemainingMs = preferences.getLong(KEY_PAUSED_REMAINING, NO_PAUSED_TIME)
            .takeUnless { it == NO_PAUSED_TIME },
    )

    fun setEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun pause(nowMs: Long, deadlineMs: Long): Boolean {
        val remainingMs = BlockPolicy.remainingMs(nowMs, deadlineMs)
        return preferences.edit()
            .putBoolean(KEY_ENABLED, false)
            .putLong(KEY_PAUSED_REMAINING, remainingMs)
            .commit()
    }

    fun resume(nowMs: Long): Long {
        val remainingMs = preferences.getLong(KEY_PAUSED_REMAINING, 0L)
        val deadlineMs = if (remainingMs > 0L && nowMs <= Long.MAX_VALUE - remainingMs) {
            nowMs + remainingMs
        } else {
            nowMs
        }
        preferences.edit()
            .putBoolean(KEY_ENABLED, true)
            .putLong(KEY_PAUSED_REMAINING, NO_PAUSED_TIME)
            .commit()
        return deadlineMs
    }

    private companion object {
        const val KEY_ENABLED = "blocking_enabled"
        const val KEY_PAUSED_REMAINING = "paused_remaining_ms"
        const val NO_PAUSED_TIME = -1L
    }
}
