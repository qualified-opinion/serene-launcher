package app.serene.launcher.model

/** One of an app's own shortcuts ("New message", "Navigate home"), shown in its long-press menu. */
data class AppShortcut(
    val id: String,
    val packageName: String,
    val label: String,
    /** The profile of the app the shortcut belongs to, so a work app's shortcut opens in work. */
    val profileSerial: Long
)
