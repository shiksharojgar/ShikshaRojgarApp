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

    // एक फोन/इंस्टॉलेशन की स्थायी ID
    val stableInstallId = prefs.getString("install_id", null)
        ?.takeIf { it.isNotBlank() }
        ?: java.util.UUID.randomUUID().toString().also {
            prefs.edit().putString("install_id", it).apply()
        }

    ChannelRepository.ensureSignedIn { connected ->
        if (!connected) {
            android.util.Log.w(
                "AnalyticsTracker",
                "Firebase sign-in failed"
            )
            return@ensureSignedIn
        }

        val currentUser = auth.currentUser
            ?: return@ensureSignedIn

        val uid = currentUser.uid
        val isRealLogin = !currentUser.isAnonymous
        val now = System.currentTimeMillis()

        val installRef = db.collection("app_installs")
            .document(stableInstallId)

        val configRef = db.document("channel_config/main")

        // पहला रिकॉर्ड: इस फोन पर ऐप कब पहली बार खुला।
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
                    mapOf(
                        "totalAppUsers" to currentCount + 1L
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            } else {
                // पुराने UID और पहली बार खुलने का समय सुरक्षित रखें।
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
                "Install record update failed",
                error
            )
        }

        // दूसरा रिकॉर्ड: वास्तविक Login अकाउंट की जानकारी।
        // Anonymous/Guest अकाउंट यहाँ दर्ज नहीं होगा।
        if (isRealLogin) {
            val linkId = "${stableInstallId}_$uid"

            val accountRef = db.collection("app_install_accounts")
                .document(linkId)

            accountRef.set(
                mapOf(
                    "installId" to stableInstallId,
                    "accountUid" to uid,
                    "lastLoginAt" to now
                ),
                com.google.firebase.firestore.SetOptions.merge()
            ).addOnFailureListener { error ->
                android.util.Log.e(
                    "AnalyticsTracker",
                    "Login link update failed",
                    error
                )
            }
        }

        // दैनिक सक्रियता रिकॉर्ड
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
                "lastSeenAt" to now
            ),
            com.google.firebase.firestore.SetOptions.merge()
        ).addOnFailureListener { error ->
            android.util.Log.e(
                "AnalyticsTracker",
                "Daily usage update failed",
                error
            )
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

    fun uniqueCategoryPostView(context: Context, postId: String) {
    if (postId.isBlank()) return

    ChannelRepository.ensureSignedIn { connected ->
        if (!connected) return@ensureSignedIn

        val uid = auth.currentUser?.uid ?: return@ensureSignedIn

        val postRef = db.collection("category_posts")
            .document(postId)

        val viewRef = postRef.collection("viewers")
            .document(uid)

        db.runTransaction { tx ->
            val viewSnapshot = tx.get(viewRef)
            val postSnapshot = tx.get(postRef)

            if (!postSnapshot.exists() || viewSnapshot.exists()) {
                return@runTransaction null
            }

            val currentViews =
                postSnapshot.getLong("viewCount") ?: 0L

            tx.set(
                viewRef,
                mapOf(
                    "uid" to uid,
                    "at" to System.currentTimeMillis()
                )
            )

            tx.update(
                postRef,
                "viewCount",
                currentViews + 1L
            )

            null
        }.addOnFailureListener { error ->
            android.util.Log.e(
                "AnalyticsTracker",
                "Category post view update failed: $postId",
                error
            )
        }
    }
}

 fun recordCategoryPostShare(
    context: Context,
    postId: String
) {
    if (postId.isBlank()) return

    ChannelRepository.ensureSignedIn { connected ->
        if (!connected) return@ensureSignedIn

        val postRef = db.collection("category_posts")
            .document(postId)

        db.runTransaction { tx ->
            val postSnapshot = tx.get(postRef)

            if (!postSnapshot.exists()) {
                return@runTransaction null
            }

            val currentShares =
                postSnapshot.getLong("shareCount") ?: 0L

            tx.update(
                postRef,
                "shareCount",
                currentShares + 1L
            )

            null
        }.addOnFailureListener { error ->
            android.util.Log.e(
                "AnalyticsTracker",
                "Category post share failed: $postId",
                error
            )
        }
    }
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
    event(
        context,
        "post_view",
        mapOf("post_id" to postId)
    )

    ChannelRepository.ensureSignedIn { connected ->
        if (!connected) {
            android.util.Log.w(
                "AnalyticsTracker",
                "View count: Firebase sign-in failed"
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

            val currentViews =
                postSnapshot.getLong("viewCount") ?: 0L

            val totalViews =
                configSnapshot.getLong("totalPostViews") ?: 0L

            tx.update(
                postRef,
                "viewCount",
                currentViews + 1L
            )

            tx.set(
                configRef,
                mapOf(
                    "totalPostViews" to totalViews + 1L
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )

            null
        }.addOnFailureListener { error ->
            android.util.Log.e(
                "AnalyticsTracker",
                "View count update failed for $postId",
                error
            )
        }
    }
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
