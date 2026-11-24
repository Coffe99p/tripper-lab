package com.joaofabijack.tripper

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MapsNotificationListener : NotificationListenerService() {
    companion object { var instance: MapsNotificationListener? = null; private set }

    override fun onCreate() { super.onCreate(); MapsDiagnostics.init(this) }
    override fun onListenerConnected() {
        instance = this
        MapsDiagnostics.connection(true)
        snapshotNavigation()
        if (MapsDiagnostics.active()) snapshot()
    }
    override fun onListenerDisconnected() {
        instance = null
        MapsNavigationFeed.unavailable()
        MapsDiagnostics.connection(false)
    }
    override fun onDestroy() {
        if (instance === this) { instance = null; MapsNavigationFeed.unavailable(); MapsDiagnostics.connection(false) }
        super.onDestroy()
    }
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != MapsCaptureGate.MAPS_PACKAGE) return
        MapsNavigationFeed.posted(sbn)
        MapsDiagnostics.capture(sbn, "POSTED")
    }
    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap, reason: Int) {
        if (sbn.packageName != MapsCaptureGate.MAPS_PACKAGE) return
        MapsNavigationFeed.removed(sbn)
        MapsDiagnostics.capture(sbn, "REMOVED", reason)
    }
    fun snapshotNavigation() {
        if (!MapsNavigationFeed.active()) return
        try {
            activeNotifications?.filter { it.packageName == MapsCaptureGate.MAPS_PACKAGE }
                ?.sortedBy { it.postTime }?.forEach { MapsNavigationFeed.posted(it, snapshot = true) }
        } catch (_: RuntimeException) { MapsNavigationFeed.unavailable() }
    }
    fun snapshot() {
        if (!MapsDiagnostics.connected || !MapsDiagnostics.active()) return
        try {
            activeNotifications?.filter { it.packageName == MapsCaptureGate.MAPS_PACKAGE }
                ?.forEach { MapsDiagnostics.capture(it, "SNAPSHOT") }
        } catch (_: SecurityException) { MapsDiagnostics.connection(false) }
    }
}
