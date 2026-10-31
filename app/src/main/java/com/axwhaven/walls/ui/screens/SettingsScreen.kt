package com.axwhaven.walls.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
import kotlin.math.abs

@Composable
fun SettingsScreen(
    uiState: WallpaperUiState,
    colors: AxWhavenColors,
    onThemeSelect: (AppThemeMode) -> Unit,
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

    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomClearance = 110.dp + navBottomInset

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
            } else if (colors.isExpressive) {
                Modifier.border(
                    0.5.dp,
                    colors.outlineVariant.copy(alpha = 0.25f),
                    outerCardShape
                )
            } else {
                Modifier.border(
                    1.dp,
                    colors.cardBorder,
                    outerCardShape
                )
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
            contentPadding = PaddingValues(top = 10.dp, bottom = bottomClearance)
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
                    Column(modifier = Modifier.padding(14.dp)) {
                        AppThemeMode.values().forEach { mode ->
                            val isSelected = uiState.currentTheme == mode
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) {
                                            if (colors.isGlassmorphism) Color(0x38FFFFFF)
                                            else if (colors.isExpressive) colors.surfaceContainerHigh
                                            else colors.surfaceElevated
                                        } else Color.Transparent
                                    )
                                    .then(
                                        if (isSelected && colors.isGlassmorphism) {
                                            Modifier.border(0.8.dp, Color(0x60FFFFFF), RoundedCornerShape(14.dp))
                                        } else Modifier
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, color = colors.primaryAccent)
                                    ) { onThemeSelect(mode) }
                                    .padding(12.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onThemeSelect(mode) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = colors.primaryAccent,
                                        unselectedColor = colors.textSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = mode.title,
                                            fontSize = 14.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) colors.primaryAccent else colors.textPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        // Visual Color Palette Swatches
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            mode.previewColors.forEach { swatch ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(swatch)
                                                        .border(0.5.dp, colors.cardBorder, CircleShape)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = mode.desc,
                                        fontSize = 12.sp,
                                        color = colors.textSecondary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                            if (mode != AppThemeMode.values().last()) {
                                HorizontalDivider(
                                    color = colors.cardBorder.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
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
                            // Active blur badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.primaryAccent.copy(alpha = 0.18f))
                                    .border(1.dp, colors.primaryAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (uiState.backgroundBlurRadius <= 0f) "Off (0dp)" else "${uiState.backgroundBlurRadius.toInt()} dp",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primaryAccent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Interactive Blur Slider
                        Slider(
                            value = uiState.backgroundBlurRadius,
                            onValueChange = onBlurRadiusChange,
                            valueRange = 0f..60f,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.primaryAccent,
                                activeTrackColor = colors.primaryAccent,
                                inactiveTrackColor = if (colors.isGlassmorphism) Color(0x38FFFFFF) else colors.cardBorder
                            ),
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
                                val isSelected = abs(uiState.backgroundBlurRadius - presetVal) < 2f
                                ExpressiveFilterChip(
                                    selected = isSelected,
                                    label = presetLabel,
                                    colors = colors,
                                    onClick = { onBlurRadiusChange(presetVal) },
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
                                                if (uiState.isUsingDefaultStorage) Color(0x2810B981)
                                                else colors.primaryAccent.copy(alpha = 0.2f)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (uiState.isUsingDefaultStorage) "Default" else "Custom",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (uiState.isUsingDefaultStorage) Color(0xFF10B981) else colors.primaryAccent
                                        )
                                    }
                                }
                                Text(
                                    text = uiState.activeStorageDesc.ifBlank { "Android/data/com.axwhaven.walls/files/wallpapers/" },
                                    fontSize = 11.5.sp,
                                    color = colors.mintText,
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
    val cornerRadius by animateDpAsState(
        targetValue = if (selected) 24.dp else 14.dp,
        animationSpec = ExpressiveMotion.FastSpatialDp,
        label = "FilterChipCornerRadius"
    )
    val safeRadius = cornerRadius.coerceAtLeast(0.dp)
    val shape = RoundedCornerShape(safeRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
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
                if (selected) colors.primaryAccent
                else if (colors.isGlassmorphism) Color(0x28FFFFFF)
                else if (colors.isExpressive) colors.surfaceContainerHigh
                else colors.surfaceElevated
            )
            .border(
                if (colors.isExpressive && !selected) 0.5.dp else 1.dp,
                if (selected) colors.primaryAccent
                else if (colors.isGlassmorphism) Color(0x50FFFFFF)
                else if (colors.isExpressive) colors.outlineVariant.copy(alpha = 0.3f)
                else colors.cardBorder,
                shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = colors.primaryAccent),
                onClick = onClick
            )
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) colors.primaryAccentText else colors.textPrimary,
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
        isDestructive -> Color(0xFFEF4444)
        colors.isExpressive -> colors.primaryAccent
        else -> colors.primaryAccent
    }

    val containerColor = when {
        isDestructive -> if (colors.isGlassmorphism) Color(0x40EF4444) else Color(0x26EF4444)
        colors.isGlassmorphism -> Color(0x35FFFFFF)
        colors.isExpressive -> colors.surfaceContainerHigh
        else -> colors.surfaceElevated
    }

    val borderStroke = when {
        isDestructive -> BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
        colors.isGlassmorphism -> BorderStroke(1.dp, Color(0x60FFFFFF))
        colors.isExpressive -> BorderStroke(0.5.dp, colors.outlineVariant.copy(alpha = 0.25f))
        else -> BorderStroke(1.dp, colors.cardBorder)
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
