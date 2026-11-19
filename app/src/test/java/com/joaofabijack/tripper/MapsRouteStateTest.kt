package com.joaofabijack.tripper

import org.junit.Assert.*
import org.junit.Test

class MapsRouteStateTest {
    private fun parse(title: String) = MapsInstructionParser.parse(title, "navigation", true)

    @Test fun aNewManeuverReplacesBothDirectionAndDistance() {
        val route = MapsRouteState(0)
        route.posted("route", parse("200 m · Turn right"), 100)
        val next = route.posted("route", parse("1.5 km · Turn left"), 200)
        assertEquals(TripperProtocol.Maneuver.LEFT, next.maneuver)
        assertEquals(1500, next.meters)
    }

    @Test fun incompleteUnknownAndRecalculatingNeverKeepThePreviousArrow() {
        for (title in listOf("Turn right", "200 m · Unknown instruction", "Rerouting...", "Head toward Test Road")) {
            val route = MapsRouteState(0)
            route.posted("route", parse("200 m · Turn left"), 100)
            val next = route.posted("route", parse(title), 200)
            assertNull(title, next.maneuver)
            assertNull(title, next.meters)
            assertFalse(next.stop)
        }
    }

    @Test fun unrelatedNotificationsAndTheirRemovalDoNotClearTheRoute() {
        val route = MapsRouteState(0)
        val first = route.posted("route", parse("200 m · Turn right"), 100)
        assertEquals(first, route.posted("other", MapsInstructionParser.parse("Offer", "promo", false), 200))
        assertEquals(first, route.removed("other", 300))
        assertTrue(route.removed("route", 400).stop)
    }

    @Test fun removingOldKeyDoesNotStopNewSourceAndRemovalNeverResurrectsOldOne() {
        val route = MapsRouteState(0)
        route.posted("old", parse("200 m · Turn right"), 100)
        val fresh = route.posted("new", parse("50 m · Turn left"), 200)
        assertEquals(fresh, route.removed("old", 300))
        assertTrue(route.removed("new", 400).stop)
        val afterEnd = route.posted("old", parse("200 m · Turn right"), 500)
        assertTrue(afterEnd.stop)
        assertNull(afterEnd.maneuver)
    }

    @Test fun freshnessExpiresAtThirtySecondsAndRequiresNewDataToResume() {
        val route = MapsRouteState(0)
        route.posted("route", parse("200 m · Turn right"), 100)
        assertNotNull(route.current(30_099).maneuver)
        assertNull(route.current(30_100).maneuver)
        assertNull(route.current(31_000).meters)
        assertEquals(TripperProtocol.Maneuver.LEFT, route.posted("route", parse("50 m · Turn left"), 32_000).maneuver)
    }

    @Test fun snapshotsKeepOriginalAgeAndRejectOldOrFutureTimestamps() {
        for (age in listOf(-1L, 30_000L, 90_000L)) {
            val route = MapsRouteState(100_000)
            assertNull(route.posted("route", parse("200 m · Turn right"), 100_000, age).maneuver)
        }
        val route = MapsRouteState(100_000)
        assertNotNull(route.posted("route", parse("200 m · Turn right"), 100_000, 29_000).maneuver)
        assertNull(route.current(101_000).maneuver)
    }

    @Test fun arrivalApproachIsNotCompletionOrZeroDistance() {
        val route = MapsRouteState(0)
        route.posted("route", parse("50 m · Turn left"), 100)
        val approaching = route.posted("route", parse("Arriving"), 200)
        assertFalse(approaching.stop)
        assertNull(approaching.meters)
        assertNull(approaching.maneuver)
        assertTrue(route.removed("route", 300).stop)
    }

    @Test fun explicitArrivalEndsSessionAndDoesNotShowAnInventedDistance() {
        val route = MapsRouteState(0)
        val arrival = route.posted("route", parse("You have arrived"), 100)
        assertTrue(arrival.stop)
        assertNull(arrival.maneuver)
    }

    @Test fun groupSummaryCannotBecomeGuidanceAndSourceChangingCategoryClearsGuidance() {
        val route = MapsRouteState(0)
        val first = route.posted("route", parse("50 m · Turn left"), 100)
        assertEquals(first, route.posted("summary", MapsInstructionParser.parse("200 m · Turn right", "navigation", true, true), 200))
        assertNull(route.posted("route", MapsInstructionParser.parse("200 m · Turn right", "promo", true), 300).maneuver)
    }

    @Test fun roundaboutsWithoutExplicitExitAreNotGuessed() {
        val route = MapsRouteState(0)
        for (title in listOf("100 m · At the roundabout, turn right onto Test Road", "100 m · Exit the roundabout")) {
            assertNull(route.posted("route", parse(title), 100).maneuver)
        }
        val numbered = route.posted("route", parse("200 m · At the roundabout, take the 4th exit"), 200)
        assertEquals(4, numbered.exit)
        assertEquals(TripperProtocol.Maneuver.ROUNDABOUT, numbered.maneuver)
    }

    @Test fun softTurnsKeepAndMergeHaveDistinctProtocolSymbols() {
        val cases = mapOf("Slight left" to 0x18, "Slight right" to 0x19, "Keep left" to 0x28,
            "Keep right" to 0x27, "Merge onto Test Road" to 0x1b)
        for ((title, expected) in cases) {
            val guidance = MapsRouteState(0).posted("route", parse("200 m · $title"), 100)
            val packet = TripperProtocol.maneuver(guidance.maneuver!!, guidance.meters!!, guidance.exit)
            assertEquals(title, expected, packet[2].toInt() and 255)
        }
    }

    @Test fun newSessionCannotReusePreviousRoute() {
        MapsRouteState(0).posted("route", parse("200 m · Turn right"), 100)
        val next = MapsRouteState(1000).current(1000)
        assertNull(next.maneuver)
        assertNull(next.meters)
        assertFalse(next.stop)
    }
}
