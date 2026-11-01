package com.axwhaven.walls.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import android.content.Context
import androidx.compose.material3.ColorScheme

@Composable
fun AxWhavenDynamicTheme(
    themeMode: AppThemeMode,
    paletteSeed: PixelPaletteSeed = PixelPaletteSeed.SYSTEM,
    content: @Composable (AxWhavenColors) -> Unit
) {
    val context = LocalContext.current
    val baseAxColors = getThemeColors(themeMode)
    val view = LocalView.current

    val (materialColors, axColors) = when (themeMode) {
        AppThemeMode.EXPRESSIVE -> {
            val dynamicColors = getSeedColorScheme(paletteSeed, isAmoled = false, context = context)
            val resolvedAx = baseAxColors.copy(
                background = dynamicColors.surfaceContainerLowest,
                surface = dynamicColors.surface,
                surfaceElevated = dynamicColors.surfaceContainerHigh,
                cardBorder = dynamicColors.outlineVariant.copy(alpha = 0.25f),
                textPrimary = dynamicColors.onSurface,
                textSecondary = dynamicColors.onSurfaceVariant,
                primaryAccent = dynamicColors.primary,
                primaryAccentText = dynamicColors.onPrimary,
                secondaryAccent = dynamicColors.secondary,
                surfaceContainerLowest = dynamicColors.surfaceContainerLowest,
                surfaceContainerLow = dynamicColors.surfaceContainerLow,
                surfaceContainer = dynamicColors.surfaceContainer,
                surfaceContainerHigh = dynamicColors.surfaceContainerHigh,
                surfaceContainerHighest = dynamicColors.surfaceContainerHighest,
                outlineVariant = dynamicColors.outlineVariant,
                bottomNavBg = dynamicColors.surfaceContainer.copy(alpha = 0.95f),
                bottomNavBorder = dynamicColors.outlineVariant.copy(alpha = 0.25f),
                isExpressive = true
            )
            Pair(dynamicColors, resolvedAx)
        }
        AppThemeMode.AMOLED_BLACK -> {
            val amoledColors = getSeedColorScheme(paletteSeed, isAmoled = true, context = context)
            val resolvedAx = baseAxColors.copy(
                background = Color(0xFF000000),
                surface = Color(0xFF080808),
                surfaceElevated = Color(0xFF141416),
                cardBorder = Color(0xFF222228),
                textPrimary = Color(0xFFFFFFFF),
                textSecondary = Color(0xFFAAAAAA),
                primaryAccent = amoledColors.primary,
                primaryAccentText = amoledColors.onPrimary,
                secondaryAccent = amoledColors.secondary,
                surfaceContainerLowest = Color(0xFF000000),
                surfaceContainerLow = Color(0xFF080808),
                surfaceContainer = Color(0xFF0E0E0E),
                surfaceContainerHigh = Color(0xFF141416),
                surfaceContainerHighest = Color(0xFF1A1A1E),
                outlineVariant = Color(0xFF222228),
                bottomNavBg = Color(0xF2050507),
                bottomNavBorder = Color(0xFF18181D)
            )
            Pair(amoledColors, resolvedAx)
        }
        AppThemeMode.FROSTED_GLASS -> {
            val colors = darkColorScheme(
                primary = baseAxColors.primaryAccent,
                onPrimary = baseAxColors.primaryAccentText,
                secondary = baseAxColors.secondaryAccent,
                background = baseAxColors.background,
                surface = baseAxColors.surface,
                surfaceVariant = baseAxColors.surfaceElevated,
                onSurface = baseAxColors.textPrimary,
                outline = baseAxColors.cardBorder
            )
            Pair(colors, baseAxColors)
        }
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = axColors.isLight
            controller.isAppearanceLightNavigationBars = axColors.isLight
        }
    }

    MaterialTheme(
        colorScheme = materialColors,
        typography = Typography
    ) {
        CompositionLocalProvider(LocalAxWhavenColors provides axColors) {
            content(axColors)
        }
    }
}

private fun getSeedColorScheme(
    seed: PixelPaletteSeed,
    isAmoled: Boolean,
    context: Context
): ColorScheme {
    val base = when (seed) {
        PixelPaletteSeed.SYSTEM -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(context)
            } else {
                getElectricLavenderColorScheme()
            }
        }
        PixelPaletteSeed.LAVENDER -> getElectricLavenderColorScheme()
        PixelPaletteSeed.GOLDEN_AMBER -> getGoldenAmberColorScheme()
        PixelPaletteSeed.SAKURA_ROSE -> getSakuraRoseColorScheme()
        PixelPaletteSeed.CYBER_CYAN -> getCyberCyanColorScheme()
        PixelPaletteSeed.MONOCHROME -> getMonochromeColorScheme()
    }

    return if (isAmoled) {
        base.copy(
            background = Color(0xFF000000),
            surface = Color(0xFF000000),
            surfaceContainerLowest = Color(0xFF000000),
            surfaceContainerLow = Color(0xFF080808),
            surfaceContainer = Color(0xFF0E0E0E),
            surfaceContainerHigh = Color(0xFF141416),
            surfaceContainerHighest = Color(0xFF1A1A1E),
            outlineVariant = Color(0xFF222228)
        )
    } else {
        base
    }
}

private fun getElectricLavenderColorScheme() = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFF88F2FF),
    onTertiary = Color(0xFF00363D),
    tertiaryContainer = Color(0xFF004F58),
    onTertiaryContainer = Color(0xFFA6EEFF),
    background = Color(0xFF141218),
    surface = Color(0xFF141218),
    surfaceVariant = Color(0xFF49454F),
    surfaceContainerLowest = Color(0xFF0F0D13),
    surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B),
    onSurface = Color(0xFFE6E0E9),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

private fun getGoldenAmberColorScheme() = darkColorScheme(
    primary = Color(0xFFE6C48D),
    onPrimary = Color(0xFF422C05),
    primaryContainer = Color(0xFF5B4319),
    onPrimaryContainer = Color(0xFFFFDEAC),
    secondary = Color(0xFFD7C3A1),
    onSecondary = Color(0xFF3B2E16),
    secondaryContainer = Color(0xFF52442B),
    onSecondaryContainer = Color(0xFFF4DFBB),
    tertiary = Color(0xFFC7DE9B),
    onTertiary = Color(0xFF2E3814),
    tertiaryContainer = Color(0xFF445028),
    onTertiaryContainer = Color(0xFFE3FBB5),
    background = Color(0xFF16130E),
    surface = Color(0xFF16130E),
    surfaceVariant = Color(0xFF4C453C),
    surfaceContainerLowest = Color(0xFF110E0A),
    surfaceContainerLow = Color(0xFF1F1B15),
    surfaceContainer = Color(0xFF231F19),
    surfaceContainerHigh = Color(0xFF2E2923),
    surfaceContainerHighest = Color(0xFF39342D),
    onSurface = Color(0xFFEAE1D9),
    onSurfaceVariant = Color(0xFFCEC4B7),
    outline = Color(0xFF988F83),
    outlineVariant = Color(0xFF4C453C)
)

private fun getSakuraRoseColorScheme() = darkColorScheme(
    primary = Color(0xFFFFB1C8),
    onPrimary = Color(0xFF5E1133),
    primaryContainer = Color(0xFF7B2949),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFE5BDC6),
    onSecondary = Color(0xFF432931),
    secondaryContainer = Color(0xFF5C3F47),
    onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = Color(0xFFFFD9B3),
    onTertiary = Color(0xFF4A2800),
    tertiaryContainer = Color(0xFF693C00),
    onTertiaryContainer = Color(0xFFFFDCC1),
    background = Color(0xFF181113),
    surface = Color(0xFF181113),
    surfaceVariant = Color(0xFF504346),
    surfaceContainerLowest = Color(0xFF120C0E),
    surfaceContainerLow = Color(0xFF21171A),
    surfaceContainer = Color(0xFF251B1E),
    surfaceContainerHigh = Color(0xFF302528),
    surfaceContainerHighest = Color(0xFF3C3033),
    onSurface = Color(0xFFEFE0E2),
    onSurfaceVariant = Color(0xFFD3C2C5),
    outline = Color(0xFF9C8C90),
    outlineVariant = Color(0xFF504346)
)

private fun getCyberCyanColorScheme() = darkColorScheme(
    primary = Color(0xFF80D8FF),
    onPrimary = Color(0xFF003644),
    primaryContainer = Color(0xFF004D61),
    onPrimaryContainer = Color(0xFFBCE9FF),
    secondary = Color(0xFFB2CAD5),
    onSecondary = Color(0xFF1D333C),
    secondaryContainer = Color(0xFF334A53),
    onSecondaryContainer = Color(0xFFCEE6F1),
    tertiary = Color(0xFFB388FF),
    onTertiary = Color(0xFF2F007D),
    tertiaryContainer = Color(0xFF4A00B4),
    onTertiaryContainer = Color(0xFFEBDDFF),
    background = Color(0xFF0E1417),
    surface = Color(0xFF0E1417),
    surfaceVariant = Color(0xFF42474B),
    surfaceContainerLowest = Color(0xFF090F12),
    surfaceContainerLow = Color(0xFF171D20),
    surfaceContainer = Color(0xFF1B2124),
    surfaceContainerHigh = Color(0xFF252B2E),
    surfaceContainerHighest = Color(0xFF303639),
    onSurface = Color(0xFFDEE3E6),
    onSurfaceVariant = Color(0xFFC1C7CE),
    outline = Color(0xFF8B9297),
    outlineVariant = Color(0xFF42474B)
)

private fun getMonochromeColorScheme() = darkColorScheme(
    primary = Color(0xFFE2E2E6),
    onPrimary = Color(0xFF1A1C1E),
    primaryContainer = Color(0xFF45474C),
    onPrimaryContainer = Color(0xFFF1F0F4),
    secondary = Color(0xFFC6C6CA),
    onSecondary = Color(0xFF2F3033),
    secondaryContainer = Color(0xFF46474A),
    onSecondaryContainer = Color(0xFFE2E2E6),
    tertiary = Color(0xFFBDBDC2),
    onTertiary = Color(0xFF282A2D),
    tertiaryContainer = Color(0xFF3F4144),
    onTertiaryContainer = Color(0xFFDFDFE4),
    background = Color(0xFF121316),
    surface = Color(0xFF121316),
    surfaceVariant = Color(0xFF46474A),
    surfaceContainerLowest = Color(0xFF0C0E10),
    surfaceContainerLow = Color(0xFF1A1C1E),
    surfaceContainer = Color(0xFF1E2023),
    surfaceContainerHigh = Color(0xFF292A2D),
    surfaceContainerHighest = Color(0xFF333538),
    onSurface = Color(0xFFE2E2E6),
    onSurfaceVariant = Color(0xFFC6C6CA),
    outline = Color(0xFF8E9094),
    outlineVariant = Color(0xFF46474A)
)
