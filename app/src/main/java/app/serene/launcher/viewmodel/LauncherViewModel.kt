package app.serene.launcher.viewmodel

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.serene.launcher.R
import app.serene.launcher.data.AppsRepository
import app.serene.launcher.data.InstalledAppsRepository
import app.serene.launcher.data.LauncherPreferencesRepository
import app.serene.launcher.data.LauncherUserPreferences
import app.serene.launcher.domain.AppSearch
import app.serene.launcher.model.AppItem
import app.serene.launcher.model.LauncherUiState
import app.serene.launcher.model.StatusMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class LauncherViewModel(
    private val installedAppsRepository: AppsRepository,
    private val preferencesRepository: LauncherPreferencesRepository
) : ViewModel() {

    private val _rawInstalledApps = MutableStateFlow<List<AppItem>>(emptyList())
    private val _isLoadingApps = MutableStateFlow(true)
    private val _searchQuery = MutableStateFlow("")
    private val _isDrawerOpen = MutableStateFlow(false)
    private val _selectedAppForMenu = MutableStateFlow<AppItem?>(null)
    private val _statusMessage = MutableStateFlow<StatusMessage?>(null)

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private var statusClearJob: Job? = null
    private var refreshJob: Job? = null

    init {
        observeState()
        refreshInstalledApps()
        viewModelScope.launch {
            installedAppsRepository.appChanges().collect { refreshInstalledApps() }
        }
    }

    private fun observeState() {
        viewModelScope.launch {
            combine(
                _rawInstalledApps,
                _isLoadingApps,
                preferencesRepository.preferencesFlow,
                _searchQuery,
                _isDrawerOpen
            ) { rawApps, isLoading, prefs, query, drawerOpen ->
                buildUiState(
                    rawApps = rawApps,
                    isLoading = isLoading,
                    prefs = prefs,
                    query = query,
                    isDrawerOpen = drawerOpen,
                    selectedApp = _selectedAppForMenu.value,
                    statusMessage = _statusMessage.value
                )
            }.combine(
                combine(_selectedAppForMenu, _statusMessage) { selected, status -> selected to status }
            ) { baseState, (selected, status) ->
                val updatedSelected = selected?.let { sel ->
                    baseState.allApps.find { it.id == sel.id } ?: sel
                }
                baseState.copy(
                    selectedAppForMenu = updatedSelected,
                    statusMessage = status
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun buildUiState(
        rawApps: List<AppItem>,
        isLoading: Boolean,
        prefs: LauncherUserPreferences,
        query: String,
        isDrawerOpen: Boolean,
        selectedApp: AppItem?,
        statusMessage: StatusMessage?
    ): LauncherUiState {
        val allAppsWithPin = rawApps.map { app ->
            app.copy(isPinned = prefs.pinnedAppIds.contains(app.id))
        }

        val pinnedApps = allAppsWithPin.filter { it.isPinned }
        val filteredDrawerApps = AppSearch.filter(allAppsWithPin, query)
        val alphabetMap = AppSearch.alphabetIndex(filteredDrawerApps)

        return LauncherUiState(
            isLoading = isLoading,
            pinnedApps = pinnedApps,
            allApps = allAppsWithPin,
            filteredDrawerApps = filteredDrawerApps,
            alphabetIndexMap = alphabetMap,
            availableLetters = AppSearch.railLetters,
            searchQuery = query,
            isDrawerOpen = isDrawerOpen,
            isDarkTheme = prefs.isDarkTheme,
            showAppIcons = prefs.showAppIcons,
            autoLaunchSingleMatch = prefs.autoLaunchSingleMatch,
            selectedAppForMenu = selectedApp,
            statusMessage = statusMessage
        )
    }

    fun refreshInstalledApps() {
        // Only the newest refresh may publish its list, so a slow older one can't overwrite it.
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _isLoadingApps.value = true
            val loaded = installedAppsRepository.loadInstalledApps()
            _rawInstalledApps.value = loaded
            val initialIds = installedAppsRepository.selectInitialPinnedIds(loaded)
            preferencesRepository.initializeDefaultPinsIfNeeded(initialIds)
            _isLoadingApps.value = false
        }
    }

    fun openDrawer() {
        _searchQuery.value = ""
        _isDrawerOpen.value = true
    }

    fun closeDrawer() {
        _searchQuery.value = ""
        _isDrawerOpen.value = false
    }

    fun onSearchQueryChanged(newQuery: String) {
        val previousQuery = _searchQuery.value.trim()
        _searchQuery.value = newQuery

        val currentState = _uiState.value
        val trimmed = newQuery.trim()
        // One more character than before: typed, not pasted or autocompleted.
        val typedOneCharacter = trimmed.length == previousQuery.length + 1 && trimmed.startsWith(previousQuery)

        if (currentState.autoLaunchSingleMatch && typedOneCharacter) {
            val matches = AppSearch.filter(currentState.allApps, trimmed)
            val matchedApp = matches.singleOrNull()
            // Only a label that starts with the query opens by itself, never a word-start or initials match.
            if (matchedApp != null && AppSearch.isPrefixMatch(matchedApp.label, trimmed)) {
                installedAppsRepository.launchApp(matchedApp)
                    .onSuccess {
                        _searchQuery.value = ""
                        _isDrawerOpen.value = false
                        showTransientMessage(StatusMessage(R.string.status_auto_opened, listOf(matchedApp.label)))
                    }
                    .onFailure { showTransientMessage(StatusMessage(R.string.status_cant_open, listOf(matchedApp.label))) }
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun launchApp(app: AppItem) {
        _selectedAppForMenu.value = null
        installedAppsRepository.launchApp(app)
            .onSuccess {
                if (_isDrawerOpen.value) {
                    _searchQuery.value = ""
                    _isDrawerOpen.value = false
                }
                showTransientMessage(StatusMessage(R.string.status_opening, listOf(app.label)))
            }
            .onFailure { showTransientMessage(StatusMessage(R.string.status_cant_open, listOf(app.label))) }
    }

    fun onAppLongPress(app: AppItem) {
        _selectedAppForMenu.value = app
    }

    fun dismissAppMenu() {
        _selectedAppForMenu.value = null
    }

    fun togglePin(app: AppItem) {
        viewModelScope.launch {
            val isNowPinned = preferencesRepository.togglePinApp(app.id)
            _selectedAppForMenu.value = null
            val messageRes = if (isNowPinned) R.string.status_pinned else R.string.status_unpinned
            showTransientMessage(StatusMessage(messageRes, listOf(app.label)))
        }
    }

    /** Called when a system screen opened from Home (alarms, calendar) isn't available on this phone. */
    fun onSystemScreenUnavailable(@StringRes messageRes: Int) {
        showTransientMessage(StatusMessage(messageRes))
    }

    fun openAppInfo(app: AppItem) {
        _selectedAppForMenu.value = null
        installedAppsRepository.openAppInfo(app)
            .onFailure { showTransientMessage(StatusMessage(R.string.status_cant_show_app_info, listOf(app.label))) }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            val nextDark = !_uiState.value.isDarkTheme
            preferencesRepository.setDarkTheme(nextDark)
            showTransientMessage(StatusMessage(if (nextDark) R.string.status_dark_theme else R.string.status_light_theme))
        }
    }

    fun toggleShowAppIcons() {
        viewModelScope.launch {
            val nextIcons = !_uiState.value.showAppIcons
            preferencesRepository.setShowAppIcons(nextIcons)
            showTransientMessage(StatusMessage(if (nextIcons) R.string.status_icons_on else R.string.status_icons_off))
        }
    }

    fun toggleAutoLaunchSingleMatch() {
        viewModelScope.launch {
            val nextAuto = !_uiState.value.autoLaunchSingleMatch
            preferencesRepository.setAutoLaunchSingleMatch(nextAuto)
            showTransientMessage(
                StatusMessage(if (nextAuto) R.string.status_auto_open_on else R.string.status_auto_open_off)
            )
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private fun showTransientMessage(message: StatusMessage) {
        statusClearJob?.cancel()
        _statusMessage.value = message
        statusClearJob = viewModelScope.launch {
            delay(2200L)
            _statusMessage.value = null
        }
    }

    class Factory(
        private val application: Application,
        private val installedAppsRepository: AppsRepository = InstalledAppsRepository(application),
        private val preferencesRepository: LauncherPreferencesRepository = LauncherPreferencesRepository(application)
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LauncherViewModel(
                installedAppsRepository = installedAppsRepository,
                preferencesRepository = preferencesRepository
            ) as T
        }
    }
}
