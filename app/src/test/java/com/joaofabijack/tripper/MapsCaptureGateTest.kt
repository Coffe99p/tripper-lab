package com.joaofabijack.tripper

import org.junit.Assert.*
import org.junit.Test

class MapsCaptureGateTest {
    @Test fun disabledUntilUserStartsAndNeverRestartsImplicitly() {
        val gate = MapsCaptureGate()
        assertFalse(gate.accepts(MapsCaptureGate.MAPS_PACKAGE, 0))
        gate.start(100)
        assertTrue(gate.accepts(MapsCaptureGate.MAPS_PACKAGE, 101))
        assertFalse(MapsCaptureGate().accepts(MapsCaptureGate.MAPS_PACKAGE, 101))
    }
    @Test fun onlyExactMapsPackageIsAccepted() {
        val gate = MapsCaptureGate().apply { start(0) }
        for (name in listOf("com.whatsapp", "com.google.android.apps.maps.fake", "com.google.android.apps.messaging", "")) {
            assertFalse(gate.accepts(name, 1))
        }
        assertTrue(gate.accepts(MapsCaptureGate.MAPS_PACKAGE, 1))
    }
    @Test fun timeoutIsEnforcedAtEventTimeEvenWithoutTimerCallback() {
        val gate = MapsCaptureGate(1000).apply { start(50) }
        assertTrue(gate.active(1049))
        assertFalse(gate.active(1050))
        assertFalse(gate.accepts(MapsCaptureGate.MAPS_PACKAGE, 99999))
    }
    @Test fun stopImmediatelyRejectsFurtherEvents() {
        val gate = MapsCaptureGate().apply { start(50); stop() }
        assertFalse(gate.accepts(MapsCaptureGate.MAPS_PACKAGE, 51))
    }
}
