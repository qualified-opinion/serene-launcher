package app.serene.launcher.domain

import app.serene.launcher.testing.app
import org.junit.Assert.assertEquals
import org.junit.Test

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
}
