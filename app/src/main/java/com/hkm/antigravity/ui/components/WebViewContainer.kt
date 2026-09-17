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
    webTheme: String = "dark", // "dark", "light", "follow_app"
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

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Safe, targeted CSS optimizations for mobile
    fun injectWebOptimizations(view: WebView, dark: Boolean) {
        val bgHex = if (dark) "#0C0E14" else "#FFFFFF"
        val textHex = if (dark) "#F1F5F9" else "#0F172A"

        val optScript = """
        (function() {
            var dark = $isWebDark;
            document.documentElement.classList.toggle('dark', dark);
            document.documentElement.classList.toggle('theme-dark', dark);
            document.documentElement.classList.toggle('light', !dark);
            document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light');
            document.documentElement.style.colorScheme = dark ? 'dark' : 'light';
            try {
                localStorage.setItem('theme', dark ? 'dark' : 'light');
                localStorage.setItem('antigravity_theme', dark ? 'dark' : 'light');
                localStorage.setItem('themeMode', dark ? 'dark' : 'light');
            } catch(e) {}

            var styleId = 'antigravity-native-enhancements';
            var style = document.getElementById(styleId);
            if (!style) {
                style = document.createElement('style');
                style.id = styleId;
                document.head.appendChild(style);
            }
            style.innerHTML = `
                body, html {
                    background-color: $bgHex !important;
                    color: $textHex !important;
                    -webkit-font-smoothing: antialiased;
                }
                /* Enlarge Add Context button so it is effortless to tap */
                button[aria-label*="context" i], button[title*="context" i], button[aria-label*="Add" i] {
                    min-width: 38px !important;
                    min-height: 38px !important;
                    display: inline-flex !important;
                    align-items: center !important;
                    justify-content: center !important;
                }
                /* Enlarge Model Selector pill comfortably without interfering with dropdown */
                button[aria-haspopup="true"], [role="combobox"] {
                    min-height: 36px !important;
                    padding-left: 10px !important;
                    padding-right: 10px !important;
                }
                /* Keep model picker and thinking effort menus at high z-index and unblocked */
                [role="menu"], [role="listbox"], [role="dialog"], [data-radix-popper-content-wrapper] {
                    z-index: 999999 !important;
                }
                * {
                    -webkit-overflow-scrolling: touch;
                }
            `;
        })();
        """.trimIndent()

        view.evaluateJavascript(optScript, null)
    }

    fun applyThemeSettings(view: WebView, dark: Boolean) {
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
            Log.e("WebViewContainer", "Error applying theme settings: ${e.message}")
        }

        view.setBackgroundColor(if (dark) AndroidColor.parseColor("#0C0E14") else AndroidColor.parseColor("#FFFFFF"))
        injectWebOptimizations(view, dark)
    }

    LaunchedEffect(isWebDark) {
        webViewRef?.let {
            applyThemeSettings(it, isWebDark)
            it.reload()
        }
    }

    fun applyDesktopMode(view: WebView, desktop: Boolean) {
        val settings = view.settings
        if (desktop) {
            settings.userAgentString = DESKTOP_UA
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
        } else {
            val defaultUA = settings.userAgentString
            settings.userAgentString = defaultUA
                .replace("; wv", "")
                .replace("Version/4.0 ", "")
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
        }
    }

    LaunchedEffect(isDesktopMode) {
        webViewRef?.let {
            applyDesktopMode(it, isDesktopMode)
            it.reload()
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

                applyThemeSettings(this, isWebDark)
                applyDesktopMode(this, isDesktopMode)

                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgressChange(newProgress)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        if (!title.isNullOrEmpty()) {
                            onTitleChange(title)
                        }
                    }

                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        return onShowFileChooser(filePathCallback, fileChooserParams)
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        if (!url.isNullOrEmpty()) {
                            onUrlChange(url)
                        }
                        view?.let { injectWebOptimizations(it, isWebDark) }
                    }

                    override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                        if (!finishedUrl.isNullOrEmpty()) {
                            onUrlChange(finishedUrl)
                        }
                        onProgressChange(100)
                        CookieManager.getInstance().flush()

                        view?.let { injectWebOptimizations(it, isWebDark) }

                        if (autoConnect && finishedUrl != null && finishedUrl.contains("antigravity.google.com")) {
                            val jsAutoConnect = """
                            (function() {
                                if (window.__antiAutoConnected) return;
                                function tryClickConnect() {
                                    if (window.__antiAutoConnected) return false;
                                    var candidates = document.querySelectorAll('button, a, [role="button"]');
                                    for (var i = 0; i < candidates.length; i++) {
                                        var el = candidates[i];
                                        var text = (el.textContent || '').trim().toLowerCase();
                                        var aria = (el.getAttribute('aria-label') || '').toLowerCase();
                                        if (text === 'connect' || aria === 'connect' || text.indexOf('connect') !== -1 || aria.indexOf('connect') !== -1) {
                                            if (el.offsetParent !== null && !el.disabled && !el.getAttribute('aria-disabled')) {
                                                window.__antiAutoConnected = true;
                                                el.click();
                                                return true;
                                            }
                                        }
                                    }
                                    return false;
                                }
                                if (tryClickConnect()) return;
                                var observer = new MutationObserver(function(mutations, obs) {
                                    if (tryClickConnect()) {
                                        obs.disconnect();
                                    }
                                });
                                if (document.body) {
                                    observer.observe(document.body, { childList: true, subtree: true });
                                }
                                setTimeout(function() {
                                    observer.disconnect();
                                }, 15000);
                            })();
                            """.trimIndent()
                            evaluateJavascript(jsAutoConnect, null)
                        }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return false
                    }
                }

                loadUrl(url)
                onWebViewCreated(this)
            }
        },
        update = {
            // Recreated on key(accountId)
        }
    )
}
