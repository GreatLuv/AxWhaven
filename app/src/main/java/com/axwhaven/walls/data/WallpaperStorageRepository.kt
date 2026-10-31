package com.axwhaven.walls.data

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.axwhaven.walls.R
import com.axwhaven.walls.model.ArtDirectory
import com.axwhaven.walls.model.WallpaperItem
import com.axwhaven.walls.model.WallpaperSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class WallpaperStorageRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("axwhaven_storage_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOM_PATH = "custom_download_path"
        private const val KEY_SAF_URI = "saf_tree_uri"

        @Volatile
        private var memoryCachedAssetDirectories: List<ArtDirectory>? = null

        // Precompiled static manifest of bundled assets for instant 0ms Frame-0 rendering
        private val BUNDLED_STATIC_CATALOG: Map<String, List<String>> = mapOf(
            "Alya" to listOf(
                "alya_00.jpg"
            ),
            "Asuka" to listOf(
                "asuka_00.jpg",
                "asuka_01.png",
                "asuka_02.png",
                "asuka_03.jpg",
                "asuka_04.png",
                "3a63de13d45c75adf47ad1e821c225fc.jpg",
                "9287623723dfcae3ec1df2be201f7b1c.jpg",
                "bd78228b0db7c601ab5664254161d4eb.jpg"
            ),
            "Kurumi" to listOf(
                "kurumi_00.png",
                "kurumi_01.png",
                "kurumi_02.png",
                "kurumi_03.png",
                "kurumi_04.png",
                "kurumi_05.png"
            ),
            "Lumine" to listOf(
                "lumine_00.jpg",
                "lumine_01.png"
            ),
            "Miku" to listOf(
                "IMG_20261014_000422_615.jpg",
                "IMG_20261014_000429_700.jpg",
                "IMG_20261014_000442_798.jpg",
                "IMG_20261014_000455_982.jpg",
                "IMG_20261014_000501_756.jpg",
                "IMG_20261014_000507_624.jpg",
                "IMG_20261014_000517_385.jpg",
                "IMG_20261014_000522_406.jpg"
            ),
            "Random" to listOf(
                "752c7e8bca460b8965cf28f9d123625c.jpg",
                "a6281ef13ca1992f94e8d6d5dc8dcf2b.jpg",
                "baf8bef2258d5f1ed0cb60e4e07f6e8d.jpg",
                "random_00.png",
                "random_01.jpg"
            ),
            "Saber" to listOf(
                "24faaf1223b95c74bb8c7bc0a09919c6.jpg",
                "saber_00.jpg",
                "saber_01.png",
                "saber_02.jpg"
            )
        )

        private fun buildBundledDirectories(): List<ArtDirectory> {
            return BUNDLED_STATIC_CATALOG.map { (folderKey, files) ->
                val displayName = folderKey.replace('_', ' ').replaceFirstChar { it.uppercase() }
                val items = files.mapIndexed { idx, fileName ->
                    WallpaperItem(
                        id = "asset_${folderKey}_$idx",
                        title = fileName.substringBeforeLast('.'),
                        directoryName = displayName,
                        source = WallpaperSource.AssetPath("wallpapers/$folderKey/$fileName")
                    )
                }
                ArtDirectory(
                    id = folderKey,
                    artName = displayName,
                    items = items,
                    isDownloadedFolder = false
                )
            }
        }
    }

    /**
     * Default app-specific storage directory:
     * /storage/emulated/0/Android/data/com.axwhaven.walls/files/wallpapers/
     */
    val appDataDownloadDir: File
        get() = File(context.getExternalFilesDir(null), "wallpapers").apply { mkdirs() }

    /**
     * Optional custom folder path typed by user (e.g. /storage/emulated/0/Pictures/Wallpapers)
     */
    val customPath: String?
        get() = prefs.getString(KEY_CUSTOM_PATH, null)?.takeIf { it.isNotBlank() }

    /**
     * Optional custom SAF directory URI picked by user via system folder picker
     */
    var downloadTreeUri: Uri?
        get() = prefs.getString(KEY_SAF_URI, null)?.let(Uri::parse)
        set(value) {
            if (value != null) {
                setCustomTreeUri(value)
            } else {
                prefs.edit().remove(KEY_SAF_URI).apply()
            }
        }

    fun isUsingDefaultLocation(): Boolean {
        return customPath.isNullOrBlank() && downloadTreeUri == null
    }

    fun getActiveLocationDescription(): String {
        val path = customPath
        if (!path.isNullOrBlank()) {
            return path
        }
        val uri = downloadTreeUri
        if (uri != null) {
            val doc = runCatching { DocumentFile.fromTreeUri(context, uri) }.getOrNull()
            return doc?.name ?: uri.lastPathSegment ?: "Custom SAF Folder"
        }
        return "Default: Android/data/com.axwhaven.walls/files/wallpapers/"
    }

    fun setCustomPath(pathStr: String): Result<Unit> {
        return runCatching {
            val trimmed = pathStr.trim()
            if (trimmed.isBlank()) throw IllegalArgumentException("Path cannot be empty.")
            val dir = File(trimmed)
            if (!dir.exists()) {
                val created = dir.mkdirs()
                if (!created && !dir.exists()) {
                    throw IllegalStateException("Failed to create folder at: $trimmed. Check permissions.")
                }
            }
            if (!dir.isDirectory) {
                throw IllegalArgumentException("The specified path is not a folder: $trimmed")
            }
            // Test folder writability
            val testFile = File(dir, ".axwhaven_write_test_${System.currentTimeMillis()}")
            try {
                testFile.writeText("ok")
                testFile.delete()
            } catch (e: Exception) {
                throw IllegalStateException("The folder is not writable. Please grant storage access or choose another path: ${e.message}")
            }

            prefs.edit()
                .putString(KEY_CUSTOM_PATH, trimmed)
                .remove(KEY_SAF_URI)
                .apply()
        }
    }

    fun setCustomTreeUri(uri: Uri) {
        prefs.edit()
            .putString(KEY_SAF_URI, uri.toString())
            .remove(KEY_CUSTOM_PATH)
            .apply()
    }

    fun resetToDefaultLocation() {
        prefs.edit()
            .remove(KEY_CUSTOM_PATH)
            .remove(KEY_SAF_URI)
            .apply()
    }

    fun getDefaultWallpapersCount(): Int {
        return runCatching {
            appDataDownloadDir.listFiles { f ->
                f.isFile && (f.name.endsWith(".jpg", true) ||
                        f.name.endsWith(".png", true) ||
                        f.name.endsWith(".webp", true) ||
                        f.name.endsWith(".jpeg", true))
            }?.size ?: 0
        }.getOrDefault(0)
    }

    /**
     * Copies all wallpapers from the default Android/data storage to a user-selected SAF folder
     * before uninstallation.
     */
    suspend fun copyDefaultWallpapersTo(targetUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val rootDoc = DocumentFile.fromTreeUri(context, targetUri)
                ?: throw IllegalStateException("Cannot access destination folder.")
            val files = appDataDownloadDir.listFiles { f ->
                f.isFile && (f.name.endsWith(".jpg", true) ||
                        f.name.endsWith(".png", true) ||
                        f.name.endsWith(".webp", true) ||
                        f.name.endsWith(".jpeg", true))
            }.orEmpty()
            var count = 0
            for (f in files) {
                val extension = f.extension.lowercase()
                val mimeType = if (extension == "png") "image/png" else "image/jpeg"
                val newDoc = rootDoc.createFile(mimeType, f.name) ?: continue
                context.contentResolver.openOutputStream(newDoc.uri)?.use { out ->
                    f.inputStream().use { inp -> inp.copyTo(out) }
                }
                count++
            }
            count
        }
    }

    /**
     * Copies all wallpapers from the default Android/data storage to Pictures/AxWhaven_Wallpapers
     */
    suspend fun copyDefaultWallpapersToPictures(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val destDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "AxWhaven_Wallpapers"
            )
            if (!destDir.exists()) destDir.mkdirs()
            val files = appDataDownloadDir.listFiles { f ->
                f.isFile && (f.name.endsWith(".jpg", true) ||
                        f.name.endsWith(".png", true) ||
                        f.name.endsWith(".webp", true) ||
                        f.name.endsWith(".jpeg", true))
            }.orEmpty()
            var count = 0
            for (f in files) {
                val target = File(destDir, f.name)
                f.copyTo(target, overwrite = true)
                runCatching {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(target.absolutePath),
                        arrayOf(if (f.extension.lowercase() == "png") "image/png" else "image/jpeg"),
                        null
                    )
                }
                count++
            }
            count
        }
    }

    /**
     * Instantly returns initial asset directories without waiting for IO.
     * Guarantees 0ms Frame-0 rendering of all collections on fresh app start.
     */
    fun getInitialDirectories(): List<ArtDirectory> {
        val cached = memoryCachedAssetDirectories
        if (cached != null) return cached
        val initial = buildBundledDirectories()
        memoryCachedAssetDirectories = initial
        return initial
    }

    private fun scanAssetDirectories(): List<ArtDirectory> {
        val directories = mutableListOf<ArtDirectory>()
        val assetManager = context.assets

        val allAssetFolders = runCatching { assetManager.list("wallpapers")?.toList().orEmpty() }
            .getOrDefault(emptyList())

        if (allAssetFolders.isEmpty()) {
            return buildBundledDirectories()
        }

        for (folderKey in allAssetFolders) {
            val assetSubPath = "wallpapers/$folderKey"
            val files = runCatching { assetManager.list(assetSubPath)?.toList().orEmpty() }
                .getOrDefault(emptyList())
                .filter { it.endsWith(".jpg", true) || it.endsWith(".png", true) || it.endsWith(".webp", true) }

            if (files.isNotEmpty()) {
                val displayName = folderKey.replace('_', ' ').replaceFirstChar { it.uppercase() }
                val items = files.mapIndexed { idx, fileName ->
                    WallpaperItem(
                        id = "asset_${folderKey}_$idx",
                        title = fileName.substringBeforeLast('.'),
                        directoryName = displayName,
                        source = WallpaperSource.AssetPath("$assetSubPath/$fileName")
                    )
                }
                directories.add(
                    ArtDirectory(
                        id = folderKey,
                        artName = displayName,
                        items = items,
                        isDownloadedFolder = false
                    )
                )
            }
        }
        return if (directories.isNotEmpty()) directories else buildBundledDirectories()
    }

    /**
     * Loads all local collections:
     * 1. Reuses cached static asset folders (or loads them once)
     * 2. The active "AxW Surf" folder (only if it contains at least 1 image)
     */
    suspend fun loadAllDirectories(): List<ArtDirectory> = withContext(Dispatchers.IO) {
        val directories = mutableListOf<ArtDirectory>()

        // 1. In-memory cached static asset directories
        var assetDirs = memoryCachedAssetDirectories
        if (assetDirs == null) {
            assetDirs = scanAssetDirectories()
            memoryCachedAssetDirectories = assetDirs
        }
        directories.addAll(assetDirs)

        // 2. Only show "AxW Surf" directory when it has at least 1 image!
        val downloadedItems = loadDownloadedWallpapers()
        if (downloadedItems.isNotEmpty()) {
            directories.add(
                ArtDirectory(
                    id = "user_downloads",
                    artName = "AxW Surf",
                    items = downloadedItems,
                    isDownloadedFolder = true
                )
            )
        }

        directories
    }

    /**
     * Scans EXCLUSIVELY the currently selected or chosen download directory.
     * The previous directory will no longer be linked!
     */
    suspend fun loadDownloadedWallpapers(): List<WallpaperItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<WallpaperItem>()
        val path = customPath
        val uri = downloadTreeUri

        when {
            // A. Custom Path is active: Scan only this directory
            !path.isNullOrBlank() -> {
                try {
                    val dir = File(path)
                    if (dir.exists() && dir.isDirectory) {
                        val files = dir.listFiles { file ->
                            file.isFile && (file.name.endsWith(".jpg", true) ||
                                    file.name.endsWith(".png", true) ||
                                    file.name.endsWith(".webp", true) ||
                                    file.name.endsWith(".jpeg", true))
                        }?.sortedByDescending { it.lastModified() }.orEmpty()

                        for ((idx, file) in files.withIndex()) {
                            items.add(
                                WallpaperItem(
                                    id = "custom_${file.name}_$idx",
                                    title = file.name.substringBeforeLast('.'),
                                    directoryName = "AxW Surf",
                                    source = WallpaperSource.FilePath(file.absolutePath)
                                )
                            )
                        }
                    }
                } catch (_: Exception) { }
            }

            // B. Custom SAF Tree is active: Scan only this SAF tree
            uri != null -> {
                try {
                    val rootDoc = DocumentFile.fromTreeUri(context, uri)
                    if (rootDoc != null && rootDoc.canRead()) {
                        val safFiles = rootDoc.listFiles()
                            .filter { file ->
                                file.isFile && (file.name?.endsWith(".jpg", true) == true ||
                                        file.name?.endsWith(".png", true) == true ||
                                        file.name?.endsWith(".webp", true) == true ||
                                        file.name?.endsWith(".jpeg", true) == true)
                            }
                            .sortedByDescending { it.lastModified() }

                        for ((idx, doc) in safFiles.withIndex()) {
                            val name = (doc.name ?: "Saved_$idx").substringBeforeLast('.')
                            items.add(
                                WallpaperItem(
                                    id = "saf_${doc.name}_$idx",
                                    title = name,
                                    directoryName = "AxW Surf",
                                    source = WallpaperSource.SafUri(doc.uri)
                                )
                            )
                        }
                    }
                } catch (_: Exception) { }
            }

            // C. Default active: Scan ONLY Android/data/com.axwhaven.walls/files/wallpapers/
            else -> {
                try {
                    if (appDataDownloadDir.exists() && appDataDownloadDir.isDirectory) {
                        val files = appDataDownloadDir.listFiles { file ->
                            file.isFile && (file.name.endsWith(".jpg", true) ||
                                    file.name.endsWith(".png", true) ||
                                    file.name.endsWith(".webp", true) ||
                                    file.name.endsWith(".jpeg", true))
                        }?.sortedByDescending { it.lastModified() }.orEmpty()

                        for ((idx, file) in files.withIndex()) {
                            items.add(
                                WallpaperItem(
                                    id = "appdata_${file.name}_$idx",
                                    title = file.name.substringBeforeLast('.'),
                                    directoryName = "AxW Surf",
                                    source = WallpaperSource.FilePath(file.absolutePath)
                                )
                            )
                        }
                    }
                } catch (_: Exception) { }
            }
        }

        items
    }

    /**
     * Downloads image directly and exclusively to the currently chosen directory:
     * - Default: Android/data/com.axwhaven.walls/files/wallpapers/
     * - Custom Path: User typed path
     * - Custom SAF: User chosen document tree
     */
    suspend fun downloadRemoteWallpaper(
        item: WallpaperItem,
        useOriginal: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val remote = item.source as? WallpaperSource.RemoteUrl
                ?: throw IllegalArgumentException("Item is already stored locally.")

            val targetUrl = if (useOriginal && !remote.originalImageUrl.isNullOrBlank()) {
                remote.originalImageUrl
            } else {
                remote.fullImageUrl
            }

            val extension = targetUrl.substringAfterLast('.', "jpg")
                .substringBefore('?')
                .lowercase()
                .takeIf { it in listOf("jpg", "jpeg", "png", "webp") } ?: "jpg"

            val mimeType = if (extension == "png") "image/png" else "image/jpeg"
            val qualitySuffix = if (useOriginal && !remote.originalImageUrl.isNullOrBlank() && remote.originalImageUrl != remote.fullImageUrl) "_orig" else ""
            val fileName = "AxWhaven_${remote.site.alias}_${remote.id}$qualitySuffix.$extension"
            val referer = remote.referer ?: "https://${remote.site.baseUrl.substringAfter("://")}/"

            val uri = downloadTreeUri
            val path = customPath

            when {
                // 1. User selected SAF folder
                uri != null -> {
                    val root = DocumentFile.fromTreeUri(context, uri)
                        ?: throw IllegalStateException("Cannot access selected folder.")
                    val docFile = root.createFile(mimeType, fileName)
                        ?: throw IllegalStateException("Cannot create file in selected folder.")
                    context.contentResolver.openOutputStream(docFile.uri)?.use { out ->
                        downloadUrlToStream(targetUrl, out, referer)
                    } ?: throw IllegalStateException("Cannot open output stream for selected folder.")
                    "Saved to ${root.name ?: "selected folder"}!"
                }

                // 2. User typed custom path
                !path.isNullOrBlank() -> {
                    val dir = File(path)
                    if (!dir.exists()) dir.mkdirs()
                    if (!dir.canWrite()) throw IllegalStateException("Folder is not writable: $path")
                    val targetFile = File(dir, fileName)
                    targetFile.outputStream().use { out ->
                        downloadUrlToStream(targetUrl, out, referer)
                    }
                    runCatching {
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(targetFile.absolutePath),
                            arrayOf(mimeType),
                            null
                        )
                    }
                    "Saved to $path!"
                }

                // 3. Default: Android/data/com.axwhaven.walls/files/wallpapers/
                else -> {
                    val appDataFile = File(appDataDownloadDir, fileName)
                    appDataFile.outputStream().use { out ->
                        downloadUrlToStream(targetUrl, out, referer)
                    }
                    runCatching {
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(appDataFile.absolutePath),
                            arrayOf(mimeType),
                            null
                        )
                    }
                    "Saved to AxWhaven vault (Android/data)!"
                }
            }
        }
    }

    private fun downloadUrlToStream(urlString: String, output: OutputStream, referer: String? = null) {
        // 1. Check if Coil already downloaded & cached this image
        val cached = runCatching {
            val loader = coil.Coil.imageLoader(context)
            val snapshot = loader.diskCache?.openSnapshot(urlString)
            snapshot?.use { snap ->
                snap.data.toFile().inputStream().use { inp ->
                    inp.copyTo(output)
                }
                true
            } ?: false
        }.getOrDefault(false)

        if (cached) return

        // 2. Direct network download with site-appropriate User-Agent and headers
        val ua = if (urlString.contains("donmai.us") || urlString.contains("anime-pictures.net")) {
            "AxWhaven/1.0 (Android; Mobile)"
        } else {
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
        }
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", ua)
            if (referer != null) {
                setRequestProperty("Referer", referer)
            }
            if (urlString.contains("danbooru.donmai.us")) {
                val cookie = android.webkit.CookieManager.getInstance().getCookie("https://danbooru.donmai.us")
                if (!cookie.isNullOrBlank()) {
                    setRequestProperty("Cookie", cookie)
                }
            }
        }
        try {
            conn.inputStream.use { input ->
                input.copyTo(output, bufferSize = 16 * 1024)
            }
        } finally {
            conn.disconnect()
        }
    }
}
