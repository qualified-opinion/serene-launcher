package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.InstalledAppsRepository
import com.example.data.LauncherPreferencesRepository
import com.example.viewmodel.LauncherViewModel
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
    fun `installed apps repository loads sorted apps and initial pinned selection`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = InstalledAppsRepository(context)
        val apps = repo.loadInstalledApps()

        assertTrue("Expected offline app list to be non-empty", apps.isNotEmpty())
        val initialPins = repo.selectInitialPinnedIds(apps)
        assertTrue("Expected default initial pins to be populated", initialPins.isNotEmpty())
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
