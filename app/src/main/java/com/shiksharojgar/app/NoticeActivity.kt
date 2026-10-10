package com.shiksharojgar.app

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NoticeActivity : AppCompatActivity() {

private val db = FirebaseFirestore.getInstance()

private lateinit var listBox: LinearLayout

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(buildUi())

    loadNotices()
}

// ============================================================
// UI
// ============================================================

private fun buildUi(): LinearLayout {

    val root =
        LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            setBackgroundColor(
                Color.rgb(
                    248,
                    250,
                    252
                )
            )
        }

    // --------------------------------------------------------
    // TOP BAR
    // --------------------------------------------------------

    val bar =
        LinearLayout(this).apply {

            orientation =
                LinearLayout.HORIZONTAL

            gravity =
                Gravity.CENTER_VERTICAL

            setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
            )

            setBackgroundColor(
                Color.rgb(
                    7,
                    89,
                    133
                )
            )
        }

    val back =
        TextView(this).apply {

            text =
                "←"

            textSize =
                28f

            setTextColor(
                Color.WHITE
            )

            gravity =
                Gravity.CENTER

            setOnClickListener {
                finish()
            }
        }

    bar.addView(
        back,
        LinearLayout.LayoutParams(
            dp(52),
            dp(52)
        )
    )

    val title =
        TextView(this).apply {

            text =
                "📢 Notice / Update"

            textSize =
                19f

            typeface =
                Typeface.DEFAULT_BOLD

            setTextColor(
                Color.WHITE
            )

            gravity =
                Gravity.CENTER_VERTICAL
        }

    bar.addView(
        title,
        LinearLayout.LayoutParams(
            0,
            dp(52),
            1f
        )
    )

    root.addView(
        bar
    )

    // --------------------------------------------------------
    // NOTICE LIST
    // --------------------------------------------------------

    val scroll =
        ScrollView(this)

    listBox =
        LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            setPadding(
                dp(12),
                dp(12),
                dp(12),
                dp(28)
            )
        }

    scroll.addView(
        listBox
    )

    root.addView(
    scroll,
    LinearLayout.LayoutParams(
        -1,
        0,
        1f
    )
)


/*
 * ============================================================
 * SHARED FIVE-BUTTON BOTTOM BAR
 * ============================================================
 */
val bottomBar =
    CommonPageUi.createBottomBar(
        this,
        CommonPageUi.Section.NOTICE
    )

root.addView(
    bottomBar,
    LinearLayout.LayoutParams(
        -1,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
)

ViewCompat.setOnApplyWindowInsetsListener(
    bottomBar
) { view, insets ->

    val navigationBottom =
        insets.getInsets(
            WindowInsetsCompat.Type.navigationBars()
        ).bottom

    view.setPadding(
        view.paddingLeft,
        dp(4),
        view.paddingRight,
        navigationBottom + dp(4)
    )

    insets
}


return root
}
// ============================================================
// LOAD NOTICES
// ============================================================

private fun loadNotices() {

    listBox.removeAllViews()

    db.collection(
        "home_notices"
    )
        .whereEqualTo(
            "enabled",
            true
        )
        
        .get()
        .addOnSuccessListener { snapshot ->

            listBox.removeAllViews()

            val now =
                System.currentTimeMillis()

            /*
             * startAt / endAt:
             *
             * startAt = 0  → कोई start restriction नहीं
             * endAt   = 0  → कोई end restriction नहीं
             */
             val sortedDocuments =
    snapshot.documents.sortedByDescending {
        it.getLong("createdAt") ?: 0L
    }
            val notices =
    sortedDocuments.filter { doc ->

                    val startAt =
                        doc.getLong(
                            "startAt"
                        ) ?: 0L

                    val endAt =
                        doc.getLong(
                            "endAt"
                        ) ?: 0L

                    (
                        startAt == 0L ||
                        now >= startAt
                    ) &&
                    (
                        endAt == 0L ||
                        now <= endAt
                    )
                }

            if (
                notices.isEmpty()
            ) {

                listBox.addView(
                    TextView(this).apply {

                        text =
                            "अभी कोई नया Notice / Update उपलब्ध नहीं है।"

                        textSize =
                            16f

                        gravity =
                            Gravity.CENTER

                        setPadding(
                            dp(16),
                            dp(60),
                            dp(16),
                            dp(60)
                        )
                    }
                )
                loadCommonNoticePosts()
                return@addOnSuccessListener
            }

            notices.forEach { doc ->

                addNoticeCard(
                    doc
                )
            }
            val newestNoticeTime =
    notices
        .mapNotNull {
            it.getLong("createdAt")
        }
        .maxOrNull()
        ?: System.currentTimeMillis()

getSharedPreferences(
    "sr_notifications",
    MODE_PRIVATE
)
.edit()
.putLong(
    "notice_last_seen_at",
    newestNoticeTime
)
.apply()
                   loadCommonNoticePosts()
        }
        .addOnFailureListener { e ->

            listBox.removeAllViews()

            listBox.addView(
                TextView(this).apply {

                    text =
                        "Notice लोड नहीं हो सका।"

                    textSize =
                        16f

                    gravity =
                        Gravity.CENTER

                    setPadding(
                        dp(16),
                        dp(60),
                        dp(16),
                        dp(60)
                    )
                }
            )

            Toast.makeText(
                this,
                e.message
                    ?: "Firestore error",
                Toast.LENGTH_SHORT
            ).show()
                        loadCommonNoticePosts()
        }
}

// ============================================================
// NOTICE CARD
// ============================================================

private fun addNoticeCard(
    doc: DocumentSnapshot
) {

    val title =
        doc.getString(
            "title"
        )
            .orEmpty()
            .ifBlank {
                "Notice / Update"
            }

    val body =
        doc.getString(
            "body"
        )
            .orEmpty()

    val imageUrl =
        doc.getString(
            "imageUrl"
        )
            .orEmpty()

    val linkUrl =
        doc.getString(
            "linkUrl"
        )
            .orEmpty()

    val linkLabel =
        doc.getString(
            "linkLabel"
        )
            .orEmpty()
            .ifBlank {
                "🔗 Open Link"
            }

    val card =
        LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            setPadding(
                dp(14),
                dp(14),
                dp(14),
                dp(14)
            )

            setBackgroundColor(
                Color.WHITE
            )
        }

    // --------------------------------------------------------
    // TITLE
    // --------------------------------------------------------

    card.addView(
        TextView(this).apply {

            text =
                title

            textSize =
                20f

            typeface =
                Typeface.DEFAULT_BOLD

            setTextColor(
                Color.rgb(
                    7,
                    89,
                    133
                )
            )

            setPadding(
                0,
                0,
                0,
                dp(8)
            )
        }
    )

    // --------------------------------------------------------
    // PHOTO
    // --------------------------------------------------------

    if (
        imageUrl.isNotBlank()
    ) {

        val image =
            ImageView(this).apply {

                adjustViewBounds =
                    true

                scaleType =
                    ImageView.ScaleType.CENTER_CROP

                setBackgroundColor(
                    Color.rgb(
                        238,
                        242,
                        247
                    )
                )
            }

        card.addView(
            image,
            LinearLayout.LayoutParams(
                -1,
                dp(220)
            ).apply {

                bottomMargin =
                    dp(10)
            }
        )

        /*
         * Firestore chunk media और
         * पुराने supported media दोनों के लिए
         * ChannelRepository का loader इस्तेमाल।
         */
        ChannelRepository.loadImage(
            imageUrl
        ) { bitmap, _ ->

            runOnUiThread {

                if (
                    bitmap != null
                ) {

                    image.setImageBitmap(
                        bitmap
                    )
                }
            }
        }
    }

    // --------------------------------------------------------
    // FULL MULTI-LINE TEXT
    // --------------------------------------------------------

    if (
        body.isNotBlank()
    ) {

        card.addView(
            TextView(this).apply {

                text =
                    body

                textSize =
                    16f

                setTextColor(
                    Color.rgb(
                        30,
                        41,
                        59
                    )
                )

                setLineSpacing(
                    dp(4).toFloat(),
                    1.05f
                )

                setPadding(
                    0,
                    dp(4),
                    0,
                    dp(10)
                )
            }
        )
    }

    // --------------------------------------------------------
    // CLICKABLE LINK
    // --------------------------------------------------------

    if (
        linkUrl.isNotBlank()
    ) {

        card.addView(
            TextView(this).apply {

                text =
                    linkLabel

                textSize =
                    15f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.rgb(
                        25,
                        118,
                        210
                    )
                )

                setPadding(
                    0,
                    dp(8),
                    0,
                    dp(4)
                )

                setOnClickListener {

    try {

        startActivity(
            Intent(
                this@NoticeActivity,
                WebViewActivity::class.java
            ).putExtra(
                "url",
                linkUrl
            )
        )

    } catch (
        _: Exception
    ) {

                        Toast.makeText(
                            this@NoticeActivity,
                            "Link open नहीं हो सका।",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

// --------------------------------------------------------
// SHARE THIS NOTICE
// --------------------------------------------------------

val noticeShareText =
    buildString {

        append(
            "📱 Shiksha Rojgar App 🔔 Notice / Update"
        )

        append("\n\n")
        append(title)

        if (body.isNotBlank()) {
            append("\n\n")
            append(body)
        }

        if (linkUrl.isNotBlank()) {
            append("\n\n")
            append(linkLabel)
            append("\n")
            append(linkUrl)
        }

        append("\n\n🌐 Official Website")
        append("\nhttps://www.shiksharojgar.com/")

        append("\n\n📱 Download Shiksha Rojgar App")
        append(
            "\nhttps://www.shiksharojgar.com/2026/09/" +
                "shiksha-rojgar-android-app-latest.html"
        )
    }

card.addView(
    TextView(this).apply {

        text = "📤 Share ↗️"

        textSize = 15f

        typeface = Typeface.DEFAULT_BOLD

        gravity = Gravity.CENTER

        setTextColor(
            Color.rgb(7, 89, 133)
        )

        setPadding(
            dp(12),
            dp(12),
            dp(12),
            dp(12)
        )

        setOnClickListener {

            try {

                val shareIntent =
                    Intent(Intent.ACTION_SEND).apply {

                        type = "text/plain"

                        putExtra(
                            Intent.EXTRA_TEXT,
                            noticeShareText
                        )
                    }

                startActivity(
                    Intent.createChooser(
                        shareIntent,
                        "Share Notice"
                    )
                )

            } catch (_: Exception) {

                Toast.makeText(
                    this@NoticeActivity,
                    "Notice शेयर नहीं हो सका।",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
)    // --------------------------------------------------------
    // ADD CARD
    // --------------------------------------------------------

    listBox.addView(
        card,
        LinearLayout.LayoutParams(
            -1,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {

            bottomMargin =
                dp(12)
        }
    )

    // --------------------------------------------------------
    // DIVIDER
    // --------------------------------------------------------

    listBox.addView(
        android.view.View(this).apply {

            setBackgroundColor(
                Color.rgb(
                    220,
                    228,
                    236
                )
            )
        },
        LinearLayout.LayoutParams(
            -1,
            dp(1)
        ).apply {

            bottomMargin =
                dp(8)
        }
    )
}

private fun loadCommonNoticePosts() {

    db.collection("category_posts")
        .get()
        .addOnSuccessListener { snapshot ->

            val posts = snapshot.documents
                .filter { doc ->
                    doc.getString("category") == "notice" &&
                    (doc.getBoolean("enabled") ?: true)
                }
                .sortedWith(
                    compareByDescending<com.google.firebase.firestore.DocumentSnapshot> {
                        it.getBoolean("pinned") ?: false
                    }.thenByDescending {
                        it.getLong("pinOrder") ?: 0L
                    }.thenByDescending {
                        it.getLong("createdAt") ?: 0L
                    }
                )

            if (posts.isNotEmpty()) {
                if (
                    listBox.childCount == 1 &&
                    (listBox.getChildAt(0) as? TextView)
                        ?.text?.toString() ==
                    "अभी कोई नया Notice / Update उपलब्ध नहीं है।"
                ) {
                    listBox.removeAllViews()
                }
            }

            posts.forEach { doc ->

                val title = doc.getString("title")
                    .orEmpty()
                    .ifBlank { "Notice / Update" }

                val body = doc.getString("body").orEmpty()
                val description =
                    doc.getString("description").orEmpty()
                val imageUrl =
                    doc.getString("imageUrl").orEmpty()
                val websiteUrl =
                    doc.getString("websiteUrl").orEmpty().trim()

                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), dp(14), dp(14), dp(14))
                    setBackgroundColor(Color.WHITE)
                }

                card.addView(
                    TextView(this).apply {
                        text = title
                        textSize = 20f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(Color.rgb(7, 89, 133))
                        setPadding(0, 0, 0, dp(8))
                    }
                )

                if (imageUrl.isNotBlank()) {
                    val image = ImageView(this).apply {
                        adjustViewBounds = true
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }

                    card.addView(
                        image,
                        LinearLayout.LayoutParams(
                            -1,
                            dp(220)
                        ).apply {
                            bottomMargin = dp(10)
                        }
                    )

                    ChannelRepository.loadImage(imageUrl) { bitmap, _ ->
                        runOnUiThread {
                            if (bitmap != null) {
                                image.setImageBitmap(bitmap)
                            }
                        }
                    }
                }

                if (body.isNotBlank()) {
                    card.addView(
                        TextView(this).apply {
                            
val formatRow = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
}

fun applyBodyFormat(prefix: String, suffix: String) {
    val start = bodyField.selectionStart.coerceAtLeast(0)
    val end = bodyField.selectionEnd.coerceAtLeast(0)
    val from = minOf(start, end)
    val to = maxOf(start, end)

    if (from != to) {
        val selectedText = bodyField.text
            .substring(from, to)
        bodyField.text.replace(
            from,
            to,
            "$prefix$selectedText$suffix"
        )
        bodyField.setSelection(
            from,
            from + prefix.length + selectedText.length + suffix.length
        )
    } else {
        bodyField.text.insert(start, "$prefix$suffix")
        bodyField.setSelection(start + prefix.length)
    }
}

formatRow.addView(Button(this).apply {
    text = "B बोल्ड"
    setOnClickListener {
        applyBodyFormat("*", "*")
    }
})

formatRow.addView(Button(this).apply {
    text = "> हल्का"
    setOnClickListener {
        applyBodyFormat("> ", "")
    }
})

formatRow.addView(Button(this).apply {
    text = "` हाइलाइट"
    setOnClickListener {
        applyBodyFormat("`", "`")
    }
})

box.addView(formatRow)

                            textSize = 16f
                            setTextColor(Color.rgb(30, 41, 59))
                            setPadding(0, dp(4), 0, dp(8))
                        }
                    )
                }

                if (description.isNotBlank()) {
                    card.addView(
                        TextView(this).apply {
                            text = description
                            textSize = 15f
                            setTextColor(Color.rgb(71, 85, 105))
                            setPadding(0, 0, 0, dp(8))
                        }
                    )
                }

                if (websiteUrl.isNotBlank()) {
                    card.addView(
                        TextView(this).apply {
                            text = "🌐 वेबसाइट खोलें"
                            textSize = 16f
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(Color.rgb(25, 118, 210))
                            setPadding(0, dp(8), 0, dp(8))

                            setOnClickListener {
                                try {
                                    val url = if (
                                        websiteUrl.startsWith("https://",
                                            ignoreCase = true
                                        ) ||
                                        websiteUrl.startsWith("http://",
                                            ignoreCase = true
                                        )
                                    ) {
                                        websiteUrl
                                    } else {
                                        "https://$websiteUrl"
                                    }

                                    startActivity(
                                        Intent(
                                            this@NoticeActivity,
                                            WebViewActivity::class.java
                                        ).putExtra("url", url)
                                    )
                                } catch (_: Exception) {
                                    Toast.makeText(
                                        this@NoticeActivity,
                                        "वेबसाइट लिंक नहीं खुल सका।",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )
                }

                listBox.addView(
                    card,
                    LinearLayout.LayoutParams(
                        -1,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(12)
                    }
                )
            }
        }
        .addOnFailureListener { error ->
            Toast.makeText(
                this,
                "नई पोस्ट लोड नहीं हुई: ${error.message ?: "Firestore error"}",
                Toast.LENGTH_SHORT
            ).show()
        }
}
private fun formatPostBody(value: String): CharSequence {
    val result = android.text.SpannableStringBuilder()

    value.lines().forEachIndexed { lineIndex, line ->
        if (lineIndex > 0) result.append("\n")

        val isQuote = line.trimStart().startsWith("> ")
        val content = if (isQuote) {
            line.trimStart().removePrefix("> ")
        } else {
            line
        }

        val lineStart = result.length
        var i = 0

        while (i < content.length) {
            val marker = content[i]

            if (marker == '*' || marker == '`') {
                val end = content.indexOf(marker, i + 1)

                if (end > i + 1) {
                    val startIndex = result.length
                    result.append(content.substring(i + 1, end))
                    val endIndex = result.length

                    if (marker == '*') {
                        result.setSpan(
                            android.text.style.StyleSpan(
                                android.graphics.Typeface.BOLD
                            ),
                            startIndex,
                            endIndex,
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    } else {
                        result.setSpan(
                            android.text.style.BackgroundColorSpan(
                                Color.rgb(255, 243, 176)
                            ),
                            startIndex,
                            endIndex,
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }

                    i = end + 1
                    continue
                }
            }

            result.append(marker)
            i++
        }

        if (isQuote && result.length > lineStart) {
            result.setSpan(
                android.text.style.QuoteSpan(Color.rgb(100, 116, 139)),
                lineStart,
                result.length,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    return result
}
// ============================================================
// DP
// ============================================================

private fun dp(
    value: Int
): Int {

    return (
        value *
        resources.displayMetrics.density
    ).toInt()
}

}
