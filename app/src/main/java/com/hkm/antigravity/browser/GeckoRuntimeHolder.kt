package com.hkm.antigravity.browser

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.StorageController
import org.mozilla.geckoview.WebExtension

/**
 * Manages the process-wide [GeckoRuntime] instance, built-in WebExtension bridge,
 * and shared storage/color-scheme controls across account sessions.
 */
object GeckoRuntimeHolder {
    private const val TAG = "GeckoRuntimeHolder"
    private const val EXTENSION_URI = "resource://android/assets/antigravity_ext/"
    private const val EXTENSION_ID = "antigravity@hkm.local"

    @Volatile
    private var runtime: GeckoRuntime? = null

    @Volatile
    private var builtInExtension: WebExtension? = null

    private val pendingExtensionCallbacks = mutableListOf<(WebExtension) -> Unit>()
    private var activityLauncher: ((PendingIntent) -> GeckoResult<Intent>?)? = null

    fun getOrCreate(context: Context, initialDark: Boolean): GeckoRuntime {
        runtime?.let { existing ->
            updatePreferredColorScheme(initialDark)
            return existing
        }

        val appContext = context.applicationContext
        val contentBlocking = ContentBlocking.Settings.Builder()
            .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.NONE)
            .antiTracking(ContentBlocking.AntiTracking.NONE)
            .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_ALL)
            .strictSocialTrackingProtection(false)
            .build()

        val settings = GeckoRuntimeSettings.Builder()
            .javaScriptEnabled(true)
            .webFontsEnabled(true)
            .webManifest(true)
            .loginAutofillEnabled(true)
            .inputAutoZoomEnabled(false)
            .contentBlocking(contentBlocking)
            .preferredColorScheme(
                if (initialDark) GeckoRuntimeSettings.COLOR_SCHEME_DARK
                else GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
            )
            .build()

        val created = GeckoRuntime.create(appContext, settings)
        created.activityDelegate = GeckoRuntime.ActivityDelegate { pendingIntent ->
            activityLauncher?.invoke(pendingIntent)
                ?: GeckoResult.fromException(IllegalStateException("No active Activity launcher"))
        }

        created.webExtensionController
            .ensureBuiltIn(EXTENSION_URI, EXTENSION_ID)
            .accept(
                { ext ->
                    if (ext != null) {
                        val callbacks: List<(WebExtension) -> Unit>
                        synchronized(this) {
                            builtInExtension = ext
                            callbacks = pendingExtensionCallbacks.toList()
                            pendingExtensionCallbacks.clear()
                        }
                        callbacks.forEach { it(ext) }
                    }
                },
                { err ->
                    Log.e(TAG, "Failed to install built-in WebExtension", err)
                }
            )

        runtime = created
        return created
    }

    fun getIfCreated(): GeckoRuntime? = runtime

    fun setActivityLauncher(launcher: ((PendingIntent) -> GeckoResult<Intent>?)?) {
        activityLauncher = launcher
    }

    fun withExtension(onReady: (WebExtension) -> Unit) {
        val ready = synchronized(this) {
            val current = builtInExtension
            if (current == null) {
                pendingExtensionCallbacks.add(onReady)
            }
            current
        }
        if (ready != null) {
            onReady(ready)
        }
    }

    fun updatePreferredColorScheme(dark: Boolean) {
        runtime?.settings?.preferredColorScheme = if (dark) {
            GeckoRuntimeSettings.COLOR_SCHEME_DARK
        } else {
            GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
        }
    }

    fun clearCache() {
        runtime?.storageController?.clearData(StorageController.ClearFlags.ALL_CACHES)
    }

    fun clearAccountData(accountId: String) {
        runtime?.storageController?.clearDataForSessionContext("account_$accountId")
    }
}
