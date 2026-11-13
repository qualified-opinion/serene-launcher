package app.serene.launcher.data

import app.serene.launcher.model.BackgroundColour
import app.serene.launcher.model.ThemeMode
import app.serene.launcher.model.WallpaperDim
import org.json.JSONArray
import org.json.JSONObject

/**
 * The settings backup file: plain JSON the user saves and opens with the phone's file picker.
 * Version 1 holds pins in order, hidden apps, custom names, the swipe apps and every option.
 * Fields added later (such as the background colour) are simply missing from older files.
 * Reading is forgiving: an unknown or broken field is skipped and counted, never fatal.
 */
object SettingsBackup {
    const val FORMAT = "serene-launcher-settings"
    const val VERSION = 1

    /** Larger files can't be a settings backup, so they're refused before reading them whole. */
    const val MAX_BYTES = 1_000_000

    /**
     * A real backup is almost flat (the file, then a list or a name map). Anything nested deeper
     * than this is refused before org.json walks it. org.json does that walk with recursion, and
     * a deep file makes it throw StackOverflowError.
     */
    private const val MAX_NESTING = 64

    /** What a backup file held. A null field wasn't in the file and is left as it is on import. */
    data class Contents(
        val pins: List<String>? = null,
        val hidden: Set<String>? = null,
        val names: Map<String, String>? = null,
        val theme: ThemeMode? = null,
        val showAppIcons: Boolean? = null,
        val autoLaunchSingleMatch: Boolean? = null,
        val openKeyboardOnDrawer: Boolean? = null,
        val swipeDownForNotifications: Boolean? = null,
        val doubleTapToLock: Boolean? = null,
        /** An empty string means "this swipe does nothing". */
        val swipeLeftApp: String? = null,
        val swipeRightApp: String? = null,
        val showWallpaper: Boolean? = null,
        val useWallpaperColours: Boolean? = null,
        val backgroundColour: BackgroundColour? = null,
        val wallpaperDim: WallpaperDim? = null,
        /** How many entries were skipped because they were broken. */
        val skipped: Int = 0
    )

    sealed interface ReadResult {
        data class Ok(val contents: Contents) : ReadResult
        /** Not a Serene settings file at all: nothing should change. */
        data object NotABackup : ReadResult
    }

    fun write(prefs: LauncherUserPreferences, theme: ThemeMode = prefs.themeMode): String = JSONObject()
        .put("format", FORMAT)
        .put("version", VERSION)
        .put("pins", JSONArray(prefs.pinnedAppOrder))
        .put("hidden", JSONArray(prefs.hiddenAppIds.sorted()))
        .put("names", JSONObject().apply { prefs.customLabels.toSortedMap().forEach { (id, name) -> put(id, name) } })
        .put("theme", theme.name)
        .put("showAppIcons", prefs.showAppIcons)
        .put("autoLaunchSingleMatch", prefs.autoLaunchSingleMatch)
        .put("openKeyboardOnDrawer", prefs.openKeyboardOnDrawer)
        .put("swipeDownForNotifications", prefs.swipeDownForNotifications)
        .put("doubleTapToLock", prefs.doubleTapToLock)
        .put("swipeLeftApp", prefs.swipeLeftAppId.orEmpty())
        .put("swipeRightApp", prefs.swipeRightAppId.orEmpty())
        .put("showWallpaper", prefs.showWallpaper)
        .put("useWallpaperColours", prefs.useWallpaperColours)
        .put("backgroundColour", prefs.backgroundColour.name)
        .put("wallpaperDim", prefs.wallpaperDim.name)
        .toString(2)

    fun read(text: String): ReadResult = try {
        // StackOverflowError is an Error, so catch (Exception) would let it crash the home screen.
        // A file this deep is not a backup either way.
        if (nestsTooDeep(text)) ReadResult.NotABackup else readContents(text)
    } catch (_: Throwable) {
        ReadResult.NotABackup
    }

    private fun readContents(text: String): ReadResult {
        val json = JSONObject(text)
        // Newer versions are read too: the fields this version knows keep their meaning.
        if (json.optString("format") != FORMAT || json.optInt("version", 0) < 1) return ReadResult.NotABackup

        var skipped = 0
        fun <T> field(name: String, parse: (Any) -> T?): T? {
            if (!json.has(name)) return null
            return parse(json.get(name)).also { if (it == null) skipped++ }
        }
        fun ids(value: Any): List<String>? {
            val array = value as? JSONArray ?: return null
            return (0 until array.length()).mapNotNull { i ->
                (array.opt(i) as? String)?.takeIf(::isAppId).also { if (it == null) skipped++ }
            }.distinct()
        }
        fun bool(value: Any) = value as? Boolean
        fun swipeApp(value: Any) = (value as? String)?.takeIf { it.isEmpty() || isAppId(it) }

        val contents = Contents(
            pins = field("pins", ::ids),
            hidden = field("hidden") { ids(it)?.toSet() },
            names = field("names") { value ->
                val names = value as? JSONObject ?: return@field null
                names.keys().asSequence().mapNotNull { id ->
                    val name = (names.opt(id) as? String)?.let { LauncherPreferencesRepository.cleanLabel(it) }
                    if (isAppId(id) && !name.isNullOrEmpty()) id to name else null.also { skipped++ }
                }.toMap()
            },
            theme = field("theme") { value -> ThemeMode.entries.firstOrNull { it.name == value } },
            showAppIcons = field("showAppIcons", ::bool),
            autoLaunchSingleMatch = field("autoLaunchSingleMatch", ::bool),
            openKeyboardOnDrawer = field("openKeyboardOnDrawer", ::bool),
            swipeDownForNotifications = field("swipeDownForNotifications", ::bool),
            doubleTapToLock = field("doubleTapToLock", ::bool),
            swipeLeftApp = field("swipeLeftApp", ::swipeApp),
            swipeRightApp = field("swipeRightApp", ::swipeApp),
            showWallpaper = field("showWallpaper", ::bool),
            useWallpaperColours = field("useWallpaperColours", ::bool),
            backgroundColour = field("backgroundColour") { value -> BackgroundColour.entries.firstOrNull { it.name == value } },
            wallpaperDim = field("wallpaperDim") { value -> WallpaperDim.entries.firstOrNull { it.name == value } }
        )
        return ReadResult.Ok(contents.copy(skipped = skipped))
    }

    /** `package/activity`, optionally `@serial`, with nothing that would break how ids are stored. */
    private fun isAppId(id: String): Boolean =
        id.length in 3..512 && '/' in id && id.none { it == '\n' || it == '\t' || it.isWhitespace() }

    /**
     * True when objects or arrays nest deeper than [MAX_NESTING]. Walks the text in a loop, so
     * this check itself can't overflow. Braces and brackets inside strings don't count.
     */
    private fun nestsTooDeep(text: String): Boolean {
        var depth = 0
        var inString = false
        var escaped = false
        for (ch in text) {
            if (inString) {
                when {
                    escaped -> escaped = false
                    ch == '\\' -> escaped = true
                    ch == '"' -> inString = false
                }
                continue
            }
            when (ch) {
                '"' -> inString = true
                '{', '[' -> {
                    depth++
                    if (depth > MAX_NESTING) return true
                }
                '}', ']' -> if (depth > 0) depth--
            }
        }
        return false
    }
}
