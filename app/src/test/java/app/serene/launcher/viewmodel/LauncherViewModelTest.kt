package app.serene.launcher.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewModelScope
import app.serene.launcher.R
import app.serene.launcher.data.AppChange
import app.serene.launcher.data.LauncherPreferencesRepository
import app.serene.launcher.data.ProfilePausedException
import app.serene.launcher.model.ProfileKind
import app.serene.launcher.model.StatusMessage
import app.serene.launcher.model.ThemeMode
import app.serene.launcher.testing.FakeAppsRepository
import app.serene.launcher.testing.InMemoryPreferencesDataStore
import app.serene.launcher.testing.app
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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
    fun `empty scan retries default pins and initialized users are never reseeded`() = runTest(dispatcher) {
        appsRepository.apps = emptyList()
        val viewModel = LauncherViewModel(appsRepository, preferences)
        try {
            runCurrent()
            assertFalse(viewModel.uiState.value.isLoading)
            assertFalse(preferences.preferencesFlow.first().hasInitializedDefaultPins)
            assertTrue(preferences.preferencesFlow.first().pinnedAppIds.isEmpty())

            appsRepository.apps = listOf(maps)
            viewModel.refreshInstalledApps()
            runCurrent()
            assertTrue(preferences.preferencesFlow.first().hasInitializedDefaultPins)
            assertEquals(setOf(maps.id), preferences.preferencesFlow.first().pinnedAppIds)
            assertEquals(listOf(maps.id), viewModel.uiState.value.pinnedApps.map { it.id })

            preferences.setPinState(maps.id, false)
            viewModel.refreshInstalledApps()
            runCurrent()
            assertTrue(preferences.preferencesFlow.first().hasInitializedDefaultPins)
            assertTrue(preferences.preferencesFlow.first().pinnedAppIds.isEmpty())
            assertTrue(viewModel.uiState.value.pinnedApps.isEmpty())
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `failed saves keep refresh and pinning usable and dismiss the menu`() = runTest(dispatcher) {
        var writes = 0
        val failingStore = object : DataStore<Preferences> by InMemoryPreferencesDataStore() {
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                writes++
                throw IOException("write failed")
            }
        }
        preferences = LauncherPreferencesRepository(failingStore)
        val viewModel = LauncherViewModel(appsRepository, preferences)
        try {
            runCurrent()
            assertEquals(1, writes)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(appsRepository.apps, viewModel.uiState.value.allApps)

            viewModel.refreshInstalledApps()
            runCurrent()
            assertEquals(2, writes)
            assertFalse(viewModel.uiState.value.isLoading)

            viewModel.onAppLongPress(camera)
            runCurrent()
            assertEquals(camera, viewModel.uiState.value.selectedAppForMenu)
            viewModel.togglePin(camera)
            runCurrent()
            assertEquals(3, writes)
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.selectedAppForMenu)
            assertNull(viewModel.uiState.value.statusMessage)
            assertTrue(viewModel.uiState.value.pinnedApps.isEmpty())
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `toggling a pin adds and then removes it from Home`() = runTest(dispatcher) {
        val viewModel = loadedViewModel()

        viewModel.togglePin(camera)
        advanceUntilIdle()
        assertEquals(listOf(maps.id, camera.id), viewModel.uiState.value.pinnedApps.map { it.id })

        viewModel.togglePin(camera)
        advanceUntilIdle()
        assertEquals(listOf(maps.id), viewModel.uiState.value.pinnedApps.map { it.id })
    }

    @Test
    fun `starting with legacy pins keeps label order without writing the new key`() = runTest(dispatcher) {
        val store = InMemoryPreferencesDataStore()
        store.edit { it[LauncherPreferencesRepository.KEY_PINNED_APP_IDS] = setOf(maps.id, camera.id) }
        preferences = LauncherPreferencesRepository(store)
        val vm = loadedViewModel()
        try {
            assertEquals(listOf(camera.id, maps.id), vm.uiState.value.pinnedApps.map { it.id })
            assertNull(store.data.first()[LauncherPreferencesRepository.KEY_PINNED_APP_ORDER])
        } finally { vm.viewModelScope.cancel() }
    }

    @Test
    fun `removed events prune only the named package and profile and save failures stay usable`() = runTest(dispatcher) {
        val ids = listOf("pkg/Main", "pkg/Other", "pkg/Main@10", "pkg.extra/Main", "pkg/Main@11")
        ids.forEach { preferences.setPinState(it, true) }
        val viewModel = loadedViewModel()
        try {
            appsRepository.changes.emit(AppChange.Removed("pkg", null))
            runCurrent()
            assertEquals(ids.drop(2), preferences.preferencesFlow.first().pinnedAppOrder)
            appsRepository.changes.emit(AppChange.Removed("pkg", 10))
            runCurrent()
            assertEquals(ids.drop(3), preferences.preferencesFlow.first().pinnedAppOrder)
        } finally { viewModel.viewModelScope.cancel() }
        val store = InMemoryPreferencesDataStore()
        LauncherPreferencesRepository(store).setPinState(maps.id, true)
        preferences = LauncherPreferencesRepository(object : DataStore<Preferences> by store {
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = throw IOException()
        })
        val failing = loadedViewModel()
        try {
            appsRepository.changes.emit(AppChange.Removed(maps.packageName, null))
            runCurrent()
            assertEquals(listOf(maps.id), failing.uiState.value.pinnedApps.map { it.id })
            assertFalse(failing.uiState.value.isLoading)
        } finally { failing.viewModelScope.cancel() }
    }

    @Test
    fun `restored pins survive empty and paused scans and appear in order when apps return`() = runTest(dispatcher) {
        val work = app("Work", "pkg/Main@10").copy(profileSerial = 10, profileKind = ProfileKind.WORK)
        val ordered = listOf(work, maps, camera)
        ordered.forEach { preferences.setPinState(it.id, true) }
        appsRepository.apps = emptyList()
        val viewModel = loadedViewModel()
        try {
            assertTrue(viewModel.uiState.value.pinnedApps.isEmpty())
            for (scan in listOf(listOf(camera, maps), emptyList(), listOf(camera, maps, work))) {
                appsRepository.apps = scan
                appsRepository.changes.emit(AppChange.Refresh)
                runCurrent()
                assertEquals(ordered.map { it.id }, preferences.preferencesFlow.first().pinnedAppOrder)
                assertEquals(ordered.filter { it in scan }.map { it.id }, viewModel.uiState.value.pinnedApps.map { it.id })
            }
            viewModel.startReorderingPins()
            runCurrent()
            assertTrue(viewModel.uiState.value.isReorderingPins)
            viewModel.finishReorderingPins()
            runCurrent()
            assertFalse(viewModel.uiState.value.isReorderingPins)
        } finally { viewModel.viewModelScope.cancel() }
    }

    @Test
    fun `launching a pinned app exits reorder mode`() = runTest(dispatcher) {
        val viewModel = loadedViewModel()
        try {
            viewModel.startReorderingPins()
            runCurrent()
            assertTrue(viewModel.uiState.value.isReorderingPins)

            viewModel.launchApp(viewModel.uiState.value.pinnedApps.single())
            runCurrent()

            assertEquals(listOf(maps.id), appsRepository.launched.map { it.id })
            assertFalse(viewModel.uiState.value.isReorderingPins)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `typing filters the drawer`() = runTest(dispatcher) {
        val viewModel = loadedViewModel()
        viewModel.setAutoLaunchSingleMatch(false) // so typing only filters
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

        viewModel.onSearchQueryChanged("m") // Camera and Maps both still match
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
    fun `pasting a query that matches one app does not open it`() = runTest(dispatcher) {
        val viewModel = loadedViewModel()
        viewModel.openDrawer()
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("map")
        advanceUntilIdle()

        assertTrue(appsRepository.launched.isEmpty())
        assertEquals(listOf("Maps"), viewModel.uiState.value.filteredDrawerApps.map { it.label })
    }

    @Test
    fun `a single match that only starts a later word does not open by itself`() = runTest(dispatcher) {
        appsRepository = FakeAppsRepository(listOf(calendar, app("Google Maps")))
        val viewModel = loadedViewModel()
        viewModel.openDrawer()
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("m")
        advanceUntilIdle()

        assertTrue(appsRepository.launched.isEmpty())
        assertTrue(viewModel.uiState.value.isDrawerOpen)
    }

    @Test
    fun `an app that fails to open keeps the drawer open and says so`() = runTest(dispatcher) {
        val viewModel = loadedViewModel()
        viewModel.openDrawer()
        advanceUntilIdle()
        appsRepository.failLaunches = true

        viewModel.launchApp(camera)
        runCurrent()

        assertTrue(viewModel.uiState.value.isDrawerOpen)
        assertEquals(
            StatusMessage(R.string.status_cant_open, listOf("Camera")),
            viewModel.uiState.value.statusMessage
        )
    }

    @Test
    fun `theme choice is saved`() = runTest(dispatcher) {
        val viewModel = loadedViewModel()
        assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.themeMode)

        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun `settings open and close`() = runTest(dispatcher) {
        val viewModel = loadedViewModel()

        viewModel.openSettings()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSettingsOpen)

        viewModel.closeSettings()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSettingsOpen)
    }

    @Test
    fun `drawer options are saved`() = runTest(dispatcher) {
        val viewModel = loadedViewModel()

        viewModel.setShowAppIcons(true)
        viewModel.setAutoLaunchSingleMatch(false)
        viewModel.setOpenKeyboardOnDrawer(true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showAppIcons)
        assertFalse(state.autoLaunchSingleMatch)
        assertTrue(state.openKeyboardOnDrawer)
    }

    @Test
    fun `hiding an app removes it from the drawer and Home and unhiding brings both back`() = runTest(dispatcher) {
        val viewModel = loadedViewModel()

        viewModel.hideApp(maps)
        runCurrent()
        var state = viewModel.uiState.value
        assertEquals(StatusMessage(R.string.status_hidden, listOf("Maps")), state.statusMessage)
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(listOf(calendar.id, camera.id), state.filteredDrawerApps.map { it.id })
        assertTrue(state.pinnedApps.isEmpty())
        assertEquals(listOf(maps.id), state.hiddenApps.map { it.id })

        viewModel.unhideApp(state.hiddenApps.single())
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(listOf(maps.id), state.pinnedApps.map { it.id })
        assertTrue(state.hiddenApps.isEmpty())
    }

    @Test
    fun `typing never auto-opens a hidden app`() = runTest(dispatcher) {
        val viewModel = loadedViewModel()
        viewModel.hideApp(maps)
        advanceUntilIdle()

        viewModel.openDrawer()
        viewModel.onSearchQueryChanged("m")
        advanceUntilIdle()

        // "m" still finds Camera, but only as a contains match, which never opens by itself.
        assertTrue(appsRepository.launched.isEmpty())
        assertEquals(listOf(camera.id), viewModel.uiState.value.filteredDrawerApps.map { it.id })
    }

    @Test
    fun `renaming an app shows and sorts the new name and resetting restores it`() = runTest(dispatcher) {
        val viewModel = loadedViewModel()

        viewModel.startRenaming(maps)
        advanceUntilIdle()
        assertEquals(maps.id, viewModel.uiState.value.appBeingRenamed?.id)

        viewModel.finishRenaming("  Atlas\t ")
        advanceUntilIdle()
        var state = viewModel.uiState.value
        assertNull(state.appBeingRenamed)
        assertEquals(listOf("Atlas", "Calendar", "Camera"), state.allApps.map { it.label })
        assertEquals("Maps", state.allApps.first().originalLabel)
        assertEquals("Atlas", state.pinnedApps.single().label)

        viewModel.onSearchQueryChanged("atl")
        advanceUntilIdle()
        assertEquals(listOf(maps.id), viewModel.uiState.value.filteredDrawerApps.map { it.id })
        viewModel.clearSearch()

        viewModel.startRenaming(state.allApps.first())
        viewModel.finishRenaming(null)
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(listOf("Calendar", "Camera", "Maps"), state.allApps.map { it.label })
        assertNull(state.allApps.last().originalLabel)
    }

    @Test
    fun `cancelling a rename keeps the name`() = runTest(dispatcher) {
        val viewModel = loadedViewModel()

        viewModel.startRenaming(camera)
        viewModel.cancelRenaming()
        viewModel.finishRenaming("Ignored")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.appBeingRenamed)
        assertEquals(listOf("Calendar", "Camera", "Maps"), viewModel.uiState.value.allApps.map { it.label })
    }

    // The test helper takes the activity from the id, so set it to match the personal copy.
    private val workMaps = app("Maps", "pkg.maps/Main@10")
        .copy(activityName = "Main", profileSerial = 10, profileKind = ProfileKind.WORK)

    @Test
    fun `typing opens the personal copy of an app installed in both profiles`() = runTest(dispatcher) {
        appsRepository = FakeAppsRepository(listOf(calendar, maps, workMaps))
        val viewModel = loadedViewModel()
        viewModel.openDrawer()
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("m")
        advanceUntilIdle()

        assertEquals(listOf(maps.id), appsRepository.launched.map { it.id })
    }

    @Test
    fun `different apps with the same name in two profiles still wait for more typing`() = runTest(dispatcher) {
        val workOther = app("Maps", "other.maps/Main@10").copy(profileSerial = 10, profileKind = ProfileKind.WORK)
        appsRepository = FakeAppsRepository(listOf(calendar, maps, workOther))
        val viewModel = loadedViewModel()
        viewModel.openDrawer()
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("m")
        advanceUntilIdle()

        assertTrue(appsRepository.launched.isEmpty())
    }

    @Test
    fun `opening an app in a paused work profile says so`() = runTest(dispatcher) {
        appsRepository = FakeAppsRepository(listOf(calendar, maps, workMaps))
        val viewModel = loadedViewModel()
        try {
            appsRepository.launchError = ProfilePausedException(resumeRequested = true)
            viewModel.launchApp(workMaps)
            runCurrent()
            assertEquals(StatusMessage(R.string.status_resuming_profile, listOf("Maps")), viewModel.uiState.value.statusMessage)

            appsRepository.launchError = ProfilePausedException(resumeRequested = false)
            viewModel.launchApp(workMaps)
            runCurrent()
            assertEquals(StatusMessage(R.string.status_profile_paused, listOf("Maps")), viewModel.uiState.value.statusMessage)
        } finally { viewModel.viewModelScope.cancel() }
    }

    @Test
    fun `work pins from an old phone show up on Home after the first scan`() = runTest(dispatcher) {
        preferences.setPinState("pkg.maps/Main@5", pinned = true)
        appsRepository = FakeAppsRepository(listOf(calendar, maps, workMaps))
        appsRepository.liveSerials = setOf(0L, 10L)

        val state = loadedViewModel().uiState.value

        assertEquals(listOf(workMaps.id), state.pinnedApps.map { it.id })
    }
}
