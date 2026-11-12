package com.reelfree.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Live state published by the running service to the UI (same process). */
object ServiceStatus {
    private val _connected = MutableStateFlow(false)

    /** True between onServiceConnected and onUnbind/onDestroy. */
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    internal fun setConnected(value: Boolean) {
        _connected.value = value
    }
}
