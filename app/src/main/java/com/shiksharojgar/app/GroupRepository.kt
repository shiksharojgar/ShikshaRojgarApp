package com.shiksharojgar.app

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import java.util.Date

object GroupRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val postsRef
        get() = db.collection("group_posts")

    private val reportsRef
        get() = db.collection("group_reports")

    // --------------------------------------------------
    // 1. SIGN-IN
    // --------------------------------------------------

    fun ensureSignedIn(done: (Boolean) -> Unit) {
        if (auth.currentUser != null) {
            done(true)
            return
        }

        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                done(task.isSuccessful && auth.currentUser != null)
            }
    }

    // --------------------------------------------------
    // 2. USER NEW POST
    // पोस्ट हमेशा Pending बनेगी।
    // --------------------------------------------------

        fun submitPost(
        displayName: String,
        body: String,
        done: (Boolean, String?) -> Unit
    ) {
        val user = auth.currentUser
        val cleanName = displayName.trim()
        val cleanBody = body.trim()

        if (user == null) {
            done(false, "पहले लॉगिन करें।")
            return
        }

        if (user.isAnonymous) {
            done(
                false,
                "आपका अकाउंट अभी Guest मोड में है। " +
                    "ईमेल और पासवर्ड से लॉगिन करें।"
            )
            return
        }

        if (cleanName.isBlank() || cleanName.length > 40) {
            done(false, "नाम 1 से 40 अक्षरों के बीच रखें।")
            return
        }

        if (cleanBody.isBlank() || cleanBody.length > 5000) {
            done(false, "पोस्ट 1 से 5000 अक्षरों के बीच रखें।")
            return
        }

        user.reload()
            .addOnSuccessListener {

                if (!user.isEmailVerified) {
                    done(
                        false,
                        "पोस्ट भेजने से पहले Gmail सत्यापित करें।"
                    )
                    return@addOnSuccessListener
                }

                user.getIdToken(true)
                    .addOnSuccessListener { tokenResult ->

                        val firebaseClaims =
                            tokenResult.claims["firebase"] as? Map<*, *>

                        val provider =
                            firebaseClaims?.get("sign_in_provider")
                                ?.toString()

                        val emailVerified =
                            tokenResult.claims["email_verified"] == true

                        if (provider == "anonymous") {
                            done(
                                false,
                                "Firebase अभी आपको Guest अकाउंट मान रहा है। " +
                                    "लॉगआउट करके अपने सत्यापित ईमेल से दोबारा लॉगिन करें।"
                            )
                            return@addOnSuccessListener
                        }

                        if (!emailVerified) {
                            done(
                                false,
                                "Firebase टोकन में Gmail सत्यापन सक्रिय नहीं है। " +
                                    "लॉगिन करके दोबारा प्रयास करें।"
                            )
                            return@addOnSuccessListener
                        }

                        val data = hashMapOf<String, Any>(
                            "uid" to user.uid,
                            "displayName" to cleanName,
                            "body" to cleanBody,
                            "status" to "pending",
                            "createdAt" to FieldValue.serverTimestamp()
                        )

                        postsRef.add(data)
                            .addOnSuccessListener {
                                done(true, null)
                            }
                            .addOnFailureListener { error ->
                                done(
                                    false,
                                    "पोस्ट सेव नहीं हुई: " +
                                        (error.localizedMessage
                                            ?: "अनुमति नहीं मिली।")
                                )
                            }
                    }
                    .addOnFailureListener { error ->
                        done(
                            false,
                            "Firebase सुरक्षा टोकन नहीं मिला: " +
                                (error.localizedMessage ?: "अज्ञात त्रुटि")
                        )
                    }
            }
            .addOnFailureListener { error ->
                done(
                    false,
                    "अकाउंट की जाँच नहीं हो सकी: " +
                        (error.localizedMessage ?: "अज्ञात त्रुटि")
                )
            }
    }

    // --------------------------------------------------
    // 3. APPROVED PUBLIC POSTS
    // केवल मंजूर और समय-सीमा में रहने वाली पोस्ट।
    // --------------------------------------------------

    fun approvedPosts(
        onResult: (List<GroupPost>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        postsRef
            .whereEqualTo("status", "approved")
            .whereGreaterThan("expiresAt", Timestamp.now())
            .orderBy("expiresAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val created = doc.getTimestamp("createdAt")
                    val approved = doc.getTimestamp("approvedAt")
                    val expires = doc.getTimestamp("expiresAt")

                    GroupPost(
                        id = doc.id,
                        uid = doc.getString("uid").orEmpty(),
                        displayName = doc.getString("displayName").orEmpty(),
                        body = doc.getString("body").orEmpty(),
                        status = doc.getString("status") ?: "pending",
                        createdAt = created?.toDate()?.time ?: 0L,
                        approvedAt = approved?.toDate()?.time ?: 0L,
                        expiresAt = expires?.toDate()?.time ?: 0L
                    )
                }.sortedBy { it.createdAt }

                onResult(posts)
            }
    }
    // --------------------------------------------------
    // GROUP: LOAD ONE PUBLIC POST BY ID
    // केवल मंजूर और समय-सीमा में रहने वाली पोस्ट।
    // --------------------------------------------------

    fun approvedPostById(
        postId: String,
        onResult: (GroupPost?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (postId.isBlank() || postId.length > 150) {
            onResult(null)
            return
        }

        postsRef.document(postId)
            .get()
            .addOnSuccessListener { doc ->
                val now = System.currentTimeMillis()
                val status = doc.getString("status").orEmpty()
                val createdAt = doc.getTimestamp("createdAt")
                    ?.toDate()?.time ?: 0L
                val approvedAt = doc.getTimestamp("approvedAt")
                    ?.toDate()?.time ?: 0L
                val expiresAt = doc.getTimestamp("expiresAt")
                    ?.toDate()?.time ?: 0L

                if (!doc.exists() ||
                    status != "approved" ||
                    expiresAt <= now
                ) {
                    onResult(null)
                    return@addOnSuccessListener
                }

                onResult(
                    GroupPost(
                        id = doc.id,
                        uid = doc.getString("uid").orEmpty(),
                        displayName = doc.getString("displayName").orEmpty(),
                        body = doc.getString("body").orEmpty(),
                        status = status,
                        createdAt = createdAt,
                        approvedAt = approvedAt,
                        expiresAt = expiresAt
                    )
                )
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }
    
    // --------------------------------------------------
    // 4. ADMIN: PENDING POSTS
    // --------------------------------------------------

    fun pendingPosts(
        onResult: (List<GroupPost>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        postsRef
    .whereEqualTo("status", "pending")
    .get()
            .addOnSuccessListener { snapshot ->
                val posts = snapshot.documents.map { doc ->
    GroupPost(
        id = doc.id,
        uid = doc.getString("uid").orEmpty(),
        displayName = doc.getString("displayName").orEmpty(),
        body = doc.getString("body").orEmpty(),
        status = doc.getString("status") ?: "pending",
        createdAt = doc.getTimestamp("createdAt")
            ?.toDate()?.time ?: 0L
    )
}.sortedBy { it.createdAt }

onResult(posts)
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }

    // --------------------------------------------------
    // 5. ADMIN: APPROVE POST
    // 7, 8 या 30 दिन जैसी अवधि दे सकते हैं।
    // --------------------------------------------------

    fun approvePost(
        postId: String,
        expiryDays: Int,
        done: (Boolean, String?) -> Unit
    ) {
        if (expiryDays !in 1..365) {
            done(false, "समय-सीमा 1 से 365 दिन के बीच रखें।")
            return
        }

        val expiryDate = Date(
            System.currentTimeMillis() +
                expiryDays.toLong() * 24L * 60L * 60L * 1000L
        )

        postsRef.document(postId)
            .update(
                mapOf(
                    "status" to "approved",
                    "approvedAt" to FieldValue.serverTimestamp(),
                    "expiresAt" to Timestamp(expiryDate)
                )
            )
            .addOnSuccessListener {
                done(true, null)
            }
            .addOnFailureListener { error ->
                done(false, error.localizedMessage ?: "पोस्ट मंजूर नहीं हुई।")
            }
    }

    // --------------------------------------------------
    // 6. ADMIN: REJECT POST
    // --------------------------------------------------

    fun rejectPost(
        postId: String,
        done: (Boolean, String?) -> Unit
    ) {
        postsRef.document(postId)
            .update("status", "rejected")
            .addOnSuccessListener {
                done(true, null)
            }
            .addOnFailureListener { error ->
                done(false, error.localizedMessage ?: "पोस्ट रिजेक्ट नहीं हुई।")
            }
    }

            // --------------------------------------------------
    // GROUP POST LIKE
    // हर यूज़र का अपना Like दस्तावेज़
    // --------------------------------------------------

    fun toggleLike(
        postId: String,
        done: (Boolean, Long, String?) -> Unit
    ) {
        val user = auth.currentUser

        if (user == null || user.isAnonymous) {
            done(false, 0L, "Like करने के लिए लॉगिन करें।")
            return
        }

        if (postId.isBlank()) {
            done(false, 0L, "पोस्ट की जानकारी उपलब्ध नहीं है।")
            return
        }

        val postRef = postsRef.document(postId)
        val likeRef = postRef.collection("likes")
            .document(user.uid)

        db.runTransaction { transaction ->

            val likeSnapshot = transaction.get(likeRef)
            val liked = !likeSnapshot.exists()

            if (liked) {
                transaction.set(
                    likeRef,
                    mapOf(
                        "uid" to user.uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                transaction.delete(likeRef)
            }

            liked

        }.addOnSuccessListener { liked ->

            postRef.collection("likes")
                .get()
                .addOnSuccessListener { snapshot ->
                    done(liked, snapshot.size().toLong(), null)
                }
                .addOnFailureListener { error ->
                    done(
                        liked,
                        0L,
                        error.localizedMessage
                            ?: "Like की गिनती नहीं पढ़ी जा सकी।"
                    )
                }

        }.addOnFailureListener { error ->
            done(
                false,
                0L,
                error.localizedMessage
                    ?: "Like अपडेट नहीं हो सका।"
            )
        }
    }
    

    // --------------------------------------------------
        // --------------------------------------------------
    // GROUP POST SHARE COUNT
    // एक यूज़र की एक पोस्ट पर एक Share एंट्री
    // --------------------------------------------------

    fun recordShare(
        postId: String,
        done: (Boolean, Long, String?) -> Unit
    ) {
        val user = auth.currentUser

        if (user == null || user.isAnonymous) {
            done(false, 0L, "Share काउंट के लिए लॉगिन करें।")
            return
        }

        if (postId.isBlank()) {
            done(false, 0L, "पोस्ट की जानकारी उपलब्ध नहीं है।")
            return
        }

        val postRef = postsRef.document(postId)
        val shareRef = postRef.collection("shares")
            .document(user.uid)

        shareRef.get()
            .addOnSuccessListener { existingShare ->

                if (existingShare.exists()) {
                    postRef.collection("shares")
                        .get()
                        .addOnSuccessListener { snapshot ->
                            done(
                                true,
                                snapshot.size().toLong(),
                                null
                            )
                        }
                        .addOnFailureListener { error ->
                            done(
                                false,
                                0L,
                                error.localizedMessage
                                    ?: "Share की गिनती नहीं पढ़ी जा सकी।"
                            )
                        }

                    return@addOnSuccessListener
                }

                shareRef.set(
                    mapOf(
                        "uid" to user.uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).addOnSuccessListener {

                    postRef.collection("shares")
                        .get()
                        .addOnSuccessListener { snapshot ->
                            done(
                                true,
                                snapshot.size().toLong(),
                                null
                            )
                        }
                        .addOnFailureListener { error ->
                            done(
                                false,
                                0L,
                                error.localizedMessage
                                    ?: "Share की गिनती नहीं पढ़ी जा सकी।"
                            )
                        }

                }.addOnFailureListener { error ->
                    done(
                        false,
                        0L,
                        error.localizedMessage
                            ?: "Share रिकॉर्ड नहीं हो सका।"
                    )
                }
            }
            .addOnFailureListener { error ->
                done(
                    false,
                    0L,
                    error.localizedMessage
                        ?: "Share रिकॉर्ड जाँचा नहीं जा सका।"
                )
            }
    }
    
    // --------------------------------------------------
    // 7. DELETE POST
    // सुरक्षा: Rules मालिक या Admin को ही अनुमति दें।
    // --------------------------------------------------

    fun deletePost(
    postId: String,
    done: (Boolean, String?) -> Unit
) {
    ChannelRepository.deleteGroupPost(postId) { success ->

        if (success) {
            done(true, null)
        } else {
            done(
                false,
                "ग्रुप पोस्ट डिलीट नहीं हुई या मीडिया की सफाई पूरी नहीं हो पाई।"
            )
        }
    }
}

    // --------------------------------------------------
    // 8. ADMIN: DELETE ALL EXPIRED APPROVED POSTS
    // यह सफाई Admin के ऑनलाइन होने पर चल सकती है।
    // --------------------------------------------------

    fun deleteExpiredPosts(
    done: (Int, String?) -> Unit
) {
    val now = Timestamp.now()

    postsRef
        .whereEqualTo("status", "approved")
        .whereLessThanOrEqualTo("expiresAt", now)
        .get()
        .addOnSuccessListener { snapshot ->

            val documents = snapshot.documents

            if (documents.isEmpty()) {
                done(0, null)
                return@addOnSuccessListener
            }

            var index = 0
            var deletedCount = 0
            var hasFailure = false

            fun deleteNextPost() {
                if (index >= documents.size) {
                    if (hasFailure) {
                        done(
                            deletedCount,
                            "कुछ पुरानी पोस्ट या उनकी मीडिया सफाई पूरी नहीं हो सकी।"
                        )
                    } else {
                        done(deletedCount, null)
                    }
                    return
                }

                val document = documents[index]
                index++

                ChannelRepository.deleteGroupPost(document.id) { success ->
                    if (success) {
                        deletedCount++
                    } else {
                        hasFailure = true
                    }

                    deleteNextPost()
                }
            }

            deleteNextPost()
        }
        .addOnFailureListener { error ->
            done(
                0,
                error.localizedMessage
                    ?: "पुरानी पोस्ट की सफाई नहीं हो सकी।"
            )
        }
}

    // --------------------------------------------------
    // 9. USER: SEND PRIVATE REPORT / COMPLAINT
    // --------------------------------------------------

    fun submitReport(
        subject: String,
        message: String,
        suggestedSolution: String,
        relatedPostId: String?,
        done: (Boolean, String?) -> Unit
    ) {
        val user = auth.currentUser

        if (user == null) {
            done(false, "पहले साइन-इन आवश्यक है।")
            return
        }

        val cleanSubject = subject.trim()
        val cleanMessage = message.trim()
        val cleanSolution = suggestedSolution.trim()

        if (cleanSubject.isBlank() || cleanSubject.length > 150) {
            done(false, "विषय 1 से 150 अक्षरों के बीच रखें।")
            return
        }

        if (cleanMessage.isBlank() || cleanMessage.length > 3000) {
            done(false, "शिकायत 1 से 3000 अक्षरों के बीच रखें।")
            return
        }

        if (cleanSolution.length > 2000) {
            done(false, "सुझाव अधिकतम 2000 अक्षरों का रखें।")
            return
        }

        val data = hashMapOf<String, Any>(
            "uid" to user.uid,
            "subject" to cleanSubject,
            "message" to cleanMessage,
            "suggestedSolution" to cleanSolution,
            "status" to "open",
            "createdAt" to FieldValue.serverTimestamp()
        )

        if (!relatedPostId.isNullOrBlank()) {
            data["postId"] = relatedPostId.take(150)
        }

        reportsRef.add(data)
            .addOnSuccessListener {
                done(true, null)
            }
            .addOnFailureListener { error ->
                done(false, error.localizedMessage ?: "शिकायत भेजी नहीं जा सकी।")
            }
    }
}
