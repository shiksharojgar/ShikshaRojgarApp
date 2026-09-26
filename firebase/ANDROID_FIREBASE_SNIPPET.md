# Android Firebase Messaging addition

The base project intentionally does not contain a private `google-services.json`. After you create your Firebase project, add that file to `app/` and then add the following.

## 1) app/build.gradle.kts

Uncomment the Google Services plugin:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

dependencies {
    implementation("com.google.firebase:firebase-messaging:24.1.0")
}
```

## 2) Root build.gradle.kts

The plugin is already declared in this project:

```kotlin
id("com.google.gms.google-services") version "4.4.2" apply false
```

## 3) Create `ShikshaMessagingService.kt`

```kotlin
class ShikshaMessagingService : FirebaseMessagingService() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                "shiksha_updates",
                "Shiksha Rojgar Updates",
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: "Shiksha Rojgar"
        val body = message.notification?.body ?: "नई पोस्ट उपलब्ध है"
        val url = message.data["url"] ?: "https://www.shiksharojgar.com/"
        val intent = Intent(this, WebViewActivity::class.java).apply {
            putExtra("url", url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pending = PendingIntent.getActivity(
            this, url.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, "shiksha_updates")
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(this).notify(url.hashCode(), notification)
    }
}
```

Also add `androidx.core:core:ktx` (already present) and the service to `AndroidManifest.xml`:

```xml
<service
    android:name=".ShikshaMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

Subscribe after Firebase initialization:

```kotlin
FirebaseMessaging.getInstance().subscribeToTopic("all_updates")
```

On Android 13+, request `POST_NOTIFICATIONS` at runtime.
