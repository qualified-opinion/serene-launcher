package app.serene.launcher.ui

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import app.serene.launcher.model.BackgroundColour
import app.serene.launcher.model.ThemeMode
import app.serene.launcher.model.WallpaperDim
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
    private val backgroundChanges = mutableListOf<BackgroundColour>()
    private val dimChanges = mutableListOf<WallpaperDim>()
    private var wallpaperPickerOpened = 0

    private fun showSettings(supportsWallpaperColours: Boolean, showWallpaper: Boolean = false) = compose.setContent {
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
                supportsWallpaperColours = supportsWallpaperColours,
                showWallpaper = showWallpaper,
                onBackgroundColourChange = { backgroundChanges += it },
                onWallpaperDimChange = { dimChanges += it },
                onChangeWallpaper = { wallpaperPickerOpened++ }
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

    @Test
    fun `tapping a colour swatch chooses that background`() {
        showSettings(supportsWallpaperColours = true)

        compose.onNodeWithTag("settings_background_paper").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("settings_background_sage").performScrollTo().assertIsNotSelected().performClick()

        compose.runOnIdle { assertEquals(listOf(BackgroundColour.SAGE), backgroundChanges) }
    }

    @Test
    fun `the dim choice and change wallpaper only show with the wallpaper on`() {
        showSettings(supportsWallpaperColours = true, showWallpaper = false)

        compose.onNodeWithTag("settings_wallpaper_dim_light").assertDoesNotExist()
        compose.onNodeWithTag("settings_change_wallpaper").assertDoesNotExist()
    }

    @Test
    fun `with the wallpaper on, the dim can be changed and the wallpaper picker opened`() {
        showSettings(supportsWallpaperColours = true, showWallpaper = true)

        compose.onNodeWithTag("settings_wallpaper_dim_medium").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("settings_wallpaper_dim_light").performScrollTo().performClick()
        compose.onNodeWithTag("settings_change_wallpaper").performScrollTo().performClick()

        compose.runOnIdle {
            assertEquals(listOf(WallpaperDim.LIGHT), dimChanges)
            assertEquals(1, wallpaperPickerOpened)
        }
    }
}
