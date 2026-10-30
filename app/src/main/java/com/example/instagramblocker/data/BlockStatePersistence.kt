package com.example.instagramblocker.data

import com.example.instagramblocker.domain.BlockState

/** Small persistence boundary so the store behavior can be tested without Android. */
interface BlockStatePersistence {
    fun read(): BlockState
    fun write(state: BlockState): Boolean
}
