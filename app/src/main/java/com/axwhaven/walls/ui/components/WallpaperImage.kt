package com.axwhaven.walls.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.axwhaven.walls.model.ProviderSite
import com.axwhaven.walls.model.WallpaperSource
import com.axwhaven.walls.ui.theme.LocalAxWhavenColors
import java.io.File

@Composable
fun WallpaperImage(
    source: WallpaperSource?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    useFullResolution: Boolean = false
) {
    val context = LocalContext.current
    val colors = LocalAxWhavenColors.current

    val model = when (source) {
        is WallpaperSource.AssetPath -> "file:///android_asset/${source.assetRelativePath}"
        is WallpaperSource.FilePath -> File(source.absolutePath)
        is WallpaperSource.SafUri -> source.uri
        is WallpaperSource.RemoteUrl -> if (useFullResolution) source.fullImageUrl else source.previewUrl
        null -> null
    }

    if (model == null) {
        Box(
            modifier = modifier.background(colors.surfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = colors.textMuted.copy(alpha = 0.5f),
                modifier = Modifier.size(36.dp)
            )
        }
        return
    }

    val request = ImageRequest.Builder(context)
        .data(model)
        .crossfade(350)
        .allowHardware(true)
        .apply {
            if (source is WallpaperSource.RemoteUrl) {
                val referer = source.referer ?: when (source.site) {
                    ProviderSite.PIXIV -> "https://www.pixiv.net/"
                    ProviderSite.GELBOORU -> "https://gelbooru.com/"
                    ProviderSite.ANIME_PICTURES -> "https://anime-pictures.net/"
                    ProviderSite.DANBOORU -> "https://danbooru.donmai.us/"
                    ProviderSite.SAFEBOORU -> "https://safebooru.org/"
                    ProviderSite.YANDERE -> "https://yande.re/"
                    ProviderSite.KONACHAN -> "https://konachan.com/"
                    ProviderSite.WALLHAVEN -> "https://wallhaven.cc/"
                }
                addHeader("Referer", referer)
                val ua = if (source.site == ProviderSite.DANBOORU || source.site == ProviderSite.ANIME_PICTURES) {
                    "AxWhaven/1.0 (Android; Mobile)"
                } else {
                    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
                }
                addHeader("User-Agent", ua)
            }
        }
        .build()

    // Subtle pulsing animation for loading state
    val infiniteTransition = rememberInfiniteTransition(label = "ImageLoadingShimmer")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    SubcomposeAsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        loading = {
            if (useFullResolution && source is WallpaperSource.RemoteUrl && source.previewUrl.isNotBlank() && source.previewUrl != source.fullImageUrl) {
                // Instantly display cached preview while high-res loads in background
                val previewRequest = ImageRequest.Builder(context)
                    .data(source.previewUrl)
                    .allowHardware(true)
                    .apply {
                        val referer = source.referer ?: when (source.site) {
                            ProviderSite.PIXIV -> "https://www.pixiv.net/"
                            ProviderSite.GELBOORU -> "https://gelbooru.com/"
                            ProviderSite.ANIME_PICTURES -> "https://anime-pictures.net/"
                            ProviderSite.DANBOORU -> "https://danbooru.donmai.us/"
                            ProviderSite.SAFEBOORU -> "https://safebooru.org/"
                            ProviderSite.YANDERE -> "https://yande.re/"
                            ProviderSite.KONACHAN -> "https://konachan.com/"
                            ProviderSite.WALLHAVEN -> "https://wallhaven.cc/"
                        }
                        addHeader("Referer", referer)
                        val ua = if (source.site == ProviderSite.DANBOORU || source.site == ProviderSite.ANIME_PICTURES) {
                            "AxWhaven/1.0 (Android; Mobile)"
                        } else {
                            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
                        }
                        addHeader("User-Agent", ua)
                    }
                    .build()
                SubcomposeAsyncImage(
                    model = previewRequest,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.surfaceElevated.copy(alpha = pulseAlpha)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = colors.primaryAccent.copy(alpha = 0.25f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        error = {
            if (useFullResolution && source is WallpaperSource.RemoteUrl && source.previewUrl.isNotBlank() && source.previewUrl != source.fullImageUrl) {
                // Fallback to preview thumbnail if full resolution URL fails
                val fallbackRequest = ImageRequest.Builder(context)
                    .data(source.previewUrl)
                    .crossfade(300)
                    .allowHardware(true)
                    .apply {
                        val referer = source.referer ?: when (source.site) {
                            ProviderSite.PIXIV -> "https://www.pixiv.net/"
                            ProviderSite.GELBOORU -> "https://gelbooru.com/"
                            ProviderSite.ANIME_PICTURES -> "https://anime-pictures.net/"
                            ProviderSite.DANBOORU -> "https://danbooru.donmai.us/"
                            ProviderSite.SAFEBOORU -> "https://safebooru.org/"
                            ProviderSite.YANDERE -> "https://yande.re/"
                            ProviderSite.KONACHAN -> "https://konachan.com/"
                            ProviderSite.WALLHAVEN -> "https://wallhaven.cc/"
                        }
                        addHeader("Referer", referer)
                        val ua = if (source.site == ProviderSite.DANBOORU || source.site == ProviderSite.ANIME_PICTURES) {
                            "AxWhaven/1.0 (Android; Mobile)"
                        } else {
                            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
                        }
                        addHeader("User-Agent", ua)
                    }
                    .build()
                SubcomposeAsyncImage(
                    model = fallbackRequest,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.surfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = "Image unavailable",
                            tint = colors.textMuted.copy(alpha = 0.6f),
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Unavailable",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMuted
                        )
                    }
                }
            }
        }
    )
}
