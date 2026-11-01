package app.serene.launcher.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.serene.launcher.model.ProfileKind
import app.serene.launcher.testing.app
import app.serene.launcher.testing.inProfile
import app.serene.launcher.ui.components.DrawerAppRow
import app.serene.launcher.ui.theme.SereneLauncherTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProfileLabelTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `apps in other profiles are tagged with their profile and paused state`() {
        val apps = listOf(
            app("Camera"),
            inProfile(app("Mail"), serial = 10).copy(isProfilePaused = true),
            inProfile(app("Notes"), serial = 20, kind = ProfileKind.PRIVATE),
            inProfile(app("Chat"), serial = 30, kind = ProfileKind.CLONE)
        )
        compose.setContent {
            SereneLauncherTheme {
                Column {
                    apps.forEach { DrawerAppRow(app = it, showIcon = false, onClick = {}, onLongClick = {}, onQuickTogglePin = {}) }
                }
            }
        }

        compose.onNodeWithText("Work, paused").assertIsDisplayed()
        compose.onNodeWithText("Private").assertIsDisplayed()
        compose.onNodeWithText("Clone").assertIsDisplayed()
        compose.onNodeWithText("Work").assertDoesNotExist()
    }
}
