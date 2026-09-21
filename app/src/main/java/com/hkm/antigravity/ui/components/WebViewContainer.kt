package com.hkm.antigravity.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
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
    initialAccessToken: String? = null,
    onWebViewCreated: (WebView) -> Unit,
    onProgressChange: (Int) -> Unit,
    onTitleChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onGoogleAuthRequired: (String) -> Unit,
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

    fun applyDesktopMode(view: WebView, desktop: Boolean, defaultUserAgent: String) {
        val s = view.settings
        if (desktop) {
            s.userAgentString = DESKTOP_UA
            s.useWideViewPort = true
            s.loadWithOverviewMode = true
            s.setSupportZoom(true)
            s.builtInZoomControls = true
            s.displayZoomControls = false
        } else {
            // Keep the genuine WebView identity. Removing `; wv` only makes the
            // fingerprint inconsistent and cannot make Google OAuth supported.
            s.userAgentString = defaultUserAgent
            s.useWideViewPort = true
            s.loadWithOverviewMode = true
        }
    }

    LaunchedEffect(isDesktopMode) {
        webViewRef?.let { wv ->
            applyDesktopMode(wv, isDesktopMode, WebSettings.getDefaultUserAgent(wv.context))
            wv.reload()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                webViewRef = this
                val defaultUserAgent = WebSettings.getDefaultUserAgent(context)

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
                }

                // The model picker lives in a cross-origin iframe. evaluateJavascript
                // targets only the host page; register before navigation for every matching frame.
                if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                    try {
                        WebViewCompat.addDocumentStartJavaScript(
                            this, EffortPickerBridge.script, EffortPickerBridge.allowedOrigins
                        )
                    } catch (_: Exception) {
                        Toast.makeText(context, "Model effort support is unavailable in this WebView.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    android.widget.Toast.makeText(
                        context,
                        "Hãy cập nhật Android System WebView để chọn model và effort đúng cách.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }

                applyThemeToWebView(this, currentIsWebDark)
                applyDesktopMode(this, isDesktopMode, defaultUserAgent)

                profileCookieManager.setAcceptCookie(true)
                profileCookieManager.setAcceptThirdPartyCookies(this, true)

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
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        if (!url.isNullOrEmpty()) currentOnUrlChange(AntigravityAuthorization.callbackUrl(url))
                        if (AuthUrlPolicy.isGoogleAuthUrl(url)) {
                            view?.stopLoading()
                            onGoogleAuthRequired(url!!)
                            return
                        }
                        view?.let {
                            if (isAntigravityHost(url)) injectWebTheme(it, currentIsWebDark)
                        }
                    }

                    override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                        if (!finishedUrl.isNullOrEmpty()) currentOnUrlChange(AntigravityAuthorization.callbackUrl(finishedUrl))
                        currentOnProgressChange(100)
                        flushCookiesForWebView(view)
                        if (AuthUrlPolicy.isGoogleAuthUrl(finishedUrl)) {
                            onGoogleAuthRequired(finishedUrl!!)
                            return
                        }
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
                        if (request?.isForMainFrame == true &&
                            AuthUrlPolicy.isGoogleAuthUrl(request.url.toString())
                        ) {
                            onGoogleAuthRequired(request.url.toString())
                            return true
                        }
                        return false
                    }
                }

                onWebViewCreated(this)
                loadUrl(AntigravityAuthorization.initialUrl(url, initialAccessToken))
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
