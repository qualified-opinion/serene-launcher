package app.serene.launcher.viewmodel

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.serene.launcher.R
import app.serene.launcher.data.AppChange
import app.serene.launcher.data.AppsRepository
import app.serene.launcher.data.InstalledAppsRepository
import app.serene.launcher.data.LauncherPreferencesRepository
import app.serene.launcher.data.LauncherUserPreferences
import app.serene.launcher.data.ProfilePausedException
import app.serene.launcher.domain.AppSearch
import app.serene.launcher.model.AppItem
import app.serene.launcher.model.LauncherUiState
import app.serene.launcher.model.ProfileKind
import app.serene.launcher.model.StatusMessage
import app.serene.launcher.model.ThemeMode
import kotlinx.coroutines.CancellationException
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
    private val _isReordering = MutableStateFlow(false)
    private val _appBeingRenamed = MutableStateFlow<AppItem?>(null)
    private val _isSettingsOpen = MutableStateFlow(false)
    private val _statusMessage = MutableStateFlow<StatusMessage?>(null)

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private var statusClearJob: Job? = null
    private var refreshJob: Job? = null

    init {
        observeState()
        refreshInstalledApps()
        viewModelScope.launch {
            installedAppsRepository.appChanges().collect { change ->
                if (change is AppChange.Removed) savePreferences {
                    preferencesRepository.prunePins(change.packageName, change.profileSerial, _rawInstalledApps.value)
                }
                refreshInstalledApps()
            }
        }
    }

    private fun observeState() {
        viewModelScope.launch {
            combine(
                _rawInstalledApps,
                _isLoadingApps,
                preferencesRepository.preferencesFlow(_rawInstalledApps),
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
                combine(_selectedAppForMenu, _statusMessage, _isSettingsOpen) { selected, status, settingsOpen ->
                    Triple(selected, status, settingsOpen)
                }
            ) { baseState, (selected, status, settingsOpen) ->
                val updatedSelected = selected?.let { sel ->
                    baseState.allApps.find { it.id == sel.id } ?: sel
                }
                baseState.copy(
                    selectedAppForMenu = updatedSelected,
                    statusMessage = status,
                    isSettingsOpen = settingsOpen
                )
            }.combine(
                combine(_isReordering, _appBeingRenamed) { reordering, renaming -> reordering to renaming }
            ) { state, (reordering, renaming) ->
                state.copy(isReorderingPins = reordering, appBeingRenamed = renaming)
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
        val labelledApps = rawApps.map { app ->
            val customLabel = prefs.customLabels[app.id]
            app.copy(
                label = customLabel ?: app.label,
                originalLabel = if (customLabel != null) app.label else null,
                isPinned = prefs.pinnedAppIds.contains(app.id),
                isHidden = app.id in prefs.hiddenAppIds
            )
        }
        // Renamed apps move to where their new name sorts.
        val allAppsWithPin = if (prefs.customLabels.isEmpty()) labelledApps else AppSearch.sort(labelledApps)
        val (hiddenApps, visibleApps) = allAppsWithPin.partition { it.isHidden }

        // Hidden apps leave Home too, but their pins stay saved for when they're unhidden.
        val appsById = visibleApps.associateBy { it.id }
        val pinnedApps = prefs.pinnedAppOrder.mapNotNull { appsById[it] }
        val filteredDrawerApps = AppSearch.filter(visibleApps, query)
        val alphabetMap = AppSearch.alphabetIndex(filteredDrawerApps)

        return LauncherUiState(
            isLoading = isLoading,
            pinnedApps = pinnedApps,
            allApps = visibleApps,
            hiddenApps = hiddenApps,
            filteredDrawerApps = filteredDrawerApps,
            alphabetIndexMap = alphabetMap,
            availableLetters = AppSearch.railLetters,
            searchQuery = query,
            isDrawerOpen = isDrawerOpen,
            themeMode = prefs.themeMode,
            showAppIcons = prefs.showAppIcons,
            autoLaunchSingleMatch = prefs.autoLaunchSingleMatch,
            openKeyboardOnDrawer = prefs.openKeyboardOnDrawer,
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
            if (loaded.isNotEmpty()) {
                savePreferences {
                    preferencesRepository.adoptSettingsFromGoneProfiles(loaded, installedAppsRepository.profileSerials())
                }
                savePreferences {
                    preferencesRepository.initializeDefaultPinsIfNeeded(
                        installedAppsRepository.selectInitialPinnedIds(loaded)
                    )
                }
            }
            _isLoadingApps.value = false
        }
    }

    fun openDrawer() {
        finishReorderingPins()
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
            val matchedApp = matches.singleOrNull() ?: personalCopyIfSameApp(matches)
            // Only a label that starts with the query opens by itself, never a word-start or initials match.
            if (matchedApp != null && AppSearch.isPrefixMatch(matchedApp.label, trimmed)) {
                installedAppsRepository.launchApp(matchedApp)
                    .onSuccess {
                        _searchQuery.value = ""
                        _isDrawerOpen.value = false
                        showTransientMessage(StatusMessage(R.string.status_auto_opened, listOf(matchedApp.label)))
                    }
                    .onFailure { showLaunchFailure(matchedApp, it) }
            }
        }
    }

    /**
     * When every match is the same app installed in several profiles (personal and work), the
     * personal copy, so typing its name still opens it. Null when the matches are different apps.
     */
    private fun personalCopyIfSameApp(matches: List<AppItem>): AppItem? {
        if (matches.size < 2) return null
        val sameApp = matches.all {
            it.packageName == matches[0].packageName && it.activityName == matches[0].activityName &&
                it.label == matches[0].label
        }
        return if (sameApp) matches.singleOrNull { it.profileKind == ProfileKind.PERSONAL } else null
    }

    private fun showLaunchFailure(app: AppItem, error: Throwable) {
        val messageRes = when {
            error !is ProfilePausedException -> R.string.status_cant_open
            error.resumeRequested -> R.string.status_resuming_profile
            else -> R.string.status_profile_paused
        }
        showTransientMessage(StatusMessage(messageRes, listOf(app.label)))
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun launchApp(app: AppItem) {
        finishReorderingPins()
        _selectedAppForMenu.value = null
        installedAppsRepository.launchApp(app)
            .onSuccess {
                if (_isDrawerOpen.value) {
                    _searchQuery.value = ""
                    _isDrawerOpen.value = false
                }
                showTransientMessage(StatusMessage(R.string.status_opening, listOf(app.label)))
            }
            .onFailure { showLaunchFailure(app, it) }
    }

    fun onAppLongPress(app: AppItem) {
        _selectedAppForMenu.value = app
    }

    fun dismissAppMenu() {
        _selectedAppForMenu.value = null
    }

    fun togglePin(app: AppItem) {
        _selectedAppForMenu.value = null
        viewModelScope.launch {
            savePreferences {
                val isNowPinned = preferencesRepository.togglePinApp(app.id, _rawInstalledApps.value)
                val messageRes = if (isNowPinned) R.string.status_pinned else R.string.status_unpinned
                showTransientMessage(StatusMessage(messageRes, listOf(app.label)))
            }
        }
    }

    fun startReorderingPins() {
        _selectedAppForMenu.value = null
        closeDrawer()
        _isReordering.value = true
    }

    fun finishReorderingPins() { _isReordering.value = false }

    fun movePin(app: AppItem, offset: Int) {
        val hiddenIds = _uiState.value.hiddenApps.map { it.id }.toSet()
        val shownApps = _rawInstalledApps.value.filterNot { it.id in hiddenIds }
        viewModelScope.launch { savePreferences { preferencesRepository.movePin(app.id, offset, shownApps) } }
    }

    fun hideApp(app: AppItem) {
        _selectedAppForMenu.value = null
        viewModelScope.launch {
            savePreferences {
                preferencesRepository.setHidden(app.id, hidden = true)
                showTransientMessage(StatusMessage(R.string.status_hidden, listOf(app.label)))
            }
        }
    }

    fun unhideApp(app: AppItem) {
        viewModelScope.launch {
            savePreferences {
                preferencesRepository.setHidden(app.id, hidden = false)
                showTransientMessage(StatusMessage(R.string.status_unhidden, listOf(app.label)))
            }
        }
    }

    fun startRenaming(app: AppItem) {
        _selectedAppForMenu.value = null
        _appBeingRenamed.value = app
    }

    fun cancelRenaming() {
        _appBeingRenamed.value = null
    }

    /** Saves [name] for the app being renamed; a blank name, or null, goes back to the app's own name. */
    fun finishRenaming(name: String?) {
        val app = _appBeingRenamed.value ?: return
        _appBeingRenamed.value = null
        viewModelScope.launch { savePreferences { preferencesRepository.setCustomLabel(app.id, name) } }
    }

    private suspend fun savePreferences(save: suspend () -> Unit) {
        try {
            save()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Log.w("LauncherViewModel", "Unable to save launcher settings")
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

    fun openSettings() {
        finishReorderingPins()
        _selectedAppForMenu.value = null
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { savePreferences { preferencesRepository.setThemeMode(mode) } }
    }

    fun setShowAppIcons(show: Boolean) {
        viewModelScope.launch { savePreferences { preferencesRepository.setShowAppIcons(show) } }
    }

    fun setAutoLaunchSingleMatch(autoLaunch: Boolean) {
        viewModelScope.launch { savePreferences { preferencesRepository.setAutoLaunchSingleMatch(autoLaunch) } }
    }

    fun setOpenKeyboardOnDrawer(open: Boolean) {
        viewModelScope.launch { savePreferences { preferencesRepository.setOpenKeyboardOnDrawer(open) } }
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
