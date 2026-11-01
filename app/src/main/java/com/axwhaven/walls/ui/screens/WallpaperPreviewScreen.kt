package com.axwhaven.walls.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.axwhaven.walls.model.ScaleMode
import com.axwhaven.walls.model.WallpaperItem
import com.axwhaven.walls.model.WallpaperTarget
import com.axwhaven.walls.ui.components.WallpaperImage
import com.axwhaven.walls.ui.theme.AxWhavenColors
import com.axwhaven.walls.ui.theme.ExpressiveMotion
import com.axwhaven.walls.ui.theme.LocalAxWhavenColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun WallpaperPreviewScreen(
    item: WallpaperItem,
    onClose: () -> Unit,
    onApply: (ScaleMode, WallpaperTarget) -> Unit,
    onLaunchSystemPicker: (Activity, WallpaperItem) -> Unit,
    colors: AxWhavenColors = LocalAxWhavenColors.current,
    onSetAppBackground: ((WallpaperItem) -> Unit)? = null
) {
    val activity = LocalContext.current as? Activity
    var selectedScale by remember { mutableStateOf(ScaleMode.FILL) }
    var showTargetModal by remember { mutableStateOf(false) }
    var isControlsVisible by remember { mutableStateOf(true) }

    // Intercept back navigation / gesture to dismiss preview
    BackHandler {
        onClose()
    }

    // Root Container: Edge-to-edge full screen with pitch-black base
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        // 1. Full-screen dynamic canvas with Fill vs Fit (Tap to toggle UI controls)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { isControlsVisible = !isControlsVisible }
                )
        ) {
            AnimatedContent(
                targetState = selectedScale,
                transitionSpec = {
                    (fadeIn(animationSpec = ExpressiveMotion.DefaultEffectsFloat) +
                            scaleIn(
                                initialScale = if (targetState == ScaleMode.FILL) 0.88f else 1.12f,
                                animationSpec = ExpressiveMotion.DefaultSpatialFloat
                            )
                    ).togetherWith(
                        fadeOut(animationSpec = ExpressiveMotion.DefaultEffectsFloat) +
                                scaleOut(
                                    targetScale = if (targetState == ScaleMode.FILL) 1.12f else 0.88f,
                                    animationSpec = ExpressiveMotion.DefaultSpatialFloat
                                )
                    )
                },
                label = "CanvasScaleTransition"
            ) { scale ->
                WallpaperImage(
                    source = item.source,
                    contentDescription = item.title,
                    contentScale = if (scale == ScaleMode.FILL) ContentScale.Crop else ContentScale.Fit,
                    useFullResolution = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 2. TOP FLOATING HEADER OVERLAY
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp)
            ) {
                // Glassmorphic / Tonal Close Button (✕)
                val closeBtnBg = if (colors.isGlassmorphism) Color(0x8A141722) else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f)
                val closeBtnBorder = if (colors.isGlassmorphism) Modifier.border(1.dp, Color(0x33FFFFFF), CircleShape) else Modifier

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(closeBtnBg)
                        .then(closeBtnBorder)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.White),
                            onClick = onClose
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = if (colors.isGlassmorphism) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Title & Subtitle Stack wrapped in subtle tonal pill
                val headerPillBg = if (colors.isGlassmorphism) {
                    Color(0x7A141722)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f)
                }
                val headerPillBorder = if (colors.isGlassmorphism) {
                    Modifier.border(0.8.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                } else {
                    Modifier
                }

                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(headerPillBg)
                        .then(headerPillBorder)
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Column {
                        Text(
                            text = item.directoryName.ifBlank { item.title.ifBlank { "Artwork" } },
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (colors.isGlassmorphism) Color.White else MaterialTheme.colorScheme.onSurface,
                                shadow = if (colors.isGlassmorphism) Shadow(color = Color(0xCC000000), blurRadius = 8f) else null
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (colors.isGlassmorphism) colors.mintText else MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (item.width > 0 && item.height > 0) "${item.width}x${item.height} • Preview" else "Anime Wallpaper Preview",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (colors.isGlassmorphism) Color(0xFFD1D5DB) else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = TextStyle(
                                    shadow = if (colors.isGlassmorphism) Shadow(color = Color(0xCC000000), blurRadius = 6f) else null
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. BOTTOM FLOATING CONTROL DOCK
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val haptics = LocalHapticFeedback.current

            val dockBackgroundBrush = if (colors.isGlassmorphism) {
                Brush.verticalGradient(
                    listOf(
                        Color(0x3DFFFFFF), // Top specular reflection on frosted glass
                        Color(0x1F121728)  // Translucent acrylic dock base
                    )
                )
            } else {
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f)
                    )
                )
            }

            val dockBorderModifier = if (colors.isGlassmorphism) {
                Modifier.border(
                    1.dp,
                    Brush.verticalGradient(listOf(Color(0x8CFFFFFF), Color(0x28FFFFFF))),
                    RoundedCornerShape(28.dp)
                )
            } else {
                Modifier
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .shadow(
                        elevation = if (colors.isGlassmorphism) 20.dp else 16.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = if (colors.isGlassmorphism) colors.primaryAccent.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(dockBackgroundBrush)
                    .then(dockBorderModifier)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Segmented Scale Switch ("Fill Screen" | "Fit Entire")
                    val isFill = selectedScale == ScaleMode.FILL

                    val switchBgModifier = if (colors.isGlassmorphism) {
                        Modifier
                            .background(Brush.verticalGradient(listOf(Color(0x28FFFFFF), Color(0x12FFFFFF))))
                            .border(1.dp, Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x20FFFFFF))), RoundedCornerShape(23.dp))
                    } else {
                        Modifier
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f))
                    }

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(23.dp))
                            .then(switchBgModifier)
                            .padding(4.dp)
                    ) {
                        val pillWidth = maxWidth / 2
                        val animatedOffsetFraction by animateFloatAsState(
                            targetValue = if (isFill) 0f else 1f,
                            animationSpec = ExpressiveMotion.FastSpatialFloat,
                            label = "SegmentedPillOffset"
                        )

                        // Liquid Sliding Indicator Pill
                        val pillBackground = if (colors.isGlassmorphism) {
                            colors.primaryAccent
                        } else {
                            MaterialTheme.colorScheme.primary
                        }

                        Box(
                            modifier = Modifier
                                .width(pillWidth)
                                .fillMaxHeight()
                                .offset {
                                    IntOffset(
                                        x = (animatedOffsetFraction * pillWidth.toPx()).roundToInt(),
                                        y = 0
                                    )
                                }
                                .clip(RoundedCornerShape(19.dp))
                                .background(pillBackground)
                        )

                        Row(modifier = Modifier.fillMaxSize()) {
                            // "Fill Screen" Segment
                            val fillTextColor = if (isFill) {
                                if (colors.isGlassmorphism) colors.primaryAccentText else MaterialTheme.colorScheme.onPrimary
                            } else {
                                if (colors.isGlassmorphism) colors.textSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(19.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            if (selectedScale != ScaleMode.FILL) {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedScale = ScaleMode.FILL
                                            }
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Crop,
                                        contentDescription = null,
                                        tint = fillTextColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Fill Screen",
                                        fontSize = 13.sp,
                                        fontWeight = if (isFill) FontWeight.Bold else FontWeight.Medium,
                                        color = fillTextColor
                                    )
                                }
                            }

                            // "Fit Entire" Segment
                            val fitTextColor = if (!isFill) {
                                if (colors.isGlassmorphism) colors.primaryAccentText else MaterialTheme.colorScheme.onPrimary
                            } else {
                                if (colors.isGlassmorphism) colors.textSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(19.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            if (selectedScale != ScaleMode.FIT) {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedScale = ScaleMode.FIT
                                            }
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FitScreen,
                                        contentDescription = null,
                                        tint = fitTextColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Fit Entire",
                                        fontSize = 13.sp,
                                        fontWeight = if (!isFill) FontWeight.Bold else FontWeight.Medium,
                                        color = fitTextColor
                                    )
                                }
                            }
                        }
                    }

                    // Primary "Set Wallpaper" CTA Button
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showTargetModal = true
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (colors.isGlassmorphism) colors.primaryAccent else MaterialTheme.colorScheme.primary,
                            contentColor = if (colors.isGlassmorphism) colors.primaryAccentText else MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .height(50.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Wallpaper,
                                contentDescription = null,
                                tint = if (colors.isGlassmorphism) colors.primaryAccentText else MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Set Wallpaper",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (colors.isGlassmorphism) colors.primaryAccentText else MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }

        // 4. "SET WALLPAPER" TARGET SELECTION MODAL DIALOG
        if (showTargetModal) {
            TargetSelectionModalDialog(
                colors = colors,
                onDismiss = { showTargetModal = false },
                onClose = onClose,
                onSelectTarget = { target ->
                    onApply(selectedScale, target)
                },
                onSelectSystemCropper = {
                    if (activity != null) {
                        onLaunchSystemPicker(activity, item)
                    }
                },
                onSelectAppBackground = if (onSetAppBackground != null) {
                    {
                        onSetAppBackground.invoke(item)
                    }
                } else null
            )
        }
    }
}

@Composable
fun TargetSelectionModalDialog(
    colors: AxWhavenColors,
    onDismiss: () -> Unit,
    onClose: () -> Unit,
    onSelectTarget: (WallpaperTarget) -> Unit,
    onSelectSystemCropper: () -> Unit,
    onSelectAppBackground: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var applyingOptionId by remember { mutableStateOf<String?>(null) }
    var completedOptionId by remember { mutableStateOf<String?>(null) }
    val anyActive = applyingOptionId != null || completedOptionId != null

    val totalItems = if (onSelectAppBackground != null) 5 else 4

    val handleOptionClick: (String, () -> Unit) -> Unit = { id, action ->
        if (!anyActive) {
            applyingOptionId = id
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            coroutineScope.launch {
                action()
                delay(650)
                completedOptionId = id
                applyingOptionId = null
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(550)
                onDismiss()
                onClose()
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!anyActive) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !anyActive,
            dismissOnClickOutside = !anyActive
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (!anyActive) onDismiss()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            val dialogBgModifier = if (colors.isGlassmorphism) {
                Modifier.background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x3BFFFFFF), // Frosted glass reflection
                            Color(0xE6141828)  // Deep acrylic tinted base
                        )
                    )
                )
            } else {
                Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
            }

            val dialogBorderModifier = if (colors.isGlassmorphism) {
                Modifier.border(
                    1.dp,
                    Brush.verticalGradient(listOf(Color(0x80FFFFFF), Color(0x30FFFFFF))),
                    RoundedCornerShape(28.dp)
                )
            } else {
                Modifier
            }

            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .then(dialogBgModifier)
                    .then(dialogBorderModifier)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* prevent click through */ }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    // Modal Header
                    Text(
                        text = "Set Wallpaper",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (colors.isGlassmorphism) colors.textPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Choose where to apply this artwork",
                        fontSize = 13.sp,
                        color = if (colors.isGlassmorphism) colors.textSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                    )

                    // Android 17 Segmented Card Group (3.dp spacing, dynamic corner morphing)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        // 0: HOME
                        SegmentedTargetOptionRow(
                            index = 0,
                            totalItems = totalItems,
                            icon = Icons.Default.Home,
                            iconContainerColor = if (colors.isGlassmorphism) colors.surfaceElevated else MaterialTheme.colorScheme.primaryContainer,
                            iconTint = if (colors.isGlassmorphism) colors.primaryAccent else MaterialTheme.colorScheme.primary,
                            title = "Home Screen",
                            subtitle = "Set as launcher background",
                            completedSubtitle = "Applied to Home screen!",
                            isApplying = applyingOptionId == "HOME",
                            isCompleted = completedOptionId == "HOME",
                            anyActive = anyActive,
                            colors = colors,
                            onClick = {
                                handleOptionClick("HOME") {
                                    onSelectTarget(WallpaperTarget.HOME)
                                }
                            }
                        )

                        // 1: LOCK
                        SegmentedTargetOptionRow(
                            index = 1,
                            totalItems = totalItems,
                            icon = Icons.Default.Lock,
                            iconContainerColor = if (colors.isGlassmorphism) colors.surfaceElevated else MaterialTheme.colorScheme.secondaryContainer,
                            iconTint = if (colors.isGlassmorphism) colors.mintText else MaterialTheme.colorScheme.secondary,
                            title = "Lock Screen",
                            subtitle = "Set as device lock screen",
                            completedSubtitle = "Applied to Lock screen!",
                            isApplying = applyingOptionId == "LOCK",
                            isCompleted = completedOptionId == "LOCK",
                            anyActive = anyActive,
                            colors = colors,
                            onClick = {
                                handleOptionClick("LOCK") {
                                    onSelectTarget(WallpaperTarget.LOCK)
                                }
                            }
                        )

                        // 2: BOTH
                        SegmentedTargetOptionRow(
                            index = 2,
                            totalItems = totalItems,
                            icon = Icons.Default.Layers,
                            iconContainerColor = if (colors.isGlassmorphism) colors.surfaceElevated else MaterialTheme.colorScheme.tertiaryContainer,
                            iconTint = if (colors.isGlassmorphism) Color(0xFFF59E0B) else MaterialTheme.colorScheme.tertiary,
                            title = "Both Screens",
                            subtitle = "Apply to Home and Lock screens",
                            completedSubtitle = "Applied to both screens!",
                            isApplying = applyingOptionId == "BOTH",
                            isCompleted = completedOptionId == "BOTH",
                            anyActive = anyActive,
                            colors = colors,
                            onClick = {
                                handleOptionClick("BOTH") {
                                    onSelectTarget(WallpaperTarget.BOTH)
                                }
                            }
                        )

                        // 3: CROPPER
                        SegmentedTargetOptionRow(
                            index = 3,
                            totalItems = totalItems,
                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                            iconContainerColor = if (colors.isGlassmorphism) colors.surfaceElevated else MaterialTheme.colorScheme.surfaceContainerHighest,
                            iconTint = if (colors.isGlassmorphism) colors.textMuted else MaterialTheme.colorScheme.onSurfaceVariant,
                            title = "System Wallpaper Cropper",
                            subtitle = "Open with gallery or default cropper",
                            completedSubtitle = "Opening cropper...",
                            isApplying = applyingOptionId == "CROPPER",
                            isCompleted = completedOptionId == "CROPPER",
                            anyActive = anyActive,
                            colors = colors,
                            onClick = {
                                handleOptionClick("CROPPER") {
                                    onSelectSystemCropper()
                                }
                            }
                        )

                        // 4: APP BACKGROUND (Optional)
                        if (onSelectAppBackground != null) {
                            SegmentedTargetOptionRow(
                                index = 4,
                                totalItems = totalItems,
                                icon = Icons.Default.Palette,
                                iconContainerColor = if (colors.isGlassmorphism) colors.surfaceElevated else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                iconTint = if (colors.isGlassmorphism) colors.secondaryAccent else MaterialTheme.colorScheme.primary,
                                title = "Set as App Background",
                                subtitle = "Use this artwork as the app's internal backdrop",
                                completedSubtitle = "Set as backdrop!",
                                isApplying = applyingOptionId == "APP_BG",
                                isCompleted = completedOptionId == "APP_BG",
                                anyActive = anyActive,
                                colors = colors,
                                onClick = {
                                    handleOptionClick("APP_BG") {
                                        onSelectAppBackground()
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Modal Footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            enabled = !anyActive,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (colors.isGlassmorphism) colors.textSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedTargetOptionRow(
    index: Int,
    totalItems: Int,
    icon: ImageVector,
    iconContainerColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    completedSubtitle: String,
    isApplying: Boolean,
    isCompleted: Boolean,
    anyActive: Boolean,
    colors: AxWhavenColors,
    onClick: () -> Unit
) {
    val isSelected = isApplying || isCompleted
    val isTop = index == 0
    val isBottom = index == totalItems - 1

    val animatedTopStart by animateDpAsState(
        targetValue = if (isSelected) 20.dp else if (isTop) 20.dp else 6.dp,
        animationSpec = ExpressiveMotion.FastSpatialDp,
        label = "TopStart"
    )
    val animatedTopEnd by animateDpAsState(
        targetValue = if (isSelected) 20.dp else if (isTop) 20.dp else 6.dp,
        animationSpec = ExpressiveMotion.FastSpatialDp,
        label = "TopEnd"
    )
    val animatedBottomEnd by animateDpAsState(
        targetValue = if (isSelected) 20.dp else if (isBottom) 20.dp else 6.dp,
        animationSpec = ExpressiveMotion.FastSpatialDp,
        label = "BottomEnd"
    )
    val animatedBottomStart by animateDpAsState(
        targetValue = if (isSelected) 20.dp else if (isBottom) 20.dp else 6.dp,
        animationSpec = ExpressiveMotion.FastSpatialDp,
        label = "BottomStart"
    )

    val currentShape = RoundedCornerShape(
        topStart = animatedTopStart.coerceAtLeast(0.dp),
        topEnd = animatedTopEnd.coerceAtLeast(0.dp),
        bottomEnd = animatedBottomEnd.coerceAtLeast(0.dp),
        bottomStart = animatedBottomStart.coerceAtLeast(0.dp)
    )

    val rowBg by animateColorAsState(
        targetValue = when {
            isSelected -> if (colors.isGlassmorphism) Color(0x40FFFFFF) else MaterialTheme.colorScheme.secondaryContainer
            else -> if (colors.isGlassmorphism) Color(0x28FFFFFF) else MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = ExpressiveMotion.DefaultEffectsColor,
        label = "RowBg"
    )

    val rowBorder = if (colors.isGlassmorphism) {
        Modifier.border(1.dp, Color(0x4DFFFFFF), currentShape)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(currentShape)
            .background(rowBg)
            .then(rowBorder)
            .clickable(
                enabled = !anyActive,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = if (colors.isGlassmorphism) colors.primaryAccent else MaterialTheme.colorScheme.primary),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Squircle Icon Badge Box
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Title & Subtitle
            Column(
                modifier = Modifier
                    .padding(start = 14.dp)
                    .weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isSelected -> if (colors.isGlassmorphism) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                        else -> if (colors.isGlassmorphism) colors.textPrimary else MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = if (isCompleted) completedSubtitle else subtitle,
                    fontSize = 12.sp,
                    color = when {
                        isCompleted -> if (colors.isGlassmorphism) colors.mintText else MaterialTheme.colorScheme.primary
                        isSelected -> if (colors.isGlassmorphism) colors.mintText else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        else -> if (colors.isGlassmorphism) colors.textSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(top = 1.dp)
                )
            }

            // Right Action Indicator (Arrow -> Spinner -> Spring Checkmark)
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = when {
                        isCompleted -> 2
                        isApplying -> 1
                        else -> 0
                    },
                    transitionSpec = {
                        if (targetState == 2) {
                            scaleIn(
                                initialScale = 0.5f,
                                animationSpec = ExpressiveMotion.FastSpatialFloat
                            ) + fadeIn(animationSpec = ExpressiveMotion.DefaultEffectsFloat) togetherWith
                                    fadeOut(animationSpec = ExpressiveMotion.DefaultEffectsFloat)
                        } else {
                            (fadeIn(animationSpec = ExpressiveMotion.DefaultEffectsFloat) +
                                    scaleIn(initialScale = 0.8f, animationSpec = ExpressiveMotion.DefaultSpatialFloat)) togetherWith
                                    (fadeOut(animationSpec = ExpressiveMotion.DefaultEffectsFloat) +
                                            scaleOut(targetScale = 0.8f, animationSpec = ExpressiveMotion.DefaultSpatialFloat))
                        }
                    },
                    label = "RowIndicator"
                ) { state ->
                    when (state) {
                        2 -> Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Applied",
                            tint = if (colors.isGlassmorphism) colors.mintText else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        1 -> CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.2.dp,
                            color = if (colors.isGlassmorphism) Color.White else MaterialTheme.colorScheme.primary
                        )
                        else -> Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = if (colors.isGlassmorphism) colors.textMuted else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
