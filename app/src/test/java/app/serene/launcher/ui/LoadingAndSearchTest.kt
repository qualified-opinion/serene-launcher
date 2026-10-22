package app.serene.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import app.serene.launcher.testing.FakeAppsRepository
import app.serene.launcher.testing.app
import app.serene.launcher.ui.screens.AppDrawerScreen
import app.serene.launcher.ui.screens.HomeScreen
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
class LoadingAndSearchTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `Search ignores empty and whitespace queries`() {
        val apps = FakeAppsRepository(listOf(app("Camera")))
        var query by mutableStateOf("")
        compose.setContent {
            SereneLauncherTheme {
                TestDrawer(apps, query = query, onQueryChange = { query = it })
            }
        }
        val search = compose.onNodeWithTag("drawer_search_input")
        search.performImeAction()
        compose.runOnIdle { assertTrue(apps.launched.isEmpty()) }
        search.performTextReplacement("   ")
        search.performImeAction()
        compose.runOnIdle { assertTrue(apps.launched.isEmpty()) }
        search.performTextReplacement("Camera")
        search.performImeAction()
        compose.runOnIdle { assertEquals(apps.apps, apps.launched) }
    }

    @Test
    fun `Home has no empty card while loading`() {
        var loading by mutableStateOf(true)
        compose.setContent {
            SereneLauncherTheme {
                HomeScreen(
                    pinnedApps = emptyList(), isLoading = loading, totalInstalledCount = 0,
                    showAppIcons = false, onAppClick = {}, onAppLongPress = {}, onOpenDrawer = {},
                    onOpenSettings = {}, onClockClick = {}, onDateClick = {}
                )
            }
        }
        compose.onNodeWithTag("empty_pinned_state").assertDoesNotExist()
        compose.runOnIdle { loading = false }
        compose.onNodeWithTag("empty_pinned_state").assertIsDisplayed()
    }

    @Test
    fun `drawer has no empty results while loading`() {
        var loading by mutableStateOf(true)
        compose.setContent { SereneLauncherTheme { TestDrawer(FakeAppsRepository(emptyList()), loading) } }
        compose.onNodeWithTag("empty_search_state").assertDoesNotExist()
        compose.runOnIdle { loading = false }
        compose.onNodeWithTag("empty_search_state").assertIsDisplayed()
    }

    @Composable
    private fun TestDrawer(
        apps: FakeAppsRepository, loading: Boolean = false,
        query: String = "", onQueryChange: (String) -> Unit = {}
    ) = AppDrawerScreen(
        apps = apps.apps, isLoading = loading, alphabetIndexMap = emptyMap(), availableLetters = emptyList(),
        searchQuery = query, showAppIcons = false, openKeyboardOnOpen = false,
        onSearchQueryChange = onQueryChange, onClearSearch = {}, onAppClick = { apps.launchApp(it) },
        onAppLongPress = {}, onQuickTogglePin = {}, onCloseDrawer = {}
    )
}
