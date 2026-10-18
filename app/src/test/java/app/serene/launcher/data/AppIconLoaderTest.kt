package app.serene.launcher.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.serene.launcher.testing.app
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppIconLoaderTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `an app that isn't installed has no icon and doesn't crash`() = runBlocking {
        val loader = AppIconLoader.get(context)

        assertNull(loader.load(app("Missing", id = "not.installed/Main"), sizePx = 48))
    }

    @Test
    fun `the loader is shared across the process`() {
        assertSame(AppIconLoader.get(context), AppIconLoader.get(context))
    }
}
