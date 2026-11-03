package app.serene.launcher.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import app.serene.launcher.model.ThemeMode
import app.serene.launcher.ui.screens.SettingsScreen
import app.serene.launcher.ui.theme.SereneLauncherTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppearanceSettingsTest {
    @get:Rule val compose = createComposeRule()

    private val wallpaperChanges = mutableListOf<Boolean>()
    private val colourChanges = mutableListOf<Boolean>()

    private fun showSettings(supportsWallpaperColours: Boolean) = compose.setContent {
        SereneLauncherTheme {
            SettingsScreen(
                themeMode = ThemeMode.SYSTEM,
                showAppIcons = false,
                autoLaunchSingleMatch = true,
                openKeyboardOnDrawer = false,
                onThemeModeChange = {},
                onShowAppIconsChange = {},
                onAutoLaunchSingleMatchChange = {},
                onOpenKeyboardOnDrawerChange = {},
                onClose = {},
                onShowWallpaperChange = { wallpaperChanges += it },
                onUseWallpaperColoursChange = { colourChanges += it },
                supportsWallpaperColours = supportsWallpaperColours
            )
        }
    }

    @Test
    fun `both wallpaper switches turn on from Settings`() {
        showSettings(supportsWallpaperColours = true)

        compose.onNodeWithTag("settings_show_wallpaper").performScrollTo().performClick()
        compose.onNodeWithTag("settings_wallpaper_colours").performScrollTo().performClick()

        compose.runOnIdle {
            assertEquals(listOf(true), wallpaperChanges)
            assertEquals(listOf(true), colourChanges)
        }
    }

    @Test
    fun `wallpaper colours are hidden before Android 12`() {
        showSettings(supportsWallpaperColours = false)

        compose.onNodeWithTag("settings_show_wallpaper").assertExists()
        compose.onNodeWithTag("settings_wallpaper_colours").assertDoesNotExist()
    }
}
