package com.axwhaven.walls.ui.screens

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.axwhaven.walls.WallpaperUiState
import com.axwhaven.walls.model.WallpaperSource
import com.axwhaven.walls.ui.components.WallpaperImage
import com.axwhaven.walls.ui.theme.AppThemeMode
import com.axwhaven.walls.ui.theme.AxWhavenColors
import com.axwhaven.walls.ui.theme.ExpressiveMotion
import com.axwhaven.walls.ui.theme.FloatingDockClearance
import com.axwhaven.walls.ui.theme.FrostyFastBadgeGreen
import com.axwhaven.walls.ui.theme.FrostyFastIconBoxBgGreen
import com.axwhaven.walls.ui.theme.PixelPaletteSeed
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: WallpaperUiState,
    colors: AxWhavenColors,
    onThemeSelect: (AppThemeMode) -> Unit,
    onPaletteSelect: (PixelPaletteSeed) -> Unit = {},
    onBlurRadiusChange: (Float) -> Unit,
    onSelectDownloadFolder: () -> Unit,
    onSetCustomPath: (String) -> Unit = {},
    onResetDownloadDirectory: () -> Unit = {},
    onClearAppBackground: () -> Unit,
    onCopyWallpapersToSafFolder: () -> Unit = {},
    onCopyWallpapersToPictures: () -> Unit = {},
    onRequestUninstallApp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCustomPathDialog by remember { mutableStateOf(false) }
    var showUninstallDialog by remember { mutableStateOf(false) }

    val haptics = LocalHapticFeedback.current
    val bottomClearance = FloatingDockClearance

    val outerCardShape = if (colors.isExpressive) RoundedCornerShape(28.dp) else RoundedCornerShape(22.dp)
    val outerCardContainerColor = if (colors.isGlassmorphism) Color(0x28FFFFFF) else if (colors.isExpressive) colors.surfaceContainerLow else colors.surface

    fun Modifier.outerCardStyle(verticalPadding: Dp = 8.dp): Modifier = this
        .fillMaxWidth()
        .padding(vertical = verticalPadding)
        .then(
            if (colors.isGlassmorphism) {
                Modifier.border(
                    1.dp,
                    Brush.verticalGradient(listOf(Color(0x65FFFFFF), Color(0x22FFFFFF))),
                    outerCardShape
                )
            } else {
                Modifier // Zero border in M3 Expressive & AMOLED
            }
        )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (uiState.appBackgroundItem != null || colors.isGlassmorphism) Color.Transparent else colors.background
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 160.dp)
        ) {
            // =========================================================================
            // SETTINGS HEADER
            // =========================================================================
            item {
                Text(
                    text = "Settings",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Appearance, storage & application preferences",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 18.dp)
                )
            }

            // =========================================================================
            // 1. THEME CUSTOMIZATION DASHBOARD (CURATED THEMES WITH FROSTED GLASS)
            // =========================================================================
            item {
                SettingsSectionHeader(title = "Theme & Appearance", icon = Icons.Default.Palette, colors = colors)
                Card(
                    shape = outerCardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = outerCardContainerColor
                    ),
                    modifier = Modifier.outerCardStyle()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        AppThemeMode.values().forEachIndexed { index, mode ->
                            val isSelected = uiState.currentTheme == mode

                            val topStartTarget = when {
                                isSelected -> 16.dp
                                index == 0 -> 20.dp
                                else -> 6.dp
                            }
                            val bottomStartTarget = when {
                                isSelected -> 16.dp
                                index == AppThemeMode.values().size - 1 -> 20.dp
                                else -> 6.dp
                            }

                            val animTopStart by animateDpAsState(
                                targetValue = topStartTarget,
                                animationSpec = ExpressiveMotion.FastSpatialDp,
                                label = "ThemeCornerTop_${mode.name}"
                            )
                            val animBottomStart by animateDpAsState(
                                targetValue = bottomStartTarget,
                                animationSpec = ExpressiveMotion.FastSpatialDp,
                                label = "ThemeCornerBottom_${mode.name}"
                            )

                            val subCardShape = RoundedCornerShape(
                                topStart = animTopStart.coerceAtLeast(0.dp),
                                topEnd = animTopStart.coerceAtLeast(0.dp),
                                bottomEnd = animBottomStart.coerceAtLeast(0.dp),
                                bottomStart = animBottomStart.coerceAtLeast(0.dp)
                            )

                            val subCardBg = when {
                                isSelected -> if (colors.isGlassmorphism) Color(0x38FFFFFF) else MaterialTheme.colorScheme.secondaryContainer
                                colors.isGlassmorphism -> Color(0x18FFFFFF)
                                colors.isExpressive -> colors.surfaceContainerHigh.copy(alpha = 0.45f)
                                else -> colors.surfaceElevated.copy(alpha = 0.6f)
                            }

                            val titleTextColor = when {
                                isSelected -> if (colors.isGlassmorphism) colors.primaryAccent else MaterialTheme.colorScheme.onSecondaryContainer
                                else -> colors.textPrimary
                            }
                            val descTextColor = when {
                                isSelected -> if (colors.isGlassmorphism) colors.textSecondary else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                else -> colors.textSecondary
                            }

                            val dynamicDots = when (mode) {
                                AppThemeMode.EXPRESSIVE -> listOf(
                                    MaterialTheme.colorScheme.surfaceContainer,
                                    colors.primaryAccent,
                                    MaterialTheme.colorScheme.tertiary
                                )
                                AppThemeMode.AMOLED_BLACK -> listOf(
                                    Color(0xFF000000),
                                    colors.primaryAccent,
                                    MaterialTheme.colorScheme.tertiary
                                )
                                AppThemeMode.FROSTED_GLASS -> mode.previewColors
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(subCardShape)
                                    .background(subCardBg)
                                    .then(
                                        if (isSelected && colors.isGlassmorphism) {
                                            Modifier.border(0.8.dp, Color(0x60FFFFFF), subCardShape)
                                        } else Modifier
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, color = colors.primaryAccent)
                                    ) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onThemeSelect(mode)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onThemeSelect(mode)
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = if (colors.isGlassmorphism) colors.primaryAccent else MaterialTheme.colorScheme.primary,
                                        unselectedColor = colors.textSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = mode.title,
                                            fontSize = 14.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = titleTextColor
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        // Visual Color Palette Swatches / Preview dots
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            dynamicDots.forEach { swatch ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(swatch)
                                                        .border(0.5.dp, colors.cardBorder.copy(alpha = 0.5f), CircleShape)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = mode.desc,
                                        fontSize = 12.sp,
                                        color = descTextColor,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }

                        // Pixel 3-Quadrant Palette Swatch Picker (Visible when EXPRESSIVE or AMOLED_BLACK)
                        if (uiState.currentTheme == AppThemeMode.EXPRESSIVE || uiState.currentTheme == AppThemeMode.AMOLED_BLACK) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(
                                color = colors.cardBorder.copy(alpha = 0.2f),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Color Palette",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Text(
                                text = "Google Pixel Material You dynamic palettes",
                                fontSize = 11.5.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 1.dp, bottom = 10.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PixelPaletteSeed.values().forEach { seed ->
                                    PixelPaletteSwatch(
                                        seed = seed,
                                        isSelected = uiState.currentPalette == seed,
                                        colors = colors,
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onPaletteSelect(seed)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 2. BACKGROUND BLUR & GLASSMORPHISM INTENSITY SLIDER
            // =========================================================================
            item {
                Spacer(modifier = Modifier.height(14.dp))
                SettingsSectionHeader(
                    title = "Background Blur & Glassmorphism",
                    icon = Icons.Default.BlurOn,
                    colors = colors
                )
                Card(
                    shape = outerCardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = outerCardContainerColor
                    ),
                    modifier = Modifier.outerCardStyle()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val sliderInteractionSource = remember { MutableInteractionSource() }
                        val isSliderDragged by sliderInteractionSource.collectIsDraggedAsState()
                        val isSliderPressed by sliderInteractionSource.collectIsPressedAsState()
                        val isInteracting = isSliderDragged || isSliderPressed
                        var lastTickValue by remember { mutableIntStateOf(uiState.backgroundBlurRadius.roundToInt()) }
                        var localBlurRadius by remember(uiState.backgroundBlurRadius) { mutableFloatStateOf(uiState.backgroundBlurRadius) }

                        val badgeScale by animateFloatAsState(
                            targetValue = if (isInteracting) 1.10f else 1.0f,
                            animationSpec = ExpressiveMotion.FastSpatialFloat,
                            label = "BlurBadgeScale"
                        )
                        val badgeCorner by animateDpAsState(
                            targetValue = if (isInteracting) 16.dp else 12.dp,
                            animationSpec = ExpressiveMotion.FastSpatialDp,
                            label = "BlurBadgeCorner"
                        )
                        val badgeShape = RoundedCornerShape(badgeCorner.coerceAtLeast(0.dp))
                        val badgeBg = if (colors.isGlassmorphism) {
                            colors.primaryAccent.copy(alpha = if (isInteracting) 0.32f else 0.18f)
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        }
                        val badgeBorder = if (colors.isGlassmorphism) {
                            Modifier.border(1.dp, colors.primaryAccent.copy(alpha = 0.4f), badgeShape)
                        } else Modifier

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (colors.isGlassmorphism) Color(0x30FFFFFF) else colors.surfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BlurOn,
                                    contentDescription = null,
                                    tint = colors.primaryAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Backdrop Blur Radius",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Diffuses wallpaper under glass panels",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            // Active blur badge (scale-pop 1.0f -> 1.10f, secondaryContainer fill, zero border in M3 Expressive)
                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = badgeScale
                                        scaleY = badgeScale
                                    }
                                    .clip(badgeShape)
                                    .background(badgeBg)
                                    .then(badgeBorder)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (localBlurRadius <= 0f) "Off (0dp)" else "${localBlurRadius.roundToInt()} dp",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (colors.isGlassmorphism) colors.primaryAccent else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Interactive Blur Slider with Expressive physics
                        val thumbHeight by animateDpAsState(
                            targetValue = if (isInteracting) 38.dp else 32.dp,
                            animationSpec = ExpressiveMotion.FastSpatialDp,
                            label = "BlurThumbHeight"
                        )
                        val thumbWidth by animateDpAsState(
                            targetValue = if (isInteracting) 6.dp else 4.dp,
                            animationSpec = ExpressiveMotion.FastSpatialDp,
                            label = "BlurThumbWidth"
                        )

                        Slider(
                            value = localBlurRadius,
                            onValueChange = { newVal ->
                                localBlurRadius = newVal
                                val intVal = newVal.roundToInt()
                                if (isInteracting && intVal != lastTickValue) {
                                    lastTickValue = intVal
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                                }
                                onBlurRadiusChange(newVal)
                            },
                            onValueChangeFinished = {
                                onBlurRadiusChange(localBlurRadius)
                            },
                            interactionSource = sliderInteractionSource,
                            valueRange = 0f..60f,
                            thumb = {
                                Box(
                                    modifier = Modifier
                                        .size(width = thumbWidth, height = thumbHeight)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (colors.isGlassmorphism) colors.primaryAccent else MaterialTheme.colorScheme.primary)
                                )
                            },
                            track = { sliderState ->
                                SliderDefaults.Track(
                                    sliderState = sliderState,
                                    colors = SliderDefaults.colors(
                                        thumbColor = if (colors.isGlassmorphism) colors.primaryAccent else MaterialTheme.colorScheme.primary,
                                        activeTrackColor = if (colors.isGlassmorphism) colors.primaryAccent else MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = if (colors.isGlassmorphism) Color(0x38FFFFFF) else MaterialTheme.colorScheme.surfaceContainerHighest
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Preset Chips: Sharp (0dp), Soft (15dp), Frosted (25dp), Heavy (45dp)
                        val presets = listOf(
                            0f to "0 dp (Sharp)",
                            15f to "15 dp (Soft)",
                            25f to "25 dp (Glass)",
                            45f to "45 dp (Heavy)"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presets.forEach { (presetVal, presetLabel) ->
                                val isSelected = abs(localBlurRadius - presetVal) < 2f
                                ExpressiveFilterChip(
                                    selected = isSelected,
                                    label = presetLabel,
                                    colors = colors,
                                    onClick = {
                                        localBlurRadius = presetVal
                                        lastTickValue = presetVal.roundToInt()
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onBlurRadiusChange(presetVal)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 3. APP BACKGROUND WALLPAPER MANAGEMENT
            // =========================================================================
            item {
                Spacer(modifier = Modifier.height(14.dp))
                SettingsSectionHeader(title = "App Background Wallpaper", icon = Icons.Default.Wallpaper, colors = colors)
                Card(
                    shape = outerCardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = outerCardContainerColor
                    ),
                    modifier = Modifier.outerCardStyle()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (uiState.appBackgroundItem != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(
                                            1.dp,
                                            if (colors.isGlassmorphism) Color(0x66FFFFFF) else colors.cardBorder,
                                            RoundedCornerShape(14.dp)
                                        )
                                ) {
                                    WallpaperImage(
                                        source = uiState.appBackgroundItem.source,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Active App Wallpaper",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = uiState.appBackgroundItem.title,
                                        fontSize = 12.sp,
                                        color = colors.mintText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            ExpressiveActionButton(
                                onClick = onClearAppBackground,
                                text = "Reset to Default Canvas",
                                icon = Icons.Default.DeleteOutline,
                                colors = colors,
                                isDestructive = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (colors.isGlassmorphism) Color(0x30FFFFFF) else colors.surfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = colors.primaryAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Default Canvas Backdrop",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "No custom wallpaper active",
                                        fontSize = 12.sp,
                                        color = colors.textSecondary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "To set an anime wallpaper as the app's internal backdrop, tap any wallpaper to preview it, choose 'Apply Wallpaper', and select 'Set as App Background'.",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 4. STORAGE & DOWNLOAD DESTINATION
            // =========================================================================
            item {
                Spacer(modifier = Modifier.height(14.dp))
                SettingsSectionHeader(title = "Storage & Downloads", icon = Icons.Default.Folder, colors = colors)
                Card(
                    shape = outerCardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = outerCardContainerColor
                    ),
                    modifier = Modifier.outerCardStyle()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (colors.isGlassmorphism) Color(0x30FFFFFF) else colors.surfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderSpecial,
                                    contentDescription = null,
                                    tint = colors.primaryAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Active Storage Vault",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (colors.isGlassmorphism) {
                                                    if (uiState.isUsingDefaultStorage) FrostyFastIconBoxBgGreen
                                                    else colors.primaryAccent.copy(alpha = 0.2f)
                                                } else {
                                                    if (uiState.isUsingDefaultStorage) MaterialTheme.colorScheme.tertiaryContainer
                                                    else MaterialTheme.colorScheme.secondaryContainer
                                                }
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (uiState.isUsingDefaultStorage) "Default" else "Custom",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (colors.isGlassmorphism) {
                                                if (uiState.isUsingDefaultStorage) FrostyFastBadgeGreen else colors.primaryAccent
                                            } else {
                                                if (uiState.isUsingDefaultStorage) MaterialTheme.colorScheme.onTertiaryContainer
                                                else MaterialTheme.colorScheme.onSecondaryContainer
                                            }
                                        )
                                    }
                                }
                                Text(
                                    text = uiState.activeStorageDesc.ifBlank { "Android/data/com.axwhaven.walls/files/wallpapers/" },
                                    fontSize = 11.5.sp,
                                    color = if (colors.isGlassmorphism) colors.mintText else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Actions Row: Choose Folder (SAF) & Enter Custom Path
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExpressiveActionButton(
                                onClick = onSelectDownloadFolder,
                                text = "Select Folder",
                                icon = Icons.Default.FolderOpen,
                                colors = colors,
                                modifier = Modifier.weight(1f)
                            )

                            ExpressiveActionButton(
                                onClick = { showCustomPathDialog = true },
                                text = "Enter Path",
                                icon = Icons.Default.Edit,
                                colors = colors,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (!uiState.isUsingDefaultStorage) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = onResetDownloadDirectory,
                                modifier = Modifier.align(Alignment.End),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = null,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Reset to Default (Android/data)",
                                    fontSize = 11.5.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "New downloads are saved exclusively to the active directory. The previous folder is no longer linked.",
                            fontSize = 11.5.sp,
                            color = colors.textSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Pre-Uninstall Wallpapers Backup & Safe Uninstall Card
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    shape = outerCardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = outerCardContainerColor
                    ),
                    modifier = Modifier.outerCardStyle(verticalPadding = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEAB308).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color(0xFFEAB308),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pre-Uninstall Backup",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Preserve wallpapers before uninstalling",
                                    fontSize = 11.5.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Default Android/data storage (${uiState.defaultStorageWallpapersCount} wallpapers) is wiped by Android upon app uninstall unless preserved. You can copy all wallpapers to Pictures or a folder of your choice.",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExpressiveActionButton(
                                onClick = onCopyWallpapersToPictures,
                                text = "Copy to Pictures",
                                icon = Icons.Default.SaveAlt,
                                colors = colors,
                                fontSize = 11.5.sp,
                                modifier = Modifier.weight(1f)
                            )

                            ExpressiveActionButton(
                                onClick = onCopyWallpapersToSafFolder,
                                text = "Choose Folder...",
                                icon = Icons.Default.FolderOpen,
                                colors = colors,
                                fontSize = 11.5.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        ExpressiveActionButton(
                            onClick = { showUninstallDialog = true },
                            text = "Uninstall AxWhaven...",
                            icon = Icons.Default.DeleteForever,
                            colors = colors,
                            isDestructive = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // =========================================================================
            // 5. ABOUT & APPLICATION DETAILS
            // =========================================================================
            item {
                Spacer(modifier = Modifier.height(14.dp))
                SettingsSectionHeader(title = "About", icon = Icons.Default.Info, colors = colors)
                Card(
                    shape = outerCardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = outerCardContainerColor
                    ),
                    modifier = Modifier.outerCardStyle()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (colors.isGlassmorphism) Color(0x30FFFFFF) else colors.surfaceElevated)
                                    .border(
                                        1.dp,
                                        if (colors.isGlassmorphism) Color(0x60FFFFFF) else colors.cardBorder,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = com.axwhaven.walls.R.drawable.app_icon),
                                    contentDescription = "AxWhaven Icon",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "AxWhaven",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Version 1.0 • Modern Anime Wallpapers",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(
                            color = if (colors.isGlassmorphism) Color(0x33FFFFFF) else colors.cardBorder.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Curated anime art discovery powered by 8 community providers (Wallhaven, Pixiv, Gelbooru, Safebooru, Danbooru, Yande.re, Konachan, Anime-Pictures) with native lock/home screen wallpaper application.",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(160.dp).navigationBarsPadding())
            }
        }

        // Custom Folder Path Dialog
        if (showCustomPathDialog) {
            var pathText by remember {
                mutableStateOf(
                    if (!uiState.isUsingDefaultStorage && uiState.activeStorageDesc.startsWith("/")) {
                        uiState.activeStorageDesc
                    } else {
                        "/storage/emulated/0/Download/AxWhaven"
                    }
                )
            }

            Dialog(
                onDismissRequest = { showCustomPathDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x8A000000))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showCustomPathDialog = false }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (colors.isGlassmorphism) Color(0xF2181C2E) else colors.surfaceElevated
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (colors.isGlassmorphism) Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x22FFFFFF)))
                            else Brush.linearGradient(listOf(colors.cardBorder, colors.cardBorder))
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clickable(enabled = false, onClick = {})
                    ) {
                        Column(modifier = Modifier.padding(22.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = colors.primaryAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Enter Storage Path",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Specify any folder path on your device. The app will save new downloads and load your vault from here.",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = pathText,
                                onValueChange = { pathText = it },
                                label = { Text("Folder Path") },
                                placeholder = { Text("/storage/emulated/0/Download/Wallpapers") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.primaryAccent,
                                    unfocusedBorderColor = colors.cardBorder,
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showCustomPathDialog = false }) {
                                    Text("Cancel", color = colors.textSecondary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        showCustomPathDialog = false
                                        onSetCustomPath(pathText)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primaryAccent,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Apply Path")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Safe Uninstall & Backup Dialog
        if (showUninstallDialog) {
            Dialog(
                onDismissRequest = { showUninstallDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x8A000000))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showUninstallDialog = false }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (colors.isGlassmorphism) Color(0xF2181C2E) else colors.surfaceElevated
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (colors.isGlassmorphism) Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x22FFFFFF)))
                            else Brush.linearGradient(listOf(colors.cardBorder, colors.cardBorder))
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clickable(enabled = false, onClick = {})
                    ) {
                        Column(modifier = Modifier.padding(22.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Uninstall AxWhaven",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (uiState.isUsingDefaultStorage && uiState.defaultStorageWallpapersCount > 0) {
                                    "You have ${uiState.defaultStorageWallpapersCount} wallpapers in default Android/data storage. Would you like to copy them to Pictures before uninstalling so they aren't lost?"
                                } else {
                                    "Are you sure you want to proceed with uninstalling AxWhaven?"
                                },
                                fontSize = 13.sp,
                                color = colors.textSecondary,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            if (uiState.isUsingDefaultStorage && uiState.defaultStorageWallpapersCount > 0) {
                                Button(
                                    onClick = {
                                        showUninstallDialog = false
                                        onCopyWallpapersToPictures()
                                        onRequestUninstallApp()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primaryAccent,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy to Pictures & Uninstall", fontWeight = FontWeight.SemiBold)
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showUninstallDialog = false }) {
                                    Text("Cancel", color = colors.textSecondary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = {
                                        showUninstallDialog = false
                                        onRequestUninstallApp()
                                    }
                                ) {
                                    Text("Uninstall Directly", color = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpressiveFilterChip(
    selected: Boolean,
    label: String,
    colors: AxWhavenColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current

    val targetRadius = when {
        isPressed -> 8.dp
        selected -> 28.dp
        else -> 12.dp
    }
    val cornerRadius by animateDpAsState(
        targetValue = targetRadius,
        animationSpec = ExpressiveMotion.FastSpatialDp,
        label = "FilterChipCornerRadius"
    )
    val safeRadius = cornerRadius.coerceAtLeast(0.dp)
    val shape = RoundedCornerShape(safeRadius)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = ExpressiveMotion.FastSpatialFloat,
        label = "FilterChipScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(
                if (selected) {
                    if (colors.isGlassmorphism) colors.primaryAccent else MaterialTheme.colorScheme.secondaryContainer
                } else if (colors.isGlassmorphism) Color(0x28FFFFFF)
                else if (colors.isExpressive) colors.surfaceContainerHigh
                else colors.surfaceElevated
            )
            .then(
                if (colors.isGlassmorphism) {
                    Modifier.border(
                        1.dp,
                        if (selected) colors.primaryAccent else Color(0x50FFFFFF),
                        shape
                    )
                } else {
                    Modifier // Zero border in M3 Expressive
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = colors.primaryAccent),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) {
                if (colors.isGlassmorphism) colors.primaryAccentText else MaterialTheme.colorScheme.onSecondaryContainer
            } else colors.textPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun ExpressiveActionButton(
    onClick: () -> Unit,
    text: String,
    icon: ImageVector,
    colors: AxWhavenColors,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 24.dp else 14.dp,
        animationSpec = ExpressiveMotion.FastSpatialDp,
        label = "ActionButtonCornerRadius"
    )
    val safeRadius = cornerRadius.coerceAtLeast(0.dp)
    val shape = RoundedCornerShape(safeRadius)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = ExpressiveMotion.FastSpatialFloat,
        label = "ActionButtonScale"
    )

    val contentColor = when {
        isDestructive -> {
            if (colors.isGlassmorphism) Color(0xFFEF4444)
            else if (colors.isExpressive) MaterialTheme.colorScheme.onErrorContainer
            else Color(0xFFEF4444)
        }
        colors.isExpressive -> colors.primaryAccent
        else -> colors.primaryAccent
    }

    val containerColor = when {
        isDestructive -> {
            if (colors.isGlassmorphism) Color(0x40EF4444)
            else if (colors.isExpressive) MaterialTheme.colorScheme.errorContainer
            else Color(0x26EF4444)
        }
        colors.isGlassmorphism -> Color(0x35FFFFFF)
        colors.isExpressive -> colors.surfaceContainerHigh
        else -> colors.surfaceElevated
    }

    val borderStroke = when {
        colors.isGlassmorphism -> BorderStroke(1.dp, if (isDestructive) Color(0xFFEF4444).copy(alpha = 0.5f) else Color(0x60FFFFFF))
        else -> null
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = borderStroke,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(
    title: String,
    icon: ImageVector,
    colors: AxWhavenColors
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.primaryAccent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
    }
}

@Composable
private fun PixelPaletteSwatch(
    seed: PixelPaletteSeed,
    isSelected: Boolean,
    colors: AxWhavenColors,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val cornerRadius by animateDpAsState(
        targetValue = if (isSelected) 12.dp else 20.dp,
        animationSpec = ExpressiveMotion.FastSpatialDp,
        label = "SwatchCornerRadius_${seed.name}"
    )
    val swatchShape = RoundedCornerShape(cornerRadius.coerceAtLeast(0.dp))

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.94f,
        animationSpec = ExpressiveMotion.FastSpatialFloat,
        label = "SwatchScale_${seed.name}"
    )

    val outerCornerRadius by animateDpAsState(
        targetValue = if (isSelected) 15.dp else 23.dp,
        animationSpec = ExpressiveMotion.FastSpatialDp,
        label = "SwatchOuterCorner_${seed.name}"
    )
    val outerBorderShape = RoundedCornerShape(outerCornerRadius.coerceAtLeast(0.dp))

    val (primaryColor, secContainerColor, tertColor) = remember(seed, context) {
        if (seed == PixelPaletteSeed.SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val dyn = dynamicDarkColorScheme(context)
                Triple(dyn.primary, dyn.secondaryContainer, dyn.tertiary)
            } catch (e: Exception) {
                Triple(seed.primary, seed.secondaryContainer, seed.tertiary)
            }
        } else {
            Triple(seed.primary, seed.secondaryContainer, seed.tertiary)
        }
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (isSelected) {
                    Modifier
                        .border(2.dp, MaterialTheme.colorScheme.primary, outerBorderShape)
                        .padding(2.5.dp)
                } else {
                    Modifier.padding(2.5.dp)
                }
            )
            .clip(swatchShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.primaryAccent),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = primaryColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true
            )
            drawArc(
                color = secContainerColor,
                startAngle = 90f,
                sweepAngle = 90f,
                useCenter = true
            )
            drawArc(
                color = tertColor,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = true
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(17.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}
