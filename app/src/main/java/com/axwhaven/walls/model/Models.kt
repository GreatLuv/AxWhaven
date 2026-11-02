package com.axwhaven.walls.model

import android.app.WallpaperManager
import android.net.Uri

enum class ProviderSite(val alias: String, val displayName: String, val baseUrl: String) {
    WALLHAVEN("wallhaven", "Wallhaven", "https://wallhaven.cc"),
    YANDERE("yandere", "Yande.re", "https://yande.re"),
    KONACHAN("konachan", "Konachan", "https://konachan.com"),
    SAFEBOORU("safebooru", "Safebooru", "https://safebooru.org"),
    PIXIV("pixiv", "Pixiv", "https://www.pixiv.net"),
    ANIME_PICTURES("anime_pictures", "Anime-Pictures", "https://api.anime-pictures.net"),
    GELBOORU("gelbooru", "Gelbooru", "https://gelbooru.com"),
    DANBOORU("danbooru", "Danbooru", "https://danbooru.donmai.us")
}

enum class ContentRating(val token: String, val displayName: String) {
    SFW("sfw", "SFW Only"),
    QUESTIONABLE("sketchy", "Questionable"),
    NSFW("nsfw", "Explicit / NSFW"),
    ALL("all", "All Ratings")
}

enum class ResolutionFilter(val displayName: String, val minWidth: Int, val minHeight: Int) {
    ALL("All Resolutions", 0, 0),
    HD_720P("Min 720p (1280x720)", 1280, 720),
    FHD_1080P("Min 1080p (1920x1080)", 1920, 1080),
    QHD_1440P("Min 1440p (2560x1440)", 2560, 1440),
    UHD_4K("Min 4K (3840x2160)", 3840, 2160)
}

enum class AspectRatioFilter(val displayName: String, val ratioToken: String?) {
    UNRESTRICTED("Any Aspect Ratio", null),
    PORTRAIT("Portrait (9:16)", "9x16"),
    LANDSCAPE("Landscape (16:9)", "16x9"),
    SQUARE("Square (1:1)", "1x1")
}

data class HavenSurfFilterState(
    val resolution: ResolutionFilter = ResolutionFilter.ALL,
    val aspectRatio: AspectRatioFilter = AspectRatioFilter.UNRESTRICTED,
    val rating: ContentRating = ContentRating.SFW,
    val preferOriginal: Boolean = true
) {
    val isDefault: Boolean
        get() = resolution == ResolutionFilter.ALL &&
                aspectRatio == AspectRatioFilter.UNRESTRICTED &&
                rating == ContentRating.SFW
}

enum class ScaleMode {
    FIT,
    FILL
}

enum class WallpaperTarget(val flag: Int, val label: String) {
    HOME(WallpaperManager.FLAG_SYSTEM, "Home screen"),
    LOCK(WallpaperManager.FLAG_LOCK, "Lock screen"),
    BOTH(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK, "Both Home & Lock screens")
}

sealed class WallpaperSource {
    data class AssetPath(val assetRelativePath: String) : WallpaperSource()
    data class SafUri(val uri: Uri) : WallpaperSource()
    data class FilePath(val absolutePath: String) : WallpaperSource()
    data class RemoteUrl(
        val id: String,
        val previewUrl: String,
        val fullImageUrl: String,
        val width: Int,
        val height: Int,
        val site: ProviderSite,
        val rating: ContentRating,
        val referer: String? = null,
        val originalImageUrl: String? = null
    ) : WallpaperSource()
}

data class WallpaperItem(
    val id: String,
    val title: String,
    val directoryName: String,
    val source: WallpaperSource,
    val width: Int = 0,
    val height: Int = 0,
    val site: ProviderSite? = null
)

data class ArtDirectory(
    val id: String,
    val artName: String,
    val items: List<WallpaperItem>,
    val isDownloadedFolder: Boolean = false
) {
    val totalArtsText: String get() = if (items.size == 1) "1 art" else "${items.size} arts"
    val coverItem: WallpaperItem? get() = items.firstOrNull()
}

enum class PrimaryAppTab(val title: String) {
    HOME("Home"),
    COLLECTIONS("Collections"),
    HAVEN_SURF("AxW Surf"),
    SETTINGS("Settings")
}
