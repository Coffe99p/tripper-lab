package com.joaofabijack.tripper

import java.util.UUID

/** Independent encoder of the public, community-documented Tripper Pod wire format.
 * See docs/PROTOCOLO.md. No third-party application code. */
object TripperProtocol {
    val SERVICE_UUID: UUID = UUID.fromString("01ff0100-ba5e-f4ee-5ca1-eb1e5e4b1ce0")
    val CHARACTERISTIC_UUID: UUID = UUID.fromString("01ff0101-ba5e-f4ee-5ca1-eb1e5e4b1ce0")
    const val DEVICE_NAME = "RE_DISP"

    enum class Maneuver(val code: Int) {
        LEFT(0x14), RIGHT(0x15), STRAIGHT(0x09), ARRIVAL(0x00), ROUNDABOUT_SECOND(0x33),
        SLIGHT_LEFT(0x18), SLIGHT_RIGHT(0x19), KEEP_LEFT(0x28), KEEP_RIGHT(0x27),
        MERGE(0x1b), ROUNDABOUT(0x31),
    }

    fun crc16(bytes: ByteArray): Int {
        var crc = 0xffff
        for (byte in bytes) {
            crc = crc xor ((byte.toInt() and 0xff) shl 8)
            repeat(8) {
                crc = ((crc shl 1) xor (if (crc and 0x8000 != 0) 0x1021 else 0)) and 0xffff
            }
        }
        return crc
    }

    private fun packet(fill: (ByteArray) -> Unit): ByteArray {
        val body = ByteArray(18).also(fill)
        val checksum = crc16(body)
        return body + byteArrayOf((checksum ushr 8).toByte(), checksum.toByte())
    }

    fun handshake(newDevice: Boolean): ByteArray = packet {
        it[0] = 0x21
        it[1] = if (newDevice) 1 else 0
    }

    fun ping(): ByteArray = packet { it[0] = 0x03 }

    fun pin(value: String): ByteArray {
        require(value.matches(Regex("[0-9]{1,6}"))) { "O PIN deve ter de 1 a 6 dígitos." }
        return packet {
            it[0] = 0x20
            value.toByteArray(Charsets.US_ASCII).copyInto(it, 1)
        }
    }

    fun time(hour: Int, minute: Int): ByteArray {
        require(hour in 0..23 && minute in 0..59)
        return packet { it[0] = 0x50; it[1] = hour.toByte(); it[2] = minute.toByte() }
    }

    /** Non-directional loading/recalculation screen; visual behavior requires hardware confirmation. */
    fun waiting(): ByteArray = packet { it[0] = 0x10; it[1] = 0x11; it[2] = 0x1c }

    fun maneuver(maneuver: Maneuver, meters: Int, exit: Int? = null): ByteArray {
        require(meters in 0..6_553_500) { "Distância fora do intervalo do protocolo." }
        require(maneuver != Maneuver.ROUNDABOUT || exit in 1..9) { "Informe uma saída de 1 a 9." }
        val distance = if (meters <= 999) meters else meters / 100
        return packet {
            // Counter-clockwise roundabouts, right-hand traffic (Brazil).
            it[0] = 0x10; it[1] = 0x11
            it[2] = (if (maneuver == Maneuver.ROUNDABOUT) 0x31 + exit!! else maneuver.code).toByte()
            it[3] = (distance ushr 8).toByte(); it[4] = distance.toByte()
            it[5] = if (meters <= 999) 1 else 2
            // Day display, no second arrow, radar or bottom-line data.
            for (index in intArrayOf(7, 8, 9, 11, 12, 13)) it[index] = 0xff.toByte()
            it[10] = 0x41
        }
    }
}
