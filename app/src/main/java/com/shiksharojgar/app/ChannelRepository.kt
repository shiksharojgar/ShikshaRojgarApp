package com.shiksharojgar.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.TimeUnit
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.Blob

object ChannelRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun ensureSignedIn(done: (Boolean) -> Unit) {
        if (auth.currentUser != null) { done(true); return }
        auth.signInAnonymously().addOnCompleteListener { done(it.isSuccessful) }
    }

    fun posts(onResult: (List<ChannelPost>) -> Unit, onError: (Exception) -> Unit) {
        fun mapPosts(snap: com.google.firebase.firestore.QuerySnapshot): List<ChannelPost> = snap.documents.map { d ->
            ChannelPost(d.id, d.getString("title").orEmpty(), d.getString("body").orEmpty(),
                d.getString("imageUrl").orEmpty(), d.getString("fileUrl").orEmpty(), d.getString("fileName").orEmpty(),
                d.getString("imageMime") ?: "image/jpeg", d.getString("fileMime") ?: "application/pdf",
                d.getString("category") ?: "आदेश/निर्देश", d.getLong("createdAt") ?: 0L,
                d.getBoolean("commentsEnabled") ?: true, d.getLong("likeCount") ?: 0L, d.getLong("commentCount") ?: 0L, d.getLong("viewCount") ?: 0L, d.getLong("shareCount") ?: 0L)
        }.sortedBy { it.createdAt }

        db.collection("channel_posts").orderBy("createdAt", Query.Direction.ASCENDING).limit(100)
            .addSnapshotListener { snap, err ->
                if (err == null) { onResult(mapPosts(snap ?: return@addSnapshotListener)); return@addSnapshotListener }
                // Fallback for older/mixed posts where createdAt/index data is inconsistent.
                // This keeps text-only posts visible instead of making the whole feed blank.
                db.collection("channel_posts").limit(100).get()
                    .addOnSuccessListener { fallback -> onResult(mapPosts(fallback)) }
                    .addOnFailureListener { fallbackErr -> onError(fallbackErr) }
            }
    }

    fun config(onResult: (Long, Boolean) -> Unit) {
        db.document("channel_config/main").addSnapshotListener { d, _ ->
            onResult(d?.getLong("followerCount") ?: 0L, d?.getBoolean("commentsEnabled") ?: true)
        }
    }

    fun follow(follow: Boolean, done: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return done(false)
        val ref = db.collection("channel_followers").document(uid)
        val configRef = db.document("channel_config/main")
        // IMPORTANT: the follower counter must change only when the user's
        // actual follow document changes. This prevents duplicate Follow taps
        // and repeated Unfollow calls from pushing the count into negative values.
        db.runTransaction { tx ->
            val followerSnap = tx.get(ref)
            val configSnap = tx.get(configRef)
            val currentCount = (configSnap.getLong("followerCount") ?: 0L).coerceAtLeast(0L)

            when {
                follow && !followerSnap.exists() -> {
                    tx.set(ref, mapOf("uid" to uid, "joinedAt" to System.currentTimeMillis()))
                    tx.set(configRef, mapOf("followerCount" to currentCount + 1L), com.google.firebase.firestore.SetOptions.merge())
                }
                !follow && followerSnap.exists() -> {
                    tx.delete(ref)
                    tx.set(configRef, mapOf("followerCount" to (currentCount - 1L).coerceAtLeast(0L)), com.google.firebase.firestore.SetOptions.merge())
                }
                else -> {
                    // Idempotent: already followed / already unfollowed.
                    if (currentCount != (configSnap.getLong("followerCount") ?: 0L)) {
                        tx.set(configRef, mapOf("followerCount" to currentCount), com.google.firebase.firestore.SetOptions.merge())
                    }
                }
            }
            null
        }.addOnSuccessListener { done(true) }
         .addOnFailureListener { done(false) }
    }

    fun isFollowing(done: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return done(false)
        db.collection("channel_followers").document(uid).get().addOnSuccessListener { done(it.exists()) }.addOnFailureListener { done(false) }
    }

    fun like(postId: String, liked: Boolean, done: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return done(false)
        val ref = db.collection("channel_likes").document(postId).collection("users").document(uid)
        val postRef = db.collection("channel_posts").document(postId)
        val configRef = db.document("channel_config/main")
        db.runTransaction { tx ->
            val likeSnap = tx.get(ref)
            val postSnap = tx.get(postRef)
            val configSnap = tx.get(configRef)
            val current = (postSnap.getLong("likeCount") ?: 0L).coerceAtLeast(0L)
            val total = (configSnap.getLong("totalLikes") ?: 0L).coerceAtLeast(0L)
            when {
                liked && !likeSnap.exists() -> {
                    tx.set(ref, mapOf("uid" to uid, "at" to System.currentTimeMillis()))
                    tx.update(postRef, "likeCount", current + 1L)
                    tx.set(configRef, mapOf("totalLikes" to total + 1L), com.google.firebase.firestore.SetOptions.merge())
                }
                !liked && likeSnap.exists() -> {
                    tx.delete(ref)
                    tx.update(postRef, "likeCount", (current - 1L).coerceAtLeast(0L))
                    tx.set(configRef, mapOf("totalLikes" to (total - 1L).coerceAtLeast(0L)), com.google.firebase.firestore.SetOptions.merge())
                }
            }
            null
        }.addOnSuccessListener { done(true) }.addOnFailureListener { done(false) }
    }

    fun isLiked(postId: String, done: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return done(false)
        db.collection("channel_likes").document(postId).collection("users").document(uid).get()
            .addOnSuccessListener { done(it.exists()) }.addOnFailureListener { done(false) }
    }

    fun comments(postId: String, onResult: (List<Map<String, Any>>) -> Unit) {
        db.collection("channel_posts").document(postId).collection("comments").orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ -> onResult(snap?.documents?.map { it.data ?: emptyMap() }.orEmpty()) }
    }

    fun addComment(postId: String, text: String, done: (Boolean) -> Unit) {
        val user = auth.currentUser ?: return done(false)
        db.collection("channel_posts").document(postId).collection("comments").add(
            mapOf("uid" to user.uid, "text" to text, "name" to "ऐप उपयोगकर्ता", "createdAt" to System.currentTimeMillis())
        ).addOnSuccessListener {
            db.collection("channel_posts").document(postId).update(
                "commentCount", com.google.firebase.firestore.FieldValue.increment(1)
            ).addOnCompleteListener {
                db.document("channel_config/main").update("totalComments", com.google.firebase.firestore.FieldValue.increment(1))
                done(true)
            }
        }.addOnFailureListener { done(false) }
    }

    /**
     * Free/Spark-compatible media upload. Firebase Storage is deliberately not used.
     * The file is split into Firestore Blob chunks, each well below the 1 MiB document limit.
     */
    fun upload(uri: Uri, folder: String, done: (String?, String?) -> Unit) {
        try {
            val resolver = appContextResolver
            val name = "${System.currentTimeMillis()}_${UUID.randomUUID()}"
            val mediaId = name
            val mime = resolver.getType(uri) ?: if (folder == "images") "image/jpeg" else "application/pdf"
            val fileName = queryDisplayName(resolver, uri)
            val size = querySize(resolver, uri)
            val maxBytes = 20L * 1024L * 1024L
            if (size > maxBytes) { done(null, "File 20 MB से बड़ी है। कृपया छोटा file चुनें।"); return }
            val mediaRef = db.collection("channel_media").document(mediaId)
            mediaRef.set(mapOf(
                "mediaId" to mediaId, "mimeType" to mime, "fileName" to fileName,
                "size" to size, "folder" to folder, "createdAt" to System.currentTimeMillis()
            )).addOnSuccessListener {
                Thread {
                    try {
                        resolver.openInputStream(uri)?.use { input ->
                            val bufferSize = 700 * 1024
                            val buffer = ByteArray(bufferSize)
                            var index = 0
                            while (true) {
                                val read = input.read(buffer)
                                if (read <= 0) break
                                val bytes = if (read == buffer.size) buffer.copyOf() else buffer.copyOf(read)
                                Tasks.await(mediaRef.collection("chunks").document(index.toString()).set(mapOf(
                                    "index" to index, "data" to Blob.fromBytes(bytes)
                                )), 60, TimeUnit.SECONDS)
                                index++
                            }
                            if (index == 0) throw IllegalArgumentException("खाली file")
                            runOnMain { done("firestore-media://$mediaId", null) }
                        } ?: throw IllegalArgumentException("File पढ़ी नहीं जा सकी")
                    } catch (e: Exception) {
                        runOnMain { done(null, e.message ?: "Media upload failed") }
                    }
                }.start()
            }.addOnFailureListener { done(null, it.message ?: "Media metadata save failed") }
        } catch (e: Exception) { done(null, e.message ?: "Media upload failed") }
    }

    private val appContextResolver: android.content.ContentResolver
        get() = appContext.contentResolver
    private val appContext: Context by lazy {
        // Firebase has no Context accessor exposed directly; use the application context from FirebaseApp.
        com.google.firebase.FirebaseApp.getInstance().applicationContext
    }
    private fun queryDisplayName(resolver: android.content.ContentResolver, uri: Uri): String {
        var n = "document.pdf"
        resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) n = c.getString(0) ?: n
        }
        return n
    }
    private fun querySize(resolver: android.content.ContentResolver, uri: Uri): Long {
        resolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) return c.getLong(0)
        }
        return -1L
    }
    private fun runOnMain(block: () -> Unit) { android.os.Handler(android.os.Looper.getMainLooper()).post(block) }

    fun loadImage(mediaRef: String, done: (android.graphics.Bitmap?, String?) -> Unit) {
        Thread {
            try {
                val bytes: ByteArray = if (mediaRef.startsWith("firestore-media://")) {
                    val id = mediaRef.removePrefix("firestore-media://")
                    val mediaRefDb = db.collection("channel_media").document(id)
                    val chunks = Tasks.await(mediaRefDb.collection("chunks").orderBy("index").get(), 60, TimeUnit.SECONDS)
                    val out = java.io.ByteArrayOutputStream()
                    chunks.documents.forEach { d -> d.getBlob("data")?.toBytes()?.let(out::write) }
                    out.toByteArray()
                } else {
                    val c = URL(mediaRef).openConnection() as HttpURLConnection
                    c.connectTimeout = 20000
                    c.readTimeout = 30000
                    c.instanceFollowRedirects = true
                    c.inputStream.use { it.readBytes() }.also { c.disconnect() }
                }
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                runOnMain { done(bitmap, if (bitmap == null) "Image decode नहीं हो सकी" else null) }
            } catch (e: Exception) {
                runOnMain { done(null, e.message ?: "Image नहीं खुली") }
            }
        }.start()
    }

    fun openMedia(context: Context, mediaRef: String, mimeType: String, fileName: String, done: (Boolean, String?) -> Unit) {
        if (!mediaRef.startsWith("firestore-media://")) {
            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mediaRef))); done(true, null) } catch (e: Exception) { done(false, e.message) }
            return
        }
        Thread {
            try {
                val id = mediaRef.removePrefix("firestore-media://")
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "ShikshaRojgarMedia")
                dir.mkdirs()
                val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "document" }
                val out = File(dir, "${id}_$safeName")
                readMediaToFile(id, out)
                val contentUri = androidx.core.content.FileProvider.getUriForFile(context, "com.shiksharojgar.app.fileprovider", out)
                runOnMain {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(contentUri, mimeType.ifBlank { "application/octet-stream" }); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) })
                        done(true, null)
                    } catch (e: Exception) { done(false, "इस file को खोलने के लिए उपयुक्त viewer नहीं मिला") }
                }
            } catch (e: Exception) { runOnMain { done(false, e.message ?: "Media पढ़ी नहीं जा सकी") } }
        }.start()
    }

    private fun readMediaToFile(mediaId: String, out: File) {
        val mediaRef = db.collection("channel_media").document(mediaId)
        val chunks = Tasks.await(mediaRef.collection("chunks").orderBy("index").get(), 60, TimeUnit.SECONDS)
        out.outputStream().use { output ->
            chunks.documents.forEach { d -> d.getBlob("data")?.toBytes()?.let(output::write) }
        }
    }

    fun createPost(data: Map<String, Any>, done: (Boolean, String?) -> Unit) {
        db.collection("channel_posts").add(data).addOnCompleteListener { done(it.isSuccessful, it.exception?.message) }
    }

    fun updatePost(id: String, data: Map<String, Any>, done: (Boolean, String?) -> Unit) {
        db.collection("channel_posts").document(id).update(data).addOnCompleteListener { done(it.isSuccessful, it.exception?.message) }
    }

    fun deletePost(id: String, done: (Boolean) -> Unit) { db.collection("channel_posts").document(id).delete().addOnCompleteListener { done(it.isSuccessful) } }

    fun saveOffline(context: Context, post: ChannelPost, done: (Boolean, String?) -> Unit) {
        Thread {
            try {
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "ShikshaRojgarOffline")
                if (!dir.exists()) dir.mkdirs()
                val folder = File(dir, post.id.ifBlank { System.currentTimeMillis().toString() }); folder.mkdirs()
                File(folder, "post.txt").writeText("${post.title}\n\n${post.body}\n\nश्रेणी: ${post.category}")
                if (post.imageUrl.isNotBlank()) download(post.imageUrl, File(folder, "image.jpg"))
                if (post.fileUrl.isNotBlank()) download(post.fileUrl, File(folder, post.fileName.ifBlank { "document.pdf" }))
                OfflineStore.mark(context, post, folder.absolutePath)
                done(true, null)
            } catch (e: Exception) { done(false, e.message) }
        }.start()
    }

    fun openOffline(context: Context, post: ChannelPost): File? = OfflineStore.path(context, post.id)?.let(::File)

    private fun download(url: String, target: File) {
        if (url.startsWith("firestore-media://")) {
            readMediaToFile(url.removePrefix("firestore-media://"), target)
            return
        }
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 20000; c.readTimeout = 30000
        c.inputStream.use { input -> target.outputStream().use { output -> input.copyTo(output) } }
        c.disconnect()
    }
}
