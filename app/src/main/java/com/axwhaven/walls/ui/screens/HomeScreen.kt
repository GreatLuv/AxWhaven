package com.axwhaven.walls.ui.screens

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.lerp as colorLerp
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.absoluteValue
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
import com.axwhaven.walls.ui.theme.MainScreenBottomClearance
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    uiState: WallpaperUiState,
    colors: AxWhavenColors,
    onTabSelect: (HomeNavigationTab) -> Unit = {},
    onDirectorySelect: (ArtDirectory?) -> Unit = {},
    onWallpaperPreview: (WallpaperItem) -> Unit,
    onRollDie: (WallpaperTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDieDialog by remember { mutableStateOf(false) }

    val haptics = LocalHapticFeedback.current
    val allWallpapersGridState = rememberLazyGridState()

    // Bottom clearance: MainScreenBottomClearance (150.dp) to completely clear floating nav bar
    val density = LocalDensity.current
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomClearance = MainScreenBottomClearance

    // Scroll Direction Vanish (10.dp threshold + anti-feedback loop settle)
    val scrollThresholdPx = remember(density) { with(density) { 10.dp.toPx() } }
    var isScrollingDown by remember { mutableStateOf(false) }
    var scrollAccumulator by remember { mutableFloatStateOf(0f) }
    var lastVisibilityChangeTime by remember { mutableLongStateOf(0L) }

    val activeGridState = allWallpapersGridState

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

    // Reset to "shown" on initial composition
    LaunchedEffect(Unit) {
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
                        items(
                            items = uiState.selectedDirectory.items,
                            key = { it.id },
                            contentType = { "wallpaper_card" }
                        ) { item ->
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

                // 2. ALL WALLPAPERS GRID (Hero Spotlight Carousel scrolls naturally as top item)
                val allItems = uiState.allLocalWallpapers
                if (allItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No wallpapers available.", color = colors.textSecondary)
                    }
                } else {
                    val sessionSeed = rememberSaveable { kotlin.random.Random.nextLong() }

                    val spotlightWallpapers = remember(uiState.directories, allItems, sessionSeed) {
                        if (allItems.isEmpty()) {
                            emptyList()
                        } else {
                            val rng = kotlin.random.Random(sessionSeed)
                            val sampledFromDirs = uiState.directories.mapNotNull { dir ->
                                if (dir.items.isNotEmpty()) {
                                    dir.items.random(rng)
                                } else {
                                    dir.coverItem
                                }
                            }.shuffled(rng)

                            if (sampledFromDirs.isNotEmpty()) {
                                sampledFromDirs.take(7)
                            } else {
                                allItems.shuffled(rng).take(7)
                            }
                        }
                    }

                    LazyVerticalGrid(
                        state = allWallpapersGridState,
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(top = 8.dp, bottom = bottomClearance),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (spotlightWallpapers.isNotEmpty()) {
                            item(
                                span = { GridItemSpan(maxLineSpan) },
                                key = "home_featured_spotlight_carousel",
                                contentType = "featured_spotlight_carousel"
                            ) {
                                FeaturedSpotlightCarousel(
                                    items = spotlightWallpapers,
                                    colors = colors,
                                    onWallpaperClick = { item -> onWallpaperPreview(item) }
                                )
                            }
                        }

                        items(
                            items = allItems,
                            key = { it.id },
                            contentType = { "wallpaper_card" }
                        ) { item ->
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

        // =========================================================================
        // STANDALONE DICE ACTION BUTTON (All Wallpapers only, scrolls away on down-scroll)
        // =========================================================================
        if (uiState.selectedDirectory == null) {
            val fabBottomPadding = (maxHeight * 0.25f - 26.dp).coerceIn(120.dp, 240.dp)
            AnimatedVisibility(
                visible = !isScrollingDown,
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
fun FeaturedSpotlightCarousel(
    items: List<WallpaperItem>,
    colors: AxWhavenColors,
    onWallpaperClick: (WallpaperItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })
    val coroutineScope = rememberCoroutineScope()
    val motionSpec = ExpressiveMotion.SlowSpatialFloat

    // Stop advancing when screen is inactive/backgrounded
    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isResumed = (event == Lifecycle.Event.ON_RESUME)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Respect system animation preferences and reduced-motion settings
    val context = LocalContext.current
    val areAnimationsDisabled = remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            ) == 0f
        } catch (e: Exception) {
            false
        }
    }

    // Auto-advance every 5.0s when user is not manually scrolling, paused during touch/drag and backgrounding
    LaunchedEffect(pagerState, items.size, isResumed, areAnimationsDisabled) {
        if (items.size <= 1 || areAnimationsDisabled) return@LaunchedEffect
        while (true) {
            delay(5000L)
            if (!pagerState.isScrollInProgress && isResumed) {
                val next = (pagerState.currentPage + 1) % items.size
                pagerState.animateScrollToPage(
                    page = next,
                    animationSpec = motionSpec
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Compose HorizontalPager configured for 3D Cover Flow layout
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 48.dp),
            pageSpacing = (-16).dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
        ) { page ->
            val item = items[page]
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val safeOffset = pageOffset.coerceIn(-1f, 1f)
            val distanceFromCenter = kotlin.math.abs(safeOffset)
            val centerProgress = (1f - distanceFromCenter).coerceIn(0f, 1f)

            // 3D Cover Flow transformations
            // Left card (safeOffset > 0): positive rotation, up to +18 deg
            // Center card (safeOffset == 0): exactly 0 deg
            // Right card (safeOffset < 0): negative rotation, down to -18 deg
            val rotationY = safeOffset * 18f
            val scaleX = lerp(0.90f, 1.0f, centerProgress)
            val scaleY = lerp(0.85f, 1.0f, centerProgress)
            val cardAlpha = lerp(0.75f, 1.0f, centerProgress)
            val cardCornerRadius = (20f + 8f * centerProgress).dp.coerceAtLeast(0.dp)
            val cardElevation = (2f + 14f * centerProgress).dp

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(centerProgress)
                    .semantics {
                        selected = (page == pagerState.currentPage)
                        role = Role.Tab
                    }
            ) {
                FeaturedSpotlightItemCard(
                    item = item,
                    colors = colors,
                    cardRotationY = rotationY,
                    cardScaleX = scaleX,
                    cardScaleY = scaleY,
                    cardAlpha = cardAlpha,
                    cardCornerRadius = cardCornerRadius,
                    cardElevation = cardElevation,
                    safeOffset = safeOffset,
                    onClick = {
                        if (page != pagerState.currentPage) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(page, animationSpec = motionSpec)
                            }
                        } else {
                            onWallpaperClick(item)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Fluid Morphing & Clickable Page Indicators
        if (items.size > 1) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            ) {
                val currentPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
                repeat(items.size) { index ->
                    val distance = (currentPosition - index).absoluteValue
                    val factor = (1f - distance).coerceIn(0f, 1f)
                    val dotWidth = (8f + 16f * factor).dp
                    val activeColor = if (colors.isGlassmorphism) colors.primaryAccent else MaterialTheme.colorScheme.primary
                    val inactiveColor = if (colors.isGlassmorphism) Color(0x35FFFFFF) else MaterialTheme.colorScheme.surfaceContainerHighest
                    val dotColor = colorLerp(inactiveColor, activeColor, factor)

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(8.dp)
                            .width(dotWidth)
                            .clip(RoundedCornerShape(4.dp))
                            .background(dotColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (pagerState.currentPage != index) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index, animationSpec = motionSpec)
                                        }
                                    }
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedSpotlightItemCard(
    item: WallpaperItem,
    colors: AxWhavenColors,
    cardRotationY: Float,
    cardScaleX: Float,
    cardScaleY: Float,
    cardAlpha: Float,
    cardCornerRadius: androidx.compose.ui.unit.Dp,
    cardElevation: androidx.compose.ui.unit.Dp,
    safeOffset: Float,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = ExpressiveMotion.FastSpatialFloat,
        label = "SpotlightPressScale"
    )

    val pressCornerBonus by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 0.dp,
        animationSpec = ExpressiveMotion.FastSpatialDp,
        label = "SpotlightCornerBonus"
    )

    val shape = RoundedCornerShape((cardCornerRadius + pressCornerBonus).coerceAtLeast(0.dp))

    // Softly pulsing primary dot for Material You spotlight pill
    val infiniteTransition = rememberInfiniteTransition(label = "SpotlightPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.80f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SpotlightDotScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.60f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SpotlightDotAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                transformOrigin = TransformOrigin.Center
                rotationY = cardRotationY
                scaleX = cardScaleX * pressScale
                scaleY = cardScaleY * pressScale
                alpha = cardAlpha
                val cardWidth = size.width
                cameraDistance = if (cardWidth > 0f) {
                    (cardWidth * 2.5f).coerceIn(1200f, 6000f)
                } else {
                    (12f * density).coerceIn(1200f, 6000f)
                }
            }
    ) {
        // Dynamic soft ambient glow shadow behind the active card
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .shadow(
                    elevation = cardElevation,
                    shape = shape,
                    spotColor = colors.primaryAccent.copy(alpha = if (colors.isGlassmorphism) 0.38f else 0.25f),
                    ambientColor = colors.primaryAccent.copy(alpha = 0.16f)
                )
        )

        Card(
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = if (colors.isGlassmorphism) Color(0x28FFFFFF) else colors.surfaceContainerLow
            ),
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .then(
                    if (colors.isGlassmorphism) {
                        Modifier.border(
                            1.dp,
                            Brush.verticalGradient(listOf(Color(0x80FFFFFF), Color(0x20FFFFFF))),
                            shape
                        )
                    } else Modifier // Zero border in M3 Expressive
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, color = colors.primaryAccent),
                    onClick = onClick
                )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Parallax-shifted Full Portrait Wallpaper Image, top-center aligned for anime artwork
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = safeOffset * 36.dp.toPx()
                            scaleX = 1.15f
                            scaleY = 1.15f
                        }
                ) {
                    WallpaperImage(
                        source = item.source,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Top-Left: Tonal Material You "• SPOTLIGHT" Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (colors.isGlassmorphism) Color(0x600B0E1E)
                            else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.90f)
                        )
                        .then(
                            if (colors.isGlassmorphism) {
                                Modifier.border(0.8.dp, Color(0x75FFFFFF), RoundedCornerShape(50))
                            } else Modifier
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .graphicsLayer {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                    alpha = pulseAlpha
                                }
                                .clip(CircleShape)
                                .background(
                                    if (colors.isGlassmorphism) colors.primaryAccent
                                    else MaterialTheme.colorScheme.primary
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SPOTLIGHT",
                            color = if (colors.isGlassmorphism) Color.White
                            else MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Bottom Overlay Bar with subtle gradient scrim, collection/character title, and 44dp squircle action button
                val displayTitle = if (item.directoryName.isNotBlank()) item.directoryName else item.title
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.35f to Color(0x50000000),
                                    1.0f to Color(0xDD000000)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Featured Art",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.80f),
                                maxLines = 1
                            )
                        }

                        // 44.dp expressive squircle action button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (colors.isGlassmorphism) colors.primaryAccent
                                    else MaterialTheme.colorScheme.primaryContainer
                                )
                                .then(
                                    if (colors.isGlassmorphism) {
                                        Modifier.border(1.dp, Color(0x80FFFFFF), RoundedCornerShape(14.dp))
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Explore",
                                tint = if (colors.isGlassmorphism) colors.primaryAccentText
                                else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
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
        targetValue = if (isPressed) 28.dp else 22.dp,
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
                    alignment = Alignment.TopCenter,
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
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = if (colors.isGlassmorphism) colors.secondaryAccent else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .size(12.dp)
                                    .offset(x = arrowOffset)
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
            .aspectRatio(0.64f)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(shape)
            .then(
                if (colors.isGlassmorphism) {
                    Modifier.border(
                        1.dp,
                        Brush.verticalGradient(listOf(Color(0x65FFFFFF), Color(0x18FFFFFF))),
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
            WallpaperImage(
                source = item.source,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
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
