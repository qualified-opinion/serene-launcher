package app.serene.launcher.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewModelScope
import app.serene.launcher.R
import app.serene.launcher.data.AppChange
import app.serene.launcher.data.LauncherPreferencesRepository
import app.serene.launcher.data.LockResult
import app.serene.launcher.model.AppShortcut
import app.serene.launcher.model.ProfileKind
import app.serene.launcher.model.ProfileState
import app.serene.launcher.model.StatusMessage
import app.serene.launcher.model.ThemeMode
import app.serene.launcher.testing.FakeAppsRepository
import app.serene.launcher.testing.FakeSystemActions
import app.serene.launcher.testing.InMemoryPreferencesDataStore
import app.serene.launcher.testing.app
import app.serene.launcher.testing.inProfile
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
    private val systemActions = FakeSystemActions()

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
        LauncherViewModel(appsRepository, preferences, systemActions).also { advanceUntilIdle() }

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

    @Test
    fun `a paused work profile keeps its pinned apps and says so when one is tapped`() = runTest(dispatcher) {
        val workMail = inProfile(app("Mail"), serial = 10)
        appsRepository.apps = listOf(calendar, camera, maps, workMail)
        preferences.setPinState(maps.id, true)
        preferences.setPinState(workMail.id, true)
        val viewModel = loadedViewModel()
        try {
            assertEquals(listOf(maps.id, workMail.id), viewModel.uiState.value.pinnedApps.map { it.id })

            appsRepository.apps = listOf(calendar, camera, maps)
            appsRepository.profileStates[10] = ProfileState.PAUSED
            appsRepository.changes.emit(AppChange.Refresh)
            runCurrent()
            var state = viewModel.uiState.value
            val pausedMail = state.pinnedApps.last()
            assertEquals(listOf(maps.id, workMail.id), state.pinnedApps.map { it.id })
            assertTrue(pausedMail.isProfilePaused)
            assertTrue(state.allApps.single { it.id == workMail.id }.isProfilePaused)

            appsRepository.failLaunches = true
            viewModel.launchApp(pausedMail)
            runCurrent()
            assertEquals(
                StatusMessage(R.string.status_cant_open_paused, listOf("Mail")),
                viewModel.uiState.value.statusMessage
            )

            appsRepository.apps = listOf(calendar, camera, maps, workMail)
            appsRepository.profileStates.remove(10)
            appsRepository.changes.emit(AppChange.Refresh)
            runCurrent()
            state = viewModel.uiState.value
            assertFalse(state.pinnedApps.last().isProfilePaused)
            assertEquals(listOf(workMail.id), preferences.preferencesFlow.first().pinnedAppOrder.filter { '@' in it })
        } finally { viewModel.viewModelScope.cancel() }
    }

    @Test
    fun `typing opens the personal copy of an app installed in both profiles`() = runTest(dispatcher) {
        appsRepository.apps = listOf(calendar, camera, maps, inProfile(maps, serial = 10))
        val viewModel = loadedViewModel()
        viewModel.openDrawer()
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("m")
        viewModel.onSearchQueryChanged("ma")
        advanceUntilIdle()

        assertEquals(listOf(maps.id), appsRepository.launched.map { it.id })
        assertFalse(viewModel.uiState.value.isDrawerOpen)
    }

    @Test
    fun `work pins from an old phone move to this phone's work profile`() = runTest(dispatcher) {
        // Settings brought over from a phone whose work profile had serial 10. This phone's is 12.
        val oldWorkMail = "pkg.mail/Main@10"
        preferences.setPinState(oldWorkMail, true)
        preferences.setPinState(camera.id, true)
        val newWorkMail = inProfile(app("Mail"), serial = 12)
        appsRepository.apps = listOf(calendar, camera, maps, newWorkMail)
        val viewModel = loadedViewModel()
        try {
            assertEquals(listOf(newWorkMail.id, camera.id), viewModel.uiState.value.pinnedApps.map { it.id })
            assertEquals(listOf(newWorkMail.id, camera.id), preferences.preferencesFlow.first().pinnedAppOrder)
        } finally { viewModel.viewModelScope.cancel() }
    }

    @Test
    fun `work pins stay put while their profile still exists`() = runTest(dispatcher) {
        val oldWorkMail = "pkg.mail/Main@10"
        preferences.setPinState(oldWorkMail, true)
        appsRepository.otherKnownProfiles += 10L
        appsRepository.apps = listOf(calendar, camera, maps, inProfile(app("Mail"), serial = 12))
        val viewModel = loadedViewModel()
        try {
            assertEquals(listOf(oldWorkMail), preferences.preferencesFlow.first().pinnedAppOrder)
        } finally { viewModel.viewModelScope.cancel() }
    }

    @Test
    fun `long-pressing an app shows its shortcuts and tapping one opens it and closes the menu`() = runTest(dispatcher) {
        val newPhoto = AppShortcut("new_photo", camera.packageName, "New photo", profileSerial = 0)
        appsRepository.shortcuts[camera.id] = listOf(newPhoto)
        val viewModel = loadedViewModel()
        viewModel.openDrawer()
        viewModel.onAppLongPress(camera)
        advanceUntilIdle()
        assertEquals(listOf(newPhoto), viewModel.uiState.value.appShortcuts)

        viewModel.startShortcut(newPhoto)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(listOf(newPhoto), appsRepository.startedShortcuts)
        assertNull(state.selectedAppForMenu)
        assertFalse(state.isDrawerOpen)
        assertEquals(StatusMessage(R.string.status_opening, listOf("New photo")), state.statusMessage)
    }

    @Test
    fun `the menu never shows the shortcuts of the app pressed before`() = runTest(dispatcher) {
        appsRepository.shortcuts[camera.id] = listOf(AppShortcut("new_photo", camera.packageName, "New photo", 0))
        val viewModel = loadedViewModel()
        viewModel.onAppLongPress(camera)
        advanceUntilIdle()
        viewModel.dismissAppMenu()

        viewModel.onAppLongPress(maps)
        advanceUntilIdle()

        assertEquals(maps.id, viewModel.uiState.value.selectedAppForMenu?.id)
        assertTrue(viewModel.uiState.value.appShortcuts.isEmpty())
    }

    @Test
    fun `a shortcut that fails to open keeps the drawer open and says so`() = runTest(dispatcher) {
        val newPhoto = AppShortcut("new_photo", camera.packageName, "New photo", 0)
        appsRepository.shortcuts[camera.id] = listOf(newPhoto)
        val viewModel = loadedViewModel()
        viewModel.openDrawer()
        viewModel.onAppLongPress(camera)
        advanceUntilIdle()
        appsRepository.failLaunches = true

        viewModel.startShortcut(newPhoto)
        runCurrent()

        assertTrue(viewModel.uiState.value.isDrawerOpen)
        assertEquals(StatusMessage(R.string.status_cant_open, listOf("New photo")), viewModel.uiState.value.statusMessage)
    }

    @Test
    fun `swiping down on Home opens notifications unless it is turned off`() = runTest(dispatcher) {
        val viewModel = loadedViewModel()

        viewModel.onHomeSwipeDown()
        assertEquals(1, systemActions.notificationRequests)

        viewModel.setSwipeDownForNotifications(false)
        advanceUntilIdle()
        viewModel.onHomeSwipeDown()
        assertEquals(1, systemActions.notificationRequests)
        assertFalse(viewModel.uiState.value.swipeDownForNotifications)
    }

    @Test
    fun `notifications that won't open point to Accessibility settings`() = runTest(dispatcher) {
        systemActions.notificationsOpen = false
        val viewModel = loadedViewModel()

        viewModel.onHomeSwipeDown()
        runCurrent()

        assertEquals(StatusMessage(R.string.status_cant_open_notifications), viewModel.uiState.value.statusMessage)
    }

    @Test
    fun `double-tap does nothing until it is turned on, then locks the screen`() = runTest(dispatcher) {
        val viewModel = loadedViewModel()
        assertFalse(viewModel.uiState.value.doubleTapToLock)

        viewModel.onHomeDoubleTap()
        assertEquals(0, systemActions.lockRequests)

        viewModel.setDoubleTapToLock(true)
        advanceUntilIdle()
        viewModel.onHomeDoubleTap()
        runCurrent()

        assertEquals(1, systemActions.lockRequests)
        assertNull(viewModel.uiState.value.statusMessage)
    }

    @Test
    fun `double-tap explains what is missing when the screen can't be locked`() = runTest(dispatcher) {
        val viewModel = loadedViewModel()
        viewModel.setDoubleTapToLock(true)
        advanceUntilIdle()

        systemActions.lockResult = LockResult.NEEDS_SERVICE
        viewModel.onHomeDoubleTap()
        runCurrent()
        assertEquals(StatusMessage(R.string.status_lock_needs_service), viewModel.uiState.value.statusMessage)

        systemActions.lockResult = LockResult.NOT_SUPPORTED
        viewModel.onHomeDoubleTap()
        runCurrent()
        assertEquals(StatusMessage(R.string.status_lock_not_supported), viewModel.uiState.value.statusMessage)
    }
}
