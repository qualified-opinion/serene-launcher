package app.serene.launcher.model

/** How the launcher picks between its light and dark looks. */
enum class ThemeMode {
    /** Follow the phone's dark mode setting. */
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        /**
         * Reads the stored theme. Before the Settings screen existed the theme was a plain
         * dark/light switch, so a missing [stored] value falls back to that [legacyDark] switch,
         * and only a fresh install (neither stored) follows the system.
         */
        fun fromStored(stored: String?, legacyDark: Boolean?): ThemeMode =
            entries.firstOrNull { it.name == stored }
                ?: when (legacyDark) {
                    true -> DARK
                    false -> LIGHT
                    null -> SYSTEM
                }
    }
}
