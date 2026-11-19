package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Visual definition of a 3D theme style for Hesabyar (Barberg).
 * Defines realistic lighting, material bevel, specular shine, layered natural shadows,
 * and decorative ambient backdrop styling.
 */
@Immutable
data class Barbarg3DStyle(
    val themeId: String,
    val titleFa: String,
    val isDark: Boolean,
    // Ambient backdrop gradient (top-down or diagonal ambient fill)
    val backgroundBrush: Brush,
    val backgroundWaves: List<WaveSpec> = emptyList(),
    // Card styling
    val cardBackground: Brush,
    val cardBorder: Brush,
    val cardBorderWidth: Dp = 1.25.dp,
    val cardSpecularShine: Brush? = null,
    val cardSpecularHeight: Dp = 28.dp,
    val cardElevation: Dp = 8.dp,
    val cardPressedElevation: Dp = 14.dp,
    val cardAmbientShadow: Color = Color.Black.copy(alpha = 0.12f),
    // Button styling
    val buttonBackground: Brush,
    val buttonBorder: Brush? = null,
    val buttonSpecularShine: Brush? = null,
    val buttonSpecularHeight: Dp = 14.dp,
    val buttonElevation: Dp = 5.dp,
    val buttonPressedElevation: Dp = 2.dp,
    // Accent / Glow
    val accentGlow: Color = Color.Transparent,
    // Navigation bar surface tint
    val navSurfaceBrush: Brush,
    val navBorder: Brush? = null
)

@Immutable
data class WaveSpec(
    val pathPoints: List<Float>, // relative x,y control points for stylized waves
    val brush: Brush
)

val LocalBarbarg3DStyle = staticCompositionLocalOf<Barbarg3DStyle> {
    MidnightHd3DStyle
}

// -------------------------------------------------------------
// 1. MIDNIGHT_HD (Deep Blue / Neon Midnight)
// -------------------------------------------------------------
val MidnightHd3DStyle = Barbarg3DStyle(
    themeId = "MIDNIGHT_HD",
    titleFa = "نیمه‌شب نئونی",
    isDark = true,
    backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF040E1B),
            Color(0xFF07172C),
            Color(0xFF0B213D),
            Color(0xFF061528)
        )
    ),
    cardBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF142C48),
            Color(0xFF0F2238),
            Color(0xFF0A1828)
        )
    ),
    cardBorder = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF60A5FA).copy(alpha = 0.85f),
            Color(0xFF2563EB).copy(alpha = 0.50f),
            Color(0xFF38BDF8).copy(alpha = 0.70f)
        )
    ),
    cardBorderWidth = 1.25.dp,
    cardSpecularShine = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF93C5FD).copy(alpha = 0.28f),
            Color(0xFF60A5FA).copy(alpha = 0.10f),
            Color.Transparent
        )
    ),
    cardSpecularHeight = 32.dp,
    cardElevation = 10.dp,
    cardPressedElevation = 16.dp,
    cardAmbientShadow = Color(0xFF01060D).copy(alpha = 0.65f),
    buttonBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF3B82F6),
            Color(0xFF2563EB),
            Color(0xFF1D4ED8)
        )
    ),
    buttonBorder = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF93C5FD).copy(alpha = 0.90f),
            Color(0xFF60A5FA).copy(alpha = 0.40f)
        )
    ),
    buttonSpecularShine = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.35f),
            Color.Transparent
        )
    ),
    buttonSpecularHeight = 14.dp,
    buttonElevation = 6.dp,
    buttonPressedElevation = 2.dp,
    accentGlow = Color(0xFF38BDF8).copy(alpha = 0.35f),
    navSurfaceBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F2238).copy(alpha = 0.95f),
            Color(0xFF07172C).copy(alpha = 0.98f)
        )
    ),
    navBorder = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF38BDF8).copy(alpha = 0.6f),
            Color(0xFF2563EB).copy(alpha = 0.4f),
            Color(0xFF60A5FA).copy(alpha = 0.6f)
        )
    )
)

// -------------------------------------------------------------
// 2. GLASS_HD (Glossy Oceanic Sky / Blue Glass 3D Refined)
// Matches the visual reference PNGs from the user's theme pack
// -------------------------------------------------------------
val GlassHd3DStyle = Barbarg3DStyle(
    themeId = "GLASS_HD",
    titleFa = "شیشه‌ای آبی",
    isDark = false,
    backgroundBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF074898),
            Color(0xFF167ED4),
            Color(0xFF8FD6FF),
            Color(0xFF1967B7)
        )
    ),
    cardBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF).copy(alpha = 0.96f),
            Color(0xFFF0F7FF).copy(alpha = 0.94f),
            Color(0xFFD8EBFC).copy(alpha = 0.92f)
        )
    ),
    cardBorder = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.98f),
            Color(0xFF73C9FF).copy(alpha = 0.92f),
            Color(0xFFB9E7FF).copy(alpha = 0.80f),
            Color.White.copy(alpha = 0.96f)
        )
    ),
    cardBorderWidth = 1.35.dp,
    cardSpecularShine = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.42f),
            Color.White.copy(alpha = 0.14f),
            Color.Transparent
        )
    ),
    cardSpecularHeight = 36.dp,
    cardElevation = 12.dp,
    cardPressedElevation = 18.dp,
    cardAmbientShadow = Color(0xFF06336B).copy(alpha = 0.35f),
    buttonBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1D82E2),
            Color(0xFF0A63C0),
            Color(0xFF054A94)
        )
    ),
    buttonBorder = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.95f),
            Color(0xFF90D2FF).copy(alpha = 0.65f)
        )
    ),
    buttonSpecularShine = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.45f),
            Color.White.copy(alpha = 0.12f),
            Color.Transparent
        )
    ),
    buttonSpecularHeight = 14.dp,
    buttonElevation = 6.dp,
    buttonPressedElevation = 2.dp,
    accentGlow = Color(0xFF67BFFF).copy(alpha = 0.4f),
    navSurfaceBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFEAF5FF).copy(alpha = 0.95f),
            Color(0xFFD3E9FD).copy(alpha = 0.98f)
        )
    ),
    navBorder = Brush.horizontalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.95f),
            Color(0xFF73C9FF).copy(alpha = 0.85f),
            Color.White.copy(alpha = 0.95f)
        )
    )
)

// -------------------------------------------------------------
// 3. GOLD_HD (Imperial Gold / Metallic Lustre)
// -------------------------------------------------------------
val GoldHd3DStyle = Barbarg3DStyle(
    themeId = "GOLD_HD",
    titleFa = "گلد سلطنتی",
    isDark = false,
    backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFBF2),
            Color(0xFFFDF4DC),
            Color(0xFFF7E7BE),
            Color(0xFFFFFDF8)
        )
    ),
    cardBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFDF7),
            Color(0xFFFFF8E7),
            Color(0xFFF9EDCE)
        )
    ),
    cardBorder = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFEEB8),
            Color(0xFFD4A72C),
            Color(0xFFFCE194),
            Color(0xFF9E7412)
        )
    ),
    cardBorderWidth = 1.25.dp,
    cardSpecularShine = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.48f),
            Color(0xFFFFF6D8).copy(alpha = 0.20f),
            Color.Transparent
        )
    ),
    cardSpecularHeight = 32.dp,
    cardElevation = 9.dp,
    cardPressedElevation = 15.dp,
    cardAmbientShadow = Color(0xFF523B06).copy(alpha = 0.18f),
    buttonBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE2B73C),
            Color(0xFFB8820B),
            Color(0xFF8F6304)
        )
    ),
    buttonBorder = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFF5D1),
            Color(0xFFD8A82F)
        )
    ),
    buttonSpecularShine = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.45f),
            Color.Transparent
        )
    ),
    buttonSpecularHeight = 14.dp,
    buttonElevation = 6.dp,
    buttonPressedElevation = 2.dp,
    accentGlow = Color(0xFFF7CC48).copy(alpha = 0.35f),
    navSurfaceBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFDF8).copy(alpha = 0.95f),
            Color(0xFFF7EBD0).copy(alpha = 0.98f)
        )
    ),
    navBorder = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFD4A72C).copy(alpha = 0.6f),
            Color(0xFFFCE194).copy(alpha = 0.9f),
            Color(0xFFD4A72C).copy(alpha = 0.6f)
        )
    )
)

// -------------------------------------------------------------
// 4. EMERALD_HD (Royal Jade / Emerald Jewel)
// -------------------------------------------------------------
val EmeraldHd3DStyle = Barbarg3DStyle(
    themeId = "EMERALD_HD",
    titleFa = "زمردی سلطنتی",
    isDark = false,
    backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFEAF9F1),
            Color(0xFFD5F4E4),
            Color(0xFFB8ECCD),
            Color(0xFFE8FAF1)
        )
    ),
    cardBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFCFFFD),
            Color(0xFFF0FBF5),
            Color(0xFFDCF6E8)
        )
    ),
    cardBorder = Brush.linearGradient(
        colors = listOf(
            Color(0xFFB1F4CE),
            Color(0xFF10B981),
            Color(0xFF6EE7B7),
            Color(0xFF047857)
        )
    ),
    cardBorderWidth = 1.25.dp,
    cardSpecularShine = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.44f),
            Color(0xFFE0FBEF).copy(alpha = 0.16f),
            Color.Transparent
        )
    ),
    cardSpecularHeight = 32.dp,
    cardElevation = 9.dp,
    cardPressedElevation = 15.dp,
    cardAmbientShadow = Color(0xFF063B25).copy(alpha = 0.18f),
    buttonBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF10B981),
            Color(0xFF059669),
            Color(0xFF047857)
        )
    ),
    buttonBorder = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFA7F3D0),
            Color(0xFF34D399)
        )
    ),
    buttonSpecularShine = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.40f),
            Color.Transparent
        )
    ),
    buttonSpecularHeight = 14.dp,
    buttonElevation = 6.dp,
    buttonPressedElevation = 2.dp,
    accentGlow = Color(0xFF34D399).copy(alpha = 0.35f),
    navSurfaceBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF8FFFB).copy(alpha = 0.95f),
            Color(0xFFD7F5E4).copy(alpha = 0.98f)
        )
    ),
    navBorder = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF10B981).copy(alpha = 0.5f),
            Color(0xFF6EE7B7).copy(alpha = 0.8f),
            Color(0xFF10B981).copy(alpha = 0.5f)
        )
    )
)

// -------------------------------------------------------------
// 5. ROYAL_PURPLE_HD (Imperial Amethyst / Velvet Purple)
// -------------------------------------------------------------
val RoyalPurpleHd3DStyle = Barbarg3DStyle(
    themeId = "ROYAL_PURPLE_HD",
    titleFa = "بنفش سلطنتی",
    isDark = false,
    backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF6F0FE),
            Color(0xFFEFE4FD),
            Color(0xFFE2CFFB),
            Color(0xFFF8F3FF)
        )
    ),
    cardBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFFAF5FF),
            Color(0xFFEFE3FD)
        )
    ),
    cardBorder = Brush.linearGradient(
        colors = listOf(
            Color(0xFFE9D5FF),
            Color(0xFF9333EA),
            Color(0xFFC084FC),
            Color(0xFF6B21A8)
        )
    ),
    cardBorderWidth = 1.25.dp,
    cardSpecularShine = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.45f),
            Color(0xFFF3E8FF).copy(alpha = 0.18f),
            Color.Transparent
        )
    ),
    cardSpecularHeight = 32.dp,
    cardElevation = 9.dp,
    cardPressedElevation = 15.dp,
    cardAmbientShadow = Color(0xFF3B0764).copy(alpha = 0.20f),
    buttonBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF9333EA),
            Color(0xFF7E22CE),
            Color(0xFF6B21A8)
        )
    ),
    buttonBorder = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF3E8FF),
            Color(0xFFC084FC)
        )
    ),
    buttonSpecularShine = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.40f),
            Color.Transparent
        )
    ),
    buttonSpecularHeight = 14.dp,
    buttonElevation = 6.dp,
    buttonPressedElevation = 2.dp,
    accentGlow = Color(0xFFA855F7).copy(alpha = 0.35f),
    navSurfaceBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFCFF).copy(alpha = 0.95f),
            Color(0xFFEBDCF9).copy(alpha = 0.98f)
        )
    ),
    navBorder = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF9333EA).copy(alpha = 0.5f),
            Color(0xFFC084FC).copy(alpha = 0.8f),
            Color(0xFF9333EA).copy(alpha = 0.5f)
        )
    )
)

// -------------------------------------------------------------
// 6. CARBON_HD (Stealth Carbon Fiber / Titanium Dark)
// -------------------------------------------------------------
val CarbonHd3DStyle = Barbarg3DStyle(
    themeId = "CARBON_HD",
    titleFa = "کربن تیره",
    isDark = true,
    backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF090D13),
            Color(0xFF101722),
            Color(0xFF17202E),
            Color(0xFF0C1118)
        )
    ),
    cardBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF222B38),
            Color(0xFF18202B),
            Color(0xFF111720)
        )
    ),
    cardBorder = Brush.linearGradient(
        colors = listOf(
            Color(0xFF64748B).copy(alpha = 0.80f),
            Color(0xFF334155).copy(alpha = 0.50f),
            Color(0xFF94A3B8).copy(alpha = 0.70f)
        )
    ),
    cardBorderWidth = 1.25.dp,
    cardSpecularShine = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.18f),
            Color(0xFF94A3B8).copy(alpha = 0.06f),
            Color.Transparent
        )
    ),
    cardSpecularHeight = 28.dp,
    cardElevation = 9.dp,
    cardPressedElevation = 15.dp,
    cardAmbientShadow = Color(0xFF000000).copy(alpha = 0.75f),
    buttonBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF475569),
            Color(0xFF334155),
            Color(0xFF1E293B)
        )
    ),
    buttonBorder = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF94A3B8),
            Color(0xFF475569)
        )
    ),
    buttonSpecularShine = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.25f),
            Color.Transparent
        )
    ),
    buttonSpecularHeight = 12.dp,
    buttonElevation = 5.dp,
    buttonPressedElevation = 2.dp,
    accentGlow = Color(0xFF38BDF8).copy(alpha = 0.25f),
    navSurfaceBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF171F2B).copy(alpha = 0.95f),
            Color(0xFF0E131C).copy(alpha = 0.98f)
        )
    ),
    navBorder = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF64748B).copy(alpha = 0.5f),
            Color(0xFF94A3B8).copy(alpha = 0.7f),
            Color(0xFF64748B).copy(alpha = 0.5f)
        )
    )
)

// -------------------------------------------------------------
// 7. LIGHT (Classic Soft Light 3D)
// -------------------------------------------------------------
val Light3DStyle = Barbarg3DStyle(
    themeId = "LIGHT",
    titleFa = "روشن کلاسیک",
    isDark = false,
    backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF8FAFC),
            Color(0xFFF1F5F9),
            Color(0xFFE2E8F0),
            Color(0xFFF8FAFC)
        )
    ),
    cardBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFF8FAFC),
            Color(0xFFEFF3F8)
        )
    ),
    cardBorder = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.95f),
            Color(0xFFCBD5E1),
            Color(0xFF94A3B8).copy(alpha = 0.6f)
        )
    ),
    cardBorderWidth = 1.0.dp,
    cardSpecularShine = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.40f),
            Color.Transparent
        )
    ),
    cardSpecularHeight = 24.dp,
    cardElevation = 6.dp,
    cardPressedElevation = 12.dp,
    cardAmbientShadow = Color(0xFF0F172A).copy(alpha = 0.12f),
    buttonBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF2563EB),
            Color(0xFF1D4ED8),
            Color(0xFF1E40AF)
        )
    ),
    buttonBorder = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF93C5FD),
            Color(0xFF3B82F6)
        )
    ),
    buttonSpecularShine = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.35f),
            Color.Transparent
        )
    ),
    buttonSpecularHeight = 12.dp,
    buttonElevation = 5.dp,
    buttonPressedElevation = 2.dp,
    accentGlow = Color(0xFF3B82F6).copy(alpha = 0.25f),
    navSurfaceBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF).copy(alpha = 0.95f),
            Color(0xFFF1F5F9).copy(alpha = 0.98f)
        )
    ),
    navBorder = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFE2E8F0),
            Color(0xFFCBD5E1),
            Color(0xFFE2E8F0)
        )
    )
)

// -------------------------------------------------------------
// 8. DARK (Classic Dark 3D)
// -------------------------------------------------------------
val Dark3DStyle = Barbarg3DStyle(
    themeId = "DARK",
    titleFa = "تاریک کلاسیک",
    isDark = true,
    backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0B111E),
            Color(0xFF0F172A),
            Color(0xFF1E293B),
            Color(0xFF0F172A)
        )
    ),
    cardBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF26354D),
            Color(0xFF1E293B),
            Color(0xFF161F2E)
        )
    ),
    cardBorder = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF475569).copy(alpha = 0.8f),
            Color(0xFF334155).copy(alpha = 0.5f),
            Color(0xFF64748B).copy(alpha = 0.7f)
        )
    ),
    cardBorderWidth = 1.1.dp,
    cardSpecularShine = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.15f),
            Color.Transparent
        )
    ),
    cardSpecularHeight = 26.dp,
    cardElevation = 8.dp,
    cardPressedElevation = 14.dp,
    cardAmbientShadow = Color(0xFF000000).copy(alpha = 0.65f),
    buttonBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF3B82F6),
            Color(0xFF2563EB),
            Color(0xFF1D4ED8)
        )
    ),
    buttonBorder = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF93C5FD),
            Color(0xFF60A5FA)
        )
    ),
    buttonSpecularShine = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.30f),
            Color.Transparent
        )
    ),
    buttonSpecularHeight = 12.dp,
    buttonElevation = 5.dp,
    buttonPressedElevation = 2.dp,
    accentGlow = Color(0xFF60A5FA).copy(alpha = 0.30f),
    navSurfaceBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1E293B).copy(alpha = 0.95f),
            Color(0xFF0F172A).copy(alpha = 0.98f)
        )
    ),
    navBorder = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF334155),
            Color(0xFF475569),
            Color(0xFF334155)
        )
    )
)

/**
 * Resolves the 3D style for a given normalized theme ID.
 */
fun resolve3DStyle(themeMode: String, isDark: Boolean): Barbarg3DStyle {
    val normalized = themeMode.removeSuffix("_3D")
    return when (normalized) {
        "MIDNIGHT_HD" -> MidnightHd3DStyle
        "GLASS_HD" -> GlassHd3DStyle
        "GOLD_HD" -> GoldHd3DStyle
        "EMERALD_HD" -> EmeraldHd3DStyle
        "ROYAL_PURPLE_HD" -> RoyalPurpleHd3DStyle
        "CARBON_HD" -> CarbonHd3DStyle
        "DARK" -> Dark3DStyle
        "LIGHT" -> Light3DStyle
        else -> if (isDark) Dark3DStyle else Light3DStyle
    }
}
