package app.serene.launcher.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_PINNED_APP_IDS
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_PINNED_APP_ORDER
import app.serene.launcher.testing.InMemoryPreferencesDataStore
import app.serene.launcher.testing.app
import app.serene.launcher.testing.temporaryPreferences
import app.serene.launcher.testing.temporaryPreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OrderedPinsTest {
    @get:Rule val folder = TemporaryFolder()
    private val apps = listOf(app("Zulu", "a/Main"), app("Alpha", "z/Main"))

    @Test
    fun `legacy set reads in label order with missing ids last and writes nothing`() = runTest {
        val store = InMemoryPreferencesDataStore()
        store.edit { it[KEY_PINNED_APP_IDS] = setOf("missing/z", apps[0].id, "missing/a", apps[1].id) }
        val readOnly = object : DataStore<Preferences> by store {
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                error("Reading must not write")
        }
        val prefs = LauncherPreferencesRepository(readOnly).preferencesFlow(flowOf(apps)).first()
        assertEquals(listOf(apps[1].id, apps[0].id, "missing/a", "missing/z"), prefs.pinnedAppOrder)
        LauncherPreferencesRepository(store).setPinState(apps[0].id, true, apps)
        assertNull(store.data.first()[KEY_PINNED_APP_ORDER])
    }

    @Test
    fun `a move persists across reopening and preserves work and nested activity ids`() = runTest {
        val file = folder.newFile("pins.preferences_pb")
        val job = Job(backgroundScope.coroutineContext[Job])
        val repo = LauncherPreferencesRepository(temporaryPreferencesStore(file, CoroutineScope(backgroundScope.coroutineContext + job)))
        val ids = listOf("pkg/Main\$Nested@12", "pkg/Main\$Nested")
        val installed = ids.map { app(it, it) }
        try {
            repo.initializeDefaultPinsIfNeeded(ids.toSet())
            assertEquals(ids, repo.preferencesFlow.first().pinnedAppOrder)
            repo.movePin(ids.first(), -1, installed) // Boundary moves do nothing.
            repo.movePin(ids.last(), 1, installed)
            assertEquals(ids, repo.preferencesFlow.first().pinnedAppOrder)
            repo.movePin(ids.first(), 1, installed)
        } finally {
            job.cancelAndJoin()
        }
        val restored = temporaryPreferences(file).preferencesFlow.first()
        assertEquals(ids.reversed(), restored.pinnedAppOrder)
        assertEquals(ids.toSet(), restored.pinnedAppIds)
    }

    @Test
    fun `pin appends and unpin removes only one while both keys stay in step`() = runTest {
        val store = InMemoryPreferencesDataStore()
        store.edit { it[KEY_PINNED_APP_IDS] = apps.map { app -> app.id }.toSet() }
        val repo = LauncherPreferencesRepository(store)
        assertTrue(repo.togglePinApp("new/Main", apps))
        assertEquals(listOf(apps[1].id, apps[0].id, "new/Main"), repo.preferencesFlow.first().pinnedAppOrder)
        assertEquals(store.data.first()[KEY_PINNED_APP_IDS], repo.preferencesFlow.first().pinnedAppOrder.toSet())
        repo.setPinState(apps[1].id, false)
        assertEquals(listOf(apps[0].id, "new/Main"), repo.preferencesFlow.first().pinnedAppOrder)
        assertEquals(store.data.first()[KEY_PINNED_APP_IDS], repo.preferencesFlow.first().pinnedAppOrder.toSet())
    }

    @Test
    fun `older build set wins mismatches and extra ids append without a read write`() = runTest {
        val store = InMemoryPreferencesDataStore()
        store.edit {
            it[KEY_PINNED_APP_IDS] = setOf("b/Main", "c/Main", "a/Main")
            it[KEY_PINNED_APP_ORDER] = "c/Main\nremoved/Main\nc/Main"
        }
        assertEquals(listOf("c/Main", "a/Main", "b/Main"), LauncherPreferencesRepository(store).preferencesFlow.first().pinnedAppOrder)
        assertEquals("c/Main\nremoved/Main\nc/Main", store.data.first()[KEY_PINNED_APP_ORDER])
    }
}
