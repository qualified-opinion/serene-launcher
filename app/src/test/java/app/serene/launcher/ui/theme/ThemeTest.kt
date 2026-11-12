package app.serene.launcher.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import app.serene.launcher.model.BackgroundColour
import app.serene.launcher.model.WallpaperDim
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
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
    fun `at the strongest dim text stays readable over any wallpaper from black to white`() {
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

    private val greys = (0..255 step 5).map { Color(red = it / 255f, green = it / 255f, blue = it / 255f) }

    @Test
    fun `the strongest dim is the look from before the setting`() {
        assertEquals(0.75f, WallpaperDim.STRONG.scrimAlpha, 0.001f)
        assertEquals(WALLPAPER_SCRIM_ALPHA, WallpaperDim.STRONG.scrimAlpha, 0.001f)
    }

    @Test
    fun `a lighter dim shows more of the wallpaper`() {
        val alphas = WallpaperDim.entries.map {
            launcherColorScheme(darkTheme = false, showWallpaper = true, wallpaperDim = it).background.alpha
        }
        assertEquals(alphas.sorted(), alphas)
        assertEquals(alphas.size, alphas.distinct().size)
    }

    @Test
    fun `at the medium dim main text keeps 4 to 1 over any wallpaper`() {
        for (dark in listOf(false, true)) {
            val scheme = launcherColorScheme(darkTheme = dark, showWallpaper = true, wallpaperDim = WallpaperDim.MEDIUM)
            for (wallpaper in greys) {
                val main = contrast(scheme.onBackground, scheme.background.compositeOver(wallpaper))
                assertTrue("dark=$dark wallpaper=$wallpaper main text contrast $main", main >= 4f)
            }
        }
    }

    @Test
    fun `text glows only when the wallpaper shows through a lighter dim`() {
        assertFalse(needsTextGlow(showWallpaper = false, wallpaperDim = WallpaperDim.LIGHT))
        assertFalse(needsTextGlow(showWallpaper = true, wallpaperDim = WallpaperDim.STRONG))
        assertTrue(needsTextGlow(showWallpaper = true, wallpaperDim = WallpaperDim.MEDIUM))
        assertTrue(needsTextGlow(showWallpaper = true, wallpaperDim = WallpaperDim.LIGHT))
    }

    @Test
    fun `the glow goes round every text style`() {
        val glowing = Typography.withGlow(WarmPaperBackground)
        listOf(glowing.displayMedium, glowing.bodyLarge, glowing.labelSmall).forEach { style ->
            assertEquals(WarmPaperBackground, style.shadow?.color)
        }
        assertNull(Typography.bodyLarge.shadow)
    }

    @Test
    fun `paper is Serene's own background and the tints change only background and surface`() {
        for (dark in listOf(false, true)) {
            val paper = launcherColorScheme(darkTheme = dark)
            assertEquals(paper, launcherColorScheme(darkTheme = dark, backgroundColour = BackgroundColour.PAPER))
            for (colour in BackgroundColour.entries - BackgroundColour.PAPER) {
                val tinted = launcherColorScheme(darkTheme = dark, backgroundColour = colour)
                assertNotEquals(paper.background, tinted.background)
                assertEquals(backgroundSwatch(colour, dark), tinted.background)
                assertEquals(paper.primary, tinted.primary)
                assertEquals(paper.onBackground, tinted.onBackground)
            }
        }
    }

    @Test
    fun `every background colour keeps text readable on its background and surface`() {
        for (dark in listOf(false, true)) {
            for (colour in BackgroundColour.entries) {
                val scheme = launcherColorScheme(darkTheme = dark, backgroundColour = colour)
                for (behind in listOf(scheme.background, scheme.surface)) {
                    val main = contrast(scheme.onBackground, behind)
                    val grey = contrast(scheme.onSurfaceVariant, behind)
                    assertTrue("$colour dark=$dark main text contrast $main", main >= 7f)
                    assertTrue("$colour dark=$dark grey text contrast $grey", grey >= 4.5f)
                }
            }
        }
    }

    @Test
    fun `a background colour also tints the cover over the wallpaper`() {
        val scheme = launcherColorScheme(
            darkTheme = false,
            showWallpaper = true,
            backgroundColour = BackgroundColour.SAGE,
            wallpaperDim = WallpaperDim.LIGHT
        )
        assertEquals(SageLightBackground.copy(alpha = WallpaperDim.LIGHT.scrimAlpha), scheme.background)
    }
}
