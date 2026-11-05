package com.example.instagramblocker.data

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

object SystemTimer {
    fun elapsedRealtimeMs(): Long = SystemClock.elapsedRealtime()

    fun bootCount(context: Context): Int = Settings.Global.getInt(
        context.contentResolver,
        Settings.Global.BOOT_COUNT,
        0,
    )
}
