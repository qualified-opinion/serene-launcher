package app.serene.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.viewModelScope
import app.serene.launcher.data.LauncherPreferencesRepository
import app.serene.launcher.testing.FakeAppsRepository
import app.serene.launcher.testing.InMemoryPreferencesDataStore
import app.serene.launcher.testing.app
import app.serene.launcher.ui.screens.AppDrawerScreen
import app.serene.launcher.ui.screens.HomeScreen
import app.serene.launcher.ui.theme.SereneLauncherTheme
import app.serene.launcher.viewmodel.LauncherViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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

    @Test
    fun `down on the first pin swaps the first two pins in reorder mode`() {
        val apps = listOf(app("Camera"), app("Maps"))
        val repo = LauncherPreferencesRepository(InMemoryPreferencesDataStore())
        runBlocking { repo.initializeDefaultPinsIfNeeded(apps.map { it.id }.toSet()) }
        val vm = LauncherViewModel(FakeAppsRepository(apps), repo)
        try {
            vm.startReorderingPins()
            compose.setContent {
                val state by vm.uiState.collectAsState()
                SereneLauncherTheme {
                    HomeScreen(
                        pinnedApps = state.pinnedApps, isLoading = state.isLoading, totalInstalledCount = 2,
                        showAppIcons = false, onAppClick = {}, onAppLongPress = {}, onOpenDrawer = {},
                        onOpenSettings = {}, onClockClick = {}, onDateClick = {},
                        isReorderingPins = state.isReorderingPins, onMovePin = vm::movePin
                    )
                }
            }
            compose.onNodeWithContentDescription("Move Camera down").performClick()
            compose.runOnIdle { assertEquals(apps.reversed().map { it.id }, vm.uiState.value.pinnedApps.map { it.id }) }
            assertTrue(compose.onNodeWithTag("pinned_app_row_${apps[1].id}").fetchSemanticsNode().boundsInRoot.top <
                compose.onNodeWithTag("pinned_app_row_${apps[0].id}").fetchSemanticsNode().boundsInRoot.top)
            assertEquals(apps.reversed().map { it.id }, runBlocking { repo.preferencesFlow.first().pinnedAppOrder })
        } finally { vm.viewModelScope.cancel() }
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
