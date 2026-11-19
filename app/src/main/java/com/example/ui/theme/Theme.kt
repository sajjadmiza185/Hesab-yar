package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme = darkColorScheme(
    primary = BarbargDarkPrimary,
    onPrimary = BarbargDarkOnPrimary,
    primaryContainer = BarbargDarkPrimaryContainer,
    onPrimaryContainer = BarbargDarkOnPrimaryContainer,
    secondary = BarbargDarkSecondary,
    onSecondary = BarbargDarkOnSecondary,
    secondaryContainer = BarbargDarkSecondaryContainer,
    onSecondaryContainer = BarbargDarkOnSecondaryContainer,
    tertiary = BarbargDarkTertiary,
    onTertiary = BarbargDarkOnTertiary,
    tertiaryContainer = BarbargDarkTertiaryContainer,
    onTertiaryContainer = BarbargDarkOnTertiaryContainer,
    background = BarbargDarkBackground,
    onBackground = BarbargDarkOnBackground,
    surface = BarbargDarkSurface,
    onSurface = BarbargDarkOnSurface,
    surfaceVariant = BarbargDarkSurfaceVariant,
    onSurfaceVariant = BarbargDarkOnSurfaceVariant,
    outline = BarbargDarkOutline,
    error = BarbargError,
    errorContainer = BarbargErrorContainer
)

private val LightColorScheme = lightColorScheme(
    primary = BarbargPrimary,
    onPrimary = BarbargOnPrimary,
    primaryContainer = BarbargPrimaryContainer,
    onPrimaryContainer = BarbargOnPrimaryContainer,
    secondary = BarbargSecondary,
    onSecondary = BarbargOnSecondary,
    secondaryContainer = BarbargSecondaryContainer,
    onSecondaryContainer = BarbargOnSecondaryContainer,
    tertiary = BarbargTertiary,
    onTertiary = BarbargOnTertiary,
    tertiaryContainer = BarbargTertiaryContainer,
    onTertiaryContainer = BarbargOnTertiaryContainer,
    background = BarbargBackground,
    onBackground = BarbargOnBackground,
    surface = BarbargSurface,
    onSurface = BarbargOnSurface,
    surfaceVariant = BarbargSurfaceVariant,
    onSurfaceVariant = BarbargOnSurfaceVariant,
    outline = BarbargOutline,
    error = BarbargError,
    errorContainer = BarbargErrorContainer
)

// Glass HD (Oceanic Glass): bright premium sky-blue with high-contrast surfaces.
private val GlassHdColorScheme = lightColorScheme(
    primary = Color(0xFF064A9B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E8FF),
    onPrimaryContainer = Color(0xFF04274F),
    secondary = Color(0xFF0072B8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6F0FF),
    onSecondaryContainer = Color(0xFF00375C),
    tertiary = Color(0xFF2563EB),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDBEAFE),
    onTertiaryContainer = Color(0xFF1E3A8A),
    background = Color(0xFFE2F0FD),
    onBackground = Color(0xFF041A30),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF071F38),
    surfaceVariant = Color(0xFFD8EBFC),
    onSurfaceVariant = Color(0xFF113E6B),
    outline = Color(0xFF6DAEE2),
    outlineVariant = Color(0xFFB5DAFB),
    error = Color(0xFFB91C1C),
    errorContainer = Color(0xFFFFE4E6)
)

// Gold HD: Imperial gold with warm amber surfaces and crisp contrast.
private val GoldHdColorScheme = lightColorScheme(
    primary = Color(0xFF8A5A00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE8A3),
    onPrimaryContainer = Color(0xFF4A3000),
    secondary = Color(0xFF9A6700),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF0C2),
    onSecondaryContainer = Color(0xFF5C3B00),
    tertiary = Color(0xFF7C3F00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDCC2),
    onTertiaryContainer = Color(0xFF5A2500),
    background = Color(0xFFFFFBF0),
    onBackground = Color(0xFF29210F),
    surface = Color(0xFFFFFEFA),
    onSurface = Color(0xFF29210F),
    surfaceVariant = Color(0xFFF6EBD0),
    onSurfaceVariant = Color(0xFF493600),
    outline = Color(0xFF9A7835),
    outlineVariant = Color(0xFFDECA98),
    error = Color(0xFFB42318),
    errorContainer = Color(0xFFFFE4DE)
)

// Midnight HD: Deep neon midnight dark theme.
private val MidnightHdColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF082F49),
    primaryContainer = Color(0xFF164E78),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF22D3EE),
    onSecondary = Color(0xFF083344),
    secondaryContainer = Color(0xFF155E75),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = Color(0xFFA5B4FC),
    onTertiary = Color(0xFF1E1B4B),
    tertiaryContainer = Color(0xFF3730A3),
    onTertiaryContainer = Color(0xFFE0E7FF),
    background = Color(0xFF071426),
    onBackground = Color(0xFFEAF4FF),
    surface = Color(0xFF10243B),
    onSurface = Color(0xFFEAF4FF),
    surfaceVariant = Color(0xFF1A3655),
    onSurfaceVariant = Color(0xFFC1D8EE),
    outline = Color(0xFF47749B),
    outlineVariant = Color(0xFF284D6F),
    error = Color(0xFFFF6B7A),
    errorContainer = Color(0xFF5B1D2A)
)

// Emerald HD: Royal emerald jade with lush natural surfaces.
private val EmeraldHdColorScheme = lightColorScheme(
    primary = Color(0xFF047857),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F7D5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF0F766E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF134E4A),
    tertiary = Color(0xFF4D7C0F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFECFCCB),
    onTertiaryContainer = Color(0xFF365314),
    background = Color(0xFFEAFBF2),
    onBackground = Color(0xFF102D23),
    surface = Color(0xFFF8FFFB),
    onSurface = Color(0xFF102D23),
    surfaceVariant = Color(0xFFD1F3E1),
    onSurfaceVariant = Color(0xFF123D30),
    outline = Color(0xFF397B5D),
    outlineVariant = Color(0xFFA8DFC2),
    error = Color(0xFFB91C1C),
    errorContainer = Color(0xFFFFE4E6)
)

// Royal Purple HD: Imperial amethyst with regal lavender surfaces.
private val RoyalPurpleHdColorScheme = lightColorScheme(
    primary = Color(0xFF6D28D9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9D5FF),
    onPrimaryContainer = Color(0xFF3B0764),
    secondary = Color(0xFF9333EA),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E8FF),
    onSecondaryContainer = Color(0xFF581C87),
    tertiary = Color(0xFFBE185D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE4F0),
    onTertiaryContainer = Color(0xFF831843),
    background = Color(0xFFF5F0FF),
    onBackground = Color(0xFF24143D),
    surface = Color(0xFFFFFCFF),
    onSurface = Color(0xFF24143D),
    surfaceVariant = Color(0xFFE9DDF8),
    onSurfaceVariant = Color(0xFF3F285B),
    outline = Color(0xFF80639F),
    outlineVariant = Color(0xFFD6C2EB),
    error = Color(0xFFB91C1C),
    errorContainer = Color(0xFFFFE4E6)
)

// Carbon HD: High-tech carbon fiber titanium dark theme.
private val CarbonHdColorScheme = darkColorScheme(
    primary = Color(0xFFE2E8F0),
    onPrimary = Color(0xFF111827),
    primaryContainer = Color(0xFF374151),
    onPrimaryContainer = Color(0xFFF3F4F6),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF111827),
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF082F49),
    tertiaryContainer = Color(0xFF075985),
    onTertiaryContainer = Color(0xFFE0F2FE),
    background = Color(0xFF090D14),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF151B25),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF242D3B),
    onSurfaceVariant = Color(0xFFD1D9E6),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF374659),
    error = Color(0xFFFF6B7A),
    errorContainer = Color(0xFF5B1D2A)
)

/** True when the user selected a theme with 3D styling (all active selectable themes). */
val LocalBarbarg3D = staticCompositionLocalOf { true }

/** Legacy composition local maintained for compatibility. */
val LocalBlueGloss3D = staticCompositionLocalOf { false }

@Composable
fun BarbargTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    themeMode: String = if (darkTheme) "DARK_3D" else "LIGHT_3D",
    fontScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val normalizedThemeMode = themeMode.removeSuffix("_3D")
    val colorScheme = when (normalizedThemeMode) {
        "GLASS_HD" -> GlassHdColorScheme
        "GOLD_HD" -> GoldHdColorScheme
        "MIDNIGHT_HD" -> MidnightHdColorScheme
        "EMERALD_HD" -> EmeraldHdColorScheme
        "ROYAL_PURPLE_HD" -> RoyalPurpleHdColorScheme
        "CARBON_HD" -> CarbonHdColorScheme
        else -> when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
    }

    val style3D = resolve3DStyle(themeMode, darkTheme)
    val is3DActive = true // All selectable themes in the 3D redesign use genuine 3D surfaces

    val currentDensity = LocalDensity.current
    val safeFontScale = fontScale.coerceIn(0.85f, 1.40f)
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalContentColor provides colorScheme.onSurface,
        LocalBarbarg3D provides is3DActive,
        LocalBlueGloss3D provides (normalizedThemeMode == "GLASS_HD"),
        LocalBarbarg3DStyle provides style3D,
        LocalDensity provides Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * safeFontScale
        )
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}
