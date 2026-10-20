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

class HomeCategoryActivity : AppCompatActivity() {

    private val db =
        FirebaseFirestore.getInstance()

    private lateinit var listLayout: LinearLayout
private lateinit var subCategoryLayout: LinearLayout
private lateinit var postsLayout: LinearLayout
private lateinit var unreadBadge: TextView

private var categoryPostsListener:
    com.google.firebase.firestore.ListenerRegistration? = null

private val sliderHandler =
    android.os.Handler(android.os.Looper.getMainLooper())

private var sliderIndex = 0

private val sliderImages = intArrayOf(
    R.drawable.home_teacher,
    R.drawable.home_student,
    R.drawable.home_exam
)

private val sliderRunnable = object : Runnable {
    override fun run() {
        if (isFinishing || isDestroyed) return

        sliderIndex = (sliderIndex + 1) % sliderImages.size

        val image = findViewById<android.widget.ImageView>(
            R.id.categoryBannerImage
        )

        if (image != null) {
            image.setImageResource(sliderImages[sliderIndex])
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

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        val categoryId =
            intent.getStringExtra("categoryId")
                ?: ""

        val categoryName =
            intent.getStringExtra("categoryName")
                ?: "Category"

        val categoryIcon =
            intent.getStringExtra("categoryIcon")
                ?: ""

        setContentView(
            buildUi(
                categoryName,
                categoryIcon
            )
        )

        loadSubCategories(
            categoryId
        )

        
    }

    /*
     * =========================================================
     * COMMON PAGE UI
     * =========================================================
     */
    private fun buildUi(
        categoryName: String,
        categoryIcon: String
    ): LinearLayout {

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setBackgroundColor(
                    Color.rgb(248, 250, 252)
                )
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

        val title =
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

        titleStrip.addView(
            title,
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

        // 1. ऊपर HD स्लाइडर के लिए जगह
val banner = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    setPadding(dp(4), dp(4), dp(4), dp(8))
}

val previous = TextView(this).apply {
    text = "‹"
    textSize = 36f
    gravity = Gravity.CENTER
    setTextColor(Color.rgb(25, 118, 210))
    setOnClickListener {
        sliderIndex =
            (sliderIndex - 1 + sliderImages.size) % sliderImages.size
        findViewById<android.widget.ImageView>(
            R.id.categoryBannerImage
        )?.setImageResource(sliderImages[sliderIndex])
    }
}

val bannerImage = android.widget.ImageView(this).apply {
    id = R.id.categoryBannerImage
    setImageResource(sliderImages[0])
    scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
    contentDescription = "शिक्षा रोजगार स्लाइडर"
    adjustViewBounds = true
}

val next = TextView(this).apply {
    text = "›"
    textSize = 36f
    gravity = Gravity.CENTER
    setTextColor(Color.rgb(25, 118, 210))
    setOnClickListener {
        sliderIndex = (sliderIndex + 1) % sliderImages.size
        findViewById<android.widget.ImageView>(
            R.id.categoryBannerImage
        )?.setImageResource(sliderImages[sliderIndex])
    }
}

banner.addView(previous, LinearLayout.LayoutParams(dp(36), dp(140)))

banner.addView(
    bannerImage,
    LinearLayout.LayoutParams(0, dp(140), 1f)
)

banner.addView(next, LinearLayout.LayoutParams(dp(36), dp(140)))

listLayout.addView(banner)

sliderHandler.removeCallbacks(sliderRunnable)
sliderHandler.postDelayed(sliderRunnable, 4500L)

// 2. सब-कैटेगरी का अलग हिस्सा
val subHeading = TextView(this).apply {
    text = "📚 विषय और सब-कैटेगरी"
    textSize = 18f
    typeface = Typeface.DEFAULT_BOLD
    setTextColor(Color.rgb(25, 118, 210))
    setPadding(0, dp(10), 0, dp(8))
}

listLayout.addView(subHeading)

subCategoryLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
}

listLayout.addView(subCategoryLayout)

// 3. नई पोस्ट का अलग हिस्सा
val postsHeading = TextView(this).apply {
    text = "📰 ताज़ा पोस्ट"
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

        listLayout.removeAllViews()

        val progress =
            ProgressBar(this)

        listLayout.addView(
            progress,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            )
        )

        db.collection(
            "home_subcategories"
        )
            .whereEqualTo(
                "parentId",
                categoryId
            )
            .whereEqualTo(
                "enabled",
                true
            )
            .get()
            .addOnSuccessListener { result ->

                listLayout.removeAllViews()

                val items =
                    result.documents.sortedBy {

                        it.getLong(
                            "position"
                        ) ?: 0L
                    }

                if (items.isEmpty()) {

                    val empty =
                        TextView(this).apply {

                            text =
                                "अभी कोई Sub-Category उपलब्ध नहीं है।"

                            textSize = 16f

                            gravity =
                                Gravity.CENTER

                            setPadding(
                                dp(16),
                                dp(40),
                                dp(16),
                                dp(40)
                            )
                        }

                    listLayout.addView(
                        empty
                    )

                    return@addOnSuccessListener
                }

                items.forEachIndexed {
                    index,
                    doc ->

                    val name =
                        doc.getString(
                            "name"
                        ) ?: "Sub-Category"

                    val icon =
                        doc.getString(
                            "icon"
                        ) ?: ""

                    val url =
                        doc.getString(
                            "url"
                        ) ?: ""

                    val pageType =
                        doc.getString(
                            "pageType"
                        ) ?: "Website"

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

                listLayout.removeAllViews()

                Toast.makeText(
                    this,
                    "Sub-Category load failed: ${
                        e.message ?: "Firestore error"
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    /*
     * =========================================================
     * SUB CATEGORY BUTTON
     * =========================================================
     */
    private fun addSubCategoryButton(
        name: String,
        icon: String,
        url: String,
        pageType: String,
        index: Int
    ) {

        val button =
            TextView(this).apply {

                text =
                    if (icon.isNotBlank()) {
                        "$icon  $name"
                    } else {
                        name
                    }

                textSize = 17f

                setTextColor(
                    Color.WHITE
                )

                gravity =
                    Gravity.CENTER_VERTICAL

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                setPadding(
                    dp(18),
                    0,
                    dp(18),
                    0
                )

                background =
                    GradientDrawable().apply {

                        setColor(
    this@HomeCategoryActivity.colors[
        index % this@HomeCategoryActivity.colors.size
    ]
)

                        cornerRadius =
                            dp(12).toFloat()
                    }

                setOnClickListener {

                    openSubCategory(
                        url,
                        pageType,
                        name
                    )
                }
            }

        listLayout.addView(
            button,
            LinearLayout.LayoutParams(
                -1,
                dp(58)
            ).apply {

                bottomMargin =
                    dp(12)
            }
        )
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
}
