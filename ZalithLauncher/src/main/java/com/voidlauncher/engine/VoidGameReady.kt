package com.voidlauncher.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** One-way signal from the native engine's first rendered frame. */
object VoidGameReady {
    private val _rendering = MutableStateFlow(false)
    val rendering: StateFlow<Boolean> = _rendering

    fun reset() { _rendering.value = false }
    fun markRendering() { _rendering.value = true }
}
