package com.hkm.antigravity.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Message
import android.util.Log
import android.widget.Toast
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.ProfileStore
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.hkm.antigravity.data.repository.AccountRepository

private const val DESKTOP_UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"

internal fun isAntigravityHost(rawUrl: String?): Boolean {
    val host = runCatching { Uri.parse(rawUrl).host?.lowercase() }.getOrNull() ?: return false
    return host == "antigravity.google.com" || host.endsWith(".antigravity.google.com")
}

private fun getCleanMobileUserAgent(context: Context): String {
    val defaultUa = WebSettings.getDefaultUserAgent(context)
    return defaultUa
        .replace(Regex(";\\s*wv"), "")
        .replace(Regex("Version/\\d+\\.\\d+\\s*"), "")
        .replace("; U;", ";")
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
    val currentAutoConnect by rememberUpdatedState(autoConnect)
    val currentOnUrlChange by rememberUpdatedState(onUrlChange)
    val currentOnProgressChange by rememberUpdatedState(onProgressChange)
    val currentOnTitleChange by rememberUpdatedState(onTitleChange)
    val currentOnShowFileChooser by rememberUpdatedState(onShowFileChooser)

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    fun injectWebTheme(view: WebView, dark: Boolean) {
        val bg = if (dark) "#0C0E14" else "#FFFFFF"
        val surface = if (dark) "#131314" else "#FFFFFF"
        val onSurface = if (dark) "#E3E3E3" else "#1F1F1F"
        val surfaceContainer = if (dark) "#1E1F20" else "#F0F4F9"
        val scheme = if (dark) "dark" else "light"

        val script = """
        (function() {
            var dark = $dark;
            var scheme = '$scheme';

            document.documentElement.style.setProperty('color-scheme', scheme, 'important');
            document.documentElement.setAttribute('data-theme', scheme);
            document.documentElement.classList.toggle('dark', dark);
            document.documentElement.classList.toggle('light', !dark);
            if (document.body) {
                document.body.style.setProperty('color-scheme', scheme, 'important');
            }

            try {
                localStorage.setItem('theme', scheme);
                localStorage.setItem('antigravity_theme', scheme);
                localStorage.setItem('themeMode', scheme);
            } catch(e) {}

            var styleId = 'antigravity-native-theme';
            var style = document.getElementById(styleId);
            if (!style) {
                style = document.createElement('style');
                style.id = styleId;
                (document.head || document.documentElement).appendChild(style);
            }
            style.innerHTML = [
                ':root, html, body {',
                '  color-scheme: ' + scheme + ' !important;',
                '  --mat-sys-background: $bg !important;',
                '  --mat-sys-surface: $surface !important;',
                '  --mat-sys-on-surface: $onSurface !important;',
                '  --mat-sys-on-background: $onSurface !important;',
                '  --mat-sys-surface-container: $surfaceContainer !important;',
                '  --gm3-sys-color-background: $bg !important;',
                '  --gm3-sys-color-surface: $surface !important;',
                '  --gm3-sys-color-on-surface: $onSurface !important;',
                '  background-color: $bg !important;',
                '  color: $onSurface !important;',
                '}',
                'button[aria-label*="Context" i], button[aria-label*="Add" i],',
                'button[title*="Context" i], button[title*="Add" i] {',
                '  min-width: 44px !important; min-height: 44px !important;',
                '  transform: scale(1.15) !important; transform-origin: center !important;',
                '}',
                'button[aria-haspopup="true"], [role="combobox"] {',
                '  min-height: 38px !important;',
                '  padding-left: 12px !important; padding-right: 12px !important;',
                '}',
                '[role="menu"], [role="listbox"], [role="dialog"], [data-radix-popper-content-wrapper] {',
                '  z-index: 999999 !important;',
                '}'
            ].join('\n');

        })();
        """.trimIndent()

        view.evaluateJavascript(script, null)
    }

    fun applyThemeToWebView(view: WebView, dark: Boolean) {
        try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(view.settings, dark)
            } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                @Suppress("DEPRECATION")
                WebSettingsCompat.setForceDark(
                    view.settings,
                    if (dark) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
                )
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
        if (desktop) {
            s.userAgentString = DESKTOP_UA
            s.useWideViewPort = true
            s.loadWithOverviewMode = true
            s.setSupportZoom(true)
            s.builtInZoomControls = true
            s.displayZoomControls = false
        } else {
            s.userAgentString = getCleanMobileUserAgent(view.context)
            s.useWideViewPort = true
            s.loadWithOverviewMode = true
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
                    loadWithOverviewMode = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                    allowFileAccess = true
                    allowContentAccess = true
                    mediaPlaybackRequiresUserGesture = false
                    setGeolocationEnabled(true)
                    setSupportMultipleWindows(true)
                    javaScriptCanOpenWindowsAutomatically = true
                }

                // Register effort picker bridge for remote control cross-origin iframe
                if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
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
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                userAgentString = view.settings.userAgentString
                            }
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
                        view?.let {
                            if (isAntigravityHost(url)) injectWebTheme(it, currentIsWebDark)
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
                                var obs = new MutationObserver(function(m, o) {
                                    if (tryConnect()) o.disconnect();
                                });
                                if (document.body) obs.observe(document.body, {childList:true, subtree:true});
                                setTimeout(function() { obs.disconnect(); }, 15000);
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
                        val scheme = targetUri.scheme ?: ""

                        if (scheme != "http" && scheme != "https") {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, targetUri)
                                context.startActivity(intent)
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
