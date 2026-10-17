package com.shiksharojgar.app

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Timestamp
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
            done(false, "पहले साइन-इन आवश्यक है।")
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
                done(false, error.localizedMessage ?: "पोस्ट भेजी नहीं जा सकी।")
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
    // 4. ADMIN: PENDING POSTS
    // --------------------------------------------------

    fun pendingPosts(
        onResult: (List<GroupPost>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        postsRef
            .whereEqualTo("status", "pending")
            .orderBy("createdAt", Query.Direction.ASCENDING)
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
                }
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
    // 7. DELETE POST
    // सुरक्षा: Rules मालिक या Admin को ही अनुमति दें।
    // --------------------------------------------------

    fun deletePost(
        postId: String,
        done: (Boolean, String?) -> Unit
    ) {
        postsRef.document(postId)
            .delete()
            .addOnSuccessListener {
                done(true, null)
            }
            .addOnFailureListener { error ->
                done(false, error.localizedMessage ?: "पोस्ट डिलीट नहीं हुई।")
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

                if (snapshot.isEmpty) {
                    done(0, null)
                    return@addOnSuccessListener
                }

                val documents = snapshot.documents
                val batches = documents.chunked(400)
                var deletedCount = 0

                fun deleteNextBatch(index: Int) {
                    if (index >= batches.size) {
                        done(deletedCount, null)
                        return
                    }

                    val batch = db.batch()
                    batches[index].forEach { document ->
                        batch.delete(document.reference)
                    }

                    batch.commit()
                        .addOnSuccessListener {
                            deletedCount += batches[index].size
                            deleteNextBatch(index + 1)
                        }
                        .addOnFailureListener { error ->
                            done(
                                deletedCount,
                                error.localizedMessage
                                    ?: "कुछ पुरानी पोस्ट डिलीट नहीं हुईं।"
                            )
                        }
                }

                deleteNextBatch(0)
            }
            .addOnFailureListener { error ->
                done(
                    0,
                    error.localizedMessage ?: "पुरानी पोस्ट की सफाई नहीं हो सकी।"
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
