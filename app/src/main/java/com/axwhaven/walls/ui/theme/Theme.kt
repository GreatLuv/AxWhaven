package com.axwhaven.walls.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun AxWhavenDynamicTheme(
    themeMode: AppThemeMode,
    content: @Composable (AxWhavenColors) -> Unit
) {
    val axColors = getThemeColors(themeMode)
    val view = LocalView.current

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

    val materialColors = if (axColors.isLight) {
        lightColorScheme(
            primary = axColors.primaryAccent,
            onPrimary = axColors.primaryAccentText,
            secondary = axColors.secondaryAccent,
            background = axColors.background,
            surface = axColors.surface,
            surfaceVariant = axColors.surfaceElevated,
            onSurface = axColors.textPrimary,
            outline = axColors.cardBorder
        )
    } else {
        darkColorScheme(
            primary = axColors.primaryAccent,
            onPrimary = axColors.primaryAccentText,
            secondary = axColors.secondaryAccent,
            background = axColors.background,
            surface = axColors.surface,
            surfaceVariant = axColors.surfaceElevated,
            onSurface = axColors.textPrimary,
            outline = axColors.cardBorder
        )
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
