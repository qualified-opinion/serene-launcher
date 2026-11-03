package app.serene.launcher.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class ThemeTest {

    private fun contrast(a: Color, b: Color): Float {
        val la = a.luminance()
        val lb = b.luminance()
        return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
    }

    @Test
    fun `with both wallpaper options off the colours are exactly as before`() {
        val light = launcherColorScheme(darkTheme = false)
        assertEquals(WarmPaperBackground, light.background)
        assertEquals(WarmSlateMuted, light.onSurfaceVariant)
        assertEquals(TerracottaAccent, light.primary)

        val dark = launcherColorScheme(darkTheme = true)
        assertEquals(OledDarkBackground, dark.background)
        assertEquals(WarmGrayMuted, dark.onSurfaceVariant)
        assertEquals(CoralClayAccent, dark.primary)
    }

    @Test
    fun `show wallpaper makes the background see-through and keeps the accent`() {
        val scheme = launcherColorScheme(darkTheme = false, showWallpaper = true)

        assertEquals(WALLPAPER_SCRIM_ALPHA, scheme.background.alpha, 0.001f)
        assertEquals(WarmPaperBackground.copy(alpha = WALLPAPER_SCRIM_ALPHA), scheme.background)
        assertEquals(TerracottaAccent, scheme.primary)
    }

    @Test
    fun `wallpaper colours replace Serene's own when given`() {
        val fromWallpaper = lightColorScheme(primary = Color(0xFF3366CC), background = Color(0xFFF0F4FF))

        val scheme = launcherColorScheme(darkTheme = false, wallpaperColours = fromWallpaper)

        assertEquals(Color(0xFF3366CC), scheme.primary)
        assertEquals(Color(0xFFF0F4FF), scheme.background)
    }

    @Test
    fun `text stays readable over any wallpaper from black to white`() {
        for (dark in listOf(false, true)) {
            val scheme = launcherColorScheme(darkTheme = dark, showWallpaper = true)
            for (level in 0..255 step 5) {
                val wallpaper = Color(red = level / 255f, green = level / 255f, blue = level / 255f)
                val behindText = scheme.background.compositeOver(wallpaper)
                val main = contrast(scheme.onBackground, behindText)
                val grey = contrast(scheme.onSurfaceVariant, behindText)
                assertTrue("dark=$dark wallpaper=$level main text contrast $main", main >= 4.5f)
                assertTrue("dark=$dark wallpaper=$level grey text contrast $grey", grey >= 4.5f)
            }
        }
    }
}
