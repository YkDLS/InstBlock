package com.example.instagramblocker

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
}
