package com.example.instagramblocker.usage

import com.example.instagramblocker.domain.BlockPhase

internal object MonitorDecision {
    fun shouldRecordAttempt(
        phase: BlockPhase,
        instagramForeground: Boolean,
        enteredInstagram: Boolean,
        screenUsable: Boolean,
        debounceElapsed: Boolean,
    ): Boolean = screenUsable && instagramForeground && phase != BlockPhase.RELEASED &&
        (phase == BlockPhase.READY || (enteredInstagram && debounceElapsed))
}
