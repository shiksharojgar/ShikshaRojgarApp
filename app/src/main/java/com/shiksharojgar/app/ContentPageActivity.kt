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
                    Color.rgb(248, 250, 252)
                )
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
            openMore()
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

    val shareText =
        """
        📱 Shiksha Rojgar App 📌 Imp Info

        $pageTitle

        Shiksha Rojgar App
        https://shiksha-rojgar.web.app/

        ऐप में देखें:
        shiksharojgar://channel
        """.trimIndent()

    val intent =
        Intent(Intent.ACTION_SEND).apply {

            type = "text/plain"

            putExtra(
                Intent.EXTRA_TEXT,
                shareText
            )
        }

    startActivity(
        Intent.createChooser(
            intent,
            "Share"
        )
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

                val body =
                    d.getString("body")
                        .orEmpty()

                val image =
                    d.getString("imageUrl")
                        .orEmpty()

                titleView.text =
                    title

                bodyBox.removeAllViews()

                /*
                 * CONTENT TITLE
                 */
                bodyBox.addView(
                    TextView(this).apply {

                        text = title

                        textSize = 23f

                        typeface =
                            Typeface.DEFAULT_BOLD

                        setTextColor(
                            Color.rgb(7, 89, 133)
                        )

                        setPadding(
                            0,
                            0,
                            0,
                            12
                        )
                    }
                )

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

                        text =
                            body.ifBlank {
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

                    val card = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(
                            dp(12), dp(12), dp(12), dp(12)
                        )
                        setBackgroundColor(Color.WHITE)
                    }

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
                                -1, dp(220)
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
                                        val largeImage =
                                            ImageView(this).apply {
                                                setImageBitmap(bitmap)
                                                adjustViewBounds = true
                                                scaleType =
                                                    ImageView.ScaleType.FIT_CENTER
                                                setBackgroundColor(Color.BLACK)
                                            }

                                        AlertDialog.Builder(this)
                                            .setTitle("🖼️ पोस्ट की फोटो")
                                            .setView(largeImage)
                                            .setPositiveButton("बंद करें", null)
                                            .show()
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

                    if (websiteUrl.isNotBlank()) {
                        val websiteButton = Button(this).apply {
                            text = "🌐 वेबसाइट खोलें"
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
