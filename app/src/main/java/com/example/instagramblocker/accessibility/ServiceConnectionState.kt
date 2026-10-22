package com.example.instagramblocker.accessibility

import kotlinx.coroutines.flow.MutableStateFlow

/** Estado do processo atual; a configuração Android é consultada separadamente. */
object ServiceConnectionState {
    val connected = MutableStateFlow(false)
}

