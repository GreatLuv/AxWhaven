package com.axwhaven.walls.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

/**
 * DanbooruBridge: Chromium WebView execution bridge for Danbooru.
 *
 * Cloudflare Turnstile blocks Java HttpURLConnection due to TLS handshake / JA3 fingerprinting.
 * By executing queries directly inside Chromium WebView via same-origin fetch(), requests inherit
 * genuine Chromium TLS signatures and active session cookies (__cf_bm, cf_clearance, _danbooru_session).
 */
object DanbooruBridge {

    private var activeWebView: WebView? = null
    private var pendingFetchDeferred: CompletableDeferred<String>? = null
    private var apiCheckCallback: ((Boolean, Int) -> Unit)? = null
    private var isTurnstileCleared = false

    private val jsInterface = object {
        @JavascriptInterface
        fun onFetchSuccess(data: String) {
            val deferred = pendingFetchDeferred
            pendingFetchDeferred = null
            deferred?.complete(data)
        }

        @JavascriptInterface
        fun onFetchError(error: String) {
            val deferred = pendingFetchDeferred
            pendingFetchDeferred = null
            deferred?.completeExceptionally(IllegalStateException(error))
        }

        @JavascriptInterface
        fun onApiCheck(status: Int) {
            val ok = status in 200..299
            if (ok) isTurnstileCleared = true
            Handler(Looper.getMainLooper()).post {
                apiCheckCallback?.invoke(ok, status)
            }
        }
    }

    fun isCleared(): Boolean = isTurnstileCleared

    fun markCleared(cleared: Boolean = true) {
        isTurnstileCleared = cleared
    }

    fun getOrCreateWebView(context: Context): WebView {
        if (activeWebView == null) {
            activeWebView = WebView(context.applicationContext).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                @Suppress("DEPRECATION")
                settings.databaseEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.userAgentString = runCatching {
                    WebSettings.getDefaultUserAgent(context)
                }.getOrNull()

                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

                addJavascriptInterface(jsInterface, "AxWDanbooruBridge")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        CookieManager.getInstance().flush()
                        testApiAccess()
                    }
                }

                loadUrl("https://danbooru.donmai.us/posts")
            }
        }
        return activeWebView!!
    }

    fun testApiAccess(callback: ((Boolean, Int) -> Unit)? = null) {
        if (callback != null) {
            apiCheckCallback = callback
        }
        val view = activeWebView ?: return
        val testJs = """
            (function() {
                try {
                    fetch('https://danbooru.donmai.us/posts.json?limit=1', {
                        method: 'GET',
                        credentials: 'include',
                        headers: { 'Accept': 'application/json' }
                    })
                    .then(function(r) {
                        window.AxWDanbooruBridge.onApiCheck(r.status);
                    })
                    .catch(function(e) {
                        window.AxWDanbooruBridge.onApiCheck(0);
                    });
                } catch(err) {
                    window.AxWDanbooruBridge.onApiCheck(0);
                }
            })();
        """.trimIndent()
        view.evaluateJavascript(testJs, null)
    }

    suspend fun fetchJson(context: Context, urlString: String): String = withContext(Dispatchers.Main) {
        val view = getOrCreateWebView(context)
        val deferred = CompletableDeferred<String>()
        pendingFetchDeferred = deferred

        // If webView is not loaded to danbooru yet, wait or trigger load
        if (view.url == null || !view.url.orEmpty().contains("danbooru.donmai.us")) {
            view.loadUrl("https://danbooru.donmai.us/posts")
        }

        val escapedUrl = JSONObject.quote(urlString)
        val fetchJs = """
            (function() {
                try {
                    fetch($escapedUrl, {
                        method: 'GET',
                        credentials: 'include',
                        headers: {
                            'Accept': 'application/json, text/plain, */*'
                        }
                    })
                    .then(function(res) {
                        if (!res.ok) {
                            throw new Error('HTTP ' + res.status);
                        }
                        return res.text();
                    })
                    .then(function(body) {
                        window.AxWDanbooruBridge.onFetchSuccess(body);
                    })
                    .catch(function(err) {
                        window.AxWDanbooruBridge.onFetchError(err.message || 'Fetch failed');
                    });
                } catch(e) {
                    window.AxWDanbooruBridge.onFetchError(e.message || 'Execution error');
                }
            })();
        """.trimIndent()

        view.evaluateJavascript(fetchJs, null)

        val result = withTimeoutOrNull(15_000) {
            deferred.await()
        } ?: throw IllegalStateException("Danbooru WebView request timed out. Please verify via In-App Browser.")

        result
    }
}
