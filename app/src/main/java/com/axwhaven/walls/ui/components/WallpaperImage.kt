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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.axwhaven.walls.model.ProviderSite
import com.axwhaven.walls.model.WallpaperSource
import com.axwhaven.walls.ui.theme.AxWhavenColors
import com.axwhaven.walls.ui.theme.LocalAxWhavenColors
import java.io.File

/**
 * Referer header required by each remote provider for hotlink-protected origins.
 */
private fun ProviderSite.defaultReferer(): String = when (this) {
    ProviderSite.PIXIV -> "https://www.pixiv.net/"
    ProviderSite.GELBOORU -> "https://gelbooru.com/"
    ProviderSite.ANIME_PICTURES -> "https://anime-pictures.net/"
    ProviderSite.DANBOORU -> "https://danbooru.donmai.us/"
    ProviderSite.SAFEBOORU -> "https://safebooru.org/"
    ProviderSite.YANDERE -> "https://yande.re/"
    ProviderSite.KONACHAN -> "https://konachan.com/"
    ProviderSite.WALLHAVEN -> "https://wallhaven.cc/"
}

/**
 * Booru APIs reject browser UAs, while image CDNs reject the app UA.
 */
private fun ProviderSite.imageUserAgent(): String =
    if (this == ProviderSite.DANBOORU || this == ProviderSite.ANIME_PICTURES) {
        "AxWhaven/1.0 (Android; Mobile)"
    } else {
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
    }

/**
 * Single source of truth for outbound image request headers.
 * Previously this header block was duplicated at every call site.
 */
private fun ImageRequest.Builder.applyRemoteHeaders(source: WallpaperSource.RemoteUrl): ImageRequest.Builder =
    apply {
        addHeader("Referer", source.referer ?: source.site.defaultReferer())
        addHeader("User-Agent", source.site.imageUserAgent())
    }

@Composable
fun WallpaperImage(
    source: WallpaperSource?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
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

    if (model == null || (source is WallpaperSource.AssetPath && source.assetRelativePath.isBlank())) {
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

    val remoteSource = source as? WallpaperSource.RemoteUrl

    val request = ImageRequest.Builder(context)
        .data(model)
        .crossfade(if (remoteSource != null) 220 else 0)
        .allowHardware(true)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .apply { if (remoteSource != null) applyRemoteHeaders(remoteSource) }
        .build()

    // Cached thumbnail shown instantly while the full-resolution bitmap streams in.
    val fallbackRemote = remoteSource?.takeIf {
        useFullResolution && it.previewUrl.isNotBlank() && it.previewUrl != it.fullImageUrl
    }

    SubcomposeAsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        alignment = alignment,
        modifier = modifier,
        loading = {
            if (fallbackRemote != null) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(fallbackRemote.previewUrl)
                        .allowHardware(true)
                        .apply { applyRemoteHeaders(fallbackRemote) }
                        .build(),
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    alignment = alignment,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ImageLoadingSkeleton(colors = colors)
            }
        },
        error = {
            if (fallbackRemote != null) {
                // Fall back to the thumbnail when the full-resolution URL fails.
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(fallbackRemote.previewUrl)
                        .crossfade(300)
                        .allowHardware(true)
                        .apply { applyRemoteHeaders(fallbackRemote) }
                        .build(),
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    alignment = alignment,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ImageUnavailablePlaceholder(colors = colors)
            }
        }
    )
}

/**
 * Pulsing skeleton for the loading slot only.
 *
 * The infinite transition deliberately lives inside this composable so it is created
 * exclusively while [WallpaperImage] is still loading. Hoisting it into the parent meant
 * every cell in a grid ran its own infinite frame-clock animation even after the bitmap
 * had finished loading.
 */
@Composable
private fun ImageLoadingSkeleton(colors: AxWhavenColors) {
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

    val skeletonColor = if (colors.isGlassmorphism) {
        colors.surfaceElevated
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(skeletonColor.copy(alpha = pulseAlpha)),
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

@Composable
private fun ImageUnavailablePlaceholder(colors: AxWhavenColors) {
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
