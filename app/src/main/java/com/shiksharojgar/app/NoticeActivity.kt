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
 * BOTTOM NAVIGATION
 */
val bottomBar =
    LinearLayout(this).apply {

        orientation =
            LinearLayout.HORIZONTAL

        gravity =
            Gravity.CENTER

        setPadding(
            dp(6),
            dp(6),
            dp(6),
            dp(6)
        )

        setBackgroundColor(
            Color.WHITE
        )

        elevation = 10f
    }

val homeButton =
    TextView(this).apply {

        text = "⌂\nHome"

        textSize = 12f

        gravity =
            Gravity.CENTER

        setTextColor(
            Color.rgb(
                7,
                89,
                133
            )
        )

        setOnClickListener {

            val intent =
                Intent(
                    this@NoticeActivity,
                    MainActivity::class.java
                ).apply {

                    flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                }

            startActivity(intent)

            finish()
        }
    }

val shareButton =
    TextView(this).apply {

        text = "↗\nShare"

        textSize = 12f

        gravity =
            Gravity.CENTER

        setTextColor(
            Color.rgb(
                7,
                89,
                133
            )
        )

        setOnClickListener {

            val shareText =
                "📢 Notice / Update\n\nShiksha Rojgar App"

            startActivity(
                Intent.createChooser(
                    Intent(
                        Intent.ACTION_SEND
                    ).apply {

                        type =
                            "text/plain"

                        putExtra(
                            Intent.EXTRA_TEXT,
                            shareText
                        )
                    },
                    "Share"
                )
            )
        }
    }

val backButton =
    TextView(this).apply {

        text = "‹\nBack"

        textSize = 12f

        gravity =
            Gravity.CENTER

        setTextColor(
            Color.rgb(
                7,
                89,
                133
            )
        )

        setOnClickListener {
            finish()
        }
    }

bottomBar.addView(
    homeButton,
    LinearLayout.LayoutParams(
        0,
        dp(58),
        1f
    )
)

bottomBar.addView(
    shareButton,
    LinearLayout.LayoutParams(
        0,
        dp(58),
        1f
    )
)

bottomBar.addView(
    backButton,
    LinearLayout.LayoutParams(
        0,
        dp(58),
        1f
    )
)

root.addView(
    bottomBar,
    LinearLayout.LayoutParams(
        -1,
        dp(70)
    )
)

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

                return@addOnSuccessListener
            }

            notices.forEach { doc ->

                addNoticeCard(
                    doc
                )
            }
            getSharedPreferences(
    "sr_notifications",
    MODE_PRIVATE
)
    .edit()
    .putLong(
        "notice_last_seen_at",
        System.currentTimeMillis()
    )
    .apply()
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
