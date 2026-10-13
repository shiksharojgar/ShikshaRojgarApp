
package com.shiksharojgar.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import androidx.core.app.TaskStackBuilder

class ShikshaMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val isChannel = message.data["type"] == "channel_post"

        val prefs = getSharedPreferences("sr_notifications", MODE_PRIVATE)
        var unread = prefs.getInt("channel_unread", 0)

        // मौजूदा लाल बैज की व्यवस्था सुरक्षित रखें
        if (isChannel) {
            unread += 1
            prefs.edit().putInt("channel_unread", unread).apply()
            sendBroadcast(Intent("com.shiksharojgar.app.CHANNEL_UNREAD_CHANGED"))
        }

        val title = message.notification?.title
            ?: message.data["title"]
            ?: if (isChannel) "📢 Shiksha Rojgar Channel" else "Shiksha Rojgar"

        val body = message.notification?.body
            ?: message.data["body"]
            ?: "नई सूचना उपलब्ध है"

        val postId = message.data["postId"].orEmpty()

        
        val intent = Intent(
            this,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP

            action = "com.shiksharojgar.app.OPEN_CHANNEL_FROM_NOTIFICATION"
            putExtra("open_channel_from_notification", isChannel)
            putExtra("notification_post_id", postId)
            data = android.net.Uri.parse(
                "shiksharojgar://notification/${if (postId.isNotBlank()) android.net.Uri.encode(postId) else "channel"}"
            )
        }


        // हर पोस्ट का PendingIntent अलग रखें
        val requestCode = if (isChannel && postId.isNotBlank()) {
            postId.hashCode() and 0x7fffffff
        } else if (isChannel) {
            21
        } else {
            22
        }

        
        
        val pending = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )



        val manager = getSystemService(NotificationManager::class.java)
        val channelId = if (isChannel) "shiksha_channel" else "shiksha_updates"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    if (isChannel) "Shiksha Rojgar Channel" else "Shiksha Rojgar Updates",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .build()

        manager.notify(
            if (isChannel) 2100 + unread else 2200,
            notification
        )
    }
}
