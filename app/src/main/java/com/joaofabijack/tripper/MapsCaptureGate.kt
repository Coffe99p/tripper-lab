package com.joaofabijack.tripper

/** Process-local, opt-in collection. Restarting the process never resumes a capture. */
class MapsCaptureGate(private val durationMs: Long = 20 * 60 * 1000L) {
    companion object { const val MAPS_PACKAGE = "com.google.android.apps.maps" }
    private var deadline: Long? = null
    fun start(now: Long) { deadline = now + durationMs }
    fun stop() { deadline = null }
    fun active(now: Long): Boolean = deadline?.let { now < it } == true
    fun accepts(packageName: String, now: Long): Boolean = packageName == MAPS_PACKAGE && active(now)
}
