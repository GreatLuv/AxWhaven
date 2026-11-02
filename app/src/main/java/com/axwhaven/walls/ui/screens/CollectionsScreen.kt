package com.axwhaven.walls.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axwhaven.walls.WallpaperUiState
import com.axwhaven.walls.model.ArtDirectory
import com.axwhaven.walls.model.WallpaperItem
import com.axwhaven.walls.ui.components.ArtsCountHeaderBadge
import com.axwhaven.walls.ui.theme.AxWhavenColors
import com.axwhaven.walls.ui.theme.ExpressiveMotion
import com.axwhaven.walls.ui.theme.FloatingDockClearance
import com.axwhaven.walls.ui.theme.MainScreenBottomClearance

@Composable
fun CollectionsScreen(
    uiState: WallpaperUiState,
    colors: AxWhavenColors,
    onDirectorySelect: (ArtDirectory?) -> Unit,
    onWallpaperPreview: (WallpaperItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomClearance = MainScreenBottomClearance
    val collectionsGridState = rememberLazyGridState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (uiState.appBackgroundItem != null || colors.isGlassmorphism) Color.Transparent else colors.background
            )
    ) {
        if (uiState.selectedDirectory != null) {
            // =========================================================================
            // FOLDER DRILL-DOWN VIEW (SHOWING WALLPAPERS IN SELECTED DIRECTORY)
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
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
            // DEDICATED COLLECTIONS ROOT PAGE
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                // Top Collections Header Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp)
                ) {
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
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Collections",
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .weight(1f)
                    ) {
                        Text(
                            text = "Collections",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Curated offline anime portfolios",
                            fontSize = 12.5.sp,
                            color = colors.textSecondary
                        )
                    }

                    ArtsCountHeaderBadge(
                        text = "${uiState.directories.size} folders",
                        colors = colors
                    )
                }

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
                        items(
                            items = uiState.directories,
                            key = { it.id },
                            contentType = { "collection_card" }
                        ) { directory ->
                            CollectionArtCard(
                                directory = directory,
                                colors = colors,
                                onClick = { onDirectorySelect(directory) }
                            )
                        }
                    }
                }
            }
        }
    }
}
