package com.example.instagramblocker.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BlockPolicyTest {
    @Test fun firstAttemptStartsFiveMinuteWindow() {
        assertEquals(300_000L, BlockPolicy.nextDeadline(0L, 0L))
    }

    @Test fun newAttemptAddsFiveMinutesToCurrentRemainingTime() {
        val first = BlockPolicy.nextDeadline(0L, 0L)
        assertEquals(600_000L, BlockPolicy.nextDeadline(60_000L, first))
    }

    @Test fun remainingTimeNeverExceedsThreeHours() {
        val deadline = BlockPolicy.nextDeadline(0L, BlockPolicy.MAX_MS)
        assertEquals(BlockPolicy.MAX_MS, deadline)
    }

    @Test fun expiredBlockRestartsAtFiveMinutesAndRemainingIsZeroBeforeNewAttempt() {
        assertEquals(900_000L, BlockPolicy.nextDeadline(600_000L, 300_000L))
        assertEquals(0L, BlockPolicy.remainingMs(600_000L, 300_000L))
    }

    @Test fun remainingTimeTracksNowAndNeverBecomesNegative() {
        assertEquals(240_000L, BlockPolicy.remainingMs(60_000L, 300_000L))
        assertEquals(0L, BlockPolicy.remainingMs(301_000L, 300_000L))
    }

    @Test fun completedBlockOpensTenMinuteReleaseWindow() {
        val deadline = 300_000L
        assertEquals(BlockPhase.BLOCKED, BlockPolicy.phase(299_999L, deadline))
        assertEquals(BlockPhase.RELEASED, BlockPolicy.phase(deadline, deadline))
        assertEquals(600_000L, BlockPolicy.releaseRemainingMs(deadline, deadline))
        assertEquals(1L, BlockPolicy.releaseRemainingMs(899_999L, deadline))
    }

    @Test fun releaseWindowEndsAfterTenContinuousMinutes() {
        val deadline = 300_000L
        assertEquals(BlockPhase.READY, BlockPolicy.phase(900_000L, deadline))
        assertEquals(0L, BlockPolicy.releaseRemainingMs(900_000L, deadline))
    }

    @Test fun cleanInstallDoesNotStartWithAReleaseWindow() {
        assertEquals(BlockPhase.READY, BlockPolicy.phase(0L, 0L))
    }

    @Test fun snapshotMovesFromBlockToReleaseUsingMonotonicDeadline() {
        val snapshot = BlockPolicy.snapshot(300_000L, BlockPhase.BLOCKED, 300_000L)
        assertEquals(BlockPhase.RELEASED, snapshot.phase)
        assertEquals(900_000L, snapshot.deadlineMs)
        assertEquals(600_000L, snapshot.remainingMs)
    }

    @Test fun snapshotMovesFromReleaseBackToReady() {
        val snapshot = BlockPolicy.snapshot(900_000L, BlockPhase.RELEASED, 900_000L)
        assertEquals(CycleSnapshot(BlockPhase.READY, 0L, 0L), snapshot)
    }
}
