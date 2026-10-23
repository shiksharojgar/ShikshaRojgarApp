package com.shiksharojgar.app

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.concurrent.Executors
import java.util.regex.Pattern

object LinkPreviewUi {

    private val executor = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())

    private data class PreviewData(
        val title: String,
        val description: String,
        val imageUrl: String,
        val youtube: Boolean
    )

    fun addPreviews(
        context: Context,
        container: LinearLayout,
        text: String,
        maxLinks: Int = 2
    ) {
        val urls = extractUrls(text)
            .distinct()
            .take(maxLinks)

        urls.forEach { url ->
            addPreviewCard(context, container, url)
        }
    }

    
fun addPostPreview(
    context: Context,
    container: LinearLayout,
    post: Post,
    onOpen: () -> Unit
) {
    val card = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(
            dp(context, 10),
            dp(context, 10),
            dp(context, 10),
            dp(context, 10)
        )
        background = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dp(context, 12).toFloat()
            setStroke(dp(context, 1), Color.rgb(220, 230, 240))
        }
    }

    val image = ImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        adjustViewBounds = true
        scaleType = ImageView.ScaleType.FIT_CENTER
        contentDescription = "वेबसाइट पोस्ट की बड़ी इमेज"
        visibility = if (post.imageUrl.isBlank()) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    val title = TextView(context).apply {
        text = post.title
        textSize = 17f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.rgb(20, 75, 130))
        setPadding(0, dp(context, 10), 0, dp(context, 6))
    }

    val description = TextView(context).apply {
        text = post.description
        textSize = 14f
        setTextColor(Color.DKGRAY)
        visibility = if (post.description.isBlank()) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    val buttonsRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    val openButton = TextView(context).apply {
        text = "🌐 ओपन पेज"
        gravity = Gravity.CENTER
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        setPadding(dp(context, 6), 0, dp(context, 6), 0)
        background = GradientDrawable().apply {
            setColor(Color.rgb(37, 99, 235))
            cornerRadius = dp(context, 8).toFloat()
        }
        setOnClickListener {
            onOpen()
        }
    }

    val shareButton = TextView(context).apply {
        text = "↗ शेयर"
        gravity = Gravity.CENTER
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        setPadding(dp(context, 6), 0, dp(context, 6), 0)
        background = GradientDrawable().apply {
            setColor(Color.rgb(22, 163, 74))
            cornerRadius = dp(context, 8).toFloat()
        }
        
setOnClickListener {
    ShareHelper.shareImageOrText(
        context,
        post.title,
        "${post.title}\n${post.url}",
        chooserTitle = "पोस्ट शेयर करें"
    )
}


            context.startActivity(
                Intent.createChooser(
                    shareIntent,
                    "पोस्ट शेयर करें"
                )
            )
        }
    }

    buttonsRow.addView(
        openButton,
        LinearLayout.LayoutParams(
            0,
            dp(context, 44),
            1f
        ).apply {
            marginEnd = dp(context, 8)
        }
    )

    buttonsRow.addView(
        shareButton,
        LinearLayout.LayoutParams(
            0,
            dp(context, 44),
            1f
        )
    )

    card.addView(image)
    card.addView(title)
    card.addView(description)

    card.addView(
        buttonsRow,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(context, 12)
        }
    )

    container.addView(
        card,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(context, 10)
        }
    )

    if (post.imageUrl.isNotBlank()) {
        val largeImageUrl = post.imageUrl
            .replace(
                Regex("=s\\d+(-c)?"),
                "=s1200"
            )
            .replace(
                Regex("=w\\d+-h\\d+[^&]*"),
                "=w1200"
            )
            .replace(
                Regex("/s\\d+(-c)?/"),
                "/s1200/"
            )

        loadImage(largeImageUrl) { bitmap ->
            if (bitmap != null) {
                image.setImageBitmap(bitmap)
                image.visibility = View.VISIBLE
            }
        }
    }
}

    private fun extractUrls(text: String): List<String> {
        val pattern = Pattern.compile(
            """https?://[^\s<>"']+""",
            Pattern.CASE_INSENSITIVE
        )

        val matcher = pattern.matcher(text)
        val result = mutableListOf<String>()

        while (matcher.find()) {
            val url = matcher.group()
                .trim()
                .trimEnd('.', ',', ';', ')', ']', '}', '।')

            if (
                url.startsWith("https://") ||
                url.startsWith("http://")
            ) {
                result.add(url)
            }
        }

        return result
    }

    private fun addPreviewCard(
        context: Context,
        container: LinearLayout,
        url: String
    ) {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 10), dp(context, 10),
                dp(context, 10), dp(context, 10))

            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(context, 12).toFloat()
                setStroke(dp(context, 1), Color.rgb(220, 230, 240))
            }

            isClickable = true
            setOnClickListener {
                openUrl(context, url)
            }
        }

        val image = ImageView(context).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = View.GONE
            contentDescription = "लिंक प्रीव्यू इमेज"
        }

        val title = TextView(context).apply {
            text = "🔗 लिंक प्रीव्यू लोड हो रहा है…"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(20, 75, 130))
            setPadding(0, dp(context, 8), 0, dp(context, 5))
        }

        val description = TextView(context).apply {
            textSize = 13f
            setTextColor(Color.DKGRAY)
            maxLines = 3
            visibility = View.GONE
        }

        
val buttonsRow = LinearLayout(context).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
}

val openButton = TextView(context).apply {
    text = "🌐 पूरी जानकारी देखें"
    gravity = Gravity.CENTER
    textSize = 13f
    typeface = Typeface.DEFAULT_BOLD
    setTextColor(Color.WHITE)
    setPadding(
        dp(context, 8), dp(context, 12),
        dp(context, 8), dp(context, 12)
    )
    background = GradientDrawable().apply {
        setColor(Color.rgb(37, 99, 235))
        cornerRadius = dp(context, 8).toFloat()
    }
    setOnClickListener {
        openUrl(context, url)
    }
}

val shareButton = TextView(context).apply {
    text = "↗ शेयर"
    gravity = Gravity.CENTER
    textSize = 13f
    typeface = Typeface.DEFAULT_BOLD
    setTextColor(Color.WHITE)
    setPadding(
        dp(context, 8), dp(context, 12),
        dp(context, 8), dp(context, 12)
    )
    background = GradientDrawable().apply {
        setColor(Color.rgb(22, 163, 74))
        cornerRadius = dp(context, 8).toFloat()
    }
    setOnClickListener {
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "${title.text}\n$url"
                    )
                },
                "पोस्ट शेयर करें"
            )
        )
    }
}

buttonsRow.addView(
    openButton,
    LinearLayout.LayoutParams(0, dp(context, 44), 1f).apply {
        marginEnd = dp(context, 6)
    }
)

buttonsRow.addView(
    shareButton,
    LinearLayout.LayoutParams(0, dp(context, 44), 1f)
)


        
card.addView(
    image,
    LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
)

card.addView(title)
card.addView(description)
card.addView(
    buttonsRow,
    LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        topMargin = dp(context, 8)
    }
)


        executor.execute {
            val data = loadPreview(url)

            mainHandler.post {
                if (data != null) {
                    title.text = if (data.youtube) {
                        "▶️ ${data.title}"
                    } else {
                        "🌐 ${data.title}"
                    }

                    if (data.description.isNotBlank()) {
                        description.text = data.description
                        description.visibility = View.VISIBLE
                    }

                    openButton.text = if (data.youtube) {
                        "▶️ YouTube वीडियो देखें"
                    } else {
                        "🌐 पूरी जानकारी देखें"
                    }

                    if (data.imageUrl.isNotBlank()) {
                        loadImage(data.imageUrl) { bitmap ->
                            if (bitmap != null) {
                                image.setImageBitmap(bitmap)
                                image.visibility = View.VISIBLE
                            }
                        }
                    }
                } else {
                    title.text = if (isYouTube(url)) {
                        "▶️ YouTube वीडियो"
                    } else {
                        "🌐 वेबसाइट लिंक"
                    }

                    openButton.text = if (isYouTube(url)) {
                        "▶️ वीडियो खोलें"
                    } else {
                        "🌐 वेबसाइट खोलें"
                    }
                }
            }
        }
    }

    private fun loadPreview(url: String): PreviewData? {
        if (isYouTube(url)) {
            val videoId = getYouTubeId(url)

            if (!videoId.isNullOrBlank()) {
                return PreviewData(
                    title = "YouTube वीडियो",
                    description = "वीडियो देखने के लिए बटन दबाएँ।",
                    imageUrl =
                        "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                    youtube = true
                )
            }
        }

        var connection: HttpURLConnection? = null

        return try {
            connection = URL(url).openConnection()
                as HttpURLConnection

            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Android) ShikshaRojgar"
            )

            connection.connect()

            if (connection.responseCode !in 200..299) {
                return null
            }

            val contentType =
                connection.contentType.orEmpty()

            if (!contentType.contains("text/html", true)) {
                return PreviewData(
                    title = "वेबसाइट लिंक",
                    description = "",
                    imageUrl = "",
                    youtube = false
                )
            }

            val html = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText().take(700_000) }

            val title = getMeta(html, "og:title")
                .ifBlank { getTitle(html) }
                .ifBlank { URL(url).host }

            val description = getMeta(html, "og:description")
                .ifBlank { getMeta(html, "description") }

            val image = getMeta(html, "og:image")
                .ifBlank { getMeta(html, "twitter:image") }

            PreviewData(
                title = decodeHtml(title),
                description = decodeHtml(description),
                imageUrl = resolveImageUrl(url, image),
                youtube = false
            )
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun getMeta(
        html: String,
        key: String
    ): String {
        val escaped = Pattern.quote(key)

        val patterns = listOf(
            """(?is)<meta[^>]+(?:property|name)\s*=\s*["']$escaped["'][^>]+content\s*=\s*["']([^"']*)["']""",
            """(?is)<meta[^>]+content\s*=\s*["']([^"']*)["'][^>]+(?:property|name)\s*=\s*["']$escaped["']"""
        )

        for (pattern in patterns) {
            val matcher = Pattern.compile(pattern).matcher(html)
            if (matcher.find()) {
                return matcher.group(1).orEmpty()
            }
        }

        return ""
    }

    private fun getTitle(html: String): String {
        val matcher = Pattern.compile(
            """(?is)<title[^>]*>(.*?)</title>"""
        ).matcher(html)

        return if (matcher.find()) {
            matcher.group(1).orEmpty().trim()
        } else {
            ""
        }
    }

    private fun resolveImageUrl(
        pageUrl: String,
        imageUrl: String
    ): String {
        if (imageUrl.isBlank()) return ""

        return try {
            URL(URL(pageUrl), imageUrl).toString()
        } catch (_: Exception) {
            ""
        }
    }

    private fun decodeHtml(value: String): String {
        return value
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .trim()
    }

    private fun loadImage(
        imageUrl: String,
        callback: (android.graphics.Bitmap?) -> Unit
    ) {
        executor.execute {
            val bitmap = try {
                val connection =
                    URL(imageUrl).openConnection()
                        as HttpURLConnection

                connection.connectTimeout = 5000
                connection.readTimeout = 7000

                try {
                    connection.inputStream.use {
                        BitmapFactory.decodeStream(it)
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (_: Exception) {
                null
            }

            mainHandler.post {
                callback(bitmap)
            }
        }
    }

    private fun isYouTube(url: String): Boolean {
        return try {
            val host = Uri.parse(url).host.orEmpty()
                .lowercase()

            host == "youtu.be" ||
                host == "youtube.com" ||
                host.endsWith(".youtube.com") ||
                host == "youtube-nocookie.com" ||
                host.endsWith(".youtube-nocookie.com")
        } catch (_: Exception) {
            false
        }
    }

    private fun getYouTubeId(url: String): String? {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host.orEmpty().lowercase()

            val id = when {
                host == "youtu.be" ->
                    uri.lastPathSegment

                uri.path == "/watch" ->
                    uri.getQueryParameter("v")

                uri.path.orEmpty().startsWith("/shorts/") ->
                    uri.pathSegments.getOrNull(1)

                uri.path.orEmpty().startsWith("/embed/") ->
                    uri.pathSegments.getOrNull(1)

                else -> null
            }

            id?.takeIf {
                it.matches(Regex("^[A-Za-z0-9_-]{6,20}$"))
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun openUrl(
        context: Context,
        url: String
    ) {
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )
            )
        } catch (_: Exception) {
            android.widget.Toast.makeText(
                context,
                "यह लिंक नहीं खुल सका।",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun dp(
        context: Context,
        value: Int
    ): Int {
        return (value * context.resources.displayMetrics.density)
            .toInt()
    }
}
