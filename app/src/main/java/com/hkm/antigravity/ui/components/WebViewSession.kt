package com.hkm.antigravity.ui.components

import android.webkit.CookieManager
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/** Flushes the cookie jar that actually belongs to this WebView's profile. */
internal fun flushCookiesForWebView(webView: WebView?) {
    if (webView == null) return

    runCatching {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
            WebViewCompat.getProfile(webView).getCookieManager().flush()
        } else {
            CookieManager.getInstance().flush()
        }
    }.onFailure {
        // Keep the legacy/default-profile fallback for older WebView providers.
        runCatching { CookieManager.getInstance().flush() }
    }
}
