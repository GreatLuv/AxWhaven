package com.axwhaven.walls.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.axwhaven.walls.WallpaperUiState
import com.axwhaven.walls.model.*
import com.axwhaven.walls.ui.components.M3ExpressiveMorphingLoader
import com.axwhaven.walls.ui.components.WallpaperImage
import com.axwhaven.walls.ui.theme.AxWhavenColors
import com.axwhaven.walls.ui.theme.ExpressiveMotion
import com.axwhaven.walls.ui.theme.FloatingDockClearance
import com.axwhaven.walls.ui.theme.FrostyFastBadgeBgGreen
import com.axwhaven.walls.ui.theme.FrostyFastBadgeGreen
import com.axwhaven.walls.ui.theme.FrostyFastIconBoxBgGreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HavenSurfScreen(
    uiState: WallpaperUiState,
    colors: AxWhavenColors,
    onProviderSelect: (ProviderSite) -> Unit,
    onQueryChange: (String) -> Unit,
    onApplySuggestion: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSubmitSearch: () -> Unit,
    onOpenFilterSheet: () -> Unit,
    onApplyFilters: (HavenSurfFilterState) -> Unit,
    onResetFilters: () -> Unit,
    onLoadNextPage: () -> Unit,
    onWallpaperPreview: (WallpaperItem) -> Unit,
    onDownloadItem: (WallpaperItem, Boolean) -> Unit = { _, _ -> },
    onSaveKonachanCookie: (String) -> Unit = {},
    onDismissKonachanCookieDialog: () -> Unit = {},
    onOpenKonachanCookieDialog: () -> Unit = {},
    onSaveDanbooruCookie: (String) -> Unit = {},
    onDismissDanbooruCookieDialog: () -> Unit = {},
    onOpenDanbooruCookieDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showProviderDropdown by remember { mutableStateOf(false) }
    var downloadQualityTargetItem by remember { mutableStateOf<WallpaperItem?>(null) }
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val isImeVisible = WindowInsets.isImeVisible
    val currentImeVisible by rememberUpdatedState(isImeVisible)
    var isSearchFocused by remember { mutableStateOf(false) }
    var inFlightSheetJob by remember { mutableStateOf<Job?>(null) }

    // Safe Sheet Opener: prevents keyboard focus race & double-bounce
    val openSheetSafely: (() -> Unit) -> Unit = { openAction ->
        if (inFlightSheetJob?.isActive != true) {
            if (isImeVisible || isSearchFocused) {
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
                inFlightSheetJob = coroutineScope.launch {
                    withTimeoutOrNull(400L) {
                        snapshotFlow { currentImeVisible }.first { !it }
                    }
                    delay(50L) // settle margin
                    openAction()
                }
            } else {
                focusManager.clearFocus(force = true)
                openAction()
            }
        }
    }

    // Search execution handler: fires simultaneous crisp Confirm haptic with query submission
    var lastSearchQuery by remember { mutableStateOf<String?>(null) }
    var lastSearchTimestamp by remember { mutableLongStateOf(0L) }

    val handleExecuteSearch: () -> Unit = {
        val currentQuery = uiState.searchQueryText.trim()
        val now = System.currentTimeMillis()
        if (currentQuery.isNotEmpty() && (currentQuery != lastSearchQuery || now - lastSearchTimestamp > 400L)) {
            lastSearchQuery = currentQuery
            lastSearchTimestamp = now
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            onSubmitSearch()
        } else if (currentQuery.isEmpty() && uiState.remoteResults.isNotEmpty()) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            onSubmitSearch()
        }
    }

    // Scroll Direction Vanish (~10.dp threshold + settle margin)
    val gridState = rememberLazyGridState()
    var isScrollingDown by rememberSaveable { mutableStateOf(false) }
    var scrollAccumulator by remember { mutableFloatStateOf(0f) }
    var lastVisibilityChangeTime by remember { mutableLongStateOf(0L) }
    val scrollThresholdPx = with(LocalDensity.current) { 10.dp.toPx() }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val now = System.currentTimeMillis()
                // Anti-feedback-loop: ignore scroll deltas for ~350ms after visibility toggle
                if (now - lastVisibilityChangeTime < 350L) {
                    return Offset.Zero
                }
                // Never collapse while search is focused or IME is visible
                if (isSearchFocused || isImeVisible) {
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

    // Always show header when at the top of the grid
    LaunchedEffect(gridState) {
        snapshotFlow {
            gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
        }.collect { isAtTop ->
            if (isAtTop && isScrollingDown) {
                isScrollingDown = false
                scrollAccumulator = 0f
            }
        }
    }

    val shouldShowTitleRow = !isScrollingDown || isSearchFocused || isImeVisible

    val activeFilterCount = (if (uiState.filterState.rating != ContentRating.SFW) 1 else 0) +
            (if (uiState.filterState.resolution != ResolutionFilter.ALL) 1 else 0) +
            (if (uiState.filterState.aspectRatio != AspectRatioFilter.UNRESTRICTED) 1 else 0)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (uiState.appBackgroundItem != null || colors.isGlassmorphism) Color.Transparent else colors.background
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
        ) {
            // =========================================================================
            // TOP HEADER: HAVEN SURF DISCOVERY HUB & PROVIDER SELECTOR PILL (COLLAPSIBLE)
            // =========================================================================
            AnimatedVisibility(
                visible = shouldShowTitleRow,
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
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AxW Surf",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Online wallpaper explorer",
                            fontSize = 12.5.sp,
                            color = colors.textSecondary
                        )
                    }

                    // Interactive Provider Selector Pill
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (colors.isGlassmorphism) Color(0x2EFFFFFF) else colors.surfaceElevated)
                                .then(
                                    if (colors.isGlassmorphism) {
                                        Modifier.border(
                                            1.dp,
                                            Brush.verticalGradient(listOf(Color(0x75FFFFFF), Color(0x25FFFFFF))),
                                            RoundedCornerShape(20.dp)
                                        )
                                    } else Modifier
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = colors.primaryAccent),
                                    onClick = { openSheetSafely { showProviderDropdown = true } }
                                )
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(colors.primaryAccent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = uiState.activeProvider.displayName,
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Provider",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Refined Provider Selector Dropdown Menu
                        val menuShape = RoundedCornerShape(24.dp)
                        val menuBorderStroke = if (colors.isGlassmorphism) {
                            BorderStroke(1.dp, Brush.verticalGradient(listOf(Color(0x75FFFFFF), Color(0x25FFFFFF))))
                        } else null

                        DropdownMenu(
                            expanded = showProviderDropdown,
                            onDismissRequest = { showProviderDropdown = false },
                            shape = menuShape,
                            containerColor = if (colors.isGlassmorphism) Color(0xEB11172A) else colors.surfaceContainerHigh,
                            border = menuBorderStroke,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .width(210.dp)
                                .clip(menuShape)
                        ) {
                            ProviderSite.values().forEach { provider ->
                                val isSelected = provider == uiState.activeProvider
                                val itemShape = RoundedCornerShape(14.dp)
                                DropdownMenuItem(
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .clip(itemShape)
                                        .background(
                                            if (isSelected) {
                                                if (colors.isGlassmorphism) Color(0x38FFFFFF)
                                                else MaterialTheme.colorScheme.secondaryContainer
                                            } else Color.Transparent
                                        ),
                                    text = {
                                        Text(
                                            text = provider.displayName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) {
                                                if (colors.isGlassmorphism) colors.primaryAccent else MaterialTheme.colorScheme.onSecondaryContainer
                                            } else colors.textPrimary,
                                            fontSize = 14.sp
                                        )
                                    },
                                    trailingIcon = {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = if (colors.isGlassmorphism) colors.primaryAccent else MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        showProviderDropdown = false
                                        onProviderSelect(provider)
                                    },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Filter Sheet Button with Badge
                    Box {
                        IconButton(
                            onClick = { openSheetSafely { onOpenFilterSheet() } },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (activeFilterCount > 0) colors.primaryAccent.copy(alpha = 0.25f)
                                    else if (colors.isGlassmorphism) Color(0x2EFFFFFF)
                                    else colors.surfaceElevated
                                )
                                .border(
                                    1.dp,
                                    if (activeFilterCount > 0) Brush.linearGradient(listOf(colors.primaryAccent, colors.primaryAccent))
                                    else if (colors.isGlassmorphism) Brush.verticalGradient(listOf(Color(0x75FFFFFF), Color(0x25FFFFFF)))
                                    else Brush.linearGradient(listOf(colors.cardBorder, colors.cardBorder)),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filters",
                                tint = if (activeFilterCount > 0) colors.primaryAccent else colors.textPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        if (activeFilterCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(colors.primaryAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activeFilterCount.toString(),
                                    color = colors.primaryAccentText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    val isKonachanNsfw = uiState.activeProvider == ProviderSite.KONACHAN && uiState.filterState.rating != ContentRating.SFW
                    val isDanbooruActive = uiState.activeProvider == ProviderSite.DANBOORU
                    if (isKonachanNsfw || isDanbooruActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (colors.isGlassmorphism) Color(0x35FFFFFF) else colors.surfaceElevated
                                )
                                .border(
                                    1.dp,
                                    Brush.linearGradient(listOf(colors.primaryAccent, colors.primaryAccent)),
                                    CircleShape
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = colors.primaryAccent),
                                    onClick = if (isDanbooruActive) onOpenDanbooruCookieDialog else onOpenKonachanCookieDialog
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = if (isDanbooruActive) "Danbooru Cookie" else "Konachan Cookie",
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // PINNED SEARCH BAR WITH TACTILE HAPTICS ON SEARCH EXECUTION
            // =========================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                TextField(
                    value = uiState.searchQueryText,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            "Search ${uiState.activeProvider.displayName} wallpapers...",
                            color = colors.textMuted,
                            fontSize = 13.5.sp
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { handleExecuteSearch() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isSearchFocused = it.isFocused }
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (colors.isGlassmorphism) {
                                Brush.verticalGradient(listOf(Color(0x2EFFFFFF), Color(0x18FFFFFF)))
                            } else {
                                Brush.linearGradient(listOf(colors.surfaceContainerHigh, colors.surfaceContainerHigh))
                            }
                        )
                        .then(
                            if (colors.isGlassmorphism) {
                                Modifier.border(
                                    1.dp,
                                    Brush.verticalGradient(listOf(Color(0x75FFFFFF), Color(0x25FFFFFF))),
                                    RoundedCornerShape(24.dp)
                                )
                            } else Modifier
                        ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = colors.primaryAccent
                    ),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.searchQueryText.isNotBlank()) {
                                IconButton(
                                    onClick = onClearQuery,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = handleExecuteSearch,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Search",
                                    tint = colors.primaryAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                )
            }

            // Dynamic Autocomplete Tag Suggestions Row
            AnimatedVisibility(
                visible = uiState.tagSuggestions.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uiState.tagSuggestions) { tag ->
                        AssistChip(
                            onClick = { onApplySuggestion(tag) },
                            label = {
                                Text(
                                    "#$tag",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (colors.isGlassmorphism) Color(0x2BFFFFFF) else colors.surfaceElevated,
                                labelColor = colors.textPrimary
                            ),
                            border = AssistChipDefaults.assistChipBorder(
                                true,
                                borderColor = if (colors.isGlassmorphism) Color(0x55FFFFFF) else colors.cardBorder
                            )
                        )
                    }
                }
            }

            // =========================================================================
            // SEARCH RESULTS GRID + PAGINATION
            // =========================================================================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.isSearching) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            M3ExpressiveMorphingLoader(
                                loaderSize = 56.dp,
                                colors = colors
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            // Gentle pulsing alpha using tween (explicit exception to no-tween rule per spec for infiniteRepeatable)
                            val infinitePulse = rememberInfiniteTransition(label = "SearchPulsingTransition")
                            val pulseAlpha by infinitePulse.animateFloat(
                                initialValue = 0.55f,
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween<Float>(durationMillis = 1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "SearchPulseAlpha"
                            )
                            Text(
                                text = "Searching ${uiState.activeProvider.displayName}...",
                                color = (if (colors.isGlassmorphism) colors.textPrimary else MaterialTheme.colorScheme.onSurface).copy(alpha = pulseAlpha),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                } else if (uiState.remoteResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ImageSearch,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = colors.primaryAccent
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Discover Anime Wallpapers",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Explore trending wallpapers from ${uiState.activeProvider.displayName} or try popular searches:",
                                fontSize = 13.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(18.dp))

                            // Popular Search Starter Pills
                            val popularTags = listOf("Anime", "Genshin", "Cyberpunk", "Catgirl", "Landscape", "Vocaloid")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                popularTags.take(3).forEach { tag ->
                                    AssistChip(
                                        onClick = { onApplySuggestion(tag) },
                                        label = { Text("#$tag", fontSize = 12.sp) },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (colors.isGlassmorphism) Color(0x2BFFFFFF) else colors.surfaceElevated,
                                            labelColor = colors.primaryAccent
                                        ),
                                        border = AssistChipDefaults.assistChipBorder(
                                            true,
                                            borderColor = if (colors.isGlassmorphism) Color(0x55FFFFFF) else colors.cardBorder
                                        ),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                popularTags.drop(3).forEach { tag ->
                                    AssistChip(
                                        onClick = { onApplySuggestion(tag) },
                                        label = { Text("#$tag", fontSize = 12.sp) },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (colors.isGlassmorphism) Color(0x2BFFFFFF) else colors.surfaceElevated,
                                            labelColor = colors.primaryAccent
                                        ),
                                        border = AssistChipDefaults.assistChipBorder(
                                            true,
                                            borderColor = if (colors.isGlassmorphism) Color(0x55FFFFFF) else colors.cardBorder
                                        ),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = FloatingDockClearance),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(nestedScrollConnection)
                    ) {
                        items(
                            items = uiState.remoteResults,
                            key = { it.id },
                            contentType = { "wallpaper_surf_card" }
                        ) { item ->
                            val isDownloading = uiState.downloadingItemIds.contains(item.id)
                            val isDownloaded = uiState.downloadedItemIds.contains(item.id) ||
                                    uiState.allLocalWallpapers.any { it.id == item.id || (it.title.isNotBlank() && it.title == item.title) }

                            HavenSurfResultCard(
                                item = item,
                                colors = colors,
                                isDownloading = isDownloading,
                                isDownloaded = isDownloaded,
                                onClick = { onWallpaperPreview(item) },
                                onDownload = { downloadQualityTargetItem = item }
                            )
                        }

                        // Pagination Footer Item
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.isLoadingMore) {
                                    M3ExpressiveMorphingLoader(
                                        loaderSize = 36.dp,
                                        colors = colors
                                    )
                                } else if (uiState.canLoadMore) {
                                    Button(
                                        onClick = onLoadNextPage,
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.surfaceElevated,
                                            contentColor = colors.textPrimary
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.cardBorder),
                                        modifier = Modifier.height(44.dp)
                                    ) {
                                        Text("Next Page (${uiState.currentPage + 1})", fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        "Reached final page",
                                        color = colors.textMuted,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Status Bar Scrim Overlay (pinned at the very top of the screen)
        val statusBarScrim = if (colors.isGlassmorphism) {
            Brush.verticalGradient(
                listOf(
                    Color(0xD9090C19),
                    Color(0x99090C19),
                    Color.Transparent
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    colors.background,
                    colors.background.copy(alpha = 0.92f),
                    Color.Transparent
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(statusBarScrim)
                .align(Alignment.TopCenter)
        )

        // =========================================================================
        // ADVANCED FILTER MODAL SHEET
        // =========================================================================
        if (uiState.isFilterSheetVisible) {
            HavenSurfFilterModalSheet(
                currentFilters = uiState.filterState,
                colors = colors,
                onDismiss = onResetFilters,
                onApply = onApplyFilters
            )
        }

        if (uiState.showKonachanCookieDialog) {
            ProviderCookieDialog(
                providerName = "Konachan",
                verificationUrl = "https://konachan.com/post",
                cookieDomain = "https://konachan.com",
                infoText = "Konachan uncensored/NSFW works are hosted on konachan.com and guarded by Cloudflare. Choose an option to authenticate:",
                currentCookie = uiState.konachanCookieText,
                onSaveCookie = onSaveKonachanCookie,
                onDismiss = onDismissKonachanCookieDialog,
                colors = colors
            )
        }

        if (uiState.showDanbooruCookieDialog) {
            ProviderCookieDialog(
                providerName = "Danbooru",
                verificationUrl = "https://danbooru.donmai.us/posts",
                cookieDomain = "https://danbooru.donmai.us",
                infoText = "Danbooru is protected by Cloudflare Turnstile anti-bot challenge. Complete verification in the in-app browser or paste your cookie to search:",
                currentCookie = uiState.danbooruCookieText,
                onSaveCookie = onSaveDanbooruCookie,
                onDismiss = onDismissDanbooruCookieDialog,
                colors = colors
            )
        }

        if (downloadQualityTargetItem != null) {
            DownloadQualityDialog(
                item = downloadQualityTargetItem!!,
                onDownloadOriginal = {
                    val target = downloadQualityTargetItem!!
                    downloadQualityTargetItem = null
                    onDownloadItem(target, true)
                },
                onDownloadCompressed = {
                    val target = downloadQualityTargetItem!!
                    downloadQualityTargetItem = null
                    onDownloadItem(target, false)
                },
                onDismiss = { downloadQualityTargetItem = null },
                colors = colors
            )
        }
    }
}

@Composable
fun HavenSurfResultCard(
    item: WallpaperItem,
    colors: AxWhavenColors,
    isDownloading: Boolean = false,
    isDownloaded: Boolean = false,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = ExpressiveMotion.FastSpatialFloat,
        label = "HavenSurfCardScale"
    )

    val cardCornerRadius by animateDpAsState(
        targetValue = if (isPressed) 28.dp else 20.dp,
        animationSpec = ExpressiveMotion.FastSpatialDp,
        label = "HavenSurfCardCorner"
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

            // Smooth Multi-stop Scrim Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.5f to Color.Transparent,
                                0.78f to colors.scrimEnd.copy(alpha = 0.6f),
                                1.0f to colors.scrimEnd
                            )
                        )
                    )
            )

            // Resolution badge top-end
            if (item.width > 0 && item.height > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xB3000000))
                        .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${item.width}x${item.height}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Bottom metadata & Download action
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                val dlInteractionSource = remember { MutableInteractionSource() }
                val isDlPressed by dlInteractionSource.collectIsPressedAsState()

                val targetRadius = when {
                    isDownloaded -> 22.dp
                    isDownloading -> 22.dp
                    isDlPressed -> 22.dp
                    else -> 14.dp
                }
                val dlCornerRadius by animateDpAsState(
                    targetValue = targetRadius,
                    animationSpec = ExpressiveMotion.FastSpatialDp,
                    label = "HavenSurfDlCorner"
                )
                val dlShape = RoundedCornerShape(dlCornerRadius.coerceAtLeast(0.dp))

                val checkPopAnimatable = remember { Animatable(1.0f) }
                LaunchedEffect(isDownloaded) {
                    if (isDownloaded) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        checkPopAnimatable.snapTo(1.22f)
                        checkPopAnimatable.animateTo(
                            targetValue = 1.0f,
                            animationSpec = ExpressiveMotion.FastSpatialFloat
                        )
                    }
                }

                val pressScale by animateFloatAsState(
                    targetValue = if (isDlPressed && !isDownloading) 0.88f else 1.0f,
                    animationSpec = ExpressiveMotion.FastSpatialFloat,
                    label = "HavenSurfDlPressScale"
                )
                val currentScale = pressScale * checkPopAnimatable.value

                val targetContainerColor = when {
                    isDownloaded -> if (colors.isGlassmorphism) colors.primaryAccent else MaterialTheme.colorScheme.primary
                    isDownloading -> if (colors.isGlassmorphism) Color(0x60FFFFFF) else MaterialTheme.colorScheme.primaryContainer
                    else -> if (colors.isGlassmorphism) Color(0xB3000000) else MaterialTheme.colorScheme.secondaryContainer
                }
                val containerColor by animateColorAsState(
                    targetValue = targetContainerColor,
                    animationSpec = ExpressiveMotion.FastEffectsColor,
                    label = "HavenSurfDlColor"
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsLayer {
                            scaleX = currentScale
                            scaleY = currentScale
                        }
                        .clip(dlShape)
                        .background(containerColor)
                        .then(
                            if (colors.isGlassmorphism) {
                                Modifier.border(0.5.dp, Color(0x33FFFFFF), dlShape)
                            } else Modifier
                        )
                        .clickable(
                            enabled = !isDownloading,
                            interactionSource = dlInteractionSource,
                            indication = ripple(bounded = true, color = colors.primaryAccent),
                            onClick = {
                                if (isDownloaded) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDownload()
                                } else {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDownload()
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(
                        targetState = when {
                            isDownloaded -> 2
                            isDownloading -> 1
                            else -> 0
                        },
                        label = "HavenSurfDlContent"
                    ) { state ->
                        when (state) {
                            1 -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.2.dp,
                                    color = if (colors.isGlassmorphism) Color.White else MaterialTheme.colorScheme.primary
                                )
                            }
                            2 -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Downloaded",
                                    tint = if (colors.isGlassmorphism) colors.primaryAccentText else MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = if (colors.isGlassmorphism) colors.primaryAccent else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HavenSurfFilterModalSheet(
    currentFilters: HavenSurfFilterState,
    colors: AxWhavenColors,
    onDismiss: () -> Unit,
    onApply: (HavenSurfFilterState) -> Unit
) {
    var selectedRes by remember { mutableStateOf(currentFilters.resolution) }
    var selectedAspect by remember { mutableStateOf(currentFilters.aspectRatio) }
    var selectedRating by remember { mutableStateOf(currentFilters.rating) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        contentWindowInsets = { WindowInsets.navigationBars },
        containerColor = if (colors.isGlassmorphism) Color(0xF2101526) else colors.surfaceElevated,
        dragHandle = { BottomSheetDefaults.DragHandle(color = if (colors.isGlassmorphism) Color(0x60FFFFFF) else colors.cardBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Search Filters",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                TextButton(onClick = {
                    selectedRes = ResolutionFilter.ALL
                    selectedAspect = AspectRatioFilter.UNRESTRICTED
                    selectedRating = ContentRating.SFW
                }) {
                    Text("Reset All", color = colors.primaryAccent, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Content Rating Filter
            Text("Content Rating", fontWeight = FontWeight.SemiBold, color = colors.textPrimary, fontSize = 13.5.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ContentRating.values().forEach { rating ->
                    val (label, indicatorColor) = when (rating) {
                        ContentRating.SFW -> "SFW" to colors.mintText
                        ContentRating.QUESTIONABLE -> "Sketchy" to Color(0xFFF59E0B)
                        ContentRating.NSFW -> "NSFW" to Color(0xFFEF4444)
                        ContentRating.ALL -> "All" to colors.textMuted
                    }
                    FilterPillItem(
                        label = label,
                        indicatorColor = indicatorColor,
                        isSelected = selectedRating == rating,
                        colors = colors,
                        onClick = { selectedRating = rating },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Minimum Resolution Filter (2-Row comfortable grid to prevent crushing)
            Text("Minimum Resolution", fontWeight = FontWeight.SemiBold, color = colors.textPrimary, fontSize = 13.5.sp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        ResolutionFilter.ALL to "All Res",
                        ResolutionFilter.HD_720P to "720p+",
                        ResolutionFilter.FHD_1080P to "1080p+"
                    ).forEach { (res, label) ->
                        FilterPillItem(
                            label = label,
                            isSelected = selectedRes == res,
                            colors = colors,
                            onClick = { selectedRes = res },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        ResolutionFilter.QHD_1440P to "1440p (2K+)",
                        ResolutionFilter.UHD_4K to "2160p (4K+)"
                    ).forEach { (res, label) ->
                        FilterPillItem(
                            label = label,
                            isSelected = selectedRes == res,
                            colors = colors,
                            onClick = { selectedRes = res },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Aspect Ratio Filter (2-Row comfortable layout)
            Text("Aspect Ratio", fontWeight = FontWeight.SemiBold, color = colors.textPrimary, fontSize = 13.5.sp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        AspectRatioFilter.UNRESTRICTED to "Any Ratio",
                        AspectRatioFilter.PORTRAIT to "Portrait (9:16)"
                    ).forEach { (ar, label) ->
                        FilterPillItem(
                            label = label,
                            isSelected = selectedAspect == ar,
                            colors = colors,
                            onClick = { selectedAspect = ar },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        AspectRatioFilter.LANDSCAPE to "Landscape (16:9)",
                        AspectRatioFilter.SQUARE to "Square (1:1)"
                    ).forEach { (ar, label) ->
                        FilterPillItem(
                            label = label,
                            isSelected = selectedAspect == ar,
                            colors = colors,
                            onClick = { selectedAspect = ar },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Apply Filters CTA
            Button(
                onClick = {
                    onApply(
                        HavenSurfFilterState(
                            resolution = selectedRes,
                            aspectRatio = selectedAspect,
                            rating = selectedRating
                        )
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryAccent,
                    contentColor = colors.primaryAccentText
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Apply Filters", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun FilterPillItem(
    label: String,
    isSelected: Boolean,
    colors: AxWhavenColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    indicatorColor: Color? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current

    val targetRadius = when {
        isPressed -> 8.dp
        isSelected -> 28.dp
        else -> 12.dp
    }
    val cornerRadius by animateDpAsState(
        targetValue = targetRadius,
        animationSpec = ExpressiveMotion.FastSpatialDp,
        label = "FilterPillCorner"
    )
    val safeRadius = cornerRadius.coerceAtLeast(0.dp)
    val shape = RoundedCornerShape(safeRadius)

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = ExpressiveMotion.FastSpatialFloat,
        label = "FilterPillScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(
                if (isSelected) {
                    if (colors.isGlassmorphism) colors.primaryAccent else MaterialTheme.colorScheme.secondaryContainer
                } else if (colors.isGlassmorphism) Color(0x28FFFFFF)
                else if (colors.isExpressive) colors.surfaceContainerHigh
                else colors.surface
            )
            .then(
                if (colors.isGlassmorphism) {
                    Modifier.border(
                        1.dp,
                        if (isSelected) colors.primaryAccent else Color(0x55FFFFFF),
                        shape
                    )
                } else Modifier // Zero border in M3 Expressive
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = colors.primaryAccent),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (indicatorColor != null) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) {
                                if (colors.isGlassmorphism) colors.primaryAccentText else MaterialTheme.colorScheme.onSecondaryContainer
                            } else indicatorColor
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) {
                    if (colors.isGlassmorphism) colors.primaryAccentText else MaterialTheme.colorScheme.onSecondaryContainer
                } else colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ProviderCookieDialog(
    providerName: String,
    verificationUrl: String,
    cookieDomain: String,
    infoText: String,
    currentCookie: String,
    onSaveCookie: (String) -> Unit,
    onDismiss: () -> Unit,
    colors: AxWhavenColors
) {
    var isWebViewVisible by remember { mutableStateOf(false) }
    var cookieText by remember { mutableStateOf(currentCookie) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (colors.isGlassmorphism) Color(0xF0181822) else colors.surfaceElevated
            ),
            border = BorderStroke(1.dp, colors.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.primaryAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$providerName Verification",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Cloudflare protection bypass",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isWebViewVisible) {
                    var statusNote by remember { mutableStateOf("Complete the Cloudflare verification challenge below.") }
                    var hasValidCookie by remember { mutableStateOf(false) }

                    Text(
                        text = statusNote,
                        fontSize = 12.sp,
                        color = if (hasValidCookie) FrostyFastBadgeGreen else colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(12.dp))
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.databaseEnabled = true
                                    settings.useWideViewPort = true
                                    settings.loadWithOverviewMode = true
                                    settings.userAgentString = WebSettings.getDefaultUserAgent(ctx)
                                    val cookieManager = CookieManager.getInstance()
                                    cookieManager.setAcceptCookie(true)
                                    cookieManager.setAcceptThirdPartyCookies(this, true)

                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            cookieManager.flush()
                                            val c = cookieManager.getCookie(cookieDomain)
                                            val title = view?.title.orEmpty()
                                            val isChallenge = title.contains("Just a moment", ignoreCase = true) ||
                                                title.contains("Cloudflare", ignoreCase = true) ||
                                                url?.contains("__cf_chl") == true ||
                                                url?.contains("challenge") == true

                                            if (!c.isNullOrBlank() && c.contains("cf_clearance")) {
                                                hasValidCookie = true
                                                if (!isChallenge) {
                                                    statusNote = "Verification successful! Cookie captured."
                                                    onSaveCookie(c)
                                                } else {
                                                    statusNote = "Challenge active. Please complete the Turnstile checkbox."
                                                }
                                            } else {
                                                statusNote = if (isChallenge) {
                                                    "Challenge active. Please complete the Turnstile checkbox."
                                                } else {
                                                    "Complete the verification challenge above, then tap 'Capture & Save Cookie'."
                                                }
                                            }
                                        }
                                    }
                                    loadUrl(verificationUrl)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val cm = CookieManager.getInstance()
                            cm.flush()
                            val c = cm.getCookie(cookieDomain).orEmpty()
                            if (c.contains("cf_clearance") || c.isNotBlank()) {
                                onSaveCookie(c)
                            } else {
                                statusNote = "Challenge not yet verified. Please complete the checkbox above first."
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryAccent,
                            contentColor = colors.primaryAccentText
                        ),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Capture & Save Cookie", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = infoText,
                        fontSize = 12.5.sp,
                        color = colors.textSecondary,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { isWebViewVisible = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryAccent,
                            contentColor = colors.primaryAccentText
                        ),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verify in In-App Browser", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = colors.cardBorder)
                        Text("  OR PASTE COOKIE  ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.textMuted)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = colors.cardBorder)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = cookieText,
                        onValueChange = { cookieText = it },
                        label = { Text("cf_clearance or full cookie", fontSize = 12.sp) },
                        placeholder = { Text("cf_clearance=...; ...", fontSize = 11.sp, color = colors.textMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primaryAccent,
                            unfocusedBorderColor = colors.cardBorder,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(44.dp),
                            border = BorderStroke(1.dp, colors.cardBorder)
                        ) {
                            Text("Cancel", color = colors.textSecondary)
                        }

                        Button(
                            onClick = {
                                val trimmed = cookieText.trim()
                                if (trimmed.isNotBlank()) {
                                    runCatching {
                                        trimmed.split(";").forEach { part ->
                                            if (part.isNotBlank()) {
                                                CookieManager.getInstance().setCookie(cookieDomain, part.trim())
                                            }
                                        }
                                        CookieManager.getInstance().flush()
                                    }
                                }
                                onSaveCookie(trimmed)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primaryAccent,
                                contentColor = colors.primaryAccentText
                            ),
                            modifier = Modifier.weight(1.5f).height(44.dp)
                        ) {
                            Text("Save & Search", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadQualityDialog(
    item: WallpaperItem,
    onDownloadOriginal: () -> Unit,
    onDownloadCompressed: () -> Unit,
    onDismiss: () -> Unit,
    colors: AxWhavenColors
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
                .background(Color(0x8A000000))
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
                    containerColor = if (colors.isGlassmorphism) Color(0xF2181C2E) else colors.surfaceElevated
                ),
                border = BorderStroke(
                    1.dp,
                    if (colors.isGlassmorphism) Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x22FFFFFF)))
                    else Brush.linearGradient(listOf(colors.cardBorder, colors.cardBorder))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clickable(enabled = false, onClick = {})
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(colors.primaryAccent.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Download Quality",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = item.title,
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Option 1: Original Resolution (Top Card of Segmented Group)
                    DownloadOptionCard(
                        isTop = true,
                        title = "Original Resolution",
                        subtitle = "Full uncompressed quality as uploaded (PNG/Original)",
                        badge = "Pristine",
                        badgeBgColor = if (colors.isGlassmorphism) colors.primaryAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                        badgeTextColor = if (colors.isGlassmorphism) colors.primaryAccent else MaterialTheme.colorScheme.primary,
                        iconBoxBgColor = if (colors.isGlassmorphism) colors.primaryAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                        iconTint = if (colors.isGlassmorphism) colors.primaryAccent else MaterialTheme.colorScheme.primary,
                        icon = Icons.Default.HighQuality,
                        colors = colors,
                        onClick = onDownloadOriginal
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // Option 2: High-Quality JPEG (Bottom Card of Segmented Group)
                    DownloadOptionCard(
                        isTop = false,
                        title = "High-Quality JPEG",
                        subtitle = "Fast download, optimized file size with full visual fidelity",
                        badge = "Fast",
                        badgeBgColor = if (colors.isGlassmorphism) FrostyFastBadgeBgGreen else MaterialTheme.colorScheme.tertiaryContainer,
                        badgeTextColor = if (colors.isGlassmorphism) FrostyFastBadgeGreen else MaterialTheme.colorScheme.onTertiaryContainer,
                        iconBoxBgColor = if (colors.isGlassmorphism) FrostyFastIconBoxBgGreen else MaterialTheme.colorScheme.tertiaryContainer,
                        iconTint = if (colors.isGlassmorphism) FrostyFastBadgeGreen else MaterialTheme.colorScheme.onTertiaryContainer,
                        icon = Icons.Default.Speed,
                        colors = colors,
                        onClick = onDownloadCompressed
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadOptionCard(
    isTop: Boolean,
    title: String,
    subtitle: String,
    badge: String,
    badgeBgColor: Color,
    badgeTextColor: Color,
    iconBoxBgColor: Color,
    iconTint: Color,
    icon: ImageVector,
    colors: AxWhavenColors,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedTopStart by animateDpAsState(
        targetValue = if (isPressed) 18.dp else if (isTop) 20.dp else 6.dp,
        animationSpec = ExpressiveMotion.FastSpatialDp,
        label = "DownloadTopStart"
    )
    val animatedTopEnd by animateDpAsState(
        targetValue = if (isPressed) 18.dp else if (isTop) 20.dp else 6.dp,
        animationSpec = ExpressiveMotion.FastSpatialDp,
        label = "DownloadTopEnd"
    )
    val animatedBottomStart by animateDpAsState(
        targetValue = if (isPressed) 18.dp else if (!isTop) 20.dp else 6.dp,
        animationSpec = ExpressiveMotion.FastSpatialDp,
        label = "DownloadBottomStart"
    )
    val animatedBottomEnd by animateDpAsState(
        targetValue = if (isPressed) 18.dp else if (!isTop) 20.dp else 6.dp,
        animationSpec = ExpressiveMotion.FastSpatialDp,
        label = "DownloadBottomEnd"
    )

    val currentShape = RoundedCornerShape(
        topStart = animatedTopStart.coerceAtLeast(0.dp),
        topEnd = animatedTopEnd.coerceAtLeast(0.dp),
        bottomEnd = animatedBottomEnd.coerceAtLeast(0.dp),
        bottomStart = animatedBottomStart.coerceAtLeast(0.dp)
    )

    val cardBg by animateColorAsState(
        targetValue = when {
            isPressed -> if (colors.isGlassmorphism) Color(0x40FFFFFF) else MaterialTheme.colorScheme.secondaryContainer
            else -> if (colors.isGlassmorphism) Color(0x28FFFFFF) else MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = ExpressiveMotion.FastEffectsColor,
        label = "DownloadCardBg"
    )

    val cardBorder = if (colors.isGlassmorphism) {
        Modifier.border(
            1.dp,
            Brush.verticalGradient(listOf(Color(0x44FFFFFF), Color(0x18FFFFFF))),
            currentShape
        )
    } else Modifier

    val animatedIconBoxBg by animateColorAsState(
        targetValue = iconBoxBgColor,
        animationSpec = ExpressiveMotion.FastEffectsColor,
        label = "DownloadIconBoxBg"
    )
    val animatedIconTint by animateColorAsState(
        targetValue = iconTint,
        animationSpec = ExpressiveMotion.FastEffectsColor,
        label = "DownloadIconTint"
    )
    val animatedBadgeBg by animateColorAsState(
        targetValue = badgeBgColor,
        animationSpec = ExpressiveMotion.FastEffectsColor,
        label = "DownloadBadgeBg"
    )
    val animatedBadgeText by animateColorAsState(
        targetValue = badgeTextColor,
        animationSpec = ExpressiveMotion.FastEffectsColor,
        label = "DownloadBadgeText"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(currentShape)
            .background(cardBg)
            .then(cardBorder)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = if (colors.isGlassmorphism) colors.primaryAccent else MaterialTheme.colorScheme.primary),
                onClick = onClick
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(animatedIconBoxBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = animatedIconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPressed && !colors.isGlassmorphism) MaterialTheme.colorScheme.onSecondaryContainer else colors.textPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(animatedBadgeBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = animatedBadgeText
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = if (isPressed && !colors.isGlassmorphism) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f) else colors.textSecondary,
                lineHeight = 15.sp
            )
        }
    }
}
