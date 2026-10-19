package app.serene.launcher.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `a fresh install follows the system`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored(stored = null, legacyDark = null))
    }

    @Test
    fun `the old dark switch carries over`() {
        assertEquals(ThemeMode.DARK, ThemeMode.fromStored(stored = null, legacyDark = true))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStored(stored = null, legacyDark = false))
    }

    @Test
    fun `a saved choice wins over the old switch`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored(stored = "SYSTEM", legacyDark = true))
    }

    @Test
    fun `an unknown saved value falls back safely`() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStored(stored = "SEPIA", legacyDark = false))
    }
}
