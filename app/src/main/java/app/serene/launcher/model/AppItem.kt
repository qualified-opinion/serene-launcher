package app.serene.launcher.model

import androidx.annotation.StringRes
import app.serene.launcher.domain.AppSearch

enum class FallbackIconGlyph {
    GENERIC,
    PHONE,
    MESSAGES,
    BROWSER,
    CAMERA,
    CLOCK,
    SETTINGS,
    CALENDAR,
    CALCULATOR,
    CONTACTS,
    FILES,
    GALLERY,
    MAIL,
    MAPS,
    MUSIC,
    NOTES,
    WEATHER
}

/** Which kind of Android profile an app lives in. Only [PERSONAL] apps have ids without a serial. */
enum class ProfileKind { PERSONAL, WORK, PRIVATE, CLONE, OTHER }

data class AppItem(
    val id: String,
    val label: String,
    val packageName: String,
    val activityName: String? = null,
    val fallbackGlyph: FallbackIconGlyph = FallbackIconGlyph.GENERIC,
    val isPinned: Boolean = false,
    /** Serial number of the Android user profile the app belongs to (personal, work, ...). */
    val profileSerial: Long = 0L,
    val profileKind: ProfileKind = ProfileKind.PERSONAL,
    /** The profile is switched off (work apps paused); launching asks Android to turn it back on. */
    val isProfilePaused: Boolean = false,
    val isHidden: Boolean = false,
    /** The app's own name when the user renamed it, so it can be shown and restored. */
    val originalLabel: String? = null
) {
    /** True for every profile other than the personal one, so the id carries the profile serial. */
    val isWorkProfile: Boolean
        get() = profileKind != ProfileKind.PERSONAL

    val firstLetterSection: Char
        get() = AppSearch.sectionOf(label)
}

data class LauncherUiState(
    val isLoading: Boolean = true,
    val pinnedApps: List<AppItem> = emptyList(),
    val allApps: List<AppItem> = emptyList(),
    /** Apps the user hid from the drawer, for the Settings list. */
    val hiddenApps: List<AppItem> = emptyList(),
    val filteredDrawerApps: List<AppItem> = emptyList(),
    val alphabetIndexMap: Map<Char, Int> = emptyMap(),
    val availableLetters: List<Char> = AppSearch.railLetters,
    val searchQuery: String = "",
    val isDrawerOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val isReorderingPins: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showAppIcons: Boolean = false,
    val autoLaunchSingleMatch: Boolean = true,
    val openKeyboardOnDrawer: Boolean = false,
    val selectedAppForMenu: AppItem? = null,
    val appBeingRenamed: AppItem? = null,
    val statusMessage: StatusMessage? = null
)

/**
 * A short confirmation shown in the status pill, kept as a string resource and its arguments so
 * the ViewModel needs no Context and the text follows the phone's language.
 */
data class StatusMessage(@StringRes val resId: Int, val args: List<Any> = emptyList())
