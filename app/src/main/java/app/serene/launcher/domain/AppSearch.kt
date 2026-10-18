package app.serene.launcher.domain

import app.serene.launcher.model.AppItem
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/** Pure search, sorting and indexing logic for the app drawer, kept free of Android types so it is easy to test. */
object AppSearch {

    /** Section for labels that don't start with a Latin letter (digits, symbols, other scripts). */
    const val OTHER_SECTION = '#'

    /** Letters shown on the drawer's index rail, with [OTHER_SECTION] first to match the list order. */
    val railLetters: List<Char> = listOf(OTHER_SECTION) + ('A'..'Z')

    private val combiningMarks = Regex("\\p{Mn}+")

    /** Lowercases [text] and strips accents, so "Éclair" and "eclair" compare equal. */
    fun normalize(text: String): String =
        Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .lowercase(Locale.ROOT)

    /** The index section for [label]: its first letter without accents, or [OTHER_SECTION]. */
    fun sectionOf(label: String): Char {
        val first = normalize(label).firstOrNull()?.uppercaseChar() ?: return OTHER_SECTION
        return if (first in 'A'..'Z') first else OTHER_SECTION
    }

    /**
     * Sorts [apps] for the drawer: all [OTHER_SECTION] apps first in one block, then the rest in
     * the order the user's language expects (so "Éclair" sits with the E's, not after "Z").
     */
    fun sort(apps: List<AppItem>, locale: Locale = Locale.getDefault()): List<AppItem> {
        val collator = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }
        return apps.sortedWith(
            compareBy<AppItem> { sectionOf(it.label) != OTHER_SECTION }
                .thenComparator { a, b -> collator.compare(a.label, b.label) }
        )
    }

    /**
     * Returns the apps matching [query], ignoring case and accents, best matches first:
     * label starts with the query, then a later word starts with it, then the query spells the
     * words' initials ("gm" for Google Maps), then the label merely contains it. Apps keep their
     * existing order within each group. A blank query returns [apps] unchanged.
     */
    fun filter(apps: List<AppItem>, query: String): List<AppItem> {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty()) return apps

        return apps
            .mapNotNull { app -> matchRank(app.label, normalizedQuery)?.let { app to it } }
            .sortedBy { (_, rank) -> rank }
            .map { (app, _) -> app }
    }

    /** True when [label] starts with [query], the only kind of match allowed to open an app by itself. */
    fun isPrefixMatch(label: String, query: String): Boolean {
        val normalizedQuery = normalize(query)
        return normalizedQuery.isNotEmpty() && normalize(label).startsWith(normalizedQuery)
    }

    private fun matchRank(label: String, normalizedQuery: String): Int? {
        val normalizedLabel = normalize(label)
        val words = normalizedLabel.split(wordSeparators).filter { it.isNotEmpty() }
        return when {
            normalizedLabel.startsWith(normalizedQuery) -> 0
            words.drop(1).any { it.startsWith(normalizedQuery) } -> 1
            normalizedQuery.length >= 2 &&
                words.joinToString("") { it.take(1) }.startsWith(normalizedQuery) -> 2
            normalizedLabel.contains(normalizedQuery) -> 3
            else -> null
        }
    }

    private val wordSeparators = Regex("[\\s\\-_.:/&+]+")

    /** Maps each section letter to the index of the first app in [apps] that belongs to it. */
    fun alphabetIndex(apps: List<AppItem>): Map<Char, Int> {
        val map = mutableMapOf<Char, Int>()
        apps.forEachIndexed { index, app ->
            map.putIfAbsent(app.firstLetterSection, index)
        }
        return map
    }
}
