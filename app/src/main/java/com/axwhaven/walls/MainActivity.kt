package com.axwhaven.walls

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axwhaven.walls.model.PrimaryAppTab
import com.axwhaven.walls.model.WallpaperItem
import com.axwhaven.walls.ui.components.AxWhavenBottomNavBar
import com.axwhaven.walls.ui.components.WallpaperImage
import com.axwhaven.walls.ui.screens.HavenSurfScreen
import com.axwhaven.walls.ui.screens.HomeScreen
import com.axwhaven.walls.ui.screens.SettingsScreen
import com.axwhaven.walls.ui.screens.WallpaperPreviewScreen
import com.axwhaven.walls.ui.theme.AxWhavenDynamicTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val vm: WallpaperViewModel = viewModel(factory = WallpaperViewModel.provideFactory(this))
            val uiState by vm.uiState.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            var activePreviewItem by remember { mutableStateOf<WallpaperItem?>(null) }
            val tabNavigationHistory = remember { mutableStateListOf(PrimaryAppTab.HOME) }

            // Hierarchical Back Navigation Handler:
            // Home is the root of the app. Back exits ONLY on Home (root).
            // On other tabs, folders, or previews, back returns to the previous state/screen.
            val canGoBack = activePreviewItem != null ||
                    uiState.isFilterSheetVisible ||
                    (uiState.currentTab == PrimaryAppTab.HOME && uiState.selectedDirectory != null) ||
                    uiState.currentTab != PrimaryAppTab.HOME ||
                    tabNavigationHistory.size > 1

            BackHandler(enabled = canGoBack) {
                when {
                    activePreviewItem != null -> {
                        activePreviewItem = null
                    }
                    uiState.isFilterSheetVisible -> {
                        vm.setFilterSheetVisible(false)
                    }
                    uiState.currentTab == PrimaryAppTab.HOME && uiState.selectedDirectory != null -> {
                        vm.selectDirectory(null)
                    }
                    tabNavigationHistory.size > 1 -> {
                        tabNavigationHistory.removeAt(tabNavigationHistory.lastIndex)
                        val previousTab = tabNavigationHistory.lastOrNull() ?: PrimaryAppTab.HOME
                        vm.selectPrimaryTab(previousTab)
                    }
                    uiState.currentTab != PrimaryAppTab.HOME -> {
                        tabNavigationHistory.clear()
                        tabNavigationHistory.add(PrimaryAppTab.HOME)
                        vm.selectPrimaryTab(PrimaryAppTab.HOME)
                    }
                }
            }

            // SAF Folder picker launcher
            val folderLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri: Uri? ->
                if (uri != null) {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(uri, flags)
                    vm.setCustomStorageUri(uri)
                    Toast.makeText(this, "External download folder linked!", Toast.LENGTH_SHORT).show()
                }
            }

            // Backup Folder picker launcher for pre-uninstall
            val backupFolderLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri: Uri? ->
                if (uri != null) {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(uri, flags)
                    vm.copyWallpapersToFolder(uri)
                }
            }

            fun launchUninstall() {
                val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(uninstallIntent)
            }

            // Toast / status notifications
            LaunchedEffect(uiState.statusMessage) {
                uiState.statusMessage?.let { msg ->
                    snackbarHostState.showSnackbar(msg)
                    vm.clearStatus()
                }
            }

            AxWhavenDynamicTheme(themeMode = uiState.currentTheme) { colors ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (colors.isGlassmorphism && uiState.appBackgroundItem == null) {
                                Modifier.background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF0F111E),
                                            Color(0xFF0B0D17),
                                            Color(0xFF06070B)
                                        )
                                    )
                                )
                            } else {
                                Modifier.background(colors.background)
                            }
                        )
                ) {
                    // Glassmorphism ambient luminous backdrops
                    if (colors.isGlassmorphism) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (uiState.backgroundBlurRadius > 0f) {
                                        Modifier.blur(
                                            radius = (uiState.backgroundBlurRadius * 1.5f).dp,
                                            edgeTreatment = BlurredEdgeTreatment.Unbounded
                                        )
                                    } else Modifier
                                )
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0x408B5CF6), Color.Transparent),
                                        center = androidx.compose.ui.geometry.Offset(250f, 350f),
                                        radius = 850f
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (uiState.backgroundBlurRadius > 0f) {
                                        Modifier.blur(
                                            radius = (uiState.backgroundBlurRadius * 1.5f).dp,
                                            edgeTreatment = BlurredEdgeTreatment.Unbounded
                                        )
                                    } else Modifier
                                )
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0x3506B6D4), Color.Transparent),
                                        center = androidx.compose.ui.geometry.Offset(900f, 1400f),
                                        radius = 950f
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (uiState.backgroundBlurRadius > 0f) {
                                        Modifier.blur(
                                            radius = (uiState.backgroundBlurRadius * 1.5f).dp,
                                            edgeTreatment = BlurredEdgeTreatment.Unbounded
                                        )
                                    } else Modifier
                                )
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0x28EC4899), Color.Transparent),
                                        center = androidx.compose.ui.geometry.Offset(500f, 900f),
                                        radius = 650f
                                    )
                                )
                        )
                    }

                    // Full-screen App Background Wallpaper if configured
                    if (uiState.appBackgroundItem != null) {
                        WallpaperImage(
                            source = uiState.appBackgroundItem?.source,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            useFullResolution = true,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (uiState.backgroundBlurRadius > 0f) {
                                        Modifier
                                            .scale(1f + (uiState.backgroundBlurRadius / 250f).coerceAtMost(0.15f))
                                            .blur(
                                                radius = uiState.backgroundBlurRadius.dp,
                                                edgeTreatment = BlurredEdgeTreatment.Unbounded
                                            )
                                    } else Modifier
                                )
                        )
                        // Ambient Scrim Overlay: balances wallpaper visibility with frosted glass contrast
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (colors.isGlassmorphism) {
                                        val scrimAlpha = (0.35f + (uiState.backgroundBlurRadius / 60f) * 0.25f).coerceIn(0.35f, 0.70f)
                                        Color(0xFF080A16).copy(alpha = scrimAlpha)
                                    } else {
                                        Color(0xC4090A0F)
                                    }
                                )
                        )
                    }

                    Scaffold(
                        containerColor = if (uiState.appBackgroundItem != null || colors.isGlassmorphism) Color.Transparent else colors.background,
                        snackbarHost = {
                            SnackbarHost(snackbarHostState) { data ->
                                Snackbar(
                                    snackbarData = data,
                                    shape = RoundedCornerShape(16.dp),
                                    containerColor = colors.surfaceElevated,
                                    contentColor = colors.textPrimary,
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                                )
                            }
                        },
                        bottomBar = {
                            // Persistent 3-tab Bottom Navigation Bar (Hidden while previewing/setting wallpaper)
                            if (activePreviewItem == null) {
                                AxWhavenBottomNavBar(
                                    currentTab = uiState.currentTab,
                                    colors = colors,
                                    onTabSelect = { tab ->
                                        if (tab != uiState.currentTab) {
                                            if (tab == PrimaryAppTab.HOME) {
                                                tabNavigationHistory.clear()
                                                tabNavigationHistory.add(PrimaryAppTab.HOME)
                                            } else {
                                                tabNavigationHistory.remove(tab)
                                                tabNavigationHistory.add(tab)
                                            }
                                            vm.selectPrimaryTab(tab)
                                        } else if (tab == PrimaryAppTab.HOME && uiState.selectedDirectory != null) {
                                            vm.selectDirectory(null)
                                        }
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val TabTransitionDuration = 200
                            val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

                            AnimatedContent(
                                targetState = uiState.currentTab,
                                transitionSpec = {
                                    if (targetState.ordinal > initialState.ordinal) {
                                        // Forward (Home -> AxW Surf -> Settings): fast initial burst, decelerating smoothly
                                        (slideInHorizontally(
                                            animationSpec = tween(TabTransitionDuration, easing = EmphasizedDecelerateEasing),
                                            initialOffsetX = { fullWidth -> fullWidth }
                                        ) + fadeIn(animationSpec = tween(TabTransitionDuration - 40, easing = LinearEasing))).togetherWith(
                                            slideOutHorizontally(
                                                animationSpec = tween(TabTransitionDuration, easing = EmphasizedDecelerateEasing),
                                                targetOffsetX = { fullWidth -> -fullWidth }
                                            ) + fadeOut(animationSpec = tween(TabTransitionDuration - 60, easing = LinearEasing))
                                        )
                                    } else if (targetState.ordinal < initialState.ordinal) {
                                        // Inverse / Backward (Settings -> AxW Surf -> Home): fast initial burst, decelerating smoothly
                                        (slideInHorizontally(
                                            animationSpec = tween(TabTransitionDuration, easing = EmphasizedDecelerateEasing),
                                            initialOffsetX = { fullWidth -> -fullWidth }
                                        ) + fadeIn(animationSpec = tween(TabTransitionDuration - 40, easing = LinearEasing))).togetherWith(
                                            slideOutHorizontally(
                                                animationSpec = tween(TabTransitionDuration, easing = EmphasizedDecelerateEasing),
                                                targetOffsetX = { fullWidth -> fullWidth }
                                            ) + fadeOut(animationSpec = tween(TabTransitionDuration - 60, easing = LinearEasing))
                                        )
                                    } else {
                                        fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(120))
                                    }
                                },
                                label = "PrimaryTabTransition"
                            ) { tab ->
                                when (tab) {
                                    PrimaryAppTab.HOME -> {
                                        HomeScreen(
                                            uiState = uiState,
                                            colors = colors,
                                            onTabSelect = vm::selectHomeTab,
                                            onDirectorySelect = vm::selectDirectory,
                                            onWallpaperPreview = { item -> activePreviewItem = item },
                                            onRollDie = vm::rollRandomDie
                                        )
                                    }
                                    PrimaryAppTab.HAVEN_SURF -> {
                                        HavenSurfScreen(
                                            uiState = uiState,
                                            colors = colors,
                                            onProviderSelect = vm::selectProvider,
                                            onQueryChange = vm::onSearchQueryChanged,
                                            onApplySuggestion = vm::applySuggestion,
                                            onClearQuery = vm::clearSearchInput,
                                            onSubmitSearch = { vm.executeHavenSurfSearch(isNewSearch = true) },
                                            onOpenFilterSheet = { vm.setFilterSheetVisible(true) },
                                            onApplyFilters = vm::applyFilters,
                                            onResetFilters = { vm.setFilterSheetVisible(false) },
                                            onLoadNextPage = vm::loadNextPage,
                                            onWallpaperPreview = { item -> activePreviewItem = item },
                                            onDownloadItem = vm::downloadItem,
                                            onSaveKonachanCookie = vm::saveKonachanCookie,
                                            onDismissKonachanCookieDialog = vm::dismissKonachanCookieDialog,
                                            onOpenKonachanCookieDialog = vm::openKonachanCookieDialog,
                                            onSaveDanbooruCookie = vm::saveDanbooruCookie,
                                            onDismissDanbooruCookieDialog = vm::dismissDanbooruCookieDialog,
                                            onOpenDanbooruCookieDialog = vm::openDanbooruCookieDialog
                                        )
                                    }
                                    PrimaryAppTab.SETTINGS -> {
                                        SettingsScreen(
                                            uiState = uiState,
                                            colors = colors,
                                            onThemeSelect = vm::selectTheme,
                                            onBlurRadiusChange = vm::setBackgroundBlurRadius,
                                            onSelectDownloadFolder = { folderLauncher.launch(null) },
                                            onSetCustomPath = vm::setCustomStoragePath,
                                            onResetDownloadDirectory = vm::resetStorageToDefault,
                                            onClearAppBackground = vm::clearAppBackgroundWallpaper,
                                            onCopyWallpapersToSafFolder = { backupFolderLauncher.launch(null) },
                                            onCopyWallpapersToPictures = vm::copyWallpapersToPictures,
                                            onRequestUninstallApp = { launchUninstall() }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Full Screen Interactive Wallpaper Preview Modal (Placed on top of Scaffold so bottom nav bar never obscures it)
                    activePreviewItem?.let { item ->
                        WallpaperPreviewScreen(
                            item = item,
                            colors = colors,
                            onClose = { activePreviewItem = null },
                            onApply = { mode, target ->
                                vm.applyWallpaper(item, mode, target)
                                activePreviewItem = null
                            },
                            onLaunchSystemPicker = { activity, targetItem ->
                                vm.launchSystemPicker(activity, targetItem)
                                activePreviewItem = null
                            },
                            onSetAppBackground = { targetItem ->
                                vm.setAppBackgroundWallpaper(targetItem)
                                activePreviewItem = null
                            }
                        )
                    }
                }
            }
        }
    }
}
