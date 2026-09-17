package com.hkm.antigravity.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    modifier: Modifier = Modifier,
    url: String,
    onWebViewCreated: (WebView) -> Unit,
    onProgressChange: (Int) -> Unit,
    onTitleChange: (String) -> Unit,
    onUrlChange: (String) -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
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

                    // Remove Android WebView identifiers from UA to allow Google OAuth login
                    val defaultUA = userAgentString
                    userAgentString = defaultUA
                        .replace("; wv", "")
                        .replace("Version/4.0 ", "")
                }

                // Cookie manager setup
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
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        if (!url.isNullOrEmpty()) {
                            onUrlChange(url)
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (!url.isNullOrEmpty()) {
                            onUrlChange(url)
                        }
                        onProgressChange(100)
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return false // Load all inside webview
                    }
                }

                loadUrl(url)
                onWebViewCreated(this)
            }
        },
        update = { webView ->
            // If url changed from external switch, load it
            val current = webView.url ?: ""
            if (url.isNotEmpty() && !current.startsWith(url) && !url.startsWith(current)) {
                webView.loadUrl(url)
            }
        }
    )
}
