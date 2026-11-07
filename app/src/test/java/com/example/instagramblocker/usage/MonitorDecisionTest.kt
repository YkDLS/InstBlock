package com.example.instagramblocker.usage

import com.example.instagramblocker.domain.BlockPhase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitorDecisionTest {
    @Test fun blocksWhenReleaseExpiresWhileInstagramRemainsOpen() {
        assertTrue(MonitorDecision.shouldRecordAttempt(BlockPhase.READY, true, false, true, true))
    }

    @Test fun neverBlocksDuringReleaseOrOverAnotherApp() {
        assertFalse(MonitorDecision.shouldRecordAttempt(BlockPhase.RELEASED, true, true, true, true))
        assertFalse(MonitorDecision.shouldRecordAttempt(BlockPhase.READY, false, false, true, true))
    }

    @Test fun waitsUntilScreenIsUsableAndDeduplicatesBlockedEntries() {
        assertFalse(MonitorDecision.shouldRecordAttempt(BlockPhase.READY, true, false, false, true))
        assertFalse(MonitorDecision.shouldRecordAttempt(BlockPhase.BLOCKED, true, false, true, true))
        assertFalse(MonitorDecision.shouldRecordAttempt(BlockPhase.BLOCKED, true, true, true, false))
        assertTrue(MonitorDecision.shouldRecordAttempt(BlockPhase.BLOCKED, true, true, true, true))
    }
}
