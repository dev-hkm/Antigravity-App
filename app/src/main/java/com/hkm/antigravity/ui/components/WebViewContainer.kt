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

    fun injectWebOptimizations(view: WebView, dark: Boolean) {
        val bg = if (dark) "#0C0E14" else "#FFFFFF"
        val surface = if (dark) "#131314" else "#FFFFFF"
        val onSurface = if (dark) "#E3E3E3" else "#1F1F1F"
        val surfaceContainer = if (dark) "#1E1F20" else "#F0F4F9"
        val colorScheme = if (dark) "dark" else "light"

        val optScript = """
        (function() {
            var dark = $dark;
            var scheme = '$colorScheme';

            // 1. Enforce root color-scheme and CSS variables
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

            var styleId = 'antigravity-native-enhancements';
            var style = document.getElementById(styleId);
            if (!style) {
                style = document.createElement('style');
                style.id = styleId;
                document.head.appendChild(style);
            }

            style.innerHTML = `
                :root, html, body {
                    color-scheme: $colorScheme !important;
                    --mat-sys-background: $bg !important;
                    --mat-sys-surface: $surface !important;
                    --mat-sys-on-surface: $onSurface !important;
                    --mat-sys-on-background: $onSurface !important;
                    --mat-sys-surface-container: $surfaceContainer !important;
                    --gm3-sys-color-background: $bg !important;
                    --gm3-sys-color-surface: $surface !important;
                    --gm3-sys-color-on-surface: $onSurface !important;
                    --og-bar-background: $surface !important;
                    --og-bar-color: $onSurface !important;
                    background-color: $bg !important;
                    color: $onSurface !important;
                }

                /* 2. Enlarge Add Context (+) button */
                button[aria-label*="Context" i], button[aria-label*="Add" i],
                button[title*="Context" i], button[title*="Add" i],
                [data-tooltip*="Context" i], [data-tooltip*="Add" i] {
                    min-width: 44px !important;
                    min-height: 44px !important;
                    padding: 8px !important;
                    transform: scale(1.18) !important;
                    transform-origin: center center !important;
                }

                /* 3. Model selector comfort size */
                button[aria-haspopup="true"], [role="combobox"] {
                    min-height: 38px !important;
                    padding-left: 12px !important;
                    padding-right: 12px !important;
                }

                /* 4. Keep popups and effort menus on top */
                [role="menu"], [role="listbox"], [role="dialog"], [data-radix-popper-content-wrapper] {
                    z-index: 999999 !important;
                }
            `;

            // 5. Synthesize mouse hover for Model Picker on mobile so Thinking Effort submenu expands
            if (!window.__antiTouchHoverSetup) {
                window.__antiTouchHoverSetup = true;
                document.addEventListener('touchstart', function(e) {
                    var item = e.target.closest('[role="menuitem"], [role="option"], li');
                    if (item && item.closest('[role="menu"], [role="listbox"], [data-radix-popper-content-wrapper]')) {
                        var enterEvent = new MouseEvent('mouseenter', { bubbles: true, cancelable: true });
                        var overEvent = new MouseEvent('mouseover', { bubbles: true, cancelable: true });
                        item.dispatchEvent(enterEvent);
                        item.dispatchEvent(overEvent);
                    }
                }, { passive: true, capture: true });
            }
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
