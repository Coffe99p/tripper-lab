package com.joaofabijack.tripper

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Intent
import android.content.ComponentName
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.*
import android.util.Log
import java.io.File
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.ArrayDeque

/** All mutable session state and callbacks are confined to the main looper.
 * Every BLE callback captures a session generation so cancelled sessions cannot revive.
 * Runtime permission checks gate entry; later revocation is handled by guarded(). */
@SuppressLint("MissingPermission")
class TripperService : Service() {
    enum class State { IDLE, SEARCHING, CHOOSING, CONNECTING, PIN, READY, ERROR, WAITING }
    data class Candidate(val address: String, val name: String, val rssi: Int)
    data class Snapshot(
        val state: State, val message: String, val devices: List<Candidate>,
        val sent: Int, val received: Int, val simulation: String, val confirmed: Boolean, val hasSimulation: Boolean,
        val log: String, val mapsActive: Boolean, val mapsStatus: String,
    )
    inner class LocalBinder : Binder() { val service: TripperService get() = this@TripperService }
    private data class Write(val bytes: ByteArray, val label: String, val done: (() -> Unit)? = null)

    private val handler = Handler(Looper.getMainLooper())
    private val binder = LocalBinder()
    private val manager by lazy { getSystemService(BluetoothManager::class.java) }
    private val prefs by lazy { getSharedPreferences("tripper", MODE_PRIVATE) }
    private val candidates = linkedMapOf<String, Pair<Candidate, BluetoothDevice>>()
    private val queue = ArrayDeque<Write>()
    private val logs = ArrayDeque<String>()
    private var generation = 0
    private var state = State.IDLE
    private var message = "Pronto para procurar o Tripper."
    private var scanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    private var gatt: BluetoothGatt? = null
    private var server: BluetoothGattServer? = null
    private var characteristic: BluetoothGattCharacteristic? = null
    private var pending: Write? = null
    private var writeTimeout: Runnable? = null
    private var connectTimeout: Runnable? = null
    private var pinTimeout: Runnable? = null
    private var screen: ByteArray? = null
    private var simulation = "Nenhum comando simulado selecionado."
    private var selectedAddress: String? = null
    private var confirmed = false
    private var sent = 0
    private var received = 0
    private var lastSent = 0L
    private var wakeLock: PowerManager.WakeLock? = null
    private var active = false
    private val watch = SavedTripperWatch()
    private val retryWatch: Runnable = Runnable { refreshWatch() }
    private var sessionStartedAt = 0L
    private var resumeMapsAfterDrop = false
    private var logFlushScheduled = false
    private val logFlush = Runnable { flushLog() }
    private val bluetoothState = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_ON -> refreshWatch()
                BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF -> {
                    if (active) fail("Bluetooth do celular desligado.")
                    else cancelWatch("Bluetooth desligado. Ligue-o para aguardar o Tripper.")
                }
            }
        }
    }
    private var mapsRoute: MapsRouteState? = null
    private var mapsClosing = false
    private var lastGuidance: MapsRouteState.Guidance? = null
    var listener: ((Snapshot) -> Unit)? = null
        set(value) { field = value; publish() }

    companion object {
        const val SCAN = "com.joaofabijack.tripper.SCAN"
        const val RECONNECT = "com.joaofabijack.tripper.RECONNECT"
        private const val KEEP_SESSION = "com.joaofabijack.tripper.KEEP_SESSION"
        const val STOP = "com.joaofabijack.tripper.STOP"
        private const val CHANNEL = "tripper_test"
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        BatteryLab.init(this)
        BatteryLab.onMapsOnlyChanged = ::refreshWatch
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        // This protected system broadcast can originate from the privileged Bluetooth app.
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(bluetoothState, filter, RECEIVER_EXPORTED)
        else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(bluetoothState, filter)
        }
        runCatching {
            val saved = File(filesDir, "last-session.txt")
            if (saved.isFile && saved.length() < 100_000) saved.readLines().takeLast(100).forEach(logs::addLast)
        }
        prefs.getString("lastOutcome", null)?.let { message = "Última sessão: $it" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Teste do Tripper", NotificationManager.IMPORTANCE_LOW),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Discovery already started the session synchronously while a screen was visible.
        if (intent?.action == KEEP_SESSION) {
            if (!active) stopSelf(startId)
            return START_NOT_STICKY
        }
        if (intent?.action == STOP) { stopSession(); return START_NOT_STICKY }
        if (intent?.action !in listOf(SCAN, RECONNECT)) { stopSelf(); return START_NOT_STICKY }
        if (!active) guarded { startSession(intent?.action == RECONNECT) }
        return START_NOT_STICKY
    }

    fun snapshot() = Snapshot(state, message, candidates.values.map { it.first }, sent, received,
        simulation, confirmed, screen != null && mapsRoute == null, logs.joinToString("\n"), mapsRoute != null,
        if (mapsRoute != null) simulation else when (state) {
            State.READY -> "Maps: envio desligado. Toque em Usar Maps no Tripper."
            State.ERROR -> "Maps: envio desligado porque a conexão foi interrompida."
            State.CONNECTING, State.PIN -> "Maps: aguardando a conexão com o Tripper."
            else -> "Maps: envio desligado. Conecte o Tripper para ativar."
        })

    fun hasSavedDevice(): Boolean = prefs.contains("address")
    fun isSelectedDeviceSaved(): Boolean = selectedAddress != null && selectedAddress == prefs.getString("address", null)

    private fun permissionsGranted(): Boolean = listOf(Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT).all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    fun screenOpened() {
        watch.open()
        refreshWatch()
    }

    fun screenClosed() {
        watch.close()
        if (!canWatch()) cancelWatch()
    }

    fun pauseWatch() {
        watch.pause()
        cancelWatch()
    }

    private fun canWatch() = watch.canWatch(active, BatteryLab.measuringMapsOnly())

    private fun cancelWatch(reason: String = "Espera pelo Tripper encerrada.") {
        handler.removeCallbacks(retryWatch)
        if (state == State.WAITING) {
            stopScan()
            change(State.IDLE, reason)
        }
    }

    private fun refreshWatch(): Unit = guarded {
        if (!canWatch()) { cancelWatch(); return@guarded }
        if (scanCallback != null || handler.hasCallbacks(retryWatch) || !permissionsGranted()) return@guarded
        val saved = prefs.getString("address", null) ?: return@guarded
        val adapter = manager.adapter ?: return@guarded
        if (!adapter.isEnabled || !BluetoothAdapter.checkBluetoothAddress(saved)) return@guarded
        val ble = adapter.bluetoothLeScanner ?: return@guarded
        val token = generation
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) = route(token) {
                if (scanCallback !== this || !canWatch() || result.device.address != saved) return@route
                if (prefs.getString("address", null) != saved) return@route
                stopScan()
                // Promote the bound service only on discovery. A stale start intent cannot reconnect.
                startService(Intent(this@TripperService, TripperService::class.java).setAction(KEEP_SESSION))
                startSession(true)
            }

            override fun onScanFailed(errorCode: Int) = route(token) {
                if (scanCallback !== this) return@route
                stopScan()
                change(State.WAITING, "A espera pelo Tripper falhou ($errorCode). Nova busca em 30 segundos.")
                handler.postDelayed(retryWatch, 30_000)
            }
        }
        scanner = ble
        scanCallback = callback
        ble.startScan(listOf(ScanFilter.Builder().setDeviceAddress(saved).build()),
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build(), callback)
        change(State.WAITING, "Ligue a ignição. O app vai conectar quando encontrar o seu Tripper.")
    }

    private fun startSession(reconnect: Boolean) {
        if (!permissionsGranted()) { fail("Autorize dispositivos próximos para iniciar."); return }
        if (manager.adapter?.isEnabled != true) { fail("Ligue o Bluetooth do celular e tente novamente."); return }
        cleanup()
        active = true
        sessionStartedAt = SystemClock.elapsedRealtime()
        prefs.edit().putString("lastOutcome", "A sessão anterior foi interrompida. Reconecte para iniciar outra; nenhuma sessão é retomada sozinha.").apply()
        // Keep one preceding attempt so a retry does not erase the failure under investigation.
        runCatching {
            val previous = File(filesDir, "last-session.txt")
            if (previous.isFile && previous.length() < 100_000) {
                File(filesDir, "previous-session.txt").writeText(previous.readText())
            }
        }
        logs.clear(); candidates.clear(); sent = 0; received = 0; confirmed = false
        screen = null; simulation = "Nenhum comando simulado selecionado."
        startForeground(3, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        log("Sessão ${BuildConfig.VERSION_NAME} iniciada.")
        val saved = prefs.getString("address", null)
        if (reconnect && saved != null && BluetoothAdapter.checkBluetoothAddress(saved)) {
            connect(manager.adapter.getRemoteDevice(saved), resume = true)
        } else startScan()
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, TripperService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_tripper)
            .setContentTitle(if (mapsRoute != null) "Tripper Lab • Maps no Tripper" else "Tripper Lab • conexão Bluetooth")
            .setContentText("Bluetooth ativo. Toque para abrir ou encerrar.")
            .setContentIntent(open).setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Encerrar", stop).build()).build()
    }

    private fun startScan() {
        val token = generation
        scanner = manager.adapter.bluetoothLeScanner ?: run { fail("Busca Bluetooth indisponível."); return }
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) = route(token) {
                if (state != State.SEARCHING) return@route
                val address = result.device.address
                val name = result.scanRecord?.deviceName ?: result.device.name ?: "Tripper candidato"
                candidates[address] = Candidate(address, name, result.rssi) to result.device
                publish()
            }
            override fun onScanFailed(errorCode: Int) = route(token) { fail("Busca falhou (código $errorCode). Aguarde alguns segundos e tente novamente.") }
        }
        change(State.SEARCHING, "Procurando por 20 segundos. Ligue o Tripper agora e selecione-o abaixo.")
        scanner!!.startScan(listOf(
            ScanFilter.Builder().setDeviceName(TripperProtocol.DEVICE_NAME).build(),
            ScanFilter.Builder().setServiceUuid(ParcelUuid(TripperProtocol.SERVICE_UUID)).build(),
        ), ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), scanCallback!!)
        handler.postDelayed({ guarded {
            if (state == State.SEARCHING) {
                stopScan()
                if (candidates.isEmpty()) fail("Tripper não encontrado. Desligue e ligue a ignição durante uma nova busca; feche a conexão do app Royal.")
                else change(State.CHOOSING, "Busca encerrada. Selecione o seu Tripper.")
            }
        } }, 20_000)
    }

    fun select(address: String) = guarded {
        if (state !in listOf(State.SEARCHING, State.CHOOSING)) return@guarded
        val device = candidates[address]?.second ?: return@guarded
        stopScan()
        connect(device, resume = address == prefs.getString("address", null))
    }

    private fun connect(device: BluetoothDevice, resume: Boolean) {
        val token = generation
        // The Pod needs regular writes while the phone sleeps. Renew only while the link is active.
        if (wakeLock == null) wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TripperLab:connection").apply {
                setReferenceCounted(false)
                acquire(120_000L)
            }
        selectedAddress = device.address
        change(State.CONNECTING, "Conectando ao Tripper • ${device.address.takeLast(5)}…")
        connectTimeout = Runnable { fail("O Tripper não concluiu a conexão em 25 segundos.", retryOnDiscovery = true) }
        handler.postDelayed(connectTimeout!!, 25_000)
        val localService = BluetoothGattService(TripperProtocol.SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        localService.addCharacteristic(BluetoothGattCharacteristic(TripperProtocol.CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE))
        server = manager.openGattServer(this, object : BluetoothGattServerCallback() {
            override fun onServiceAdded(status: Int, service: BluetoothGattService) = route(token) {
                if (status != BluetoothGatt.GATT_SUCCESS) { fail("Não foi possível preparar o canal de resposta ($status)."); return@route }
                gatt = device.connectGatt(this@TripperService, false, callback(token, resume), BluetoothDevice.TRANSPORT_LE)
                if (gatt == null) fail("O Android não abriu a conexão Bluetooth.")
            }
            override fun onCharacteristicWriteRequest(peer: BluetoothDevice, requestId: Int,
                ch: BluetoothGattCharacteristic, preparedWrite: Boolean, responseNeeded: Boolean,
                offset: Int, value: ByteArray) {
                route(token) {
                    val valid = peer.address == selectedAddress && ch.uuid == TripperProtocol.CHARACTERISTIC_UUID && !preparedWrite && offset == 0
                    if (responseNeeded) server?.sendResponse(peer, requestId,
                        if (valid) BluetoothGatt.GATT_SUCCESS else BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, offset, null)
                    if (valid) {
                        received++
                        // Responses are not decoded as authentication success: their semantics are unknown.
                        if (received == 1 || received % 30 == 0)
                            log("Respostas do Tripper: $received; conteúdo omitido para não registrar PIN.")
                        publish()
                    }
                }
            }
            override fun onCharacteristicReadRequest(peer: BluetoothDevice, requestId: Int, offset: Int, ch: BluetoothGattCharacteristic) = route(token) {
                server?.sendResponse(peer, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, offset, null)
            }
            override fun onExecuteWrite(peer: BluetoothDevice, requestId: Int, execute: Boolean) = route(token) {
                server?.sendResponse(peer, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, 0, null)
            }
        })
        if (server?.addService(localService) != true) fail("Não foi possível abrir o canal de resposta do Tripper.")
    }

    private fun callback(token: Int, resume: Boolean) = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(client: BluetoothGatt, status: Int, newState: Int) = route(token) {
            if (client !== gatt) return@route
            if (status != BluetoothGatt.GATT_SUCCESS || newState == BluetoothProfile.STATE_DISCONNECTED) {
                fail(if (status == 147) "O Bluetooth perdeu a comunicação com o Tripper (147). Reconecte; se repetir junto à moto, tente novo PIN."
                    else "Conexão encerrada pelo Bluetooth (código $status).", retryOnDiscovery = true)
            } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                log("Link Bluetooth aberto; verificando serviço do Tripper.")
                if (!client.discoverServices()) fail("Não foi possível consultar os serviços do Tripper.")
            }
        }
        override fun onServicesDiscovered(client: BluetoothGatt, status: Int) = route(token) {
            if (client !== gatt) return@route
            if (status != BluetoothGatt.GATT_SUCCESS) { fail("Falha ao consultar serviços ($status)."); return@route }
            val ch = client.getService(TripperProtocol.SERVICE_UUID)?.getCharacteristic(TripperProtocol.CHARACTERISTIC_UUID)
            val writable = BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
            if (ch == null || ch.properties and writable == 0) {
                fail("O dispositivo não apresenta o serviço de escrita esperado do Tripper Pod."); return@route
            }
            characteristic = ch
            log("Serviço do Tripper reconhecido. Aguardando antes do primeiro comando.")
            handler.postDelayed({ guarded {
                if (token != generation) return@guarded
                enqueue(TripperProtocol.handshake(!resume), if (resume) "Retomar sessão" else "Solicitar PIN") {
                    connectTimeout?.let(handler::removeCallbacks)
                    startHeartbeat()
                    if (resume) finishHandshake() else {
                        change(State.PIN, "Digite o PIN que apareceu no Tripper. O código não será salvo.")
                        pinTimeout = Runnable { fail("Tempo para informar o PIN esgotado. Inicie uma nova conexão.") }
                        handler.postDelayed(pinTimeout!!, 120_000)
                    }
                }
            } }, 250)
        }
        override fun onCharacteristicWrite(client: BluetoothGatt, ch: BluetoothGattCharacteristic, status: Int) = route(token) {
            if (client !== gatt || ch.uuid != TripperProtocol.CHARACTERISTIC_UUID) return@route
            val write = pending ?: return@route
            writeTimeout?.let(handler::removeCallbacks)
            writeTimeout = null; pending = null
            if (status != BluetoothGatt.GATT_SUCCESS) { fail("Falha na transmissão Bluetooth ($status).", retryOnDiscovery = true); return@route }
            sent++; lastSent = SystemClock.elapsedRealtime()
            if (write.label != "Manutenção") log("Enviado: ${write.label}.")
            write.done?.invoke()
            pump(); publish()
        }
    }

    fun submitPin(pin: String) = guarded {
        if (state != State.PIN) return@guarded
        val packet = runCatching { TripperProtocol.pin(pin) }.getOrNull() ?: run {
            message = "Use somente os 1 a 6 dígitos mostrados no Tripper."; publish(); return@guarded
        }
        pinTimeout?.let(handler::removeCallbacks)
        change(State.CONNECTING, "Enviando PIN. A aceitação será confirmada no mostrador.")
        enqueue(packet, "PIN (oculto)") { finishHandshake() }
    }

    private fun finishHandshake() {
        val token = generation
        handler.postDelayed({ guarded {
            if (token != generation) return@guarded
            val now = LocalTime.now()
            enqueue(TripperProtocol.time(now.hour, now.minute), "Sincronizar relógio (24 h)") {
                handler.postDelayed({ guarded {
                    if (token != generation) return@guarded
                    enqueue(TripperProtocol.ping(), "Manutenção") {
                        enqueue(TripperProtocol.ping(), "Manutenção") {
                            if (isSelectedDeviceSaved()) watch.resume()
                            change(State.READY, "Conexão preparada. Para instruções reais, toque em Usar Maps no Tripper.")
                            if (resumeMapsAfterDrop) {
                                resumeMapsAfterDrop = false
                                startMaps()
                            }
                        }
                    }
                } }, 150)
            }
        } }, 200)
    }

    fun sendSimulation(maneuver: TripperProtocol.Maneuver, meters: Int, description: String) = guarded {
        if (state != State.READY || mapsRoute != null) return@guarded
        val bytes = TripperProtocol.maneuver(maneuver, meters)
        screen = bytes
        simulation = description
        // Drop queued screen refreshes when the requested maneuver changes.
        queue.removeAll { it.label == "Manutenção" || it.label.startsWith("Simulação") }
        enqueue(bytes, "Simulação: $description")
        publish()
    }

    fun startMaps() = guarded {
        if (state != State.READY || mapsRoute != null) return@guarded
        if (selectedAddress != prefs.getString("address", null)) {
            message = "Primeiro envie uma simulação e confirme que funcionou no mostrador para salvar o Tripper."
            log("Ativação do Maps pendente: dispositivo ainda não confirmado pelo usuário.")
            publish(); return@guarded
        }
        val access = getSystemService(NotificationManager::class.java).isNotificationListenerAccessGranted(
            ComponentName(this, MapsNotificationListener::class.java))
        val source = MapsNotificationListener.instance
        if (!access || source == null) {
            message = "Autorize o acesso às notificações na tela inicial e aguarde a conexão com o Maps."
            log("Ativação do Maps pendente: acesso=$access; listener conectado=${source != null}.")
            publish(); return@guarded
        }
        mapsRoute = MapsRouteState(SystemClock.elapsedRealtime())
        lastGuidance = null; mapsClosing = false
        // Drop previous simulated screens before accepting this separate, explicit mode.
        queue.removeAll { it.label == "Manutenção" || it.label.startsWith("Simulação") }
        val token = generation
        MapsNavigationFeed.start { event -> guarded feed@{
            if (token != generation || mapsClosing) return@feed
            val route = mapsRoute ?: return@feed
            val next = when (event) {
                is MapsNavigationFeed.Event.Posted -> route.posted(event.key, event.result, event.at, event.age)
                is MapsNavigationFeed.Event.Removed -> route.removed(event.key, event.at)
                MapsNavigationFeed.Event.Unavailable -> {
                    mapsRoute = MapsRouteState(SystemClock.elapsedRealtime())
                    MapsRouteState.Guidance(status = "Leitor de notificações indisponível. Aguardando o Maps.")
                }
            }
            applyGuidance(next)
        } }
        message = "Maps ligado ao Tripper. Aguardando instruções de navegação."
        log("Envio do Maps ativado pelo usuário; sem gravação de textos de percurso.")
        getSystemService(NotificationManager::class.java).notify(3, notification())
        applyGuidance(mapsRoute!!.current(SystemClock.elapsedRealtime()))
        source.snapshotNavigation()
    }

    private fun applyGuidance(guidance: MapsRouteState.Guidance) {
        if (mapsClosing || lastGuidance == guidance) return
        lastGuidance = guidance
        if (guidance.stop) mapsRoute = MapsRouteState(SystemClock.elapsedRealtime())
        val bytes = guidance.maneuver?.let { TripperProtocol.maneuver(it, guidance.meters!!, guidance.exit) }
            ?: TripperProtocol.waiting()
        simulation = "Maps: ${guidance.status}" + if (guidance.maneuver == null) "\nMostrador: símbolo de espera/recálculo." else ""
        // Coalesce queued frames; a write already accepted by Android cannot be revoked.
        val changed = screen?.contentEquals(bytes) != true
        screen = bytes
        if (changed) {
            queue.removeAll { it.label == "Manutenção" || it.label.startsWith("Maps:") }
            enqueue(bytes, "Maps: ${guidance.status}")
        }
        publish()
    }

    fun confirmDisplay() {
        if (state != State.READY || screen == null || mapsRoute != null) return
        val address = selectedAddress ?: return
        prefs.edit().putString("address", address).apply()
        watch.resume()
        confirmed = true
        message = "Você confirmou a exibição. Tripper salvo para reconectar."
        log("Usuário confirmou a exibição no mostrador; dispositivo salvo.")
        publish()
    }

    fun forgetDevice() {
        stopSession("Tripper esquecido. Na próxima conexão será solicitado um novo PIN.")
        prefs.edit().remove("address").apply()
        publish()
    }

    private fun startHeartbeat() {
        val token = generation
        var renewLockAt = SystemClock.elapsedRealtime() + 60_000L
        handler.postDelayed(object : Runnable {
            override fun run() = guarded {
                if (token != generation || !active || characteristic == null) return@guarded

                val now = SystemClock.elapsedRealtime()
                if (now >= renewLockAt) {
                    wakeLock?.acquire(120_000L)
                    renewLockAt = now + 60_000L
                }
                if (!mapsClosing) mapsRoute?.let { applyGuidance(it.current(SystemClock.elapsedRealtime())) }
                if (token != generation || !active) return@guarded
                if (pending == null && queue.isEmpty() && SystemClock.elapsedRealtime() - lastSent >= 1_800) {
                    enqueue(screen ?: TripperProtocol.ping(), "Manutenção")
                }
                handler.postDelayed(this, 1_000)
            }
        }, 1_000)
    }

    private fun enqueue(bytes: ByteArray, label: String, done: (() -> Unit)? = null) {
        if (!active || characteristic == null) return
        if (queue.size >= 16) { fail("Fila de envio excedida; sessão encerrada."); return }
        queue.add(Write(bytes.copyOf(), label, done))
        pump()
    }

    private fun pump() {
        if (pending != null || queue.isEmpty()) return

        val client = gatt ?: return
        val ch = characteristic ?: return
        val write = queue.removeFirst()
        pending = write
        val type = if (ch.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0)
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE else BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        val accepted = if (Build.VERSION.SDK_INT >= 33) {
            client.writeCharacteristic(ch, write.bytes, type) == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            ch.writeType = type
            @Suppress("DEPRECATION")
            ch.value = write.bytes
            @Suppress("DEPRECATION")
            client.writeCharacteristic(ch)
        }
        if (!accepted) { fail("Android recusou o envio Bluetooth.", retryOnDiscovery = true); return }
        writeTimeout = Runnable { fail("O Android não confirmou a transmissão em 4 segundos.", retryOnDiscovery = true) }
        handler.postDelayed(writeTimeout!!, 4_000)
    }

    private fun stopScan() {
        scanCallback?.let { callback -> runCatching { scanner?.stopScan(callback) } }
        scanCallback = null; scanner = null
    }

    fun stopSession(reason: String = "Sessão encerrada. Para buscar de novo, pode ser necessário desligar e ligar a ignição.") {
        pauseWatch()
        if (mapsClosing) return
        if (mapsRoute != null && state == State.READY && active) {
            mapsClosing = true
            MapsNavigationFeed.stop()
            screen = TripperProtocol.waiting()
            queue.clear()
            simulation = "Encerrando Maps e removendo a orientação anterior."
            message = simulation
            // Replace the arrow before disconnecting; retain the existing write timeout
            // and also bound the complete close in case an older write is still in flight.
            val token = generation
            handler.postDelayed({ if (token == generation) finishStop(reason) }, 4_500)
            enqueue(screen!!, "Maps: retirar orientação") { finishStop(reason) }
            publish()
            return
        }
        finishStop(reason)
    }

    private fun finishStop(reason: String) {
        cleanup()
        candidates.clear()
        change(State.IDLE, reason)
        prefs.edit().putString("lastOutcome", reason).apply()
        flushLog()
        stopSelf()
    }

    private fun fail(reason: String, retryOnDiscovery: Boolean = false) {
        val retrySaved = retryOnDiscovery && active && !mapsClosing && isSelectedDeviceSaved()
        if (active) {
            val now = SystemClock.elapsedRealtime()
            val age = if (sent > 0) "${now - lastSent} ms" else "nenhum envio confirmado"
            log("Diagnóstico da falha: estado=$state; Maps ativo=${mapsRoute != null}; " +
                "duração=${(now - sessionStartedAt) / 1000} s; envios confirmados pelo Android=$sent; " +
                "respostas do Tripper=$received; tempo desde último envio=$age.")
        }
        // A ride can lose radio contact with Maps in front or with the phone locked.
        // Try the same confirmed Pod once. A failed attempt falls back to visible-screen discovery.
        if (retrySaved && state == State.READY && permissionsGranted() && manager.adapter?.isEnabled == true) {
            val device = manager.adapter.getRemoteDevice(requireNotNull(selectedAddress))
            val hadMaps = mapsRoute != null
            cleanup(keepForeground = true)
            resumeMapsAfterDrop = hadMaps
            change(State.CONNECTING, "Comunicação interrompida. Tentando reconectar ao Tripper salvo.")
            handler.postDelayed({ guarded { connect(device, resume = true) } }, 1_000)
            return
        }
        cleanup()
        candidates.clear()
        change(State.ERROR, reason)
        prefs.edit().putString("lastOutcome", reason).apply()
        flushLog()
        stopSelf()
        if (retrySaved && canWatch() && permissionsGranted()) {
            change(State.WAITING, "$reason A busca automática recomeça em 10 segundos.")
            handler.postDelayed(retryWatch, 10_000)
        }
    }

    private fun cleanup(keepForeground: Boolean = false) {
        flushLog()
        generation++
        MapsNavigationFeed.stop()
        mapsRoute = null; lastGuidance = null; mapsClosing = false
        handler.removeCallbacksAndMessages(null)
        active = keepForeground
        resumeMapsAfterDrop = false
        BatteryLab.sessionChanged(false, false)
        stopScan()
        val oldGatt = gatt; gatt = null
        runCatching { oldGatt?.disconnect() }; runCatching { oldGatt?.close() }
        runCatching { server?.close() }; server = null
        characteristic = null; pending = null; queue.clear(); screen = null
        writeTimeout = null; connectTimeout = null; pinTimeout = null
        selectedAddress = null; confirmed = false
        simulation = "Nenhum comando simulado ativo."
        if (!keepForeground) {
            wakeLock?.let { if (it.isHeld) it.release() }
            wakeLock = null
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun route(token: Int, action: () -> Unit) {
        handler.post { if (token == generation) guarded(action) }
    }

    private fun guarded(action: () -> Unit) {
        try { action() }
        catch (_: SecurityException) { fail("A permissão Bluetooth foi removida. Autorize dispositivos próximos e tente de novo.") }
        catch (error: IllegalStateException) { fail("Bluetooth indisponível (${error.javaClass.simpleName}). Reinicie a sessão.") }
        catch (error: IllegalArgumentException) { fail("Parâmetro de teste inválido (${error.javaClass.simpleName}). Reinicie a sessão.") }
    }

    private fun change(next: State, text: String) { state = next; message = text; log(text); publish() }
    private fun publish() {
        BatteryLab.sessionChanged(active, mapsRoute != null && !mapsClosing)
        listener?.invoke(snapshot())
    }
    private fun log(text: String) {
        val line = "${LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))} $text"
        logs.addLast(line)
        while (logs.size > 100) logs.removeFirst()
        Log.i("TripperLab", text)
        if (!logFlushScheduled) {
            logFlushScheduled = true
            handler.postDelayed(logFlush, 5_000)
        }
    }
    private fun flushLog() {
        handler.removeCallbacks(logFlush)
        logFlushScheduled = false
        if (logs.isNotEmpty()) runCatching { File(filesDir, "last-session.txt").writeText(logs.joinToString("\n")) }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (state in listOf(State.SEARCHING, State.CHOOSING, State.PIN))
            stopSession("Busca encerrada ao remover o app dos recentes.")
    }
    override fun onDestroy() {
        BatteryLab.onMapsOnlyChanged = null
        listener = null; cleanup(); unregisterReceiver(bluetoothState); super.onDestroy()
    }
}
