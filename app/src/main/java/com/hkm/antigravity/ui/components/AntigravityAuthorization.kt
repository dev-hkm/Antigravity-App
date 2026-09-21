package com.hkm.antigravity.ui.components

import android.net.Uri

/**
 * Keeps the short-lived Google access token in the URL fragment only. Fragments
 * are not transmitted to the server, and the Antigravity web client consumes the
 * token before replacing the visible history entry. Never persist this value.
 */
internal object AntigravityAuthorization {
    fun initialUrl(remoteUrl: String, accessToken: String?): String {
        if (accessToken.isNullOrBlank() || !isAntigravityHost(remoteUrl)) return remoteUrl
        val cleanUrl = remoteUrl.substringBefore('#')
        return "$cleanUrl#access_token=${Uri.encode(accessToken)}&state=${Uri.encode("/")}" 
    }

    fun callbackUrl(rawUrl: String): String = rawUrl.substringBefore('#')
}
