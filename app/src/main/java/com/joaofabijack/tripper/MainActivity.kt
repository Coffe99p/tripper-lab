package com.joaofabijack.tripper

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.net.Uri
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import java.text.DateFormat
import java.util.Date

class MainActivity : Activity() {
    private var report = ""
    private var tripper: TripperService? = null
    private var bound = false
    private var pendingAction: String? = null
    private var displayedDevices = emptyList<String>()
    private var toolsExpanded = false
    private var simulationExpanded = false
    private val mapsComponent by lazy { ComponentName(this, MapsNotificationListener::class.java) }
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            tripper = (binder as TripperService.LocalBinder).service
            tripper?.listener = ::renderTripper
        }
        override fun onServiceDisconnected(name: ComponentName) { tripper = null }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingAction = savedInstanceState?.getString("pendingAction")
        toolsExpanded = savedInstanceState?.getBoolean("toolsExpanded") ?: false
        simulationExpanded = savedInstanceState?.getBoolean("simulationExpanded") ?: false
        MapsDiagnostics.init(this)
        BatteryLab.init(this)
        if (Build.VERSION.SDK_INT < 35) {
            @Suppress("DEPRECATION")
            window.setDecorFitsSystemWindows(false)
        }
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.root).setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        findViewById<View>(R.id.root).requestApplyInsets()
        setupTripper()
        findViewById<TextView>(R.id.app_version).text = getString(R.string.personal_version, BuildConfig.VERSION_NAME)
        findViewById<Button>(R.id.home_access).setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                    .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, mapsComponent.flattenToString()))
            } catch (_: android.content.ActivityNotFoundException) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
        findViewById<Button>(R.id.home_maps).setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))
                    .setPackage(MapsCaptureGate.MAPS_PACKAGE))
            } catch (_: android.content.ActivityNotFoundException) {
                Toast.makeText(this, R.string.maps_not_installed, Toast.LENGTH_LONG).show()
            }
        }
        findViewById<Button>(R.id.home_help).setOnClickListener { startActivity(Intent(this, HelpActivity::class.java)) }
        findViewById<Button>(R.id.home_diagnostics).setOnClickListener { startActivity(Intent(this, MapsActivity::class.java)) }
        findViewById<Button>(R.id.home_battery).setOnClickListener { startActivity(Intent(this, BatteryActivity::class.java)) }
        findViewById<Button>(R.id.tools_toggle).setOnClickListener { toolsExpanded = !toolsExpanded; renderTools() }
        findViewById<Button>(R.id.simulation_toggle).setOnClickListener {
            simulationExpanded = !simulationExpanded
            tripper?.snapshot()?.let(::renderTripper)
        }
        renderTools()
        findViewById<Button>(R.id.refresh_button).setOnClickListener {
            updateDiagnostics()
            Toast.makeText(this, R.string.updated, Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.copy_button).setOnClickListener {
            getSystemService(ClipboardManager::class.java)
                .setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name),
                    report + "\n\n" + (tripper?.snapshot()?.log ?: "")))
            if (Build.VERSION.SDK_INT < 33) {
                Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        bound = bindService(Intent(this, TripperService::class.java), connection, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        tripper?.listener = null
        if (bound) unbindService(connection)
        bound = false; tripper = null
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("pendingAction", pendingAction)
        outState.putBoolean("toolsExpanded", toolsExpanded)
        outState.putBoolean("simulationExpanded", simulationExpanded)
        super.onSaveInstanceState(outState)
    }

    private fun setupTripper() {
        findViewById<Button>(R.id.scan_button).setOnClickListener { requestSession(TripperService.SCAN) }
        findViewById<Button>(R.id.reconnect_button).setOnClickListener { requestSession(TripperService.RECONNECT) }
        findViewById<Button>(R.id.stop_button).setOnClickListener { tripper?.stopSession() }
        findViewById<Button>(R.id.bridge_start).setOnClickListener { tripper?.startMaps() }
        findViewById<Button>(R.id.pin_button).setOnClickListener {
            val input = findViewById<EditText>(R.id.pin_input)
            val pin = input.text.toString()
            if (!pin.matches(Regex("[0-9]{1,6}"))) { input.error = getString(R.string.pin_hint); return@setOnClickListener }
            tripper?.submitPin(pin)
            input.text.clear()
            getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                .hideSoftInputFromWindow(input.windowToken, 0)
        }
        findViewById<Spinner>(R.id.distance_spinner).setSelection(1)
        findViewById<Button>(R.id.send_button).setOnClickListener {
            val maneuverIndex = findViewById<Spinner>(R.id.maneuver_spinner).selectedItemPosition
            val distanceIndex = findViewById<Spinner>(R.id.distance_spinner).selectedItemPosition
            val maneuvers = listOf(TripperProtocol.Maneuver.RIGHT, TripperProtocol.Maneuver.LEFT,
                TripperProtocol.Maneuver.STRAIGHT, TripperProtocol.Maneuver.ARRIVAL, TripperProtocol.Maneuver.ROUNDABOUT_SECOND,
                TripperProtocol.Maneuver.SLIGHT_LEFT, TripperProtocol.Maneuver.SLIGHT_RIGHT,
                TripperProtocol.Maneuver.KEEP_LEFT, TripperProtocol.Maneuver.KEEP_RIGHT, TripperProtocol.Maneuver.MERGE)
            val meters = if (maneuverIndex == 3) 0 else listOf(50, 200, 999, 1000, 1500)[distanceIndex]
            val description = getString(R.string.simulation_description,
                resources.getStringArray(R.array.maneuvers)[maneuverIndex],
                if (maneuverIndex == 3) "0 m" else resources.getStringArray(R.array.distances)[distanceIndex])
            tripper?.sendSimulation(maneuvers[maneuverIndex], meters, description)
        }
        findViewById<Button>(R.id.confirm_button).setOnClickListener {
            AlertDialog.Builder(this).setTitle(R.string.confirm_title).setMessage(R.string.confirm_body)
                .setPositiveButton(R.string.confirm_yes) { _, _ -> tripper?.confirmDisplay() }
                .setNegativeButton(android.R.string.cancel, null).show()
        }
        findViewById<Button>(R.id.forget_button).setOnClickListener {
            AlertDialog.Builder(this).setTitle(R.string.forget_title).setMessage(R.string.forget_body)
                .setPositiveButton(R.string.forget) { _, _ -> tripper?.forgetDevice() }
                .setNegativeButton(android.R.string.cancel, null).show()
        }
        findViewById<Button>(R.id.log_button).setOnClickListener {
            AlertDialog.Builder(this).setTitle(R.string.view_log)
                .setMessage(tripper?.snapshot()?.log?.ifEmpty { getString(R.string.no_log) } ?: getString(R.string.no_log))
                .setPositiveButton(R.string.close, null).show()
        }
    }

    private fun requestSession(action: String) {
        pendingAction = action
        val permissions = mutableListOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        if (Build.VERSION.SDK_INT >= 33) permissions += Manifest.permission.POST_NOTIFICATIONS
        val missing = permissions.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), 3) else startPendingSession()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 3) startPendingSession()
    }

    private fun startPendingSession() {
        val action = pendingAction ?: return
        pendingAction = null
        if (listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            .any { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) {
            Toast.makeText(this, R.string.permissions_needed, Toast.LENGTH_LONG).show(); return
        }
        try {
            if (getSystemService(BluetoothManager::class.java).adapter?.isEnabled != true) {
                Toast.makeText(this, R.string.bluetooth_off, Toast.LENGTH_LONG).show(); return
            }
            val minutes = if (findViewById<Spinner>(R.id.session_duration).selectedItemPosition == 1) 30 else 10
            startForegroundService(Intent(this, TripperService::class.java).setAction(action)
                .putExtra(TripperService.DURATION_MINUTES, minutes))
        } catch (_: IllegalStateException) {
            Toast.makeText(this, R.string.start_failed, Toast.LENGTH_LONG).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, R.string.permissions_needed, Toast.LENGTH_LONG).show()
        }
    }

    private fun renderTripper(snapshot: TripperService.Snapshot) {
        val idle = snapshot.state in listOf(TripperService.State.IDLE, TripperService.State.ERROR)
        findViewById<Spinner>(R.id.session_duration).isEnabled = idle
        findViewById<TextView>(R.id.connection_state).text = resources.getStringArray(R.array.connection_states)[snapshot.state.ordinal]
        findViewById<TextView>(R.id.connection_message).text = snapshot.message
        findViewById<Button>(R.id.scan_button).isEnabled = idle
        findViewById<Button>(R.id.reconnect_button).isEnabled = idle && tripper?.hasSavedDevice() == true
        findViewById<Button>(R.id.stop_button).isEnabled = !idle
        findViewById<Button>(R.id.forget_button).isEnabled = tripper?.hasSavedDevice() == true
        findViewById<View>(R.id.pin_group).visibility = if (snapshot.state == TripperService.State.PIN) View.VISIBLE else View.GONE
        val ready = snapshot.state == TripperService.State.READY
        val canSimulate = ready && !snapshot.mapsActive
        // A new pairing must still be confirmed on the actual display before Maps can start.
        val needsConfirmation = canSimulate && tripper?.isSelectedDeviceSaved() != true
        findViewById<View>(R.id.simulation_toggle).visibility = if (canSimulate && !needsConfirmation) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.simulation_toggle).setText(if (simulationExpanded) R.string.hide_simulation else R.string.show_simulation)
        findViewById<View>(R.id.simulation_group).visibility = if (canSimulate && (needsConfirmation || simulationExpanded)) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.bridge_start).isEnabled = ready && !snapshot.mapsActive
        findViewById<TextView>(R.id.bridge_status).setText(if (snapshot.mapsActive) R.string.bridge_active else R.string.bridge_hint)
        findViewById<TextView>(R.id.maps_live_status).text = snapshot.mapsStatus
        findViewById<Button>(R.id.confirm_button).isEnabled = canSimulate && snapshot.hasSimulation && !snapshot.confirmed
        findViewById<TextView>(R.id.simulation_status).text = if (snapshot.mapsActive) "" else snapshot.simulation
        findViewById<View>(R.id.simulation_status).visibility = if (snapshot.hasSimulation && !snapshot.mapsActive) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.packet_counts).text = getString(R.string.counts, snapshot.sent, snapshot.received)
        val devices = if (snapshot.state in listOf(TripperService.State.SEARCHING, TripperService.State.CHOOSING)) snapshot.devices else emptyList()
        val identities = devices.map { it.address }
        if (identities != displayedDevices) {
            displayedDevices = identities
            val container = findViewById<LinearLayout>(R.id.device_list)
            container.removeAllViews()
            devices.forEach { candidate ->
                container.addView(Button(this).apply {
                    text = getString(R.string.candidate, candidate.name, candidate.address.takeLast(5), candidate.rssi)
                    isAllCaps = false
                    minHeight = (56 * resources.displayMetrics.density).toInt()
                    setOnClickListener { tripper?.select(candidate.address) }
                })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        MapsDiagnostics.observer = ::renderCaptureStatus
        updateDiagnostics()
        val access = getSystemService(NotificationManager::class.java).isNotificationListenerAccessGranted(mapsComponent)
        findViewById<TextView>(R.id.home_access_status).setText(if (access) R.string.home_access_ready else R.string.home_access_needed)
        findViewById<Button>(R.id.home_access).setText(if (access) R.string.home_access_manage else R.string.maps_access)
        if (access && MapsNotificationListener.instance == null) NotificationListenerService.requestRebind(mapsComponent)
        renderCaptureStatus()
    }

    override fun onPause() {
        MapsDiagnostics.observer = null
        super.onPause()
    }

    private fun renderTools() {
        findViewById<View>(R.id.tools_group).visibility = if (toolsExpanded) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.tools_toggle).setText(if (toolsExpanded) R.string.tools_hide else R.string.tools_show)
    }

    private fun renderCaptureStatus() {
        findViewById<View>(R.id.capture_notice).visibility = if (MapsDiagnostics.active()) View.VISIBLE else View.GONE
    }

    private fun updateDiagnostics() {
        val device = getString(
            R.string.device_info, Build.MANUFACTURER, Build.MODEL,
            Build.VERSION.RELEASE, Build.VERSION.SDK_INT, BuildConfig.VERSION_NAME,
        )
        // A sticky snapshot requires no ongoing receiver, service, or polling.
        val battery = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percentage = if (level >= 0 && scale > 0) {
            getString(R.string.battery_percentage, (level * 100f / scale).toInt())
        } else getString(R.string.unknown)
        val plugged = battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val power = getString(when {
            plugged < 0 -> R.string.unknown
            plugged == 0 -> R.string.unplugged
            else -> R.string.plugged
        })
        val temperature = if (battery?.hasExtra(BatteryManager.EXTRA_TEMPERATURE) == true) {
            getString(R.string.temperature, battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0)
        } else getString(R.string.unknown)
        val snapshot = getString(R.string.battery_info, percentage, power, temperature)
        val time = getString(R.string.updated_at, DateFormat.getTimeInstance().format(Date()))
        findViewById<TextView>(R.id.device_info).text = device
        findViewById<TextView>(R.id.battery_info).text = snapshot
        findViewById<TextView>(R.id.updated_at).text = time
        report = getString(R.string.diagnostic_report, device, snapshot, time)
    }
}
