package com.shiksharojgar.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.android.gms.tasks.Tasks
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.TimeUnit

object ChannelRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ---------------------------------------------------------
    // AUTH
    // ---------------------------------------------------------

    fun ensureSignedIn(done: (Boolean) -> Unit) {
        if (auth.currentUser != null) {
            done(true)
            return
        }

        auth.signInAnonymously()
            .addOnCompleteListener {
                done(it.isSuccessful)
            }
    }

    // ---------------------------------------------------------
    // POSTS
    // ---------------------------------------------------------

    fun posts(
        onResult: (List<ChannelPost>) -> Unit,
        onError: (Exception) -> Unit
    ) {

        fun mapPosts(
            snap: com.google.firebase.firestore.QuerySnapshot
        ): List<ChannelPost> {

            return snap.documents.map { d ->

                ChannelPost(
                    d.id,

                    d.getString("title").orEmpty(),

                    d.getString("body").orEmpty(),

                    d.getString("imageUrl").orEmpty(),

                    d.getString("fileUrl").orEmpty(),

                    d.getString("fileName").orEmpty(),

                    d.getString("imageMime") ?: "image/jpeg",

                    d.getString("fileMime") ?: "application/pdf",

                    d.getString("category") ?: "आदेश/निर्देश",

                    d.getLong("createdAt") ?: 0L,

                    d.getBoolean("commentsEnabled") ?: true,

                    d.getLong("likeCount") ?: 0L,

                    d.getLong("commentCount") ?: 0L,

                    d.getLong("viewCount") ?: 0L,

                    d.getLong("shareCount") ?: 0L
                )

            }.sortedBy {
                it.createdAt
            }
        }

        db.collection("channel_posts")
            .orderBy(
    "createdAt",
    Query.Direction.DESCENDING
            )
            .limit(100)
            .addSnapshotListener { snap, err ->

                if (err == null) {

                    if (snap != null) {
                        onResult(mapPosts(snap))
                    }

                    return@addSnapshotListener
                }

                // Fallback for older/mixed posts
                db.collection("channel_posts")
                    .limit(100)
                    .get()
                    .addOnSuccessListener { fallback ->
                        onResult(mapPosts(fallback))
                    }
                    .addOnFailureListener { fallbackErr ->
                        onError(fallbackErr)
                    }
            }
    }

    // ---------------------------------------------------------
    // CHANNEL CONFIG
    // ---------------------------------------------------------

    fun config(
        onResult: (Long, Boolean) -> Unit
    ) {

        db.document("channel_config/main")
            .addSnapshotListener { d, _ ->

                onResult(
                    d?.getLong("followerCount") ?: 0L,
                    d?.getBoolean("commentsEnabled") ?: true
                )
            }
    }

    // ---------------------------------------------------------
    // FOLLOW
    // ---------------------------------------------------------

    fun follow(
        follow: Boolean,
        done: (Boolean) -> Unit
    ) {

        val uid = auth.currentUser?.uid

        if (uid.isNullOrBlank()) {
            done(false)
            return
        }

        val ref =
            db.collection("channel_followers")
                .document(uid)

        val configRef =
            db.document("channel_config/main")

        db.runTransaction { tx ->

            val followerSnap =
                tx.get(ref)

            val configSnap =
                tx.get(configRef)

            val currentCount =
                (configSnap.getLong("followerCount") ?: 0L)
                    .coerceAtLeast(0L)

            when {

                follow && !followerSnap.exists() -> {

                    tx.set(
                        ref,
                        mapOf(
                            "uid" to uid,
                            "joinedAt" to System.currentTimeMillis()
                        )
                    )

                    tx.set(
                        configRef,
                        mapOf(
                            "followerCount" to currentCount + 1L
                        ),
                        SetOptions.merge()
                    )
                }

                !follow && followerSnap.exists() -> {

                    tx.delete(ref)

                    tx.set(
                        configRef,
                        mapOf(
                            "followerCount" to
                                    (currentCount - 1L)
                                        .coerceAtLeast(0L)
                        ),
                        SetOptions.merge()
                    )
                }

                else -> {
                    // Already followed / already unfollowed.
                    // Do nothing.
                }
            }

            null

        }.addOnSuccessListener {

            done(true)

        }.addOnFailureListener {

            done(false)
        }
    }

    // ---------------------------------------------------------
    // CHECK FOLLOW
    // ---------------------------------------------------------

    fun isFollowing(
        done: (Boolean) -> Unit
    ) {

        val uid = auth.currentUser?.uid

        if (uid.isNullOrBlank()) {
            done(false)
            return
        }

        db.collection("channel_followers")
            .document(uid)
            .get()
            .addOnSuccessListener {
                done(it.exists())
            }
            .addOnFailureListener {
                done(false)
            }
    }

    // ---------------------------------------------------------
    // LIKE
    // ---------------------------------------------------------

    fun like(
        postId: String,
        liked: Boolean,
        done: (Boolean) -> Unit
    ) {

        val uid = auth.currentUser?.uid

        if (uid.isNullOrBlank()) {
            done(false)
            return
        }

        val ref =
            db.collection("channel_likes")
                .document(postId)
                .collection("users")
                .document(uid)

        val postRef =
            db.collection("channel_posts")
                .document(postId)

        val configRef =
            db.document("channel_config/main")

        db.runTransaction { tx ->

            val likeSnap =
                tx.get(ref)

            val postSnap =
                tx.get(postRef)

            val configSnap =
                tx.get(configRef)

            val current =
                (postSnap.getLong("likeCount") ?: 0L)
                    .coerceAtLeast(0L)

            val total =
                (configSnap.getLong("totalLikes") ?: 0L)
                    .coerceAtLeast(0L)

            when {

                liked && !likeSnap.exists() -> {

                    tx.set(
                        ref,
                        mapOf(
                            "uid" to uid,
                            "at" to System.currentTimeMillis()
                        )
                    )

                    tx.update(
                        postRef,
                        "likeCount",
                        current + 1L
                    )

                    tx.set(
                        configRef,
                        mapOf(
                            "totalLikes" to total + 1L
                        ),
                        SetOptions.merge()
                    )
                }

                !liked && likeSnap.exists() -> {

                    tx.delete(ref)

                    tx.update(
                        postRef,
                        "likeCount",
                        (current - 1L).coerceAtLeast(0L)
                    )

                    tx.set(
                        configRef,
                        mapOf(
                            "totalLikes" to
                                    (total - 1L)
                                        .coerceAtLeast(0L)
                        ),
                        SetOptions.merge()
                    )
                }

                else -> {
                    // Idempotent.
                }
            }

            null

        }.addOnSuccessListener {

            done(true)

        }.addOnFailureListener {

            done(false)
        }
    }

    // ---------------------------------------------------------
    // CHECK LIKE
    // ---------------------------------------------------------

    fun isLiked(
        postId: String,
        done: (Boolean) -> Unit
    ) {

        val uid = auth.currentUser?.uid

        if (uid.isNullOrBlank()) {
            done(false)
            return
        }

        db.collection("channel_likes")
            .document(postId)
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener {
                done(it.exists())
            }
            .addOnFailureListener {
                done(false)
            }
    }

    // ---------------------------------------------------------
    // COMMENTS
    // ---------------------------------------------------------

    fun comments(
        postId: String,
        onResult: (List<Map<String, Any>>) -> Unit
    ) {

        db.collection("channel_posts")
            .document(postId)
            .collection("comments")
            .orderBy(
                "createdAt",
                Query.Direction.ASCENDING
            )
            .addSnapshotListener { snap, _ ->

                onResult(
                    snap?.documents
                        ?.map {
                            it.data ?: emptyMap()
                        }
                        .orEmpty()
                )
            }
    }

    // ---------------------------------------------------------
    // ADD COMMENT
    // ---------------------------------------------------------

    fun addComment(
        postId: String,
        text: String,
        done: (Boolean) -> Unit
    ) {

        val user =
            auth.currentUser

        if (user == null) {
            done(false)
            return
        }

        db.collection("channel_posts")
            .document(postId)
            .collection("comments")
            .add(
                mapOf(
                    "uid" to user.uid,
                    "text" to text,
                    "name" to "ऐप उपयोगकर्ता",
                    "createdAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {

                db.collection("channel_posts")
                    .document(postId)
                    .update(
                        "commentCount",
                        com.google.firebase.firestore.FieldValue.increment(1)
                    )
                    .addOnCompleteListener {

                        db.document("channel_config/main")
                            .set(
                                mapOf(
                                    "totalComments" to
                                            com.google.firebase.firestore.FieldValue.increment(
                                                1
                                            )
                                ),
                                SetOptions.merge()
                            )

                        done(true)
                    }

            }
            .addOnFailureListener {

                done(false)
            }
    }

    // ---------------------------------------------------------
    // FIRESTORE MEDIA UPLOAD
    // ---------------------------------------------------------

    /**
     * Spark/free compatible media upload.
     *
     * Firebase Storage is NOT used.
     *
     * Maximum file size = 20 MB
     * Chunk size = 700 KB
     */
    fun upload(
        uri: Uri,
        folder: String,
        done: (String?, String?) -> Unit
    ) {

        try {

            val resolver =
                appContextResolver

            val mediaId =
                "${System.currentTimeMillis()}_${UUID.randomUUID()}"

            val mime =
                resolver.getType(uri)
                    ?: if (folder == "images")
                        "image/jpeg"
                    else
                        "application/pdf"

            val fileName =
                queryDisplayName(
                    resolver,
                    uri
                )

            val size =
                querySize(
                    resolver,
                    uri
                )

            val maxBytes =
                20L * 1024L * 1024L

            if (size > maxBytes) {

                done(
                    null,
                    "File 20 MB से बड़ी है। कृपया छोटा file चुनें।"
                )

                return
            }

            val mediaRef =
                db.collection("channel_media")
                    .document(mediaId)

            mediaRef.set(
                mapOf(
                    "mediaId" to mediaId,
                    "mimeType" to mime,
                    "fileName" to fileName,
                    "size" to size,
                    "folder" to folder,
                    "createdAt" to System.currentTimeMillis()
                )
            )
                .addOnSuccessListener {

                    Thread {

                        try {

                            resolver
                                .openInputStream(uri)
                                ?.use { input ->

                                    val bufferSize =
                                        700 * 1024

                                    val buffer =
                                        ByteArray(bufferSize)

                                    var index = 0

                                    while (true) {

                                        val read =
                                            input.read(buffer)

                                        if (read <= 0) {
                                            break
                                        }

                                        val bytes =
                                            if (read == buffer.size) {
                                                buffer.copyOf()
                                            } else {
                                                buffer.copyOf(read)
                                            }

                                        Tasks.await(
                                            mediaRef
                                                .collection("chunks")
                                                .document(index.toString())
                                                .set(
                                                    mapOf(
                                                        "index" to index,
                                                        "data" to Blob.fromBytes(bytes)
                                                    )
                                                ),
                                            60,
                                            TimeUnit.SECONDS
                                        )

                                        index++
                                    }

                                    if (index == 0) {
                                        throw IllegalArgumentException(
                                            "खाली file"
                                        )
                                    }

                                    runOnMain {

                                        done(
                                            "firestore-media://$mediaId",
                                            null
                                        )
                                    }

                                }
                                ?: throw IllegalArgumentException(
                                    "File पढ़ी नहीं जा सकी"
                                )

                        } catch (e: Exception) {

                            runOnMain {

                                done(
                                    null,
                                    e.message
                                        ?: "Media upload failed"
                                )
                            }
                        }

                    }.start()
                }
                .addOnFailureListener {

                    done(
                        null,
                        it.message
                            ?: "Media metadata save failed"
                    )
                }

        } catch (e: Exception) {

            done(
                null,
                e.message
                    ?: "Media upload failed"
            )
        }
    }

    // ---------------------------------------------------------
    // CONTENT RESOLVER
    // ---------------------------------------------------------

    private val appContextResolver:
            android.content.ContentResolver
        get() = appContext.contentResolver

    private val appContext: Context by lazy {

        com.google.firebase.FirebaseApp
            .getInstance()
            .applicationContext
    }

    private fun queryDisplayName(
        resolver: android.content.ContentResolver,
        uri: Uri
    ): String {

        var name =
            "document.pdf"

        resolver.query(
            uri,
            arrayOf(
                android.provider.OpenableColumns.DISPLAY_NAME
            ),
            null,
            null,
            null
        )?.use { cursor ->

            if (cursor.moveToFirst()) {

                name =
                    cursor.getString(0)
                        ?: name
            }
        }

        return name
    }

    private fun querySize(
        resolver: android.content.ContentResolver,
        uri: Uri
    ): Long {

        resolver.query(
            uri,
            arrayOf(
                android.provider.OpenableColumns.SIZE
            ),
            null,
            null,
            null
        )?.use { cursor ->

            if (
                cursor.moveToFirst() &&
                !cursor.isNull(0)
            ) {

                return cursor.getLong(0)
            }
        }

        return -1L
    }

    private fun runOnMain(
        block: () -> Unit
    ) {

        android.os.Handler(
            android.os.Looper.getMainLooper()
        ).post(block)
    }

    // ---------------------------------------------------------
    // LOAD IMAGE
    // ---------------------------------------------------------

    fun loadImage(
        mediaRef: String,
        done: (
            android.graphics.Bitmap?,
            String?
        ) -> Unit
    ) {

        Thread {

            try {

                val bytes: ByteArray

                if (
                    mediaRef.startsWith(
                        "firestore-media://"
                    )
                ) {

                    val id =
                        mediaRef.removePrefix(
                            "firestore-media://"
                        )

                    val mediaRefDb =
                        db.collection("channel_media")
                            .document(id)

                    val chunks =
                        Tasks.await(
                            mediaRefDb
                                .collection("chunks")
                                .orderBy("index")
                                .get(),
                            60,
                            TimeUnit.SECONDS
                        )

                    val out =
                        java.io.ByteArrayOutputStream()

                    chunks.documents.forEach { document ->

                        document
                            .getBlob("data")
                            ?.toBytes()
                            ?.let(out::write)
                    }

                    bytes =
                        out.toByteArray()

                } else {

                    val connection =
                        URL(mediaRef)
                            .openConnection()
                                as HttpURLConnection

                    connection.connectTimeout =
                        20000

                    connection.readTimeout =
                        30000

                    connection.instanceFollowRedirects =
                        true

                    bytes =
                        connection.inputStream
                            .use {
                                it.readBytes()
                            }

                    connection.disconnect()
                }

                val bitmap =
                    android.graphics.BitmapFactory
                        .decodeByteArray(
                            bytes,
                            0,
                            bytes.size
                        )

                runOnMain {

                    done(
                        bitmap,
                        if (bitmap == null)
                            "Image decode नहीं हो सकी"
                        else
                            null
                    )
                }

            } catch (e: Exception) {

                runOnMain {

                    done(
                        null,
                        e.message
                            ?: "Image नहीं खुली"
                    )
                }
            }

        }.start()
    }

    // ---------------------------------------------------------
    // OPEN MEDIA
    // ---------------------------------------------------------

    fun openMedia(
        context: Context,
        mediaRef: String,
        mimeType: String,
        fileName: String,
        done: (Boolean, String?) -> Unit
    ) {

        if (
            !mediaRef.startsWith(
                "firestore-media://"
            )
        ) {

            try {

                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(mediaRef)
                    )
                )

                done(true, null)

            } catch (e: Exception) {

                done(
                    false,
                    e.message
                )
            }

            return
        }

        Thread {

            try {

                val id =
                    mediaRef.removePrefix(
                        "firestore-media://"
                    )

                val dir =
                    File(
                        context.getExternalFilesDir(
                            Environment.DIRECTORY_DOWNLOADS
                        ),
                        "ShikshaRojgarMedia"
                    )

                dir.mkdirs()

                val safeName =
                    fileName
                        .replace(
                            Regex("[^A-Za-z0-9._-]"),
                            "_"
                        )
                        .ifBlank {
                            "document"
                        }

                val out =
                    File(
                        dir,
                        "${id}_$safeName"
                    )

                readMediaToFile(
                    id,
                    out
                )

                val contentUri =
                    androidx.core.content.FileProvider
                        .getUriForFile(
                            context,
                            "com.shiksharojgar.app.fileprovider",
                            out
                        )

                runOnMain {

                    try {

                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW
                            ).apply {

                                setDataAndType(
                                    contentUri,
                                    mimeType.ifBlank {
                                        "application/octet-stream"
                                    }
                                )

                                addFlags(
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            }
                        )

                        done(true, null)

                    } catch (e: Exception) {

                        done(
                            false,
                            "इस file को खोलने के लिए उपयुक्त viewer नहीं मिला"
                        )
                    }
                }

            } catch (e: Exception) {

                runOnMain {

                    done(
                        false,
                        e.message
                            ?: "Media पढ़ी नहीं जा सकी"
                    )
                }
            }

        }.start()
    }

    // ---------------------------------------------------------
    // READ FIRESTORE MEDIA TO FILE
    // ---------------------------------------------------------

    private fun readMediaToFile(
        mediaId: String,
        out: File
    ) {

        val mediaRef =
            db.collection("channel_media")
                .document(mediaId)

        val chunks =
            Tasks.await(
                mediaRef
                    .collection("chunks")
                    .orderBy("index")
                    .get(),
                60,
                TimeUnit.SECONDS
            )

        out.outputStream().use { output ->

            chunks.documents.forEach { document ->

                document
                    .getBlob("data")
                    ?.toBytes()
                    ?.let(output::write)
            }
        }
    }

    // ---------------------------------------------------------
    // CREATE POST
    // ---------------------------------------------------------

    fun createPost(
        data: Map<String, Any>,
        done: (Boolean, String?) -> Unit
    ) {

        db.collection("channel_posts")
            .add(data)
            .addOnCompleteListener {

                done(
                    it.isSuccessful,
                    it.exception?.message
                )
            }
    }

    // ---------------------------------------------------------
    // UPDATE POST
    // ---------------------------------------------------------

    fun updatePost(
        id: String,
        data: Map<String, Any>,
        done: (Boolean, String?) -> Unit
    ) {

        db.collection("channel_posts")
            .document(id)
            .update(data)
            .addOnCompleteListener {

                done(
                    it.isSuccessful,
                    it.exception?.message
                )
            }
    }

    // ---------------------------------------------------------
    // DELETE POST
    // ---------------------------------------------------------

    /**
     * Deletes a Channel post.
     *
     * If the post uses Firestore media, its media is also cleaned
     * after checking that no other post still references it.
     */
    fun deletePost(
        id: String,
        done: (Boolean) -> Unit
    ) {

        if (id.isBlank()) {
            done(false)
            return
        }

        val postRef =
            db.collection("channel_posts")
                .document(id)

        postRef.get()
            .addOnSuccessListener { post ->

                if (!post.exists()) {

                    done(false)
                    return@addOnSuccessListener
                }

                val mediaRefs =
                    listOf(
                        post.getString("imageUrl")
                            .orEmpty(),

                        post.getString("fileUrl")
                            .orEmpty()
                    )
                        .filter {
                            it.startsWith(
                                "firestore-media://"
                            )
                        }
                        .distinct()

                // Delete ONLY this post.
                postRef.delete()
                    .addOnSuccessListener {

                        if (mediaRefs.isEmpty()) {

                            done(true)
                            return@addOnSuccessListener
                        }

                        deleteMediaRefsSequentially(
                            mediaRefs,
                            0
                        ) { cleanupOk ->

                            /*
                             * The post itself has already been
                             * deleted successfully.
                             */
                            done(cleanupOk)
                        }
                    }
                    .addOnFailureListener {

                        done(false)
                    }
            }
            .addOnFailureListener {

                done(false)
            }
    }

    // ---------------------------------------------------------
    // DELETE CATEGORY POST + MEDIA CLEANUP
    // ---------------------------------------------------------

    fun deleteCategoryPost(
        id: String,
        done: (Boolean) -> Unit
    ) {
        if (id.isBlank()) {
            done(false)
            return
        }

        val postRef = db.collection("category_posts")
            .document(id)

        postRef.get()
            .addOnSuccessListener { post ->
                if (!post.exists()) {
                    done(false)
                    return@addOnSuccessListener
                }

                val mediaRefs = listOf(
                    post.getString("imageUrl").orEmpty(),
                    post.getString("fileUrl").orEmpty()
                )
                    .filter {
                        it.startsWith("firestore-media://")
                    }
                    .distinct()

                postRef.delete()
                    .addOnSuccessListener {
                        if (mediaRefs.isEmpty()) {
                            done(true)
                        } else {
                            deleteMediaRefsSequentially(
                                mediaRefs,
                                0
                            ) { cleanupOk ->
                                done(cleanupOk)
                            }
                        }
                    }
                    .addOnFailureListener {
                        done(false)
                    }
            }
            .addOnFailureListener {
                done(false)
            }
    }

    // ---------------------------------------------------------
    // DELETE MEDIA REFERENCES SEQUENTIALLY
    // ---------------------------------------------------------

    private fun deleteMediaRefsSequentially(
        refs: List<String>,
        index: Int,
        done: (Boolean) -> Unit
    ) {

        if (index >= refs.size) {

            done(true)
            return
        }

        deleteMediaIfUnused(
            refs[index]
        ) { ok ->

            if (!ok) {

                done(false)
                return@deleteMediaIfUnused
            }

            deleteMediaRefsSequentially(
                refs,
                index + 1,
                done
            )
        }
    }

    // ---------------------------------------------------------
    // DELETE MEDIA ONLY WHEN UNUSED
    // ---------------------------------------------------------

    private fun deleteMediaIfUnused(
        mediaUrl: String,
        done: (Boolean) -> Unit
    ) {

        if (
            !mediaUrl.startsWith(
                "firestore-media://"
            )
        ) {

            done(true)
            return
        }

        val mediaId =
            mediaUrl.removePrefix(
                "firestore-media://"
            )

        if (mediaId.isBlank()) {

            done(true)
            return
        }

        val imageQuery =
            db.collection("channel_posts")
                .whereEqualTo(
                    "imageUrl",
                    mediaUrl
                )
                .get()

        val fileQuery =
            db.collection("channel_posts")
                .whereEqualTo(
                    "fileUrl",
                    mediaUrl
                )
                .get()

        imageQuery
            .addOnSuccessListener { imageSnap ->

                fileQuery
                    .addOnSuccessListener { fileSnap ->

                        val stillReferenced =
                            imageSnap.documents.isNotEmpty() ||
                                    fileSnap.documents.isNotEmpty()

                        if (stillReferenced) {

                            // Another post still uses it.
                            done(true)
                            return@addOnSuccessListener
                        }

                        deleteMediaDocumentAndChunks(
                            mediaId,
                            done
                        )
                    }
                    .addOnFailureListener {

                        done(false)
                    }
            }
            .addOnFailureListener {

                done(false)
            }
    }

    // ---------------------------------------------------------
    // DELETE MEDIA DOCUMENT + CHUNKS
    // ---------------------------------------------------------

    private fun deleteMediaDocumentAndChunks(
        mediaId: String,
        done: (Boolean) -> Unit
    ) {

        val mediaRef =
            db.collection("channel_media")
                .document(mediaId)

        mediaRef.collection("chunks")
            .get()
            .addOnSuccessListener { chunks ->

                db.runBatch { batch ->

                    chunks.documents.forEach { chunk ->

                        batch.delete(
                            chunk.reference
                        )
                    }

                    batch.delete(mediaRef)

                }
                    .addOnSuccessListener {

                        done(true)
                    }
                    .addOnFailureListener {

                        done(false)
                    }
            }
            .addOnFailureListener {

                done(false)
            }
    }

    // ---------------------------------------------------------
    // OFFLINE SAVE
    // ---------------------------------------------------------

    fun saveOffline(
        context: Context,
        post: ChannelPost,
        done: (Boolean, String?) -> Unit
    ) {

        Thread {

            try {

                val root =
                    File(
                        context.getExternalFilesDir(
                            Environment.DIRECTORY_DOWNLOADS
                        ),
                        "ShikshaRojgarOffline"
                    )

                val folder =
                    File(
                        root,
                        post.id
                    )

                if (folder.exists()) {
                    folder.deleteRecursively()
                }

                folder.mkdirs()

                // Save text / post information.
                val textFile =
                    File(
                        folder,
                        "post.txt"
                    )

                textFile.writeText(
                    buildString {

                        appendLine(
                            post.title
                        )

                        appendLine()

                        appendLine(
                            post.body
                        )

                        appendLine()

                        appendLine(
                            "Category: ${post.category}"
                        )

                        appendLine(
                            "CreatedAt: ${post.createdAt}"
                        )
                    }
                )

                // Save image.
                if (
                    post.imageUrl.isNotBlank()
                ) {

                    val imageFile =
                        File(
                            folder,
                            "image.jpg"
                        )

                    download(
                        post.imageUrl,
                        imageFile
                    )
                }

                // Save PDF/document.
                if (
                    post.fileUrl.isNotBlank()
                ) {

                    val safeFileName =
                        post.fileName
                            .replace(
                                Regex("[^A-Za-z0-9._-]"),
                                "_"
                            )
                            .ifBlank {
                                "document.pdf"
                            }

                    val pdfFile =
                        File(
                            folder,
                            safeFileName
                        )

                    download(
                        post.fileUrl,
                        pdfFile
                    )
                }

                runOnMain {

                    done(
                        true,
                        null
                    )
                }

            } catch (e: Exception) {

                runOnMain {

                    done(
                        false,
                        e.message
                            ?: "Offline save failed"
                    )
                }
            }

        }.start()
    }

    // ---------------------------------------------------------
    // OPEN OFFLINE
    // ---------------------------------------------------------

    fun openOffline(
        context: Context,
        post: ChannelPost
    ): File? {

        return try {

            OfflineStore
                .path(
                    context,
                    post.id
                )
                ?.let {
                    File(it)
                }

        } catch (e: Exception) {

            null
        }
    }

    // ---------------------------------------------------------
    // DOWNLOAD
    // ---------------------------------------------------------

    private fun download(
        url: String,
        target: File
    ) {

        if (
            url.startsWith(
                "firestore-media://"
            )
        ) {

            val mediaId =
                url.removePrefix(
                    "firestore-media://"
                )

            readMediaToFile(
                mediaId,
                target
            )

            return
        }

        val connection =
            URL(url)
                .openConnection()
                    as HttpURLConnection

        connection.connectTimeout =
            20000

        connection.readTimeout =
            30000

        connection.instanceFollowRedirects =
            true

        try {

            connection.inputStream.use { input ->

                target.outputStream().use { output ->

                    input.copyTo(
                        output
                    )
                }
            }

        } finally {

            connection.disconnect()
        }
    }
}
