package com.bluepilot.remote.model.widgets

import kotlinx.serialization.Serializable

// ----------------------------------------------------------------------
// Geometry — everything fractional (0..1 of the canvas) so layouts scale
// to any screen size and orientation.
// ----------------------------------------------------------------------

@Serializable
@androidx.compose.runtime.Immutable
data class WidgetFrame(
    val x: Float = 0f,
    val y: Float = 0f,
    val w: Float = 0.25f,
    val h: Float = 0.15f
) {
    companion object {
        const val MIN_SIZE = 0.05f // 5% of canvas — nothing smaller is touchable
    }

    /** Clamp to the canvas; enforce minimum touchable size. */
    fun sanitized(): WidgetFrame {
        val cw = w.coerceIn(MIN_SIZE, 1f)
        val ch = h.coerceIn(MIN_SIZE, 1f)
        return WidgetFrame(
            x = x.coerceIn(0f, 1f - cw),
            y = y.coerceIn(0f, 1f - ch),
            w = cw,
            h = ch
        )
    }
}
