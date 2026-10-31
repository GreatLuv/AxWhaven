package com.axwhaven.walls.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalAxWhavenColors = staticCompositionLocalOf<AxWhavenColors> {
    getThemeColors(AppThemeMode.OBSIDIAN_VIOLET)
}

enum class AppThemeMode(
    val id: String,
    val title: String,
    val desc: String,
    val previewColors: List<Color>
) {
    OBSIDIAN_VIOLET(
        "obsidian",
        "AxWhaven Obsidian (Default)",
        "Deep space obsidian with vibrant electric lavender accents",
        listOf(Color(0xFF090A0F), Color(0xFF1A1D2E), Color(0xFF9C7AFF))
    ),
    NORD_DARK(
        "nord",
        "Nord-inspired Dark",
        "Cool muted polar blue-gray with frost cyan accents",
        listOf(Color(0xFF242933), Color(0xFF3B4252), Color(0xFF88C0D0))
    ),
    AMOLED_BLACK(
        "amoled",
        "AMOLED True Black",
        "100% pitch-black background with high-contrast elements for OLEDs",
        listOf(Color(0xFF000000), Color(0xFF141416), Color(0xFFA855F7))
    ),
    CYBERPUNK(
        "cyberpunk",
        "Cyberpunk / Neon",
        "High-energy dark synthwave with neon pink & cyan highlights",
        listOf(Color(0xFF080711), Color(0xFF1E173D), Color(0xFFFF007F))
    ),
    MINIMAL_LIGHT(
        "minimal_light",
        "Minimalist Light",
        "Crisp clean light aesthetic with slate accents",
        listOf(Color(0xFFF8F9FA), Color(0xFFEFF1F5), Color(0xFF7C3AED))
    ),
    FROSTED_GLASS(
        "frosted_glass",
        "Frosted Glassmorphism",
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
        AppThemeMode.OBSIDIAN_VIOLET -> AxWhavenColors(
            isLight = false,
            background = Color(0xFF090A0F),
            surface = Color(0xFF13161F),
            surfaceElevated = Color(0xFF1A1D2E),
            cardBorder = Color(0xFF232736),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFF8A90A6),
            textMuted = Color(0xFF8E95AB),
            primaryAccent = Color(0xFF9C7AFF),
            primaryAccentText = Color(0xFF0E0B1A),
            secondaryAccent = Color(0xFF987EEC),
            mintText = Color(0xFF34D399),
            mintPillBg = Color(0xFF173732),
            mintPillBorder = Color(0xFF23473F),
            surpriseGradientStart = Color(0xFF271D42),
            surpriseGradientEnd = Color(0xFF141824),
            surpriseGradientStroke = Color(0xFF2B2544),
            surpriseIconBg = Color(0xFF37245C),
            bottomNavBg = Color(0xF010121B),
            bottomNavBorder = Color(0xFF1F2332),
            glassFill = Color(0x33FFFFFF),
            glassBorder = Color(0x4DFFFFFF),
            scrimEnd = Color(0xD9090A0F)
        )
        AppThemeMode.NORD_DARK -> AxWhavenColors(
            isLight = false,
            background = Color(0xFF242933),
            surface = Color(0xFF2E3440),
            surfaceElevated = Color(0xFF3B4252),
            cardBorder = Color(0xFF434C5E),
            textPrimary = Color(0xFFECEFF4),
            textSecondary = Color(0xFFD8DEE9),
            textMuted = Color(0xFF9EA7B8),
            primaryAccent = Color(0xFF88C0D0),
            primaryAccentText = Color(0xFF2E3440),
            secondaryAccent = Color(0xFF81A1C1),
            mintText = Color(0xFFA3BE8C),
            mintPillBg = Color(0xFF2C3933),
            mintPillBorder = Color(0xFF394C44),
            surpriseGradientStart = Color(0xFF2F3C4C),
            surpriseGradientEnd = Color(0xFF2A3340),
            surpriseGradientStroke = Color(0xFF434C5E),
            surpriseIconBg = Color(0xFF3E4F63),
            bottomNavBg = Color(0xF02B303C),
            bottomNavBorder = Color(0xFF3B4252),
            glassFill = Color(0x2CECEFF4),
            glassBorder = Color(0x40ECEFF4),
            scrimEnd = Color(0xD9242933)
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
        AppThemeMode.CYBERPUNK -> AxWhavenColors(
            isLight = false,
            background = Color(0xFF080711),
            surface = Color(0xFF110E24),
            surfaceElevated = Color(0xFF1E173D),
            cardBorder = Color(0xFF3A2165),
            textPrimary = Color(0xFFFEEFFF),
            textSecondary = Color(0xFFB997DF),
            textMuted = Color(0xFF866DA8),
            primaryAccent = Color(0xFFFF007F), // Neon Pink
            primaryAccentText = Color(0xFFFFFFFF),
            secondaryAccent = Color(0xFF00F0FF), // Neon Cyan
            mintText = Color(0xFF00F0FF),
            mintPillBg = Color(0xFF052B33),
            mintPillBorder = Color(0xFF0A4E5C),
            surpriseGradientStart = Color(0xFF38084A),
            surpriseGradientEnd = Color(0xFF0B1B36),
            surpriseGradientStroke = Color(0xFFFF007F),
            surpriseIconBg = Color(0xFF4E0E68),
            bottomNavBg = Color(0xF20F0922),
            bottomNavBorder = Color(0xFF3A2165),
            glassFill = Color(0x2BFF007F),
            glassBorder = Color(0x66FF007F),
            scrimEnd = Color(0xDE080711)
        )
        AppThemeMode.MINIMAL_LIGHT -> AxWhavenColors(
            isLight = true,
            background = Color(0xFFF8F9FA),
            surface = Color(0xFFFFFFFF),
            surfaceElevated = Color(0xFFEFF1F5),
            cardBorder = Color(0xFFE2E4E9),
            textPrimary = Color(0xFF1A1C20),
            textSecondary = Color(0xFF5F6575),
            textMuted = Color(0xFF7E8494),
            primaryAccent = Color(0xFF7C3AED),
            primaryAccentText = Color(0xFFFFFFFF),
            secondaryAccent = Color(0xFF6D28D9),
            mintText = Color(0xFF059669),
            mintPillBg = Color(0xFFE6F4EA),
            mintPillBorder = Color(0xFFB7E1CD),
            surpriseGradientStart = Color(0xFFEDE9FE),
            surpriseGradientEnd = Color(0xFFF1F5F9),
            surpriseGradientStroke = Color(0xFFDDD6FE),
            surpriseIconBg = Color(0xFFDDD6FE),
            bottomNavBg = Color(0xF4FFFFFF),
            bottomNavBorder = Color(0xFFE2E4E9),
            glassFill = Color(0x55FFFFFF),
            glassBorder = Color(0x99FFFFFF),
            scrimEnd = Color(0xCC090A0F) // Keep dark scrim for wallpaper text contrast
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
