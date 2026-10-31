@file:OptIn(coil.annotation.ExperimentalCoilApi::class)

package com.axwhaven.walls.wallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.DisplayMetrics
import androidx.core.content.FileProvider
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.axwhaven.walls.model.ProviderSite
import com.axwhaven.walls.model.ScaleMode
import com.axwhaven.walls.model.WallpaperItem
import com.axwhaven.walls.model.WallpaperSource
import com.axwhaven.walls.model.WallpaperTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

class WallpaperApplier(private val context: Context) {

    private val wallpaperManager = WallpaperManager.getInstance(context)

    suspend fun applyWallpaper(
        item: WallpaperItem,
        scaleMode: ScaleMode,
        target: WallpaperTarget
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val rawBitmap = decodeBitmapFromSource(item.source)
                ?: throw IllegalStateException("Could not decode bitmap for ${item.title}")
            val screenSize = getTargetScreenSize()
            val transformedBitmap = transformBitmap(
                source = rawBitmap,
                targetWidth = screenSize.first,
                targetHeight = screenSize.second,
                scaleMode = scaleMode
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.setBitmap(
                    transformedBitmap,
                    null,
                    true,
                    target.flag
                )
            } else {
                wallpaperManager.setBitmap(transformedBitmap)
            }
            Unit
        }
    }

    suspend fun applyRandomLocalWallpaper(
        allLocalItems: List<WallpaperItem>,
        target: WallpaperTarget
    ): Result<WallpaperItem> = withContext(Dispatchers.IO) {
        runCatching {
            if (allLocalItems.isEmpty()) {
                throw IllegalStateException("No local wallpapers found to pick from.")
            }
            val randomPick = allLocalItems[Random.nextInt(allLocalItems.size)]
            applyWallpaper(randomPick, ScaleMode.FILL, target).getOrThrow()
            randomPick
        }
    }

    suspend fun launchSystemWallpaperPicker(
        activity: Activity,
        item: WallpaperItem
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cacheFolder = File(context.cacheDir, "shared_wallpapers").apply { mkdirs() }
            val tempFile = File(cacheFolder, "wallpaper_picker_${System.currentTimeMillis()}.jpg")

            when (val src = item.source) {
                is WallpaperSource.FilePath -> {
                    File(src.absolutePath).copyTo(tempFile, overwrite = true)
                }
                is WallpaperSource.AssetPath -> {
                    context.assets.open(src.assetRelativePath).use { input ->
                        FileOutputStream(tempFile).use { out -> input.copyTo(out) }
                    }
                }
                is WallpaperSource.SafUri -> {
                    context.contentResolver.openInputStream(src.uri)?.use { input ->
                        FileOutputStream(tempFile).use { out -> input.copyTo(out) }
                    } ?: throw IllegalStateException("Could not open URI")
                }
                is WallpaperSource.RemoteUrl -> {
                    var written = false
                    // 1. Instant check: Coil's disk cache (if already loaded on screen)
                    runCatching {
                        val loader = Coil.imageLoader(context)
                        val snapshot = loader.diskCache?.openSnapshot(src.fullImageUrl)
                            ?: loader.diskCache?.openSnapshot(src.previewUrl)
                        if (snapshot != null) {
                            snapshot.use { snap ->
                                snap.data.toFile().copyTo(tempFile, overwrite = true)
                                written = tempFile.length() > 0
                            }
                        }
                    }

                    // 2. Coil ImageRequest (hits memory cache or fetches quickly)
                    if (!written) {
                        val referer = src.referer ?: when (src.site) {
                            ProviderSite.DANBOORU -> "https://danbooru.donmai.us/"
                            ProviderSite.YANDERE -> "https://yande.re/"
                            ProviderSite.KONACHAN -> "https://konachan.com/"
                            ProviderSite.GELBOORU -> "https://gelbooru.com/"
                            ProviderSite.WALLHAVEN -> "https://wallhaven.cc/"
                            ProviderSite.SAFEBOORU -> "https://safebooru.org/"
                            ProviderSite.PIXIV -> "https://www.pixiv.net/"
                            ProviderSite.ANIME_PICTURES -> "https://anime-pictures.net/"
                        }
                        val ua = if (src.site == ProviderSite.DANBOORU || src.site == ProviderSite.ANIME_PICTURES) {
                            "AxWhaven/1.0 (Android; Mobile)"
                        } else {
                            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
                        }
                        val req = ImageRequest.Builder(context)
                            .data(src.fullImageUrl)
                            .allowHardware(false)
                            .addHeader("Referer", referer)
                            .addHeader("User-Agent", ua)
                            .build()
                        val res = Coil.imageLoader(context).execute(req)
                        if (res is SuccessResult) {
                            val bmp = drawableToBitmap(res.drawable)
                            FileOutputStream(tempFile).use { out ->
                                bmp.compress(Bitmap.CompressFormat.JPEG, 95, out)
                            }
                            written = tempFile.length() > 0
                        }
                    }

                    // 3. Fallback: decode directly
                    if (!written) {
                        val bitmap = decodeBitmapFromSource(src)
                            ?: throw IllegalStateException("Could not decode image for system picker.")
                        FileOutputStream(tempFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                        }
                    }
                }
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )

            val attachIntent = Intent(Intent.ACTION_ATTACH_DATA).apply {
                setDataAndType(contentUri, "image/*")
                putExtra("mimeType", "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(attachIntent, "Set Wallpaper via")
            activity.startActivity(chooser)
            Unit
        }
    }

    private suspend fun decodeBitmapFromSource(source: WallpaperSource): Bitmap? {
        return when (source) {
            is WallpaperSource.AssetPath -> {
                context.assets.open(source.assetRelativePath).use { input ->
                    BitmapFactory.decodeStream(input)
                }
            }
            is WallpaperSource.FilePath -> {
                FileInputStream(File(source.absolutePath)).use { input ->
                    BitmapFactory.decodeStream(input)
                }
            }
            is WallpaperSource.SafUri -> {
                context.contentResolver.openInputStream(source.uri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            }
            is WallpaperSource.RemoteUrl -> {
                val referer = source.referer ?: when (source.site) {
                    ProviderSite.DANBOORU -> "https://danbooru.donmai.us/"
                    ProviderSite.YANDERE -> "https://yande.re/"
                    ProviderSite.KONACHAN -> "https://konachan.com/"
                    ProviderSite.GELBOORU -> "https://gelbooru.com/"
                    ProviderSite.WALLHAVEN -> "https://wallhaven.cc/"
                    ProviderSite.SAFEBOORU -> "https://safebooru.org/"
                    ProviderSite.PIXIV -> "https://www.pixiv.net/"
                    ProviderSite.ANIME_PICTURES -> "https://anime-pictures.net/"
                }
                val ua = if (source.site == ProviderSite.DANBOORU || source.site == ProviderSite.ANIME_PICTURES) {
                    "AxWhaven/1.0 (Android; Mobile)"
                } else {
                    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
                }

                // 1. Try Coil first (hits cache in 5ms)
                val req = ImageRequest.Builder(context)
                    .data(source.fullImageUrl)
                    .allowHardware(false)
                    .addHeader("Referer", referer)
                    .addHeader("User-Agent", ua)
                    .build()
                val res = Coil.imageLoader(context).execute(req)
                if (res is SuccessResult) {
                    drawableToBitmap(res.drawable)
                } else {
                    // 2. Direct HTTP fallback
                    val conn = (URL(source.fullImageUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15_000
                        readTimeout = 25_000
                        setRequestProperty("User-Agent", ua)
                        setRequestProperty("Referer", referer)
                    }
                    try {
                        BufferedInputStream(conn.inputStream).use { input ->
                            BitmapFactory.decodeStream(input)
                        }
                    } finally {
                        conn.disconnect()
                    }
                }
            }
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1080
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun transformBitmap(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        scaleMode: ScaleMode
    ): Bitmap {
        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.BLACK)

        val srcWidth = source.width.toFloat().coerceAtLeast(1f)
        val srcHeight = source.height.toFloat().coerceAtLeast(1f)
        val dstWidth = targetWidth.toFloat()
        val dstHeight = targetHeight.toFloat()

        val scale = when (scaleMode) {
            ScaleMode.FILL -> maxOf(dstWidth / srcWidth, dstHeight / srcHeight)
            ScaleMode.FIT -> minOf(dstWidth / srcWidth, dstHeight / srcHeight)
        }

        val scaledWidth = srcWidth * scale
        val scaledHeight = srcHeight * scale
        val dx = (dstWidth - scaledWidth) / 2f
        val dy = (dstHeight - scaledHeight) / 2f

        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(dx, dy)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        canvas.drawBitmap(source, matrix, paint)
        return output
    }

    private fun getTargetScreenSize(): Pair<Int, Int> {
        val dm: DisplayMetrics = context.resources.displayMetrics
        val width = dm.widthPixels.coerceAtLeast(1080)
        val height = dm.heightPixels.coerceAtLeast(1920)
        return width to height
    }
}
