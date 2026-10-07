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
            intent.getStringExtra("pageId") ?: "important_information"

        val fallback =
    intent.getStringExtra("pageTitle")
        ?: "📌 Important Information"

val activeSection =
    intent.getStringExtra("activeSection")
        ?: "IMP_INFO"
load(pageId, fallback)
    }
setContentView(
    buildUi(
        fallback,
        activeSection
    )
)

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

        "MORE" ->
            CommonPageUi.Section.MORE

        else ->
            CommonPageUi.Section.IMP_INFO
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
}
