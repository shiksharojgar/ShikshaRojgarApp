
package com.shiksharojgar.app

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

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
                        imageUrl = doc.getString("imageUrl").orEmpty(),
                        fileUrl = doc.getString("fileUrl").orEmpty(),
                        fileName = doc.getString("fileName").orEmpty(),
                        imageMime = doc.getString("imageMime")
                            ?: "image/jpeg",
                        fileMime = doc.getString("fileMime")
                            ?: "application/pdf",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        pinned = doc.getBoolean("pinned") ?: false,
                        pinOrder = doc.getLong("pinOrder") ?: 0L,
                        enabled = doc.getBoolean("enabled") ?: true
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

        document.set(
            mapOf(
                "category" to savedPost.category,
                "title" to savedPost.title,
                "body" to savedPost.body,
                "imageUrl" to savedPost.imageUrl,
                "fileUrl" to savedPost.fileUrl,
                "fileName" to savedPost.fileName,
                "imageMime" to savedPost.imageMime,
                "fileMime" to savedPost.fileMime,
                "createdAt" to savedPost.createdAt,
                "pinned" to savedPost.pinned,
                "pinOrder" to savedPost.pinOrder,
                "enabled" to savedPost.enabled
            )
        ).addOnSuccessListener {
            onResult(true, document.id)
        }.addOnFailureListener { error ->
            onResult(false, error.message)
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

    // केवल चुनी हुई पोस्ट हटाएँ।
    fun deletePost(
        postId: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        postsRef.document(postId)
            .delete()
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener { error ->
                onResult(false, error.message)
            }
    }
}
