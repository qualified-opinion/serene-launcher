package app.serene.launcher.ui.theme

import androidx.annotation.FontRes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.serene.launcher.R

/** Weights the app's text styles use; each maps to a point on the font's weight axis. */
private val UsedWeights = listOf(
    FontWeight.Light,
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold
)

/**
 * Builds a family from one variable font file, so every weight is real rather than synthesised.
 * Android 7.x can't apply font variations and shows the file's default (Regular) weight instead.
 */
@OptIn(ExperimentalTextApi::class)
private fun variableFontFamily(@FontRes resId: Int, vararg extra: FontVariation.Setting) = FontFamily(
    UsedWeights.map { weight ->
        Font(
            resId = resId,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight), *extra)
        )
    }
)

// Plus Jakarta Sans and DM Sans, both SIL Open Font License (see third_party/fonts).
val PlusJakartaSansFontFamily = variableFontFamily(R.font.plus_jakarta_sans)

// DM Sans also has an optical-size axis; 14 suits the 12–22sp sizes it is used at.
val DmSansFontFamily = variableFontFamily(R.font.dm_sans, FontVariation.Setting("opsz", 14f))

val Typography = Typography(
    // Live clock display on home screen
    displayMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-1.0).sp
    ),
    // Pinned app names on home screen (Niagara/Olauncher inspired typographic focus)
    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 25.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp
    ),
    // App names inside the App Drawer
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 19.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.15).sp
    ),
    // Dialog / BottomSheet titles & empty state headers
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    // Search input and general body text
    bodyLarge = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    // Secondary descriptions and subtitles
    bodyMedium = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp
    ),
    // Section headers, date header, and pill labels
    labelMedium = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.1.sp
    ),
    // Alphabet scrubber rail letters
    labelSmall = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.4.sp
    )
)
