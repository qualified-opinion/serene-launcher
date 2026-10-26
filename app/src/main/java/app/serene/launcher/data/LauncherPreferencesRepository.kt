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
    val customLabels: Map<String, String> = emptyMap()
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
                customLabels = decodeLabels(prefs[KEY_CUSTOM_APP_LABELS])
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

    suspend fun setOpenKeyboardOnDrawer(open: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_OPEN_KEYBOARD_ON_DRAWER] = open
        }
    }
}
