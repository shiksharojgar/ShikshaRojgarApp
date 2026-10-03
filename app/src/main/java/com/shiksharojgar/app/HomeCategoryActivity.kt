package com.shiksharojgar.app

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class HomeCategoryActivity : AppCompatActivity() {

    private val db =
        FirebaseFirestore.getInstance()

    private lateinit var listLayout: LinearLayout

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

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setBackgroundColor(
                    Color.WHITE
                )
            }

        val header =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    dp(16),
                    dp(12),
                    dp(16),
                    dp(12)
                )

                setBackgroundColor(
                    Color.rgb(25, 118, 210)
                )
            }

        val back =
            TextView(this).apply {

                text = "‹"

                textSize = 34f

                setTextColor(
                    Color.WHITE
                )

                gravity =
                    Gravity.CENTER

                setOnClickListener {
                    finish()
                }
            }

        header.addView(
            back,
            LinearLayout.LayoutParams(
                dp(45),
                dp(50)
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

                textSize = 20f

                setTextColor(
                    Color.WHITE
                )

                setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
                )

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        header.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                dp(50),
                1f
            )
        )

        root.addView(
            header
        )

        val scroll =
            android.widget.ScrollView(this).apply {

                fillViewport = true
            }

        listLayout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(16),
                    dp(16),
                    dp(16),
                    dp(24)
                )
            }

        scroll.addView(
            listLayout
        )

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        setContentView(root)

        loadSubCategories(
            categoryId
        )
    }

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
                    result.documents
                        .sortedBy {
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
                    android.graphics.Typeface.BOLD
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
                            colors[
                                index %
                                    colors.size
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

        when (
            pageType.lowercase()
        ) {

            "externallink",
            "external link" -> {

                try {

                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            android.net.Uri.parse(url)
                        )
                    )

                } catch (
                    e: Exception
                ) {

                    Toast.makeText(
                        this,
                        "Link open नहीं हो सका।",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            else -> {

                try {

                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            android.net.Uri.parse(url)
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
        }
    }

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                resources.displayMetrics.density
        ).toInt()
    }
}
