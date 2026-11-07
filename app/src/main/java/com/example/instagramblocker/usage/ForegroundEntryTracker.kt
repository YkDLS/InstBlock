package com.example.instagramblocker.usage

/** Tracks foreground transitions even when the wall clock used by UsageEvents moves backwards. */
internal class ForegroundEntryTracker(private val targetPackage: String) {
    var lastEventTime = 0L
        private set
    private var foregroundPackage: String? = null

    fun resetIfClockMovedBack(nowWallMs: Long) {
        if (nowWallMs < lastEventTime) {
            lastEventTime = 0L
            foregroundPackage = null
        }
    }

    fun observe(eventTime: Long, packageName: String): Boolean {
        if (eventTime <= lastEventTime) return false
        val enteredTarget = packageName == targetPackage && foregroundPackage != targetPackage
        lastEventTime = eventTime
        foregroundPackage = packageName
        return enteredTarget
    }
}
