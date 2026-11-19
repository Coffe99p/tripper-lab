package com.joaofabijack.tripper

import org.junit.Assert.*
import org.junit.Test

class TripperProtocolTest {
    private fun hex(value: String) = value.split(" ").map { it.toInt(16).toByte() }.toByteArray()

    @Test fun crcMatchesStandardCheckVector() {
        assertEquals(0x29b1, TripperProtocol.crc16("123456789".toByteArray()))
    }

    @Test fun fixedPacketsMatchPublishedHardwareExamples() {
        assertArrayEquals(hex("21 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 50 A7"), TripperProtocol.handshake(true))
        assertArrayEquals(hex("21 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 45"), TripperProtocol.handshake(false))
        assertArrayEquals(hex("03 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 45 D9"), TripperProtocol.ping())
    }

    @Test fun distancesChangeUnitsAtOneKilometerWithoutSwappingSides() {
        val cases = listOf(0 to listOf(0, 0, 1), 200 to listOf(0, 200, 1),
            999 to listOf(3, 231, 1), 1000 to listOf(0, 10, 2), 22900 to listOf(0, 229, 2))
        for ((meters, expected) in cases) {
            val packet = TripperProtocol.maneuver(TripperProtocol.Maneuver.RIGHT, meters)
            assertEquals(expected, packet.slice(3..5).map { it.toInt() and 255 })
            assertEquals(0x15, packet[2].toInt())
            assertEquals(20, packet.size)
            assertEquals(listOf(255, 255, 255), packet.slice(11..13).map { it.toInt() and 255 })
        }
        assertEquals(0x14, TripperProtocol.maneuver(TripperProtocol.Maneuver.LEFT, 200)[2].toInt())
    }

    @Test fun pinPreservesLeadingZerosAndRejectsInvalidInput() {
        assertEquals("001234", TripperProtocol.pin("001234").sliceArray(1..6).toString(Charsets.US_ASCII))
        for (pin in listOf("", "1234567", "12a4", "１２３４", " 1234")) {
            assertThrows(IllegalArgumentException::class.java) { TripperProtocol.pin(pin) }
        }
    }

    @Test fun invalidDistancesAndTimesAreRejected() {
        for (meters in listOf(-1, 6_553_501)) {
            assertThrows(IllegalArgumentException::class.java) {
                TripperProtocol.maneuver(TripperProtocol.Maneuver.STRAIGHT, meters)
            }
        }
        assertThrows(IllegalArgumentException::class.java) { TripperProtocol.time(24, 0) }
        assertThrows(IllegalArgumentException::class.java) { TripperProtocol.time(12, 60) }
        assertEquals(23, TripperProtocol.time(23, 59)[1].toInt())
    }

    @Test fun waitingUsesLoadingScreenWithoutArrowOrDistancePayload() {
        val packet = TripperProtocol.waiting()
        assertEquals(listOf(0x10, 0x11, 0x1c), packet.take(3).map { it.toInt() and 255 })
        assertTrue(packet.slice(3..17).all { it == 0.toByte() })
        val crc = TripperProtocol.crc16(packet.copyOfRange(0, 18))
        assertEquals(crc, ((packet[18].toInt() and 255) shl 8) or (packet[19].toInt() and 255))
    }

    @Test fun roundaboutsUseExplicitExitAndRejectMissingOrOutOfRangeValues() {
        for ((exit, expected) in listOf(1 to 0x32, 2 to 0x33, 4 to 0x35, 9 to 0x3a)) {
            assertEquals(expected, TripperProtocol.maneuver(TripperProtocol.Maneuver.ROUNDABOUT, 200, exit)[2].toInt())
        }
        for (exit in listOf(null, 0, 10)) {
            assertThrows(IllegalArgumentException::class.java) {
                TripperProtocol.maneuver(TripperProtocol.Maneuver.ROUNDABOUT, 200, exit)
            }
        }
    }
}
