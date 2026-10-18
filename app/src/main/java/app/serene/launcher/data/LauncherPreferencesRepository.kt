package app.serene.launcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "serene_launcher_preferences"
)

data class LauncherUserPreferences(
    val pinnedAppIds: Set<String> = emptySet(),
    val hasInitializedDefaultPins: Boolean = false,
    val isDarkTheme: Boolean = false,       // Warm paper light mode by default
    val showAppIcons: Boolean = false,      // Pure text by default
    val autoLaunchSingleMatch: Boolean = true
)

class LauncherPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.applicationContext.launcherDataStore)

    companion object {
        val KEY_PINNED_APP_IDS = stringSetPreferencesKey("pinned_app_ids")
        val KEY_HAS_INITIALIZED_DEFAULT_PINS = booleanPreferencesKey("has_initialized_default_pins")
        val KEY_IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val KEY_SHOW_APP_ICONS = booleanPreferencesKey("show_app_icons")
        val KEY_AUTO_LAUNCH_SINGLE = booleanPreferencesKey("auto_launch_single_match")
    }

    val preferencesFlow: Flow<LauncherUserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            LauncherUserPreferences(
                pinnedAppIds = prefs[KEY_PINNED_APP_IDS] ?: emptySet(),
                hasInitializedDefaultPins = prefs[KEY_HAS_INITIALIZED_DEFAULT_PINS] ?: false,
                isDarkTheme = prefs[KEY_IS_DARK_THEME] ?: false,
                showAppIcons = prefs[KEY_SHOW_APP_ICONS] ?: false,
                autoLaunchSingleMatch = prefs[KEY_AUTO_LAUNCH_SINGLE] ?: true
            )
        }

    suspend fun initializeDefaultPinsIfNeeded(defaultIds: Set<String>) {
        dataStore.edit { prefs ->
            val initialized = prefs[KEY_HAS_INITIALIZED_DEFAULT_PINS] ?: false
            if (!initialized) {
                prefs[KEY_PINNED_APP_IDS] = defaultIds
                prefs[KEY_HAS_INITIALIZED_DEFAULT_PINS] = true
            }
        }
    }

    suspend fun togglePinApp(appId: String): Boolean {
        var nowPinned = false
        dataStore.edit { prefs ->
            val current = prefs[KEY_PINNED_APP_IDS] ?: emptySet()
            val updated = if (current.contains(appId)) {
                nowPinned = false
                current - appId
            } else {
                nowPinned = true
                current + appId
            }
            prefs[KEY_PINNED_APP_IDS] = updated
            prefs[KEY_HAS_INITIALIZED_DEFAULT_PINS] = true
        }
        return nowPinned
    }

    suspend fun setPinState(appId: String, pinned: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_PINNED_APP_IDS] ?: emptySet()
            prefs[KEY_PINNED_APP_IDS] = if (pinned) current + appId else current - appId
            prefs[KEY_HAS_INITIALIZED_DEFAULT_PINS] = true
        }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_IS_DARK_THEME] = isDark
        }
    }

    suspend fun setShowAppIcons(showIcons: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_SHOW_APP_ICONS] = showIcons
        }
    }

    suspend fun setAutoLaunchSingleMatch(autoLaunch: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_LAUNCH_SINGLE] = autoLaunch
        }
    }
}
