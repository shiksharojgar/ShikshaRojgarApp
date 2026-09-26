package com.shiksharojgar.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ShikshaMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val isChannel = message.data["type"] == "channel_post"
        var unread = getSharedPreferences("sr_notifications", MODE_PRIVATE).getInt("channel_unread", 0)
        if (isChannel) {
            unread += 1
            getSharedPreferences("sr_notifications", MODE_PRIVATE).edit().putInt("channel_unread", unread).apply()
            sendBroadcast(Intent("com.shiksharojgar.app.CHANNEL_UNREAD_CHANGED"))
        }
        val title = message.notification?.title ?: message.data["title"] ?: if (isChannel) "📢 Shiksha Rojgar Channel" else "Shiksha Rojgar"
        val body = message.notification?.body ?: message.data["body"] ?: "नई सूचना उपलब्ध है"
        val intent = Intent(this, if (isChannel) ChannelActivity::class.java else MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (isChannel) {
                val postId = message.data["postId"].orEmpty()
                data = if (postId.isNotBlank()) android.net.Uri.parse("shiksharojgar://channel/post/${android.net.Uri.encode(postId)}")
                       else android.net.Uri.parse("shiksharojgar://channel")
            }
        }
        val pending = PendingIntent.getActivity(this, if (isChannel) 21 else 22, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val id = if (isChannel) "shiksha_channel" else "shiksha_updates"
            manager.createNotificationChannel(NotificationChannel(id, if (isChannel) "Shiksha Rojgar Channel" else "Shiksha Rojgar Updates", NotificationManager.IMPORTANCE_HIGH))
        }
        val id = if (isChannel) "shiksha_channel" else "shiksha_updates"
        val n = NotificationCompat.Builder(this, id)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .build()
        manager.notify(if (isChannel) 2100 + unread else 2200, n)
    }
}
