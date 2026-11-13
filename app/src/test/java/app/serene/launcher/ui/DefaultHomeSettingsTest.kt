package app.serene.launcher.ui

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
class DefaultHomeSettingsTest {
    @get:Rule val compose = createComposeRule()

    private fun showSettings(isDefaultHome: Boolean, onMakeDefaultHome: () -> Unit = {}) = compose.setContent {
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
                isDefaultHome = isDefaultHome,
                onMakeDefaultHome = onMakeDefaultHome
            )
        }
    }

    @Test
    fun `the home screen row shows when Serene is not the default`() {
        var taps = 0
        showSettings(isDefaultHome = false, onMakeDefaultHome = { taps++ })

        compose.onNodeWithTag("settings_make_default_home")
            .performScrollTo()
            .assertTextContains("Make Serene my home screen")
            .performClick()

        compose.runOnIdle { assertEquals(1, taps) }
    }

    @Test
    fun `the home screen row hides when Serene is the default`() {
        showSettings(isDefaultHome = true)

        compose.onNodeWithTag("settings_make_default_home").assertDoesNotExist()
        compose.onNodeWithText("Make Serene my home screen").assertDoesNotExist()
    }
}
