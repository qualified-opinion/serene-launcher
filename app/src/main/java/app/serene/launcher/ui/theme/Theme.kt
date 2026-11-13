package app.serene.launcher.ui.theme

import android.app.Activity
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.WindowManager
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.core.view.WindowCompat
import app.serene.launcher.model.BackgroundColour
import app.serene.launcher.model.WallpaperDim

private val WarmPaperLightColorScheme = lightColorScheme(
    primary = TerracottaAccent,
    onPrimary = Color.White,
    primaryContainer = TerracottaContainer,
    onPrimaryContainer = OnTerracottaContainer,
    secondary = WarmSlateMuted,
    onSecondary = Color.White,
    secondaryContainer = WarmPaperSurfaceVariant,
    onSecondaryContainer = WarmCharcoalInk,
    tertiary = TerracottaAccent,
    background = WarmPaperBackground,
    onBackground = WarmCharcoalInk,
    surface = WarmPaperSurface,
    onSurface = WarmCharcoalInk,
    surfaceVariant = WarmPaperSurfaceVariant,
    onSurfaceVariant = WarmSlateMuted,
    outline = WarmSubtleDivider,
    outlineVariant = WarmSubtleDivider
)

private val CalmOledDarkColorScheme = darkColorScheme(
    primary = CoralClayAccent,
    onPrimary = Color(0xFF1C1B18),
    primaryContainer = CoralClayContainer,
    onPrimaryContainer = OnCoralClayContainer,
    secondary = WarmGrayMuted,
    onSecondary = Color(0xFF1C1B18),
    secondaryContainer = OledDarkSurfaceVariant,
    onSecondaryContainer = ParchmentWhiteInk,
    tertiary = CoralClayAccent,
    background = OledDarkBackground,
    onBackground = ParchmentWhiteInk,
    surface = OledDarkSurface,
    onSurface = ParchmentWhiteInk,
    surfaceVariant = OledDarkSurfaceVariant,
    onSurfaceVariant = WarmGrayMuted,
    outline = DarkSubtleDivider,
    outlineVariant = DarkSubtleDivider
)

/**
 * How opaque Serene's background is over the wallpaper at the strongest dim, the only strength
 * before the setting existed. Together with the stronger grey text below, all text keeps a
 * contrast of at least 4.5:1 over any wallpaper from black to white, in light and dark (see ThemeTest).
 */
internal val WALLPAPER_SCRIM_ALPHA = WallpaperDim.STRONG.scrimAlpha

/** The background and surface for [colour], or null for Serene's own paper (or dark) colours. */
internal fun backgroundTint(colour: BackgroundColour, darkTheme: Boolean): Pair<Color, Color>? = when (colour) {
    BackgroundColour.PAPER -> null
    BackgroundColour.SAND -> if (darkTheme) SandDarkBackground to SandDarkSurface else SandLightBackground to SandLightSurface
    BackgroundColour.SAGE -> if (darkTheme) SageDarkBackground to SageDarkSurface else SageLightBackground to SageLightSurface
    BackgroundColour.MIST -> if (darkTheme) MistDarkBackground to MistDarkSurface else MistLightBackground to MistLightSurface
    BackgroundColour.LAVENDER ->
        if (darkTheme) LavenderDarkBackground to LavenderDarkSurface else LavenderLightBackground to LavenderLightSurface
    BackgroundColour.BLUSH -> if (darkTheme) BlushDarkBackground to BlushDarkSurface else BlushLightBackground to BlushLightSurface
}

/** The colour a Settings swatch shows for [colour]: the background it gives in light or dark. */
fun backgroundSwatch(colour: BackgroundColour, darkTheme: Boolean): Color =
    backgroundTint(colour, darkTheme)?.first ?: if (darkTheme) OledDarkBackground else WarmPaperBackground

/**
 * The colours to use: the wallpaper's own colours when [wallpaperColours] is given, otherwise
 * Serene's warm-paper or dark palette. A [backgroundColour] other than paper tints the background
 * and surfaces. With [showWallpaper], the background becomes a see-through cover, as opaque as
 * [wallpaperDim] says, so the wallpaper shows. With everything off it is exactly the original look.
 */
internal fun launcherColorScheme(
    darkTheme: Boolean,
    wallpaperColours: ColorScheme? = null,
    showWallpaper: Boolean = false,
    backgroundColour: BackgroundColour = BackgroundColour.PAPER,
    wallpaperDim: WallpaperDim = WallpaperDim.STRONG
): ColorScheme {
    val palette = wallpaperColours ?: if (darkTheme) CalmOledDarkColorScheme else WarmPaperLightColorScheme
    val base = backgroundTint(backgroundColour, darkTheme)?.let { (background, surface) ->
        palette.copy(background = background, surface = surface)
    } ?: palette
    if (!showWallpaper) return base
    return base.copy(
        background = base.background.copy(alpha = wallpaperDim.scrimAlpha),
        // Grey text (the date, hints, "Work" tags) moves halfway to the main text colour.
        onSurfaceVariant = halfway(base.onSurfaceVariant, base.onBackground)
    )
}

/** The colour halfway between [a] and [b], mixed in plain sRGB. */
private fun halfway(a: Color, b: Color): Color =
    Color(red = (a.red + b.red) / 2, green = (a.green + b.green) / 2, blue = (a.blue + b.blue) / 2)

/**
 * True when text needs a glow to stand out from the wallpaper: only when the wallpaper shows
 * through a cover lighter than the strongest, which already keeps every text readable by itself.
 */
internal fun needsTextGlow(showWallpaper: Boolean, wallpaperDim: WallpaperDim): Boolean =
    showWallpaper && wallpaperDim != WallpaperDim.STRONG

/** Every text style with a soft halo of [glow] around the letters, so they read over a busy wallpaper. */
internal fun androidx.compose.material3.Typography.withGlow(glow: Color): androidx.compose.material3.Typography {
    val shadow = Shadow(color = glow, offset = Offset.Zero, blurRadius = 10f)
    fun TextStyle.glowing() = copy(shadow = shadow)
    return copy(
        displayLarge = displayLarge.glowing(),
        displayMedium = displayMedium.glowing(),
        displaySmall = displaySmall.glowing(),
        headlineLarge = headlineLarge.glowing(),
        headlineMedium = headlineMedium.glowing(),
        headlineSmall = headlineSmall.glowing(),
        titleLarge = titleLarge.glowing(),
        titleMedium = titleMedium.glowing(),
        titleSmall = titleSmall.glowing(),
        bodyLarge = bodyLarge.glowing(),
        bodyMedium = bodyMedium.glowing(),
        bodySmall = bodySmall.glowing(),
        labelLarge = labelLarge.glowing(),
        labelMedium = labelMedium.glowing(),
        labelSmall = labelSmall.glowing()
    )
}

@Composable
fun SereneLauncherTheme(
    darkTheme: Boolean = false,
    useWallpaperColours: Boolean = false,
    showWallpaper: Boolean = false,
    backgroundColour: BackgroundColour = BackgroundColour.PAPER,
    wallpaperDim: WallpaperDim = WallpaperDim.DEFAULT,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val wallpaperColours = if (useWallpaperColours && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        null
    }
    val colorScheme = launcherColorScheme(darkTheme, wallpaperColours, showWallpaper, backgroundColour, wallpaperDim)
    val typography = if (needsTextGlow(showWallpaper, wallpaperDim)) {
        Typography.withGlow(colorScheme.background.copy(alpha = 1f))
    } else {
        Typography
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val windowBackground = colorScheme.background.toArgb()
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // Serene draws its own light and dark looks; the phone must not darken them again.
                // The theme says so too (themes.xml); this also covers phones that only check views.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.decorView.isForceDarkAllowed = false
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
                // The wallpaper only shows through a window that asks for it and has no solid background.
                if (showWallpaper) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                    window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
                    // Say outright that the window is see-through. Some phones kept it marked solid
                    // from start-up, so the screen behind Serene was black instead of the wallpaper.
                    window.setFormat(PixelFormat.TRANSLUCENT)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                    window.setFormat(PixelFormat.OPAQUE)
                    // The window matches the chosen colour and the app's own light or dark choice, so
                    // nothing of another colour shows while the keyboard opens or a screen changes.
                    window.setBackgroundDrawable(ColorDrawable(windowBackground))
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
