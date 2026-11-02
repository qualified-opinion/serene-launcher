package app.serene.launcher.domain

import app.serene.launcher.model.AppItem
import app.serene.launcher.model.ProfileKind
import app.serene.launcher.model.ProfileScan
import app.serene.launcher.model.ProfileState
import app.serene.launcher.testing.app
import app.serene.launcher.testing.inProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileAppsTest {

    private val camera = app("Camera")
    private val maps = app("Maps")
    private val workMail = inProfile(app("Mail"), serial = 10)
    private val workMaps = inProfile(maps, serial = 10)

    private fun personal(vararg apps: AppItem) = ProfileScan(0, ProfileKind.PERSONAL, ProfileState.AVAILABLE, apps.toList())
    private fun work(state: ProfileState, vararg apps: AppItem, serial: Long = 10) =
        ProfileScan(serial, ProfileKind.WORK, state, apps.toList())

    @Test
    fun `an available profile shows exactly what the scan found`() {
        val previous = listOf(camera, maps, workMail, workMaps)

        val merged = ProfileApps.merge(previous, listOf(personal(camera), work(ProfileState.AVAILABLE, workMail)))

        assertEquals(listOf(camera.id, workMail.id), merged.map { it.id })
        assertTrue(merged.none { it.isProfilePaused })
    }

    @Test
    fun `a paused work profile keeps its apps marked paused until it is resumed`() {
        val before = ProfileApps.merge(emptyList(), listOf(personal(camera), work(ProfileState.AVAILABLE, workMail, workMaps)))

        val paused = ProfileApps.merge(before, listOf(personal(camera), work(ProfileState.PAUSED)))
        assertEquals(listOf(camera.id, workMail.id, workMaps.id), paused.map { it.id })
        assertEquals(listOf(false, true, true), paused.map { it.isProfilePaused })

        val resumed = ProfileApps.merge(paused, listOf(personal(camera), work(ProfileState.AVAILABLE, workMail, workMaps)))
        assertTrue(resumed.none { it.isProfilePaused })
    }

    @Test
    fun `a locked profile adds the apps it could read to the ones it had`() {
        val previous = listOf(camera, workMail)

        val merged = ProfileApps.merge(previous, listOf(personal(camera), work(ProfileState.LOCKED, workMaps)))

        assertEquals(listOf(camera.id, workMail.id, workMaps.id), merged.map { it.id })
        assertTrue(merged.none { it.isProfilePaused })
    }

    @Test
    fun `a profile Android refused to list is not treated as empty`() {
        val previous = listOf(camera, workMail)

        val merged = ProfileApps.merge(previous, listOf(personal(camera), work(ProfileState.UNREADABLE)))

        assertEquals(listOf(camera.id, workMail.id), merged.map { it.id })
        assertFalse(merged.last().isProfilePaused)
    }

    @Test
    fun `a first scan with the work profile paused shows only what it can read`() {
        val merged = ProfileApps.merge(emptyList(), listOf(personal(camera), work(ProfileState.PAUSED)))

        assertEquals(listOf(camera.id), merged.map { it.id })
    }

    @Test
    fun `private space apps disappear while it is locked`() {
        val privateNotes = inProfile(app("Notes"), serial = 20, kind = ProfileKind.PRIVATE)
        val unlocked = ProfileScan(20, ProfileKind.PRIVATE, ProfileState.AVAILABLE, listOf(privateNotes))
        val before = ProfileApps.merge(emptyList(), listOf(personal(camera), unlocked))
        assertEquals(listOf(camera.id, privateNotes.id), before.map { it.id })
        assertEquals(ProfileKind.PRIVATE, before.last().profileKind)

        for (state in listOf(ProfileState.PAUSED, ProfileState.LOCKED, ProfileState.UNREADABLE)) {
            val locked = ProfileScan(20, ProfileKind.PRIVATE, state, listOf(privateNotes))
            assertEquals(listOf(camera.id), ProfileApps.merge(before, listOf(personal(camera), locked)).map { it.id })
        }
    }

    @Test
    fun `a removed profile takes its apps with it`() {
        val previous = listOf(camera, workMail)

        val merged = ProfileApps.merge(previous, listOf(personal(camera)))

        assertEquals(listOf(camera.id), merged.map { it.id })
    }

    @Test
    fun `apps take the kind of the profile they were scanned in`() {
        val clone = app("Chat", "pkg.chat/Main@30")
        val scan = ProfileScan(30, ProfileKind.CLONE, ProfileState.AVAILABLE, listOf(clone))

        assertEquals(ProfileKind.CLONE, ProfileApps.merge(emptyList(), listOf(scan)).single().profileKind)
    }

    @Test
    fun `the same app in personal and work is one match that opens the personal copy`() {
        assertEquals(maps, ProfileApps.personalCopyOfOneApp(listOf(workMaps, maps)))
        assertEquals(maps, ProfileApps.personalCopyOfOneApp(listOf(maps, workMaps, inProfile(maps, serial = 20))))
    }

    @Test
    fun `different apps, a single app or copies with no personal one are not merged`() {
        assertNull(ProfileApps.personalCopyOfOneApp(listOf(maps)))
        assertNull(ProfileApps.personalCopyOfOneApp(listOf(maps, camera)))
        assertNull(ProfileApps.personalCopyOfOneApp(listOf(maps, workMail)))
        assertNull(ProfileApps.personalCopyOfOneApp(listOf(workMaps, inProfile(maps, serial = 20, kind = ProfileKind.PRIVATE))))
        val otherActivity = maps.copy(id = "pkg.maps/Navigate@10", activityName = "Navigate", profileSerial = 10, profileKind = ProfileKind.WORK)
        assertNull(ProfileApps.personalCopyOfOneApp(listOf(maps, otherActivity)))
    }

    @Test
    fun `ids from a work profile that is gone move to the same app in the new work profile`() {
        val newWork = work(ProfileState.AVAILABLE, inProfile(app("Mail"), serial = 12), inProfile(maps, serial = 12), serial = 12)
        val stored = setOf(camera.id, workMail.id, workMaps.id, "pkg.gone/Main@10")

        val replacements = ProfileApps.staleWorkIdReplacements(stored, listOf(personal(camera), newWork)) { false }

        assertEquals(
            mapOf(workMail.id to "pkg.mail/Main@12", workMaps.id to "pkg.maps/Main@12"),
            replacements
        )
    }

    @Test
    fun `nothing moves while the old profile exists or the new one is not ready`() {
        val stored = setOf(workMail.id)
        val newMail = inProfile(app("Mail"), serial = 12)
        fun replace(scans: List<ProfileScan>, exists: (Long) -> Boolean = { false }) =
            ProfileApps.staleWorkIdReplacements(stored, scans, exists)

        // Android still knows profile 10, for example a hidden profile the scan can't list.
        assertTrue(replace(listOf(personal(camera), work(ProfileState.AVAILABLE, newMail, serial = 12))) { it == 10L }.isEmpty())
        // Profile 10 is still in the scan, as another kind of profile.
        val stillThere = ProfileScan(10, ProfileKind.OTHER, ProfileState.AVAILABLE, emptyList())
        assertTrue(replace(listOf(stillThere, work(ProfileState.AVAILABLE, newMail, serial = 12))).isEmpty())
        // The new work profile is paused, or its scan found nothing yet.
        assertTrue(replace(listOf(work(ProfileState.PAUSED, newMail, serial = 12))).isEmpty())
        assertTrue(replace(listOf(work(ProfileState.AVAILABLE, serial = 12))).isEmpty())
        // Two work profiles: no way to tell which one the old pins belong to.
        val other = inProfile(app("Mail"), serial = 13)
        assertTrue(replace(listOf(work(ProfileState.AVAILABLE, newMail, serial = 12), work(ProfileState.AVAILABLE, other, serial = 13))).isEmpty())
        // The app isn't installed in the new work profile.
        assertTrue(replace(listOf(work(ProfileState.AVAILABLE, inProfile(maps, serial = 12), serial = 12))).isEmpty())
        // No work profile at all.
        assertTrue(replace(listOf(personal(camera))).isEmpty())
    }

    @Test
    fun `personal ids and ids that are not app ids never move`() {
        val newWork = work(ProfileState.AVAILABLE, inProfile(camera, serial = 12), serial = 12)
        val stored = setOf(camera.id, "@10", "pkg.camera/Main@x")

        assertTrue(ProfileApps.staleWorkIdReplacements(stored, listOf(newWork)) { false }.isEmpty())
    }

    @Test
    fun `user types map to profile kinds and old phones count as work`() {
        assertEquals(ProfileKind.WORK, ProfileApps.kindForUserType(null))
        assertEquals(ProfileKind.WORK, ProfileApps.kindForUserType("android.os.usertype.profile.MANAGED"))
        assertEquals(ProfileKind.PRIVATE, ProfileApps.kindForUserType("android.os.usertype.profile.PRIVATE"))
        assertEquals(ProfileKind.CLONE, ProfileApps.kindForUserType("android.os.usertype.profile.CLONE"))
        assertEquals(ProfileKind.OTHER, ProfileApps.kindForUserType("com.example.usertype.profile.CUSTOM"))
    }
}
