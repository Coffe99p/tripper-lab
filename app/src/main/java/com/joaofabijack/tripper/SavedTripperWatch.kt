package com.joaofabijack.tripper

/** Manual stop silences discovery for the current foreground visit. */
internal class SavedTripperWatch {
    private var screens = 0
    private var paused = false

    fun open() {
        if (screens == 0) paused = false
        screens++
    }

    fun close() { screens-- }
    fun pause() { paused = true }
    fun resume() { paused = false }

    fun canWatch(sessionActive: Boolean, mapsOnlyMeasurement: Boolean) =
        screens > 0 && !paused && !sessionActive && !mapsOnlyMeasurement
}
