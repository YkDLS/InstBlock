package com.example.instagramblocker.ui.dashboard

import com.example.instagramblocker.updates.UpdateInfo

data class UpdateUiState(
    val checking: Boolean = false,
    val message: String? = null,
    val available: UpdateInfo? = null,
)
