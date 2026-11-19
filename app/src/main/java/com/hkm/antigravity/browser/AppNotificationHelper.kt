package com.hkm.antigravity.browser

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hkm.antigravity.MainActivity
import com.hkm.antigravity.R
import org.mozilla.geckoview.WebNotification
import org.mozilla.geckoview.WebNotificationDelegate

/**
 * Bridges GeckoView Web Notifications and Service Worker notifications
 * into native Android system notifications.
 */
object AppNotificationHelper {
    private const val CHANNEL_ID = "antigravity_agent_alerts"
    private const val CHANNEL_NAME = "Antigravity Alerts"

    @Volatile
    var notificationsEnabled: Boolean = true

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications from Antigravity remote sessions and agents"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun hasPostPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun createWebNotificationDelegate(appContext: Context): WebNotificationDelegate {
        ensureChannel(appContext)
        return object : WebNotificationDelegate {
            override fun onShowNotification(notification: WebNotification) {
                if (!notificationsEnabled || !hasPostPermission(appContext)) {
                    runCatching { notification.dismiss() }
                    return
                }
                ensureChannel(appContext)

                val notifId = stableId(notification)
                val launchIntent = Intent(appContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    appContext,
                    notifId,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val title = notification.title?.takeIf { it.isNotBlank() } ?: "Antigravity"
                val text = notification.text?.takeIf { it.isNotBlank() } ?: "Agent update"

                val built = NotificationCompat.Builder(appContext, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setSilent(notification.silent)
                    .setContentIntent(pendingIntent)
                    .build()

                runCatching {
                    NotificationManagerCompat.from(appContext).notify(notifId, built)
                    notification.show()
                }
            }

            override fun onCloseNotification(notification: WebNotification) {
                runCatching {
                    NotificationManagerCompat.from(appContext).cancel(stableId(notification))
                }
            }
        }
    }

    private fun stableId(notification: WebNotification): Int {
        val key = notification.tag?.takeIf { it.isNotBlank() }
            ?: "${notification.title.orEmpty()}|${notification.text.orEmpty()}"
        return (key.hashCode() and 0x7FFFFFFF).coerceAtLeast(1001)
    }
}
