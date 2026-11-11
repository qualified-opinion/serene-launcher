package app.serene.launcher.domain

import app.serene.launcher.domain.DefaultPins.Role
import app.serene.launcher.model.ProfileKind
import app.serene.launcher.testing.app
import app.serene.launcher.testing.inProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultPinsTest {

    @Test
    fun `apps Android names for a role are found in any language`() {
        // A phone in German: no label matches the English names.
        val dialer = app("Telefon", id = "com.vendor.dialer/Main")
        val sms = app("Nachrichten", id = "com.vendor.sms/Main")
        val camera = app("Kamera", id = "com.vendor.camera/Main")
        val other = app("Rechner", id = "com.vendor.calc/Main")

        val pins = DefaultPins.choose(
            listOf(camera, sms, other, dialer),
            listOf(
                Role("Phone", "com.vendor.dialer"),
                Role("Messages", "com.vendor.sms"),
                Role("Camera", "com.vendor.camera")
            )
        )

        assertEquals(listOf(dialer.id, sms.id, camera.id), pins.toList())
    }

    @Test
    fun `without a package the English name is used as before`() {
        val notes = app("Notes")
        val phone = app("Phone")

        val pins = DefaultPins.choose(listOf(notes, phone), listOf(Role("Phone", null), Role("Notes")))

        assertEquals(listOf(phone.id, notes.id), pins.toList())
    }

    @Test
    fun `a package that isn't in the list falls back to the name`() {
        val camera = app("Camera")

        val pins = DefaultPins.choose(listOf(camera), listOf(Role("Camera", "com.gone.camera")))

        assertEquals(setOf(camera.id), pins)
    }

    @Test
    fun `work apps are never pinned by default`() {
        val dialer = app("Phone", id = "com.vendor.dialer/Main")
        val workDialer = inProfile(dialer, 10, ProfileKind.WORK)

        val pins = DefaultPins.choose(listOf(workDialer, dialer), listOf(Role("Phone", "com.vendor.dialer")))

        assertEquals(setOf(dialer.id), pins)
    }

    @Test
    fun `at most five apps are pinned and one app only once`() {
        val apps = listOf("Phone", "Messages", "Camera", "Settings", "Clock", "Calendar").map { app(it) }
        val roles = apps.map { Role(it.label, it.packageName) } + Role("Phone", apps.first().packageName)

        val pins = DefaultPins.choose(apps, roles)

        assertEquals(apps.take(5).map { it.id }, pins.toList())
    }

    @Test
    fun `with nothing matched the first four apps are pinned`() {
        val apps = listOf("Alpha", "Bravo", "Charlie", "Delta", "Echo").map { app(it) }

        val pins = DefaultPins.choose(apps, listOf(Role("Phone", "com.none")))

        assertEquals(apps.take(4).map { it.id }.toSet(), pins)
    }
}
