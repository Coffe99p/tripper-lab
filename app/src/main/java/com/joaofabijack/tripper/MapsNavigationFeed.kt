package com.joaofabijack.tripper

import android.app.Notification
import android.os.SystemClock
import android.service.notification.StatusBarNotification

/** Process-local opt-in feed. No raw titles, addresses or notification keys are saved.
 * This feed is independent of the diagnostic capture and its disk/bitmap work. */
object MapsNavigationFeed {
    sealed class Event {
        data class Posted(val key: String, val result: MapsInstructionParser.Result, val at: Long, val age: Long) : Event()
        data class Removed(val key: String, val at: Long) : Event()
        data object Unavailable : Event()
    }
    private var receiver: ((Event) -> Unit)? = null
    fun active() = receiver != null
    fun start(callback: (Event) -> Unit) { receiver = callback }
    fun stop() { receiver = null }
    fun unavailable() { receiver?.invoke(Event.Unavailable) }
    fun posted(sbn: StatusBarNotification, snapshot: Boolean = false) {
        val callback = receiver ?: return
        if (sbn.packageName != MapsCaptureGate.MAPS_PACKAGE) return
        try {
            val n = sbn.notification
            val summary = n.flags and Notification.FLAG_GROUP_SUMMARY != 0
            val result = MapsInstructionParser.parse(n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.take(512),
                n.category, sbn.isOngoing, summary)
            callback(Event.Posted(sbn.key, result, SystemClock.elapsedRealtime(),
                if (snapshot) System.currentTimeMillis() - sbn.postTime else 0))
        } catch (_: RuntimeException) { unavailable() }
    }
    fun removed(sbn: StatusBarNotification) {
        if (sbn.packageName == MapsCaptureGate.MAPS_PACKAGE) receiver?.invoke(Event.Removed(sbn.key, SystemClock.elapsedRealtime()))
    }
}
