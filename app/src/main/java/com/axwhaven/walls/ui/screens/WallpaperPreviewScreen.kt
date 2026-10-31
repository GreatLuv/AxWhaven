package com.axwhaven.walls.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.axwhaven.walls.model.ScaleMode
import com.axwhaven.walls.model.WallpaperItem
import com.axwhaven.walls.model.WallpaperTarget
import com.axwhaven.walls.ui.components.WallpaperImage
import com.axwhaven.walls.ui.theme.AxWhavenColors
import com.axwhaven.walls.ui.theme.LocalAxWhavenColors

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
            Crossfade(targetState = selectedScale, label = "CanvasScaleTransition") { scale ->
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
                // Glassmorphic Close Button (✕)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x8A141722))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
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
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Title & Subtitle Stack
                Column(
                    modifier = Modifier
                        .padding(start = 14.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = item.directoryName.ifBlank { item.title.ifBlank { "Artwork" } },
                        style = TextStyle(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            shadow = Shadow(
                                color = Color(0xCC000000),
                                blurRadius = 8f
                            )
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
                                .background(colors.mintText)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (item.width > 0 && item.height > 0) "${item.width}x${item.height} • Preview" else "Anime Wallpaper Preview",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFD1D5DB),
                            style = TextStyle(
                                shadow = Shadow(color = Color(0xCC000000), blurRadius = 6f)
                            )
                        )
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
            val dockBackgroundBrush = if (colors.isGlassmorphism) {
                Brush.verticalGradient(
                    listOf(
                        Color(0x3DFFFFFF), // Top specular reflection on frosted glass
                        Color(0x1F121728)  // Translucent acrylic dock base
                    )
                )
            } else {
                Brush.linearGradient(listOf(colors.surfaceElevated.copy(alpha = 0.94f), colors.surfaceElevated.copy(alpha = 0.94f)))
            }

            val dockBorderBrush = if (colors.isGlassmorphism) {
                Brush.verticalGradient(
                    listOf(
                        Color(0x8CFFFFFF), // Crisp light highlight on top rim
                        Color(0x28FFFFFF)  // Subtle bottom glass bevel
                    )
                )
            } else {
                Brush.linearGradient(listOf(colors.cardBorder, colors.cardBorder))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .shadow(
                        elevation = if (colors.isGlassmorphism) 20.dp else 16.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = colors.primaryAccent.copy(alpha = 0.3f)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(dockBackgroundBrush)
                    .border(1.dp, dockBorderBrush, RoundedCornerShape(28.dp))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Segmented Scale Switch ("Fill Screen" | "Fit Entire")
                    val switchBgBrush = if (colors.isGlassmorphism) {
                        Brush.verticalGradient(listOf(Color(0x28FFFFFF), Color(0x12FFFFFF)))
                    } else {
                        Brush.linearGradient(listOf(colors.surface, colors.surface))
                    }
                    val switchBorderBrush = if (colors.isGlassmorphism) {
                        Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x20FFFFFF)))
                    } else {
                        Brush.linearGradient(listOf(colors.cardBorder, colors.cardBorder))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(23.dp))
                            .background(switchBgBrush)
                            .border(1.dp, switchBorderBrush, RoundedCornerShape(23.dp))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isFill = selectedScale == ScaleMode.FILL

                        val fillBg by animateColorAsState(
                            targetValue = if (isFill) colors.primaryAccent else Color.Transparent,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "FillBg"
                        )
                        val fitBg by animateColorAsState(
                            targetValue = if (!isFill) colors.primaryAccent else Color.Transparent,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "FitBg"
                        )
                        val fillTextColor by animateColorAsState(
                            targetValue = if (isFill) colors.primaryAccentText else colors.textSecondary,
                            label = "FillText"
                        )
                        val fitTextColor by animateColorAsState(
                            targetValue = if (!isFill) colors.primaryAccentText else colors.textSecondary,
                            label = "FitText"
                        )

                        // "Fill Screen" Segment
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(19.dp))
                                .background(fillBg)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = colors.primaryAccent),
                                    onClick = { selectedScale = ScaleMode.FILL }
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
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(19.dp))
                                .background(fitBg)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = colors.primaryAccent),
                                    onClick = { selectedScale = ScaleMode.FIT }
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

                    // Primary "Set Wallpaper" CTA Button
                    Button(
                        onClick = { showTargetModal = true },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryAccent,
                            contentColor = colors.primaryAccentText
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
                                tint = colors.primaryAccentText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Set Wallpaper",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryAccentText
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
                onSelectTarget = { target ->
                    showTargetModal = false
                    onApply(selectedScale, target)
                },
                onSelectSystemCropper = {
                    showTargetModal = false
                    if (activity != null) {
                        onLaunchSystemPicker(activity, item)
                    }
                },
                onSelectAppBackground = {
                    showTargetModal = false
                    onSetAppBackground?.invoke(item)
                }
            )
        }
    }
}

@Composable
fun TargetSelectionModalDialog(
    colors: AxWhavenColors,
    onDismiss: () -> Unit,
    onSelectTarget: (WallpaperTarget) -> Unit,
    onSelectSystemCropper: () -> Unit,
    onSelectAppBackground: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
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
                Modifier.background(colors.surfaceElevated)
            }

            val dialogBorderModifier = if (colors.isGlassmorphism) {
                Modifier.border(
                    1.dp,
                    Brush.verticalGradient(listOf(Color(0x80FFFFFF), Color(0x30FFFFFF))),
                    RoundedCornerShape(28.dp)
                )
            } else {
                Modifier.border(1.dp, colors.cardBorder, RoundedCornerShape(28.dp))
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
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Choose where to apply this artwork",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                    )

                    // Target Option Cards
                    TargetOptionCard(
                        icon = Icons.Default.Home,
                        iconTint = colors.primaryAccent,
                        title = "Home Screen",
                        subtitle = "Set as launcher background",
                        colors = colors,
                        onClick = { onSelectTarget(WallpaperTarget.HOME) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TargetOptionCard(
                        icon = Icons.Default.Lock,
                        iconTint = colors.mintText,
                        title = "Lock Screen",
                        subtitle = "Set as device lock screen",
                        colors = colors,
                        onClick = { onSelectTarget(WallpaperTarget.LOCK) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TargetOptionCard(
                        icon = Icons.Default.Layers,
                        iconTint = Color(0xFFF59E0B),
                        title = "Both Screens",
                        subtitle = "Apply to Home and Lock screens",
                        colors = colors,
                        onClick = { onSelectTarget(WallpaperTarget.BOTH) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TargetOptionCard(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        iconTint = colors.textMuted,
                        title = "System Wallpaper Cropper",
                        subtitle = "Open with gallery or default cropper",
                        colors = colors,
                        onClick = onSelectSystemCropper
                    )

                    if (onSelectAppBackground != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        TargetOptionCard(
                            icon = Icons.Default.Palette,
                            iconTint = colors.secondaryAccent,
                            title = "Set as App Background",
                            subtitle = "Use this artwork as the app's internal backdrop",
                            colors = colors,
                            onClick = onSelectAppBackground
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Modal Footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TargetOptionCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    colors: AxWhavenColors,
    onClick: () -> Unit
) {
    val cardBg = if (colors.isGlassmorphism) Color(0x28FFFFFF) else colors.surface
    val cardBorder = if (colors.isGlassmorphism) Color(0x4DFFFFFF) else colors.cardBorder

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.primaryAccent),
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Icon Badge Container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Text Stack
            Column(
                modifier = Modifier
                    .padding(start = 14.dp)
                    .weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
