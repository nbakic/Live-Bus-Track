package hr.zet.transit.ui.stop

import org.junit.Assert.assertEquals
import org.junit.Test

class EtaFormatTest {

    private val now = 1_700_000_000L

    @Test
    fun arrivalWithinHalfMinute_showsStize() {
        assertEquals("stiže", formatEta(now + 15, now))
    }

    @Test
    fun arrivalUnderOneMinute_showsLessThanOne() {
        assertEquals("<1 min", formatEta(now + 45, now))
    }

    @Test
    fun arrivalInMinutes_showsMinuteCount() {
        assertEquals("za 5 min", formatEta(now + 5 * 60, now))
    }

    @Test
    fun pastArrival_showsStize() {
        assertEquals("stiže", formatEta(now - 10, now))
    }
}
