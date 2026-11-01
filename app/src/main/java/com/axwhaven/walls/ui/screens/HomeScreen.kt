package com.axwhaven.walls.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.axwhaven.walls.HomeNavigationTab
import com.axwhaven.walls.WallpaperUiState
import com.axwhaven.walls.model.ArtDirectory
import com.axwhaven.walls.model.WallpaperItem
import com.axwhaven.walls.model.WallpaperTarget
import com.axwhaven.walls.ui.components.ArtsCountHeaderBadge
import com.axwhaven.walls.ui.components.GlassmorphicBadge
import com.axwhaven.walls.ui.components.WallpaperImage
import com.axwhaven.walls.ui.theme.AxWhavenColors
import com.axwhaven.walls.ui.theme.ExpressiveMotion
import com.axwhaven.walls.ui.theme.FloatingDockClearance
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    uiState: WallpaperUiState,
    colors: AxWhavenColors,
    onTabSelect: (HomeNavigationTab) -> Unit,
    onDirectorySelect: (ArtDirectory?) -> Unit,
    onWallpaperPreview: (WallpaperItem) -> Unit,
    onRollDie: (WallpaperTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDieDialog by remember { mutableStateOf(false) }

    val haptics = LocalHapticFeedback.current
    val collectionsGridState = rememberLazyGridState()
    val allWallpapersGridState = rememberLazyGridState()

    // Bottom clearance: FloatingDockClearance (150.dp) to completely clear floating nav bar
    val density = LocalDensity.current
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomClearance = FloatingDockClearance

    // Scroll Direction Vanish (10.dp threshold + anti-feedback loop settle)
    val scrollThresholdPx = remember(density) { with(density) { 10.dp.toPx() } }
    var isScrollingDown by remember { mutableStateOf(false) }
    var scrollAccumulator by remember { mutableFloatStateOf(0f) }
    var lastVisibilityChangeTime by remember { mutableLongStateOf(0L) }

    val isAllWallpapers = uiState.homeTab == HomeNavigationTab.ALL_WALLPAPERS
    val activeGridState = if (isAllWallpapers) allWallpapersGridState else collectionsGridState

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val now = System.currentTimeMillis()
                // Anti-feedback-loop: ignore scroll deltas for ~350ms after visibility toggle
                if (now - lastVisibilityChangeTime < 350L) {
                    return Offset.Zero
                }

                val delta = available.y
                if (delta < 0) {
                    // Scrolling down
                    if (scrollAccumulator > 0) scrollAccumulator = 0f
                    scrollAccumulator += delta
                    if (scrollAccumulator < -scrollThresholdPx && !isScrollingDown) {
                        isScrollingDown = true
                        lastVisibilityChangeTime = now
                        scrollAccumulator = 0f
                    }
                } else if (delta > 0) {
                    // Scrolling up
                    if (scrollAccumulator < 0) scrollAccumulator = 0f
                    scrollAccumulator += delta
                    if (scrollAccumulator > scrollThresholdPx && isScrollingDown) {
                        isScrollingDown = false
                        lastVisibilityChangeTime = now
                        scrollAccumulator = 0f
                    }
                }
                return Offset.Zero
            }
        }
    }

    // Always show header + dice when at the top (firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0)
    LaunchedEffect(activeGridState) {
        snapshotFlow {
            activeGridState.firstVisibleItemIndex == 0 && activeGridState.firstVisibleItemScrollOffset == 0
        }.collect { isAtTop ->
            if (isAtTop && isScrollingDown) {
                isScrollingDown = false
                scrollAccumulator = 0f
            }
        }
    }

    // Reset to "shown" on tab change
    LaunchedEffect(uiState.homeTab) {
        isScrollingDown = false
        scrollAccumulator = 0f
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (uiState.appBackgroundItem != null || colors.isGlassmorphism) Color.Transparent else colors.background
            )
    ) {
        if (uiState.selectedDirectory != null) {
            // =========================================================================
            // INSIDE A COLLECTION DIRECTORY VIEW
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Top Sub-header with Navigation Back
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 8.dp, bottom = 12.dp)
                ) {
                    val backBtnInteractionSource = remember { MutableInteractionSource() }
                    val isBackBtnPressed by backBtnInteractionSource.collectIsPressedAsState()
                    val backBtnScale by animateFloatAsState(
                        targetValue = if (isBackBtnPressed) 0.90f else 1.0f,
                        animationSpec = ExpressiveMotion.FastSpatialFloat,
                        label = "CollectionBackScale"
                    )
                    val backBtnCorner by animateDpAsState(
                        targetValue = if (isBackBtnPressed) 24.dp else 16.dp,
                        animationSpec = ExpressiveMotion.FastSpatialDp,
                        label = "CollectionBackCorner"
                    )
                    val backBtnShape = RoundedCornerShape(backBtnCorner.coerceAtLeast(0.dp))
                    val backBtnBg = if (colors.isGlassmorphism) Color(0x30FFFFFF) else MaterialTheme.colorScheme.surfaceContainerHigh
                    val backBtnBorder = if (colors.isGlassmorphism) {
                        Modifier.border(1.dp, Brush.verticalGradient(listOf(Color(0x70FFFFFF), Color(0x25FFFFFF))), backBtnShape)
                    } else Modifier

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .graphicsLayer {
                                scaleX = backBtnScale
                                scaleY = backBtnScale
                            }
                            .clip(backBtnShape)
                            .background(backBtnBg)
                            .then(backBtnBorder)
                            .clickable(
                                interactionSource = backBtnInteractionSource,
                                indication = ripple(bounded = true, color = colors.primaryAccent),
                                onClick = { onDirectorySelect(null) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.selectedDirectory.artName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Collection",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                    ArtsCountHeaderBadge(
                        text = uiState.selectedDirectory.totalArtsText,
                        colors = colors
                    )
                }

                if (uiState.selectedDirectory.items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (uiState.selectedDirectory.isDownloadedFolder) {
                                    "No AxW Surf wallpapers yet"
                                } else {
                                    "No wallpapers found in this directory"
                                },
                                color = colors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (uiState.selectedDirectory.isDownloadedFolder) {
                                    "Discover and save wallpapers from AxW Surf!"
                                } else {
                                    "Add images under assets/wallpapers/"
                                },
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(top = 4.dp, bottom = bottomClearance),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.selectedDirectory.items, key = { it.id }) { item ->
                            CleanWallpaperCard(
                                item = item,
                                colors = colors,
                                onClick = { onWallpaperPreview(item) }
                            )
                        }
                    }
                }
            }
        } else {
            // =========================================================================
            // MAIN HOME ROOT SCREEN
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
                    .nestedScroll(nestedScrollConnection)
            ) {
                // 1. TOP APP HEADER BAR (Vanishes on scroll-down, reappears on scroll-up or full top)
                AnimatedVisibility(
                    visible = !isScrollingDown,
                    enter = slideInVertically(
                        animationSpec = ExpressiveMotion.DefaultSpatialIntOffset,
                        initialOffsetY = { -it }
                    ) + expandVertically(
                        animationSpec = ExpressiveMotion.DefaultSpatialIntSize,
                        expandFrom = Alignment.Top
                    ) + fadeIn(animationSpec = ExpressiveMotion.DefaultEffectsFloat),
                    exit = slideOutVertically(
                        animationSpec = ExpressiveMotion.DefaultSpatialIntOffset,
                        targetOffsetY = { -it }
                    ) + shrinkVertically(
                        animationSpec = ExpressiveMotion.DefaultSpatialIntSize,
                        shrinkTowards = Alignment.Top
                    ) + fadeOut(animationSpec = ExpressiveMotion.DefaultEffectsFloat)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp)
                    ) {
                        // App Logo Icon: 40dp x 40dp round circle with app icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (colors.isGlassmorphism) Color(0x2EFFFFFF) else colors.surfaceElevated)
                                .border(
                                    1.dp,
                                    if (colors.isGlassmorphism) Brush.verticalGradient(listOf(Color(0x70FFFFFF), Color(0x25FFFFFF)))
                                    else Brush.linearGradient(listOf(colors.primaryAccent, colors.cardBorder)),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.axwhaven.walls.R.drawable.app_icon),
                                contentDescription = "AxWhaven Icon",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Title & Subtitle Stack
                        Column(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .weight(1f)
                        ) {
                            Text(
                                text = "AxWhaven",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "Curated anime gallery",
                                fontSize = 12.5.sp,
                                color = colors.textSecondary
                            )
                        }

                        // Right side: Total Arts Count Pill Badge (secondaryContainer fill + primary dot)
                        ArtsCountHeaderBadge(
                            text = if (uiState.totalArtsCount == 1) "1 art" else "${uiState.totalArtsCount} arts",
                            colors = colors
                        )
                    }
                }

                // 2. SEGMENTED NAVIGATION TOGGLE ("All Wallpapers" [0] | "Collections" [1])
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (colors.isGlassmorphism) {
                                Brush.verticalGradient(listOf(Color(0x2EFFFFFF), Color(0x18FFFFFF)))
                            } else {
                                Brush.linearGradient(listOf(colors.surface, colors.surface))
                            }
                        )
                        .then(
                            if (colors.isGlassmorphism) {
                                Modifier.border(
                                    1.dp,
                                    Brush.verticalGradient(listOf(Color(0x70FFFFFF), Color(0x25FFFFFF))),
                                    RoundedCornerShape(24.dp)
                                )
                            } else Modifier
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val pillWidth = maxWidth / 2
                    val targetTabOffset = if (isAllWallpapers) 0f else 1f
                    val animatedTabOffset by animateFloatAsState(
                        targetValue = targetTabOffset,
                        animationSpec = ExpressiveMotion.FastSpatialFloat,
                        label = "SegmentedPillOffset"
                    )

                    val activeGlassPill = if (colors.isGlassmorphism) Color(0x40FFFFFF) else colors.surfaceElevated

                    // Smooth horizontal sliding indicator pill with Expressive fastSpatial spring
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(pillWidth)
                            .offset {
                                IntOffset(
                                    x = (animatedTabOffset * pillWidth.toPx()).roundToInt(),
                                    y = 0
                                )
                            }
                            .clip(RoundedCornerShape(20.dp))
                            .background(activeGlassPill)
                            .then(
                                if (colors.isGlassmorphism) {
                                    Modifier.border(0.8.dp, Color(0x55FFFFFF), RoundedCornerShape(20.dp))
                                } else Modifier
                            )
                    )

                    val allWallpapersInteractionSource = remember { MutableInteractionSource() }
                    val isAllWallpapersPressed by allWallpapersInteractionSource.collectIsPressedAsState()
                    val allWallpapersScale by animateFloatAsState(
                        targetValue = if (isAllWallpapersPressed) 0.95f else 1.0f,
                        animationSpec = ExpressiveMotion.FastSpatialFloat,
                        label = "AllWallpapersTabScale"
                    )

                    val collectionsInteractionSource = remember { MutableInteractionSource() }
                    val isCollectionsPressed by collectionsInteractionSource.collectIsPressedAsState()
                    val collectionsScale by animateFloatAsState(
                        targetValue = if (isCollectionsPressed) 0.95f else 1.0f,
                        animationSpec = ExpressiveMotion.FastSpatialFloat,
                        label = "CollectionsTabScale"
                    )

                    val allWallpapersTextColor by animateColorAsState(
                        targetValue = if (isAllWallpapers) colors.textPrimary else colors.textMuted,
                        animationSpec = ExpressiveMotion.FastEffectsColor,
                        label = "AllTabText"
                    )
                    val collectionsTextColor by animateColorAsState(
                        targetValue = if (!isAllWallpapers) colors.textPrimary else colors.textMuted,
                        animationSpec = ExpressiveMotion.FastEffectsColor,
                        label = "ColTabText"
                    )

                    val allTabWeight by animateIntAsState(
                        targetValue = if (isAllWallpapers) 800 else 500,
                        label = "AllTabWeight"
                    )
                    val allTabScale by animateFloatAsState(
                        targetValue = if (isAllWallpapers) 1.04f else 1.0f,
                        animationSpec = ExpressiveMotion.FastSpatialFloat,
                        label = "AllTabScale"
                    )

                    val colTabWeight by animateIntAsState(
                        targetValue = if (!isAllWallpapers) 800 else 500,
                        label = "ColTabWeight"
                    )
                    val colTabScale by animateFloatAsState(
                        targetValue = if (!isAllWallpapers) 1.04f else 1.0f,
                        animationSpec = ExpressiveMotion.FastSpatialFloat,
                        label = "ColTabScale"
                    )

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tab 0: All Wallpapers (Default on launch)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .graphicsLayer {
                                    scaleX = allWallpapersScale
                                    scaleY = allWallpapersScale
                                }
                                .clip(RoundedCornerShape(20.dp))
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val press = PressInteraction.Press(down.position)
                                        allWallpapersInteractionSource.tryEmit(press)
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onTabSelect(HomeNavigationTab.ALL_WALLPAPERS)
                                        val up = waitForUpOrCancellation()
                                        if (up != null) {
                                            allWallpapersInteractionSource.tryEmit(PressInteraction.Release(press))
                                        } else {
                                            allWallpapersInteractionSource.tryEmit(PressInteraction.Cancel(press))
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = null,
                                    tint = allWallpapersTextColor,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(contentAlignment = Alignment.Center) {
                                    // Reserved space for ExtraBold text prevents layout jitter and tab width reflow
                                    Text(
                                        text = "All Wallpapers",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.Transparent,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "All Wallpapers",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight(allTabWeight.coerceIn(1, 1000)),
                                        color = allWallpapersTextColor,
                                        maxLines = 1,
                                        modifier = Modifier.graphicsLayer {
                                            scaleX = allTabScale
                                            scaleY = allTabScale
                                        }
                                    )
                                }
                            }
                        }

                        // Tab 1: Collections
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .graphicsLayer {
                                    scaleX = collectionsScale
                                    scaleY = collectionsScale
                                }
                                .clip(RoundedCornerShape(20.dp))
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val press = PressInteraction.Press(down.position)
                                        collectionsInteractionSource.tryEmit(press)
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onTabSelect(HomeNavigationTab.COLLECTIONS)
                                        val up = waitForUpOrCancellation()
                                        if (up != null) {
                                            collectionsInteractionSource.tryEmit(PressInteraction.Release(press))
                                        } else {
                                            collectionsInteractionSource.tryEmit(PressInteraction.Cancel(press))
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = collectionsTextColor,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(contentAlignment = Alignment.Center) {
                                    // Reserved space for ExtraBold text prevents layout jitter and tab width reflow
                                    Text(
                                        text = "Collections",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.Transparent,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Collections",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight(colTabWeight.coerceIn(1, 1000)),
                                        color = collectionsTextColor,
                                        maxLines = 1,
                                        modifier = Modifier.graphicsLayer {
                                            scaleX = colTabScale
                                            scaleY = colTabScale
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. MAIN CONTENT AREA (All Wallpapers [0] <-> Collections [1])
                AnimatedContent(
                    targetState = uiState.homeTab,
                    transitionSpec = {
                        if (targetState.ordinal > initialState.ordinal) {
                            // Forward (All Wallpapers [0] -> Collections [1]): moving right
                            (slideInHorizontally(
                                animationSpec = ExpressiveMotion.DefaultSpatialIntOffset,
                                initialOffsetX = { (it * 0.30f).toInt() }
                            ) + scaleIn(
                                animationSpec = ExpressiveMotion.DefaultSpatialFloat,
                                initialScale = 0.94f
                            ) + fadeIn(
                                animationSpec = ExpressiveMotion.DefaultEffectsFloat
                            )).togetherWith(
                                slideOutHorizontally(
                                    animationSpec = ExpressiveMotion.DefaultSpatialIntOffset,
                                    targetOffsetX = { -(it * 0.30f).toInt() }
                                ) + scaleOut(
                                    animationSpec = ExpressiveMotion.DefaultSpatialFloat,
                                    targetScale = 0.94f
                                ) + fadeOut(
                                    animationSpec = ExpressiveMotion.DefaultEffectsFloat
                                )
                            )
                        } else if (targetState.ordinal < initialState.ordinal) {
                            // Backward (Collections [1] -> All Wallpapers [0]): moving left
                            (slideInHorizontally(
                                animationSpec = ExpressiveMotion.DefaultSpatialIntOffset,
                                initialOffsetX = { -(it * 0.30f).toInt() }
                            ) + scaleIn(
                                animationSpec = ExpressiveMotion.DefaultSpatialFloat,
                                initialScale = 0.94f
                            ) + fadeIn(
                                animationSpec = ExpressiveMotion.DefaultEffectsFloat
                            )).togetherWith(
                                slideOutHorizontally(
                                    animationSpec = ExpressiveMotion.DefaultSpatialIntOffset,
                                    targetOffsetX = { (it * 0.30f).toInt() }
                                ) + scaleOut(
                                    animationSpec = ExpressiveMotion.DefaultSpatialFloat,
                                    targetScale = 0.94f
                                ) + fadeOut(
                                    animationSpec = ExpressiveMotion.DefaultEffectsFloat
                                )
                            )
                        } else {
                            fadeIn(animationSpec = ExpressiveMotion.DefaultEffectsFloat) togetherWith
                                    fadeOut(animationSpec = ExpressiveMotion.DefaultEffectsFloat)
                        }
                    },
                    label = "HomeContentTransition",
                    modifier = Modifier.fillMaxSize()
                ) { currentTab ->
                    when (currentTab) {
                        HomeNavigationTab.COLLECTIONS -> {
                            if (uiState.directories.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Folder,
                                            contentDescription = null,
                                            modifier = Modifier.size(56.dp),
                                            tint = colors.primaryAccent
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            "No collections detected",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = colors.textPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Add folders under assets/wallpapers/",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                            } else {
                                LazyVerticalGrid(
                                    state = collectionsGridState,
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(top = 14.dp, bottom = bottomClearance),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(uiState.directories, key = { it.id }) { directory ->
                                        CollectionArtCard(
                                            directory = directory,
                                            colors = colors,
                                            onClick = { onDirectorySelect(directory) }
                                        )
                                    }
                                }
                            }
                        }
                        HomeNavigationTab.ALL_WALLPAPERS -> {
                            val allItems = uiState.allLocalWallpapers
                            if (allItems.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No wallpapers available.", color = colors.textSecondary)
                                }
                            } else {
                                LazyVerticalGrid(
                                    state = allWallpapersGridState,
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(top = 14.dp, bottom = bottomClearance),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(allItems, key = { it.id }) { item ->
                                        CleanWallpaperCard(
                                            item = item,
                                            colors = colors,
                                            onClick = { onWallpaperPreview(item) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // STANDALONE DICE ACTION BUTTON (All Wallpapers only, scrolls away on down-scroll)
        // =========================================================================
        if (uiState.selectedDirectory == null) {
            val fabBottomPadding = (maxHeight * 0.25f - 26.dp).coerceIn(120.dp, 240.dp)
            AnimatedVisibility(
                visible = uiState.homeTab == HomeNavigationTab.ALL_WALLPAPERS && !isScrollingDown,
                enter = scaleIn(
                    animationSpec = ExpressiveMotion.FastSpatialFloat,
                    initialScale = 0.3f
                ) + slideInVertically(
                    animationSpec = ExpressiveMotion.FastSpatialIntOffset,
                    initialOffsetY = { it / 2 }
                ) + fadeIn(
                    animationSpec = ExpressiveMotion.DefaultEffectsFloat
                ),
                exit = scaleOut(
                    animationSpec = ExpressiveMotion.FastSpatialFloat,
                    targetScale = 0.3f
                ) + slideOutVertically(
                    animationSpec = ExpressiveMotion.FastSpatialIntOffset,
                    targetOffsetY = { -it / 2 }
                ) + fadeOut(
                    animationSpec = ExpressiveMotion.DefaultEffectsFloat
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = fabBottomPadding)
            ) {
                StandaloneDiceButton(
                    colors = colors,
                    onClick = { showDieDialog = true }
                )
            }
        }

        // =========================================================================
        // REFINED RANDOM DIE TARGET PROMPT DIALOG
        // =========================================================================
        if (showDieDialog) {
            RandomDieTargetDialog(
                colors = colors,
                onDismiss = { showDieDialog = false },
                onSelectTarget = { target ->
                    showDieDialog = false
                    onRollDie(target)
                }
            )
        }
    }
}

@Composable
fun StandaloneDiceButton(
    colors: AxWhavenColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    var cumulativeRotation by remember { mutableFloatStateOf(0f) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val rotation by animateFloatAsState(
        targetValue = cumulativeRotation,
        animationSpec = ExpressiveMotion.FastSpatialFloat,
        label = "DiceRotation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.82f else 1.0f,
        animationSpec = ExpressiveMotion.FastSpatialFloat,
        label = "DiceScale"
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 22.dp else 14.dp,
        animationSpec = ExpressiveMotion.FastSpatialDp,
        label = "DiceCorner"
    )

    val safeCornerRadius = cornerRadius.coerceAtLeast(0.dp)
    val dieShape = RoundedCornerShape(safeCornerRadius)

    val containerBg = if (colors.isGlassmorphism) {
        Brush.verticalGradient(listOf(Color(0x55FFFFFF), Color(0x22141A2E)))
    } else {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primaryContainer,
                colors.surfaceContainerHigh
            )
        )
    }

    val borderBrush = if (colors.isGlassmorphism) {
        Brush.verticalGradient(listOf(Color(0x99FFFFFF), Color(0x35FFFFFF)))
    } else {
        Brush.linearGradient(
            listOf(
                colors.primaryAccent.copy(alpha = 0.65f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            )
        )
    }

    val pipColor = if (colors.isGlassmorphism) {
        colors.primaryAccent
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Box(
        modifier = modifier
            .size(52.dp)
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (colors.isGlassmorphism) 14.dp else 10.dp,
                shape = dieShape,
                spotColor = colors.primaryAccent.copy(alpha = 0.5f)
            )
            .clip(dieShape)
            .background(containerBg)
            .then(
                if (colors.isGlassmorphism) {
                    Modifier.border(1.dp, borderBrush, dieShape)
                } else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                cumulativeRotation += 180f
                isPressed = true
                coroutineScope.launch {
                    delay(120)
                    isPressed = false
                }
                val now = System.currentTimeMillis()
                if (now - lastTapTime > 300L) {
                    lastTapTime = now
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(11.dp)
        ) {
            val pipRadius = 3.4.dp.toPx()
            val w = size.width
            val h = size.height

            // 5 pips: Center, Top-Left, Top-Right, Bottom-Left, Bottom-Right
            drawCircle(color = pipColor, radius = pipRadius, center = Offset(w * 0.5f, h * 0.5f))
            drawCircle(color = pipColor, radius = pipRadius, center = Offset(w * 0.22f, h * 0.22f))
            drawCircle(color = pipColor, radius = pipRadius, center = Offset(w * 0.78f, h * 0.22f))
            drawCircle(color = pipColor, radius = pipRadius, center = Offset(w * 0.22f, h * 0.78f))
            drawCircle(color = pipColor, radius = pipRadius, center = Offset(w * 0.78f, h * 0.78f))
        }
    }
}

@Composable
fun CollectionArtCard(
    directory: ArtDirectory,
    colors: AxWhavenColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = ExpressiveMotion.FastSpatialFloat,
        label = "ColCardScale"
    )

    val cardCornerRadius by animateDpAsState(
        targetValue = if (isPressed) 28.dp else 20.dp,
        animationSpec = ExpressiveMotion.FastSpatialDp,
        label = "ColCardCorner"
    )

    val arrowOffset by animateDpAsState(
        targetValue = if (isPressed) 3.dp else 0.dp,
        animationSpec = ExpressiveMotion.FastSpatialDp,
        label = "ExploreArrowNudge"
    )

    val shape = RoundedCornerShape(cardCornerRadius.coerceAtLeast(0.dp))

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (colors.isGlassmorphism) Color(0x24FFFFFF) else colors.surfaceContainerLow
        ),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(shape)
            .then(
                if (colors.isGlassmorphism) {
                    Modifier.border(
                        1.dp,
                        Brush.verticalGradient(listOf(Color(0x70FFFFFF), Color(0x25FFFFFF))),
                        shape
                    )
                } else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = colors.primaryAccent),
                onClick = onClick
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (directory.coverItem != null) {
                WallpaperImage(
                    source = directory.coverItem?.source,
                    contentDescription = directory.artName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.surfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = colors.primaryAccent,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Smooth multi-stop bottom scrim gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.45f to Color.Transparent,
                                0.75f to colors.scrimEnd.copy(alpha = 0.65f),
                                1.0f to colors.scrimEnd
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Text(
                    text = directory.artName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Per-collection art count squircle badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (colors.isGlassmorphism) Color(0x35FFFFFF)
                                else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.88f)
                            )
                            .then(
                                if (colors.isGlassmorphism) {
                                    Modifier.border(0.8.dp, Color(0x4DFFFFFF), RoundedCornerShape(10.dp))
                                } else Modifier
                            )
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = directory.totalArtsText,
                            color = if (colors.isGlassmorphism) Color(0xFFE2E8F0) else MaterialTheme.colorScheme.primary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.2.sp
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Compact tonal pill chip with nudging arrow
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (colors.isGlassmorphism) colors.primaryAccent.copy(alpha = 0.30f)
                                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                            )
                            .then(
                                if (colors.isGlassmorphism) {
                                    Modifier.border(0.8.dp, colors.primaryAccent.copy(alpha = 0.5f), RoundedCornerShape(50))
                                } else Modifier
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Explore",
                                color = if (colors.isGlassmorphism) colors.secondaryAccent else MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "→",
                                color = if (colors.isGlassmorphism) colors.secondaryAccent else MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.offset(x = arrowOffset)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CleanWallpaperCard(
    item: WallpaperItem,
    colors: AxWhavenColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = ExpressiveMotion.FastSpatialFloat,
        label = "CardScale"
    )

    val cardCornerRadius by animateDpAsState(
        targetValue = if (isPressed) 28.dp else 20.dp,
        animationSpec = ExpressiveMotion.FastSpatialDp,
        label = "CardCorner"
    )

    val shape = RoundedCornerShape(cardCornerRadius.coerceAtLeast(0.dp))

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (colors.isGlassmorphism) Color(0x20FFFFFF) else colors.surfaceContainerLow
        ),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.68f)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(shape)
            .clip(shape)
            .then(
                if (colors.isGlassmorphism) {
                    Modifier.border(
                        1.dp,
                        Brush.verticalGradient(listOf(Color(0x65FFFFFF), Color(0x20FFFFFF))),
                        shape
                    )
                } else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = colors.primaryAccent),
                onClick = onClick
            )
    ) {
        Box(modifier = Modifier.fillMaxSize().clip(shape)) {
            WallpaperImage(
                source = item.source,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun RandomDieTargetDialog(
    colors: AxWhavenColors,
    onDismiss: () -> Unit,
    onSelectTarget: (WallpaperTarget) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
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
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (colors.isGlassmorphism) Color(0xE8101528) else colors.surfaceElevated
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .border(
                        1.dp,
                        if (colors.isGlassmorphism) Brush.verticalGradient(listOf(Color(0x80FFFFFF), Color(0x30FFFFFF)))
                        else Brush.linearGradient(listOf(colors.cardBorder, colors.cardBorder)),
                        RoundedCornerShape(26.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* prevent dismissal on dialog body click */ }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    // Dialog Header with Dice Icon
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.surpriseIconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Casino,
                                contentDescription = null,
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Roll Random Wallpaper",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Instant anime art discovery",
                                fontSize = 12.5.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Choose screen target to immediately apply a random local artwork:",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Target Option 1: Home Screen
                    DieOptionButton(
                        icon = Icons.Default.Home,
                        title = "Home Screen",
                        subtitle = "Apply to launcher background",
                        colors = colors,
                        onClick = { onSelectTarget(WallpaperTarget.HOME) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Target Option 2: Lock Screen
                    DieOptionButton(
                        icon = Icons.Default.Lock,
                        title = "Lock Screen",
                        subtitle = "Apply to device lock screen",
                        colors = colors,
                        onClick = { onSelectTarget(WallpaperTarget.LOCK) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Target Option 3: Both Screens
                    DieOptionButton(
                        icon = Icons.Default.Layers,
                        title = "Both Screens",
                        subtitle = "Apply to both Home and Lock screens",
                        colors = colors,
                        onClick = { onSelectTarget(WallpaperTarget.BOTH) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cancel Footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = "Cancel",
                                color = colors.textSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DieOptionButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    colors: AxWhavenColors,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (colors.isGlassmorphism) Color(0x33FFFFFF) else colors.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (colors.isGlassmorphism) Brush.verticalGradient(listOf(Color(0x70FFFFFF), Color(0x25FFFFFF)))
                else Brush.linearGradient(listOf(colors.cardBorder, colors.cardBorder)),
                RoundedCornerShape(16.dp)
            )
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
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (colors.isGlassmorphism) Color(0x38FFFFFF) else colors.surfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.primaryAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colors.textMuted
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
