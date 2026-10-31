package app.serene.launcher

import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import app.serene.launcher.data.AppChange
import app.serene.launcher.data.InstalledAppsRepository
import app.serene.launcher.data.LauncherPreferencesRepository
import app.serene.launcher.data.LauncherUserPreferences
import app.serene.launcher.model.ProfileKind
import app.serene.launcher.model.ThemeMode
import app.serene.launcher.testing.app
import app.serene.launcher.testing.temporaryPreferences
import app.serene.launcher.testing.temporaryPreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

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
    fun `pausing or resuming a work profile asks for a refresh`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val change = async(Dispatchers.Unconfined) { InstalledAppsRepository(context).appChanges().first() }

        context.sendBroadcast(Intent(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE))
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(AppChange.Refresh, withTimeout(5_000) { change.await() })
    }

    @Test
    fun `initial pins prefer exact matches and skip work apps`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = InstalledAppsRepository(context)
        val workPhone = app("Phone", id = "work.phone/Main@10").copy(profileSerial = 10, profileKind = ProfileKind.WORK)
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
    fun `preferences repository persists pinned apps and theme toggles offline`() = runTest {
        val prefsRepo = temporaryPreferences(temporaryFolder.newFile("settings.preferences_pb"))

        prefsRepo.setThemeMode(ThemeMode.DARK)
        prefsRepo.setShowAppIcons(true)
        prefsRepo.setPinState("system/calculator", true)

        val prefs = prefsRepo.preferencesFlow.first()
        assertEquals(ThemeMode.DARK, prefs.themeMode)
        assertTrue(prefs.showAppIcons)
        assertTrue(prefs.pinnedAppIds.contains("system/calculator"))

        prefsRepo.setPinState("system/calculator", false)
        val updated = prefsRepo.preferencesFlow.first()
        assertFalse(updated.pinnedAppIds.contains("system/calculator"))
    }

    @Test
    fun `damaged settings load defaults and the next save succeeds`() = runTest {
        val file = temporaryFolder.newFile("damaged.preferences_pb").apply { writeText("broken settings") }
        val storeJob = Job(backgroundScope.coroutineContext[Job])
        val scope = CoroutineScope(backgroundScope.coroutineContext + storeJob)
        val repository = LauncherPreferencesRepository(temporaryPreferencesStore(file, scope))

        try {
            assertEquals(LauncherUserPreferences(), repository.preferencesFlow.first())
            repository.setThemeMode(ThemeMode.DARK)
            repository.setPinState("system/calculator", true)
        } finally {
            storeJob.cancelAndJoin()
        }
        // Reopen the same file so these assertions verify disk contents, not cached preferences.
        val saved = temporaryPreferences(file).preferencesFlow.first()
        assertEquals(ThemeMode.DARK, saved.themeMode)
        assertEquals(setOf("system/calculator"), saved.pinnedAppIds)
    }

    @Test
    fun `preferences keep listening after a transient read failure`() = runTest {
        val store = temporaryPreferencesStore(temporaryFolder.newFile("settings.preferences_pb"))
        LauncherPreferencesRepository(store).setThemeMode(ThemeMode.DARK)
        var reads = 0
        val failingOnce = object : DataStore<Preferences> by store {
            override val data = flow {
                if (reads++ == 0) throw IOException("read failed")
                emitAll(store.data)
            }
        }
        val values = LauncherPreferencesRepository(failingOnce).preferencesFlow.take(2).toList()

        assertEquals(listOf(ThemeMode.SYSTEM, ThemeMode.DARK), values.map { it.themeMode })
    }
}
