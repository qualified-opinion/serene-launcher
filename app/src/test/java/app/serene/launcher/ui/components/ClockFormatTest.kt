package app.serene.launcher.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ClockFormatTest {

    // Sunday 18 October 2026, 14:05 UTC
    private val afternoon = 1_792_332_300_000L
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun `24-hour setting shows the hour past 12`() {
        assertEquals("14:05", formatClock(afternoon, is24Hour = true, Locale.US, utc).time)
    }

    @Test
    fun `12-hour setting shows the afternoon hour with its marker`() {
        val time = formatClock(afternoon, is24Hour = false, Locale.US, utc).time

        assertTrue(time, time.startsWith("2:05"))
        assertTrue(time, time.endsWith("PM"))
    }

    @Test
    fun `date is short weekday, month and day in capitals`() {
        assertEquals("SUN, OCT 18", formatClock(afternoon, is24Hour = true, Locale.US, utc).date)
    }

    @Test
    fun `date follows the language`() {
        val german = formatClock(afternoon, is24Hour = true, Locale.GERMANY, utc).date

        assertNotEquals("SUN, OCT 18", german)
        assertTrue(german, german.contains("18"))
    }

    @Test
    fun `time follows the time zone`() {
        val tokyo = TimeZone.getTimeZone("Asia/Tokyo")

        assertEquals("23:05", formatClock(afternoon, is24Hour = true, Locale.US, tokyo).time)
    }
}
