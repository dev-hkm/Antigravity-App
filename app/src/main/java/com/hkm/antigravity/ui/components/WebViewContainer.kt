package com.hkm.antigravity.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
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

private data class ChromiumVersion(
    val full: String,
    val major: String
)

private fun extractChromiumVersion(context: Context): ChromiumVersion {
    val defaultUa = runCatching { WebSettings.getDefaultUserAgent(context) }.getOrNull().orEmpty()
    val match = Regex("""Chrome/(\d+\.\d+\.\d+\.\d+)""").find(defaultUa)
    val full = match?.groupValues?.getOrNull(1) ?: "136.0.0.0"
    val major = full.substringBefore('.')
    return ChromiumVersion(full = full, major = major)
}

private fun buildDesktopUserAgent(version: ChromiumVersion): String =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${version.full} Safari/537.36"

private fun buildMobileUserAgent(version: ChromiumVersion): String =
    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${version.full} Mobile Safari/537.36"

private fun applyUserAgentAndMetadata(
    view: WebView,
    useDesktop: Boolean,
    version: ChromiumVersion
) {
    val settings = view.settings
    settings.userAgentString = if (useDesktop) {
        buildDesktopUserAgent(version)
    } else {
        buildMobileUserAgent(version)
    }

    if (WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA)) {
        try {
            val brands = listOf(
                UserAgentMetadata.BrandVersion.Builder()
                    .setBrand("Not(A:Brand")
                    .setMajorVersion("99")
                    .setFullVersion("99.0.0.0")
                    .build(),
                UserAgentMetadata.BrandVersion.Builder()
                    .setBrand("Chromium")
                    .setMajorVersion(version.major)
                    .setFullVersion(version.full)
                    .build(),
                UserAgentMetadata.BrandVersion.Builder()
                    .setBrand("Google Chrome")
                    .setMajorVersion(version.major)
                    .setFullVersion(version.full)
                    .build()
            )

            val builder = UserAgentMetadata.Builder()
                .setBrandVersionList(brands)
                .setFullVersion(version.full)

            if (useDesktop) {
                builder
                    .setPlatform("Windows")
                    .setPlatformVersion("10.0.0")
                    .setArchitecture("x86")
                    .setBitness(64)
                    .setMobile(false)
                    .setModel("")
                    .setFormFactors(listOf(UserAgentMetadata.FORM_FACTOR_DESKTOP))
            } else {
                builder
                    .setPlatform("Android")
                    .setPlatformVersion("10.0.0")
                    .setArchitecture("")
                    .setBitness(0)
                    .setMobile(true)
                    .setModel(Build.MODEL ?: "")
                    .setFormFactors(listOf(UserAgentMetadata.FORM_FACTOR_MOBILE))
            }

            WebSettingsCompat.setUserAgentMetadata(settings, builder.build())
        } catch (e: Exception) {
            Log.e("WebViewContainer", "Error setting UserAgentMetadata: ${e.message}")
        }
    }
}

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
    val currentOnShowFileChooser by rememberUpdatedState(onShowFileChooser)

    val context = androidx.compose.ui.platform.LocalContext.current
    val chromiumVersion = remember(context) { extractChromiumVersion(context) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    fun injectWebTheme(view: WebView, dark: Boolean) {
        val scheme = if (dark) "dark" else "light"
        val script = """
        (function() {
            var dark = $dark;
            var scheme = '$scheme';
            try {
                document.documentElement.style.colorScheme = scheme;
                if (document.body) {
                    document.body.classList.toggle('dark-theme', dark);
                }
                localStorage.setItem('bcsp', scheme);
            } catch(e) {}
            var oldStyle = document.getElementById('antigravity-native-theme');
            if (oldStyle) oldStyle.remove();
        })();
        """.trimIndent()

        view.evaluateJavascript(script, null)
    }

    fun applyThemeToWebView(view: WebView, dark: Boolean) {
        try {
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

    LaunchedEffect(isWebDark) {
        webViewRef?.let { wv ->
            applyThemeToWebView(wv, isWebDark)
            injectWebTheme(wv, isWebDark)
        }
    }

    fun applyDesktopMode(view: WebView, desktop: Boolean) {
        val s = view.settings
        applyUserAgentAndMetadata(view, desktop, chromiumVersion)
        if (desktop) {
            s.useWideViewPort = true
            s.loadWithOverviewMode = true
            s.setSupportZoom(true)
            s.builtInZoomControls = true
            s.displayZoomControls = false
        } else {
            s.useWideViewPort = true
            s.loadWithOverviewMode = false
            s.setSupportZoom(false)
            s.builtInZoomControls = false
            s.displayZoomControls = false
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
        factory = { viewContext ->
            WebView(viewContext).apply {
                webViewRef = this
                overScrollMode = View.OVER_SCROLL_NEVER

                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

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
                    useWideViewPort = true
                    displayZoomControls = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                    allowFileAccess = true
                    allowContentAccess = true
                    mediaPlaybackRequiresUserGesture = false
                    setGeolocationEnabled(true)
                    setSupportMultipleWindows(true)
                    javaScriptCanOpenWindowsAutomatically = true
                }

                // Register effort picker bridge for remote control (iframe and direct /remote route)
                if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                    try {
                        WebViewCompat.addDocumentStartJavaScript(
                            this, EffortPickerBridge.script, EffortPickerBridge.allowedOrigins
                        )
                    } catch (_: Exception) {
                        Toast.makeText(viewContext, "Model effort support is unavailable in this WebView.", Toast.LENGTH_LONG).show()
                    }
                }

                applyThemeToWebView(this, currentIsWebDark)
                applyDesktopMode(this, currentIsDesktopMode)

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
                            overScrollMode = View.OVER_SCROLL_NEVER
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
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
                            applyUserAgentAndMetadata(this, currentIsDesktopMode, chromiumVersion)
                            profileCookieManager.setAcceptCookie(true)
                            profileCookieManager.setAcceptThirdPartyCookies(this, true)
                        }

                        val authDialog = android.app.Dialog(popupContext, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen).apply {
                            setContentView(popupWebView)
                            setOnDismissListener {
                                flushCookiesForWebView(view)
                                popupWebView.destroy()
                            }
                        }

                        popupWebView.webChromeClient = object : WebChromeClient() {
                            override fun onCloseWindow(window: WebView?) {
                                flushCookiesForWebView(view)
                                applyUserAgentAndMetadata(view, currentIsDesktopMode, chromiumVersion)
                                view.reload()
                                authDialog.dismiss()
                            }
                        }

                        popupWebView.webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(pView: WebView?, request: WebResourceRequest?): Boolean {
                                val popupUrl = request?.url?.toString() ?: return false
                                val uri = request.url
                                val scheme = uri.scheme ?: ""
                                if (scheme != "http" && scheme != "https") {
                                    runCatching {
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        popupContext.startActivity(intent)
                                    }
                                    return true
                                }
                                if (isAntigravityHost(popupUrl)) {
                                    flushCookiesForWebView(view)
                                    applyUserAgentAndMetadata(view, currentIsDesktopMode, chromiumVersion)
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
                                    applyUserAgentAndMetadata(view, currentIsDesktopMode, chromiumVersion)
                                    view.loadUrl(finishedUrl)
                                    authDialog.dismiss()
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
                                var pendingTimer = 0;
                                var obs = new MutationObserver(function(mutations) {
                                    if (pendingTimer) return;
                                    var hasAdded = false;
                                    for (var i = 0; i < mutations.length; i++) {
                                        if (mutations[i].addedNodes && mutations[i].addedNodes.length > 0) {
                                            hasAdded = true;
                                            break;
                                        }
                                    }
                                    if (!hasAdded) return;
                                    pendingTimer = setTimeout(function() {
                                        pendingTimer = 0;
                                        if (tryConnect()) obs.disconnect();
                                    }, 250);
                                });
                                if (document.body) obs.observe(document.body, {childList:true, subtree:true});
                                setTimeout(function() {
                                    if (pendingTimer) clearTimeout(pendingTimer);
                                    obs.disconnect();
                                }, 15000);
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
                            Toast.makeText(viewContext, "Unable to load the remote page. Try Reload.", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?
                    ) {
                        if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 200) >= 400) {
                            Toast.makeText(viewContext, "Remote page returned an error. Try Reload.", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onRenderProcessGone(
                        view: WebView?,
                        detail: RenderProcessGoneDetail?
                    ): Boolean {
                        flushCookiesForWebView(view)
                        view?.stopLoading()
                        view?.destroy()
                        Toast.makeText(viewContext, "WebView restarted after a renderer failure.", Toast.LENGTH_LONG).show()
                        onRendererCrashed()
                        return true
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val targetUri = request?.url ?: return false
                        val scheme = targetUri.scheme ?: ""

                        if (scheme != "http" && scheme != "https") {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, targetUri)
                                viewContext.startActivity(intent)
                                return true
                            } catch (_: Exception) {
                                return true
                            }
                        }

                        // Keep all HTTP/HTTPS navigation (including Google Accounts login) inside WebView
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
