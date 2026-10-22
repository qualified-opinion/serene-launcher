package app.serene.launcher.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.serene.launcher.model.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import java.io.IOException

internal val settingsCorruptionHandler = ReplaceFileCorruptionHandler<Preferences> {
    Log.w("LauncherPreferences", "Damaged settings replaced with defaults")
    emptyPreferences()
}

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "serene_launcher_preferences",
    corruptionHandler = settingsCorruptionHandler
)

data class LauncherUserPreferences(
    val pinnedAppIds: Set<String> = emptySet(),
    val hasInitializedDefaultPins: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showAppIcons: Boolean = false,      // Pure text by default
    val autoLaunchSingleMatch: Boolean = true,
    val openKeyboardOnDrawer: Boolean = false
)

class LauncherPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.applicationContext.launcherDataStore)

    companion object {
        val KEY_PINNED_APP_IDS = stringSetPreferencesKey("pinned_app_ids")
        val KEY_HAS_INITIALIZED_DEFAULT_PINS = booleanPreferencesKey("has_initialized_default_pins")
        /** Pre-Settings light/dark switch, still read so older installs keep their theme. */
        val KEY_IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_OPEN_KEYBOARD_ON_DRAWER = booleanPreferencesKey("open_keyboard_on_drawer")
        val KEY_SHOW_APP_ICONS = booleanPreferencesKey("show_app_icons")
        val KEY_AUTO_LAUNCH_SINGLE = booleanPreferencesKey("auto_launch_single_match")
    }

    val preferencesFlow: Flow<LauncherUserPreferences> = dataStore.data
        .retryWhen { exception, _ ->
            if (exception is IOException) {
                Log.w("LauncherPreferences", "Unable to read settings; retrying")
                emit(emptyPreferences())
                delay(1_000)
                true
            } else {
                false
            }
        }
        .map { prefs ->
            LauncherUserPreferences(
                pinnedAppIds = prefs[KEY_PINNED_APP_IDS] ?: emptySet(),
                hasInitializedDefaultPins = prefs[KEY_HAS_INITIALIZED_DEFAULT_PINS] ?: false,
                themeMode = ThemeMode.fromStored(prefs[KEY_THEME_MODE], prefs[KEY_IS_DARK_THEME]),
                showAppIcons = prefs[KEY_SHOW_APP_ICONS] ?: false,
                autoLaunchSingleMatch = prefs[KEY_AUTO_LAUNCH_SINGLE] ?: true,
                openKeyboardOnDrawer = prefs[KEY_OPEN_KEYBOARD_ON_DRAWER] ?: false
            )
        }

    suspend fun initializeDefaultPinsIfNeeded(defaultIds: Set<String>) {
        if (defaultIds.isEmpty()) return
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

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
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

    suspend fun setOpenKeyboardOnDrawer(open: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_OPEN_KEYBOARD_ON_DRAWER] = open
        }
    }
}
