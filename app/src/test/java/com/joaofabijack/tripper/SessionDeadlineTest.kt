package com.joaofabijack.tripper

import org.junit.Assert.*
import org.junit.Test

class SessionDeadlineTest {
    @Test fun invalidRequestsCannotExtendTheDefaultSession() {
        for (minutes in listOf(-1, 0, 5, 60, Int.MAX_VALUE)) {
            assertEquals(10, SessionDeadline(100, minutes).minutes)
        }
    }
    @Test fun expirationIncludesTimeSpentAsleepAndDoesNotResetOnChecks() {
        val deadline = SessionDeadline(1_000, 10)
        assertFalse(deadline.expired(600_999))
        assertTrue(deadline.expired(601_000))
        assertTrue(deadline.expired(2_000_000))
    }
    @Test fun extendedSessionRequiresTheExplicitThirtyMinuteChoice() {
        val deadline = SessionDeadline(1_000, 30)
        assertFalse(deadline.expired(1_800_999))
        assertTrue(deadline.expired(1_801_000))
    }
}
