package com.axwhaven.walls

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.axwhaven.walls.data.WallpaperRemoteClient
import com.axwhaven.walls.data.WallpaperStorageRepository
import com.axwhaven.walls.model.*
import com.axwhaven.walls.ui.theme.AppThemeMode
import com.axwhaven.walls.wallpaper.WallpaperApplier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class HomeNavigationTab {
    COLLECTIONS,
    ALL_WALLPAPERS
}

data class WallpaperUiState(
    // Global Navigation & Theme
    val currentTab: PrimaryAppTab = PrimaryAppTab.HOME,
    val currentTheme: AppThemeMode = AppThemeMode.OBSIDIAN_VIOLET,

    // Home Screen State
    val homeTab: HomeNavigationTab = HomeNavigationTab.COLLECTIONS,
    val directories: List<ArtDirectory> = emptyList(),
    val selectedDirectory: ArtDirectory? = null,

    // Haven Surf (Online Discovery) State
    val activeProvider: ProviderSite = ProviderSite.WALLHAVEN,
    val searchQueryText: String = "",
    val tagSuggestions: List<String> = emptyList(),
    val filterState: HavenSurfFilterState = HavenSurfFilterState(),
    val isFilterSheetVisible: Boolean = false,
    val remoteResults: List<WallpaperItem> = emptyList(),
    val currentPage: Int = 1,
    val canLoadMore: Boolean = true,
    val isSearching: Boolean = false,
    val isLoadingMore: Boolean = false,

    // Global Feedback
    val statusMessage: String? = null,

    // App Custom Background Wallpaper & Glassmorphism Blur
    val appBackgroundItem: WallpaperItem? = null,
    val backgroundBlurRadius: Float = 25f,

    // Konachan Cookie Bypass Dialog
    val showKonachanCookieDialog: Boolean = false,
    val konachanCookieText: String = "",

    // Danbooru Cookie Bypass Dialog
    val showDanbooruCookieDialog: Boolean = false,
    val danbooruCookieText: String = "",

    // Storage & Download Directory
    val activeStorageDesc: String = "",
    val isUsingDefaultStorage: Boolean = true,
    val defaultStorageWallpapersCount: Int = 0
) {
    val allLocalWallpapers: List<WallpaperItem>
        get() = directories.flatMap { it.items }

    val totalArtsCount: Int
        get() = directories.sumOf { it.items.size }
}

class WallpaperViewModel(
    val storageRepo: WallpaperStorageRepository,
    val remoteClient: WallpaperRemoteClient,
    val wallpaperApplier: WallpaperApplier,
    private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("axwhaven_settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        WallpaperUiState(
            directories = storageRepo.getInitialDirectories(),
            activeStorageDesc = storageRepo.getActiveLocationDescription(),
            isUsingDefaultStorage = storageRepo.isUsingDefaultLocation(),
            defaultStorageWallpapersCount = storageRepo.getDefaultWallpapersCount()
        )
    )
    val uiState: StateFlow<WallpaperUiState> = _uiState.asStateFlow()

    private var searchDebounceJob: Job? = null
    private var autocompleteJob: Job? = null

    init {
        val savedThemeId = prefs.getString("theme_mode", AppThemeMode.OBSIDIAN_VIOLET.id)
        val loadedTheme = AppThemeMode.values().find { it.id == savedThemeId } ?: AppThemeMode.OBSIDIAN_VIOLET

        val savedRatingToken = prefs.getString("content_rating", ContentRating.SFW.token)
        val loadedRating = ContentRating.values().find { it.token == savedRatingToken } ?: ContentRating.SFW

        val savedHomeTabName = prefs.getString("home_tab", HomeNavigationTab.COLLECTIONS.name)
        val loadedHomeTab = runCatching { HomeNavigationTab.valueOf(savedHomeTabName ?: "") }
            .getOrDefault(HomeNavigationTab.COLLECTIONS)

        val savedAppBg = loadSavedAppBackground()
        val savedBlurRadius = prefs.getFloat("key_bg_blur_radius", 25f)
        val savedKonachanCookie = prefs.getString("konachan_cookie", "") ?: ""
        val savedDanbooruCookie = prefs.getString("danbooru_cookie", "") ?: ""

        _uiState.value = _uiState.value.copy(
            currentTheme = loadedTheme,
            homeTab = loadedHomeTab,
            filterState = _uiState.value.filterState.copy(rating = loadedRating),
            appBackgroundItem = savedAppBg,
            backgroundBlurRadius = savedBlurRadius,
            konachanCookieText = savedKonachanCookie,
            danbooruCookieText = savedDanbooruCookie
        )

        refreshLocalDirectories()
    }

    fun selectTheme(theme: AppThemeMode) {
        prefs.edit().putString("theme_mode", theme.id).apply()
        _uiState.value = _uiState.value.copy(currentTheme = theme)
    }

    fun setBackgroundBlurRadius(radius: Float) {
        val clamped = radius.coerceIn(0f, 60f)
        prefs.edit().putFloat("key_bg_blur_radius", clamped).apply()
        _uiState.value = _uiState.value.copy(backgroundBlurRadius = clamped)
    }

    fun selectPrimaryTab(tab: PrimaryAppTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
        if (tab == PrimaryAppTab.HOME) {
            refreshLocalDirectories()
        } else if (tab == PrimaryAppTab.HAVEN_SURF) {
            if (_uiState.value.remoteResults.isEmpty() && !_uiState.value.isSearching) {
                executeHavenSurfSearch(isNewSearch = true)
            }
        }
    }

    fun selectHomeTab(tab: HomeNavigationTab) {
        prefs.edit().putString("home_tab", tab.name).apply()
        _uiState.value = _uiState.value.copy(homeTab = tab)
        refreshLocalDirectories()
    }

    fun refreshLocalDirectories() {
        viewModelScope.launch {
            val dirs = storageRepo.loadAllDirectories()
            val currentSelectedId = _uiState.value.selectedDirectory?.id
            val updatedSelected = dirs.find { it.id == currentSelectedId }
            _uiState.value = _uiState.value.copy(
                directories = dirs,
                selectedDirectory = updatedSelected,
                activeStorageDesc = storageRepo.getActiveLocationDescription(),
                isUsingDefaultStorage = storageRepo.isUsingDefaultLocation(),
                defaultStorageWallpapersCount = storageRepo.getDefaultWallpapersCount()
            )
        }
    }

    fun setCustomStoragePath(pathStr: String) {
        val result = storageRepo.setCustomPath(pathStr)
        result.onSuccess {
            _uiState.value = _uiState.value.copy(
                statusMessage = "Download folder updated to custom path!"
            )
            refreshLocalDirectories()
        }.onFailure { err ->
            _uiState.value = _uiState.value.copy(
                statusMessage = "Path error: ${err.localizedMessage}"
            )
        }
    }

    fun setCustomStorageUri(uri: Uri) {
        storageRepo.setCustomTreeUri(uri)
        _uiState.value = _uiState.value.copy(
            statusMessage = "External download folder linked!"
        )
        refreshLocalDirectories()
    }

    fun resetStorageToDefault() {
        storageRepo.resetToDefaultLocation()
        _uiState.value = _uiState.value.copy(
            statusMessage = "Download folder reset to default (Android/data)."
        )
        refreshLocalDirectories()
    }

    fun copyWallpapersToFolder(uri: Uri, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(statusMessage = "Copying wallpapers...")
            val result = storageRepo.copyDefaultWallpapersTo(uri)
            result.onSuccess { count ->
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Copied $count wallpapers to selected folder!"
                )
                refreshLocalDirectories()
                onComplete?.invoke()
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Copy failed: ${err.localizedMessage}"
                )
            }
        }
    }

    fun copyWallpapersToPictures(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(statusMessage = "Copying wallpapers to Pictures...")
            val result = storageRepo.copyDefaultWallpapersToPictures()
            result.onSuccess { count ->
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Copied $count wallpapers to Pictures/AxWhaven_Wallpapers!"
                )
                refreshLocalDirectories()
                onComplete?.invoke()
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Copy failed: ${err.localizedMessage}"
                )
            }
        }
    }

    fun selectDirectory(directory: ArtDirectory?) {
        _uiState.value = _uiState.value.copy(selectedDirectory = directory)
    }

    fun selectProvider(provider: ProviderSite) {
        if (_uiState.value.activeProvider == provider) return
        _uiState.value = _uiState.value.copy(
            activeProvider = provider,
            searchQueryText = "",
            tagSuggestions = emptyList(),
            remoteResults = emptyList(),
            currentPage = 1,
            canLoadMore = true,
            statusMessage = "Switched to ${provider.displayName}"
        )
        executeHavenSurfSearch(isNewSearch = true)
    }

    /**
     * Clean standard search query change without "/" parsing.
     */
    fun onSearchQueryChanged(text: String) {
        _uiState.value = _uiState.value.copy(searchQueryText = text)

        autocompleteJob?.cancel()
        if (text.isNotBlank()) {
            autocompleteJob = viewModelScope.launch {
                delay(300)
                val suggestions = remoteClient.fetchTagSuggestions(
                    _uiState.value.activeProvider,
                    text
                )
                _uiState.value = _uiState.value.copy(tagSuggestions = suggestions)
            }
        } else {
            _uiState.value = _uiState.value.copy(tagSuggestions = emptyList())
        }
    }

    fun applySuggestion(suggestion: String) {
        val current = _uiState.value.searchQueryText
        val cleanSug = suggestion.trim()
        val words = current.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val newText = when {
            words.isEmpty() -> cleanSug
            cleanSug.startsWith(words.last(), ignoreCase = true) -> {
                (words.dropLast(1) + cleanSug).joinToString(" ")
            }
            current.endsWith(" ") -> {
                (words + cleanSug).joinToString(" ")
            }
            else -> {
                (words.dropLast(1) + cleanSug).joinToString(" ")
            }
        }
        _uiState.value = _uiState.value.copy(
            searchQueryText = newText,
            tagSuggestions = emptyList()
        )
        executeHavenSurfSearch(isNewSearch = true)
    }

    fun clearSearchInput() {
        _uiState.value = _uiState.value.copy(
            searchQueryText = "",
            tagSuggestions = emptyList()
        )
        executeHavenSurfSearch(isNewSearch = true)
    }

    fun executeHavenSurfSearch(isNewSearch: Boolean = true) {
        searchDebounceJob?.cancel()
        val targetPage = if (isNewSearch) 1 else _uiState.value.currentPage + 1
        val provider = _uiState.value.activeProvider
        val query = _uiState.value.searchQueryText.trim()
        val filters = _uiState.value.filterState

        if (isNewSearch) {
            _uiState.value = _uiState.value.copy(
                isSearching = true,
                tagSuggestions = emptyList(),
                currentPage = 1
            )
        } else {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
        }

        searchDebounceJob = viewModelScope.launch {
            val result = remoteClient.queryWallpapers(provider, query, targetPage, filters)
            result.onSuccess { newItems ->
                val updatedList = if (isNewSearch) newItems else _uiState.value.remoteResults + newItems
                _uiState.value = _uiState.value.copy(
                    remoteResults = updatedList,
                    currentPage = targetPage,
                    canLoadMore = newItems.isNotEmpty(),
                    isSearching = false,
                    isLoadingMore = false,
                    statusMessage = if (isNewSearch && newItems.isEmpty()) "No artworks found on ${provider.displayName}." else null
                )
            }.onFailure { err ->
                val isKonachanError = provider == ProviderSite.KONACHAN &&
                    (err.message?.contains("Cloudflare", ignoreCase = true) == true ||
                     err.message?.contains("cookie", ignoreCase = true) == true ||
                     err.message?.contains("403", ignoreCase = true) == true)

                val isDanbooruError = provider == ProviderSite.DANBOORU &&
                    (err.message?.contains("Cloudflare", ignoreCase = true) == true ||
                     err.message?.contains("cookie", ignoreCase = true) == true ||
                     err.message?.contains("403", ignoreCase = true) == true)

                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    isLoadingMore = false,
                    statusMessage = when {
                        isKonachanError -> "Konachan NSFW requires verification. Tap to pass cookie."
                        isDanbooruError -> "Danbooru requires verification. Tap to pass cookie."
                        else -> "Search error: ${err.localizedMessage}"
                    },
                    showKonachanCookieDialog = isKonachanError,
                    showDanbooruCookieDialog = isDanbooruError
                )
            }
        }
    }

    fun openDanbooruCookieDialog() {
        _uiState.value = _uiState.value.copy(
            showDanbooruCookieDialog = true,
            danbooruCookieText = prefs.getString("danbooru_cookie", "") ?: ""
        )
    }

    fun dismissDanbooruCookieDialog() {
        _uiState.value = _uiState.value.copy(showDanbooruCookieDialog = false)
    }

    fun saveDanbooruCookie(cookie: String) {
        runCatching {
            android.webkit.CookieManager.getInstance().flush()
        }
        remoteClient.danbooruCookie = cookie
        prefs.edit().putString("danbooru_cookie", cookie).apply()
        _uiState.value = _uiState.value.copy(
            danbooruCookieText = cookie,
            showDanbooruCookieDialog = false,
            statusMessage = "Danbooru cookie saved!"
        )
        executeHavenSurfSearch(isNewSearch = true)
    }

    fun openKonachanCookieDialog() {
        _uiState.value = _uiState.value.copy(
            showKonachanCookieDialog = true,
            konachanCookieText = prefs.getString("konachan_cookie", "") ?: ""
        )
    }

    fun dismissKonachanCookieDialog() {
        _uiState.value = _uiState.value.copy(showKonachanCookieDialog = false)
    }

    fun saveKonachanCookie(cookie: String) {
        runCatching {
            android.webkit.CookieManager.getInstance().flush()
        }
        remoteClient.konachanCookie = cookie
        prefs.edit().putString("konachan_cookie", cookie).apply()
        _uiState.value = _uiState.value.copy(
            konachanCookieText = cookie,
            showKonachanCookieDialog = false,
            statusMessage = "Konachan cookie saved!"
        )
        executeHavenSurfSearch(isNewSearch = true)
    }

    fun loadNextPage() {
        if (!_uiState.value.isSearching && !_uiState.value.isLoadingMore && _uiState.value.canLoadMore) {
            executeHavenSurfSearch(isNewSearch = false)
        }
    }

    fun setFilterSheetVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(isFilterSheetVisible = visible)
    }

    fun applyFilters(newFilters: HavenSurfFilterState) {
        prefs.edit().putString("content_rating", newFilters.rating.token).apply()
        _uiState.value = _uiState.value.copy(
            filterState = newFilters,
            isFilterSheetVisible = false,
            currentPage = 1
        )
        executeHavenSurfSearch(isNewSearch = true)
    }

    fun resetFilters() {
        applyFilters(HavenSurfFilterState())
    }

    fun downloadItem(item: WallpaperItem, useOriginal: Boolean = false) {
        val label = if (useOriginal) "original quality" else "high quality"
        _uiState.value = _uiState.value.copy(statusMessage = "Downloading ${item.title} ($label)...")
        viewModelScope.launch {
            storageRepo.downloadRemoteWallpaper(item, useOriginal)
                .onSuccess { msg ->
                    _uiState.value = _uiState.value.copy(
                        statusMessage = msg
                    )
                    refreshLocalDirectories()
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        statusMessage = "Download failed: ${err.localizedMessage}"
                    )
                }
        }
    }

    fun applyWallpaper(item: WallpaperItem, scaleMode: ScaleMode, target: WallpaperTarget) {
        _uiState.value = _uiState.value.copy(statusMessage = "Applying wallpaper...")
        viewModelScope.launch {
            wallpaperApplier.applyWallpaper(item, scaleMode, target)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(statusMessage = "Wallpaper set successfully!")
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(statusMessage = "Failed to apply: ${err.localizedMessage}")
                }
        }
    }

    fun launchSystemPicker(activity: Activity, item: WallpaperItem) {
        viewModelScope.launch {
            wallpaperApplier.launchSystemWallpaperPicker(activity, item)
        }
    }

    fun rollRandomDie(target: WallpaperTarget) {
        viewModelScope.launch {
            val allLocal = _uiState.value.allLocalWallpapers
            wallpaperApplier.applyRandomLocalWallpaper(allLocal, target)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(statusMessage = "Wallpaper set successfully!")
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(statusMessage = "Die roll failed: ${err.localizedMessage}")
                }
        }
    }

    fun clearStatus() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    fun setAppBackgroundWallpaper(item: WallpaperItem) {
        val editor = prefs.edit()
            .putString("app_bg_id", item.id)
            .putString("app_bg_title", item.title)
            .putString("app_bg_dir", item.directoryName)
            .putInt("app_bg_width", item.width)
            .putInt("app_bg_height", item.height)

        when (val src = item.source) {
            is WallpaperSource.AssetPath -> {
                editor.putString("app_bg_type", "asset")
                editor.putString("app_bg_val", src.assetRelativePath)
            }
            is WallpaperSource.FilePath -> {
                editor.putString("app_bg_type", "file")
                editor.putString("app_bg_val", src.absolutePath)
            }
            is WallpaperSource.SafUri -> {
                editor.putString("app_bg_type", "saf")
                editor.putString("app_bg_val", src.uri.toString())
            }
            is WallpaperSource.RemoteUrl -> {
                editor.putString("app_bg_type", "remote")
                editor.putString("app_bg_val", src.fullImageUrl)
                editor.putString("app_bg_preview", src.previewUrl)
                editor.putString("app_bg_site", src.site.name)
            }
        }
        editor.apply()

        _uiState.value = _uiState.value.copy(
            appBackgroundItem = item,
            statusMessage = "Set as app background!"
        )
    }

    fun clearAppBackgroundWallpaper() {
        prefs.edit()
            .remove("app_bg_id")
            .remove("app_bg_title")
            .remove("app_bg_dir")
            .remove("app_bg_type")
            .remove("app_bg_val")
            .remove("app_bg_preview")
            .remove("app_bg_site")
            .apply()

        _uiState.value = _uiState.value.copy(
            appBackgroundItem = null,
            statusMessage = "App background reset"
        )
    }

    private fun loadSavedAppBackground(): WallpaperItem? {
        val type = prefs.getString("app_bg_type", null) ?: return null
        val value = prefs.getString("app_bg_val", null) ?: return null
        val id = prefs.getString("app_bg_id", "app_bg") ?: "app_bg"
        val title = prefs.getString("app_bg_title", "App Background") ?: "App Background"
        val dir = prefs.getString("app_bg_dir", "Custom") ?: "Custom"
        val width = prefs.getInt("app_bg_width", 0)
        val height = prefs.getInt("app_bg_height", 0)

        val source = when (type) {
            "asset" -> WallpaperSource.AssetPath(value)
            "file" -> WallpaperSource.FilePath(value)
            "saf" -> WallpaperSource.SafUri(Uri.parse(value))
            "remote" -> {
                val preview = prefs.getString("app_bg_preview", value) ?: value
                val siteName = prefs.getString("app_bg_site", ProviderSite.WALLHAVEN.name)
                val site = runCatching { ProviderSite.valueOf(siteName ?: "") }.getOrDefault(ProviderSite.WALLHAVEN)
                WallpaperSource.RemoteUrl(
                    id = id,
                    previewUrl = preview,
                    fullImageUrl = value,
                    width = width,
                    height = height,
                    site = site,
                    rating = ContentRating.SFW
                )
            }
            else -> null
        } ?: return null

        return WallpaperItem(
            id = id,
            title = title,
            directoryName = dir,
            source = source,
            width = width,
            height = height
        )
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = context.applicationContext
                    return WallpaperViewModel(
                        WallpaperStorageRepository(app),
                        WallpaperRemoteClient(app),
                        WallpaperApplier(app),
                        app
                    ) as T
                }
            }
    }
}
