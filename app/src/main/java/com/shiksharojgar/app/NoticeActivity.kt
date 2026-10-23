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
private var pendingNoticeId: String? = null
 private lateinit var noticeScroll: ScrollView
    
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(buildUi())

    loadNotices()

    handleNoticeDeepLink(intent)
}

override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleNoticeDeepLink(intent)
}

private fun handleNoticeDeepLink(intent: Intent?) {
    val uri = intent?.data ?: return

    val noticeId = uri.getQueryParameter("post")
        ?: uri.pathSegments.lastOrNull()

    if (noticeId.isNullOrBlank() || noticeId == "notice") {
        return
    }

    pendingNoticeId = noticeId

    db.collection("category_posts")
        .document(noticeId)
        .get()
        .addOnSuccessListener { doc ->
            if (doc.exists() &&
                doc.getString("category") == "notice"
            ) {
                pendingNoticeId = doc.id
scrollToNoticeIfLoaded(doc.id)

                Toast.makeText(
                    this,
                    "✅ नोटिस मिला: ${doc.getString("title").orEmpty()}",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "यह नोटिस उपलब्ध नहीं है।",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        .addOnFailureListener {
            Toast.makeText(
                this,
                "नोटिस खोलने में समस्या हुई।",
                Toast.LENGTH_SHORT
            ).show()
        }
}
private fun scrollToNoticeIfLoaded(noticeId: String) {
    if (!::listBox.isInitialized ||
        !::noticeScroll.isInitialized
    ) {
        return
    }

    val targetCard = (0 until listBox.childCount)
        .map { listBox.getChildAt(it) }
        .firstOrNull { it.tag == noticeId }
        ?: return

    noticeScroll.post {
        noticeScroll.smoothScrollTo(
            0,
            targetCard.top
        )
        pendingNoticeId = null
    }
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
ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
    val statusBarHeight = insets.getInsets(
        WindowInsetsCompat.Type.statusBars()
    ).top

    view.setPadding(
        view.paddingLeft,
        statusBarHeight,
        view.paddingRight,
        view.paddingBottom
    )

    insets
}
    // --------------------------------------------------------
    // TOP BAR
    // --------------------------------------------------------

    
/*
 * EXISTING COMMON HEADER
 * Notice / Update page only
 */
val header = CommonPageUi.createHeader(
    this,
    onBack = {
        finish()
    },
    onShare = {},
    onMenu = {
        CommonPageUi.showAccountMenu(this)
    }
)

root.addView(
    header,
    LinearLayout.LayoutParams(
        -1,
        dp(58)
    )
)


    // --------------------------------------------------------
    // NOTICE LIST
    // --------------------------------------------------------

noticeScroll =
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

    noticeScroll.addView(
    listBox
)

    root.addView(
    noticeScroll,
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

        append("*📱 Shiksha Rojgar App 🔔 नोटिस अपडेट*")

        append("\n\n*पोस्ट देखें 👇*")

        if (linkUrl.isNotBlank()) {
            append("\n")
            append(linkUrl.trim())
        } else {
            append("\nhttps://www.shiksharojgar.com/")
        }

        if (title.isNotBlank()) {
            append("\n\n*")
            append(title.trim())
            append("*")
        }

        if (body.isNotBlank()) {
            append("\n\n")
            append(body.trim())
        }

        if (linkUrl.isNotBlank()) {
            append("\n\n🔗 ")
            append(linkLabel.ifBlank { "लिंक खोलें" })
            append(" 👇\n")
            append(linkUrl.trim())
        }

        append("\n\n*✅ शिक्षा रोजगार की Official Website 🌐*")
        append("\nhttps://www.shiksharojgar.com/")

        append(
            "\n\n*☝️ यहां क्लिक करके 📱 Shiksha Rojgar App " +
                "डाउनलोड इंस्टॉल करें !*"
        )

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

                ShareHelper.shareImageOrText(
    this,
    "Shiksha Rojgar Notice",
    noticeShareText,
    imageUrl,
    "Share Notice"
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

            val newestCategoryNoticeTime =
                posts.mapNotNull {
                    it.getLong("createdAt")
                }.maxOrNull()

            if (newestCategoryNoticeTime != null) {
                getSharedPreferences(
                    "sr_notifications",
                    MODE_PRIVATE
                ).edit()
                    .putLong(
                        "category_notice_last_seen_at",
                        newestCategoryNoticeTime
                    )
                    .apply()
            }
            
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
                    val customButtons =
    (doc.get("buttons") as? List<*>)?.mapNotNull { item ->
        val button = item as? Map<*, *>
            ?: return@mapNotNull null

        CategoryPostButton(
            label = button["label"] as? String ?: "",
            url = button["url"] as? String ?: "",
            backgroundColor =
                button["backgroundColor"] as? String ?: "#1976D2",
            textColor =
                button["textColor"] as? String ?: "#FFFFFF",
            enabled = button["enabled"] as? Boolean ?: true
        )
    } ?: emptyList()

                val card = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(dp(14), dp(14), dp(14), dp(14))
    setBackgroundColor(Color.WHITE)
    tag = doc.id
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
                            
text = formatPostBody(body)
 

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
// 🌐 Notice पोस्ट में वेबसाइट और YouTube लिंक प्रीव्यू
val noticePreviewLinks = buildString {
    appendLine(title)
    appendLine(body)
    appendLine(description)
    appendLine(websiteUrl)

    customButtons.forEach { button ->
        appendLine(button.url)
    }
}

LinkPreviewUi.addPreviews(
    context = this@NoticeActivity,
    container = card,
    text = noticePreviewLinks,
    maxLinks = 2
)

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
                customButtons
                    .filter {
                        it.enabled &&
                        it.label.isNotBlank() &&
                        it.url.isNotBlank()
                    }
                    .forEach { postButton ->

                        card.addView(
                            TextView(this).apply {
                                text = postButton.label
                                gravity = Gravity.CENTER
                                textSize = 14f
                                typeface = Typeface.DEFAULT_BOLD

                                val textColor = try {
                                    Color.parseColor(postButton.textColor)
                                } catch (_: Exception) {
                                    Color.WHITE
                                }

                                setTextColor(textColor)

                                background = GradientFactory.gradient(
                                    postButton.backgroundColor,
                                    postButton.backgroundColor
                                )

                                setPadding(
                                    dp(12),
                                    dp(8),
                                    dp(12),
                                    dp(8)
                                )

                                setOnClickListener {
                                    val rawUrl = postButton.url.trim()
                                    val url = if (
                                        rawUrl.startsWith("https://", true) ||
                                        rawUrl.startsWith("http://", true)
                                    ) {
                                        rawUrl
                                    } else {
                                        "https://$rawUrl"
                                    }

                                    try {
                                        startActivity(
                                            Intent(
                                                this@NoticeActivity,
                                                WebViewActivity::class.java
                                            ).putExtra("url", url)
                                        )
                                    } catch (_: Exception) {
                                        Toast.makeText(
                                            this@NoticeActivity,
                                            "बटन का लिंक नहीं खुल सका।",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            LinearLayout.LayoutParams(
                                -1,
                                dp(42)
                            ).apply {
                                bottomMargin = dp(8)
                            }
                        )
                    }
                // 📤 CATEGORY NOTICE SHARE BUTTON

                val categoryNoticeShareText =
                    buildString {

                        append(
                            "*📱 Shiksha Rojgar App 🔔 नोटिस अपडेट*"
                        )
append("\n\n*पोस्ट देखें 👇*")
append("\nhttps://shiksha-rojgar.web.app/notice?post=")
append(doc.id)

                        if (title.isNotBlank()) {
                            append("\n\n*")
                            append(title.trim())
                            append("*")
                        }

                        if (imageUrl.isNotBlank()) {
                            append("\n\n🖼️ फोटो देखें 👇\n")
                            append(imageUrl)
                        }

                        if (body.isNotBlank()) {
                            append("\n\n")
                            append(body.trim())
                        }

                        if (description.isNotBlank()) {
                            append("\n\n")
                            append(description.trim())
                        }

                        if (websiteUrl.isNotBlank()) {
                            append("\n\n🌐 वेबसाइट लिंक 👇\n")
                            append(websiteUrl)
                        }

                        customButtons
                            .filter {
                                it.enabled && it.url.isNotBlank()
                            }
                            .forEach { button ->
                                append("\n\n🔗 ")
                                append(
                                    button.label.ifBlank {
                                        "लिंक खोलें"
                                    }
                                )
                                append(" 👇\n")
                                append(button.url)
                            }

                        val categoryNoticeFileUrl =
                            doc.getString("fileUrl").orEmpty()

                        val categoryNoticeFileName =
                            doc.getString("fileName").orEmpty()

                        if (categoryNoticeFileUrl.isNotBlank()) {
                            append("\n\n📄 ")
                            append(
                                categoryNoticeFileName.ifBlank {
                                    "PDF / Document"
                                }
                            )
                            append(" 👇\n")
                            append(categoryNoticeFileUrl)
                        }

                        append(
                            "\n\n*✅ शिक्षा रोजगार की Official Website 🌐*"
                        )
                        append("\nhttps://www.shiksharojgar.com/")

                        append(
                            "\n\n*☝️ यहां क्लिक करके 📱 Shiksha Rojgar App " +
                                "डाउनलोड इंस्टॉल करें !*"
                        )
                        append(
                            "\nhttps://www.shiksharojgar.com/2026/09/" +
                                "shiksha-rojgar-android-app-latest.html"
                        )
                    }

                card.addView(
                    TextView(this@NoticeActivity).apply {

                        text = "📤 Share ↗️"
                        textSize = 15f
                        typeface = Typeface.DEFAULT_BOLD
                        gravity = Gravity.CENTER

                        setTextColor(Color.rgb(7, 89, 133))

                        setPadding(
                            dp(12),
                            dp(12),
                            dp(12),
                            dp(12)
                        )

                        setOnClickListener {
                            try {
                                ShareHelper.shareImageOrText(
    this@NoticeActivity,
    "Shiksha Rojgar Notice / Update",
    categoryNoticeShareText,
    imageUrl,
    "Share Notice / Update"
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
                )
                listBox.addView(
                    card,
                    LinearLayout.LayoutParams(
                        -1,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                                                            ).apply {
                        bottomMargin = dp(12)
                    }
                
                )

                if (pendingNoticeId == doc.id) {
                    scrollToNoticeIfLoaded(doc.id)
                }
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
