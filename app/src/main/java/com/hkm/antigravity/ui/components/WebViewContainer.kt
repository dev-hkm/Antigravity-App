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
    themeMode: String = "system",
    isDesktopMode: Boolean = false,
    autoConnect: Boolean = true,
    onWebViewCreated: (WebView) -> Unit,
    onProgressChange: (Int) -> Unit,
    onTitleChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onShowFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Dynamic web theme synchronization
    fun applyThemeToWeb(view: WebView, dark: Boolean) {
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
            Log.e("WebViewContainer", "Error applying dark mode settings: ${e.message}")
        }

        // Web background color to prevent flash
        view.setBackgroundColor(if (dark) AndroidColor.parseColor("#0C0E14") else AndroidColor.parseColor("#FFFFFF"))

        // Inject script to enforce web classes and color-scheme
        val jsTheme = """
        (function() {
            var dark = $dark;
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
        })();
        """.trimIndent()
        view.evaluateJavascript(jsTheme, null)
    }

    // Apply Desktop / Mobile User-Agent
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

    LaunchedEffect(isDark, webViewRef) {
        webViewRef?.let { applyThemeToWeb(it, isDark) }
    }

    LaunchedEffect(isDesktopMode, webViewRef) {
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

                // Assign isolated profile if supported on device (MUST be called before any loadUrl)
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

                applyThemeToWeb(this, isDark)
                applyDesktopMode(this, isDesktopMode)

                // Cookie manager configuration
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
                        view?.let { applyThemeToWeb(it, isDark) }
                    }

                    override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                        if (!finishedUrl.isNullOrEmpty()) {
                            onUrlChange(finishedUrl)
                        }
                        onProgressChange(100)
                        CookieManager.getInstance().flush()

                        view?.let { applyThemeToWeb(it, isDark) }

                        // Auto-connect injection when on instances list
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
                        return false // Keep browsing inside WebView
                    }
                }

                loadUrl(url)
                onWebViewCreated(this)
            }
        },
        update = {
            // No-op for profile; key(accountId) recreates the view cleanly
        }
    )
}
