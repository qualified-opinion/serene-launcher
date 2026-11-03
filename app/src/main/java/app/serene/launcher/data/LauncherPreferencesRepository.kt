package app.serene.launcher.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import app.serene.launcher.domain.AppSearch
import app.serene.launcher.model.AppItem
import app.serene.launcher.model.SwipeSide
import app.serene.launcher.model.ThemeMode
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.retryWhen

internal fun createLauncherPreferencesStore(
    produceFile: () -> File,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
): DataStore<Preferences> = PreferenceDataStoreFactory.create(
    corruptionHandler = ReplaceFileCorruptionHandler {
        Log.w("LauncherPreferences", "Damaged settings replaced with defaults")
        emptyPreferences()
    },
    scope = scope,
    produceFile = produceFile
)

private object LauncherPreferencesStore {
    private var store: DataStore<Preferences>? = null

    @Synchronized
    fun get(context: Context): DataStore<Preferences> = store ?: createLauncherPreferencesStore(
        produceFile = { context.preferencesDataStoreFile("serene_launcher_preferences") }
    ).also { store = it }
}

data class LauncherUserPreferences(
    val pinnedAppIds: Set<String> = emptySet(),
    val pinnedAppOrder: List<String> = emptyList(),
    val hasInitializedDefaultPins: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showAppIcons: Boolean = false,      // Pure text by default
    val autoLaunchSingleMatch: Boolean = true,
    val openKeyboardOnDrawer: Boolean = false,
    val hiddenAppIds: Set<String> = emptySet(),
    /** Names the user gave apps, by app id. */
    val customLabels: Map<String, String> = emptyMap(),
    /** Swiping down on Home opens the notifications. On unless the user turns it off. */
    val swipeDownForNotifications: Boolean = true,
    /** Double-tapping empty space on Home locks the screen. Off until the user turns it on. */
    val doubleTapToLock: Boolean = false,
    /** Ids of the apps a swipe left or right on Home opens. Null: that swipe does nothing. */
    val swipeLeftAppId: String? = null,
    val swipeRightAppId: String? = null,
    /** The phone's wallpaper shows behind Home and the app list. Off by default. */
    val showWallpaper: Boolean = false,
    /** Material You colours taken from the wallpaper (Android 12+). Off by default. */
    val useWallpaperColours: Boolean = false
)

class LauncherPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(LauncherPreferencesStore.get(context.applicationContext))

    companion object {
        val KEY_PINNED_APP_ORDER = stringPreferencesKey("pinned_app_order")
        val KEY_PINNED_APP_IDS = stringSetPreferencesKey("pinned_app_ids")
        val KEY_HAS_INITIALIZED_DEFAULT_PINS = booleanPreferencesKey("has_initialized_default_pins")
        /** Pre-Settings light/dark switch, still read so older installs keep their theme. */
        val KEY_IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_OPEN_KEYBOARD_ON_DRAWER = booleanPreferencesKey("open_keyboard_on_drawer")
        val KEY_SHOW_APP_ICONS = booleanPreferencesKey("show_app_icons")
        val KEY_AUTO_LAUNCH_SINGLE = booleanPreferencesKey("auto_launch_single_match")
        val KEY_HIDDEN_APP_IDS = stringSetPreferencesKey("hidden_app_ids")
        val KEY_SWIPE_DOWN_NOTIFICATIONS = booleanPreferencesKey("swipe_down_notifications")
        val KEY_DOUBLE_TAP_LOCK = booleanPreferencesKey("double_tap_lock")
        val KEY_SHOW_WALLPAPER = booleanPreferencesKey("show_wallpaper")
        val KEY_WALLPAPER_COLOURS = booleanPreferencesKey("wallpaper_colours")
        val KEY_SWIPE_LEFT_APP = stringPreferencesKey("swipe_left_app")
        val KEY_SWIPE_RIGHT_APP = stringPreferencesKey("swipe_right_app")
        private val SWIPE_APP_KEYS = listOf(KEY_SWIPE_LEFT_APP, KEY_SWIPE_RIGHT_APP)

        internal fun swipeKey(side: SwipeSide) = when (side) {
            SwipeSide.LEFT -> KEY_SWIPE_LEFT_APP
            SwipeSide.RIGHT -> KEY_SWIPE_RIGHT_APP
        }
        /** One "id<TAB>name" entry per line. Ids can't hold tabs or newlines, and names are stripped of them. */
        val KEY_CUSTOM_APP_LABELS = stringPreferencesKey("custom_app_labels")

        internal fun decodeLabels(stored: String?): Map<String, String> = stored.orEmpty()
            .lineSequence()
            .mapNotNull { line ->
                val tab = line.indexOf('\t')
                if (tab <= 0 || tab == line.length - 1) null else line.substring(0, tab) to line.substring(tab + 1)
            }
            .toMap()

        internal fun encodeLabels(labels: Map<String, String>): String =
            labels.entries.joinToString("\n") { (id, name) -> "$id\t$name" }

        /** Trims a name and turns tabs, newlines and other control characters into spaces. */
        internal fun cleanLabel(name: String): String =
            name.map { if (it.isISOControl()) ' ' else it }.joinToString("").trim()
    }

    val preferencesFlow = preferencesFlow(flowOf(emptyList()))

    fun preferencesFlow(apps: Flow<List<AppItem>>): Flow<LauncherUserPreferences> = dataStore.data
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
        .combine(apps) { prefs, installed ->
            LauncherUserPreferences(
                pinnedAppIds = prefs[KEY_PINNED_APP_IDS] ?: emptySet(),
                pinnedAppOrder = pinOrder(prefs, installed),
                hasInitializedDefaultPins = prefs[KEY_HAS_INITIALIZED_DEFAULT_PINS] ?: false,
                themeMode = ThemeMode.fromStored(prefs[KEY_THEME_MODE], prefs[KEY_IS_DARK_THEME]),
                showAppIcons = prefs[KEY_SHOW_APP_ICONS] ?: false,
                autoLaunchSingleMatch = prefs[KEY_AUTO_LAUNCH_SINGLE] ?: true,
                openKeyboardOnDrawer = prefs[KEY_OPEN_KEYBOARD_ON_DRAWER] ?: false,
                hiddenAppIds = prefs[KEY_HIDDEN_APP_IDS] ?: emptySet(),
                customLabels = decodeLabels(prefs[KEY_CUSTOM_APP_LABELS]),
                swipeDownForNotifications = prefs[KEY_SWIPE_DOWN_NOTIFICATIONS] ?: true,
                doubleTapToLock = prefs[KEY_DOUBLE_TAP_LOCK] ?: false,
                swipeLeftAppId = prefs[KEY_SWIPE_LEFT_APP],
                swipeRightAppId = prefs[KEY_SWIPE_RIGHT_APP],
                showWallpaper = prefs[KEY_SHOW_WALLPAPER] ?: false,
                useWallpaperColours = prefs[KEY_WALLPAPER_COLOURS] ?: false
            )
        }

    suspend fun initializeDefaultPinsIfNeeded(defaultIds: Set<String>) {
        if (defaultIds.isEmpty()) return
        dataStore.edit { prefs ->
            val initialized = prefs[KEY_HAS_INITIALIZED_DEFAULT_PINS] ?: false
            if (!initialized) {
                if (prefs[KEY_PINNED_APP_IDS].isNullOrEmpty()) prefs.writePins(defaultIds.toList())
                prefs[KEY_HAS_INITIALIZED_DEFAULT_PINS] = true
            }
        }
    }

    // Resolve legacy pins against the same label comparator as Home, without migrating on read.
    private fun pinOrder(prefs: Preferences, apps: List<AppItem>): List<String> {
        val ids = prefs[KEY_PINNED_APP_IDS] ?: emptySet()
        val stored = prefs[KEY_PINNED_APP_ORDER]
        val ordered = if (stored != null) stored.split('\n').filter { it in ids }.distinct()
            else AppSearch.sort(apps).map { it.id }.filter { it in ids }
        return ordered + (ids - ordered.toSet()).sorted()
    }

    private fun MutablePreferences.writePins(order: List<String>) {
        this[KEY_PINNED_APP_ORDER] = order.joinToString("\n")
        this[KEY_PINNED_APP_IDS] = order.toSet()
        this[KEY_HAS_INITIALIZED_DEFAULT_PINS] = true
    }

    suspend fun togglePinApp(appId: String, apps: List<AppItem> = emptyList()): Boolean {
        var nowPinned = false
        dataStore.edit { prefs ->
            val current = pinOrder(prefs, apps)
            nowPinned = appId !in current
            prefs.writePins(if (nowPinned) current + appId else current - appId)
        }
        return nowPinned
    }

    suspend fun setPinState(appId: String, pinned: Boolean, apps: List<AppItem> = emptyList()) {
        dataStore.edit { prefs ->
            val current = pinOrder(prefs, apps)
            if ((appId in current) == pinned) return@edit
            prefs.writePins(if (pinned) (current + appId).distinct() else current - appId)
        }
    }

    suspend fun movePin(appId: String, offset: Int, apps: List<AppItem>) {
        dataStore.edit { prefs ->
            val order = pinOrder(prefs, apps).toMutableList()
            val visible = order.filter { id -> apps.any { it.id == id } }
            val index = visible.indexOf(appId)
            if (index < 0 || offset !in listOf(-1, 1)) return@edit
            val other = visible.getOrNull(index + offset) ?: return@edit
            val from = order.indexOf(appId)
            val to = order.indexOf(other)
            order[from] = other
            order[to] = appId
            prefs.writePins(order)
        }
    }

    suspend fun prunePins(packageName: String, profileSerial: Long?, apps: List<AppItem>) {
        dataStore.edit { prefs ->
            fun isRemoved(id: String) = id.substringBefore('/') == packageName &&
                (if (profileSerial == null) '@' !in id else id.endsWith("@$profileSerial"))

            // An uninstalled app also loses its hidden flag and custom name.
            val hidden = prefs[KEY_HIDDEN_APP_IDS].orEmpty()
            if (hidden.any(::isRemoved)) {
                prefs[KEY_HIDDEN_APP_IDS] = hidden.filterNot(::isRemoved).toSet()
            }
            val labels = decodeLabels(prefs[KEY_CUSTOM_APP_LABELS])
            if (labels.keys.any(::isRemoved)) {
                val keptLabels = labels.filterKeys { !isRemoved(it) }
                if (keptLabels.isEmpty()) prefs.remove(KEY_CUSTOM_APP_LABELS)
                else prefs[KEY_CUSTOM_APP_LABELS] = encodeLabels(keptLabels)
            }
            // ...and stops being what a swipe on Home opens.
            SWIPE_APP_KEYS.forEach { key -> if (prefs[key]?.let(::isRemoved) == true) prefs.remove(key) }

            val order = pinOrder(prefs, apps)
            val kept = order.filterNot(::isRemoved)
            if (kept != order) {
                if (prefs[KEY_PINNED_APP_ORDER] == null) {
                    prefs[KEY_PINNED_APP_IDS] = kept.toSet()
                } else {
                    prefs.writePins(kept)
                }
            }
        }
    }

    /**
     * Swaps saved app ids for new ones in pins, hidden apps, custom names and swipe apps. [replacements] gets
     * every saved id and returns old id to new id. Pins keep their place on Home. If the new id was
     * already saved, it is kept once, and a name already given to it wins over the old one.
     * No key is renamed or dropped: only the ids inside them change.
     */
    suspend fun replaceAppIds(replacements: (storedIds: Set<String>) -> Map<String, String>) {
        dataStore.edit { prefs ->
            val pinIds = prefs[KEY_PINNED_APP_IDS].orEmpty()
            val hidden = prefs[KEY_HIDDEN_APP_IDS].orEmpty()
            val labels = decodeLabels(prefs[KEY_CUSTOM_APP_LABELS])
            val swipeIds = SWIPE_APP_KEYS.mapNotNull { prefs[it] }
            val map = replacements(pinIds + hidden + labels.keys + swipeIds)
            if (map.isEmpty()) return@edit
            fun swap(id: String) = map[id] ?: id

            SWIPE_APP_KEYS.forEach { key ->
                val id = prefs[key]
                if (id != null && id in map) prefs[key] = swap(id)
            }

            if (pinIds.any { it in map }) {
                if (prefs[KEY_PINNED_APP_ORDER] == null) {
                    // Old installs with no saved order sort pins by name, so only the set changes.
                    prefs[KEY_PINNED_APP_IDS] = pinIds.map(::swap).toSet()
                } else {
                    prefs.writePins(pinOrder(prefs, emptyList()).map(::swap).distinct())
                }
            }
            if (hidden.any { it in map }) {
                prefs[KEY_HIDDEN_APP_IDS] = hidden.map(::swap).toSet()
            }
            if (labels.keys.any { it in map }) {
                val moved = labels.filterKeys { it in map }.mapKeys { (id, _) -> swap(id) }
                prefs[KEY_CUSTOM_APP_LABELS] = encodeLabels(moved + labels.filterKeys { it !in map })
            }
        }
    }

    suspend fun setHidden(appId: String, hidden: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_HIDDEN_APP_IDS] ?: emptySet()
            prefs[KEY_HIDDEN_APP_IDS] = if (hidden) current + appId else current - appId
        }
    }

    /** Saves [name] as the app's name, or goes back to the app's own name when [name] is blank. */
    suspend fun setCustomLabel(appId: String, name: String?) {
        val cleaned = name?.let(::cleanLabel).orEmpty()
        dataStore.edit { prefs ->
            val labels = decodeLabels(prefs[KEY_CUSTOM_APP_LABELS]).toMutableMap()
            if (cleaned.isEmpty()) labels.remove(appId) else labels[appId] = cleaned
            if (labels.isEmpty()) prefs.remove(KEY_CUSTOM_APP_LABELS)
            else prefs[KEY_CUSTOM_APP_LABELS] = encodeLabels(labels)
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

    suspend fun setSwipeDownForNotifications(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_SWIPE_DOWN_NOTIFICATIONS] = enabled }
    }

    suspend fun setDoubleTapToLock(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_DOUBLE_TAP_LOCK] = enabled }
    }

    /**
     * Applies a settings backup in one step. Whatever the file had replaces the current value;
     * whatever it didn't have stays as it is. Pins for apps that aren't installed are kept, so
     * they appear once the app is installed.
     */
    suspend fun importBackup(backup: SettingsBackup.Contents) {
        dataStore.edit { prefs ->
            backup.pins?.let { prefs.writePins(it) }
            backup.hidden?.let { prefs[KEY_HIDDEN_APP_IDS] = it }
            val names = backup.names
            if (names != null) {
                if (names.isEmpty()) prefs.remove(KEY_CUSTOM_APP_LABELS)
                else prefs[KEY_CUSTOM_APP_LABELS] = encodeLabels(names)
            }
            backup.theme?.let { prefs[KEY_THEME_MODE] = it.name }
            backup.showAppIcons?.let { prefs[KEY_SHOW_APP_ICONS] = it }
            backup.autoLaunchSingleMatch?.let { prefs[KEY_AUTO_LAUNCH_SINGLE] = it }
            backup.openKeyboardOnDrawer?.let { prefs[KEY_OPEN_KEYBOARD_ON_DRAWER] = it }
            backup.swipeDownForNotifications?.let { prefs[KEY_SWIPE_DOWN_NOTIFICATIONS] = it }
            backup.doubleTapToLock?.let { prefs[KEY_DOUBLE_TAP_LOCK] = it }
            backup.showWallpaper?.let { prefs[KEY_SHOW_WALLPAPER] = it }
            backup.useWallpaperColours?.let { prefs[KEY_WALLPAPER_COLOURS] = it }
            for ((key, id) in listOf(KEY_SWIPE_LEFT_APP to backup.swipeLeftApp, KEY_SWIPE_RIGHT_APP to backup.swipeRightApp)) {
                if (id == null) continue
                if (id.isEmpty()) prefs.remove(key) else prefs[key] = id
            }
        }
    }

    suspend fun setShowWallpaper(show: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_SHOW_WALLPAPER] = show }
    }

    suspend fun setUseWallpaperColours(use: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_WALLPAPER_COLOURS] = use }
    }

    /** Saves the app a swipe to [side] opens, or clears it when [appId] is null. */
    suspend fun setSwipeApp(side: SwipeSide, appId: String?) {
        dataStore.edit { prefs ->
            if (appId == null) prefs.remove(swipeKey(side)) else prefs[swipeKey(side)] = appId
        }
    }

    suspend fun setOpenKeyboardOnDrawer(open: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_OPEN_KEYBOARD_ON_DRAWER] = open
        }
    }
}
