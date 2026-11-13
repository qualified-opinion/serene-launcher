package app.serene.launcher.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.serene.launcher.model.PendingSettingsImport
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
class ImportSettingsTest {
    @get:Rule val compose = createComposeRule()

    private fun showSettings(
        pending: PendingSettingsImport?,
        onConfirm: () -> Unit = {},
        onCancel: () -> Unit = {}
    ) = compose.setContent {
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
                pendingSettingsImport = pending,
                onConfirmImport = onConfirm,
                onCancelImport = onCancel
            )
        }
    }

    @Test
    fun `no import dialog until a backup is waiting`() {
        showSettings(pending = null)

        compose.onNodeWithText("Replace your settings?").assertDoesNotExist()
    }

    @Test
    fun `replace confirms the import and cancel does not`() {
        var confirmed = 0
        var cancelled = 0
        showSettings(
            pending = PendingSettingsImport(pinCount = 2, hiddenCount = 1, nameCount = 3),
            onConfirm = { confirmed++ },
            onCancel = { cancelled++ }
        )

        compose.onNodeWithText("Replace your settings?").assertIsDisplayed()
        compose.onNodeWithText(
            "This backup will replace your current pins, hidden apps, custom names and options. " +
                "If it lists no pins, your Home pins stay as they are."
        ).assertIsDisplayed()
        compose.onNodeWithText("2 pins, 1 hidden apps, 3 custom names.").assertIsDisplayed()

        compose.onNodeWithTag("settings_import_cancel").performClick()
        compose.runOnIdle {
            assertEquals(1, cancelled)
            assertEquals(0, confirmed)
        }

        compose.onNodeWithTag("settings_import_replace").performClick()
        compose.runOnIdle { assertEquals(1, confirmed) }
    }
}
