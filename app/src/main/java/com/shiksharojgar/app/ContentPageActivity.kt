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
    private lateinit var unreadBadge: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pageId =
            intent.getStringExtra("pageId") ?: "important_information"

        val fallback =
            intent.getStringExtra("pageTitle")
                ?: "📌 Important Information"

        setContentView(buildUi(fallback))

        load(pageId, fallback)

        updateUnreadBadge()
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
    private fun buildUi(pageTitle: String): LinearLayout {

        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(
                    Color.rgb(248, 250, 252)
                )
            }

        /*
         * =====================================================
         * COMMON HEADER
         * =====================================================
         */
        val header =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    10,
                    6,
                    10,
                    6
                )

                setBackgroundColor(
                    Color.rgb(7, 89, 133)
                )
            }

        /*
         * APP LOGO
         */
        val logo =
    ImageView(this).apply {

        setImageResource(
            R.drawable.logo
        )

        contentDescription =
            "Shiksha Rojgar"

        scaleType =
            ImageView.ScaleType.CENTER_CROP

        adjustViewBounds = true
    }

        header.addView(
            logo,
            LinearLayout.LayoutParams(
                44,
                44
            ).apply {
                rightMargin = 8
            }
        )

        /*
         * APP NAME + TAGLINE
         */
        val headerText =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        val appName =
            TextView(this).apply {

                text =
                    "📱 Shiksha Rojgar App"

                textSize = 17f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.WHITE
                )
            }

        val tagline =
            TextView(this).apply {

                text =
                    "शिक्षा • रोजगार • महत्वपूर्ण अपडेट"

                textSize = 11f

                setTextColor(
                    Color.WHITE
                )
            }

        headerText.addView(appName)

        headerText.addView(tagline)

        header.addView(
            headerText,
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )

        /*
         * बाकी pages पर Bell नहीं होगा।
         * Bell केवल Home Header में रहेगा।
         */

        root.addView(
            header,
            LinearLayout.LayoutParams(
                -1,
                60
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

        val backButton =
            TextView(this).apply {

                text = "←"

                textSize = 25f

                gravity =
                    Gravity.CENTER

                setTextColor(
                    Color.rgb(7, 89, 133)
                )

                setOnClickListener {
                    finish()
                }
            }

        titleStrip.addView(
            backButton,
            LinearLayout.LayoutParams(
                44,
                48
            )
        )

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
         * COMMON BOTTOM BAR
         * =====================================================
         */
        val bottomBar =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER

                setPadding(
                    2,
                    4,
                    2,
                    4
                )

                setBackgroundColor(
                    Color.WHITE
                )

                elevation = 10f
            }

        /*
         * HOME
         */
        val homeButton =
            bottomButton(
                "🏠\nHome"
            ) {
                goHome()
            }

        /*
         * CHANNEL
         */
        val channelBox =
            FrameLayout(this)

        val channelButton =
            bottomButton(
                "📢\nChannel"
            ) {
                startActivity(
                    Intent(
                        this,
                        ChannelActivity::class.java
                    )
                )
            }

        channelBox.addView(
            channelButton,
            FrameLayout.LayoutParams(
                -1,
                68
            )
        )

        unreadBadge =
            TextView(this).apply {

                text = ""

                textSize = 10f

                typeface =
                    Typeface.DEFAULT_BOLD

                gravity =
                    Gravity.CENTER

                setTextColor(
                    Color.WHITE
                )

                setBackgroundColor(
                    Color.RED
                )

                visibility =
                    View.GONE
            }

        val badgeParams =
            FrameLayout.LayoutParams(
                26,
                22
            ).apply {

                gravity =
                    Gravity.TOP or Gravity.END

                topMargin = 0
                rightMargin = 4
            }

        channelBox.addView(
            unreadBadge,
            badgeParams
        )

        /*
         * NOTICE
         */
        val noticeButton =
            bottomButton(
                "🔔\nNotice"
            ) {
                startActivity(
                    Intent(
                        this,
                        NoticeActivity::class.java
                    )
                )
            }

        /*
         * IMPORTANT INFORMATION
         */
        val impButton =
            bottomButton(
                "📌\nImp Info"
            ) {
                openImportantInformation()
            }

        /*
         * MORE
         */
        val moreButton =
            bottomButton(
                "☰\nMore"
            ) {
                openMore()
            }

        bottomBar.addView(
            homeButton,
            LinearLayout.LayoutParams(
                0,
                68,
                1f
            )
        )

        bottomBar.addView(
            channelBox,
            LinearLayout.LayoutParams(
                0,
                68,
                1f
            )
        )

        bottomBar.addView(
            noticeButton,
            LinearLayout.LayoutParams(
                0,
                68,
                1f
            )
        )

        bottomBar.addView(
            impButton,
            LinearLayout.LayoutParams(
                0,
                68,
                1f
            )
        )

        bottomBar.addView(
            moreButton,
            LinearLayout.LayoutParams(
                0,
                68,
                1f
            )
        )

        root.addView(
            bottomBar,
            LinearLayout.LayoutParams(
                -1,
                72
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
     * CHANNEL UNREAD BADGE
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
