package com.hkm.antigravity.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.util.Log
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

private const val DESKTOP_UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"

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
            wv.reload()
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
            val ua = s.userAgentString
            s.userAgentString = ua.replace("; wv", "").replace("Version/4.0 ", "")
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

                if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
                    try {
                        val profileName = "account_$accountId"
                        ProfileStore.getInstance().getOrCreateProfile(profileName)
                        WebViewCompat.setProfile(this, profileName)
                    } catch (e: Exception) {
                        Log.e("WebViewContainer", "Error setting profile: ${e.message}")
                    }
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
                    WebViewCompat.addDocumentStartJavaScript(
                        this, EffortPickerBridge.script, EffortPickerBridge.allowedOrigins
                    )
                } else {
                    android.widget.Toast.makeText(
                        context,
                        "Hãy cập nhật Android System WebView để chọn model và effort đúng cách.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }

                applyThemeToWebView(this, currentIsWebDark)
                applyDesktopMode(this, isDesktopMode)

                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

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
                        if (!url.isNullOrEmpty()) currentOnUrlChange(url)
                        view?.let { injectWebTheme(it, currentIsWebDark) }
                    }

                    override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                        if (!finishedUrl.isNullOrEmpty()) currentOnUrlChange(finishedUrl)
                        currentOnProgressChange(100)
                        CookieManager.getInstance().flush()
                        view?.let { injectWebTheme(it, currentIsWebDark) }

                        if (currentAutoConnect && finishedUrl != null &&
                            finishedUrl.contains("antigravity.google.com")
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
                                        if ((t === 'connect' || a === 'connect' ||
                                             t.indexOf('connect') !== -1 || a.indexOf('connect') !== -1) &&
                                            el.offsetParent !== null && !el.disabled) {
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
                            evaluateJavascript(jsAutoConnect, null)
                        }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean = false
                }

                loadUrl(url)
                onWebViewCreated(this)
            }
        },
        update = { /* Recreated on key(accountId) */ }
    )
}
