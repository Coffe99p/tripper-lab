package com.joaofabijack.tripper

import org.junit.Assert.*
import org.junit.Test

class BatteryRunTest {
    private fun sample(time: Long = 0, percent: Int? = 80, charge: Long? = 4_000_000,
        plugged: Boolean? = false, screen: Boolean = false) =
        BatteryRun.Sample(time, percent, charge, 28.0, plugged, screen)

    @Test fun computesWholeDeviceChargeInMahAndHourlyRate() {
        val result = BatteryRun(BatteryRun.Mode.MAPS_ONLY, sample()).finish(sample(1_800_000, 78, 3_900_000))
        assertTrue(result.comparable)
        assertEquals(2, result.percentDrop)
        assertEquals(100.0, result.chargeMah!!, 0.001)
        assertEquals(200.0, result.mahPerHour!!, 0.001)
    }
    @Test fun chargingInTheMiddleInvalidatesEvenWhenBothEndpointsAreUnplugged() {
        val run = BatteryRun(BatteryRun.Mode.MAPS_ONLY, sample())
        run.observe(sample(100_000, plugged = true))
        run.observe(sample(200_000))
        val result = run.finish(sample(1_800_000, 78, 3_900_000))
        assertFalse(result.comparable)
        assertNull(result.mahPerHour)
    }
    @Test fun missingChargeCounterDoesNotBecomeZeroConsumption() {
        val result = BatteryRun(BatteryRun.Mode.MAPS_ONLY, sample(charge = null))
            .finish(sample(1_800_000, 78, null))
        assertTrue(result.comparable)
        assertNull(result.chargeMah)
        assertNull(result.mahPerHour)
        assertEquals(2, result.percentDrop)
    }
    @Test fun unchangedBatteryIsNotProofOfZeroConsumption() {
        val result = BatteryRun(BatteryRun.Mode.MAPS_ONLY, sample()).finish(sample(1_800_000))
        assertFalse(result.comparable)
        assertNull(result.mahPerHour)
    }
    @Test fun shortAndExcessivelyLongRunsHaveNoHourlyEstimate() {
        for (duration in listOf(1_000L, BatteryRun.MIN_MS - 1, BatteryRun.MAX_MS + 1)) {
            val result = BatteryRun(BatteryRun.Mode.MAPS_TRIPPER, sample()).finish(sample(duration, 78, 3_900_000))
            assertFalse(result.comparable)
            assertNull(result.mahPerHour)
        }
    }
    @Test fun durationBoundariesAreAcceptedWhenOtherConditionsHold() {
        for (duration in listOf(BatteryRun.MIN_MS, BatteryRun.MAX_MS)) {
            assertTrue(BatteryRun(BatteryRun.Mode.MAPS_ONLY, sample()).finish(sample(duration, 78, 3_900_000)).comparable)
        }
    }
    @Test fun recordsScreenTimeAcrossOnAndOffTransitions() {
        val run = BatteryRun(BatteryRun.Mode.MAPS_ONLY, sample(screen = true))
        run.observe(sample(120_000, screen = false))
        run.observe(sample(1_700_000, screen = true))
        assertEquals(220_000, run.finish(sample(1_800_000, 78, 3_900_000, screen = true)).screenOnMs)
    }
    @Test fun increasingCounterAndPercentageAreRejectedInsteadOfReportingSavings() {
        val run = BatteryRun(BatteryRun.Mode.MAPS_ONLY, sample())
        run.observe(sample(100_000, 81, 4_100_000))
        val result = run.finish(sample(1_800_000, 78, 3_900_000))
        assertFalse(result.comparable)
        assertNull(result.mahPerHour)
    }
    @Test fun unknownPowerAndPercentageCannotProduceComparableResult() {
        val result = BatteryRun(BatteryRun.Mode.MAPS_ONLY, sample(percent = null, plugged = null))
            .finish(sample(1_800_000, 78, 3_900_000))
        assertFalse(result.comparable)
        assertNull(result.percentDrop)
    }
    @Test fun externalInterruptionStaysInvalidAfterSubsequentReadings() {
        val run = BatteryRun(BatteryRun.Mode.MAPS_ONLY, sample())
        run.invalidate("Cenário alterado")
        val result = run.finish(sample(1_800_000, 78, 3_900_000))
        assertFalse(result.comparable)
        assertTrue(result.issues.contains("Cenário alterado"))
    }
    @Test fun monotonicClockRegressionIsRejected() {
        val run = BatteryRun(BatteryRun.Mode.MAPS_ONLY, sample(100_000))
        run.observe(sample(50_000))
        assertFalse(run.finish(sample(1_900_000, 78, 3_900_000)).comparable)
    }
}
