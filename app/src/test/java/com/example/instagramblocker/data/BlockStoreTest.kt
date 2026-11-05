package com.example.instagramblocker.data

import com.example.instagramblocker.domain.BlockPolicy
import com.example.instagramblocker.domain.BlockPhase
import com.example.instagramblocker.domain.BlockState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockStoreTest {
    @Test
    fun firstAttemptIsPersistedWithFiveMinuteDeadlineAndCountOne() {
        val persistence = InMemoryPersistence()
        val store = BlockStore(persistence)

        assertTrue(store.recordAttempt(nowMs = 1_000L, dayKey = "2026-10-21"))

        assertEquals(blockedState(301_000L, 1, mapOf("2026-10-21" to 1), 300_000L), store.read())
    }

    @Test
    fun secondAttemptAddsToRemainingWindowAndIncrementsCumulativeCount() {
        val persistence = InMemoryPersistence()
        val store = BlockStore(persistence)

        store.recordAttempt(nowMs = 0L, dayKey = "2026-10-21")
        store.recordAttempt(nowMs = 60_000L, dayKey = "2026-10-21")

        assertEquals(blockedState(600_000L, 2, mapOf("2026-10-21" to 2), 540_000L), store.read())
    }

    @Test
    fun expiredDeadlineStartsNewFiveMinuteWindowWithoutResettingCount() {
        val persistence = InMemoryPersistence()
        val store = BlockStore(persistence)

        store.recordAttempt(nowMs = 0L, dayKey = "2026-10-21")
        store.recordAttempt(nowMs = 900_000L, dayKey = "2026-10-22")

        assertEquals(blockedState(1_200_000L, 2, mapOf("2026-10-21" to 1, "2026-10-22" to 1), 300_000L), store.read())
        assertEquals(300_000L, BlockPolicy.remainingMs(900_000L, store.read().deadlineMs))
    }

    @Test
    fun deadlineIsCappedAtThreeHoursWhileAttemptsContinueToAccumulate() {
        val persistence = InMemoryPersistence(blockedState(BlockPolicy.MAX_MS, 7, emptyMap(), BlockPolicy.MAX_MS))
        val store = BlockStore(persistence)

        store.recordAttempt(nowMs = 0L, dayKey = "2026-10-21")
        store.recordAttempt(nowMs = 1_000L, dayKey = "2026-10-21")

        assertEquals(1_000L + BlockPolicy.MAX_MS, store.read().deadlineMs)
        assertEquals(9, store.read().attempts)
    }

    @Test
    fun statePersistsAcrossStoreInstancesUsingSamePersistence() {
        val persistence = InMemoryPersistence()
        val firstStore = BlockStore(persistence)
        firstStore.recordAttempt(nowMs = 10_000L, dayKey = "2026-10-21")

        val restartedStore = BlockStore(persistence)

        assertEquals(blockedState(310_000L, 1, mapOf("2026-10-21" to 1), 300_000L), restartedStore.read())
    }

    @Test
    fun failedWriteDoesNotReportAttemptAsSaved() {
        val persistence = InMemoryPersistence(shouldWrite = false)
        val store = BlockStore(persistence)

        assertFalse(store.recordAttempt(nowMs = 0L, dayKey = "2026-10-21"))
        assertEquals(BlockState(0L, 0), store.read())
    }

    @Test
    fun attemptsAreCountedSeparatelyByDay() {
        val persistence = InMemoryPersistence()
        val store = BlockStore(persistence)

        store.recordAttempt(nowMs = 0L, dayKey = "2026-10-21")
        store.recordAttempt(nowMs = 1_000L, dayKey = "2026-10-22")

        assertEquals(mapOf("2026-10-21" to 1, "2026-10-22" to 1), store.read().attemptsByDay)
    }

    @Test
    fun changingWallClockDoesNotChangeRemainingTime() {
        val store = BlockStore(InMemoryPersistence())
        store.recordAttempt(nowMs = 1_000L, bootCount = 4, dayKey = "2026-10-21")

        val before = store.current(nowElapsedMs = 61_000L, bootCount = 4, legacyWallNowMs = 1L)
        val afterClockChange = store.current(
            nowElapsedMs = 61_000L,
            bootCount = 4,
            legacyWallNowMs = Long.MAX_VALUE,
        )

        assertEquals(240_000L, BlockPolicy.remainingMs(61_000L, before.deadlineMs))
        assertEquals(before, afterClockChange)
    }

    @Test
    fun rebootRestoresCheckpointWithoutShorteningWait() {
        val store = BlockStore(InMemoryPersistence())
        store.recordAttempt(nowMs = 0L, bootCount = 7, dayKey = "2026-10-21")
        store.checkpoint(nowElapsedMs = 120_000L, bootCount = 7)

        val restored = store.current(nowElapsedMs = 5_000L, bootCount = 8)

        assertEquals(BlockPhase.BLOCKED, restored.phase)
        assertEquals(180_000L, BlockPolicy.remainingMs(5_000L, restored.deadlineMs))
        assertEquals(180_000L, restored.checkpointRemainingMs)
    }

    @Test
    fun rebootDuringReleasePreservesReleaseRemaining() {
        val persistence = InMemoryPersistence(
            BlockState(
                deadlineMs = 700_000L,
                attempts = 1,
                phase = BlockPhase.RELEASED,
                bootCount = 2,
                checkpointRemainingMs = 400_000L,
            ),
        )
        val restored = BlockStore(persistence).current(nowElapsedMs = 2_000L, bootCount = 3)

        assertEquals(BlockPhase.RELEASED, restored.phase)
        assertEquals(400_000L, BlockPolicy.remainingMs(2_000L, restored.deadlineMs))
    }

    private companion object {
        fun blockedState(
            deadlineMs: Long,
            attempts: Int,
            attemptsByDay: Map<String, Int>,
            checkpointRemainingMs: Long,
        ) = BlockState(
            deadlineMs = deadlineMs,
            attempts = attempts,
            attemptsByDay = attemptsByDay,
            phase = BlockPhase.BLOCKED,
            bootCount = 0,
            checkpointRemainingMs = checkpointRemainingMs,
        )
    }

    private class InMemoryPersistence(
        private var state: BlockState = BlockState(0L, 0),
        private val shouldWrite: Boolean = true,
    ) : BlockStatePersistence {
        override fun read(): BlockState = state

        override fun write(state: BlockState): Boolean {
            if (shouldWrite) this.state = state
            return shouldWrite
        }
    }
}
