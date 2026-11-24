package com.joaofabijack.tripper

import android.content.*
import android.app.Application
import android.os.*
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.util.Locale

/** Main-thread only. Uses system broadcasts, no timer, foreground service or wake lock.
 * A process restart invalidates an unfinished run rather than hiding an observation gap. */
object BatteryLab {
    private lateinit var app: Application
    private var loaded = false
    private var run: BatteryRun? = null
    private var startedAt = ""
    private var mapsVersion = ""
    private val records = ArrayDeque<JSONObject>()
    private var registered = false
    private var sessionActive = false
    private var mapsActive = false
    private var storageError = false
    var observer: (() -> Unit)? = null
    var onMapsOnlyChanged: (() -> Unit)? = null
    fun measuringMapsOnly() = run?.mode == BatteryRun.Mode.MAPS_ONLY
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val current = run ?: return
            if (intent.action == Intent.ACTION_POWER_CONNECTED)
                current.invalidate("Carregador conectado durante a medição.")
            val sample = sample()
            current.observe(sample)
            if (sample.elapsedMs - current.start.elapsedMs >= BatteryRun.MAX_MS) finish("Limite de duração.")
            observer?.invoke()
        }
    }
    fun init(context: Context) {
        if (loaded) return
        app = context.applicationContext as Application
        loaded = true
        runCatching {
            val file = File(app.noBackupFilesDir, "battery-lab.json")
            if (file.isFile && file.length() < 200_000) {
                val document = JSONObject(file.readText())
                val history = document.optJSONArray("records") ?: JSONArray()
                for (i in maxOf(0, history.length() - 20) until history.length()) records.add(history.getJSONObject(i))
                document.optJSONObject("pending")?.let {
                    records.add(it.put("status", "INTERRUPTED").put("reason", "O processo do app foi reiniciado; amostra descartada."))
                    persist()
                }
            }
        }.onFailure { storageError = true }
    }
    fun active() = run != null
    fun currentSample(): BatteryRun.Sample = sample()
    private fun sample(): BatteryRun.Sample {
        val battery = app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level in 0..scale && scale > 0) level * 100 / scale else null
        val charge = app.getSystemService(BatteryManager::class.java)
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER).takeIf { it > 0 }?.toLong()
        val temperature = if (battery?.hasExtra(BatteryManager.EXTRA_TEMPERATURE) == true)
            battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0 else null
        val plugged = battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)?.takeIf { it >= 0 }?.let { it != 0 }
        return BatteryRun.Sample(SystemClock.elapsedRealtime(), percent, charge, temperature, plugged,
            app.getSystemService(PowerManager::class.java).isInteractive)
    }
    fun start(mode: BatteryRun.Mode): String? {
        if (active()) return "Já existe uma medição em andamento."
        val initial = sample()
        if (initial.plugged != false) return "Desconecte o USB e o carregador antes de iniciar a medição."
        if (MapsDiagnostics.active()) return "Pare a captura de diagnóstico do Maps antes de medir."
        if (mode == BatteryRun.Mode.MAPS_ONLY && sessionActive) return "Encerre a sessão do Tripper para medir Maps sem Tripper."
        if (mode == BatteryRun.Mode.MAPS_TRIPPER && !mapsActive) return "Conecte o Tripper e ative Usar Maps no Tripper antes de medir."
        startedAt = Instant.now().toString()
        mapsVersion = runCatching {
            @Suppress("DEPRECATION")
            app.packageManager.getPackageInfo(MapsCaptureGate.MAPS_PACKAGE, 0).versionName ?: "indisponível"
        }.getOrDefault("indisponível")
        run = BatteryRun(mode, initial)
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED).apply {
            addAction(Intent.ACTION_POWER_CONNECTED); addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_SCREEN_OFF)
        }
        try {
            if (Build.VERSION.SDK_INT >= 33) app.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                app.registerReceiver(receiver, filter)
            }
            registered = true
        } catch (_: RuntimeException) {
            run = null
            return "Não foi possível acompanhar a bateria. Tente novamente."
        }
        if (measuringMapsOnly()) onMapsOnlyChanged?.invoke()
        persist(); observer?.invoke()
        return null
    }
    fun sessionChanged(active: Boolean, maps: Boolean) {
        sessionActive = active; mapsActive = maps
        val current = run ?: return
        if (current.mode == BatteryRun.Mode.MAPS_ONLY && active) {
            current.invalidate("A sessão do Tripper foi iniciada durante a amostra sem Tripper.")
            finish("Cenário alterado.")
        } else if (current.mode == BatteryRun.Mode.MAPS_TRIPPER && !maps) {
            finish("Envio do Maps ao Tripper encerrado; medição finalizada nesse instante.")
        }
    }
    fun captureStarted() {
        run?.invalidate("Captura de diagnóstico ativada durante a medição.")
        if (active()) finish("Captura de diagnóstico interfere na comparação.")
    }
    fun finish(reason: String = "Encerrada pelo usuário.") {
        val current = run ?: return
        val result = current.finish(sample())
        val record = metadata(current).put("endedAt", Instant.now().toString()).put("status", "FINISHED")
            .put("reason", reason).put("durationMs", result.durationMs).put("screenOnMs", result.screenOnMs)
            .put("end", json(result.end)).put("percentDrop", result.percentDrop).put("chargeMah", result.chargeMah)
            .put("mahPerHour", result.mahPerHour).put("comparable", result.comparable)
            .put("issues", JSONArray(result.issues))
        run = null
        if (registered) { app.unregisterReceiver(receiver); registered = false }
        records.add(record)
        if (current.mode == BatteryRun.Mode.MAPS_ONLY) onMapsOnlyChanged?.invoke()
        persist(); observer?.invoke()
    }
    fun cancel() {
        run?.invalidate("Medição cancelada pelo usuário.")
        finish("Cancelada.")
    }
    private fun json(s: BatteryRun.Sample) = JSONObject().put("elapsedMs", s.elapsedMs).put("percent", s.percent)
        .put("chargeUah", s.chargeUah).put("temperatureC", s.temperatureC).put("plugged", s.plugged).put("screenOn", s.screenOn)
    private fun metadata(current: BatteryRun) = JSONObject().put("mode", current.mode.name).put("startedAt", startedAt)
        .put("appVersion", BuildConfig.VERSION_NAME).put("mapsVersion", mapsVersion)
        .put("device", "${Build.MANUFACTURER} ${Build.MODEL}").put("android", Build.VERSION.RELEASE)
        .put("start", json(current.start)).put("scope", "whole_device; scenario declared by user; meter active in both scenarios")
    private fun document() = JSONObject().put("schema", 1).put("records", JSONArray(records.toList()))
        .apply { run?.let { put("pending", metadata(it)) } }
    private fun persist() {
        while (records.size > 20) records.removeFirst()
        val file = AtomicFile(File(app.noBackupFilesDir, "battery-lab.json"))
        var stream: java.io.FileOutputStream? = null
        try {
            stream = file.startWrite()
            stream.write(document().toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(stream); storageError = false
        } catch (_: Exception) { file.failWrite(stream); storageError = true }
    }
    fun export(): String = document().toString(2)
    private fun modeLabel(mode: String) = if (mode == BatteryRun.Mode.MAPS_ONLY.name) "Maps sem Tripper" else "Maps com Tripper"
    private fun number(value: Double) = String.format(Locale.getDefault(), "%.1f", value)
    fun summary(): String = buildString {
        run?.let {
            append("Medindo: ${modeLabel(it.mode.name)}\n")
            append("${(SystemClock.elapsedRealtime() - it.start.elapsedMs) / 60_000} min decorridos. Pare após 20–25 min.\n\n")
        } ?: append("Nenhuma medição ativa.\n\n")
        if (storageError) append("Falha ao salvar ou ler o histórico. Exporte os dados disponíveis.\n\n")
        if (records.isEmpty()) append("Ainda não há resultados. Consumo e economia não foram medidos.")
        records.toList().takeLast(4).reversed().forEach { record ->
            append("${modeLabel(record.getString("mode"))} • ${record.optString("startedAt")}\n")
            if (record.optString("status") == "INTERRUPTED") append("${record.optString("reason")}\n\n")
            else {
                append("${number(record.optLong("durationMs") / 60_000.0)} min • tela acesa ${number(record.optLong("screenOnMs") / 60_000.0)} min\n")
                if (record.has("percentDrop")) append("Queda: ${record.getInt("percentDrop")} pontos percentuais\n")
                if (record.has("chargeMah")) append("Variação de carga: ${number(record.getDouble("chargeMah"))} mAh (celular inteiro)\n")
                else append("Contador de carga indisponível; porcentagem tem resolução limitada.\n")
                if (record.has("mahPerHour")) append("Taxa observada: ${number(record.getDouble("mahPerHour"))} mAh/h\n")
                append("${record.optString("reason")}\n")
                val issues = record.optJSONArray("issues") ?: JSONArray()
                for (i in 0 until issues.length()) append("${issues.getString(i)}\n")
                append(if (record.optBoolean("comparable")) "Sem impedimento detectado; confira condições semelhantes entre os cenários.\n\n" else "Não usar esta amostra para comparar consumo.\n\n")
            }
        }
        append("\nO medidor não confirma que o Maps estava navegando. Use o mesmo trajeto, duração, brilho e condições; repita os dois cenários. Os valores são do celular inteiro, não apenas deste app.")
    }
}
