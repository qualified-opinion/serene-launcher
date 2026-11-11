package app.serene.launcher.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StatusPillTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `the message stays in the pill while it fades out`() {
        var message by mutableStateOf<String?>("Pinned Maps to Home")
        compose.setContent { TransientStatusPill(message = message) }
        compose.onNodeWithText("Pinned Maps to Home").assertExists()

        compose.mainClock.autoAdvance = false
        message = null
        // Part way through the fade-out, the pill still says what it said.
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(50)
        compose.onNodeWithTag("status_feedback_pill").assertExists()
        compose.onNodeWithText("Pinned Maps to Home").assertExists()

        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithTag("status_feedback_pill").assertDoesNotExist()
    }

    @Test
    fun `a new message replaces the old one`() {
        var message by mutableStateOf<String?>("Opening Maps")
        compose.setContent { TransientStatusPill(message = message) }

        message = "Opening Mail"

        compose.onNodeWithText("Opening Mail").assertExists()
        compose.onNodeWithText("Opening Maps").assertDoesNotExist()
    }
}
