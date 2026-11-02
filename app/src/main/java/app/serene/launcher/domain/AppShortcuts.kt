package app.serene.launcher.domain

import app.serene.launcher.model.AppItem
import app.serene.launcher.model.AppShortcut

/** The parts of Android's ShortcutInfo the menu needs, so the mapping is testable without Android. */
data class ShortcutEntry(
    val id: String,
    val shortLabel: String?,
    val longLabel: String?,
    val rank: Int,
    val isEnabled: Boolean,
    val isDeclaredInManifest: Boolean
)

/** Turns an app's shortcuts into the items of its long-press menu. */
object AppShortcuts {
    /** The menu shows at most this many, like most launchers. */
    const val MAX_IN_MENU = 4

    /**
     * Enabled shortcuts with a name, the app's fixed (manifest) ones first and then the ones it
     * adds while running, each in the app's own order. The short name is used when there is one.
     */
    fun forMenu(app: AppItem, entries: List<ShortcutEntry>): List<AppShortcut> = entries
        .filter { it.isEnabled }
        .sortedWith(compareBy<ShortcutEntry> { !it.isDeclaredInManifest }.thenBy { it.rank })
        .distinctBy { it.id }
        .mapNotNull { entry ->
            val label = listOf(entry.shortLabel, entry.longLabel)
                .firstNotNullOfOrNull { it?.trim()?.takeIf(String::isNotEmpty) }
                ?: return@mapNotNull null
            AppShortcut(id = entry.id, packageName = app.packageName, label = label, profileSerial = app.profileSerial)
        }
        .take(MAX_IN_MENU)
}
