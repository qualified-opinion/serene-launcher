package app.serene.launcher

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.serene.launcher.data.InstalledAppsRepository
import app.serene.launcher.data.LauncherPreferencesRepository
import app.serene.launcher.model.AppItem
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The launcher's main flows on a real device or emulator, against the apps actually installed.
 * Android's own Settings app is the test subject, because every Android image has it.
 */
@RunWith(AndroidJUnit4::class)
class MainFlowsTest {

    @get:Rule val compose = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var settingsApp: AppItem
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp(): Unit = runBlocking {
        settingsApp = InstalledAppsRepository(context).loadInstalledApps()
            .firstOrNull { it.packageName == SETTINGS_PACKAGE && it.isPersonalProfile }
            ?: error("The Settings app ($SETTINGS_PACKAGE) isn't installed on this device")

        // Start every test from the same settings: Settings not pinned, hidden or renamed, and no
        // auto-open, which would leave the launcher as soon as the search matched one app.
        LauncherPreferencesRepository(context).apply {
            setPinState(settingsApp.id, pinned = false)
            setHidden(settingsApp.id, hidden = false)
            setCustomLabel(settingsApp.id, null)
            setAutoLaunchSingleMatch(false)
            setOpenKeyboardOnDrawer(false)
        }

        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitForTag(HOME)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun swipingUpOnHomeOpensTheAppList() {
        // Start on the "all apps" hint at the bottom, away from the scrolling pin list.
        compose.onNodeWithTag(OPEN_DRAWER_BUTTON).performTouchInput {
            swipe(start = center, end = Offset(center.x, center.y - 600f), durationMillis = 300)
        }

        waitForTag(DRAWER)
        compose.onNodeWithTag(DRAWER).assertIsDisplayed()
    }

    @Test
    fun typingFiltersTheAppList() {
        openDrawer()
        val rowsBefore = drawerRowCount()

        compose.onNodeWithTag(SEARCH_INPUT).performTextInput(QUERY)
        waitForTag(rowTag(settingsApp))

        compose.waitForIdle()
        val rows = compose.onAllNodes(isDrawerRow)
        val rowsAfter = rows.fetchSemanticsNodes().size
        assertTrue("Searching should leave fewer apps ($rowsAfter of $rowsBefore)", rowsAfter in 1 until rowsBefore)
        for (index in 0 until rowsAfter) {
            rows[index].assertTextContains(QUERY, substring = true, ignoreCase = true)
        }
    }

    @Test
    fun longPressThenPinShowsTheAppOnHome() {
        openDrawer()
        compose.onNodeWithTag(SEARCH_INPUT).performTextInput(QUERY)
        waitForTag(rowTag(settingsApp))

        compose.onNodeWithTag(rowTag(settingsApp)).performTouchInput { longClick() }
        waitForTag(ACTION_SHEET)
        compose.onNodeWithTag(SHEET_PIN_BUTTON).performClick()
        waitForTagGone(ACTION_SHEET)

        compose.onNodeWithTag(CLOSE_DRAWER_BUTTON).performClick()
        waitForTag(HOME)
        waitForTag(pinnedTag(settingsApp))
        compose.onNodeWithTag(pinnedTag(settingsApp)).assertIsDisplayed()
    }

    @Test
    fun backOnHomeStaysOnHome() {
        Espresso.pressBackUnconditionally()
        compose.waitForIdle()

        assertEquals(Lifecycle.State.RESUMED, scenario.state)
        compose.onNodeWithTag(HOME).assertIsDisplayed()
    }

    @Test
    fun backInTheAppListReturnsHome() {
        openDrawer()

        Espresso.pressBackUnconditionally()

        waitForTag(HOME)
        waitForTagGone(DRAWER)
        assertEquals(Lifecycle.State.RESUMED, scenario.state)
    }

    private fun openDrawer() {
        compose.onNodeWithTag(OPEN_DRAWER_BUTTON).performClick()
        waitForTag(DRAWER)
        // Wait for the first scan, so the list has apps in it.
        compose.waitUntil(TIMEOUT_MS) { drawerRowCount() > 0 }
    }

    private fun drawerRowCount() = compose.onAllNodes(isDrawerRow).fetchSemanticsNodes().size

    private fun waitForTag(tag: String) = compose.waitUntil(TIMEOUT_MS) {
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }

    private fun waitForTagGone(tag: String) = compose.waitUntil(TIMEOUT_MS) {
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()
    }

    private companion object {
        const val SETTINGS_PACKAGE = "com.android.settings"
        const val QUERY = "Settings"
        const val TIMEOUT_MS = 10_000L

        const val HOME = "home_screen"
        const val DRAWER = "app_drawer_screen"
        const val OPEN_DRAWER_BUTTON = "open_drawer_button"
        const val CLOSE_DRAWER_BUTTON = "close_drawer_button"
        const val SEARCH_INPUT = "drawer_search_input"
        const val ACTION_SHEET = "app_action_sheet"
        const val SHEET_PIN_BUTTON = "sheet_pin_toggle_button"

        fun rowTag(app: AppItem) = "drawer_app_row_${app.id}"
        fun pinnedTag(app: AppItem) = "pinned_app_row_${app.id}"

        val isDrawerRow = SemanticsMatcher("is an app row in the drawer") { node ->
            SemanticsProperties.TestTag in node.config &&
                node.config[SemanticsProperties.TestTag].startsWith("drawer_app_row_")
        }
    }
}
