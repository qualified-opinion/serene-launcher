package app.serene.launcher.data

import androidx.datastore.preferences.core.edit
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_CUSTOM_APP_LABELS
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_HAS_INITIALIZED_DEFAULT_PINS
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_HIDDEN_APP_IDS
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_PINNED_APP_IDS
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_PINNED_APP_ORDER
import app.serene.launcher.testing.InMemoryPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Moving saved work-profile ids to a new profile serial (N-16), starting from settings as saved today. */
class ReplaceAppIdsTest {

    private val moves = mapOf("mail/Main@10" to "mail/Main@12", "maps/Main@10" to "maps/Main@12")

    @Test
    fun `saved pins, hidden apps and names move to the new ids and keep their place`() = runTest {
        val store = InMemoryPreferencesDataStore()
        store.edit {
            it[KEY_PINNED_APP_ORDER] = "phone/Main\nmail/Main@10\ncamera/Main\nmaps/Main@10"
            it[KEY_PINNED_APP_IDS] = setOf("phone/Main", "mail/Main@10", "camera/Main", "maps/Main@10")
            it[KEY_HAS_INITIALIZED_DEFAULT_PINS] = true
            it[KEY_HIDDEN_APP_IDS] = setOf("maps/Main@10", "games/Main")
            it[KEY_CUSTOM_APP_LABELS] = "mail/Main@10\tWork mail\ncamera/Main\tPhotos"
        }
        var offered = emptySet<String>()

        LauncherPreferencesRepository(store).replaceAppIds { stored -> offered = stored; moves }

        assertEquals(
            setOf("phone/Main", "mail/Main@10", "camera/Main", "maps/Main@10", "games/Main"),
            offered
        )
        val raw = store.data.first()
        assertEquals("phone/Main\nmail/Main@12\ncamera/Main\nmaps/Main@12", raw[KEY_PINNED_APP_ORDER])
        assertEquals(setOf("phone/Main", "mail/Main@12", "camera/Main", "maps/Main@12"), raw[KEY_PINNED_APP_IDS])
        assertEquals(true, raw[KEY_HAS_INITIALIZED_DEFAULT_PINS])
        assertEquals(setOf("maps/Main@12", "games/Main"), raw[KEY_HIDDEN_APP_IDS])
        assertEquals(
            mapOf("mail/Main@12" to "Work mail", "camera/Main" to "Photos"),
            LauncherPreferencesRepository.decodeLabels(raw[KEY_CUSTOM_APP_LABELS])
        )
    }

    @Test
    fun `old installs with no saved pin order only have their pin set updated`() = runTest {
        val store = InMemoryPreferencesDataStore()
        store.edit { it[KEY_PINNED_APP_IDS] = setOf("phone/Main", "mail/Main@10") }

        LauncherPreferencesRepository(store).replaceAppIds { moves }

        val raw = store.data.first()
        assertEquals(setOf("phone/Main", "mail/Main@12"), raw[KEY_PINNED_APP_IDS])
        assertNull(raw[KEY_PINNED_APP_ORDER])
        assertEquals(setOf(KEY_PINNED_APP_IDS), raw.asMap().keys)
    }

    @Test
    fun `an app already saved under its new id is kept once, with its own name`() = runTest {
        val store = InMemoryPreferencesDataStore()
        store.edit {
            it[KEY_PINNED_APP_ORDER] = "mail/Main@12\nphone/Main\nmail/Main@10"
            it[KEY_PINNED_APP_IDS] = setOf("mail/Main@12", "phone/Main", "mail/Main@10")
            it[KEY_CUSTOM_APP_LABELS] = "mail/Main@10\tOld name\nmail/Main@12\tNew name"
        }

        LauncherPreferencesRepository(store).replaceAppIds { moves }

        val raw = store.data.first()
        assertEquals("mail/Main@12\nphone/Main", raw[KEY_PINNED_APP_ORDER])
        assertEquals(setOf("mail/Main@12", "phone/Main"), raw[KEY_PINNED_APP_IDS])
        assertEquals(
            mapOf("mail/Main@12" to "New name"),
            LauncherPreferencesRepository.decodeLabels(raw[KEY_CUSTOM_APP_LABELS])
        )
    }

    @Test
    fun `no replacements leaves the settings untouched`() = runTest {
        val store = InMemoryPreferencesDataStore()
        store.edit { it[KEY_PINNED_APP_IDS] = setOf("mail/Main@10") }
        val before = store.data.first()

        LauncherPreferencesRepository(store).replaceAppIds { emptyMap() }

        assertEquals(before, store.data.first())
        assertTrue(store.data.first()[KEY_PINNED_APP_ORDER] == null)
    }
}
