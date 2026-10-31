package com.shiksharojgar.app

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import androidx.appcompat.app.AlertDialog

class ContentPageActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var bodyBox: LinearLayout
    private lateinit var titleView: TextView
    
    
private fun dp(value: Int): Int {
    return (
        value *
            resources.displayMetrics.density
        ).toInt()
}

    override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val pageId =
        intent.getStringExtra("pageId")
            ?: "important_information"

    val fallback =
        intent.getStringExtra("pageTitle")
            ?: "📌 Important Information"

    
val activeSection =
    intent.getStringExtra("activeSection")
        ?: if (pageId == "important_information") {
            "IMP_INFO"
        } else {
            "MORE"
        }


    setContentView(
        buildUi(
            fallback,
            activeSection
        )
    )

    load(pageId, fallback)
}
    /*
     * =========================================================
     * COMMON PAGE UI
     * Header
     * Title Strip
     * Content
     * Bottom Navigation
     * =========================================================
     */
    private fun buildUi(
    pageTitle: String,
    activeSection: String
): LinearLayout {

        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(
    Color.rgb(29, 78, 216)
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
/*
 * =====================================================
 * MASTER COMMON HEADER
 * =====================================================
 */
val header =
    CommonPageUi.createHeader(
        this,
        onBack = {
            finish()
        },
        onShare = {
            shareContentPage(pageTitle)
        },
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

        /*
         * =====================================================
         * PAGE TITLE STRIP
         * =====================================================
         */
        val titleStrip =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    10,
                    0,
                    10,
                    0
                )

                setBackgroundColor(
                    Color.rgb(219, 242, 255)
                )
            }

        

        titleView =
            TextView(this).apply {

                text =
                    pageTitle

                textSize = 18f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.rgb(7, 89, 133)
                )

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        titleStrip.addView(
            titleView,
            LinearLayout.LayoutParams(
                0,
                48,
                1f
            )
        )

        root.addView(
            titleStrip,
            LinearLayout.LayoutParams(
                -1,
                48
            )
        )

        /*
         * =====================================================
         * CONTENT
         * =====================================================
         */
                 val scroll =
             ScrollView(this).apply {

                isFillViewport = true

                 setBackgroundColor(
                    Color.rgb(248, 250, 252)
                )
            }

        bodyBox =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    14,
                    14,
                    14,
                    100
                )
            }

        scroll.addView(
    bodyBox,
    FrameLayout.LayoutParams(
        -1,
        -2
    )
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
 * =====================================================
 * MASTER COMMON BOTTOM BAR
 * =====================================================
 */

val section =
    when (activeSection) {
        "HOME" ->
            CommonPageUi.Section.HOME

        "CHANNEL" ->
            CommonPageUi.Section.CHANNEL

        "NOTICE" ->
            CommonPageUi.Section.NOTICE

        "IMP_INFO" ->
            CommonPageUi.Section.IMP_INFO

        else ->
            CommonPageUi.Section.MORE
    }


val bottomBar =
    CommonPageUi.createBottomBar(
        this,
        section
    )

root.addView(
    bottomBar,
    LinearLayout.LayoutParams(
        -1,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
)

        /*
         * =====================================================
         * NAVIGATION BAR INSET
         * =====================================================
         */
        ViewCompat.setOnApplyWindowInsetsListener(
            bottomBar
        ) { view, insets ->

            val navigationBottom =
                insets.getInsets(
                    WindowInsetsCompat.Type.navigationBars()
                ).bottom

            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                navigationBottom + 4
            )

            insets
        }

        return root
    }
/*
 * =========================================================
 * CONTENT PAGE SHARE
 * =========================================================
 */
private fun shareContentPage(
    pageTitle: String
) {
    val shareText = buildString {
        append("📱 Shiksha Rojgar App")
        append("\n📌 ")
        append(pageTitle)

        append("\n\n✅ शिक्षा रोजगार की Official Website 🌐")
        append("\nhttps://www.shiksharojgar.com/")

        append(
            "\n\n☝️ यहां क्लिक करके 📱 Shiksha Rojgar App " +
                "डाउनलोड इंस्टॉल करें !"
        )
        append(
            "\nhttps://www.shiksharojgar.com/2026/09/" +
                "shiksha-rojgar-android-app-latest.html"
        )
    }

    ShareHelper.shareImageOrText(
        this,
        pageTitle,
        shareText,
        chooserTitle = "पेज शेयर करें"
    )
}
    /*
     * =========================================================
     * BOTTOM BUTTON
     * =========================================================
     */
    private fun bottomButton(
        textValue: String,
        action: () -> Unit
    ): Button {

        return Button(this).apply {

            text = textValue

            textSize = 10f

            isAllCaps = false

            setTextColor(
                Color.rgb(30, 41, 59)
            )

            setBackgroundColor(
                Color.TRANSPARENT
            )

            gravity =
                Gravity.CENTER

            setOnClickListener {
                action()
            }
        }
    }

    /*
     * =========================================================
     * HOME
     * =========================================================
     */
    private fun goHome() {

        val intent =
            Intent(
                this,
                MainActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

        startActivity(intent)

        finish()
    }

    /*
     * =========================================================
     * IMPORTANT INFORMATION
     * =========================================================
     */
    private fun openImportantInformation() {

        val intent =
            Intent(
                this,
                ContentPageActivity::class.java
            ).apply {

                putExtra(
                    "pageId",
                    "important_information"
                )

                putExtra(
                    "pageTitle",
                    "📌 Important Information"
                )
            }

        startActivity(intent)
    }

    /*
     * =========================================================
     * MORE
     * =========================================================
     */
    private fun openMore() {

        /*
         * अभी existing More/Menu system को नहीं छेड़ रहे हैं।
         * बाद में इसी जगह MainActivity का existing menu
         * common तरीके से जोड़ा जाएगा।
         */

        Toast.makeText(
            this,
            "More",
            Toast.LENGTH_SHORT
        ).show()
    }

    

    /*
     * =========================================================
     * FIRESTORE CONTENT
     * =========================================================
     */
    private fun load(
        pageId: String,
        fallback: String
    ) {

        db.collection("home_pages")
            .document(pageId)
            .get()
            .addOnSuccessListener { d ->

                val title =
                    d.getString("title")
                        .orEmpty()
                        .ifBlank {
                            fallback
                        }

                                val body = d.getString("body").orEmpty()


                val image =
                    d.getString("imageUrl")
                        .orEmpty()

                titleView.text =
                    title

                bodyBox.removeAllViews()

                

                /*
                 * IMAGE
                 */
                if (image.isNotBlank()) {

                    val imageView =
                        ImageView(this).apply {

                            adjustViewBounds =
                                true

                            scaleType =
                                ImageView.ScaleType.CENTER_CROP

                            setBackgroundColor(
                                Color.WHITE
                            )
                        }

                    bodyBox.addView(
                        imageView,
                        LinearLayout.LayoutParams(
                            -1,
                            220
                        ).apply {
                            bottomMargin = 12
                        }
                    )

                    ChannelRepository.loadImage(
                        image
                    ) { bitmap, _ ->

                        runOnUiThread {

                            if (bitmap != null) {
                                imageView.setImageBitmap(
                                    bitmap
                                )
                            }
                        }
                    }
                }

                /*
                 * BODY
                 */
                bodyBox.addView(
                    TextView(this).apply {

                        text = body.ifBlank {
    "इस पेज की सामग्री अभी Admin Panel से जोड़ी नहीं गई है।"
}

                        textSize = 16f

                        setTextColor(
                            Color.rgb(30, 41, 59)
                        )

                        setLineSpacing(
                            4f,
                            1.05f
                        )
                    }
                )
                loadCategoryPosts(pageId)
            }
            .addOnFailureListener {

                bodyBox.addView(
                    TextView(this).apply {

                        text =
                            "सामग्री लोड नहीं हो सकी। कृपया बाद में फिर प्रयास करें।"

                        textSize = 16f
                    }
                )
            }
    }
        private fun loadCategoryPosts(pageId: String) {

        val categoryKey = when (pageId) {
            "notices", "notice" -> "notice"
            "important_information" -> "important_information"
            "study_material" -> "study_material"
            "career", "career_guide" -> "career_guide"
            else -> return
        }

        db.collection("category_posts")
            .whereEqualTo("category", categoryKey)
            .get()
            .addOnSuccessListener { snapshot ->

                val posts = snapshot.documents.filter {
                    it.getBoolean("enabled") ?: true
                }.sortedByDescending {
                    it.getLong("createdAt") ?: 0L
                }

                if (posts.isEmpty()) return@addOnSuccessListener
                
                if (categoryKey == "important_information") {
                    val newestTime = posts
                        .mapNotNull {
                            it.getLong("createdAt")
                        }
                        .maxOrNull()

                    if (newestTime != null) {
                        getSharedPreferences(
                            "sr_notifications",
                            MODE_PRIVATE
                        ).edit()
                            .putLong(
                                "imp_info_last_seen_at",
                                newestTime
                            )
                            .apply()

                        sendBroadcast(
                            android.content.Intent(
                                "com.shiksharojgar.app.UNREAD_COUNTS_CHANGED"
                            ).setPackage(packageName)
                        )
                    }
                }
                bodyBox.addView(
                    TextView(this).apply {
                        text = "📢 नई पोस्ट और अपडेट"
                        textSize = 21f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(Color.rgb(7, 89, 133))
                        setPadding(0, dp(20), 0, dp(10))
                    }
                )

                                posts.forEach { doc ->

                    // पोस्ट का View Count रिकॉर्ड करें।
                    AnalyticsTracker.uniqueCategoryPostView(
                        this@ContentPageActivity,
                        doc.id
                    )

                    

    val card = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(
            dp(12), dp(12), dp(12), dp(12)
        )
        background =
            android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.WHITE)
                setStroke(dp(2), Color.rgb(25, 118, 210))
                cornerRadius = dp(8).toFloat()
            }
    }

val categoryStyle = when (categoryKey) {
    "notice" ->
        "📢  NOTICE / नोटिस" to "#D32F2F"

    "study_material" ->
        "📚  STUDY MATERIAL / पढ़ाई" to "#2E7D32"

    "career_guide" ->
        "🎯  CAREER / JOBS" to "#EF6C00"

    else ->
        "📌  IMPORTANT INFORMATION" to "#6A1B9A"
}

val categoryStrip = TextView(this).apply {
    text = categoryStyle.first
    textSize = 11f
    typeface = Typeface.DEFAULT_BOLD
    setTextColor(Color.WHITE)
    gravity = Gravity.CENTER_VERTICAL
    setPadding(dp(12), dp(4), dp(12), dp(4))

    background =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor(categoryStyle.second))
            cornerRadius = dp(10).toFloat()
        }
}

card.addView(
    categoryStrip,
    LinearLayout.LayoutParams(
        -1,
        -2
    ).apply {
        bottomMargin = dp(10)
    }
)

                    val title = doc.getString("title").orEmpty()
                    val body = doc.getString("body").orEmpty()
                    val description =
                        doc.getString("description").orEmpty()
                    val imageUrl =
                        doc.getString("imageUrl").orEmpty()
                    val fileUrl =
                        doc.getString("fileUrl").orEmpty()
                    val fileName =
                        doc.getString("fileName").orEmpty()
                    
                    val websiteUrl =
                        doc.getString("websiteUrl").orEmpty()

                    val extraUrl =
                        doc.getString("extraUrl").orEmpty()

// CHANNEL STYLE: CATEGORY + DATE + TIME
val createdAt = doc.getLong("createdAt") ?: 0L

val dateTimeText = if (createdAt > 0L) {
    java.text.SimpleDateFormat(
        "dd MMM yyyy, hh:mm a",
        java.util.Locale("hi", "IN")
    ).format(java.util.Date(createdAt))
} else {
    "दिनांक उपलब्ध नहीं"
}

card.addView(
    TextView(this).apply {
        text = "📅 $dateTimeText"
        textSize = 11f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.rgb(14, 91, 215))
        setPadding(0, 0, 0, dp(8))
    }
)
                    if (title.isNotBlank()) {
                        card.addView(
                            TextView(this).apply {
                                text = title
                                textSize = 19f
                                typeface = Typeface.DEFAULT_BOLD
                                setTextColor(Color.rgb(15, 76, 129))
                            }
                        )
                    }

                    if (imageUrl.isNotBlank()) {
                        val imageView = ImageView(this).apply {
                            adjustViewBounds = true
                            scaleType = ImageView.ScaleType.FIT_CENTER
                        }

                        card.addView(
                            imageView,
                            LinearLayout.LayoutParams(
    -1,
    -2
).apply {
    topMargin = dp(8)
    bottomMargin = dp(8)
}
                        )

                        
                        ChannelRepository.loadImage(imageUrl) {
                            bitmap, _ ->
                            runOnUiThread {
                                if (bitmap != null) {
                                    imageView.setImageBitmap(bitmap)
                                
                         imageView.setOnClickListener {
    ZoomImageViewer.show(
        this@ContentPageActivity,
        bitmap
    )
}
                                }
                            }
                        }

                    }

                    if (body.isNotBlank()) {
                        card.addView(
                            TextView(this).apply {
                                text = body
                                textSize = 16f
                                setTextColor(Color.rgb(30, 41, 59))
                                setPadding(0, dp(6), 0, dp(6))
                            }
                        )
                    }

                    if (description.isNotBlank()) {
                        card.addView(
                            TextView(this).apply {
                                text = description
                                textSize = 15f
                                setTextColor(Color.DKGRAY)
                                setPadding(0, dp(4), 0, dp(4))
                            }
                        )
                    }

fun styleCategoryButton(
    button: Button,
    colorHex: String
) {
    button.setTextColor(Color.WHITE)
    button.textSize = 14f
    button.typeface = Typeface.DEFAULT_BOLD
    button.isAllCaps = false
    button.background =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor(colorHex))
            cornerRadius = dp(12).toFloat()
            setStroke(dp(1), Color.WHITE)
        }
    button.setPadding(
        dp(12), dp(10), dp(12), dp(10)
    )
}
val categoryButtonRow = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
}

val categoryButtonScroll = HorizontalScrollView(this).apply {
    isHorizontalScrollBarEnabled = false
    isFillViewport = false
    addView(
        categoryButtonRow,
        android.widget.FrameLayout.LayoutParams(
            -2,
            -2
        )
    )
}
                    if (websiteUrl.isNotBlank()) {
                        val websiteButton = Button(this).apply {
                            text = "🌐 वेबसाइट खोलें"
                            styleCategoryButton(this, "#00897B")
                            setOnClickListener {
                                val url = if (
                                    websiteUrl.startsWith("http://") ||
                                    websiteUrl.startsWith("https://")
                                ) websiteUrl else "https://$websiteUrl"

                                runCatching {
                                    startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            android.net.Uri.parse(url)
                                        )
                                    )
                                }
                            }
                        }
                        card.addView(websiteButton)
                    }

                    
if (fileUrl.isNotBlank()) {
    val pdfButton = Button(this).apply {
        text = "📄 " + fileName.ifBlank {
            "PDF / अध्ययन सामग्री खोलें"
        }

        styleCategoryButton(this, "#6A1B9A")

        setOnClickListener {
            runCatching {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        android.net.Uri.parse(fileUrl)
                    )
                )
            }
        }
    }

    card.addView(pdfButton)
}


                    // अतिरिक्त URL बटन
                    if (extraUrl.isNotBlank()) {
                        card.addView(
                            Button(this).apply {
                                text = "🔗 अतिरिक्त लिंक खोलें"
                                styleCategoryButton(this, "#EF6C00")
                                setOnClickListener {
                                    val url = if (
                                        extraUrl.startsWith("http://") ||
                                        extraUrl.startsWith("https://")
                                    ) extraUrl else "https://$extraUrl"

                                    runCatching {
                                        startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                android.net.Uri.parse(url)
                                            )
                                        )
                                    }
                                }
                            }
                        )
                    }

                                        // 🌐 वेबसाइट और YouTube लिंक प्रीव्यू
                    val previewLinks = buildString {
                        appendLine(body)
                        appendLine(description)
                        appendLine(websiteUrl)
                        appendLine(extraUrl)
                    }

                    LinkPreviewUi.addPreviews(
                        context = this,
                        container = card,
                        text = previewLinks,
                        maxLinks = 2
                    )
                    
                    // Admin द्वारा बनाए गए कस्टम बटन
                    val customButtons =
                        (doc.get("buttons") as? List<*>)
                            ?.mapNotNull { raw ->
                                val item = raw as? Map<*, *>
                                    ?: return@mapNotNull null

                                val label =
                                    item["label"] as? String ?: ""
                                val url =
                                    item["url"] as? String ?: ""
                                val enabled =
                                    item["enabled"] as? Boolean ?: true

                                if (enabled && url.isNotBlank()) {
                                    label.ifBlank { "लिंक खोलें" } to url
                                } else {
                                    null
                                }
                            } ?: emptyList()

                    customButtons.forEach { (label, rawUrl) ->
                        card.addView(
                            Button(this).apply {
                                text = "🔗 $label"
                                styleCategoryButton(this, "#1565C0")
                                setOnClickListener {
                                    val url = if (
                                        rawUrl.startsWith("http://") ||
                                        rawUrl.startsWith("https://")
                                    ) rawUrl else "https://$rawUrl"

                                    runCatching {
                                        startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                android.net.Uri.parse(url)
                                            )
                                        )
                                    }
                                }
                            }
                        )
                    }

                                        // Like और Share की एक ही लाइन
val actionRow = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    setPadding(0, dp(8), 0, dp(4))
}

var currentLiked = false
var currentLikeCount = doc.getLong("likeCount") ?: 0L
var displayedShareCount = doc.getLong("shareCount") ?: 0L

val likeButton = TextView(this).apply {
    text = "♥️"
    textSize = 21f
    setTextColor(Color.GRAY)
    setPadding(dp(6), dp(5), dp(4), dp(5))
}

val likeCountLabel = TextView(this).apply {
    text = currentLikeCount.toString()
    textSize = 14f
    setTextColor(Color.DKGRAY)
    setPadding(dp(2), 0, dp(12), 0)
}

val shareButton = TextView(this).apply {
    text = "📤 Share ↗️"
    textSize = 14f
    typeface = Typeface.DEFAULT_BOLD
    setTextColor(Color.rgb(7, 89, 133))
    setPadding(dp(8), dp(6), dp(4), dp(6))
}

val shareCountLabel = TextView(this).apply {
    text = displayedShareCount.toString()
    textSize = 14f
    setTextColor(Color.DKGRAY)
    setPadding(dp(3), 0, dp(6), 0)
}

// पहले से Like की स्थिति जाँचें।
likeButton.isEnabled = false

CategoryPostRepository.isLiked(doc.id) { liked ->
    runOnUiThread {
        if (!isFinishing && !isDestroyed) {
            currentLiked = liked

            likeButton.text =
                if (liked) "❤️" else "♥️"

            likeButton.setTextColor(
                if (liked) Color.RED else Color.GRAY
            )

            likeButton.isEnabled = true
        }
    }
}

// Like / Unlike
likeButton.setOnClickListener {
    likeButton.isEnabled = false

    val newLiked = !currentLiked

    CategoryPostRepository.setLiked(
        doc.id,
        newLiked
    ) { success ->

        runOnUiThread {
            likeButton.isEnabled = true

            if (success) {
                currentLiked = newLiked

                currentLikeCount = (
                    currentLikeCount +
                        if (newLiked) 1L else -1L
                ).coerceAtLeast(0L)

                likeButton.text =
                    if (currentLiked) "❤️" else "♥️"

                likeButton.setTextColor(
                    if (currentLiked) Color.RED else Color.GRAY
                )

                likeCountLabel.text =
                    currentLikeCount.toString()

            } else {
                Toast.makeText(
                    this@ContentPageActivity,
                    "Like सेव नहीं हुआ। इंटरनेट जाँचें।",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

// पोस्ट शेयर करने का पूरा टेक्स्ट
val shareText = buildString {

    val categoryHeader = when (pageId) {
        "career", "career_guide", "career_tools" ->
            "📱 Shiksha Rojgar App 🛠️ Career / Useful Tools"

        "study_material" ->
            "📱 Shiksha Rojgar App 📚 Study / Syllabus"

        else ->
            "📱 Shiksha Rojgar App 📌 Imp Info"
    }

    append("*")
    append(categoryHeader)
    append("*")

    append("\n*पोस्ट देखें 👇*")
    append("\n")
    append(CommonShareBuilder.directPostUrl(doc.id))

    if (title.isNotBlank()) {
        append("\n\n*")
        append(title.trim())
        append("*")
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
        val url = if (
            websiteUrl.startsWith("http://") ||
            websiteUrl.startsWith("https://")
        ) websiteUrl else "https://$websiteUrl"

        append("\n\n🌐 वेबसाइट लिंक 👇\n")
        append(url)
    }

    if (extraUrl.isNotBlank()) {
        val url = if (
            extraUrl.startsWith("http://") ||
            extraUrl.startsWith("https://")
        ) extraUrl else "https://$extraUrl"

        append("\n\n🔗 अतिरिक्त लिंक 👇\n")
        append(url)
    }

    customButtons.forEach { (label, rawUrl) ->
        val url = if (
            rawUrl.startsWith("http://") ||
            rawUrl.startsWith("https://")
        ) rawUrl else "https://$rawUrl"

        append("\n\n🔗 ")
        append(label)
        append(" 👇\n")
        append(url)
    }

    if (fileUrl.isNotBlank()) {
        append("\n\n📄 ")
        append(fileName.ifBlank { "फाइल / PDF" })
        append(" 👇\n")
        append(fileUrl)
    }

    append("\n\n*✅ शिक्षा रोजगार की Official Website 🌐*")
    append("\nhttps://www.shiksharojgar.com/")

    append(
        "\n*☝️ यहां क्लिक करके 📱 Shiksha Rojgar App " +
            "डाउनलोड इंस्टॉल करें !*"
    )
}

// Share और Share Count
shareButton.setOnClickListener {

    AnalyticsTracker.recordCategoryPostShare(
        this@ContentPageActivity,
        doc.id
    )

    displayedShareCount++

    shareCountLabel.text =
        displayedShareCount.toString()

    try {
        ShareHelper.shareImageOrText(
            this@ContentPageActivity,
            title,
            shareText,
            imageUrl,
            "पोस्ट शेयर करें"
        )
    } catch (_: Exception) {
        Toast.makeText(
            this@ContentPageActivity,
            "पोस्ट शेयर नहीं हो सकी।",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// सभी बटन एक ही लाइन में
actionRow.addView(likeButton)
actionRow.addView(likeCountLabel)
actionRow.addView(shareButton)
actionRow.addView(shareCountLabel)

card.addView(actionRow)

                    val shareCountLabel = TextView(this).apply {
                        text = "📤 Share ↗️: $displayedShareCount"
                        textSize = 13f
                        setTextColor(Color.rgb(71, 85, 105))
                        setPadding(0, dp(8), 0, dp(8))
                    }

                    card.addView(shareCountLabel)
                    
                    // पोस्ट को टेक्स्ट और क्लिक करने योग्य URL के साथ शेयर करें
                    card.addView(
                        Button(this).apply {
                            text = "📤 Share ↗️"
                            styleCategoryButton(this, "#D32F2F")
                            
                                                        setOnClickListener {

                                // हर Share टैप पर संख्या बढ़ाएँ।
                                AnalyticsTracker.recordCategoryPostShare(
                                    this@ContentPageActivity,
                                    doc.id
                                )

                                displayedShareCount++
                                shareCountLabel.text =
                                    "📤 Share ↗️: $displayedShareCount"

                                val shareText = buildString {
    val categoryHeader = when (pageId) {
    "career", "career_guide", "career_tools" ->
        "📱 Shiksha Rojgar App 🛠️ Career / Useful Tools"

    "study_material" ->
        "📱 Shiksha Rojgar App 📚 Study / Syllabus"

    else ->
        "📱 Shiksha Rojgar App 📌 Imp Info"
}

    append("*")
    append(categoryHeader)
    append("*")

    append("\n*पोस्ट देखें 👇*")
    append("\n")
    append(CommonShareBuilder.directPostUrl(doc.id))

    if (title.isNotBlank()) {
        append("\n\n*")
        append(title.trim())
        append("*")
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
        val url = if (
            websiteUrl.startsWith("http://") ||
            websiteUrl.startsWith("https://")
        ) websiteUrl else "https://$websiteUrl"

        append("\n\n🌐 वेबसाइट लिंक 👇\n")
        append(url)
    }

    if (extraUrl.isNotBlank()) {
        val url = if (
            extraUrl.startsWith("http://") ||
            extraUrl.startsWith("https://")
        ) extraUrl else "https://$extraUrl"

        append("\n\n🔗 अतिरिक्त लिंक 👇\n")
        append(url)
    }

    customButtons.forEach { (label, rawUrl) ->
        val url = if (
            rawUrl.startsWith("http://") ||
            rawUrl.startsWith("https://")
        ) rawUrl else "https://$rawUrl"

        append("\n\n🔗 ")
        append(label)
        append(" 👇\n")
        append(url)
    }

    if (fileUrl.isNotBlank()) {
        append("\n\n📄 ")
        append(fileName.ifBlank { "फाइल / PDF" })
        append(" 👇\n")
        append(fileUrl)
    }

    append("\n\n*✅ शिक्षा रोजगार की Official Website 🌐*")
    append("\nhttps://www.shiksharojgar.com/")

    append(
        "\n*☝️ यहां क्लिक करके 📱 Shiksha Rojgar App " +
            "डाउनलोड इंस्टॉल करें !*"
    )
}

                                ShareHelper.shareImageOrText(
    this@ContentPageActivity,
    title,
    shareText,
    imageUrl,
    "पोस्ट शेयर करें"
)
                            }
                        }
                    )
                    
                    bodyBox.addView(
                        card,
                        LinearLayout.LayoutParams(
                            -1, -2
                        ).apply {
                            bottomMargin = dp(12)
                        }
                    )
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "कैटेगरी पोस्ट लोड नहीं हो सकीं।",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
}
