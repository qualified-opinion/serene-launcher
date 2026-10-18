package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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

@Composable
fun SereneLauncherTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CalmOledDarkColorScheme else WarmPaperLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
