package com.joaofabijack.tripper

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import java.io.File

@Suppress("DEPRECATION")
class MapsActivity : Activity() {
    private val component by lazy { ComponentName(this, MapsNotificationListener::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapsDiagnostics.init(this)
        BatteryLab.init(this)
        if (Build.VERSION.SDK_INT < 35) window.setDecorFitsSystemWindows(false)
        setContentView(R.layout.activity_maps)
        findViewById<View>(R.id.maps_root).setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets
        }
        findViewById<View>(R.id.maps_root).requestApplyInsets()
        findViewById<Button>(R.id.maps_access).setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                    .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString()))
            } catch (_: android.content.ActivityNotFoundException) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
        findViewById<Button>(R.id.maps_start).setOnClickListener {
            if (!accessGranted()) { toast(R.string.maps_need_access); return@setOnClickListener }
            MapsDiagnostics.start()
            if (!MapsDiagnostics.connected) NotificationListenerService.requestRebind(component)
            render()
        }
        findViewById<Button>(R.id.maps_stop).setOnClickListener { MapsDiagnostics.stop(); render() }
        findViewById<Button>(R.id.maps_open).setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))
                    .setPackage(MapsCaptureGate.MAPS_PACKAGE))
            } catch (_: android.content.ActivityNotFoundException) { toast(R.string.maps_not_installed) }
        }
        findViewById<Button>(R.id.maps_export).setOnClickListener {
            try {
                // Freeze the requested export while Android's file picker is open.
                File(noBackupFilesDir, "maps-export-pending.json").writeText(MapsDiagnostics.export())
                startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/json").putExtra(Intent.EXTRA_TITLE, "Tripper-Maps-diagnostico.json"), 4)
            } catch (_: Exception) { toast(R.string.maps_failed) }
        }
        findViewById<Button>(R.id.maps_clear).setOnClickListener {
            AlertDialog.Builder(this).setTitle(R.string.maps_clear).setMessage(R.string.maps_clear_message)
                .setPositiveButton(R.string.maps_clear) { _, _ ->
                    MapsDiagnostics.clear(); File(noBackupFilesDir, "maps-export-pending.json").delete(); render()
                }.setNegativeButton(android.R.string.cancel, null).show()
        }
        findViewById<Button>(R.id.maps_bluetooth).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
        findViewById<Button>(R.id.maps_battery).setOnClickListener { startActivity(Intent(this, BatteryActivity::class.java)) }
    }

    override fun onPause() { MapsDiagnostics.observer = null; super.onPause() }
    override fun onResume() {
        super.onResume()
        MapsDiagnostics.observer = ::render
        if (!accessGranted() && MapsDiagnostics.active()) MapsDiagnostics.stop("ACCESS_REVOKED", "Acesso removido pelo usuário.")
        render()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 4) return
        val pending = File(noBackupFilesDir, "maps-export-pending.json")
        try {
            if (resultCode == RESULT_OK) {
                val uri = data?.data ?: error("Destino ausente")
                contentResolver.openOutputStream(uri, "wt")?.use { out -> pending.inputStream().use { it.copyTo(out) } }
                    ?: error("Destino indisponível")
                toast(R.string.maps_exported)
            }
        } catch (_: Exception) { toast(R.string.maps_failed) }
        finally { pending.delete() }
    }

    private fun accessGranted() = getSystemService(NotificationManager::class.java).isNotificationListenerAccessGranted(component)
    private fun render() {
        findViewById<TextView>(R.id.maps_access_status).setText(if (accessGranted()) R.string.maps_access_on else R.string.maps_access_off)
        findViewById<Button>(R.id.maps_start).isEnabled = !MapsDiagnostics.active()
        findViewById<Button>(R.id.maps_stop).isEnabled = MapsDiagnostics.active()
        findViewById<TextView>(R.id.maps_summary).text = MapsDiagnostics.summary()
        findViewById<TextView>(R.id.maps_sample).text = MapsDiagnostics.sampleText()
        for ((id, large) in listOf(R.id.maps_small_icon to false, R.id.maps_large_icon to true)) {
            val image = findViewById<ImageView>(id)
            val bytes = MapsDiagnostics.latestIcon(large)
            image.setImageBitmap(bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) })
            image.visibility = if (bytes == null) View.GONE else View.VISIBLE
        }
    }
    private fun toast(message: Int) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
}
