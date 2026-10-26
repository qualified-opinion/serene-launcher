package app.serene.launcher.data

import androidx.datastore.preferences.core.edit
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_CUSTOM_APP_LABELS
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_HIDDEN_APP_IDS
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_PINNED_APP_IDS
import app.serene.launcher.testing.InMemoryPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HiddenAndRenamedAppsTest {

    @Test
    fun `settings saved before hiding and renaming existed read as nothing hidden or renamed`() = runTest {
        val store = InMemoryPreferencesDataStore()
        store.edit { it[KEY_PINNED_APP_IDS] = setOf("pkg/Main") }

        val prefs = LauncherPreferencesRepository(store).preferencesFlow.first()

        assertTrue(prefs.hiddenAppIds.isEmpty())
        assertTrue(prefs.customLabels.isEmpty())
        assertEquals(setOf("pkg/Main"), prefs.pinnedAppIds)
    }

    @Test
    fun `hidden ids and names with work-profile and nested-class ids survive a reload`() = runTest {
        val store = InMemoryPreferencesDataStore()
        val repo = LauncherPreferencesRepository(store)
        val workId = "com.example/com.example.Main\$Alias@10"

        repo.setHidden(workId, hidden = true)
        repo.setCustomLabel(workId, "Work chat")
        repo.setCustomLabel("pkg/Main", "Line\nbreak\ttab")

        val prefs = LauncherPreferencesRepository(store).preferencesFlow.first()
        assertEquals(setOf(workId), prefs.hiddenAppIds)
        assertEquals(mapOf(workId to "Work chat", "pkg/Main" to "Line break tab"), prefs.customLabels)
    }

    @Test
    fun `a blank name removes the custom name and the key once none are left`() = runTest {
        val store = InMemoryPreferencesDataStore()
        val repo = LauncherPreferencesRepository(store)

        repo.setCustomLabel("pkg/Main", "Mine")
        repo.setCustomLabel("pkg/Main", "   ")

        assertNull(store.data.first()[KEY_CUSTOM_APP_LABELS])
    }

    @Test
    fun `uninstalling an app clears its hidden flag and name only in that profile`() = runTest {
        val store = InMemoryPreferencesDataStore()
        val repo = LauncherPreferencesRepository(store)
        repo.setHidden("pkg/Main", hidden = true)
        repo.setHidden("pkg/Main@10", hidden = true)
        repo.setHidden("pkgx/Main", hidden = true)
        repo.setCustomLabel("pkg/Main", "Personal")
        repo.setCustomLabel("pkg/Main@10", "Work")

        repo.prunePins("pkg", null, emptyList())

        val raw = store.data.first()
        assertEquals(setOf("pkg/Main@10", "pkgx/Main"), raw[KEY_HIDDEN_APP_IDS])
        assertEquals(
            mapOf("pkg/Main@10" to "Work"),
            LauncherPreferencesRepository.decodeLabels(raw[KEY_CUSTOM_APP_LABELS])
        )
    }

    @Test
    fun `names are trimmed and control characters become spaces`() {
        assertEquals("A b", LauncherPreferencesRepository.cleanLabel("  A\u0000b \n"))
    }
}
