package app.serene.launcher.domain

import app.serene.launcher.model.AppItem

/**
 * Picks the apps pinned to Home the first time Serene runs. Kept free of Android types for tests.
 *
 * Each [Role] is matched first by the package Android names for it (the phone's dialer, SMS app,
 * camera and so on), which works in any language, then by its English name, as before.
 */
object DefaultPins {

    /** One kind of everyday app, with the English name used when Android doesn't name a package. */
    data class Role(val englishName: String, val packageName: String? = null)

    /** At most this many apps are pinned. */
    const val MAX_PINS = 5

    /** With no role matched, this many apps from the top of the list are pinned instead. */
    const val FALLBACK_PINS = 4

    fun choose(apps: List<AppItem>, roles: List<Role>): Set<String> {
        val personalApps = apps.filter { it.isPersonalProfile }
        val selected = linkedSetOf<String>()
        for (role in roles) {
            if (selected.size >= MAX_PINS) break
            val match = role.packageName?.let { pkg -> personalApps.firstOrNull { it.packageName == pkg && it.id !in selected } }
                ?: personalApps.firstOrNull { it.label.equals(role.englishName, ignoreCase = true) && it.id !in selected }
                ?: personalApps.firstOrNull { it.label.contains(role.englishName, ignoreCase = true) && it.id !in selected }
            if (match != null) selected.add(match.id)
        }
        if (selected.isEmpty()) {
            personalApps.take(FALLBACK_PINS).forEach { selected.add(it.id) }
        }
        return selected
    }
}
