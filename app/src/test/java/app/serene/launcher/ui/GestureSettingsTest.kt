package app.serene.launcher.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import app.serene.launcher.model.SwipeSide
import app.serene.launcher.model.SwipeTarget
import app.serene.launcher.model.ThemeMode
import app.serene.launcher.testing.app
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
class GestureSettingsTest {
    @get:Rule val compose = createComposeRule()

    private fun showSettings(
        isLockServiceOn: Boolean,
        onDoubleTapChange: (Boolean) -> Unit = {},
        onOpenAccessibility: () -> Unit = {}
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
                doubleTapToLock = false,
                onDoubleTapToLockChange = onDoubleTapChange,
                isLockServiceOn = isLockServiceOn,
                onOpenAccessibilitySettings = onOpenAccessibility
            )
        }
    }

    @Test
    fun `turning on double-tap explains the accessibility service before opening its settings`() {
        val changes = mutableListOf<Boolean>()
        var openedAccessibility = false
        showSettings(isLockServiceOn = false, onDoubleTapChange = { changes += it }, onOpenAccessibility = { openedAccessibility = true })

        compose.onNodeWithTag("settings_double_tap_lock").performScrollTo().performClick()
        compose.onNodeWithText("Let Serene lock the screen?").assertIsDisplayed()
        compose.runOnIdle { assertTrue(!openedAccessibility) }

        compose.onNodeWithTag("lock_service_open_settings").performClick()
        compose.runOnIdle {
            assertEquals(listOf(true), changes)
            assertTrue(openedAccessibility)
        }
        compose.onNodeWithText("Let Serene lock the screen?").assertDoesNotExist()
    }

    @Test
    fun `no explanation when the service is already on`() {
        showSettings(isLockServiceOn = true)

        compose.onNodeWithTag("settings_double_tap_lock").performScrollTo().performClick()

        compose.onNodeWithText("Let Serene lock the screen?").assertDoesNotExist()
    }

    @Test
    fun `choosing an app for a swipe, and choosing Nothing to turn it off`() {
        val maps = app("Maps")
        val choices = mutableListOf<Pair<SwipeSide, String?>>()
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
                    swipeRight = SwipeTarget(maps.id, maps),
                    pickableApps = listOf(app("Camera"), maps),
                    onSwipeAppChange = { side, app -> choices += side to app?.id }
                )
            }
        }

        compose.onNodeWithTag("settings_swipe_left").performScrollTo().assertTextContains("Nothing")
        compose.onNodeWithTag("settings_swipe_right").performScrollTo().assertTextContains("Maps")

        compose.onNodeWithTag("settings_swipe_left").performClick()
        compose.onNodeWithTag("swipe_pick_${maps.id}").performClick()
        compose.onNodeWithTag("settings_swipe_right").performScrollTo().performClick()
        compose.onNodeWithTag("swipe_pick_none").performClick()

        compose.runOnIdle {
            assertEquals(listOf(SwipeSide.LEFT to maps.id, SwipeSide.RIGHT to null), choices)
        }
    }
}
