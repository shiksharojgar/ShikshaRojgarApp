package com.shiksharojgar.app

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URL

class VideoActivity : AppCompatActivity() {

    private val db by lazy {
        FirebaseFirestore.getInstance()
    }

    private lateinit var videoList: LinearLayout

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(245, 248, 252))
        }

        val header = TextView(this).apply {
            text = "🎬 शिक्षा रोजगार — वीडियो देखें"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(16), dp(12), dp(16))
            setBackgroundColor(Color.rgb(21, 101, 192))
        }

        root.addView(
            header,
            LinearLayout.LayoutParams(-1, -2)
        )

        val scroll = ScrollView(this)

        videoList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }

        scroll.addView(videoList)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(0, 0, 1f)
        )

        root.addView(
            CommonPageUi.createBottomBar(
                this,
                CommonPageUi.Section.MORE
            ),
            LinearLayout.LayoutParams(-1, -2)
        )

        setContentView(root)
        loadVideos()
    }

    private fun loadVideos() {
        videoList.removeAllViews()

        val loading = TextView(this).apply {
            text = "वीडियो लोड हो रहे हैं…"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, dp(30), 0, dp(30))
        }

        videoList.addView(loading)

        db.collection("video_posts")
            .get()
            .addOnSuccessListener { snapshot ->

                videoList.removeAllViews()

                val videos = snapshot.documents
                    .filter {
                        it.getBoolean("enabled") != false &&
                            !it.getString("url").isNullOrBlank()
                    }
                    .sortedByDescending {
                        it.getTimestamp("createdAt")?.seconds ?: 0L
                    }

                if (videos.isEmpty()) {
                    videoList.addView(
                        TextView(this).apply {
                            text = "अभी कोई वीडियो उपलब्ध नहीं है।"
                            textSize = 16f
                            gravity = Gravity.CENTER
                            setPadding(0, dp(35), 0, dp(35))
                        }
                    )
                    return@addOnSuccessListener
                }

                videos.forEach { document ->
                    val title = document.getString("title")
                        ?: "वीडियो देखें"

                    val description =
                        document.getString("description").orEmpty()

                    val url = document.getString("url").orEmpty()

                    val customThumbnail =
                        document.getString("thumbnailUrl").orEmpty()

                    addVideoCard(
                        title,
                        description,
                        url,
                        customThumbnail
                    )
                }
            }
            .addOnFailureListener {
                videoList.removeAllViews()

                videoList.addView(
                    TextView(this).apply {
                        text = "वीडियो लोड नहीं हुए। इंटरनेट कनेक्शन जाँचें।"
                        textSize = 15f
                        setPadding(dp(12), dp(20), dp(12), dp(20))
                    }
                )
            }
    }

    private fun addVideoCard(
        title: String,
        description: String,
        url: String,
        customThumbnail: String
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(9), dp(9), dp(9), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), Color.rgb(220, 228, 238))
            }
            elevation = dp(2).toFloat()
        }

        val thumbnail = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.rgb(24, 28, 35))
            contentDescription = "वीडियो थंबनेल: $title"
        }

        card.addView(
            thumbnail,
            LinearLayout.LayoutParams(-1, dp(215))
        )

        val thumbnailUrl = customThumbnail.ifBlank {
            youtubeThumbnail(url).orEmpty()
        }

        if (thumbnailUrl.isNotBlank()) {
            loadThumbnail(thumbnail, thumbnailUrl)
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(25, 39, 58))
            setPadding(0, dp(10), 0, dp(6))
        }

        card.addView(titleView)

        if (description.isNotBlank()) {
            card.addView(
                TextView(this).apply {
                    text = description
                    textSize = 14f
                    setTextColor(Color.rgb(75, 85, 99))
                    setPadding(0, 0, 0, dp(10))
                }
            )
        }

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val openButton = Button(this).apply {
            text = "▶ वीडियो खोलें"
            setTextColor(Color.WHITE)
            setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                    Color.rgb(25, 103, 210)
                )
            )
            setOnClickListener {
                openVideo(url)
            }
        }

        val shareButton = Button(this).apply {
            text = "शेयर करें"
            setTextColor(Color.WHITE)
            setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                    Color.rgb(22, 132, 74)
                )
            )
            setOnClickListener {
                shareVideo(title, description, url)
            }
        }

        buttons.addView(
            openButton,
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                rightMargin = dp(4)
            }
        )

        buttons.addView(
            shareButton,
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                leftMargin = dp(4)
            }
        )

        card.addView(buttons)

        videoList.addView(
            card,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(14)
            }
        )
    }

    private fun youtubeThumbnail(url: String): String? {
        val id = Regex(
            """(?:youtu\.be/|[?&]v=|youtube\.com/(?:embed|shorts)/)([A-Za-z0-9_-]{11})"""
        ).find(url)?.groupValues?.getOrNull(1)

        return id?.let {
            "https://img.youtube.com/vi/$it/hqdefault.jpg"
        }
    }

    private fun loadThumbnail(
        imageView: ImageView,
        imageUrl: String
    ) {
        Thread {
            try {
                val bitmap = URL(imageUrl).openStream().use {
                    BitmapFactory.decodeStream(it)
                }

                runOnUiThread {
                    if (!isFinishing && bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                    }
                }
            } catch (_: Exception) {
                // थंबनेल न मिले तो कार्ड और Open बटन फिर भी दिखेंगे।
            }
        }.start()
    }

    private fun openVideo(url: String) {
        try {
            val uri = Uri.parse(url.trim())

            if (uri.scheme !in listOf("https", "http") ||
                uri.host.isNullOrBlank()
            ) {
                Toast.makeText(
                    this,
                    "वीडियो लिंक सही नहीं है।",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            startActivity(
                Intent(Intent.ACTION_VIEW, uri)
            )
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "वीडियो खोलने में समस्या हुई।",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun shareVideo(
        title: String,
        description: String,
        url: String
    ) {
        val text = buildString {
            append("🎬 $title\n\n")

            if (description.isNotBlank()) {
                append(description)
                append("\n\n")
            }

            append("▶ वीडियो देखें:\n")
            append(url)
            append("\n\n")
            append("📱 Shiksha Rojgar App")
        }

        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                "वीडियो शेयर करें"
            )
        )
    }
}
