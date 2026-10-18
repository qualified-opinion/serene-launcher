package app.serene.launcher.domain

import app.serene.launcher.testing.app
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AppSearchTest {

    private val apps = listOf(app("2048"), app("Calendar"), app("Camera"), app("Google Maps"), app("Maps"))

    @Test
    fun `blank query returns every app in order`() {
        assertEquals(apps, AppSearch.filter(apps, "  "))
    }

    @Test
    fun `matching ignores case and surrounding spaces`() {
        assertEquals(listOf("Calendar", "Camera"), AppSearch.filter(apps, " CA ").map { it.label })
    }

    @Test
    fun `labels starting with the query come before labels that only contain it`() {
        assertEquals(listOf("Maps", "Google Maps"), AppSearch.filter(apps, "map").map { it.label })
    }

    @Test
    fun `no match returns an empty list`() {
        assertEquals(emptyList<String>(), AppSearch.filter(apps, "zzz").map { it.label })
    }

    @Test
    fun `alphabet index points at the first app of each section`() {
        assertEquals(mapOf('#' to 0, 'C' to 1, 'G' to 3, 'M' to 4), AppSearch.alphabetIndex(apps))
    }

    @Test
    fun `matching ignores accents`() {
        val withAccents = listOf(app("Éclair"), app("Café Finder"))

        assertEquals(listOf("Éclair"), AppSearch.filter(withAccents, "ecl").map { it.label })
        assertEquals(listOf("Café Finder"), AppSearch.filter(withAccents, "CAFE").map { it.label })
    }

    @Test
    fun `initials find multi-word apps`() {
        assertEquals(listOf("Google Maps"), AppSearch.filter(apps, "gm").map { it.label })
    }

    @Test
    fun `results rank prefix, then word start, then initials, then contains`() {
        val candidates = listOf(app("Google Maps"), app("Gmail"), app("My Gmail Helper"), app("Pygmy"))

        assertEquals(
            listOf("Gmail", "My Gmail Helper", "Google Maps", "Pygmy"),
            AppSearch.filter(candidates, "gm").map { it.label }
        )
    }

    @Test
    fun `only labels starting with the query count as prefix matches`() {
        assertEquals(true, AppSearch.isPrefixMatch("Maps", "ma"))
        assertEquals(false, AppSearch.isPrefixMatch("Google Maps", "ma"))
        assertEquals(false, AppSearch.isPrefixMatch("Maps", " "))
    }

    @Test
    fun `sorting follows the language and keeps one block of other apps first`() {
        val unsorted = listOf(app("Zoom"), app("Éclair"), app("eBay"), app("2048"), app("Карты"))

        val sorted = AppSearch.sort(unsorted, Locale.ENGLISH).map { it.label }

        assertEquals(setOf("2048", "Карты"), sorted.take(2).toSet())
        assertEquals(listOf("eBay", "Éclair", "Zoom"), sorted.drop(2))
    }

    @Test
    fun `accented letters share their base letter's section`() {
        assertEquals('E', AppSearch.sectionOf("Éclair"))
        assertEquals('#', AppSearch.sectionOf("Карты"))
        assertEquals('#', AppSearch.sectionOf("2048"))
        assertEquals('#', AppSearch.sectionOf("   "))
    }

    @Test
    fun `the rail offers the other section first`() {
        assertEquals('#', AppSearch.railLetters.first())
        assertEquals(27, AppSearch.railLetters.size)
    }
}
