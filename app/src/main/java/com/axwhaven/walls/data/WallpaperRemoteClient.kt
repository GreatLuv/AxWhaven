package com.axwhaven.walls.data

import android.content.Context
import com.axwhaven.walls.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class WallpaperRemoteClient(private val context: Context) {

    private val prefs = context.getSharedPreferences("axwhaven_prefs", Context.MODE_PRIVATE)

    var apiKey: String?
        get() = prefs.getString("api_key", null)
        set(value) = prefs.edit().putString("api_key", value).apply()

    var konachanCookie: String?
        get() = prefs.getString("konachan_cookie", null)
        set(value) = prefs.edit().putString("konachan_cookie", value).apply()

    var danbooruCookie: String?
        get() = prefs.getString("danbooru_cookie", null)
        set(value) = prefs.edit().putString("danbooru_cookie", value).apply()

    suspend fun queryWallpapers(
        provider: ProviderSite,
        queryText: String,
        page: Int,
        filters: HavenSurfFilterState
    ): Result<List<WallpaperItem>> = withContext(Dispatchers.IO) {
        runCatching {
            when (provider) {
                ProviderSite.WALLHAVEN -> fetchWallhaven(queryText, page, filters)
                ProviderSite.YANDERE, ProviderSite.KONACHAN -> fetchMoebooru(provider, queryText, page, filters)
                ProviderSite.SAFEBOORU -> fetchSafebooru(queryText, page, filters)
                ProviderSite.PIXIV -> fetchPixiv(queryText, page, filters)
                ProviderSite.ANIME_PICTURES -> fetchAnimePictures(queryText, page, filters)
                ProviderSite.GELBOORU -> fetchGelbooru(queryText, page, filters)
                ProviderSite.DANBOORU -> fetchDanbooru(queryText, page, filters)
            }
        }
    }

    suspend fun fetchTagSuggestions(
        provider: ProviderSite,
        prefix: String
    ): List<String> = withContext(Dispatchers.IO) {
        if (prefix.isBlank()) return@withContext emptyList()
        val queryWord = prefix.trim().substringAfterLast(' ', prefix.trim())
        if (queryWord.isBlank() || queryWord.startsWith("/")) return@withContext emptyList()

        runCatching {
            when (provider) {
                ProviderSite.YANDERE, ProviderSite.KONACHAN -> {
                    val tagBase = if (provider == ProviderSite.KONACHAN) "https://konachan.net" else provider.baseUrl
                    val cleanWord = queryWord.replace(' ', '_').lowercase()
                    val url = "$tagBase/tag.json?name=${urlEncode(cleanWord)}&order=count&limit=25"
                    val body = httpGet(url)
                    val array = JSONArray(body)
                    data class BooruTag(val name: String, val count: Int, val type: Int)
                    val tagList = mutableListOf<BooruTag>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val name = obj.optString("name")
                        val count = obj.optInt("count", 0)
                        val type = obj.optInt("type", 0)
                        if (name.isNotBlank()) {
                            tagList.add(BooruTag(name, count, type))
                        }
                    }
                    val sorted = tagList.sortedWith(
                        compareBy<BooruTag> { tag ->
                            when {
                                tag.name.equals(cleanWord, ignoreCase = true) -> 0
                                tag.type == 3 -> 1 // Copyright/franchise tag (e.g. wuthering_waves, genshin_impact)
                                tag.type == 0 -> 2 // General tag
                                else -> 3 // Character (type 4), artist (type 1), etc.
                            }
                        }.thenByDescending { it.count }
                    ).map { it.name }.distinct().take(12)

                    if (sorted.none { it.equals(cleanWord, ignoreCase = true) }) {
                        listOf(cleanWord) + sorted.take(11)
                    } else {
                        sorted
                    }
                }
                ProviderSite.SAFEBOORU, ProviderSite.GELBOORU -> {
                    val url = "https://safebooru.org/autocomplete.php?q=${urlEncode(queryWord)}"
                    val body = httpGet(url)
                    val array = JSONArray(body)
                    val results = mutableListOf<String>()
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i)
                        val value = obj?.optString("value") ?: array.optString(i)
                        if (value.isNotBlank()) results.add(value)
                    }
                    results
                }
                ProviderSite.PIXIV -> {
                    listOf(
                        "${queryWord}_illustration",
                        "${queryWord}_genshin",
                        "${queryWord}_fate",
                        "${queryWord}_original"
                    ).filter { it.startsWith(queryWord, ignoreCase = true) }
                }
                ProviderSite.ANIME_PICTURES -> {
                    listOf(
                        queryWord,
                        "${queryWord} girl",
                        "${queryWord} wallpaper",
                        "${queryWord} anime"
                    )
                }
                ProviderSite.WALLHAVEN -> {
                    val popularWallhavenKeywords = listOf(
                        "Genshin Impact", "Honkai Star Rail", "Cyberpunk", "Landscape",
                        "Scenery", "Makoto Shinkai", "Anime Girl", "Neon", "Cityscape",
                        "Fantasy", "Dark", "Samurai", "Vocaloid", "Fate", "Nature", "Space",
                        "Studio Ghibli", "Pixel Art", "Retro", "Minimalist"
                    )
                    val matched = popularWallhavenKeywords.filter { it.contains(queryWord, ignoreCase = true) }
                    if (matched.isNotEmpty()) {
                        matched
                    } else {
                        listOf(
                            queryWord,
                            "$queryWord anime",
                            "$queryWord scenery",
                            "$queryWord 4k"
                        )
                    }
                }
                else -> {
                    listOf(
                        queryWord,
                        "$queryWord anime",
                        "$queryWord scenery"
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    // 1. Wallhaven
    private fun fetchWallhaven(queryText: String, page: Int, filters: HavenSurfFilterState): List<WallpaperItem> {
        val params = mutableListOf<String>()
        val cleanQuery = queryText.trim().replace('_', ' ')
        if (cleanQuery.isNotBlank()) params.add("q=${urlEncode(cleanQuery)}")
        params.add("categories=111")
        val purityMask = when (filters.rating) {
            ContentRating.SFW -> "100"
            ContentRating.QUESTIONABLE -> "010"
            ContentRating.NSFW -> if (apiKey.isNullOrBlank()) "110" else "001"
            ContentRating.ALL -> if (apiKey.isNullOrBlank()) "110" else "111"
        }
        params.add("purity=$purityMask")
        if (filters.resolution != ResolutionFilter.ALL) {
            params.add("atleast=${filters.resolution.minWidth}x${filters.resolution.minHeight}")
        }
        filters.aspectRatio.ratioToken?.let { params.add("ratios=${urlEncode(it)}") }
        params.add("page=$page")
        apiKey?.takeIf { it.isNotBlank() }?.let { params.add("apikey=${urlEncode(it)}") }

        val body = httpGet("${ProviderSite.WALLHAVEN.baseUrl}/api/v1/search?${params.joinToString("&")}")
        val root = JSONObject(body)
        val dataArray = root.optJSONArray("data") ?: JSONArray()
        val items = mutableListOf<WallpaperItem>()
        for (i in 0 until dataArray.length()) {
            val obj = dataArray.getJSONObject(i)
            val id = obj.optString("id")
            val fullPath = obj.optString("path")
            val preview = obj.optJSONObject("thumbs")?.optString("large").takeUnless { it.isNullOrBlank() } ?: fullPath
            val width = obj.optInt("dimension_x", 0)
            val height = obj.optInt("dimension_y", 0)
            val purityStr = obj.optString("purity", "sfw")
            val rating = when (purityStr) {
                "nsfw" -> ContentRating.NSFW
                "sketchy" -> ContentRating.QUESTIONABLE
                else -> ContentRating.SFW
            }
            if (fullPath.isNotBlank()) {
                items.add(
                    WallpaperItem(
                        id = "wh_$id",
                        title = "Wallhaven #$id",
                        directoryName = "Wallhaven",
                        source = WallpaperSource.RemoteUrl(id, preview, fullPath, width, height, ProviderSite.WALLHAVEN, rating),
                        width = width,
                        height = height,
                        site = ProviderSite.WALLHAVEN
                    )
                )
            }
        }
        return items
    }

    // 2. Yande.re & Konachan
    private fun fetchMoebooru(provider: ProviderSite, queryText: String, page: Int, filters: HavenSurfFilterState): List<WallpaperItem> {
        val tags = mutableListOf<String>()
        if (queryText.isNotBlank()) {
            val words = queryText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            for (w in words) {
                when (w.lowercase()) {
                    "recent", "latest" -> tags.add("order:id_desc")
                    "random" -> tags.add("order:random")
                    else -> tags.add(w)
                }
            }
        }
        when (filters.rating) {
            ContentRating.SFW -> tags.add("rating:s")
            ContentRating.QUESTIONABLE -> tags.add("rating:q")
            ContentRating.NSFW -> {
                tags.add("rating:e")
                if (provider == ProviderSite.YANDERE) {
                    if (!tags.any { it.startsWith("order:") }) {
                        // On empty search, query random images for discovery on Yande.re NSFW
                        if (queryText.isBlank()) {
                            tags.add("order:random")
                        }
                    }
                }
            }
            ContentRating.ALL -> Unit
        }
        if (filters.resolution != ResolutionFilter.ALL) {
            tags.add("width:>=${filters.resolution.minWidth}")
            tags.add("height:>=${filters.resolution.minHeight}")
        }

        val baseUrl = when (provider) {
            ProviderSite.KONACHAN -> {
                if (filters.rating == ContentRating.SFW) "https://konachan.net" else "https://konachan.com"
            }
            else -> provider.baseUrl
        }

        return try {
            val url = "$baseUrl/post.json?limit=40&page=$page&tags=${urlEncode(tags.joinToString(" "))}"
            val body = httpGet(url, mapOf("Referer" to "$baseUrl/"))
            val array = JSONArray(body)
            val items = mutableListOf<WallpaperItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optLong("id").toString()
                val jpegUrl = normalizeUrl(obj.optString("jpeg_url"), baseUrl)
                val rawFileUrl = normalizeUrl(obj.optString("file_url"), baseUrl)
                val fastCompressedUrl = if (jpegUrl.isNotBlank()) jpegUrl else rawFileUrl
                val uncompressedOriginalUrl = if (rawFileUrl.isNotBlank()) rawFileUrl else jpegUrl
                val previewUrl = normalizeUrl(obj.optString("sample_url").ifBlank { obj.optString("preview_url") }, baseUrl)
                val width = obj.optInt("width", 0)
                val height = obj.optInt("height", 0)
                if (fastCompressedUrl.isNotBlank()) {
                    items.add(
                        WallpaperItem(
                            id = "${provider.alias}_$id",
                            title = "${provider.displayName} #$id",
                            directoryName = provider.displayName,
                            source = WallpaperSource.RemoteUrl(
                                id = id,
                                previewUrl = previewUrl.ifBlank { fastCompressedUrl },
                                fullImageUrl = fastCompressedUrl,
                                width = width,
                                height = height,
                                site = provider,
                                rating = parseBooruRating(obj.optString("rating", "s")),
                                referer = "$baseUrl/",
                                originalImageUrl = uncompressedOriginalUrl
                            ),
                            width = width,
                            height = height,
                            site = provider
                        )
                    )
                }
            }
            items
        } catch (e: Exception) {
            val isCf = e.message?.contains("403") == true ||
                e.message?.contains("Cloudflare", ignoreCase = true) == true ||
                e.message?.contains("Just a moment", ignoreCase = true) == true
            if (isCf && provider == ProviderSite.KONACHAN && filters.rating != ContentRating.SFW) {
                throw IllegalStateException("Konachan NSFW requires Cloudflare verification or cookie. Tap to pass cookie.", e)
            } else {
                throw e
            }
        }
    }

    // 3. Safebooru
    private fun fetchSafebooru(queryText: String, page: Int, filters: HavenSurfFilterState): List<WallpaperItem> {
        val tags = mutableListOf<String>()
        if (queryText.isNotBlank()) tags.addAll(queryText.trim().split(Regex("\\s+")).filter { it.isNotBlank() })
        val pid = (page - 1).coerceAtLeast(0)
        val url = "https://safebooru.org/index.php?page=dapi&s=post&q=index&json=1&limit=40&pid=$pid&tags=${urlEncode(tags.joinToString(" "))}"
        val body = httpGet(url)
        val array = JSONArray(body)
        val items = mutableListOf<WallpaperItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val id = obj.optLong("id").toString()
            val rawOriginal = obj.optString("file_url").ifBlank { obj.optString("sample_url") }
            val fastCompressed = obj.optString("sample_url").ifBlank { rawOriginal }
            val previewUrl = obj.optString("preview_url").ifBlank { fastCompressed }
            val width = obj.optInt("width", 0)
            val height = obj.optInt("height", 0)
            if (fastCompressed.isNotBlank()) {
                items.add(
                    WallpaperItem(
                        id = "safe_$id",
                        title = "Safebooru #$id",
                        directoryName = "Safebooru",
                        source = WallpaperSource.RemoteUrl(
                            id = id,
                            previewUrl = previewUrl,
                            fullImageUrl = fastCompressed,
                            width = width,
                            height = height,
                            site = ProviderSite.SAFEBOORU,
                            rating = ContentRating.SFW,
                            originalImageUrl = rawOriginal
                        ),
                        width = width,
                        height = height,
                        site = ProviderSite.SAFEBOORU
                    )
                )
            }
        }
        return items
    }

    // 4. Pixiv (Search and Discovery via Pixiv Ajax + pixiv.re proxy)
    private fun fetchPixiv(queryText: String, page: Int, filters: HavenSurfFilterState): List<WallpaperItem> {
        return try {
            val tag = if (queryText.isNotBlank()) queryText.trim() else "wallpaper"
            val safeWord = urlEncode(tag)
            val url = "https://www.pixiv.net/ajax/search/artworks/$safeWord?word=$safeWord&order=date_d&mode=all&p=$page&s_mode=s_tag_full&type=all&lang=en"
            val headers = mapOf(
                "Referer" to "https://www.pixiv.net/tags/$safeWord/artworks",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Accept" to "application/json, text/plain, */*"
            )
            val body = httpGet(url, headers)
            if (!body.trim().startsWith("{")) {
                // If Pixiv returned HTML instead of JSON (e.g. login wall / challenge), fallback gracefully
                return fetchSafebooru(if (queryText.isNotBlank()) queryText else "wallpaper", page, filters)
                    .map { it.copy(directoryName = "Pixiv", site = ProviderSite.PIXIV) }
            }
            val root = JSONObject(body)
            val dataArray = root.optJSONObject("body")?.optJSONObject("illustManga")?.optJSONArray("data") ?: JSONArray()
            val items = mutableListOf<WallpaperItem>()
            for (i in 0 until dataArray.length()) {
                val obj = dataArray.getJSONObject(i)
                val id = obj.optString("id")
                val title = obj.optString("title", "Pixiv #$id")
                val origUrl = obj.optString("url")
                if (origUrl.isNotBlank()) {
                    val fullUrl = origUrl.replace("i.pximg.net", "i.pixiv.re").replace("/c/250x250_80_a2/img-master/", "/img-master/")
                    val previewUrl = origUrl.replace("i.pximg.net", "i.pixiv.re")
                    val originalUrl = "https://pixiv.re/$id.jpg"
                    val width = obj.optInt("width", 0)
                    val height = obj.optInt("height", 0)
                    items.add(
                        WallpaperItem(
                            id = "pixiv_$id",
                            title = title,
                            directoryName = "Pixiv",
                            source = WallpaperSource.RemoteUrl(
                                id = id,
                                previewUrl = previewUrl,
                                fullImageUrl = fullUrl,
                                width = width,
                                height = height,
                                site = ProviderSite.PIXIV,
                                rating = ContentRating.SFW,
                                referer = "https://www.pixiv.net/",
                                originalImageUrl = originalUrl
                            ),
                            width = width,
                            height = height,
                            site = ProviderSite.PIXIV
                        )
                    )
                }
            }
            if (items.isNotEmpty()) items else fetchSafebooru(if (queryText.isNotBlank()) queryText else "wallpaper", page, filters)
                .map { it.copy(directoryName = "Pixiv", site = ProviderSite.PIXIV) }
        } catch (e: Exception) {
            fetchSafebooru(if (queryText.isNotBlank()) queryText else "wallpaper", page, filters)
                .map { it.copy(directoryName = "Pixiv", site = ProviderSite.PIXIV) }
        }
    }

    // 5. Anime-Pictures
    private fun fetchAnimePictures(queryText: String, page: Int, filters: HavenSurfFilterState): List<WallpaperItem> {
        return try {
            val pageIndex = (page - 1).coerceAtLeast(0)
            val params = mutableListOf("page=$pageIndex", "posts_per_page=40", "lang=en")
            if (queryText.isNotBlank()) {
                params.add("search_tag=${urlEncode(queryText.trim())}")
            }
            // Use dedicated direct API subdomain: api.anime-pictures.net
            val url = "https://api.anime-pictures.net/api/v3/posts?${params.joinToString("&")}"
            val headers = mapOf(
                "Referer" to "https://anime-pictures.net/",
                "Accept" to "application/json, text/plain, */*"
            )
            val body = httpGet(url, headers)
            if (!body.trim().startsWith("{")) {
                return fetchSafebooru(if (queryText.isNotBlank()) queryText else "anime", page, filters)
                    .map { it.copy(directoryName = "Anime-Pictures", site = ProviderSite.ANIME_PICTURES) }
            }
            val root = JSONObject(body)
            val posts = root.optJSONArray("posts") ?: JSONArray()
            val items = mutableListOf<WallpaperItem>()
            for (i in 0 until posts.length()) {
                val obj = posts.getJSONObject(i)
                val id = obj.optLong("id").toString()
                val md5 = obj.optString("md5")
                val width = obj.optInt("width", 0)
                val height = obj.optInt("height", 0)
                if (md5.length >= 3) {
                    val prefix = md5.substring(0, 3)
                    val previewUrl = "https://opreviews.anime-pictures.net/$prefix/${md5}_cp.avif"
                    val fullUrl = "https://opreviews.anime-pictures.net/$prefix/${md5}_bp.avif"
                    items.add(
                        WallpaperItem(
                            id = "ap_$id",
                            title = "Anime-Pictures #$id",
                            directoryName = "Anime-Pictures",
                            source = WallpaperSource.RemoteUrl(id, previewUrl, fullUrl, width, height, ProviderSite.ANIME_PICTURES, ContentRating.SFW, "https://anime-pictures.net/"),
                            width = width,
                            height = height,
                            site = ProviderSite.ANIME_PICTURES
                        )
                    )
                }
            }
            if (items.isNotEmpty()) items else fetchSafebooru(if (queryText.isNotBlank()) queryText else "anime", page, filters)
                .map { it.copy(directoryName = "Anime-Pictures", site = ProviderSite.ANIME_PICTURES) }
        } catch (e: Exception) {
            fetchSafebooru(if (queryText.isNotBlank()) queryText else "anime", page, filters)
                .map { it.copy(directoryName = "Anime-Pictures", site = ProviderSite.ANIME_PICTURES) }
        }
    }

    // 6. Gelbooru
    private fun fetchGelbooru(queryText: String, page: Int, filters: HavenSurfFilterState): List<WallpaperItem> {
        return try {
            val tags = mutableListOf<String>()
            if (queryText.isNotBlank()) {
                tags.addAll(queryText.trim().split(Regex("\\s+")).filter { it.isNotBlank() })
            }
            when (filters.rating) {
                ContentRating.NSFW -> tags.add("rating:explicit")
                ContentRating.QUESTIONABLE -> tags.add("rating:questionable")
                else -> tags.add("rating:general")
            }
            val tagParam = urlEncode(tags.joinToString(" "))
            val pid = (page - 1) * 42
            val url = "https://gelbooru.com/index.php?page=post&s=list&tags=$tagParam&pid=$pid"
            val html = httpGet(url, mapOf("Referer" to "https://gelbooru.com/"))
            val items = mutableListOf<WallpaperItem>()
            val regex = Regex("""<img\s+src="([^"]*thumbnails/([a-z0-9]+)/([a-z0-9]+)/thumbnail_([a-z0-9]+)\.jpg)"[^>]*alt="([^"]*)"""", RegexOption.IGNORE_CASE)
            val matches = regex.findAll(html)
            for (m in matches) {
                val thumbUrl = m.groupValues[1]
                val md5 = m.groupValues[4]
                val alt = m.groupValues[5]
                val sampleUrl = thumbUrl.replace("thumbnails", "samples").replace("thumbnail_", "sample_")
                val rawOriginalUrl = thumbUrl.replace("thumbnails", "images").replace("thumbnail_", "")
                items.add(
                    WallpaperItem(
                        id = "gel_$md5",
                        title = alt.take(30).ifBlank { "Gelbooru #$md5" },
                        directoryName = "Gelbooru",
                        source = WallpaperSource.RemoteUrl(
                            id = md5,
                            previewUrl = thumbUrl,
                            fullImageUrl = sampleUrl,
                            width = 1920,
                            height = 1080,
                            site = ProviderSite.GELBOORU,
                            rating = filters.rating,
                            referer = "https://gelbooru.com/",
                            originalImageUrl = rawOriginalUrl
                        ),
                        width = 1920,
                        height = 1080,
                        site = ProviderSite.GELBOORU
                    )
                )
            }
            if (items.isNotEmpty()) items else fetchSafebooru(if (queryText.isNotBlank()) queryText else "all", page, filters)
                .map { it.copy(directoryName = "Gelbooru", site = ProviderSite.GELBOORU) }
        } catch (e: Exception) {
            fetchSafebooru(if (queryText.isNotBlank()) queryText else "all", page, filters)
                .map { it.copy(directoryName = "Gelbooru", site = ProviderSite.GELBOORU) }
        }
    }

    // 7. Danbooru
    private fun fetchDanbooru(queryText: String, page: Int, filters: HavenSurfFilterState): List<WallpaperItem> {
        return try {
            val tags = mutableListOf<String>()
            if (queryText.isNotBlank()) tags.addAll(queryText.trim().split(Regex("\\s+")).filter { it.isNotBlank() })
            when (filters.rating) {
                ContentRating.SFW -> tags.add("rating:g")
                ContentRating.QUESTIONABLE -> tags.add("rating:s")
                ContentRating.NSFW -> tags.add("rating:e")
                ContentRating.ALL -> Unit
            }
            val url = "https://danbooru.donmai.us/posts.json?limit=40&page=$page&tags=${urlEncode(tags.joinToString(" "))}"
            val body = httpGet(url, mapOf("Referer" to "https://danbooru.donmai.us/"))
            val array = JSONArray(body)
            val items = mutableListOf<WallpaperItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optLong("id").toString()
                var fileUrl = obj.optString("file_url").ifBlank { obj.optString("large_file_url") }
                val largeFileUrl = obj.optString("large_file_url")
                var previewUrl = obj.optString("preview_file_url")
                if (fileUrl.isBlank()) {
                    val mediaAsset = obj.optJSONObject("media_asset")
                    if (mediaAsset != null) {
                        val variants = mediaAsset.optJSONArray("variants")
                        if (variants != null) {
                            for (v in 0 until variants.length()) {
                                val variant = variants.getJSONObject(v)
                                val vType = variant.optString("type")
                                val vUrl = variant.optString("url")
                                if (vType == "original" || vType == "sample") {
                                    if (fileUrl.isBlank()) fileUrl = vUrl
                                } else if (vType == "180x180" || vType == "360x360" || vType == "720x720") {
                                    if (previewUrl.isBlank()) previewUrl = vUrl
                                }
                            }
                        }
                    }
                }
                if (previewUrl.isBlank()) previewUrl = fileUrl
                val width = obj.optInt("image_width", 0)
                val height = obj.optInt("image_height", 0)

                val fastCompressedUrl = if (fileUrl.endsWith(".png", true) && largeFileUrl.isNotBlank()) largeFileUrl else fileUrl
                val uncompressedOriginalUrl = fileUrl

                if (fastCompressedUrl.isNotBlank()) {
                    items.add(
                        WallpaperItem(
                            id = "dan_$id",
                            title = "Danbooru #$id",
                            directoryName = "Danbooru",
                            source = WallpaperSource.RemoteUrl(
                                id = id,
                                previewUrl = previewUrl,
                                fullImageUrl = fastCompressedUrl,
                                width = width,
                                height = height,
                                site = ProviderSite.DANBOORU,
                                rating = parseBooruRating(obj.optString("rating", "g")),
                                referer = "https://danbooru.donmai.us/",
                                originalImageUrl = uncompressedOriginalUrl
                            ),
                            width = width,
                            height = height,
                            site = ProviderSite.DANBOORU
                        )
                    )
                }
            }
            items
        } catch (e: Exception) {
            val isCf = e.message?.contains("403") == true ||
                e.message?.contains("Cloudflare", ignoreCase = true) == true ||
                e.message?.contains("Just a moment", ignoreCase = true) == true
            if (isCf) {
                throw IllegalStateException("Danbooru requires Cloudflare verification or cookie. Tap to pass cookie.", e)
            } else {
                throw e
            }
        }
    }

    private fun parseBooruRating(code: String): ContentRating = when (code.lowercase()) {
        "g", "s", "safe", "general" -> ContentRating.SFW
        "q", "sensitive", "questionable" -> ContentRating.QUESTIONABLE
        "e", "explicit" -> ContentRating.NSFW
        else -> ContentRating.SFW
    }

    private fun normalizeUrl(rawUrl: String, baseUrl: String): String = when {
        rawUrl.isBlank() -> ""
        rawUrl.startsWith("//") -> "https:$rawUrl"
        rawUrl.startsWith("/") -> "$baseUrl$rawUrl"
        else -> rawUrl
    }

    private fun httpGet(urlString: String, extraHeaders: Map<String, String> = emptyMap()): String {
        val userAgent = runCatching {
            android.webkit.WebSettings.getDefaultUserAgent(context)
        }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"

        val domainUrl = when {
            urlString.contains("danbooru.donmai.us") -> "https://danbooru.donmai.us"
            urlString.contains("konachan.com") -> "https://konachan.com"
            else -> urlString
        }

        val cookie = runCatching {
            android.webkit.CookieManager.getInstance().getCookie(domainUrl)
        }.getOrNull()

        val savedKonachanCookie = if (urlString.contains("konachan.com")) konachanCookie else null
        val savedDanbooruCookie = if (urlString.contains("danbooru.donmai.us")) danbooruCookie else null
        val combinedCookie = mergeCookies(cookie, savedKonachanCookie, savedDanbooruCookie)

        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "application/json, text/plain, text/html, */*")
            setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            setRequestProperty("Sec-CH-UA-Mobile", "?1")
            setRequestProperty("Sec-CH-UA-Platform", "\"Android\"")
            if (urlString.contains("danbooru.donmai.us")) {
                setRequestProperty("Referer", "https://danbooru.donmai.us/posts")
            } else if (urlString.contains("konachan.com")) {
                setRequestProperty("Referer", "https://konachan.com/post")
            }
            if (combinedCookie.isNotBlank()) {
                setRequestProperty("Cookie", combinedCookie)
            }
            extraHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw IllegalStateException("HTTP $status from $urlString: ${responseText.take(200)}")
            }
            return responseText
        } finally {
            connection.disconnect()
        }
    }

    private fun mergeCookies(vararg cookieStrings: String?): String {
        val map = linkedMapOf<String, String>()
        for (str in cookieStrings) {
            if (str.isNullOrBlank()) continue
            val parts = str.split(';')
            for (part in parts) {
                val trimmed = part.trim()
                if (trimmed.isEmpty()) continue
                val eqIdx = trimmed.indexOf('=')
                if (eqIdx > 0) {
                    val key = trimmed.substring(0, eqIdx).trim()
                    val value = trimmed.substring(eqIdx + 1).trim()
                    map[key] = value
                } else {
                    map[trimmed] = ""
                }
            }
        }
        return map.entries.joinToString("; ") { (k, v) ->
            if (v.isEmpty()) k else "$k=$v"
        }
    }

    private fun urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")
}
