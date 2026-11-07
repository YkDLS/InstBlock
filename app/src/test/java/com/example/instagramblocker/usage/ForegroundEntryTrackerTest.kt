package com.example.instagramblocker.usage

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundEntryTrackerTest {
    @Test fun countsDistinctEntriesWithoutRepeatingTheSameForegroundEvent() {
        val tracker = ForegroundEntryTracker("com.instagram.android")
        assertTrue(tracker.observe(100L, "com.instagram.android"))
        assertFalse(tracker.observe(100L, "com.instagram.android"))
        assertFalse(tracker.observe(101L, "com.instagram.android"))
        assertFalse(tracker.observe(102L, "com.example.other"))
        assertTrue(tracker.observe(103L, "com.instagram.android"))
        assertTrue(tracker.isTargetForeground)
        tracker.clearForeground()
        assertFalse(tracker.isTargetForeground)
        assertTrue(tracker.observe(104L, "com.instagram.android"))
    }

    @Test fun newEntriesAreSeenAfterWallClockMovesBackwards() {
        val tracker = ForegroundEntryTracker("com.instagram.android")
        assertTrue(tracker.observe(100_000L, "com.instagram.android"))
        tracker.resetIfClockMovedBack(10_000L)
        assertFalse(tracker.observe(10_001L, "com.example.other"))
        assertTrue(tracker.observe(10_002L, "com.instagram.android"))
    }
}
