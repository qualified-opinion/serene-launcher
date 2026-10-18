package app.serene.launcher.viewmodel

import app.serene.launcher.data.LauncherPreferencesRepository
import app.serene.launcher.testing.FakeAppsRepository
import app.serene.launcher.testing.InMemoryPreferencesDataStore
import app.serene.launcher.testing.app
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val calendar = app("Calendar")
    private val camera = app("Camera")
    private val maps = app("Maps")
    private lateinit var appsRepository: FakeAppsRepository
    private lateinit var preferences: LauncherPreferencesRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        appsRepository = FakeAppsRepository(listOf(calendar, camera, maps), initialPins = setOf(maps.id))
        preferences = LauncherPreferencesRepository(InMemoryPreferencesDataStore())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.loadedViewModel(): LauncherViewModel =
        LauncherViewModel(appsRepository, preferences).also { advanceUntilIdle() }

    @Test
    fun `first load lists every app and applies the default pins`() = runTest(dispatcher) {
        val state = loadedViewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals(listOf(calendar.id, camera.id, maps.id), state.allApps.map { it.id })
        assertEquals(listOf(maps.id), state.pinnedApps.map { it.id })
    }

    @Test
    fun `toggling a pin adds and then removes it from Home`() = runTest(dispatcher) {
        val viewModel = loadedViewModel()

        viewModel.togglePin(camera)
        advanceUntilIdle()
        assertEquals(listOf(camera.id, maps.id), viewModel.uiState.value.pinnedApps.map { it.id })

        viewModel.togglePin(camera)
        advanceUntilIdle()
        assertEquals(listOf(maps.id), viewModel.uiState.value.pinnedApps.map { it.id })
    }

    @Test
    fun `typing filters the drawer`() = runTest(dispatcher) {
        val viewModel = loadedViewModel()
        viewModel.toggleAutoLaunchSingleMatch() // turn auto-open off so typing only filters
        viewModel.openDrawer()
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("ca")
        advanceUntilIdle()

        assertEquals(listOf("Calendar", "Camera"), viewModel.uiState.value.filteredDrawerApps.map { it.label })
        assertTrue(appsRepository.launched.isEmpty())
    }

    @Test
    fun `auto-open launches the only match and returns to Home`() = runTest(dispatcher) {
        val viewModel = loadedViewModel()
        viewModel.openDrawer()
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("ma")
        advanceUntilIdle()

        assertEquals(listOf(maps.id), appsRepository.launched.map { it.id })
        assertFalse(viewModel.uiState.value.isDrawerOpen)
        assertEquals("", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun `auto-open waits while more than one app matches`() = runTest(dispatcher) {
        val viewModel = loadedViewModel()
        viewModel.openDrawer()
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("c")
        advanceUntilIdle()

        assertTrue(appsRepository.launched.isEmpty())
        assertTrue(viewModel.uiState.value.isDrawerOpen)
    }

    @Test
    fun `theme choice is saved`() = runTest(dispatcher) {
        val viewModel = loadedViewModel()

        viewModel.toggleTheme()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isDarkTheme)
    }
}
