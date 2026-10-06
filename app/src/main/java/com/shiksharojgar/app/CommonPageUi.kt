package com.shiksharojgar.app

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.*
import com.google.firebase.firestore.FirebaseFirestore
object CommonPageUi {

    enum class Section {
        HOME,
        CHANNEL,
        NOTICE,
        IMP_INFO,
        MORE
    }

    private const val SKY_BLUE = "#DBF2FF"
    private const val ACTIVE_BLUE = "#075985"
    private const val TEXT_COLOR = "#1E293B"

    fun dp(
        context: Context,
        value: Int
    ): Int {

        return (
            value *
                context.resources.displayMetrics.density
            ).toInt()
    }

    /*
     * =========================================================
     * COMMON HEADER
     * =========================================================
     *
     * बाकी सभी pages:
     *
     * ← Back | 📱 Shiksha Rojgar App | 📤 Share ↗️
     *
     * नीचे:
     * शिक्षा • रोजगार • महत्वपूर्ण अपडेट
     *
     * Bell नहीं।
     * Logo circular रहेगा।
     * Extra white background/ring नहीं।
     * =========================================================
     */

    fun createHeader(
        context: Context,
        onBack: () -> Unit,
        onShare: () -> Unit
    ): LinearLayout {

        val header =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    dp(context, 7),
                    dp(context, 5),
                    dp(context, 6),
                    dp(context, 5)
                )

                setBackgroundColor(
                    Color.parseColor(SKY_BLUE)
                )
            }

        /*
         * BACK
         */
        val back =
            TextView(context).apply {

                text = "←"

                textSize = 27f

                typeface =
                    Typeface.DEFAULT_BOLD

                gravity =
                    Gravity.CENTER

                setTextColor(
                    Color.parseColor(
                        ACTIVE_BLUE
                    )
                )

                isClickable = true
                isFocusable = true

                setOnClickListener {
                    onBack()
                }
            }

        header.addView(
            back,
            LinearLayout.LayoutParams(
                dp(context, 42),
                dp(context, 48)
            )
        )

        /*
         * LOGO
         */
        val logo =
            ImageView(context).apply {

                setImageResource(
                    R.drawable.logo
                )

                contentDescription =
                    "Shiksha Rojgar"

                scaleType =
                    ImageView.ScaleType.CENTER_CROP

                /*
                 * Extra white background/ring नहीं।
                 */
                background =
                    null

                /*
                 * Logo circular
                 */
                clipToOutline = true

                outlineProvider =
                    object : ViewOutlineProvider() {

                        override fun getOutline(
                            view: View,
                            outline: Outline
                        ) {

                            outline.setOval(
                                0,
                                0,
                                view.width,
                                view.height
                            )
                        }
                    }
            }

        header.addView(
            logo,
            LinearLayout.LayoutParams(
                dp(context, 38),
                dp(context, 38)
            ).apply {

                rightMargin =
                    dp(context, 7)
            }
        )

        /*
         * APP NAME + TAGLINE
         */
        val titleBox =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        val appName =
            TextView(context).apply {

                text =
                    "📱 Shiksha Rojgar App"

                textSize = 16f

                typeface =
                    Typeface.DEFAULT_BOLD

                maxLines = 1

                setTextColor(
                    Color.parseColor(
                        ACTIVE_BLUE
                    )
                )
            }

        val tagline =
            TextView(context).apply {

                text =
                    "शिक्षा • रोजगार • महत्वपूर्ण अपडेट"

                textSize = 10.5f

                maxLines = 1

                setTextColor(
                    Color.parseColor(
                        ACTIVE_BLUE
                    )
                )
            }

        titleBox.addView(
            appName
        )

        titleBox.addView(
            tagline
        )

        header.addView(
            titleBox,
            LinearLayout.LayoutParams(
                0,
                dp(context, 48),
                1f
            )
        )

        /*
         * SHARE
         */
        val share =
            TextView(context).apply {

                text =
                    "📤 Share ↗️"

                textSize = 14f

                typeface =
                    Typeface.DEFAULT_BOLD

                gravity =
                    Gravity.CENTER

                setTextColor(
                    Color.parseColor(
                        ACTIVE_BLUE
                    )
                )

                isClickable = true
                isFocusable = true

                setPadding(
                    dp(context, 3),
                    0,
                    dp(context, 3),
                    0
                )

                setOnClickListener {
                    onShare()
                }
            }

        header.addView(
            share,
            LinearLayout.LayoutParams(
                dp(context, 91),
                dp(context, 48)
            )
        )

        return header
    }

    /*
     * =========================================================
     * COMMON BOTTOM BAR
     * =========================================================
     *
     * 🏠 Home
     * 📢 Channel
     * 🔔 Notice
     * 📌 Imp Info
     * ☰ More
     *
     * केवल Channel पर unread badge।
     * Channel page पर badge hide।
     * =========================================================
     */

    fun createBottomBar(
        context: Context,
        activeSection: Section
    ): LinearLayout {

        val bottom =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER

                setPadding(
                    dp(context, 2),
                    dp(context, 4),
                    dp(context, 2),
                    dp(context, 4)
                )

                setBackgroundColor(
                    Color.WHITE
                )

                elevation =
                    dp(context, 12).toFloat()
            }

        /*
         * HOME
         */
        val home =
            createBottomButton(
                context,
                "🏠\nHome",
                activeSection ==
                    Section.HOME
            ) {

                val intent =
                    Intent(
                        context,
                        MainActivity::class.java
                    ).apply {

                        flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }

                context.startActivity(
                    intent
                )
            }

        /*
         * CHANNEL + BADGE
         */
        val channelBox =
            FrameLayout(context)

        val channel =
            createBottomButton(
                context,
                "📢\nChannel",
                activeSection ==
                    Section.CHANNEL
            ) {

                context.startActivity(
                    Intent(
                        context,
                        ChannelActivity::class.java
                    )
                )
            }

        channelBox.addView(
            channel,
            FrameLayout.LayoutParams(
                -1,
                dp(context, 60)
            )
        )

        val badge =
            TextView(context).apply {

                textSize = 11f

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

        /*
         * Channel page पर badge नहीं।
         */
        if (
            activeSection ==
            Section.CHANNEL
        ) {

            badge.visibility =
                View.GONE

        } else {

            val unread =
                context
                    .getSharedPreferences(
                        "sr_notifications",
                        Context.MODE_PRIVATE
                    )
                    .getInt(
                        "channel_unread",
                        0
                    )

            if (unread > 0) {

                badge.text =
                    if (unread > 99) {
                        "99+"
                    } else {
                        unread.toString()
                    }

                badge.visibility =
                    View.VISIBLE
            }
        }

        channelBox.addView(
            badge,
            FrameLayout.LayoutParams(
                dp(context, 26),
                dp(context, 22)
            ).apply {

                gravity =
                    Gravity.TOP or
                        Gravity.END

                rightMargin =
                    dp(context, 4)
            }
        )

        /*
         * NOTICE
         */
        val notice =
            createBottomButton(
                context,
                "🔔\nNotice",
                activeSection ==
                    Section.NOTICE
            ) {

                context.startActivity(
                    Intent(
                        context,
                        NoticeActivity::class.java
                    )
                )
            }

        /*
         * IMPORTANT INFORMATION
         */
        val imp =
            createBottomButton(
                context,
                "📌\nImp Info",
                activeSection ==
                    Section.IMP_INFO
            ) {

                context.startActivity(
                    Intent(
                        context,
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
         * MORE
         */
        val more =
            createBottomButton(
                context,
                "☰\nMore",
                activeSection ==
                    Section.MORE
            ) {

                showMoreMenu(
                    context
                )
            }

        bottom.addView(
            home,
            weightParams(context)
        )

        bottom.addView(
            channelBox,
            weightParams(context)
        )

        bottom.addView(
            notice,
            weightParams(context)
        )

        bottom.addView(
            imp,
            weightParams(context)
        )

        bottom.addView(
            more,
            weightParams(context)
        )

        return bottom
    }

    private fun createBottomButton(
        context: Context,
        label: String,
        active: Boolean,
        action: () -> Unit
    ): TextView {

        return TextView(context).apply {

            text = label

            textSize = 12f

            typeface =
                Typeface.DEFAULT_BOLD

            gravity =
                Gravity.CENTER

            setTextColor(
                if (active) {
                    Color.parseColor(
                        ACTIVE_BLUE
                    )
                } else {
                    Color.parseColor(
                        TEXT_COLOR
                    )
                }
            )

            background =
                Color.TRANSPARENT.toDrawableCompat()

            isClickable = true
            isFocusable = true

            setOnClickListener {
                action()
            }
        }
    }

    private fun weightParams(
        context: Context
    ): LinearLayout.LayoutParams {

        return LinearLayout.LayoutParams(
            0,
            dp(context, 60),
            1f
        )
    }

    /*
     * =========================================================
     * ADMIN CONTROLLED MORE MENU
     * =========================================================
     *
     * Firestore:
     *
     * more_menu_items
     *
     * Fields:
     * label
     * icon
     * enabled
     * position
     * actionType
     * url
     * phone
     * whatsapp
     * email
     * content
     * color
     * =========================================================
     */

    private fun showMoreMenu(
        context: Context
    ) {

        val loading =
            ProgressBar(context)

        val dialog =
            AlertDialogCompat(
                context,
                "☰ More",
                loading
            )

        dialog.show()

        FirebaseFirestore
            .getInstance()
            .collection(
                "more_menu_items"
            )
            .whereEqualTo(
                "enabled",
                true
            )
            .get()
            .addOnSuccessListener { snap ->

                dialog.dismiss()

                val items =
                    snap.documents
                        .sortedBy {

                            it.getLong(
                                "position"
                            ) ?: 0L
                        }

                if (items.isEmpty()) {

                    Toast.makeText(
                        context,
                        "More Menu अभी Admin द्वारा सेट नहीं किया गया है।",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addOnSuccessListener
                }

                val labels =
                    items.map { doc ->

                        val icon =
                            doc.getString(
                                "icon"
                            ) ?: ""

                        val label =
                            doc.getString(
                                "label"
                            ) ?: "Menu"

                        if (
                            icon.isNotBlank()
                        ) {
                            "$icon  $label"
                        } else {
                            label
                        }
                    }.toTypedArray()

                AlertDialog.Builder(context)
                    .setTitle(
                        "☰ More"
                    )
                    .setItems(
                        labels
                    ) { _, which ->

                        openMoreItem(
                            context,
                            items[which]
                        )
                    }
                    .show()
            }
            .addOnFailureListener { e ->

                dialog.dismiss()

                Toast.makeText(
                    context,
                    "More Menu load नहीं हो सका।",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun openMoreItem(
        context: Context,
        doc: com.google.firebase.firestore.DocumentSnapshot
    ) {

        val actionType =
            doc.getString(
                "actionType"
            ) ?: "url"

        val url =
            doc.getString(
                "url"
            ) ?: ""

        val phone =
            doc.getString(
                "phone"
            ) ?: ""

        val whatsapp =
            doc.getString(
                "whatsapp"
            ) ?: ""

        val email =
            doc.getString(
                "email"
            ) ?: ""

        val content =
            doc.getString(
                "content"
            ) ?: ""

        val label =
            doc.getString(
                "label"
            ) ?: "More"

        try {

            when (
                actionType.lowercase()
            ) {

                "internal" -> {

                    when (
                        doc.getString(
                            "target"
                        )
                            ?.lowercase()
                    ) {

                        "home" -> {
                            context.startActivity(
                                Intent(
                                    context,
                                    MainActivity::class.java
                                )
                            )
                        }

                        "channel" -> {
                            context.startActivity(
                                Intent(
                                    context,
                                    ChannelActivity::class.java
                                )
                            )
                        }

                        "notice" -> {
                            context.startActivity(
                                Intent(
                                    context,
                                    NoticeActivity::class.java
                                )
                            )
                        }

                        "important_information",
                        "imp_info" -> {

                            context.startActivity(
                                Intent(
                                    context,
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
                    }
                }

                "phone" -> {

                    if (phone.isNotBlank()) {

                        context.startActivity(
                            Intent(
                                Intent.ACTION_DIAL,
                                Uri.parse(
                                    "tel:$phone"
                                )
                            )
                        )
                    }
                }

                "whatsapp" -> {

                    val number =
                        whatsapp
                            .replace(
                                "+",
                                ""
                            )
                            .replace(
                                " ",
                                ""
                            )

                    if (number.isNotBlank()) {

                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                    "https://wa.me/$number"
                                )
                            )
                        )
                    }
                }

                "email" -> {

                    if (email.isNotBlank()) {

                        context.startActivity(
                            Intent(
                                Intent.ACTION_SENDTO,
                                Uri.parse(
                                    "mailto:$email"
                                )
                            )
                        )
                    }
                }

                "text",
                "about",
                "privacy",
                "disclaimer",
                "help" -> {

                    AlertDialog.Builder(context)
                        .setTitle(label)
                        .setMessage(content)
                        .setPositiveButton(
                            "OK",
                            null
                        )
                        .show()
                }

                "share" -> {

                    context.startActivity(
                        Intent.createChooser(
                            Intent(
                                Intent.ACTION_SEND
                            ).apply {

                                type =
                                    "text/plain"

                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    if (
                                        content.isNotBlank()
                                    ) {
                                        content
                                    } else {
                                        url
                                    }
                                )
                            },
                            "Share"
                        )
                    )
                }

                "external" -> {

                    if (url.isNotBlank()) {

                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(url)
                            )
                        )
                    }
                }

                else -> {

                    if (url.isNotBlank()) {

                        context.startActivity(
                            Intent(
                                context,
                                WebViewActivity::class.java
                            ).putExtra(
                                "url",
                                url
                            )
                        )
                    }
                }
            }

        } catch (_: Exception) {

            Toast.makeText(
                context,
                "$label open नहीं हो सका।",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /*
     * =========================================================
     * SIMPLE LOADING DIALOG
     * =========================================================
     */

    private class AlertDialogCompat(
        context: Context,
        title: String,
        view: View
    ) {

        private val dialog =
            android.app.AlertDialog.Builder(
                context
            )
                .setTitle(title)
                .setView(view)
                .create()

        fun show() {
            dialog.show()
        }

        fun dismiss() {
            dialog.dismiss()
        }
    }

    /*
     * =========================================================
     * TRANSPARENT BACKGROUND HELPER
     * =========================================================
     */

    
        private fun Int.toDrawableCompat():
    android.graphics.drawable.Drawable {

    return ColorDrawable(this)
    }
}
