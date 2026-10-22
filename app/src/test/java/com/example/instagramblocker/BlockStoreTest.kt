package com.example.instagramblocker

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

        assertEquals(BlockState(301_000L, 1, mapOf("2026-10-21" to 1)), store.read())
    }

    @Test
    fun secondAttemptAddsToRemainingWindowAndIncrementsCumulativeCount() {
        val persistence = InMemoryPersistence()
        val store = BlockStore(persistence)

        store.recordAttempt(nowMs = 0L, dayKey = "2026-10-21")
        store.recordAttempt(nowMs = 60_000L, dayKey = "2026-10-21")

        assertEquals(BlockState(600_000L, 2, mapOf("2026-10-21" to 2)), store.read())
    }

    @Test
    fun expiredDeadlineStartsNewFiveMinuteWindowWithoutResettingCount() {
        val persistence = InMemoryPersistence()
        val store = BlockStore(persistence)

        store.recordAttempt(nowMs = 0L, dayKey = "2026-10-21")
        store.recordAttempt(nowMs = 600_000L, dayKey = "2026-10-22")

        assertEquals(BlockState(900_000L, 2, mapOf("2026-10-21" to 1, "2026-10-22" to 1)), store.read())
        assertEquals(300_000L, BlockPolicy.remainingMs(600_000L, store.read().deadlineMs))
    }

    @Test
    fun deadlineIsCappedAtThreeHoursWhileAttemptsContinueToAccumulate() {
        val persistence = InMemoryPersistence(BlockState(BlockPolicy.MAX_MS, 7))
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

        assertEquals(BlockState(310_000L, 1, mapOf("2026-10-21" to 1)), restartedStore.read())
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
