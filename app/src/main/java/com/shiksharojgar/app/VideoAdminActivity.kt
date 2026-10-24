package com.shiksharojgar.app

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class VideoAdminActivity : AppCompatActivity() {

    private val db by lazy {
        FirebaseFirestore.getInstance()
    }

    private lateinit var titleField: EditText
    private lateinit var urlField: EditText
    private lateinit var descriptionField: EditText
    private lateinit var thumbnailField: EditText
    private lateinit var postsList: LinearLayout
private lateinit var thumbnailPreview: ImageView
private var selectedGalleryUri: Uri? = null

private val galleryPicker =
    registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedGalleryUri = uri
            thumbnailPreview.setImageURI(uri)
        }
    }
    private var selectedId: String? = null

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(245, 248, 252))
        }

        root.addView(
            TextView(this).apply {
                text = "🎬 वीडियो पोस्ट मैनेजमेंट"
                textSize = 20f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setPadding(dp(10), dp(16), dp(10), dp(16))
                setBackgroundColor(Color.rgb(21, 101, 192))
            },
            LinearLayout.LayoutParams(-1, -2)
        )

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(18))
        }

        scroll.addView(content)

        titleField = field("वीडियो का टाइटल *")
        urlField = field("YouTube / Facebook / Instagram लिंक *")
        descriptionField = field("वीडियो का डिस्क्रिप्शन")
        thumbnailField = field("थंबनेल इमेज का HTTPS लिंक (वैकल्पिक)")

        content.addView(label("1. वीडियो का टाइटल"))
        content.addView(titleField)

        content.addView(label("2. वीडियो लिंक"))
        content.addView(urlField)

        content.addView(label("3. डिस्क्रिप्शन"))
        content.addView(descriptionField)

        content.addView(label("4. थंबनेल लिंक"))
        content.addView(thumbnailField)
        thumbnailPreview = ImageView(this).apply {
    scaleType = ImageView.ScaleType.FIT_CENTER
    setBackgroundColor(Color.rgb(230, 235, 242))
    contentDescription = "वीडियो इमेज Preview"
}

content.addView(
    thumbnailPreview,
    LinearLayout.LayoutParams(-1, dp(190)).apply {
        topMargin = dp(8)
        bottomMargin = dp(8)
    }
)

content.addView(
    Button(this).apply {
        text = "🖼️ Gallery से इमेज चुनें"
        setOnClickListener {
            galleryPicker.launch("image/*")
        }
    },
    LinearLayout.LayoutParams(-1, dp(48))
)

        val saveButton = Button(this).apply {
            text = "💾 वीडियो सेव / अपडेट करें"
            setOnClickListener {
                saveVideo()
            }
        }

        content.addView(
            saveButton,
            LinearLayout.LayoutParams(-1, dp(50)).apply {
                topMargin = dp(10)
                bottomMargin = dp(5)
            }
        )

        content.addView(
            Button(this).apply {
                text = "➕ नई पोस्ट के लिए फॉर्म साफ करें"
                setOnClickListener {
                    clearForm()
                }
            },
            LinearLayout.LayoutParams(-1, dp(46))
        )

        content.addView(
            label("📋 पहले से प्रकाशित वीडियो")
        )

        postsList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        content.addView(postsList)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        setContentView(root)

ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
    val statusBarHeight =
        insets.getInsets(WindowInsetsCompat.Type.statusBars()).top

    val navigationBarHeight =
        insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

    view.setPadding(
        0,
        statusBarHeight,
        0,
        navigationBarHeight
    )

    insets
}

ViewCompat.requestApplyInsets(root)

loadVideos()

    private fun field(hintText: String): EditText {
        return EditText(this).apply {
            hint = hintText
            textSize = 15f
            setSingleLine(false)
            maxLines = if (hintText.contains("डिस्क्रिप्शन")) 5 else 3
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
    }

    private fun label(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 15f
            setTextColor(Color.rgb(30, 55, 85))
            setPadding(0, dp(10), 0, dp(3))
        }
    }

    private fun saveVideo() {
        val title = titleField.text.toString().trim()
        val url = urlField.text.toString().trim()
        val description =
            descriptionField.text.toString().trim()
        val thumbnail =
            thumbnailField.text.toString().trim()

        if (title.isBlank()) {
            titleField.error = "टाइटल लिखें"
            return
        }

        val uri = try {
            android.net.Uri.parse(url)
        } catch (_: Exception) {
            null
        }

        if (
            uri == null ||
            uri.scheme !in listOf("https", "http") ||
            uri.host.isNullOrBlank()
        ) {
            urlField.error = "सही वीडियो लिंक पेस्ट करें"
            return
        }

        if (thumbnail.isNotBlank()) {
            val imageUri = android.net.Uri.parse(thumbnail)

            if (
                imageUri.scheme != "https" ||
                imageUri.host.isNullOrBlank()
            ) {
                thumbnailField.error =
                    "थंबनेल लिंक HTTPS होना चाहिए"
                return
            }
        }
val selectedUri = selectedGalleryUri

if (selectedUri != null) {
    ChannelRepository.upload(selectedUri, "images") { mediaRef, error ->
        if (mediaRef.isNullOrBlank()) {
            Toast.makeText(
                this,
                "इमेज अपलोड नहीं हुई: ${error ?: "अज्ञात त्रुटि"}",
                Toast.LENGTH_LONG
            ).show()
        } else {
            saveVideoRecord(
                title,
                url,
                description,
                mediaRef
            )
        }
    }
    return
}

val thumbnail = thumbnailField.text.toString().trim()

if (thumbnail.isNotBlank()) {
    val imageUri = Uri.parse(thumbnail)
    val isFirestoreMedia = thumbnail.startsWith("firestore-media://")

    if (!isFirestoreMedia &&
        (imageUri.scheme != "https" || imageUri.host.isNullOrBlank())
    ) {
        thumbnailField.error =
            "HTTPS इमेज लिंक दें या Gallery से इमेज चुनें"
        return
    }
}

saveVideoRecord(title, url, description, thumbnail)

        
private fun saveVideo() {
    val title = titleField.text.toString().trim()
    val url = urlField.text.toString().trim()
    val description = descriptionField.text.toString().trim()

    if (title.isBlank()) {
        titleField.error = "टाइटल लिखें"
        return
    }

    val videoUri = Uri.parse(url)

    if (
        videoUri.scheme !in listOf("https", "http") ||
        videoUri.host.isNullOrBlank()
    ) {
        urlField.error = "सही वीडियो लिंक पेस्ट करें"
        return
    }

    val selectedUri = selectedGalleryUri

    if (selectedUri != null) {
        ChannelRepository.upload(selectedUri, "images") { mediaRef, error ->
            if (mediaRef.isNullOrBlank()) {
                Toast.makeText(
                    this,
                    "इमेज अपलोड नहीं हुई: ${error ?: "अज्ञात त्रुटि"}",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                saveVideoRecord(title, url, description, mediaRef)
            }
        }
        return
    }

    val thumbnail = thumbnailField.text.toString().trim()

    if (thumbnail.isNotBlank()) {
        val imageUri = Uri.parse(thumbnail)
        val isFirestoreMedia =
            thumbnail.startsWith("firestore-media://")

        if (
            !isFirestoreMedia &&
            (
                imageUri.scheme != "https" ||
                imageUri.host.isNullOrBlank()
            )
        ) {
            thumbnailField.error =
                "HTTPS इमेज लिंक दें या Gallery से इमेज चुनें"
            return
        }
    }

    saveVideoRecord(title, url, description, thumbnail)
}

  
private fun saveVideoRecord(
    title: String,
    url: String,
    description: String,
    thumbnail: String
) {
    val data = hashMapOf<String, Any>(
        "title" to title,
        "url" to url,
        "description" to description,
        "thumbnailUrl" to thumbnail,
        "enabled" to true,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    val id = selectedId

    val reference = if (id.isNullOrBlank()) {
        db.collection("video_posts").document()
    } else {
        db.collection("video_posts").document(id)
    }

    if (id.isNullOrBlank()) {
        data["createdAt"] = FieldValue.serverTimestamp()
    }

    reference.set(data, SetOptions.merge())
        .addOnSuccessListener {
            Toast.makeText(
                this,
                "वीडियो पोस्ट सेव हो गई।",
                Toast.LENGTH_SHORT
            ).show()

            clearForm()
            loadPosts()
        }
        .addOnFailureListener { error ->
            Toast.makeText(
                this,
                "सेव नहीं हुआ: ${error.message}",
                Toast.LENGTH_LONG
            ).show()
        }
}
  
        
private fun updateThumbnailPreview() {
    val custom = thumbnailField.text.toString().trim()

    val source = custom.ifBlank {
        youtubeThumbnail(urlField.text.toString().trim()).orEmpty()
    }

    if (selectedGalleryUri != null) {
        thumbnailPreview.setImageURI(selectedGalleryUri)
        return
    }

    if (source.isBlank()) {
        thumbnailPreview.setImageDrawable(null)
        return
    }

    ChannelRepository.loadImage(source) { bitmap, _ ->
        if (!isFinishing && bitmap != null) {
            thumbnailPreview.setImageBitmap(bitmap)
        }
    }
}

private fun youtubeThumbnail(url: String): String? {
    val id = Regex(
        """(?:youtu\.be/|[?&]v=|youtube\.com/(?:embed|shorts)/)([A-Za-z0-9_-]{11})"""
    ).find(url)?.groupValues?.getOrNull(1)

    return id?.let {
        "https://img.youtube.com/vi/$it/hqdefault.jpg"
    }
}
    private fun loadPosts() {
        postsList.removeAllViews()

        db.collection("video_posts")
            .get()
            .addOnSuccessListener { snapshot ->

                postsList.removeAllViews()

                val documents = snapshot.documents.sortedByDescending {
                    it.getTimestamp("createdAt")?.seconds ?: 0L
                }

                if (documents.isEmpty()) {
                    postsList.addView(
                        label("अभी कोई वीडियो पोस्ट नहीं है।")
                    )
                    return@addOnSuccessListener
                }

                documents.forEach { document ->

                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(
                            dp(10), dp(8), dp(10), dp(8)
                        )
                        setBackgroundColor(Color.WHITE)
                    }

                    row.addView(
                        TextView(this).apply {
                            text = "🎬 ${
                                document.getString("title")
                                    ?: "वीडियो"
                            }"
                            textSize = 16f
                            setTextColor(Color.rgb(25, 50, 80))
                        }
                    )

                    val actions = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                    }

                    actions.addView(
                        Button(this).apply {
                            text = "✏️ एडिट"
                            setOnClickListener {
                                selectedId = document.id
                                titleField.setText(
                                    document.getString("title").orEmpty()
                                )
                                urlField.setText(
                                    document.getString("url").orEmpty()
                                )
                                descriptionField.setText(
                                    document.getString("description").orEmpty()
                                )
                                thumbnailField.setText(
                                    document.getString("thumbnailUrl").orEmpty()
                                )

                                Toast.makeText(
                                    this@VideoAdminActivity,
                                    "अब बदलाव करके सेव करें।",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        LinearLayout.LayoutParams(0, dp(46), 1f)
                    )

                    actions.addView(
                        Button(this).apply {
                            text = "🗑️ डिलीट"
                            setTextColor(Color.rgb(180, 25, 25))
                            setOnClickListener {
                                AlertDialog.Builder(
                                    this@VideoAdminActivity
                                )
                                    .setTitle("वीडियो डिलीट करें?")
                                    .setMessage(
                                        "यह वीडियो पोस्ट ऐप की सूची से हट जाएगी।"
                                    )
                                    .setNegativeButton("रद्द करें", null)
                                    .setPositiveButton("डिलीट करें") { _, _ ->
                                        document.reference.delete()
                                            .addOnSuccessListener {
                                                if (selectedId == document.id) {
                                                    clearForm()
                                                }
                                                loadPosts()
                                                Toast.makeText(
                                                    this@VideoAdminActivity,
                                                    "वीडियो पोस्ट डिलीट हो गई।",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            .addOnFailureListener { error ->
                                                Toast.makeText(
                                                    this@VideoAdminActivity,
                                                    "डिलीट नहीं हुआ: ${error.message}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                    }
                                    .show()
                            }
                        },
                        LinearLayout.LayoutParams(0, dp(46), 1f)
                    )

                    row.addView(actions)

                    postsList.addView(
                        row,
                        LinearLayout.LayoutParams(-1, -2).apply {
                            bottomMargin = dp(8)
                        }
                    )
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    "पोस्ट लोड नहीं हुई: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    
private fun clearForm() {
    selectedId = null
    selectedGalleryUri = null

    titleField.text.clear()
    urlField.text.clear()
    descriptionField.text.clear()
    thumbnailField.text.clear()

    thumbnailPreview.setImageDrawable(null)
}

}
