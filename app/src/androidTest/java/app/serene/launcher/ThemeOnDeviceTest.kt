package app.serene.launcher

import android.graphics.Bitmap
import android.graphics.Color
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.serene.launcher.data.LauncherPreferencesRepository
import app.serene.launcher.model.BackgroundColour
import app.serene.launcher.model.ThemeMode
import app.serene.launcher.model.WallpaperDim
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * What the screen really shows for the theme and wallpaper settings, checked on a screenshot.
 * The colour is read at the left edge halfway down Home, where only the background is drawn.
 */
@RunWith(AndroidJUnit4::class)
class ThemeOnDeviceTest {

    @get:Rule val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private var scenario: ActivityScenario<MainActivity>? = null

    @After
    fun tearDown() {
        scenario?.close()
        shell("cmd uimode night no")
        shell("setprop debug.hwui.force_dark false")
        runBlocking {
            LauncherPreferencesRepository(context).apply {
                setThemeMode(ThemeMode.SYSTEM)
                setShowWallpaper(false)
                setWallpaperDim(WallpaperDim.DEFAULT)
            }
        }
    }

    @Test
    fun lightThemeStaysLightWhenThePhoneIsDarkAndForcesDarkOnApps() {
        // The phone is in dark mode and darkens apps that look light (the "Override force-dark"
        // developer option; Android 16's expanded dark theme and some phone makers do the same).
        shell("cmd uimode night yes")
        shell("setprop debug.hwui.force_dark true")
        assertEquals("true", shell("getprop debug.hwui.force_dark").trim())

        val pixel = backgroundPixel(ThemeMode.LIGHT, showWallpaper = false)

        assertTrue("Light theme drew a dark background: ${hex(pixel)}", luminance(pixel) > 0.8)
    }

    @Test
    fun darkThemeIsDarkOnALightPhone() {
        val pixel = backgroundPixel(ThemeMode.DARK, showWallpaper = false)

        assertTrue("Dark theme drew a light background: ${hex(pixel)}", luminance(pixel) < 0.1)
    }

    @Test
    fun showWallpaperLetsTheWallpaperShowThrough() {
        val plain = backgroundPixel(ThemeMode.LIGHT, showWallpaper = false)
        scenario?.close()
        val overWallpaper = backgroundPixel(ThemeMode.LIGHT, showWallpaper = true)

        assertTrue(
            "The background is the same with and without the wallpaper: ${hex(plain)} and ${hex(overWallpaper)}",
            channelDistance(plain, overWallpaper) > 0.05
        )
    }

    /** Opens Home with [themeMode] and [showWallpaper] and returns the background colour shown. */
    private fun backgroundPixel(themeMode: ThemeMode, showWallpaper: Boolean): Int {
        runBlocking {
            LauncherPreferencesRepository(context).apply {
                setThemeMode(themeMode)
                setShowWallpaper(showWallpaper)
                setUseWallpaperColours(false)
                setBackgroundColour(BackgroundColour.PAPER)
                // The lightest cover shows the most wallpaper, so the difference is easiest to see.
                setWallpaperDim(WallpaperDim.LIGHT)
            }
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(TIMEOUT_MS) {
            compose.onAllNodesWithTag(HOME, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
        // Let the window background and the wallpaper flag settle on screen.
        Thread.sleep(SETTLE_MS)
        val screen: Bitmap = instrumentation.uiAutomation.takeScreenshot()
            ?: error("Couldn't take a screenshot")
        return screen.getPixel(2, screen.height / 2).also { screen.recycle() }
    }

    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
            .bufferedReader().use { it.readText() }

    private fun luminance(pixel: Int): Double =
        (0.2126 * Color.red(pixel) + 0.7152 * Color.green(pixel) + 0.0722 * Color.blue(pixel)) / 255.0

    private fun channelDistance(a: Int, b: Int): Double = maxOf(
        abs(Color.red(a) - Color.red(b)),
        abs(Color.green(a) - Color.green(b)),
        abs(Color.blue(a) - Color.blue(b))
    ) / 255.0

    private fun hex(pixel: Int) = "#%08X".format(pixel)

    private companion object {
        const val HOME = "home_screen"
        const val TIMEOUT_MS = 10_000L
        const val SETTLE_MS = 1_500L
    }
}
