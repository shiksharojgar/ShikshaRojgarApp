
package com.shiksharojgar.app

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.auth.FirebaseAuth

object CategoryPostRepository {

    private val db = FirebaseFirestore.getInstance()

    private val postsRef
        get() = db.collection("category_posts")

    // सभी पोस्ट पढ़ें; पिन की गई पोस्ट पहले दिखाएँ।
    // category में हर बार संबंधित कैटेगरी की पहचान दें।
    fun observePosts(
        category: String,
        onResult: (List<CategoryPost>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return postsRef.addSnapshotListener { snapshot, error ->

            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }

            if (snapshot == null) {
                onResult(emptyList())
                return@addSnapshotListener
            }

            val posts = snapshot.documents
                .mapNotNull { doc ->
                    val postCategory =
                        doc.getString("category").orEmpty()

                    if (postCategory != category) {
                        return@mapNotNull null
                    }

                    
                    CategoryPost(
                        id = doc.id,
                        category = postCategory,
                        title = doc.getString("title").orEmpty(),
                        body = doc.getString("body").orEmpty(),
                        description = doc.getString("description").orEmpty(),
websiteUrl = doc.getString("websiteUrl").orEmpty(),
bodyBold = doc.getBoolean("bodyBold") ?: false,
                        bodyHighlight = doc.getBoolean("bodyHighlight") ?: false,
                        bodyColor = doc.getString("bodyColor") ?: "#222222",
                        buttons = (doc.get("buttons") as? List<*>)
                            .orEmpty()
                            .mapNotNull buttonMap@ { item ->
                                val button = item as? Map<*, *>
                                    ?: return@buttonMap null

                                CategoryPostButton(
                                    label = button["label"] as? String ?: "",
                                    url = button["url"] as? String ?: "",
                                    backgroundColor = button["backgroundColor"] as? String
                                        ?: "#1976D2",
                                    textColor = button["textColor"] as? String
                                        ?: "#FFFFFF",
                                    enabled = button["enabled"] as? Boolean
                                        ?: true
                                )
                            },
                        imageUrl = doc.getString("imageUrl").orEmpty(),
                        fileUrl = doc.getString("fileUrl").orEmpty(),
                        fileName = doc.getString("fileName").orEmpty(),
                        imageMime = doc.getString("imageMime") ?: "image/jpeg",
                        fileMime = doc.getString("fileMime") ?: "application/pdf",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        pinned = doc.getBoolean("pinned") ?: false,
                        pinOrder = doc.getLong("pinOrder") ?: 0L,
                                                enabled = doc.getBoolean("enabled") ?: true,
                        viewCount = doc.getLong("viewCount") ?: 0L,
shareCount = doc.getLong("shareCount") ?: 0L,
likeCount = doc.getLong("likeCount") ?: 0L
                    )

                }
                .filter { it.enabled }
                .sortedWith(
                    compareByDescending<CategoryPost> { it.pinned }
                        .thenByDescending { it.pinOrder }
                        .thenByDescending { it.createdAt }
                )

            onResult(posts)
        }
    }

    // हर नई पोस्ट को अलग Firestore दस्तावेज़ में सेव करें।
    
fun savePost(
    post: CategoryPost,
    onResult: (Boolean, String?) -> Unit
) {
    if (post.category.isBlank()) {
        onResult(false, "कैटेगरी चुनना जरूरी है")
        return
    }

    val document = if (post.id.isBlank()) {
        postsRef.document()
    } else {
        postsRef.document(post.id)
    }

    val savedPost = post.copy(
        id = document.id,
        createdAt = if (post.createdAt > 0L) {
            post.createdAt
        } else {
            System.currentTimeMillis()
        }
    )

    postsRef
        .whereEqualTo("category", savedPost.category)
        .whereEqualTo("enabled", true)
        .get()
        .addOnSuccessListener { existing ->

            // नई पोस्ट पर ही 7 पोस्ट की सीमा लागू करें।
            // पुरानी पोस्ट को एडिट करने की अनुमति रहेगी।
            if (post.id.isBlank() && existing.size() >= 7) {
                onResult(
                    false,
                    "इस कैटेगरी में पहले से 7 पोस्ट हैं। नई पोस्ट जोड़ने से पहले कोई पोस्ट डिलीट करें।"
                )
                return@addOnSuccessListener
            }
            document.get()
                .addOnSuccessListener { current ->

                    val data = mapOf(
                        "category" to savedPost.category,
                        "title" to savedPost.title,
                        "body" to savedPost.body,
                        "description" to savedPost.description,
                        "websiteUrl" to savedPost.websiteUrl,
                        "bodyBold" to savedPost.bodyBold,
                        "bodyHighlight" to savedPost.bodyHighlight,
                        "bodyColor" to savedPost.bodyColor,
                        "buttons" to savedPost.buttons.map { button ->
                            mapOf(
                                "label" to button.label,
                                "url" to button.url,
                                "backgroundColor" to button.backgroundColor,
                                "textColor" to button.textColor,
                                "enabled" to button.enabled
                            )
                        },
                        "imageUrl" to savedPost.imageUrl,
                        "fileUrl" to savedPost.fileUrl,
                        "fileName" to savedPost.fileName,
                        "imageMime" to savedPost.imageMime,
                        "fileMime" to savedPost.fileMime,
                        "createdAt" to savedPost.createdAt,
                        "pinned" to savedPost.pinned,
                        "pinOrder" to savedPost.pinOrder,
                        "enabled" to savedPost.enabled,
                    "viewCount" to (current.getLong("viewCount") ?: 0L),
"shareCount" to (current.getLong("shareCount") ?: 0L),
"likeCount" to (current.getLong("likeCount") ?: 0L)
                    )

                    document.set(data)
                        .addOnSuccessListener {
                            onResult(true, document.id)
                        }
                        .addOnFailureListener { error ->
                            onResult(false, error.message)
                        }
                }
                .addOnFailureListener { error ->
                    onResult(
                        false,
                        "पुरानी पोस्ट की गिनती पढ़ने में समस्या: ${error.message}"
                    )
                }
}

}
    // पोस्ट को पिन या अनपिन करें।
    fun setPinned(
        postId: String,
        pinned: Boolean,
        onResult: (Boolean, String?) -> Unit
    ) {
        val updates = mapOf(
            "pinned" to pinned,
            "pinOrder" to if (pinned) {
                System.currentTimeMillis()
            } else {
                0L
            }
        )

        postsRef.document(postId)
            .update(updates)
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener { error ->
                onResult(false, error.message)
            }
    }


    // केवल चुनी हुई पोस्ट हटाएँ और अप्रयुक्त मीडिया साफ करें।
    fun deletePost(
        postId: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (postId.isBlank()) {
            onResult(false, "पोस्ट ID खाली है")
            return
        }

        ChannelRepository.deleteCategoryPost(postId) { success ->
            if (success) {
                onResult(true, null)
            } else {
                onResult(
                    false,
                    "पोस्ट डिलीट नहीं हुई या मीडिया साफ नहीं हो पाया"
                )
            }
        }
    }
    // किसी पोस्ट पर Like / Unlike सुरक्षित करें।
    fun setLiked(
        postId: String,
        liked: Boolean,
        onResult: (Boolean) -> Unit
    ) {
        if (postId.isBlank()) {
            onResult(false)
            return
        }

        ChannelRepository.ensureSignedIn { signedIn ->

            if (!signedIn) {
                onResult(false)
                return@ensureSignedIn
            }

            val uid = FirebaseAuth.getInstance().currentUser?.uid

            if (uid.isNullOrBlank()) {
                onResult(false)
                return@ensureSignedIn
            }

            val likeRef = db.collection("category_likes")
                .document(postId)
                .collection("users")
                .document(uid)

            val postRef = postsRef.document(postId)

            db.runTransaction { transaction ->

                val likeSnapshot = transaction.get(likeRef)
                val postSnapshot = transaction.get(postRef)

                if (!postSnapshot.exists()) {
                    throw IllegalStateException("पोस्ट उपलब्ध नहीं है")
                }

                val currentCount =
                    (postSnapshot.getLong("likeCount") ?: 0L)
                        .coerceAtLeast(0L)

                if (liked && !likeSnapshot.exists()) {
                    transaction.set(
                        likeRef,
                        mapOf(
                            "uid" to uid,
                            "at" to System.currentTimeMillis()
                        )
                    )

                    transaction.update(
                        postRef,
                        "likeCount",
                        currentCount + 1L
                    )

                } else if (!liked && likeSnapshot.exists()) {
                    transaction.delete(likeRef)

                    transaction.update(
                        postRef,
                        "likeCount",
                        (currentCount - 1L).coerceAtLeast(0L)
                    )
                }

                null

            }.addOnSuccessListener {
                onResult(true)
            }.addOnFailureListener {
                onResult(false)
            }
        }
    }

    // जाँचें कि वर्तमान यूज़र ने पोस्ट Like की है या नहीं।
    fun isLiked(
        postId: String,
        onResult: (Boolean) -> Unit
    ) {
        ChannelRepository.ensureSignedIn { signedIn ->

            if (!signedIn) {
                onResult(false)
                return@ensureSignedIn
            }

            val uid = FirebaseAuth.getInstance().currentUser?.uid

            if (uid.isNullOrBlank()) {
                onResult(false)
                return@ensureSignedIn
            }

            db.collection("category_likes")
                .document(postId)
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener {
                    onResult(it.exists())
                }
                .addOnFailureListener {
                    onResult(false)
                }
        }
    }

    // प्रत्येक Share दबाने पर संख्या एक बढ़ाएँ।
    fun increaseShareCount(
        postId: String,
        onResult: (Boolean) -> Unit
    ) {
        if (postId.isBlank()) {
            onResult(false)
            return
        }

        postsRef.document(postId)
            .update(
                "shareCount",
                com.google.firebase.firestore.FieldValue.increment(1L)
            )
            .addOnSuccessListener {
                onResult(true)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }
}
