package com.hkm.antigravity.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

internal object SecureBrowserLauncher {
    fun open(context: Context, rawUrl: String): Boolean {
        val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false

        return try {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .build()
                .launchUrl(context, uri)
            true
        } catch (_: Exception) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
