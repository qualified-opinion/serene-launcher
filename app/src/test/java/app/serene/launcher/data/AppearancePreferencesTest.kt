package app.serene.launcher.data

import androidx.datastore.preferences.core.edit
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_BACKGROUND_COLOUR
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_SHOW_APP_ICONS
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_SHOW_WALLPAPER
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_THEME_MODE
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_WALLPAPER_COLOURS
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_WALLPAPER_DIM
import app.serene.launcher.model.BackgroundColour
import app.serene.launcher.model.ThemeMode
import app.serene.launcher.model.WallpaperDim
import app.serene.launcher.testing.InMemoryPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The wallpaper and background settings are new keys, so settings saved before them must read as the defaults. */
class AppearancePreferencesTest {

    @Test
    fun `settings saved before the wallpaper options read as both off`() = runTest {
        val store = InMemoryPreferencesDataStore()
        store.edit {
            it[KEY_THEME_MODE] = ThemeMode.DARK.name
            it[KEY_SHOW_APP_ICONS] = true
        }

        val prefs = LauncherPreferencesRepository(store).preferencesFlow.first()

        assertFalse(prefs.showWallpaper)
        assertFalse(prefs.useWallpaperColours)
        assertEquals(BackgroundColour.PAPER, prefs.backgroundColour)
        assertEquals(WallpaperDim.MEDIUM, prefs.wallpaperDim)
        assertEquals(ThemeMode.DARK, prefs.themeMode)
        assertTrue(prefs.showAppIcons)
        assertEquals(setOf(KEY_THEME_MODE, KEY_SHOW_APP_ICONS), store.data.first().asMap().keys)
    }

    @Test
    fun `the wallpaper options are saved`() = runTest {
        val store = InMemoryPreferencesDataStore()
        val repo = LauncherPreferencesRepository(store)

        repo.setShowWallpaper(true)
        repo.setUseWallpaperColours(true)
        repo.setShowWallpaper(false)

        val prefs = repo.preferencesFlow.first()
        assertFalse(prefs.showWallpaper)
        assertTrue(prefs.useWallpaperColours)
        assertEquals(false, store.data.first()[KEY_SHOW_WALLPAPER])
        assertEquals(true, store.data.first()[KEY_WALLPAPER_COLOURS])
    }

    @Test
    fun `the background colour and wallpaper dim are saved by name`() = runTest {
        val store = InMemoryPreferencesDataStore()
        val repo = LauncherPreferencesRepository(store)

        repo.setBackgroundColour(BackgroundColour.MIST)
        repo.setWallpaperDim(WallpaperDim.LIGHT)

        val prefs = repo.preferencesFlow.first()
        assertEquals(BackgroundColour.MIST, prefs.backgroundColour)
        assertEquals(WallpaperDim.LIGHT, prefs.wallpaperDim)
        assertEquals("MIST", store.data.first()[KEY_BACKGROUND_COLOUR])
        assertEquals("LIGHT", store.data.first()[KEY_WALLPAPER_DIM])
    }

    @Test
    fun `a colour or dim name this version doesn't know reads as the default`() = runTest {
        val store = InMemoryPreferencesDataStore()
        store.edit {
            it[KEY_BACKGROUND_COLOUR] = "TURQUOISE"
            it[KEY_WALLPAPER_DIM] = "NONE"
        }

        val prefs = LauncherPreferencesRepository(store).preferencesFlow.first()

        assertEquals(BackgroundColour.PAPER, prefs.backgroundColour)
        assertEquals(WallpaperDim.DEFAULT, prefs.wallpaperDim)
    }
}
