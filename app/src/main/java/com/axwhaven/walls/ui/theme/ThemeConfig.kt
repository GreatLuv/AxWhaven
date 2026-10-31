package com.axwhaven.walls.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalAxWhavenColors = staticCompositionLocalOf<AxWhavenColors> {
    getThemeColors(AppThemeMode.EXPRESSIVE)
}

enum class AppThemeMode(
    val id: String,
    val title: String,
    val desc: String,
    val previewColors: List<Color>
) {
    EXPRESSIVE(
        "expressive",
        "Material 3 Expressive",
        "Dynamic Pixel-style expressive tonal surfaces with electric lavender and spring motion",
        listOf(Color(0xFF141218), Color(0xFF2B2930), Color(0xFFD0BCFF))
    ),
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
    val isGlassmorphism: Boolean = false,
    val isExpressive: Boolean = false,
    val surfaceContainerLowest: Color = surface,
    val surfaceContainerLow: Color = surface,
    val surfaceContainer: Color = surface,
    val surfaceContainerHigh: Color = surfaceElevated,
    val surfaceContainerHighest: Color = surfaceElevated,
    val outlineVariant: Color = cardBorder
)

fun getThemeColors(themeMode: AppThemeMode): AxWhavenColors {
    return when (themeMode) {
        AppThemeMode.EXPRESSIVE -> AxWhavenColors(
            isLight = false,
            background = Color(0xFF141218),
            surface = Color(0xFF141218),
            surfaceElevated = Color(0xFF2B2930),
            cardBorder = Color(0xFF49454F),
            textPrimary = Color(0xFFE6E0E9),
            textSecondary = Color(0xFFCAC4D0),
            textMuted = Color(0xFF938F99),
            primaryAccent = Color(0xFFD0BCFF),
            primaryAccentText = Color(0xFF381E72),
            secondaryAccent = Color(0xFF4ED8E8),
            mintText = Color(0xFF4EEDB4),
            mintPillBg = Color(0xFF1D2F28),
            mintPillBorder = Color(0xFF2E4D40),
            surpriseGradientStart = Color(0xFF2F2445),
            surpriseGradientEnd = Color(0xFF1D1B20),
            surpriseGradientStroke = Color(0xFF49454F),
            surpriseIconBg = Color(0xFF4F378B),
            bottomNavBg = Color(0xF21D1B20),
            bottomNavBorder = Color(0xFF49454F),
            glassFill = Color(0x33FFFFFF),
            glassBorder = Color(0x4DFFFFFF),
            scrimEnd = Color(0xE6141218),
            isExpressive = true,
            surfaceContainerLowest = Color(0xFF0F0D13),
            surfaceContainerLow = Color(0xFF1D1B20),
            surfaceContainer = Color(0xFF211F26),
            surfaceContainerHigh = Color(0xFF2B2930),
            surfaceContainerHighest = Color(0xFF36343B),
            outlineVariant = Color(0xFF49454F)
        )
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
