package app.serene.launcher.data

import androidx.datastore.preferences.core.edit
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_PINNED_APP_IDS
import app.serene.launcher.model.AppItem
import app.serene.launcher.model.ProfileKind
import app.serene.launcher.testing.InMemoryPreferencesDataStore
import app.serene.launcher.testing.app
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkProfileSettingsTest {

    private fun workApp(label: String, serial: Long, kind: ProfileKind = ProfileKind.WORK): AppItem =
        app(label, "work.${label.lowercase()}/Main@$serial")
            .copy(profileSerial = serial, profileKind = kind)

    private val maps = app("Maps")
    private val mail = workApp("Mail", serial = 12)
    private val chat = workApp("Chat", serial = 12)
    private val oldMail = "work.mail/Main@5"
    private val oldChat = "work.chat/Main@5"

    private suspend fun repoWithOldWorkSettings(): LauncherPreferencesRepository {
        val repo = LauncherPreferencesRepository(InMemoryPreferencesDataStore())
        listOf(oldMail, maps.id, oldChat).forEach { repo.setPinState(it, pinned = true) }
        repo.setHidden(oldChat, hidden = true)
        repo.setCustomLabel(oldMail, "Office mail")
        return repo
    }

    @Test
    fun `settings from a work profile that is gone move to the new work profile in the same order`() = runTest {
        val repo = repoWithOldWorkSettings()

        repo.adoptSettingsFromGoneProfiles(listOf(maps, mail, chat), liveSerials = setOf(0, 12))

        val prefs = repo.preferencesFlow.first()
        assertEquals(listOf(mail.id, maps.id, chat.id), prefs.pinnedAppOrder)
        assertEquals(setOf(chat.id), prefs.hiddenAppIds)
        assertEquals(mapOf(mail.id to "Office mail"), prefs.customLabels)
    }

    @Test
    fun `a paused work profile that still exists keeps its own settings`() = runTest {
        val repo = repoWithOldWorkSettings()

        repo.adoptSettingsFromGoneProfiles(listOf(maps, mail, chat), liveSerials = setOf(0, 5, 12))

        assertEquals(listOf(oldMail, maps.id, oldChat), repo.preferencesFlow.first().pinnedAppOrder)
    }

    @Test
    fun `nothing moves when some profiles may be invisible or there are two work profiles`() = runTest {
        val repo = repoWithOldWorkSettings()

        repo.adoptSettingsFromGoneProfiles(listOf(maps, mail, chat), liveSerials = null)
        repo.adoptSettingsFromGoneProfiles(
            listOf(maps, mail, chat, workApp("Mail", serial = 20)),
            liveSerials = setOf(0, 12, 20)
        )

        assertEquals(listOf(oldMail, maps.id, oldChat), repo.preferencesFlow.first().pinnedAppOrder)
    }

    @Test
    fun `settings never move into a private space, and apps missing from the new profile stay saved`() = runTest {
        val repo = repoWithOldWorkSettings()
        val privateMail = workApp("Mail", serial = 30, kind = ProfileKind.PRIVATE)

        repo.adoptSettingsFromGoneProfiles(listOf(maps, privateMail), liveSerials = setOf(0, 30))
        assertEquals(listOf(oldMail, maps.id, oldChat), repo.preferencesFlow.first().pinnedAppOrder)

        repo.adoptSettingsFromGoneProfiles(listOf(maps, mail), liveSerials = setOf(0, 12))
        assertEquals(listOf(mail.id, maps.id, oldChat), repo.preferencesFlow.first().pinnedAppOrder)
    }

    @Test
    fun `pins saved before the pin order existed move too`() = runTest {
        val store = InMemoryPreferencesDataStore()
        store.edit { it[KEY_PINNED_APP_IDS] = setOf(oldMail, maps.id) }
        val repo = LauncherPreferencesRepository(store)

        repo.adoptSettingsFromGoneProfiles(listOf(maps, mail), liveSerials = setOf(0, 12))

        assertEquals(setOf(mail.id, maps.id), repo.preferencesFlow.first().pinnedAppIds)
    }
}
