package com.joaofabijack.tripper

import android.app.KeyguardManager
import android.app.Application
import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import android.util.AtomicFile
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.time.Instant

/** Diagnostic samples only. This object has no reference to the Bluetooth transport.
 * All access occurs on the main looper (including NotificationListenerService callbacks). */
object MapsDiagnostics {
    private val gate = MapsCaptureGate()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var app: Application
    private val events = ArrayDeque<JSONObject>()
    private val icons = linkedMapOf<String, String>()
    private val present = linkedMapOf<String, JSONObject>()
    private var loaded = false
    private var started = false
    private var omitted = 0
    private var error: String? = null
    private var savedVersion = ""
    var connected = false
        private set
    var observer: (() -> Unit)? = null
    private val expiry = Runnable { stop("LIMIT", "Limite de 20 minutos atingido.") }

    fun init(context: Context) {
        if (loaded) return
        app = context.applicationContext as Application
        loaded = true
        runCatching {
            val file = File(app.noBackupFilesDir, "maps-capture.json")
            if (file.exists() && file.length() < 3_000_000) {
                val json = JSONObject(file.readText())
                val list = json.optJSONArray("events") ?: JSONArray()
                for (i in maxOf(0, list.length() - 2000) until list.length()) events.addLast(list.getJSONObject(i))
                val images = json.optJSONObject("icons") ?: JSONObject()
                images.keys().asSequence().take(32).forEach { key -> icons[key] = images.getString(key) }
                savedVersion = json.optString("mapsVersion")
                omitted = json.optInt("omitted")
                // Historical events stay available; no live notification survives process death.
            }
        }.onFailure { error = "O registro anterior não pôde ser lido." }
    }

    fun active(): Boolean = gate.active(SystemClock.elapsedRealtime())
    fun accepts(packageName: String): Boolean {
        if (started && !active()) stop("LIMIT", "Limite de 20 minutos atingido.")
        return gate.accepts(packageName, SystemClock.elapsedRealtime())
    }

    fun start() {
        if (active()) return
        BatteryLab.captureStarted()
        gate.start(SystemClock.elapsedRealtime()); started = true; present.clear(); error = null
        savedVersion = runCatching {
            @Suppress("DEPRECATION")
            app.packageManager.getPackageInfo(MapsCaptureGate.MAPS_PACKAGE, 0).versionName ?: "desconhecida"
        }.getOrDefault("indisponível")
        add(systemEvent("START", "Captura iniciada pelo usuário."))
        handler.removeCallbacks(expiry); handler.postDelayed(expiry, 20 * 60 * 1000L)
        MapsNotificationListener.instance?.snapshot()
    }

    fun stop(kind: String = "STOP", description: String = "Captura encerrada pelo usuário.") {
        val wasStarted = started
        gate.stop(); started = false; present.clear(); handler.removeCallbacks(expiry)
        if (wasStarted) add(systemEvent(kind, description)) else observer?.invoke()
    }

    fun connection(value: Boolean) {
        connected = value
        if (!value) present.clear()
        if (active()) add(systemEvent(if (value) "LISTENER_CONNECTED" else "LISTENER_DISCONNECTED",
            if (value) "Acesso às notificações conectado." else "Acesso interrompido; amostras atuais invalidadas."))
        observer?.invoke()
    }

    fun capture(sbn: StatusBarNotification, kind: String, reason: Int? = null) {
        // Never inspect notification contents before this package + opt-in gate.
        if (!accepts(sbn.packageName)) return
        try {
            val key = digest(sbn.key.toByteArray()).take(16)
            val event = base(kind).put("key", key).put("postTime", sbn.postTime)
            if (kind == "REMOVED") {
                present.remove(key)
                event.put("reason", reason)
            } else {
                val notification = sbn.notification
                val extras = notification.extras
                val fields = JSONObject()
                for (field in listOf(Notification.EXTRA_TITLE, Notification.EXTRA_TITLE_BIG,
                    Notification.EXTRA_TEXT, Notification.EXTRA_BIG_TEXT, Notification.EXTRA_SUB_TEXT,
                    Notification.EXTRA_SUMMARY_TEXT, Notification.EXTRA_INFO_TEXT, "android.shortCriticalText")) {
                    extras.getCharSequence(field)?.toString()?.take(512)?.let { fields.put(field, it) }
                }
                extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.take(8)?.let { lines ->
                    fields.put(Notification.EXTRA_TEXT_LINES, JSONArray(lines.map { it.toString().take(512) }))
                }
                @Suppress("DEPRECATION")
                val hasCustomView = notification.contentView != null
                val interpreted = MapsInstructionParser.parse(fields.optString(Notification.EXTRA_TITLE),
                    notification.category, sbn.isOngoing, notification.flags and Notification.FLAG_GROUP_SUMMARY != 0)
                event.put("interpretation", JSONObject().put("state", interpreted.state.name)
                    .put("direction", interpreted.direction?.name).put("meters", interpreted.meters).put("exit", interpreted.exit))
                event.put("fields", fields).put("ongoing", sbn.isOngoing)
                    .put("category", notification.category ?: JSONObject.NULL)
                    .put("channel", notification.channelId)
                    .put("groupSummary", notification.flags and Notification.FLAG_GROUP_SUMMARY != 0)
                    .put("extraKeys", JSONArray(extras.keySet().sorted().take(50)))
                    .put("customView", hasCustomView)
                    .put("smallIconType", notification.smallIcon?.type)
                    .put("largeIconType", notification.getLargeIcon()?.type)
                icon(notification.smallIcon)?.let { event.put("smallIcon", it) }
                icon(notification.getLargeIcon())?.let { event.put("largeIcon", it) }
                present.remove(key); present[key] = event
                while (present.size > 32) present.remove(present.keys.first())
            }
            add(event)
        } catch (_: RuntimeException) {
            // Malformed/unsupported extras do not preserve an old sample as current.
            present.clear()
            add(systemEvent("EXTRACTION_ERROR", "Não foi possível extrair uma notificação do Maps."))
        }
    }

    private fun icon(icon: Icon?): String? {
        if (icon == null || icon.type !in listOf(Icon.TYPE_RESOURCE, Icon.TYPE_BITMAP, Icon.TYPE_ADAPTIVE_BITMAP)) return null
        return runCatching {
            val drawable = icon.loadDrawable(app) ?: return null
            val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, 96, 96); drawable.draw(Canvas(bitmap))
            val bytes = ByteArrayOutputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); out.toByteArray() }
            bitmap.recycle()
            val hash = digest(bytes)
            if (icons.containsKey(hash)) return hash
            if (icons.size >= 32 || bytes.size > 24_000) return null
            icons[hash] = Base64.encodeToString(bytes, Base64.NO_WRAP)
            hash
        }.getOrNull()
    }

    private fun base(kind: String): JSONObject = JSONObject().put("kind", kind)
        .put("receivedAt", Instant.now().toString()).put("elapsedMs", SystemClock.elapsedRealtime())
        .put("locked", app.getSystemService(KeyguardManager::class.java).isKeyguardLocked)

    private fun systemEvent(kind: String, note: String) = base(kind).put("note", note)
    private fun add(event: JSONObject) {
        events.addLast(event)
        while (events.size > 2000) { events.removeFirst(); omitted++ }
        persist(); observer?.invoke()
    }

    private fun document(): JSONObject = JSONObject().put("schema", 1).put("appVersion", BuildConfig.VERSION_NAME)
        .put("mapsVersion", savedVersion).put("capturing", active()).put("omitted", omitted)
        .put("events", JSONArray(events.toList())).put("icons", JSONObject(icons as Map<*, *>))

    fun export(): String = document().toString(2)

    private fun persist() {
        val file = AtomicFile(File(app.noBackupFilesDir, "maps-capture.json"))
        var stream: java.io.FileOutputStream? = null
        try {
            var bytes = document().toString().toByteArray(Charsets.UTF_8)
            while (bytes.size > 2_000_000 && events.isNotEmpty()) {
                events.removeFirst(); omitted++
                bytes = document().toString().toByteArray(Charsets.UTF_8)
            }
            stream = file.startWrite()
            stream.write(bytes)
            file.finishWrite(stream)
        } catch (_: Exception) {
            file.failWrite(stream); error = "Falha ao salvar o registro local."
        }
    }

    fun clear() {
        stop(); events.clear(); icons.clear(); present.clear(); omitted = 0; savedVersion = ""; error = null
        AtomicFile(File(app.noBackupFilesDir, "maps-capture.json")).delete()
        observer?.invoke()
    }

    fun summary(): String = buildString {
        append(when {
            active() && connected -> "Capturando somente o Maps • até 20 minutos"
            active() -> "Captura iniciada, mas o acesso às notificações está desconectado."
            else -> "Captura parada"
        })
        append("\n${events.count { it.has("key") }} eventos do Maps no registro • $omitted eventos antigos descartados")
        if (savedVersion.isNotEmpty()) append("\nMaps: $savedVersion")
        error?.let { append("\n$it") }
    }

    fun sampleText(): String {
        if (!active() || !connected) return "Sem amostra atual. O histórico pode ser exportado para análise."
        val latest = present.values.lastOrNull() ?: return "Nenhuma notificação atual do Maps observada nesta captura."
        val fields = latest.optJSONObject("fields") ?: JSONObject()
        return buildString {
            val time = Instant.parse(latest.getString("receivedAt")).atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
            append("Amostra recebida às $time\n")
            val labels = mapOf("android.title" to "Título", "android.text" to "Texto", "android.subText" to "Complemento",
                "android.bigText" to "Texto expandido", "android.shortCriticalText" to "Resumo curto")
            for (key in fields.keys()) append("${labels[key] ?: key.removePrefix("android.")}: ${fields.opt(key)}\n")
            if (fields.length() == 0) append("Nenhum texto disponível.\n")
            val parsed = MapsInstructionParser.parse(fields.optString(Notification.EXTRA_TITLE), latest.optString("category"),
                latest.optBoolean("ongoing"), latest.optBoolean("groupSummary"))
            val direction = when (parsed.direction) {
                MapsInstructionParser.Direction.LEFT -> "Esquerda"
                MapsInstructionParser.Direction.RIGHT -> "Direita"
                MapsInstructionParser.Direction.SLIGHT_LEFT -> "Curva suave à esquerda"
                MapsInstructionParser.Direction.SLIGHT_RIGHT -> "Curva suave à direita"
                MapsInstructionParser.Direction.STRAIGHT -> "Em frente"
                MapsInstructionParser.Direction.KEEP_LEFT -> "Manter à esquerda"
                MapsInstructionParser.Direction.KEEP_RIGHT -> "Manter à direita"
                MapsInstructionParser.Direction.ROUNDABOUT -> "Rotatória, saída ${parsed.exit}"
                MapsInstructionParser.Direction.ROUNDABOUT_RIGHT -> "Direita na rotatória • número da saída ausente"
                MapsInstructionParser.Direction.EXIT_ROUNDABOUT -> "Sair da rotatória"
                MapsInstructionParser.Direction.MERGE -> "Entrar na via • lado não informado"
                null -> null
            }
            append("\nLeitura experimental: ")
            append(if (direction != null) "$direction • ${parsed.meters?.let { "$it m" } ?: "distância não informada"}" else when (parsed.state) {
                MapsInstructionParser.State.APPROACHING_ARRIVAL -> "Aproximando-se do destino"
                MapsInstructionParser.State.ARRIVED -> "Mensagem de chegada"
                MapsInstructionParser.State.RECALCULATING -> "Recálculo"
                else -> "Informação incompleta ou não reconhecida"
            })
            append("\nConteúdo de diagnóstico; não é uma instrução de navegação validada.")
        }
    }

    fun latestIcon(large: Boolean): ByteArray? {
        if (!active() || !connected) return null
        val key = present.values.lastOrNull()?.optString(if (large) "largeIcon" else "smallIcon") ?: return null
        return icons[key]?.let { Base64.decode(it, Base64.NO_WRAP) }
    }

    private fun digest(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
}
