package app.serene.launcher.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import app.serene.launcher.model.ThemeMode
import app.serene.launcher.testing.app
import app.serene.launcher.ui.components.RenameAppDialog
import app.serene.launcher.ui.screens.SettingsScreen
import app.serene.launcher.ui.theme.SereneLauncherTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HideAndRenameScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `Settings lists hidden apps and Unhide reports the app`() {
        val maps = app("Maps")
        val unhidden = mutableListOf<String>()
        compose.setContent {
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
                    hiddenApps = listOf(maps),
                    onUnhideApp = { unhidden += it.id }
                )
            }
        }

        compose.onNodeWithTag("settings_hidden_${maps.id}").assertIsDisplayed()
        compose.onNodeWithContentDescription("Unhide Maps").performClick()
        compose.runOnIdle { assertEquals(listOf(maps.id), unhidden) }
    }

    @Test
    fun `rename dialog saves the typed name and offers reset only for renamed apps`() {
        val renamed = app("Atlas").copy(originalLabel = "Maps")
        val saved = mutableListOf<String>()
        var reset = false
        compose.setContent {
            SereneLauncherTheme {
                RenameAppDialog(app = renamed, onSave = { saved += it }, onReset = { reset = true }, onDismiss = {})
            }
        }

        compose.onNodeWithTag("rename_field").performTextReplacement("Directions")
        compose.onNodeWithTag("rename_save_button").performClick()
        compose.onNodeWithTag("rename_reset_button").performClick()
        compose.runOnIdle {
            assertEquals(listOf("Directions"), saved)
            assertTrue(reset)
        }
    }
}
