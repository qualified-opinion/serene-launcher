package app.serene.launcher.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** The clock and date lines shown on Home, already formatted for the phone's settings. */
data class ClockText(val time: String, val date: String)

/**
 * Formats [millis] the way the phone is set up: 12- or 24-hour time per [is24Hour], and a short
 * weekday, month and day in the order and words [locale] uses.
 */
fun formatClock(
    millis: Long,
    is24Hour: Boolean,
    locale: Locale = Locale.getDefault(),
    timeZone: TimeZone = TimeZone.getDefault()
): ClockText {
    val timePattern = DateFormat.getBestDateTimePattern(locale, if (is24Hour) "Hm" else "hm")
    val datePattern = DateFormat.getBestDateTimePattern(locale, "EEEMMMd")
    val date = Date(millis)
    fun format(pattern: String) = SimpleDateFormat(pattern, locale).apply { this.timeZone = timeZone }.format(date)
    return ClockText(time = format(timePattern), date = format(datePattern).uppercase(locale))
}

/**
 * The current time as [ClockText], updated exactly on each minute and whenever the time, time
 * zone, 12/24-hour setting or language changes, and again when Home comes back into view.
 */
@Composable
fun rememberClockText(): ClockText {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                now = System.currentTimeMillis()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED) // also sent when the 12/24-hour setting changes
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_LOCALE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    // Minute ticks pause while the screen is off, so catch up as soon as Home is visible again.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        now = System.currentTimeMillis()
    }

    val is24Hour = DateFormat.is24HourFormat(context)
    val locale = Locale.getDefault()
    val timeZone = TimeZone.getDefault()
    return remember(now, is24Hour, locale, timeZone) { formatClock(now, is24Hour, locale, timeZone) }
}
