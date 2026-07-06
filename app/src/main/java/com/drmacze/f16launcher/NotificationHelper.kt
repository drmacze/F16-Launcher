package com.drmacze.f16launcher

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * NotificationHelper — local push notifications untuk Community Phase 2.
 *
 * Membutuhkan:
 *  - <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
 *    (untuk Android 13+ / API 33+, sudah ditambahkan ke AndroidManifest.xml)
 *  - Runtime permission request di MainShell (ActivityResultContracts.RequestPermission).
 *
 * Channel: "dlavie_community" (IMPORTANCE_DEFAULT).
 * Notification tap → buka ModernLauncherActivity dengan extra "post_id".
 */
object NotificationHelper {
    const val CHANNEL_ID = "dlavie_community"
    private const val CHANNEL_NAME = "Komunitas DLavie"

    /** Idempotent — create notification channel (Android O+). Safe untuk dipanggil berkali-kali. */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifikasi post baru dari user yang Anda follow"
                enableVibration(true)
                enableLights(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Tampilkan local notification untuk post baru dari followed user.
     *
     * @param title  mis. "Post baru dari John"
     * @param body   mis. "Cek gameplay terbaru saya"
     * @param postId id post — dipassing sebagai Intent extra + dipakai sebagai notification id (hashCode)
     */
    fun showNotification(
        context: Context,
        title: String,
        body: String,
        postId: String
    ) {
        createChannel(context)

        val intent = Intent(context, ModernLauncherActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("post_id", postId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            postId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.dlavie_launcher_icon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .build()

        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(postId.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission denied (Android 13+) — silent fail; user masih lihat banner in-app.
        } catch (_: Throwable) {
            // Best-effort — jangan crash app karena notifikasi gagal.
        }
    }
}
