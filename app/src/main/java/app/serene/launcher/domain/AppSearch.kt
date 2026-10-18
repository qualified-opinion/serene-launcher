package app.serene.launcher.domain

import app.serene.launcher.model.AppItem
import java.util.Locale

/** Pure search and indexing logic for the app drawer, kept free of Android types so it is easy to test. */
object AppSearch {

    /**
     * Returns the apps whose label contains [query], ignoring case, with labels that start with
     * the query listed before labels that only contain it. A blank query returns [apps] unchanged.
     */
    fun filter(apps: List<AppItem>, query: String): List<AppItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return apps
        val lowerQuery = trimmed.lowercase(Locale.getDefault())

        val (prefixMatches, containsMatches) = apps
            .filter { it.label.lowercase(Locale.getDefault()).contains(lowerQuery) }
            .partition { it.label.lowercase(Locale.getDefault()).startsWith(lowerQuery) }

        return prefixMatches + containsMatches
    }

    /** Maps each section letter to the index of the first app in [apps] that belongs to it. */
    fun alphabetIndex(apps: List<AppItem>): Map<Char, Int> {
        val map = mutableMapOf<Char, Int>()
        apps.forEachIndexed { index, app ->
            map.putIfAbsent(app.firstLetterSection, index)
        }
        return map
    }
}
