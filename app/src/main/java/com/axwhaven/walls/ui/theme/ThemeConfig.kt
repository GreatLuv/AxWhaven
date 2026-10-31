package com.axwhaven.walls.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalAxWhavenColors = staticCompositionLocalOf<AxWhavenColors> {
    getThemeColors(AppThemeMode.AMOLED_BLACK)
}

enum class AppThemeMode(
    val id: String,
    val title: String,
    val desc: String,
    val previewColors: List<Color>
) {
    AMOLED_BLACK(
        "amoled",
        "AMOLED Black",
        "100% pitch-black background with high-contrast elements for OLEDs",
        listOf(Color(0xFF000000), Color(0xFF141416), Color(0xFFA855F7))
    ),
    FROSTED_GLASS(
        "frosted_glass",
        "Frosty Glass",
        "Translucent frosted acrylic surfaces with prismatic cyan & lavender glow",
        listOf(Color(0xFF0A0C16), Color(0x3BFFFFFF), Color(0xFF7CE8FF))
    )
}

data class AxWhavenColors(
    val isLight: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val primaryAccent: Color,
    val primaryAccentText: Color,
    val secondaryAccent: Color,
    val mintText: Color,
    val mintPillBg: Color,
    val mintPillBorder: Color,
    val surpriseGradientStart: Color,
    val surpriseGradientEnd: Color,
    val surpriseGradientStroke: Color,
    val surpriseIconBg: Color,
    val bottomNavBg: Color,
    val bottomNavBorder: Color,
    val glassFill: Color,
    val glassBorder: Color,
    val scrimEnd: Color,
    val shimmerBase: Color = surfaceElevated,
    val shimmerHighlight: Color = cardBorder,
    val dockSurface: Color = surfaceElevated,
    val dockBorder: Color = cardBorder,
    val isGlassmorphism: Boolean = false
)

fun getThemeColors(themeMode: AppThemeMode): AxWhavenColors {
    return when (themeMode) {
        AppThemeMode.AMOLED_BLACK -> AxWhavenColors(
            isLight = false,
            background = Color(0xFF000000),
            surface = Color(0xFF0B0B0C),
            surfaceElevated = Color(0xFF141416),
            cardBorder = Color(0xFF202024),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFAAAAAA),
            textMuted = Color(0xFF777777),
            primaryAccent = Color(0xFFA855F7),
            primaryAccentText = Color(0xFF000000),
            secondaryAccent = Color(0xFFC084FC),
            mintText = Color(0xFF10B981),
            mintPillBg = Color(0xFF06231A),
            mintPillBorder = Color(0xFF0D3D2E),
            surpriseGradientStart = Color(0xFF1E0A2D),
            surpriseGradientEnd = Color(0xFF0B0B0C),
            surpriseGradientStroke = Color(0xFF28133C),
            surpriseIconBg = Color(0xFF290A3F),
            bottomNavBg = Color(0xF2050507),
            bottomNavBorder = Color(0xFF18181D),
            glassFill = Color(0x22FFFFFF),
            glassBorder = Color(0x38FFFFFF),
            scrimEnd = Color(0xE6000000)
        )
        AppThemeMode.FROSTED_GLASS -> AxWhavenColors(
            isLight = false,
            background = Color(0xFF090B14),
            surface = Color(0x24FFFFFF), // Translucent frosted acrylic sheet
            surfaceElevated = Color(0x36FFFFFF), // Higher opacity frosted glass layer
            cardBorder = Color(0x4DFFFFFF), // 30% specular rim highlight
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFD4DCED),
            textMuted = Color(0xFF909BB5),
            primaryAccent = Color(0xFF7CE8FF), // Prismatic Cyan
            primaryAccentText = Color(0xFF04101A),
            secondaryAccent = Color(0xFFC084FC), // Iridescent Soft Violet
            mintText = Color(0xFF4EEDB4), // Prismatic Mint
            mintPillBg = Color(0x334EEDB4),
            mintPillBorder = Color(0x664EEDB4),
            surpriseGradientStart = Color(0x4D351B63),
            surpriseGradientEnd = Color(0x330F2040),
            surpriseGradientStroke = Color(0x667CE8FF),
            surpriseIconBg = Color(0x52271650),
            bottomNavBg = Color(0x42121728),
            bottomNavBorder = Color(0x59FFFFFF),
            glassFill = Color(0x33FFFFFF),
            glassBorder = Color(0x55FFFFFF),
            scrimEnd = Color(0xE6090B14),
            shimmerBase = Color(0x22FFFFFF),
            shimmerHighlight = Color(0x3DFFFFFF),
            dockSurface = Color(0x4D121728),
            dockBorder = Color(0x59FFFFFF),
            isGlassmorphism = true
        )
    }
}
