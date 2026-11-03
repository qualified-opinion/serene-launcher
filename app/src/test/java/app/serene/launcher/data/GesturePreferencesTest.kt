package app.serene.launcher.data

import androidx.datastore.preferences.core.edit
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_DOUBLE_TAP_LOCK
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_PINNED_APP_IDS
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_SHOW_APP_ICONS
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_SWIPE_DOWN_NOTIFICATIONS
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_SWIPE_LEFT_APP
import app.serene.launcher.data.LauncherPreferencesRepository.Companion.KEY_SWIPE_RIGHT_APP
import app.serene.launcher.model.SwipeSide
import app.serene.launcher.testing.InMemoryPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        assertNull(prefs.swipeLeftAppId)
        assertNull(prefs.swipeRightAppId)
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

    @Test
    fun `the swipe apps are saved and cleared`() = runTest {
        val store = InMemoryPreferencesDataStore()
        val repo = LauncherPreferencesRepository(store)

        repo.setSwipeApp(SwipeSide.LEFT, "pkg.mail/Main")
        repo.setSwipeApp(SwipeSide.RIGHT, "pkg.maps/Main@10")
        var prefs = repo.preferencesFlow.first()
        assertEquals("pkg.mail/Main", prefs.swipeLeftAppId)
        assertEquals("pkg.maps/Main@10", prefs.swipeRightAppId)

        repo.setSwipeApp(SwipeSide.LEFT, null)
        prefs = repo.preferencesFlow.first()
        assertNull(prefs.swipeLeftAppId)
        assertNull(store.data.first()[KEY_SWIPE_LEFT_APP])
        assertEquals("pkg.maps/Main@10", prefs.swipeRightAppId)
    }

    @Test
    fun `uninstalling an app clears only the swipe that opened that copy of it`() = runTest {
        val store = InMemoryPreferencesDataStore()
        store.edit {
            it[KEY_SWIPE_LEFT_APP] = "pkg.mail/Main"
            it[KEY_SWIPE_RIGHT_APP] = "pkg.mail/Main@10"
        }
        val repo = LauncherPreferencesRepository(store)

        repo.prunePins("pkg.mail", profileSerial = null, apps = emptyList())

        val raw = store.data.first()
        assertNull(raw[KEY_SWIPE_LEFT_APP])
        assertEquals("pkg.mail/Main@10", raw[KEY_SWIPE_RIGHT_APP])
    }

    @Test
    fun `a swipe to a work app follows it to a new phone's work profile`() = runTest {
        val store = InMemoryPreferencesDataStore()
        store.edit {
            it[KEY_SWIPE_LEFT_APP] = "pkg.mail/Main@10"
            it[KEY_SWIPE_RIGHT_APP] = "pkg.maps/Main"
        }
        var offered = emptySet<String>()

        LauncherPreferencesRepository(store).replaceAppIds { stored ->
            offered = stored
            mapOf("pkg.mail/Main@10" to "pkg.mail/Main@12")
        }

        assertTrue("pkg.mail/Main@10" in offered)
        val raw = store.data.first()
        assertEquals("pkg.mail/Main@12", raw[KEY_SWIPE_LEFT_APP])
        assertEquals("pkg.maps/Main", raw[KEY_SWIPE_RIGHT_APP])
    }
}
