package com.example.instagramblocker.domain

data class BlockState(
    val deadlineMs: Long,
    val attempts: Int,
    val attemptsByDay: Map<String, Int> = emptyMap(),
)
