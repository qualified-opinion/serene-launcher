package app.serene.launcher.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.serene.launcher.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StringsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `status messages fill in the app name`() {
        assertEquals("Can't open Camera", context.getString(R.string.status_cant_open, "Camera"))
        assertEquals("Pinned Maps to Home", context.getString(R.string.status_pinned, "Maps"))
    }

    @Test
    fun `app count uses the plural resource`() {
        assertEquals("All apps (1)", context.resources.getQuantityString(R.plurals.all_apps_count, 1, 1))
        assertEquals("All apps (42)", context.resources.getQuantityString(R.plurals.all_apps_count, 42, 42))
    }
}
