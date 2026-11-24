package com.joaofabijack.tripper

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedTripperWatchTest {
    private val watch = SavedTripperWatch()

    @Test fun waitsOnlyWhileThereIsAVisibleScreen() {
        assertFalse(watch.canWatch(false, false))
        watch.open()
        watch.open()
        watch.close()
        assertTrue(watch.canWatch(false, false))
        watch.close()
        assertFalse(watch.canWatch(false, false))
    }

    @Test fun stoppingPausesUntilTheNextVisit() {
        watch.open()
        watch.pause()
        watch.open()
        watch.close()
        assertFalse(watch.canWatch(false, false))
        watch.close()
        watch.open()
        assertTrue(watch.canWatch(false, false))
    }

    @Test fun existingSessionAndMapsOnlyMeasurementBlockAutoConnect() {
        watch.open()
        assertFalse(watch.canWatch(true, false))
        assertFalse(watch.canWatch(false, true))
        assertTrue(watch.canWatch(false, false))
    }
}
