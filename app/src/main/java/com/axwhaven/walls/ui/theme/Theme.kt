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

@Composable
fun AxWhavenDynamicTheme(
    themeMode: AppThemeMode,
    content: @Composable (AxWhavenColors) -> Unit
) {
    val context = LocalContext.current
    val baseAxColors = getThemeColors(themeMode)
    val view = LocalView.current

    val (materialColors, axColors) = if (themeMode == AppThemeMode.EXPRESSIVE) {
        val dynamicColors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicDarkColorScheme(context)
        } else {
            darkColorScheme(
                primary = Color(0xFFD0BCFF),
                onPrimary = Color(0xFF381E72),
                primaryContainer = Color(0xFF4F378B),
                onPrimaryContainer = Color(0xFFEADDFF),
                secondary = Color(0xFF4ED8E8),
                onSecondary = Color(0xFF00363D),
                secondaryContainer = Color(0xFF004F58),
                onSecondaryContainer = Color(0xFF88F2FF),
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
        }

        val resolvedAx = baseAxColors.copy(
            background = dynamicColors.background,
            surface = dynamicColors.surface,
            surfaceElevated = dynamicColors.surfaceContainerHigh,
            cardBorder = dynamicColors.outlineVariant.copy(alpha = 0.35f),
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
            bottomNavBorder = dynamicColors.outlineVariant.copy(alpha = 0.35f),
            isExpressive = true
        )
        Pair(dynamicColors, resolvedAx)
    } else {
        val colors = if (baseAxColors.isLight) {
            lightColorScheme(
                primary = baseAxColors.primaryAccent,
                onPrimary = baseAxColors.primaryAccentText,
                secondary = baseAxColors.secondaryAccent,
                background = baseAxColors.background,
                surface = baseAxColors.surface,
                surfaceVariant = baseAxColors.surfaceElevated,
                onSurface = baseAxColors.textPrimary,
                outline = baseAxColors.cardBorder
            )
        } else {
            darkColorScheme(
                primary = baseAxColors.primaryAccent,
                onPrimary = baseAxColors.primaryAccentText,
                secondary = baseAxColors.secondaryAccent,
                background = baseAxColors.background,
                surface = baseAxColors.surface,
                surfaceVariant = baseAxColors.surfaceElevated,
                onSurface = baseAxColors.textPrimary,
                outline = baseAxColors.cardBorder
            )
        }
        Pair(colors, baseAxColors)
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
