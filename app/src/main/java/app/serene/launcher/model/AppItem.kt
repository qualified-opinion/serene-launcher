package app.serene.launcher.model

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

data class AppItem(
    val id: String,
    val label: String,
    val packageName: String,
    val activityName: String? = null,
    val fallbackGlyph: FallbackIconGlyph = FallbackIconGlyph.GENERIC,
    val isPinned: Boolean = false,
    /** Serial number of the Android user profile the app belongs to (personal, work, ...). */
    val profileSerial: Long = 0L,
    val isWorkProfile: Boolean = false
) {
    val firstLetterSection: Char
        get() {
            val first = label.trim().firstOrNull()?.uppercaseChar() ?: '#'
            return if (first in 'A'..'Z') first else '#'
        }
}

data class LauncherUiState(
    val isLoading: Boolean = true,
    val pinnedApps: List<AppItem> = emptyList(),
    val allApps: List<AppItem> = emptyList(),
    val filteredDrawerApps: List<AppItem> = emptyList(),
    val alphabetIndexMap: Map<Char, Int> = emptyMap(),
    val availableLetters: List<Char> = ('A'..'Z').toList(),
    val searchQuery: String = "",
    val isDrawerOpen: Boolean = false,
    val isDarkTheme: Boolean = false,
    val showAppIcons: Boolean = false,
    val autoLaunchSingleMatch: Boolean = true,
    val selectedAppForMenu: AppItem? = null,
    val statusMessage: String? = null
)
