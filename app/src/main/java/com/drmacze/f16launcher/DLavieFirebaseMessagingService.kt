package com.drmacze.f16launcher

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * DLavie Firebase Cloud Messaging (FCM) Service
 *
 * Handles:
 * 1. Token refresh — log + (future) upload to Supabase user_fcm_tokens table
 * 2. Push notification rendering — title, body, image (via NotificationCompat)
 * 3. Data-only messages — pass extras to ModernLauncherActivity via Intent
 *
 * Notification channel: "dlavie_notifications" (created on first message)
 *
 * Future enhancement: when Dev Dashboard sends a notification campaign,
 * Supabase Edge Function can call FCM Admin SDK to deliver to all
 * registered tokens. This service handles the device-side rendering.
 */
class DLavieFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "DLavieFCM"
        private const val CHANNEL_ID = "dlavie_notifications"
        private const val CHANNEL_NAME = "DLavie Notifications"
        private const val CHANNEL_DESC = "Pengumuman, update, dan notifikasi penting dari DLavie"
    }

    /**
     * Called when FCM issues a new token. This happens:
     * - On first app install
     * - When user clears Google Play Services data
     * - When app is restored on a new device
     * - When token expires (rare)
     *
     * TODO: upload this token to Supabase `user_fcm_tokens` table so the
     * Dev Dashboard can target this device for push campaigns.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM Token refreshed: ${token.take(20)}...${token.takeLast(10)}")

        // Persist token locally — Supabase sync happens on next app open
        // (when user is logged in, we upload to user_fcm_tokens table).
        getSharedPreferences("dlavie_fcm", Context.MODE_PRIVATE)
            .edit()
            .putString("fcm_token", token)
            .apply()
    }

    /**
     * Called when a push notification is received.
     *
     * Two types of FCM messages:
     * 1. Notification messages — have `notification` field with title/body.
     *    System auto-displays these when app is in background. We display
     *    manually when app is in foreground.
     * 2. Data messages — have `data` field with arbitrary key-value pairs.
     *    Always handled by this callback regardless of app state.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Message ID: ${remoteMessage.messageId}")
        Log.d(TAG, "Data payload: ${remoteMessage.data}")

        // Extract notification details (from `notification` field OR `data` field)
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "DLavie"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: ""
        val imageUrl = remoteMessage.notification?.imageUrl?.toString()
            ?: remoteMessage.data["image_url"]
        val category = remoteMessage.data["category"] ?: "general"

        // Log to Telemetry (existing system)
        try {
            val telemetry = Telemetry(applicationContext)
            telemetry.logAppEvent(
                event = "fcm_notification_received",
                properties = mapOf(
                    "title" to title,
                    "category" to category,
                    "message_id" to (remoteMessage.messageId ?: "")
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log telemetry for FCM event", e)
        }

        // Display notification (always — even in foreground, since we want
        // the user to see it as a system notification, not just in-app)
        sendNotification(
            title = title,
            body = body,
            category = category,
            data = remoteMessage.data
        )
    }

    /**
     * Create and display a system notification.
     * - Creates notification channel (Android 8+)
     * - Builds NotificationCompat with title, body, icon, sound
     * - Sets up PendingIntent to open ModernLauncherActivity on tap
     * - Includes data extras so ModernLauncherActivity can deep-link
     *   (e.g. navigate to Community tab if category=community)
     */
    private fun sendNotification(
        title: String,
        body: String,
        category: String,
        data: Map<String, String>
    ) {
        // Ensure notification channel exists (Android 8+)
        createNotificationChannel()

        // Intent to open the launcher when notification is tapped
        val intent = Intent(this, ModernLauncherActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            // Pass data extras for deep-linking
            data.forEach { (k, v) -> putExtra("fcm_$k", v) }
            putExtra("fcm_category", category)
            putExtra("fcm_title", title)
            putExtra("fcm_body", body)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.dlavie_launcher_icon) // app icon
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)

        // Add category-specific color accent
        when (category) {
            "maintenance" -> notificationBuilder.setColor(0xFFFFAB00.toInt()) // amber
            "update" -> notificationBuilder.setColor(0xFF00E676.toInt()) // mint green
            "community" -> notificationBuilder.setColor(0xFFB388FF.toInt()) // violet
            else -> notificationBuilder.setColor(0xFF00E5FF.toInt()) // cyan
        }

        // Show notification (unique ID per message so they stack)
        val notificationId = System.currentTimeMillis().toInt()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notificationBuilder.build())

        Log.d(TAG, "Notification displayed: id=$notificationId title='$title'")
    }

    /**
     * Create notification channel for Android 8+.
     * Idempotent — safe to call multiple times.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
                lightColor = 0xFF00E5FF.toInt()
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
