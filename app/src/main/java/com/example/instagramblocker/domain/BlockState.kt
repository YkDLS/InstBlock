package com.example.instagramblocker.domain

data class BlockState(
    val deadlineMs: Long,
    val attempts: Int,
    val attemptsByDay: Map<String, Int> = emptyMap(),
    val phase: BlockPhase = BlockPhase.READY,
    val bootCount: Int = UNKNOWN_BOOT,
    val checkpointRemainingMs: Long = 0L,
)

const val UNKNOWN_BOOT = -1
