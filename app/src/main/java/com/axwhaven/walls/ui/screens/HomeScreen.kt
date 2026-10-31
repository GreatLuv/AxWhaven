package com.axwhaven.walls.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.axwhaven.walls.HomeNavigationTab
import com.axwhaven.walls.WallpaperUiState
import com.axwhaven.walls.model.ArtDirectory
import com.axwhaven.walls.model.WallpaperItem
import com.axwhaven.walls.model.WallpaperTarget
import com.axwhaven.walls.ui.components.GlassmorphicBadge
import com.axwhaven.walls.ui.components.WallpaperImage
import com.axwhaven.walls.ui.theme.AxWhavenColors

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
    var isDiceRollVisible by remember { mutableStateOf(true) }

    val collectionsGridState = rememberLazyGridState()
    val allWallpapersGridState = rememberLazyGridState()

    val isCollections = uiState.homeTab == HomeNavigationTab.COLLECTIONS

    val nestedScrollConnection = remember(isCollections) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // When scrolling down, hide the banner once scrolled past the top
                if (available.y < -12f) {
                    val activeState = if (isCollections) collectionsGridState else allWallpapersGridState
                    if (activeState.firstVisibleItemIndex > 0 || activeState.firstVisibleItemScrollOffset > 8) {
                        if (isDiceRollVisible) {
                            isDiceRollVisible = false
                        }
                    }
                }
                // Do NOT show banner when scrolling up mid-list.
                // The banner must ONLY reappear after reaching the full top.
                return Offset.Zero
            }
        }
    }

    // Monitor scroll position: Only reveal the banner when reaching the full top (index 0, offset <= 4)
    LaunchedEffect(isCollections) {
        val activeState = if (isCollections) collectionsGridState else allWallpapersGridState
        snapshotFlow {
            Pair(activeState.firstVisibleItemIndex, activeState.firstVisibleItemScrollOffset)
        }.collect { (index, offset) ->
            if (index == 0 && offset <= 4) {
                isDiceRollVisible = true
            } else if (index > 0 || offset > 20) {
                isDiceRollVisible = false
            }
        }
    }

    Box(
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
                    IconButton(
                        onClick = { onDirectorySelect(null) },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (colors.isGlassmorphism) Color(0x30FFFFFF) else colors.surfaceElevated)
                            .border(
                                1.dp,
                                if (colors.isGlassmorphism) Brush.verticalGradient(listOf(Color(0x70FFFFFF), Color(0x25FFFFFF)))
                                else Brush.linearGradient(listOf(colors.cardBorder, colors.cardBorder)),
                                CircleShape
                            )
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
                    GlassmorphicBadge(
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
                        contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
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
                // 1. TOP APP HEADER BAR
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

                    // Right side: Total Arts Count Pill Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.mintPillBg)
                            .border(1.dp, colors.mintPillBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(colors.mintText)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${uiState.totalArtsCount} arts",
                                color = colors.mintText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // 2. SEGMENTED NAVIGATION TOGGLE ("Collections" | "All Wallpapers")
                val isCollections = uiState.homeTab == HomeNavigationTab.COLLECTIONS
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
                        .border(
                            1.dp,
                            if (colors.isGlassmorphism) {
                                Brush.verticalGradient(listOf(Color(0x70FFFFFF), Color(0x25FFFFFF)))
                            } else {
                                Brush.linearGradient(listOf(colors.cardBorder, colors.cardBorder))
                            },
                            RoundedCornerShape(24.dp)
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val pillWidth = maxWidth / 2
                    val targetTabOffset = if (isCollections) 0f else 1f
                    val animatedTabOffset by animateFloatAsState(
                        targetValue = targetTabOffset,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "SegmentedPillOffset"
                    )

                    val activeGlassPill = if (colors.isGlassmorphism) Color(0x40FFFFFF) else colors.surfaceElevated

                    // Smooth horizontal sliding indicator pill
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

                    val collectionsTextColor by animateColorAsState(
                        targetValue = if (isCollections) colors.textPrimary else colors.textMuted,
                        label = "ColTabText"
                    )
                    val allWallpapersTextColor by animateColorAsState(
                        targetValue = if (!isCollections) colors.textPrimary else colors.textMuted,
                        label = "AllTabText"
                    )

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tab 1: Collections
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = colors.primaryAccent)
                                ) { onTabSelect(HomeNavigationTab.COLLECTIONS) },
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
                                Text(
                                    text = "Collections",
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isCollections) FontWeight.SemiBold else FontWeight.Medium,
                                    color = collectionsTextColor
                                )
                            }
                        }

                        // Tab 2: All Wallpapers
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = colors.primaryAccent)
                                ) { onTabSelect(HomeNavigationTab.ALL_WALLPAPERS) },
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
                                Text(
                                    text = "All Wallpapers",
                                    fontSize = 13.5.sp,
                                    fontWeight = if (!isCollections) FontWeight.SemiBold else FontWeight.Medium,
                                    color = allWallpapersTextColor
                                )
                            }
                        }
                    }
                }

                // 3. "SURPRISE ME" RANDOMIZER BANNER CARD (Auto-hides on scroll down, reappears on scroll up)
                AnimatedVisibility(
                    visible = isDiceRollVisible,
                    enter = fadeIn(animationSpec = tween(220)) + expandVertically(animationSpec = tween(250)),
                    exit = fadeOut(animationSpec = tween(180)) + shrinkVertically(animationSpec = tween(220))
                ) {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .border(1.dp, colors.surpriseGradientStroke, RoundedCornerShape(22.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = colors.primaryAccent)
                            ) { showDieDialog = true }
                    ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(colors.surpriseGradientStart, colors.surpriseGradientEnd)
                                )
                            )
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Left Icon Container: 48dp x 48dp rounded square with dice
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.surpriseIconBg)
                                    .border(1.dp, colors.primaryAccent.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Casino,
                                    contentDescription = null,
                                    tint = colors.primaryAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Center Text Stack
                            Column(
                                modifier = Modifier
                                    .padding(start = 14.dp)
                                    .weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Surprise Me",
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "✦",
                                        color = colors.primaryAccent,
                                        fontSize = 13.sp
                                    )
                                }
                                Text(
                                    text = "Roll a random unseen anime wallpaper",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            // Right Action Button
                                    Button(
                                        onClick = { showDieDialog = true },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.primaryAccent,
                                            contentColor = colors.primaryAccentText
                                        ),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .height(38.dp)
                                            .defaultMinSize(minWidth = 70.dp)
                                    ) {
                                        Text(
                                            text = "Roll",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.primaryAccentText
                                        )
                                    }
                                }
                            }
                        }
                    }

                // 4. MAIN CONTENT AREA (Collections Grid vs All Wallpapers Grid)
                AnimatedContent(
                    targetState = uiState.homeTab,
                    transitionSpec = {
                        if (targetState.ordinal > initialState.ordinal) {
                            // Forward (Collections -> All Wallpapers): slide in from right, slide out to left
                            (slideInHorizontally(
                                animationSpec = tween(320, easing = FastOutSlowInEasing),
                                initialOffsetX = { fullWidth -> fullWidth }
                            ) + fadeIn(animationSpec = tween(260))).togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(320, easing = FastOutSlowInEasing),
                                    targetOffsetX = { fullWidth -> -fullWidth }
                                ) + fadeOut(animationSpec = tween(220))
                            )
                        } else if (targetState.ordinal < initialState.ordinal) {
                            // Backward / Inverse (All Wallpapers -> Collections): slide in from left, slide out to right
                            (slideInHorizontally(
                                animationSpec = tween(320, easing = FastOutSlowInEasing),
                                initialOffsetX = { fullWidth -> -fullWidth }
                            ) + fadeIn(animationSpec = tween(260))).togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(320, easing = FastOutSlowInEasing),
                                    targetOffsetX = { fullWidth -> fullWidth }
                                ) + fadeOut(animationSpec = tween(220))
                            )
                        } else {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                        }
                    },
                    label = "HomeContentTransition",
                    modifier = Modifier.fillMaxSize()
                ) { currentTab ->
                    if (currentTab == HomeNavigationTab.COLLECTIONS) {
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
                                contentPadding = PaddingValues(top = 14.dp, bottom = 96.dp),
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
                    } else {
                        val allItems = uiState.allLocalWallpapers
                        if (allItems.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No wallpapers available.", color = colors.textSecondary)
                            }
                        } else {
                            LazyVerticalGrid(
                                state = allWallpapersGridState,
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(top = 14.dp, bottom = 96.dp),
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
fun CollectionArtCard(
    directory: ArtDirectory,
    colors: AxWhavenColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (colors.isGlassmorphism) Color(0x24FFFFFF) else colors.surface
        ),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .border(
                1.dp,
                if (colors.isGlassmorphism) Brush.verticalGradient(listOf(Color(0x70FFFFFF), Color(0x25FFFFFF)))
                else Brush.linearGradient(listOf(colors.cardBorder, colors.cardBorder)),
                RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
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
                    GlassmorphicBadge(
                        text = directory.totalArtsText,
                        colors = colors
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "Explore →",
                        color = colors.secondaryAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
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
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (colors.isGlassmorphism) Color(0x20FFFFFF) else colors.surface
        ),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.68f)
            .border(
                1.dp,
                if (colors.isGlassmorphism) Brush.verticalGradient(listOf(Color(0x65FFFFFF), Color(0x20FFFFFF)))
                else Brush.linearGradient(listOf(colors.cardBorder, colors.cardBorder)),
                RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.primaryAccent),
                onClick = onClick
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
