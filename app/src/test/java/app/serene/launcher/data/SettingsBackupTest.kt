package app.serene.launcher.data

import app.serene.launcher.model.SwipeSide
import app.serene.launcher.model.ThemeMode
import app.serene.launcher.testing.InMemoryPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsBackupTest {

    private suspend fun configuredRepository(): LauncherPreferencesRepository =
        LauncherPreferencesRepository(InMemoryPreferencesDataStore()).apply {
            setPinState("pkg.maps/Main", true)
            setPinState("pkg.mail/Main@10", true)
            setPinState("pkg.notinstalled/Main", true)
            setHidden("pkg.games/Main", true)
            setCustomLabel("pkg.maps/Main", "Atlas")
            setThemeMode(ThemeMode.DARK)
            setShowAppIcons(true)
            setAutoLaunchSingleMatch(false)
            setOpenKeyboardOnDrawer(true)
            setSwipeDownForNotifications(false)
            setDoubleTapToLock(true)
            setSwipeApp(SwipeSide.LEFT, "pkg.camera/Main")
            setShowWallpaper(true)
            setUseWallpaperColours(true)
        }

    private fun readOk(text: String): SettingsBackup.Contents {
        val result = SettingsBackup.read(text)
        assertTrue("Expected a backup, got $result", result is SettingsBackup.ReadResult.Ok)
        return (result as SettingsBackup.ReadResult.Ok).contents
    }

    @Test
    fun `export then import on a fresh install brings everything back`() = runTest {
        val original = configuredRepository().preferencesFlow.first()
        val file = SettingsBackup.write(original)

        val fresh = LauncherPreferencesRepository(InMemoryPreferencesDataStore())
        fresh.importBackup(readOk(file))
        val restored = fresh.preferencesFlow.first()

        // Pins keep their order, including the work app and the app that isn't installed.
        assertEquals(listOf("pkg.maps/Main", "pkg.mail/Main@10", "pkg.notinstalled/Main"), restored.pinnedAppOrder)
        assertTrue(restored.hasInitializedDefaultPins)
        assertEquals(original.copy(hasInitializedDefaultPins = true), restored)
    }

    @Test
    fun `importing replaces what the file has and keeps what it doesn't`() = runTest {
        val repo = configuredRepository()
        val file = """{"format": "serene-launcher-settings", "version": 1, "pins": ["pkg.phone/Main"], "theme": "LIGHT", "swipeLeftApp": ""}"""

        repo.importBackup(readOk(file))
        val prefs = repo.preferencesFlow.first()

        assertEquals(listOf("pkg.phone/Main"), prefs.pinnedAppOrder)
        assertEquals(ThemeMode.LIGHT, prefs.themeMode)
        assertNull(prefs.swipeLeftAppId)
        // Not in the file, so unchanged:
        assertEquals(setOf("pkg.games/Main"), prefs.hiddenAppIds)
        assertEquals(mapOf("pkg.maps/Main" to "Atlas"), prefs.customLabels)
        assertTrue(prefs.showWallpaper)
    }

    @Test
    fun `a file that isn't a Serene backup is refused`() {
        for (text in listOf("", "not json at all", "\u0089PNG\r\n\u001a\n", "[1, 2, 3]", """{"pins": ["pkg.maps/Main"]}""",
            """{"format": "something-else", "version": 1}""", """{"format": "serene-launcher-settings"}""")) {
            assertEquals("for: $text", SettingsBackup.ReadResult.NotABackup, SettingsBackup.read(text))
        }
    }

    @Test
    fun `broken fields are skipped and counted, the rest is imported`() {
        val contents = readOk(
            """
            {"format": "serene-launcher-settings", "version": 1,
             "pins": ["pkg.maps/Main", 42, "", "has space/Main", "pkg.maps/Main", "pkg.camera/Main"],
             "hidden": "not a list",
             "names": {"pkg.maps/Main": "Atlas\tTwo", "pkg.mail/Main": 7, "nonsense": "x"},
             "theme": "PURPLE",
             "showAppIcons": "yes",
             "doubleTapToLock": true,
             "swipeRightApp": "not an id"}
            """
        )

        assertEquals(listOf("pkg.maps/Main", "pkg.camera/Main"), contents.pins)
        assertNull(contents.hidden)
        assertEquals(mapOf("pkg.maps/Main" to "Atlas Two"), contents.names)
        assertNull(contents.theme)
        assertNull(contents.showAppIcons)
        assertEquals(true, contents.doubleTapToLock)
        assertNull(contents.swipeRightApp)
        // 3 bad pins, hidden, 2 bad names, theme, showAppIcons, swipeRightApp.
        assertEquals(9, contents.skipped)
    }

    @Test
    fun `a newer version is read for the fields this version knows`() {
        val contents = readOk(
            """{"format": "serene-launcher-settings", "version": 7, "pins": ["pkg.maps/Main"], "futureOption": {"a": 1}}"""
        )

        assertEquals(listOf("pkg.maps/Main"), contents.pins)
        assertEquals(0, contents.skipped)
    }
}
