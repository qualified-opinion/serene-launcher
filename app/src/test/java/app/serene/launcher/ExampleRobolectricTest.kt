package app.serene.launcher

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.serene.launcher.data.InstalledAppsRepository
import app.serene.launcher.data.LauncherPreferencesRepository
import app.serene.launcher.testing.app
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Serene Launcher", appName)
    }

    @Test
    fun `installed apps repository loads without crashing`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val apps = InstalledAppsRepository(context).loadInstalledApps()

        assertTrue("Launcher should never list itself", apps.none { it.packageName == context.packageName })
    }

    @Test
    fun `initial pins prefer exact matches and skip work apps`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = InstalledAppsRepository(context)
        val workPhone = app("Phone", id = "work.phone/Main@10").copy(profileSerial = 10, isWorkProfile = true)
        val apps = listOf(
            app("Phone Backup"),
            app("Phone"),
            workPhone,
            app("Camera"),
            app("Weather")
        )

        val pins = repo.selectInitialPinnedIds(apps)

        assertTrue(pins.contains("pkg.phone/Main"))
        assertFalse(pins.contains("pkg.phone backup/Main"))
        assertFalse(pins.contains(workPhone.id))
        assertTrue(pins.contains("pkg.camera/Main"))
        assertFalse(pins.contains("pkg.weather/Main"))
    }

    @Test
    fun `initial pins fall back to first personal apps when nothing matches`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val apps = listOf("Alpha", "Bravo", "Charlie", "Delta", "Echo").map { app(it) }

        val pins = InstalledAppsRepository(context).selectInitialPinnedIds(apps)

        assertEquals(apps.take(4).map { it.id }.toSet(), pins)
    }

    @Test
    fun `preferences repository persists pinned apps and theme toggles offline`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val prefsRepo = LauncherPreferencesRepository(context)

        prefsRepo.setDarkTheme(true)
        prefsRepo.setShowAppIcons(true)
        prefsRepo.setPinState("system/calculator", true)

        val prefs = prefsRepo.preferencesFlow.first()
        assertTrue(prefs.isDarkTheme)
        assertTrue(prefs.showAppIcons)
        assertTrue(prefs.pinnedAppIds.contains("system/calculator"))

        prefsRepo.setPinState("system/calculator", false)
        val updated = prefsRepo.preferencesFlow.first()
        assertFalse(updated.pinnedAppIds.contains("system/calculator"))
    }
}
