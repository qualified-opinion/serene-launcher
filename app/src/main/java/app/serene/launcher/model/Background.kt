package app.serene.launcher.model

/**
 * The colour behind Home, the app list and Settings. [PAPER] is Serene's own warm white (or
 * near-black in dark mode); the others are soft tints, each with a light and a dark version.
 */
enum class BackgroundColour {
    PAPER,
    SAND,
    SAGE,
    MIST,
    LAVENDER,
    BLUSH;

    companion object {
        /** Reads the stored colour. Nothing stored, or a name this version doesn't know, is [PAPER]. */
        fun fromStored(stored: String?): BackgroundColour = entries.firstOrNull { it.name == stored } ?: PAPER
    }
}

/**
 * How much the background colour covers the phone's wallpaper when the wallpaper is shown.
 * [scrimAlpha] is how opaque that cover is: lower shows more of the wallpaper.
 */
enum class WallpaperDim(val scrimAlpha: Float) {
    /** The wallpaper shows clearly; text gets a soft glow to stay readable. */
    LIGHT(0.35f),
    /** A balance between the wallpaper and readability. The default. */
    MEDIUM(0.6f),
    /** The look from before this setting existed: all text keeps 4.5:1 contrast over any wallpaper. */
    STRONG(0.75f);

    companion object {
        val DEFAULT = MEDIUM

        /** Reads the stored strength. Nothing stored, or an unknown name, is [DEFAULT]. */
        fun fromStored(stored: String?): WallpaperDim = entries.firstOrNull { it.name == stored } ?: DEFAULT
    }
}
