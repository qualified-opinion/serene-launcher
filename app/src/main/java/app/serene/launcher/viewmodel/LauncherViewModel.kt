package app.serene.launcher.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.serene.launcher.data.InstalledAppsRepository
import app.serene.launcher.data.LauncherPreferencesRepository
import app.serene.launcher.data.LauncherUserPreferences
import app.serene.launcher.model.AppItem
import app.serene.launcher.model.LauncherUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Locale

class LauncherViewModel(
    application: Application,
    private val installedAppsRepository: InstalledAppsRepository,
    private val preferencesRepository: LauncherPreferencesRepository
) : AndroidViewModel(application) {

    private val _rawInstalledApps = MutableStateFlow<List<AppItem>>(emptyList())
    private val _isLoadingApps = MutableStateFlow(true)
    private val _searchQuery = MutableStateFlow("")
    private val _isDrawerOpen = MutableStateFlow(false)
    private val _selectedAppForMenu = MutableStateFlow<AppItem?>(null)
    private val _statusMessage = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private var statusClearJob: Job? = null

    init {
        observeState()
        refreshInstalledApps()
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
        statusMessage: String?
    ): LauncherUiState {
        val allAppsWithPin = rawApps.map { app ->
            app.copy(isPinned = prefs.pinnedAppIds.contains(app.id))
        }

        val pinnedApps = allAppsWithPin.filter { it.isPinned }
        val filteredDrawerApps = filterAppsByQuery(allAppsWithPin, query)
        val alphabetMap = buildAlphabetIndexMap(filteredDrawerApps)

        return LauncherUiState(
            isLoading = isLoading,
            pinnedApps = pinnedApps,
            allApps = allAppsWithPin,
            filteredDrawerApps = filteredDrawerApps,
            alphabetIndexMap = alphabetMap,
            availableLetters = ('A'..'Z').toList(),
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
        viewModelScope.launch {
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
        val previousQuery = _searchQuery.value
        _searchQuery.value = newQuery

        val currentState = _uiState.value
        val trimmed = newQuery.trim()
        val isTypingForward = trimmed.length > previousQuery.trim().length

        if (currentState.autoLaunchSingleMatch && isTypingForward && trimmed.isNotEmpty()) {
            val matches = filterAppsByQuery(currentState.allApps, trimmed)
            if (matches.size == 1) {
                val matchedApp = matches.first()
                _searchQuery.value = ""
                _isDrawerOpen.value = false
                installedAppsRepository.launchApp(matchedApp)
                showTransientMessage("Auto-opened ${matchedApp.label}")
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun launchApp(app: AppItem) {
        installedAppsRepository.launchApp(app)
        _selectedAppForMenu.value = null
        if (_isDrawerOpen.value) {
            _searchQuery.value = ""
            _isDrawerOpen.value = false
        }
        showTransientMessage("Opening ${app.label}")
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
            val message = if (isNowPinned) {
                "Pinned ${app.label} to Home"
            } else {
                "Unpinned ${app.label} from Home"
            }
            showTransientMessage(message)
        }
    }

    fun openAppInfo(app: AppItem) {
        _selectedAppForMenu.value = null
        installedAppsRepository.openAppInfo(app)
        showTransientMessage("App info: ${app.label}")
    }

    fun toggleTheme() {
        viewModelScope.launch {
            val nextDark = !_uiState.value.isDarkTheme
            preferencesRepository.setDarkTheme(nextDark)
            showTransientMessage(if (nextDark) "Dark theme enabled" else "Warm paper theme enabled")
        }
    }

    fun toggleShowAppIcons() {
        viewModelScope.launch {
            val nextIcons = !_uiState.value.showAppIcons
            preferencesRepository.setShowAppIcons(nextIcons)
            showTransientMessage(if (nextIcons) "Small monochrome icons on" else "Pure text mode on")
        }
    }

    fun toggleAutoLaunchSingleMatch() {
        viewModelScope.launch {
            val nextAuto = !_uiState.value.autoLaunchSingleMatch
            preferencesRepository.setAutoLaunchSingleMatch(nextAuto)
            showTransientMessage(
                if (nextAuto) "Auto-open single search match on" else "Auto-open single search match off"
            )
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private fun showTransientMessage(message: String) {
        statusClearJob?.cancel()
        _statusMessage.value = message
        statusClearJob = viewModelScope.launch {
            delay(2200L)
            _statusMessage.value = null
        }
    }

    private fun filterAppsByQuery(apps: List<AppItem>, query: String): List<AppItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return apps
        val lowerQuery = trimmed.lowercase(Locale.getDefault())

        val (prefixMatches, containsMatches) = apps
            .filter { it.label.lowercase(Locale.getDefault()).contains(lowerQuery) }
            .partition { it.label.lowercase(Locale.getDefault()).startsWith(lowerQuery) }

        return prefixMatches + containsMatches
    }

    private fun buildAlphabetIndexMap(apps: List<AppItem>): Map<Char, Int> {
        val map = mutableMapOf<Char, Int>()
        apps.forEachIndexed { index, app ->
            val letter = app.firstLetterSection
            if (!map.containsKey(letter)) {
                map[letter] = index
            }
        }
        return map
    }

    class Factory(
        private val application: Application,
        private val installedAppsRepository: InstalledAppsRepository = InstalledAppsRepository(application),
        private val preferencesRepository: LauncherPreferencesRepository = LauncherPreferencesRepository(application)
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LauncherViewModel(
                application = application,
                installedAppsRepository = installedAppsRepository,
                preferencesRepository = preferencesRepository
            ) as T
        }
    }
}
