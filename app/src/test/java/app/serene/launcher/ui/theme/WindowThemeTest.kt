package app.serene.launcher.ui.theme

import android.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import app.serene.launcher.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The activity theme must stop the phone from darkening Serene, because Serene draws its own
 * light and dark looks. Otherwise the Light theme comes out black on a phone in dark mode.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WindowThemeTest {

    private fun attribute(attr: Int, default: Boolean): Boolean {
        val themed = ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.Theme_SereneLauncher)
        val values = themed.obtainStyledAttributes(intArrayOf(attr))
        return values.getBoolean(0, default).also { values.recycle() }
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `force dark is off on a light phone`() {
        assertFalse(attribute(android.R.attr.forceDarkAllowed, default = true))
    }

    @Test
    @Config(qualifiers = "night")
    fun `force dark is off on a dark phone`() {
        assertFalse(attribute(android.R.attr.forceDarkAllowed, default = true))
    }

    @Test
    @Config(qualifiers = "night")
    fun `on a dark phone the theme is not marked light, so expanded dark theme leaves it alone`() {
        assertFalse(attribute(android.R.attr.isLightTheme, default = true))
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `on a light phone the theme stays marked light`() {
        assertTrue(attribute(android.R.attr.isLightTheme, default = false))
    }
}
