package com.shiksharojgar.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

/** Central place for Firebase Analytics + unique Firestore engagement counters. */
object AnalyticsTracker {
    private const val PREF = "sr_analytics"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    
fun appIdentity(context: Context) {
    val prefs = context.getSharedPreferences(
        "sr_analytics",
        Context.MODE_PRIVATE
    )

    // एक इंस्टॉलेशन के लिए स्थायी ID।
    // ऐप अपडेट पर यही ID बनी रहेगी।
    var installId = prefs.getString("install_id", null)

    if (installId.isNullOrBlank()) {
        installId = java.util.UUID.randomUUID().toString()
        prefs.edit().putString("install_id", installId).apply()
    }

    val stableInstallId = installId

    // Firebase Auth तैयार होने के बाद ही रिकॉर्ड करें।
    ChannelRepository.ensureSignedIn { connected ->
        if (!connected) {
            android.util.Log.w(
                "AnalyticsTracker",
                "Install count: Firebase sign-in failed"
            )
            return@ensureSignedIn
        }

        val uid = auth.currentUser?.uid ?: return@ensureSignedIn

        val installRef = db.collection("app_installs")
            .document(stableInstallId)

        val configRef = db.document("channel_config/main")
        val now = System.currentTimeMillis()

        db.runTransaction { transaction ->
            val installSnapshot = transaction.get(installRef)

            if (!installSnapshot.exists()) {
                val configSnapshot = transaction.get(configRef)
                val currentCount =
                    configSnapshot.getLong("totalAppUsers") ?: 0L

                transaction.set(
                    installRef,
                    mapOf(
                        "installId" to stableInstallId,
                        "uid" to uid,
                        "firstSeenAt" to now,
                        "lastSeenAt" to now
                    )
                )

                transaction.set(
                    configRef,
                    mapOf("totalAppUsers" to currentCount + 1L),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            } else {
                transaction.update(
                    installRef,
                    "lastSeenAt",
                    now
                )
            }

            null
        }.addOnFailureListener { error ->
            android.util.Log.e(
                "AnalyticsTracker",
                "Install count update failed",
                error
            )
        }
        
        val day = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(Date())

        val dayRef = db.collection("app_usage")
            .document(uid)
            .collection("days")
            .document(day)

        dayRef.set(
            mapOf(
                "uid" to uid,
                "day" to day,
                "lastSeenAt" to System.currentTimeMillis()
            ),
            com.google.firebase.firestore.SetOptions.merge()
        ).addOnSuccessListener {
            dayRef.get().addOnSuccessListener { daySnapshot ->
                if (!daySnapshot.exists()) {
                    db.document("channel_config/main").update(
                        "totalActiveUserDays",
                        com.google.firebase.firestore.FieldValue.increment(1)
                    )
                }
            }
        }
        
    }
}


    fun appOpen(context: Context) {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (!p.getBoolean("first_use_logged", false)) {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                db.collection("app_first_use").document(uid).set(mapOf("uid" to uid, "version" to BuildConfig.VERSION_NAME, "at" to System.currentTimeMillis()), com.google.firebase.firestore.SetOptions.merge())
            }
            p.edit().putBoolean("first_use_logged", true).apply()
        }
    }

    fun event(context: Context, name: String, params: Map<String, String> = emptyMap()) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("app_events").document(uid).collection("events").add(mapOf("name" to name, "params" to params, "at" to System.currentTimeMillis()))
    }

    fun websiteOpen(context: Context, source: String) = event(context, "website_open", mapOf("source" to source))
    fun appShare(context: Context) = event(context, "app_share")
    fun channelShare(context: Context) = event(context, "channel_share")
    fun offlineDownload(context: Context, postId: String) = event(context, "offline_download", mapOf("post_id" to postId))
    fun channelFollow(context: Context, followed: Boolean) = event(context, "channel_follow", mapOf("followed" to followed.toString()))

    fun uniqueFeedPostView(context: Context, postId: String) {
        event(context, "feed_post_view", mapOf("post_id" to postId))
        uniqueAction("feed_post_views", safeId(postId), postId, context, null)
    }

    
fun uniquePostShare(context: Context, postId: String) {
    ChannelRepository.ensureSignedIn { connected ->
        if (!connected) {
            android.util.Log.w(
                "AnalyticsTracker",
                "Share count: Firebase sign-in failed"
            )
            return@ensureSignedIn
        }

        val postRef = db.collection("channel_posts")
            .document(postId)

        val configRef = db.document("channel_config/main")

        db.runTransaction { tx ->
            val postSnapshot = tx.get(postRef)

            if (!postSnapshot.exists()) {
                return@runTransaction null
            }

            val configSnapshot = tx.get(configRef)

            val currentPostShares =
                postSnapshot.getLong("shareCount") ?: 0L

            val currentTotalShares =
                configSnapshot.getLong("totalPostShares") ?: 0L

            // हर Share बटन टैप पर एक गिनती बढ़ेगी।
            tx.update(
                postRef,
                "shareCount",
                currentPostShares + 1L
            )

            tx.set(
                configRef,
                mapOf(
                    "totalPostShares" to currentTotalShares + 1L
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )

            null
        }.addOnSuccessListener {
            event(
                context,
                "post_share",
                mapOf("post_id" to postId)
            )
        }.addOnFailureListener { error ->
            android.util.Log.e(
                "AnalyticsTracker",
                "Share count update failed for post $postId",
                error
            )
        }
    }
}


    fun uniquePostView(context: Context, postId: String) {
        event(context, "post_view", mapOf("post_id" to postId))
        uniqueAction("post_views", safeId(postId), postId, context, "viewCount")
    }

    fun uniquePostShare(context: Context, postId: String) {
        event(context, "post_share", mapOf("post_id" to postId))
        uniqueAction("post_shares", safeId(postId), postId, context, "shareCount")
    }

    private fun safeId(value: String): String = java.util.UUID.nameUUIDFromBytes(value.toByteArray()).toString()

    /** Ensures Anonymous Auth is ready before recording automatic view/share actions. */
    private fun uniqueAction(collection: String, actionDocId: String, originalPostId: String, context: Context, counterField: String?) {
        ChannelRepository.ensureSignedIn { connected ->
            if (!connected) return@ensureSignedIn
            val uid = auth.currentUser?.uid ?: return@ensureSignedIn
            val metricDocId = if (collection == "post_views" || collection == "post_shares") originalPostId else actionDocId
            val ref = db.collection(collection).document(metricDocId).collection("users").document(uid)
            ref.get().addOnSuccessListener { existing ->
                if (existing.exists()) return@addOnSuccessListener
                ref.set(mapOf("uid" to uid, "at" to System.currentTimeMillis(), "metric" to if (collection == "feed_post_views") "feedPostView" else if (collection == "feed_post_shares") "feedPostShare" else collection)).addOnSuccessListener {
                    if (counterField != null) {
                        val postRef = db.collection("channel_posts").document(originalPostId)
                        val totalField = if (counterField == "viewCount") "totalPostViews" else "totalPostShares"
                        db.runTransaction { tx ->
                            val postSnap = tx.get(postRef)
                            val current = postSnap.getLong(counterField) ?: 0L
                            tx.update(postRef, counterField, current + 1L)
                            val configRef = db.document("channel_config/main")
                            val configSnap = tx.get(configRef)
                            val total = configSnap.getLong(totalField) ?: 0L
                            tx.set(configRef, mapOf(totalField to total + 1L), com.google.firebase.firestore.SetOptions.merge())
                            null
                        }.addOnFailureListener {
                            // The unique event remains recorded; the hourly Cloud Function can repair counters.
                        }
                    }
                }
            }
        }
    }
}
