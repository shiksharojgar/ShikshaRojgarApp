package com.shiksharojgar.app

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import android.graphics.BitmapFactory
import java.net.URL

class HomeCategoryActivity : AppCompatActivity() {

    private val db =
        FirebaseFirestore.getInstance()

    private lateinit var listLayout: LinearLayout
private lateinit var subCategoryLayout: LinearLayout
private lateinit var postsLayout: LinearLayout
private lateinit var unreadBadge: TextView

private var categoryPostsListener:
    com.google.firebase.firestore.ListenerRegistration? = null
    
private lateinit var categoryPageTitleView: TextView
private lateinit var categoryPageDescriptionView: TextView
private lateinit var categoryBannerImageView: ImageView

private var categoryLayoutStyle = "standard"
private var customBannerImageUrl = ""
    

private val sliderHandler =
    android.os.Handler(android.os.Looper.getMainLooper())

private var sliderIndex = 0
    private var customSliderImageUrls: List<String> = emptyList()

private fun showSchoolSliderImage(index: Int) {
    if (customSliderImageUrls.isEmpty()) {
        categoryBannerImageView.setImageResource(
            sliderImages[index.coerceIn(0, sliderImages.lastIndex)]
        )
        return
    }

    val imageUrl = customSliderImageUrls[
        index.coerceIn(0, customSliderImageUrls.lastIndex)
    ]

    if (!imageUrl.startsWith("https://")) return

    Thread {
        try {
            val connection = URL(imageUrl).openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val bitmap = connection.getInputStream().use {
                BitmapFactory.decodeStream(it)
            }

            runOnUiThread {
                if (!isFinishing && !isDestroyed && bitmap != null) {
                    categoryBannerImageView.setImageBitmap(bitmap)
                }
            }
        } catch (_: Exception) {
            // फोटो लोड न होने पर वर्तमान फोटो बनी रहेगी।
        }
    }.start()
}
private fun showCategorySlide(index: Int) {
    val total = if (customSliderImageUrls.isNotEmpty()) {
        customSliderImageUrls.size
    } else {
        sliderImages.size
    }

    if (total == 0) return

    sliderIndex = ((index % total) + total) % total

    if (customSliderImageUrls.isNotEmpty()) {
        showSchoolSliderImage(sliderIndex)
    } else {
        categoryBannerImageView.setImageResource(
            sliderImages[sliderIndex]
        )
    }
}

private val sliderImages = intArrayOf(
    R.drawable.home_teacher,
    R.drawable.home_student,
    R.drawable.home_exam
)

private val sliderRunnable = object : Runnable {
    override fun run() {
        if (isFinishing || isDestroyed) return
        if (customBannerImageUrl.isNotBlank() &&
    customSliderImageUrls.isEmpty()
) return

        showCategorySlide(sliderIndex + 1)

         {
            if (customSliderImageUrls.isNotEmpty()) {
    showSchoolSliderImage(sliderIndex)
} else {
    image.setImageResource(sliderImages[sliderIndex])
}
        }

        sliderHandler.postDelayed(this, 4500L)
    }
}


    private val colors = listOf(
        Color.rgb(46, 125, 50),
        Color.rgb(25, 118, 210),
        Color.rgb(123, 31, 162),
        Color.rgb(239, 108, 0),
        Color.rgb(0, 121, 107),
        Color.rgb(198, 40, 40),
        Color.rgb(2, 119, 189),
        Color.rgb(85, 139, 47)
    )

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    CommonPageUi.applyBlueStatusBar(this)

    val categoryId = intent.getStringExtra("categoryId") ?: ""
    val categoryName = intent.getStringExtra("categoryName") ?: "Category"
    val categoryIcon = intent.getStringExtra("categoryIcon") ?: ""

    setContentView(buildUi(categoryName, categoryIcon, categoryId))

    loadSubCategories(categoryId)
    loadCategoryPosts(categoryId)
    loadCategoryPageSettings(categoryId, categoryName, categoryIcon)
}

private fun loadCategoryPageSettings(
    categoryId: String,
    categoryName: String,
    categoryIcon: String
) {
    if (categoryId.isBlank()) return

    db.collection("category_page_settings")
        .document(categoryId)
        .get()
        .addOnSuccessListener { snapshot ->
            if (isFinishing || isDestroyed) return@addOnSuccessListener
            if (!snapshot.exists()) return@addOnSuccessListener

            val savedTitle = snapshot.getString("title").orEmpty()
            val description = snapshot.getString("description").orEmpty()
            val imageUrl = snapshot.getString("imageUrl").orEmpty()

            val titleText = savedTitle.ifBlank { categoryName }
            categoryPageTitleView.text =
                if (categoryIcon.isNotBlank()) {
                    "$categoryIcon  $titleText"
                } else {
                    titleText
                }

            categoryPageDescriptionView.text = description
            categoryPageDescriptionView.visibility =
                if (description.isBlank()) View.GONE else View.VISIBLE

            categoryLayoutStyle =
                snapshot.getString("layoutStyle") ?: "standard"

            customBannerImageUrl = imageUrl
            customSliderImageUrls = listOf(
    snapshot.getString("sliderImage1").orEmpty(),
    snapshot.getString("sliderImage2").orEmpty(),
    snapshot.getString("sliderImage3").orEmpty()
).filter { it.startsWith("https://") }

if (categoryId.equals("school", ignoreCase = true) &&
    customSliderImageUrls.isNotEmpty()
) {
    sliderHandler.removeCallbacks(sliderRunnable)
    sliderIndex = 0
    showSchoolSliderImage(sliderIndex)
    sliderHandler.postDelayed(sliderRunnable, 4500L)
}
if (
    imageUrl.startsWith("https://") &&
    !(
        categoryId.equals("school", ignoreCase = true) &&
        customSliderImageUrls.isNotEmpty()
    )
) {
                sliderHandler.removeCallbacks(sliderRunnable)

                Thread {
                    try {
                        val connection = URL(imageUrl).openConnection()
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000

                        val bitmap = connection.getInputStream().use {
                            BitmapFactory.decodeStream(it)
                        }

                        runOnUiThread {
                            if (!isFinishing && !isDestroyed && bitmap != null) {
                                categoryBannerImageView.setImageBitmap(bitmap)
                            }
                        }
                    } catch (_: Exception) {
                        runOnUiThread {
                            if (!isFinishing && !isDestroyed) {
                                Toast.makeText(
                                    this,
                                    "कैटेगरी की फोटो लोड नहीं हुई। फोटो URL जाँचें।",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }.start()
            }

            loadCategoryPosts(categoryId)
        }
        .addOnFailureListener {
            if (!isFinishing && !isDestroyed) {
                Toast.makeText(
                    this,
                    "कैटेगरी सेटिंग लोड नहीं हुई। इंटरनेट और Firestore Rules जाँचें।",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
}


    /*
     * =========================================================
     * COMMON PAGE UI
     * =========================================================
     */
    private fun buildUi(
    categoryName: String,
    categoryIcon: String,
    categoryId: String
): LinearLayout {

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setBackgroundColor(
                    Color.rgb(248, 250, 252)
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
                    dp(10),
                    0,
                    dp(10),
                    0
                )

                setBackgroundColor(
                    Color.rgb(219, 242, 255)
                )
            }

        val back =
            TextView(this).apply {

                text = "←"

                textSize = 25f

                setTextColor(
                    Color.rgb(7, 89, 133)
                )

                gravity =
                    Gravity.CENTER

                setOnClickListener {
                    finish()
                }
            }

        titleStrip.addView(
            back,
            LinearLayout.LayoutParams(
                dp(44),
                dp(48)
            )
        )

        
categoryPageTitleView = TextView(this).apply {
    text = if (categoryIcon.isNotBlank()) {
        "$categoryIcon  $categoryName"
    } else {
        categoryName
    }

    textSize = 18f
    typeface = Typeface.DEFAULT_BOLD
    setTextColor(Color.rgb(7, 89, 133))
    gravity = Gravity.CENTER_VERTICAL
}


                

        titleStrip.addView(
            categoryPageTitleView,
            LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
            )
        )

        
        /*
         * =====================================================
         * COMMON HEADER FOR SIX MAIN CATEGORIES
         * Other pages retain their existing title strip.
         * =====================================================
         */

        val useCommonHeader =
            categoryId.lowercase() in setOf(
                "teacher",
                "student",
                "school",
                "vacancy",
                "result",
                "admit_card"
            )

        categoryPageTitleView =
            TextView(this).apply {

                text =
                    if (categoryIcon.isNotBlank()) {
                        "$categoryIcon  $categoryName"
                    } else {
                        categoryName
                    }

                textSize = 18f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.rgb(7, 89, 133)
                )

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        if (useCommonHeader) {

            val header =
                CommonPageUi.createHeader(
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

        } else {

            val titleStrip =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.HORIZONTAL

                    gravity =
                        Gravity.CENTER_VERTICAL

                    setPadding(
                        dp(10),
                        0,
                        dp(10),
                        0
                    )

                    setBackgroundColor(
                        Color.rgb(219, 242, 255)
                    )
                }

            val back =
                TextView(this).apply {

                    text = "←"

                    textSize = 25f

                    setTextColor(
                        Color.rgb(7, 89, 133)
                    )

                    gravity =
                        Gravity.CENTER

                    setOnClickListener {
                        finish()
                    }
                }

            titleStrip.addView(
                back,
                LinearLayout.LayoutParams(
                    dp(44),
                    dp(48)
                )
            )

            titleStrip.addView(
                categoryPageTitleView,
                LinearLayout.LayoutParams(
                    0,
                    dp(48),
                    1f
                )
            )

            root.addView(
                titleStrip,
                LinearLayout.LayoutParams(
                    -1,
                    dp(48)
                )
            )
        }


        /*
         * =====================================================
         * SUB CATEGORY CONTENT
         * =====================================================
         */
        val scroll =
            ScrollView(this).apply {

                isFillViewport = true
            }

        listLayout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(16),
                    dp(16),
                    dp(16),
                    dp(100)
                )
            }

            
        // 1. पूरी स्क्रीन चौड़ाई वाला HD इमेज स्लाइडर

val banner = FrameLayout(this).apply {
    background = GradientDrawable().apply {
        setColor(Color.WHITE)
        cornerRadius = dp(16).toFloat()
    }
    clipToOutline = true
    setPadding(0, dp(4), 0, dp(8))
}

categoryBannerImageView = ImageView(this).apply {
    id = R.id.categoryBannerImage
    setImageResource(sliderImages[0])
    scaleType = ImageView.ScaleType.FIT_CENTER
    adjustViewBounds = true
    contentDescription = "कैटेगरी बैनर फोटो"
}

banner.addView(
    categoryBannerImageView,
    FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    )
)

val previousButton = Button(this).apply {
    text = "‹"
    textSize = 30f
    setTextColor(Color.WHITE)
    background = GradientDrawable().apply {
        setColor(0x99000000.toInt())
        cornerRadius = dp(8).toFloat()
    }
    minWidth = 0
    minHeight = 0
    setPadding(0, 0, 0, 0)
    setOnClickListener {
        showCategorySlide(sliderIndex - 1)
    }
}

val nextButton = Button(this).apply {
    text = "›"
    textSize = 30f
    setTextColor(Color.WHITE)
    background = GradientDrawable().apply {
        setColor(0x99000000.toInt())
        cornerRadius = dp(8).toFloat()
    }
    minWidth = 0
    minHeight = 0
    setPadding(0, 0, 0, 0)
    setOnClickListener {
        showCategorySlide(sliderIndex + 1)
    }
}

banner.addView(
    previousButton,
    FrameLayout.LayoutParams(
        dp(40),
        dp(48),
        Gravity.CENTER_VERTICAL or Gravity.START
    ).apply {
        marginStart = dp(8)
    }
)

banner.addView(
    nextButton,
    FrameLayout.LayoutParams(
        dp(40),
        dp(48),
        Gravity.CENTER_VERTICAL or Gravity.END
    ).apply {
        marginEnd = dp(8)
    }
)

listLayout.addView(
    banner,
    LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        marginStart = -dp(16)
        marginEnd = -dp(16)
    }
)

sliderHandler.removeCallbacks(sliderRunnable)
sliderHandler.postDelayed(sliderRunnable, 4500L)


categoryPageDescriptionView = TextView(this).apply {
    textSize = 14f
    setTextColor(Color.DKGRAY)
    setPadding(dp(4), dp(4), dp(4), dp(12))
    visibility = View.GONE
}

listLayout.addView(categoryPageDescriptionView)


// 2. सब-कैटेगरी का अलग हिस्सा
val subHeading = TextView(this).apply {
    text = when (categoryId.lowercase()) {
        "school" -> "📚 स्कूल शिक्षा की प्रमुख श्रेणियाँ"
        "teacher" -> "👩‍🏫 शिक्षक जानकारी की प्रमुख श्रेणियाँ"
        "student" -> "🎓 विद्यार्थी जानकारी की प्रमुख श्रेणियाँ"
        "vacancy" -> "💼 भर्ती की प्रमुख श्रेणियाँ"
        "result" -> "🏆 परिणाम की प्रमुख श्रेणियाँ"
        "admit_card" -> "🎫 प्रवेश पत्र की प्रमुख श्रेणियाँ"
        else -> "📚 विषय और सब-कैटेगरी"
    }
    textSize = 18f
    typeface = Typeface.DEFAULT_BOLD
    setTextColor(Color.rgb(25, 118, 210))
    setPadding(0, dp(10), 0, dp(8))
}

listLayout.addView(subHeading)


subCategoryLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
}

subCategoryRow = null
subCategoryRowCount = 0

listLayout.addView(subCategoryLayout)


// 3. नई पोस्ट का अलग हिस्सा
val postsHeading = TextView(this).apply {
    text = when (categoryId.lowercase()) {
        "school" -> "🔥 ताज़ा पोस्ट"
        "teacher" -> "🔥 शिक्षक अपडेट"
        "student" -> "🔥 विद्यार्थी अपडेट"
        "vacancy" -> "🔥 ताज़ा भर्तियाँ"
        "result" -> "🔥 ताज़ा परिणाम"
        "admit_card" -> "🔥 ताज़ा प्रवेश पत्र"
        else -> "📰 ताज़ा पोस्ट"
    }
    textSize = 18f
    typeface = Typeface.DEFAULT_BOLD
    setTextColor(Color.rgb(25, 118, 210))
    setPadding(0, dp(18), 0, dp(8))
}

listLayout.addView(postsHeading)

postsLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
}

listLayout.addView(postsLayout)

scroll.addView(listLayout)

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
 * SHARED BOTTOM BAR
 * =====================================================
 */
val bottomBar =
    CommonPageUi.createBottomBar(
        this,
        CommonPageUi.Section.MORE
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

        textSize = 12f

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

        startActivity(
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
        )
    }

    /*
     * =========================================================
     * MORE
     * =========================================================
     */
    private fun openMore() {

        Toast.makeText(
            this,
            "More",
            Toast.LENGTH_SHORT
        ).show()
    }

    /*
     * =========================================================
     * UNREAD BADGE
     * =========================================================
     */
    private fun updateUnreadBadge() {

        val prefs =
            getSharedPreferences(
                "sr_notifications",
                MODE_PRIVATE
            )

        val unread =
            prefs.getInt(
                "channel_unread",
                0
            )

        if (unread > 0) {

            unreadBadge.text =
                if (unread > 99) {
                    "99+"
                } else {
                    unread.toString()
                }

            unreadBadge.visibility =
                View.VISIBLE

        } else {

            unreadBadge.visibility =
                View.GONE
        }
    }

    /*
     * =========================================================
     * LOAD SUB CATEGORIES
     * =========================================================
     */
    
private fun loadSubCategories(
    categoryId: String
) {
    subCategoryLayout.removeAllViews()

    val progress = ProgressBar(this)

    subCategoryLayout.addView(
        progress,
        LinearLayout.LayoutParams(
            -1,
            dp(60)
        )
    )

    db.collection("home_subcategories")
        .whereEqualTo("parentId", categoryId)
        .whereEqualTo("enabled", true)
        .get()
        .addOnSuccessListener { result ->

            subCategoryLayout.removeAllViews()
            subCategoryRow = null
subCategoryRowCount = 0

            val items = result.documents.sortedBy {
                it.getLong("position") ?: 0L
            }

            
            if (items.isEmpty()) {
                val defaults = when (categoryId.lowercase()) {
                    "school" -> listOf(
                        Triple("स्कूल समाचार", "📰", ""),
                        Triple("शैक्षणिक कैलेंडर", "📅", ""),
                        Triple("पाठ्यक्रम एवं पुस्तकें", "📚", ""),
                        Triple("छात्रवृत्ति एवं योजनाएँ", "🎓", ""),
                        Triple("परीक्षा एवं परिणाम", "📝", ""),
                        Triple("विभागीय आदेश", "📜", "")
                    )

                    "teacher" -> listOf(
                        Triple("शिक्षक भर्ती", "👩‍🏫", ""),
                        Triple("स्थानांतरण एवं पदोन्नति", "🔁", ""),
                        Triple("प्रशिक्षण एवं पाठ्यक्रम", "📚", ""),
                        Triple("विभागीय आदेश", "📜", "")
                    )

                    "student" -> listOf(
                        Triple("पाठ्यक्रम एवं पुस्तकें", "📚", ""),
                        Triple("छात्रवृत्ति एवं योजनाएँ", "🎓", ""),
                        Triple("परीक्षा एवं परिणाम", "📝", ""),
                        Triple("अध्ययन सामग्री", "📖", "")
                    )

                    "vacancy" -> listOf(
                        Triple("नई भर्तियाँ", "💼", ""),
                        Triple("आवेदन एवं पात्रता", "📝", ""),
                        Triple("भर्ती परीक्षा", "📅", ""),
                        Triple("आधिकारिक सूचनाएँ", "📜", "")
                    )

                    "result" -> listOf(
                        Triple("नवीनतम परिणाम", "🏆", ""),
                        Triple("मेरिट सूची", "📋", ""),
                        Triple("कटऑफ एवं चयन सूची", "📊", ""),
                        Triple("आधिकारिक परिणाम लिंक", "🔗", "")
                    )

                    "admit_card" -> listOf(
                        Triple("नवीनतम प्रवेश पत्र", "🎫", ""),
                        Triple("परीक्षा तिथि", "📅", ""),
                        Triple("परीक्षा केंद्र निर्देश", "📍", ""),
                        Triple("डाउनलोड लिंक", "⬇️", "")
                    )

                    else -> emptyList()
                }

                if (defaults.isEmpty()) {
                    subCategoryLayout.addView(
                        TextView(this).apply {
                            text = "अभी कोई Sub-Category उपलब्ध नहीं है।"
                            textSize = 16f
                            gravity = Gravity.CENTER
                            setPadding(dp(16), dp(24), dp(16), dp(24))
                        }
                    )
                } else {
                    defaults.forEachIndexed { index, item ->
                        addSubCategoryButton(
                            name = item.first,
                            icon = item.second,
                            url = item.third,
                            pageType = "Website",
                            index = index
                        )
                    }
                }

                return@addOnSuccessListener
            }
            items.forEachIndexed { index, doc ->
                val name = doc.getString("name")
                    ?: "Sub-Category"

                val icon = doc.getString("icon")
                    ?: ""

                val url = doc.getString("url")
                    ?: ""

                val pageType = doc.getString("pageType")
                    ?: "Website"

                addSubCategoryButton(
                    name = name,
                    icon = icon,
                    url = url,
                    pageType = pageType,
                    index = index
                )
            }
        }
        .addOnFailureListener { e ->
            subCategoryLayout.removeAllViews()

            val errorText = TextView(this).apply {
                text = "Sub-Category लोड नहीं हुई। इंटरनेट और Firestore जाँचें।"
                textSize = 14f
                setPadding(
                    dp(8),
                    dp(16),
                    dp(8),
                    dp(16)
                )
            }

            subCategoryLayout.addView(errorText)

            Toast.makeText(
                this,
                "Sub-Category load failed: ${
                    e.message ?: "Firestore error"
                }",
                Toast.LENGTH_LONG
            ).show()
        }
}


private fun loadCategoryPosts(categoryId: String) {
    postsLayout.removeAllViews()

    if (categoryId.isBlank()) {
        return
    }

    val progress = ProgressBar(this)

    postsLayout.addView(
        progress,
        LinearLayout.LayoutParams(
            -1,
            dp(48)
        )
    )

    categoryPostsListener?.remove()

    categoryPostsListener = CategoryPostRepository.observePosts(
        categoryId,
        onResult = { posts ->
            postsLayout.removeAllViews()

            if (posts.isEmpty()) {
                val empty = TextView(this).apply {
                    text = "अभी इस कैटेगरी में कोई पोस्ट उपलब्ध नहीं है।"
                    textSize = 14f
                    setTextColor(Color.DKGRAY)
                    setPadding(dp(8), dp(16), dp(8), dp(16))
                }

                postsLayout.addView(empty)
                return@observePosts
            }

            posts.forEach { post ->

    // पोस्ट दिखने पर View Count रिकॉर्ड करें।
    AnalyticsTracker.uniqueCategoryPostView(
        this@HomeCategoryActivity,
        post.id
    )

    val isLargeImage = categoryLayoutStyle == "large_image"
val isCompact = categoryLayoutStyle == "compact"

val card = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(
        dp(if (isCompact) 6 else 10),
        dp(if (isCompact) 6 else 10),
        dp(if (isCompact) 6 else 10),
        dp(if (isCompact) 6 else 10)
    )
    background = GradientDrawable().apply {
        setColor(Color.WHITE)
        cornerRadius = dp(12).toFloat()
        setStroke(dp(1), Color.rgb(220, 230, 238))
    }
}

val row = LinearLayout(this).apply {
    orientation = if (isLargeImage) {
        LinearLayout.VERTICAL
    } else {
        LinearLayout.HORIZONTAL
    }
    gravity = Gravity.TOP
}

if (post.imageUrl.isNotBlank()) {
    val image = ImageView(this).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        adjustViewBounds = true
    }

    val imageParams = if (isLargeImage) {
        LinearLayout.LayoutParams(-1, dp(200))
    } else {
        LinearLayout.LayoutParams(
            dp(if (isCompact) 76 else 105),
            dp(if (isCompact) 80 else 110)
        )
    }

    row.addView(image, imageParams)

    
    if (post.imageUrl.startsWith("firestore-media://")) {
        ChannelRepository.loadImage(post.imageUrl) { bitmap, _ ->
            runOnUiThread {
                if (!isFinishing && !isDestroyed && bitmap != null) {
                    image.setImageBitmap(bitmap)
                    image.setOnClickListener {
    ZoomImageViewer.show(
        this@HomeCategoryActivity,
        bitmap
    )
                    }
                }
            }
        }
    } else if (
        post.imageUrl.startsWith("https://") ||
        post.imageUrl.startsWith("http://")
    ) {
        Thread {
            val bitmap = try {
                val connection = URL(post.imageUrl).openConnection()
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                connection.getInputStream().use {
                    BitmapFactory.decodeStream(it)
                }
            } catch (_: Exception) {
                null
            }

            runOnUiThread {
                if (!isFinishing && !isDestroyed && bitmap != null) {
                    image.setImageBitmap(bitmap)
                    image.setOnClickListener {
    ZoomImageViewer.show(
        this@HomeCategoryActivity,
        bitmap
    )
                    }
                }
            }
        }.start()
    }

}              

                val textColumn = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(
    if (isLargeImage) 0 else dp(if (isCompact) 6 else 10),
    if (isLargeImage) dp(8) else 0,
    0,
    0
)
                }

                val title = TextView(this).apply {
                    text = post.title.ifBlank { "बिना शीर्षक की पोस्ट" }
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.rgb(25, 80, 130))
                }

                textColumn.addView(title)

                val description = post.description.ifBlank {
                    post.body
                }

                if (description.isNotBlank()) {
                    textColumn.addView(
                        TextView(this).apply {
                            text = description
                            textSize = 14f
                            setTextColor(Color.DKGRAY)
                            setPadding(0, dp(6), 0, dp(6))
                        }
                    )
                }

                if (post.createdAt > 0L) {
                    textColumn.addView(
                        TextView(this).apply {
                            text = android.text.format.DateFormat.format(
                                "dd MMM yyyy, hh:mm a",
                                post.createdAt
                            ).toString()
                            textSize = 11f
                            setTextColor(Color.GRAY)
                        }
                    )
                }

                row.addView(
    textColumn,
    if (isLargeImage) {
        LinearLayout.LayoutParams(-1, -2)
    } else {
        LinearLayout.LayoutParams(0, -2, 1f)
    }
)

card.addView(row)

                card.setOnClickListener {
                    val url = post.websiteUrl.ifBlank {
                        post.fileUrl
                    }

                    if (url.isNotBlank()) {
                        openSubCategory(
                            url = url,
                            pageType = "Website",
                            name = post.title
                        )
                    } else {
                        Toast.makeText(
                            this,
                            post.title + "\n" + description,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                postsLayout.addView(
                    card,
                    LinearLayout.LayoutParams(
                        -1,
                        -2
                    ).apply {
                        bottomMargin = dp(12)
                    }
                )
            }
        },
        onError = { error ->
            postsLayout.removeAllViews()

            postsLayout.addView(
                TextView(this).apply {
                    text = "पोस्ट लोड नहीं हुई। इंटरनेट और Firestore नियम जाँचें।"
                    textSize = 14f
                    setTextColor(Color.RED)
                    setPadding(dp(8), dp(16), dp(8), dp(16))
                }
            )

            Toast.makeText(
                this,
                "पोस्ट लोड त्रुटि: ${error.message ?: "Firestore error"}",
                Toast.LENGTH_LONG
            ).show()
        }
    )
}

    /*
     * =========================================================
     * SUB CATEGORY BUTTON
     * =========================================================
     */
    
private var subCategoryRow: LinearLayout? = null
private var subCategoryRowCount = 0

private fun addSubCategoryButton(
    name: String,
    icon: String,
    url: String,
    pageType: String,
    index: Int
) {
    if (subCategoryRow == null || subCategoryRowCount >= 2) {
        subCategoryRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        subCategoryRowCount = 0

        subCategoryLayout.addView(
            subCategoryRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
        )
    }

    val button = TextView(this).apply {
        text = if (icon.isNotBlank()) "$icon\n$name" else name
        textSize = 15f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        typeface = Typeface.DEFAULT_BOLD
        setPadding(dp(6), dp(10), dp(6), dp(10))
        minHeight = dp(68)

        background = GradientDrawable().apply {
            setColor(
                this@HomeCategoryActivity.colors[
                    index % this@HomeCategoryActivity.colors.size
                ]
            )
            cornerRadius = dp(14).toFloat()
            setStroke(dp(1), Color.WHITE)
        }

        setOnClickListener {
            openSubCategory(url, pageType, name)
        }
    }

    subCategoryRow?.addView(
        button,
        LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        ).apply {
            marginStart = dp(4)
            marginEnd = dp(4)
        }
    )

    subCategoryRowCount++
}


    /*
     * =========================================================
     * OPEN SUB CATEGORY
     * =========================================================
     */
    private fun openSubCategory(
        url: String,
        pageType: String,
        name: String
    ) {

        if (url.isBlank()) {

            Toast.makeText(
                this,
                "$name का URL अभी उपलब्ध नहीं है।",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        try {

            startActivity(
                Intent(
                    this,
                    WebViewActivity::class.java
                ).putExtra(
                    "url",
                    url
                )
            )

        } catch (
            e: Exception
        ) {

            Toast.makeText(
                this,
                "Page open नहीं हो सका।",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /*
     * =========================================================
     * DP
     * =========================================================
     */
    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                resources.displayMetrics.density
        ).toInt()
    }

    override fun onDestroy() {
    sliderHandler.removeCallbacks(sliderRunnable)
    categoryPostsListener?.remove()
    super.onDestroy()
    }
}
