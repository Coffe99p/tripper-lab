package com.joaofabijack.tripper

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.util.Locale

@Suppress("DEPRECATION")
class BatteryActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BatteryLab.init(this)
        MapsDiagnostics.init(this)
        if (Build.VERSION.SDK_INT < 35) window.setDecorFitsSystemWindows(false)
        setContentView(R.layout.activity_battery)
        findViewById<View>(R.id.battery_root).setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets
        }
        findViewById<View>(R.id.battery_root).requestApplyInsets()
        findViewById<Button>(R.id.battery_baseline).setOnClickListener { start(BatteryRun.Mode.MAPS_ONLY) }
        findViewById<Button>(R.id.battery_tripper).setOnClickListener { start(BatteryRun.Mode.MAPS_TRIPPER) }
        findViewById<Button>(R.id.battery_finish).setOnClickListener { BatteryLab.finish(); render() }
        findViewById<Button>(R.id.battery_cancel).setOnClickListener { BatteryLab.cancel(); render() }
        findViewById<Button>(R.id.battery_refresh).setOnClickListener { render() }
        findViewById<Button>(R.id.battery_export).setOnClickListener {
            try {
                File(noBackupFilesDir, "battery-export-pending.json").writeText(BatteryLab.export())
                startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/json").putExtra(Intent.EXTRA_TITLE, "Tripper-bateria.json"), 6)
            } catch (_: Exception) { toast(getString(R.string.maps_failed)) }
        }
    }
    private fun start(mode: BatteryRun.Mode) {
        BatteryLab.start(mode)?.let(::toast)
        render()
    }
    override fun onStart() { super.onStart(); BatteryLab.observer = ::render }
    override fun onStop() { BatteryLab.observer = null; super.onStop() }
    override fun onResume() { super.onResume(); render() }
    private fun render() {
        val sample = BatteryLab.currentSample()
        findViewById<TextView>(R.id.battery_reading).text = getString(R.string.battery_reading,
            sample.percent?.let { "$it%" } ?: getString(R.string.unknown),
            sample.temperatureC?.let { String.format(Locale.getDefault(), "%.1f °C", it) } ?: getString(R.string.unknown),
            if (sample.plugged == false) getString(R.string.unplugged) else getString(R.string.plugged))
        findViewById<TextView>(R.id.battery_results).text = BatteryLab.summary()
        findViewById<Button>(R.id.battery_baseline).isEnabled = !BatteryLab.active()
        findViewById<Button>(R.id.battery_tripper).isEnabled = !BatteryLab.active()
        findViewById<Button>(R.id.battery_finish).isEnabled = BatteryLab.active()
        findViewById<Button>(R.id.battery_cancel).isEnabled = BatteryLab.active()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 6) return
        val file = File(noBackupFilesDir, "battery-export-pending.json")
        try {
            if (resultCode == RESULT_OK) {
                val uri = data?.data ?: error("Destino ausente")
                contentResolver.openOutputStream(uri, "wt")?.use { out -> file.inputStream().use { it.copyTo(out) } }
                    ?: error("Destino indisponível")
                toast(getString(R.string.maps_exported))
            }
        } catch (_: Exception) { toast(getString(R.string.maps_failed)) }
        finally { file.delete() }
    }
    private fun toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
}
