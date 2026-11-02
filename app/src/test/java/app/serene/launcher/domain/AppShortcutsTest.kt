package app.serene.launcher.domain

import app.serene.launcher.model.AppShortcut
import app.serene.launcher.model.ProfileKind
import app.serene.launcher.testing.app
import app.serene.launcher.testing.inProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppShortcutsTest {

    private val messages = app("Messages")

    private fun entry(
        id: String,
        short: String? = id,
        long: String? = null,
        rank: Int = 0,
        enabled: Boolean = true,
        manifest: Boolean = true
    ) = ShortcutEntry(id, short, long, rank, enabled, manifest)

    @Test
    fun `fixed shortcuts come first, each kind in the app's order`() {
        val shortcuts = AppShortcuts.forMenu(
            messages,
            listOf(
                entry("recent-2", rank = 1, manifest = false),
                entry("new", rank = 1),
                entry("recent-1", rank = 0, manifest = false),
                entry("search", rank = 0)
            )
        )

        assertEquals(listOf("search", "new", "recent-1", "recent-2"), shortcuts.map { it.id })
    }

    @Test
    fun `the short name is used, then the long one, and nameless or disabled shortcuts are left out`() {
        val shortcuts = AppShortcuts.forMenu(
            messages,
            listOf(
                entry("a", short = "  New message ", long = "Start a new message"),
                entry("b", short = " ", long = "Mark all read"),
                entry("c", short = null, long = null),
                entry("d", short = "Archived", enabled = false)
            )
        )

        assertEquals(listOf("New message", "Mark all read"), shortcuts.map { it.label })
    }

    @Test
    fun `the menu shows at most four, with no repeats`() {
        val entries = (1..6).map { entry("s$it", rank = it) } + entry("s1", rank = 0)

        val shortcuts = AppShortcuts.forMenu(messages, entries)

        assertEquals(AppShortcuts.MAX_IN_MENU, shortcuts.size)
        assertEquals(listOf("s1", "s2", "s3", "s4"), shortcuts.map { it.id })
    }

    @Test
    fun `a work app's shortcuts open in the work profile`() {
        val workMessages = inProfile(messages, serial = 10, kind = ProfileKind.WORK)

        val shortcut = AppShortcuts.forMenu(workMessages, listOf(entry("new"))).single()

        assertEquals(AppShortcut(id = "new", packageName = "pkg.messages", label = "new", profileSerial = 10), shortcut)
    }

    @Test
    fun `an app with no shortcuts gets an empty menu section`() {
        assertTrue(AppShortcuts.forMenu(messages, emptyList()).isEmpty())
    }
}
