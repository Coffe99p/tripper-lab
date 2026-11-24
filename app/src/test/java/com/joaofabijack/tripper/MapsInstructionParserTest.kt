package com.joaofabijack.tripper

import org.junit.Assert.*
import org.junit.Test

class MapsInstructionParserTest {
    private fun parse(title: String) = MapsInstructionParser.parse(title, "navigation", true)
    @Test fun recognizesObservedPixelTitlesWithStreetNamesAnonymized() {
        val right = parse("190\u00a0m · Turn right onto Rua de teste")
        assertEquals(MapsInstructionParser.Direction.RIGHT, right.direction)
        assertEquals(190, right.meters)
        val left = parse("400\u00a0m · Turn left onto Rua de teste")
        assertEquals(MapsInstructionParser.Direction.LEFT, left.direction)
        assertEquals(400, left.meters)
    }
    @Test fun directionAndCompactDistanceMustBelongToOneRecognizedInstruction() {
        fun notification(title: String, compact: String) =
            MapsInstructionParser.fromNotification(title, compact, "navigation", true, false)
        assertEquals(80, notification("Turn left onto B", "80\u00a0m").meters)
        assertEquals(40, notification("40 m · Turn left onto B", "80 m").meters)
        assertNull(notification("Turn left onto B", "Arrive 16:02").meters)
        assertNull(notification("Head toward B", "80 m").direction)
    }

    @Test fun observedStartupAndHeadingDoNotBecomeStraightAhead() {
        for (title in listOf("Starting navigation…", "Head toward Rua de teste")) {
            assertEquals(MapsInstructionParser.State.INCOMPLETE, parse(title).state)
            assertNull(parse(title).direction)
        }
    }
    @Test fun portugueseDecimalAndNonBreakingSpacesAreHandled() {
        val result = parse("1,5\u202fkm · Vire à esquerda na Rua de teste")
        assertEquals(MapsInstructionParser.Direction.LEFT, result.direction)
        assertEquals(1500, result.meters)
    }
    @Test fun etaAndRoadNamesAreNotMistakenForManeuversOrArrival() {
        for (title in listOf("Arrive 16:02", "Rua Turn right", "200 m · Head toward Right Street", "200 m · Turn right then turn left")) {
            assertNull(parse(title).direction)
            assertNotEquals(MapsInstructionParser.State.ARRIVED, parse(title).state)
        }
        assertEquals(MapsInstructionParser.State.NOT_NAVIGATION,
            MapsInstructionParser.parse("200 m · Turn right", "recommendation", true).state)
        assertEquals(MapsInstructionParser.State.NOT_NAVIGATION,
            MapsInstructionParser.parse("200 m · Turn right", "navigation", true, true).state)
    }
    @Test fun unsupportedAndAmbiguousDistancesAreRejected() {
        for (title in listOf("1,000 m · Turn right", "200 ft · Turn right", "200 m · Make a U-turn", "999999999 km · Turn right")) {
            assertNull(parse(title).direction)
        }
    }
    @Test fun syntheticRoundaboutAndForkPatternsNeedRealWorldValidation() {
        assertEquals(2, parse("300 m · At the roundabout, take the 2nd exit onto Test Road").exit)
        assertEquals(MapsInstructionParser.Direction.KEEP_LEFT, parse("200 m · Keep left onto Test Road").direction)
        assertEquals(3, parse("200 m · Na rotatória, pegue a 3ª saída para Rua de teste").exit)
    }
    @Test fun explicitArrivalAndRecalculationAreSeparateFromGuidance() {
        assertEquals(MapsInstructionParser.State.ARRIVED, parse("You have arrived").state)
        assertEquals(MapsInstructionParser.State.RECALCULATING, parse("Rerouting…").state)
        assertNull(parse("Rerouting…").direction)
        assertNull(parse("You have arrived").meters)
    }

    @Test fun landmarkSideDoesNotOverrideTheTurnDirection() {
        assertEquals(MapsInstructionParser.Direction.LEFT,
            parse("90 m · Turn left after Example Shop (on the right) onto Test Road").direction)
        assertEquals(MapsInstructionParser.Direction.RIGHT,
            parse("200 m · Turn right after Example Shop (on the left) onto Test Road").direction)
    }

    @Test fun observedRoundaboutVariantsPreserveWhatIsKnown() {
        val numbered = parse("1.0 km · At the roundabout, take the 1st exit and stay on Test Road")
        assertEquals(MapsInstructionParser.Direction.ROUNDABOUT, numbered.direction)
        assertEquals(1, numbered.exit)
        assertEquals(1000, numbered.meters)
        val unnumbered = parse("90 m · At the roundabout, turn right onto Test Road")
        assertEquals(MapsInstructionParser.Direction.ROUNDABOUT_RIGHT, unnumbered.direction)
        assertNull(unnumbered.exit)
        assertEquals(MapsInstructionParser.Direction.EXIT_ROUNDABOUT, parse("100 m · Exit the roundabout").direction)
    }

    @Test fun slightTurnsAndMergingAreNotChangedIntoOrdinaryTurns() {
        assertEquals(MapsInstructionParser.Direction.SLIGHT_RIGHT, parse("1.2 km · Slight right to stay on Test Road").direction)
        assertEquals(MapsInstructionParser.Direction.SLIGHT_LEFT, parse("50 m · Slight left").direction)
        assertEquals(MapsInstructionParser.Direction.MERGE, parse("100 m · Merge onto Test Road").direction)
    }

    @Test fun missingDistanceNeverReusesOrInventsZeroMeters() {
        assertEquals(200, parse("200 m · Turn right").meters)
        val missing = parse("Turn right")
        assertEquals(MapsInstructionParser.State.INCOMPLETE, missing.state)
        assertEquals(MapsInstructionParser.Direction.RIGHT, missing.direction)
        assertNull(missing.meters)
        assertEquals(0, parse("0 m · Turn right").meters)
    }

    @Test fun arrivingIsNotCompletedArrivalAndADestinationNameIsNotADirection() {
        val arriving = parse("Arriving")
        assertEquals(MapsInstructionParser.State.APPROACHING_ARRIVAL, arriving.state)
        assertNull(arriving.direction)
        assertNull(arriving.meters)
        assertEquals(MapsInstructionParser.State.UNKNOWN, parse("100 m · Example Destination").state)
    }

    @Test fun kilometerConversionIsExactAndRejectsFractionalMeters() {
        assertEquals(2010, parse("2.01 km · Turn right").meters)
        assertEquals(1010, parse("1,01 km · Turn left").meters)
        assertNull(parse("1.5 m · Turn right").direction)
    }
}
