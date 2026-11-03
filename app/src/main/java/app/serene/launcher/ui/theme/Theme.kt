package app.serene.launcher.ui.theme

import android.app.Activity
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import app.serene.launcher.R

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
 * How opaque Serene's background is over the wallpaper in "Show wallpaper" mode. Together with the
 * stronger grey text below, all text keeps a contrast of at least 4.5:1 over any wallpaper from
 * black to white, in light and dark (see ThemeTest).
 */
internal const val WALLPAPER_SCRIM_ALPHA = 0.75f

/**
 * The colours to use: the wallpaper's own colours when [wallpaperColours] is given, otherwise
 * Serene's warm-paper or dark palette. With [showWallpaper], the background becomes a see-through
 * scrim so the wallpaper shows while text stays readable. With both off it is exactly today's look.
 */
internal fun launcherColorScheme(
    darkTheme: Boolean,
    wallpaperColours: ColorScheme? = null,
    showWallpaper: Boolean = false
): ColorScheme {
    val base = wallpaperColours ?: if (darkTheme) CalmOledDarkColorScheme else WarmPaperLightColorScheme
    if (!showWallpaper) return base
    return base.copy(
        background = base.background.copy(alpha = WALLPAPER_SCRIM_ALPHA),
        // Grey text (the date, hints, "Work" tags) moves halfway to the main text colour.
        onSurfaceVariant = halfway(base.onSurfaceVariant, base.onBackground)
    )
}

/** The colour halfway between [a] and [b], mixed in plain sRGB. */
private fun halfway(a: Color, b: Color): Color =
    Color(red = (a.red + b.red) / 2, green = (a.green + b.green) / 2, blue = (a.blue + b.blue) / 2)

@Composable
fun SereneLauncherTheme(
    darkTheme: Boolean = false,
    useWallpaperColours: Boolean = false,
    showWallpaper: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val wallpaperColours = if (useWallpaperColours && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        null
    }
    val colorScheme = launcherColorScheme(darkTheme, wallpaperColours, showWallpaper)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
                // The wallpaper only shows through a window that asks for it and has no solid background.
                if (showWallpaper) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                    window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                    window.setBackgroundDrawableResource(R.color.launcher_background)
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
