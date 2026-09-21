package com.hkm.antigravity.ui.components

import android.net.Uri

/**
 * Google authentication must never be rendered inside the app-controlled WebView.
 * Keep this deliberately narrow: only a top-level Google Accounts navigation is
 * treated as an auth hand-off; ordinary Google resources remain untouched.
 */
internal object AuthUrlPolicy {
    private const val GOOGLE_ACCOUNTS_HOST = "accounts.google.com"

    fun isGoogleAuthUrl(rawUrl: String?): Boolean {
        if (rawUrl.isNullOrBlank()) return false

        val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return false
        return uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals(GOOGLE_ACCOUNTS_HOST, ignoreCase = true)
    }
}
