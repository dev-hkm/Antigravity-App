package com.hkm.antigravity.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.ProfileStore
import androidx.webkit.UserAgentMetadata
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.hkm.antigravity.data.repository.AccountRepository

private val CHROME_VERSION_REGEX = Regex("""Chrome/(\d+\.\d+\.\d+\.\d+)""")
private val WV_TOKEN_REGEX = Regex(""";\s*wv(?=\))""")
private val VERSION_TOKEN_REGEX = Regex("""Version/\d+\.\d+\s*""")

internal fun isGoogleAuthUrl(rawUrl: String?): Boolean {
    val host = runCatching { Uri.parse(rawUrl).host?.lowercase() }.getOrNull() ?: return false
    return host == "accounts.google.com" ||
        host.endsWith(".accounts.google.com") ||
        host == "oauth2.googleapis.com" ||
        host.contains("accounts.google.")
}

internal fun isAntigravityHost(rawUrl: String?): Boolean {
    val host = runCatching { Uri.parse(rawUrl).host?.lowercase() }.getOrNull() ?: return false
    return host == "antigravity.google.com" || host.endsWith(".antigravity.google.com")
}

internal fun isGoogleAuthRejectedUrl(rawUrl: String?): Boolean {
    if (rawUrl.isNullOrBlank()) return false
    val lower = rawUrl.lowercase()
    return isGoogleAuthUrl(rawUrl) && (
        lower.contains("/v3/signin/rejected") ||
            lower.contains("disallowed_useragent")
        )
}

private data class ResolvedUserAgent(
    val userAgent: String,
    val majorVersion: String,
    val fullVersion: String
)

private fun buildUserAgent(context: Context, desktop: Boolean): ResolvedUserAgent {
    val defaultUa = runCatching { WebSettings.getDefaultUserAgent(context) }.getOrNull().orEmpty()
    val fullVersion = CHROME_VERSION_REGEX.find(defaultUa)?.groupValues?.get(1) ?: "142.0.0.0"
    val majorVersion = fullVersion.substringBefore('.')

    val userAgent = if (desktop) {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$majorVersion.0.0.0 Safari/537.36"
    } else {
        defaultUa
            .replace(WV_TOKEN_REGEX, "")
            .replace("; wv", "")
            .replace(VERSION_TOKEN_REGEX, "")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
            .ifBlank {
                "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$fullVersion Mobile Safari/537.36"
            }
    }
    return ResolvedUserAgent(userAgent = userAgent, majorVersion = majorVersion, fullVersion = fullVersion)
}

private fun applyUserAgentAndMetadata(view: WebView, desktop: Boolean) {
    val resolved = buildUserAgent(view.context, desktop)
    val settings = view.settings
    settings.userAgentString = resolved.userAgent

    if (WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA)) {
        runCatching {
            val existing = WebSettingsCompat.getUserAgentMetadata(settings)
            var hasGoogleChrome = false
            val brands = existing.brandVersionList.map { bv ->
                if (bv.brand.contains("WebView", ignoreCase = true)) {
                    hasGoogleChrome = true
                    UserAgentMetadata.BrandVersion.Builder()
                        .setBrand("Google Chrome")
                        .setMajorVersion(bv.majorVersion.ifBlank { resolved.majorVersion })
                        .setFullVersion(bv.fullVersion.ifBlank { resolved.fullVersion })
                        .build()
                } else {
                    if (bv.brand.equals("Google Chrome", ignoreCase = true)) {
                        hasGoogleChrome = true
                    }
                    bv
                }
            }.toMutableList()

            if (!hasGoogleChrome) {
                brands.add(
                    UserAgentMetadata.BrandVersion.Builder()
                        .setBrand("Google Chrome")
                        .setMajorVersion(resolved.majorVersion)
                        .setFullVersion(resolved.fullVersion)
                        .build()
                )
            }

            val builder = UserAgentMetadata.Builder(existing)
                .setBrandVersionList(brands)
                .setFullVersion(existing.fullVersion?.takeIf { it.isNotBlank() } ?: resolved.fullVersion)

            if (desktop) {
                builder
                    .setPlatform("Windows")
                    .setPlatformVersion("15.0.0")
                    .setArchitecture("x86")
                    .setModel("")
                    .setMobile(false)
                    .setBitness(64)
                    .setWow64(false)
                if (WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA_FORM_FACTORS)) {
                    builder.setFormFactors(listOf(UserAgentMetadata.FORM_FACTOR_DESKTOP))
                }
            } else {
                builder
                    .setPlatform(existing.platform?.takeIf { it.isNotBlank() } ?: "Android")
                    .setMobile(true)
                if (WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA_FORM_FACTORS)) {
                    builder.setFormFactors(listOf(UserAgentMetadata.FORM_FACTOR_MOBILE))
                }
            }

            WebSettingsCompat.setUserAgentMetadata(settings, builder.build())
        }.onFailure { e ->
            Log.w("WebViewContainer", "Failed to update UserAgentMetadata: ${e.message}")
        }
    }
}

private fun handleNonHttpUri(context: Context, webView: WebView?, uri: Uri, rawUrl: String): Boolean {
    if (uri.scheme.equals("intent", ignoreCase = true)) {
        val intent = runCatching { Intent.parseUri(rawUrl, Intent.URI_INTENT_SCHEME) }.getOrNull()
        if (intent != null) {
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            val launched = runCatching {
                context.startActivity(intent)
                true
            }.getOrDefault(false)
            if (launched) return true

            val fallbackUrl = intent.getStringExtra("browser_fallback_url")
            if (!fallbackUrl.isNullOrBlank()) {
                webView?.loadUrl(fallbackUrl)
            }
        }
        return true
    }

    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
    return true
}

private const val REMOTE_LAYOUT_POLISH_SCRIPT = """
(function() {
    if (window.__antiLayoutPolishV1103) return;
    window.__antiLayoutPolishV1103 = true;
    function installLayoutPolish() {
        if (document.getElementById('anti-layout-polish-style')) return;
        var style = document.createElement('style');
        style.id = 'anti-layout-polish-style';
        style.textContent = [
            'html, body { overscroll-behavior: none; }',
            '.ga-content:has(> ga-remote-agents, > ga-remote-control) {',
            '  overflow: hidden !important;',
            '  display: flex !important;',
            '  flex-direction: column !important;',
            '}',
            '.ga-content > ga-remote-agents,',
            '.ga-content > ga-remote-control {',
            '  flex: 1 1 0% !important;',
            '  min-height: 0 !important;',
            '  height: 100% !important;',
            '}',
            '.ga-content > ga-remote-agents > .theme-standalone {',
            '  height: 100% !important;',
            '  min-height: 0 !important;',
            '}',
            '[role="dialog"]:has(> [role="navigation"][aria-label="Sidebar"]) {',
            '  height: 100% !important;',
            '  max-height: 100% !important;',
            '}',
            '[role="navigation"][aria-label="Sidebar"] > .overflow-y-auto,',
            '[data-sidebar-scroll-container="true"],',
            '[data-testid="conversation-list-sidebar"],',
            '[data-testid="conversation-view"],',
            '[data-bottom-sheet-popup] .overflow-y-auto {',
            '  overscroll-behavior-y: contain;',
            '  -webkit-overflow-scrolling: touch;',
            '}'
        ].join('\n');
        (document.head || document.documentElement).appendChild(style);
    }
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', installLayoutPolish, { once: true });
    } else {
        installLayoutPolish();
    }
})();
"""

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    modifier: Modifier = Modifier,
    accountId: String,
    url: String,
    appThemeMode: String = "system",
    webTheme: String = "dark",
    isDesktopMode: Boolean = false,
    autoConnect: Boolean = true,
    onWebViewCreated: (WebView) -> Unit,
    onProgressChange: (Int) -> Unit,
    onTitleChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onGoogleAuthRejected: (String) -> Unit = {},
    onRendererCrashed: () -> Unit,
    onShowFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean
) {
    val systemDark = isSystemInDarkTheme()
    val isWebDark = when (webTheme) {
        "dark" -> true
        "light" -> false
        else -> when (appThemeMode) {
            "dark" -> true
            "light" -> false
            else -> systemDark
        }
    }

    val currentIsWebDark by rememberUpdatedState(isWebDark)
    val currentIsDesktopMode by rememberUpdatedState(isDesktopMode)
    val currentAutoConnect by rememberUpdatedState(autoConnect)
    val currentOnUrlChange by rememberUpdatedState(onUrlChange)
    val currentOnProgressChange by rememberUpdatedState(onProgressChange)
    val currentOnTitleChange by rememberUpdatedState(onTitleChange)
    val currentOnGoogleAuthRejected by rememberUpdatedState(onGoogleAuthRejected)
    val currentOnShowFileChooser by rememberUpdatedState(onShowFileChooser)

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    fun injectWebTheme(view: WebView, dark: Boolean) {
        val scheme = if (dark) "dark" else "light"
        val script = """
        (function() {
            var dark = $dark;
            var scheme = '$scheme';
            try {
                localStorage.setItem('bcsp', scheme);
                localStorage.setItem('theme', scheme);
                localStorage.setItem('themeMode', scheme);
            } catch(e) {}

            function applyThemeClasses() {
                document.documentElement.style.colorScheme = scheme;
                document.documentElement.setAttribute('data-theme', scheme);
                if (document.body) {
                    document.body.classList.toggle('dark-theme', dark);
                    if (document.body.classList.contains('theme-standalone') ||
                        document.querySelector('.theme-standalone')) {
                        document.body.classList.toggle('dark', dark);
                        document.body.classList.toggle('theme-dark', dark);
                        document.body.classList.toggle('theme-light', !dark);
                    }
                }
            }

            applyThemeClasses();
            if (!document.body) {
                document.addEventListener('DOMContentLoaded', applyThemeClasses, { once: true });
            }
        })();
        """.trimIndent()

        view.evaluateJavascript(script, null)
    }

    fun applyThemeToWebView(view: WebView, dark: Boolean) {
        try {
            // Antigravity Web implements native light/dark themes; disable algorithmic darkening
            // to avoid double-darkened surfaces and extra compositor paint passes.
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(view.settings, false)
            } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                @Suppress("DEPRECATION")
                WebSettingsCompat.setForceDark(view.settings, WebSettingsCompat.FORCE_DARK_OFF)
            }
        } catch (e: Exception) {
            Log.e("WebViewContainer", "Error applying theme: ${e.message}")
        }
        view.setBackgroundColor(
            if (dark) AndroidColor.parseColor("#0C0E14") else AndroidColor.parseColor("#FFFFFF")
        )
    }

    fun applyDesktopMode(view: WebView, desktop: Boolean) {
        applyUserAgentAndMetadata(view, desktop)
        view.settings.apply {
            useWideViewPort = true
            loadWithOverviewMode = desktop
            setSupportZoom(desktop)
            builtInZoomControls = desktop
            displayZoomControls = false
        }
        view.isHorizontalScrollBarEnabled = desktop
    }

    LaunchedEffect(isWebDark) {
        webViewRef?.let { wv ->
            applyThemeToWebView(wv, isWebDark)
            if (isAntigravityHost(wv.url)) {
                injectWebTheme(wv, isWebDark)
            }
        }
    }

    LaunchedEffect(isDesktopMode) {
        webViewRef?.let { wv ->
            applyDesktopMode(wv, isDesktopMode)
            wv.reload()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                webViewRef = this

                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                overScrollMode = View.OVER_SCROLL_NEVER
                scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                isNestedScrollingEnabled = false

                val profileCookieManager = if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
                    try {
                        val profileName = "account_$accountId"
                        val profile = ProfileStore.getInstance().getOrCreateProfile(profileName)
                        WebViewCompat.setProfile(this, profileName)
                        profile.getCookieManager()
                    } catch (e: Exception) {
                        Log.e("WebViewContainer", "Error setting profile: ${e.message}")
                        CookieManager.getInstance()
                    }
                } else {
                    CookieManager.getInstance()
                }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    @Suppress("DEPRECATION")
                    databaseEnabled = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    allowFileAccess = true
                    allowContentAccess = true
                    mediaPlaybackRequiresUserGesture = false
                    setGeolocationEnabled(true)
                    setSupportMultipleWindows(true)
                    javaScriptCanOpenWindowsAutomatically = true
                }

                if (WebViewFeature.isFeatureSupported(WebViewFeature.BACK_FORWARD_CACHE)) {
                    runCatching { WebSettingsCompat.setBackForwardCacheEnabled(settings, true) }
                }

                if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
                    runCatching {
                        @Suppress("DEPRECATION")
                        WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet())
                    }
                }

                if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                    try {
                        WebViewCompat.addDocumentStartJavaScript(
                            this,
                            REMOTE_LAYOUT_POLISH_SCRIPT,
                            setOf("https://antigravity.google.com", "https://antigravity.static.usercontent.goog")
                        )
                    } catch (e: Exception) {
                        Log.e("WebViewContainer", "Error adding layout polish script: ${e.message}")
                    }

                    try {
                        WebViewCompat.addDocumentStartJavaScript(
                            this, EffortPickerBridge.script, EffortPickerBridge.allowedOrigins
                        )
                    } catch (_: Exception) {
                        Toast.makeText(context, "Model effort support is unavailable in this WebView.", Toast.LENGTH_LONG).show()
                    }
                }

                applyThemeToWebView(this, currentIsWebDark)
                applyDesktopMode(this, isDesktopMode)

                profileCookieManager.setAcceptCookie(true)
                profileCookieManager.setAcceptThirdPartyCookies(this, true)
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        currentOnProgressChange(newProgress)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        if (!title.isNullOrEmpty()) currentOnTitleChange(title)
                    }

                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        return currentOnShowFileChooser(filePathCallback, fileChooserParams)
                    }

                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: Message?
                    ): Boolean {
                        if (view == null || resultMsg == null) return false
                        val popupContext = view.context

                        val popupWebView = WebView(popupContext).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            overScrollMode = View.OVER_SCROLL_NEVER
                            if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
                                try {
                                    val profileName = "account_$accountId"
                                    ProfileStore.getInstance().getOrCreateProfile(profileName)
                                    WebViewCompat.setProfile(this, profileName)
                                } catch (e: Exception) {
                                    Log.e("WebViewContainer", "Error setting profile on popup: ${e.message}")
                                }
                            }
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                @Suppress("DEPRECATION")
                                databaseEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = currentIsDesktopMode
                                setSupportZoom(currentIsDesktopMode)
                                builtInZoomControls = currentIsDesktopMode
                                displayZoomControls = false
                            }
                            if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
                                runCatching {
                                    @Suppress("DEPRECATION")
                                    WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet())
                                }
                            }
                            applyUserAgentAndMetadata(this, currentIsDesktopMode)
                            profileCookieManager.setAcceptCookie(true)
                            profileCookieManager.setAcceptThirdPartyCookies(this, true)
                        }

                        val dialogTheme = if (currentIsWebDark) {
                            android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen
                        } else {
                            android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen
                        }
                        val authDialog = android.app.Dialog(popupContext, dialogTheme).apply {
                            setContentView(popupWebView)
                            setOnDismissListener {
                                flushCookiesForWebView(view)
                                popupWebView.destroy()
                            }
                        }

                        popupWebView.webChromeClient = object : WebChromeClient() {
                            override fun onCloseWindow(window: WebView?) {
                                flushCookiesForWebView(view)
                                view.reload()
                                authDialog.dismiss()
                            }
                        }

                        popupWebView.webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(pView: WebView?, request: WebResourceRequest?): Boolean {
                                val uri = request?.url ?: return false
                                val popupUrl = uri.toString()
                                val scheme = uri.scheme ?: ""
                                if (scheme != "http" && scheme != "https") {
                                    return handleNonHttpUri(popupContext, pView, uri, popupUrl)
                                }
                                if (isAntigravityHost(popupUrl)) {
                                    flushCookiesForWebView(view)
                                    view.loadUrl(popupUrl)
                                    authDialog.dismiss()
                                    return true
                                }
                                return false
                            }

                            override fun onPageFinished(pView: WebView?, finishedUrl: String?) {
                                super.onPageFinished(pView, finishedUrl)
                                flushCookiesForWebView(view)
                                if (finishedUrl != null && isAntigravityHost(finishedUrl)) {
                                    view.loadUrl(finishedUrl)
                                    authDialog.dismiss()
                                } else if (finishedUrl != null && isGoogleAuthRejectedUrl(finishedUrl)) {
                                    authDialog.dismiss()
                                    currentOnGoogleAuthRejected(finishedUrl)
                                }
                            }
                        }

                        val transport = resultMsg.obj as? WebView.WebViewTransport
                        transport?.webView = popupWebView
                        resultMsg.sendToTarget()
                        authDialog.show()
                        return true
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        if (!url.isNullOrEmpty()) currentOnUrlChange(url)
                        if (isAntigravityHost(url)) {
                            view?.let { injectWebTheme(it, currentIsWebDark) }
                        }
                    }

                    override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                        if (!finishedUrl.isNullOrEmpty()) currentOnUrlChange(finishedUrl)
                        currentOnProgressChange(100)
                        flushCookiesForWebView(view)
                        view?.let {
                            if (isAntigravityHost(finishedUrl)) injectWebTheme(it, currentIsWebDark)
                        }

                        if (finishedUrl != null && isGoogleAuthRejectedUrl(finishedUrl)) {
                            currentOnGoogleAuthRejected(finishedUrl)
                        } else if (finishedUrl != null && isGoogleAuthUrl(finishedUrl)) {
                            view?.evaluateJavascript(
                                "(function(){var t=(document.body&&document.body.innerText)||'';return /browser or app may not be secure|trình duyệt hoặc ứng dụng này có thể không an toàn|disallowed_useragent/i.test(t);})();"
                            ) { result ->
                                if (result == "true") {
                                    currentOnGoogleAuthRejected(finishedUrl)
                                }
                            }
                        }

                        if (currentAutoConnect && finishedUrl != null &&
                            view?.url == finishedUrl &&
                            Uri.parse(finishedUrl).host == Uri.parse(AccountRepository.DEFAULT_REMOTE_URL).host
                        ) {
                            val jsAutoConnect = """
                            (function() {
                                if (window.__antiAutoConnected) return;
                                function tryConnect() {
                                    if (window.__antiAutoConnected) return false;
                                    var els = document.querySelectorAll('button, a, [role="button"]');
                                    for (var i = 0; i < els.length; i++) {
                                        var el = els[i];
                                        var t = (el.textContent || '').trim().toLowerCase();
                                        var a = (el.getAttribute('aria-label') || '').toLowerCase();
                                        var textIsConnect = /(^|\s)connect(\s|$)/.test(t);
                                        var ariaIsConnect = /(^|\s)connect(\s|$)/.test(a);
                                        if ((textIsConnect || ariaIsConnect) &&
                                            el.offsetParent !== null && !el.disabled &&
                                            el.getAttribute('aria-disabled') !== 'true') {
                                            window.__antiAutoConnected = true;
                                            el.click();
                                            return true;
                                        }
                                    }
                                    return false;
                                }
                                if (tryConnect()) return;
                                var pendingTimer = null;
                                var obs = new MutationObserver(function() {
                                    if (window.__antiAutoConnected) { obs.disconnect(); return; }
                                    if (pendingTimer) return;
                                    pendingTimer = setTimeout(function() {
                                        pendingTimer = null;
                                        if (tryConnect()) obs.disconnect();
                                    }, 250);
                                });
                                if (document.body) obs.observe(document.body, {childList:true, subtree:true});
                                setTimeout(function() {
                                    obs.disconnect();
                                    if (pendingTimer) clearTimeout(pendingTimer);
                                }, 10000);
                            })();
                            """.trimIndent()
                            view?.evaluateJavascript(jsAutoConnect, null)
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            Toast.makeText(context, "Unable to load the remote page. Try Reload.", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?
                    ) {
                        if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 200) >= 400) {
                            Toast.makeText(context, "Remote page returned an error. Try Reload.", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onRenderProcessGone(
                        view: WebView?,
                        detail: RenderProcessGoneDetail?
                    ): Boolean {
                        flushCookiesForWebView(view)
                        view?.stopLoading()
                        view?.destroy()
                        Toast.makeText(context, "WebView restarted after a renderer failure.", Toast.LENGTH_LONG).show()
                        onRendererCrashed()
                        return true
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val targetUri = request?.url ?: return false
                        val targetUrl = targetUri.toString()
                        val scheme = targetUri.scheme ?: ""

                        if (scheme != "http" && scheme != "https") {
                            return handleNonHttpUri(context, view, targetUri, targetUrl)
                        }

                        return false
                    }
                }

                onWebViewCreated(this)
                loadUrl(url)
            }
        },
        update = { /* Recreated on key(accountId) */ }
    )

    DisposableEffect(webViewRef) {
        onDispose {
            flushCookiesForWebView(webViewRef)
        }
    }
}
