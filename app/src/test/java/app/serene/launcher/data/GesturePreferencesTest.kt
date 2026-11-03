package app.serene.launcher.data

import androidx.datastore.preferences.core.edit
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_DOUBLE_TAP_LOCK
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_PINNED_APP_IDS
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_SHOW_APP_ICONS
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_SWIPE_DOWN_NOTIFICATIONS
import app.serene.launcher.testing.InMemoryPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The gesture settings are new keys, so settings saved before them must read as the defaults. */
class GesturePreferencesTest {

    @Test
    fun `settings saved before gestures existed get swipe down on and double-tap off`() = runTest {
        val store = InMemoryPreferencesDataStore()
        store.edit {
            it[KEY_PINNED_APP_IDS] = setOf("pkg.camera/Main")
            it[KEY_SHOW_APP_ICONS] = true
        }

        val prefs = LauncherPreferencesRepository(store).preferencesFlow.first()

        assertTrue(prefs.swipeDownForNotifications)
        assertFalse(prefs.doubleTapToLock)
        // Reading adds nothing, and the other settings are untouched.
        val raw = store.data.first()
        assertEquals(setOf(KEY_PINNED_APP_IDS, KEY_SHOW_APP_ICONS), raw.asMap().keys)
        assertTrue(prefs.showAppIcons)
        assertEquals(setOf("pkg.camera/Main"), prefs.pinnedAppIds)
    }

    @Test
    fun `turning gestures on and off is saved`() = runTest {
        val store = InMemoryPreferencesDataStore()
        val repo = LauncherPreferencesRepository(store)

        repo.setSwipeDownForNotifications(false)
        repo.setDoubleTapToLock(true)

        val prefs = repo.preferencesFlow.first()
        assertFalse(prefs.swipeDownForNotifications)
        assertTrue(prefs.doubleTapToLock)
        assertEquals(false, store.data.first()[KEY_SWIPE_DOWN_NOTIFICATIONS])
        assertEquals(true, store.data.first()[KEY_DOUBLE_TAP_LOCK])
    }
}
