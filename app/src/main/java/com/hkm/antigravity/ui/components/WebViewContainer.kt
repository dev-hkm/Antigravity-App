package com.hkm.antigravity.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    modifier: Modifier = Modifier,
    accountId: String,
    url: String,
    autoConnect: Boolean = true,
    onWebViewCreated: (WebView) -> Unit,
    onProgressChange: (Int) -> Unit,
    onTitleChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onShowFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
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

                    // Modern desktop/mobile clean UA to allow seamless Google authentication
                    val defaultUA = userAgentString
                    userAgentString = defaultUA
                        .replace("; wv", "")
                        .replace("Version/4.0 ", "")
                }

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
                    }

                    override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                        if (!finishedUrl.isNullOrEmpty()) {
                            onUrlChange(finishedUrl)
                        }
                        onProgressChange(100)
                        CookieManager.getInstance().flush()

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
