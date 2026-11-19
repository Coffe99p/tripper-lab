package com.joaofabijack.tripper

/** A session's limit is fixed at start, using elapsed realtime, including sleep. */
class SessionDeadline(startedAt: Long, requestedMinutes: Int) {
    val minutes = if (requestedMinutes == 30) 30 else 10
    val durationMs = minutes * 60_000L
    private val deadline = startedAt + durationMs
    fun expired(now: Long) = now >= deadline
}
