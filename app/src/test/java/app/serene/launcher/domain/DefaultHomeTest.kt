package app.serene.launcher.domain

import app.serene.launcher.domain.DefaultHome.Action
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultHomeTest {

    private val ours = "app.serene.launcher"

    @Test
    fun `android 10 and later trusts the home role, not the resolved package`() {
        assertTrue(
            DefaultHome.isDefault(
                sdkInt = DefaultHome.ROLE_SDK,
                roleHeld = true,
                resolvedHomePackage = "other.launcher",
                ownPackageName = ours
            )
        )
        assertFalse(DefaultHome.isDefault(sdkInt = 36, roleHeld = false, resolvedHomePackage = ours, ownPackageName = ours))
    }

    @Test
    fun `before android 10 the resolved home package has to be ours`() {
        assertTrue(DefaultHome.isDefault(sdkInt = 28, roleHeld = false, resolvedHomePackage = ours, ownPackageName = ours))
        assertFalse(DefaultHome.isDefault(sdkInt = 28, roleHeld = true, resolvedHomePackage = "other.launcher", ownPackageName = ours))
        assertFalse(DefaultHome.isDefault(sdkInt = 24, roleHeld = false, resolvedHomePackage = null, ownPackageName = ours))
        assertFalse(DefaultHome.isDefault(sdkInt = 28, roleHeld = false, resolvedHomePackage = "app.serene.launcher.other", ownPackageName = ours))
    }

    @Test
    fun `android 10 asks with the home role when it is available and not held`() {
        assertEquals(
            Action.REQUEST_ROLE,
            DefaultHome.chooseAction(sdkInt = DefaultHome.ROLE_SDK, roleAvailable = true, roleHeld = false)
        )
        assertEquals(
            Action.REQUEST_ROLE,
            DefaultHome.chooseAction(sdkInt = 36, roleAvailable = true, roleHeld = false, homeSettingsAvailable = false)
        )
    }

    @Test
    fun `no role dialog when the role is missing, already held, or the request was refused`() {
        assertEquals(
            Action.OPEN_HOME_SETTINGS,
            DefaultHome.chooseAction(sdkInt = 29, roleAvailable = false, roleHeld = false)
        )
        assertEquals(
            Action.OPEN_HOME_SETTINGS,
            DefaultHome.chooseAction(sdkInt = 29, roleAvailable = true, roleHeld = true)
        )
        assertEquals(
            Action.OPEN_HOME_SETTINGS,
            DefaultHome.chooseAction(sdkInt = 29, roleAvailable = true, roleHeld = false, roleRequestNotGranted = true)
        )
    }

    @Test
    fun `before android 10 the role flags are ignored`() {
        assertEquals(
            Action.OPEN_HOME_SETTINGS,
            DefaultHome.chooseAction(sdkInt = 28, roleAvailable = true, roleHeld = false)
        )
    }

    @Test
    fun `home settings that can't be opened fall through to the main settings app`() {
        assertEquals(
            Action.OPEN_SETTINGS,
            DefaultHome.chooseAction(
                sdkInt = 28,
                roleAvailable = false,
                roleHeld = false,
                homeSettingsAvailable = false
            )
        )
        assertEquals(
            Action.OPEN_SETTINGS,
            DefaultHome.chooseAction(
                sdkInt = 29,
                roleAvailable = true,
                roleHeld = false,
                roleRequestNotGranted = true,
                homeSettingsAvailable = false
            )
        )
    }
}
