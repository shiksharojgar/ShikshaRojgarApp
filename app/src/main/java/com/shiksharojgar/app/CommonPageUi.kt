package com.shiksharojgar.app

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
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

    /*
 * =========================================================
 * FINAL MASTER CHANNEL-STYLE HEADER
 * =========================================================
 *
 * सभी सामान्य pages पर:
 *
 * [←] [Logo] [📱 Shiksha Rojgar App] [↗️] [⋮]
 *                              Share
 *
 * नीचे:
 * शिक्षा • रोजगार • महत्वपूर्ण अपडेट
 *
 * Channel जैसा navy header.
 * Extra white circle/ring नहीं।
 * =========================================================
 */

fun createHeader(
    context: Context,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onMenu: () -> Unit
): LinearLayout {

    val header =
        LinearLayout(context).apply {

            orientation =
                LinearLayout.HORIZONTAL

            gravity =
                Gravity.CENTER_VERTICAL

            setPadding(
                dp(context, 7),
                dp(context, 4),
                dp(context, 4),
                dp(context, 4)
            )

            setBackgroundColor(
                Color.parseColor("#0B2A4A")
            )
        }

    /*
     * =====================================================
     * BACK
     * =====================================================
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
                Color.WHITE
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
            dp(context, 38),
            dp(context, 52)
        )
    )

    /*
     * =====================================================
     * LOGO
     * =====================================================
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
             * कोई extra white background/ring नहीं।
             */
            background = null

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
                dp(context, 6)
        }
    )

    /*
     * =====================================================
     * APP NAME + TAGLINE
     * =====================================================
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

            textSize = 14f

            typeface =
                Typeface.DEFAULT_BOLD

            maxLines = 1

            setTextColor(
                Color.WHITE
            )
        }

    val tagline =
        TextView(context).apply {

            text =
                "शिक्षा • रोजगार • महत्वपूर्ण अपडेट"

            textSize = 8.5f

            maxLines = 1

            setTextColor(
                Color.WHITE
            )
        }

    titleBox.addView(
        appName,
        LinearLayout.LayoutParams(
            -1,
            dp(context, 22)
        )
    )

    titleBox.addView(
        tagline,
        LinearLayout.LayoutParams(
            -1,
            dp(context, 18)
        )
    )

    header.addView(
        titleBox,
        LinearLayout.LayoutParams(
            0,
            dp(context, 52),
            1f
        )
    )

    /*
     * =====================================================
     * SHARE
     *
     * ऊपर केवल ↗️
     * नीचे Share
     * =====================================================
     */

    val shareBox =
        LinearLayout(context).apply {

            orientation =
                LinearLayout.VERTICAL

            gravity =
                Gravity.CENTER

            isClickable = true
            isFocusable = true

            setOnClickListener {
                onShare()
            }
        }

    val shareIcon =
        TextView(context).apply {

            text =
                "↗️"

            textSize = 19f

            gravity =
                Gravity.CENTER

            setTextColor(
                Color.WHITE
            )

            typeface =
                Typeface.DEFAULT_BOLD
        }

    val shareText =
        TextView(context).apply {

            text =
                "Share"

            textSize = 9f

            gravity =
                Gravity.CENTER

            setTextColor(
                Color.WHITE
            )

            typeface =
                Typeface.DEFAULT_BOLD
        }

    shareBox.addView(
        shareIcon,
        LinearLayout.LayoutParams(
            -1,
            dp(context, 25)
        )
    )

    shareBox.addView(
        shareText,
        LinearLayout.LayoutParams(
            -1,
            dp(context, 17)
        )
    )

    header.addView(
        shareBox,
        LinearLayout.LayoutParams(
            dp(context, 52),
            dp(context, 52)
        )
    )

    /*
     * =====================================================
     * THREE DOT MENU
     * =====================================================
     */

    val menu =
        TextView(context).apply {

            text =
                "⋮"

            textSize = 28f

            gravity =
                Gravity.CENTER

            typeface =
                Typeface.DEFAULT_BOLD

            setTextColor(
                Color.WHITE
            )

            isClickable = true
            isFocusable = true

            setOnClickListener {
                onMenu()
            }
        }

    header.addView(
        menu,
        LinearLayout.LayoutParams(
            dp(context, 38),
            dp(context, 52)
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

        textSize = 10f

        typeface =
            Typeface.DEFAULT_BOLD

        gravity =
            Gravity.CENTER

        setTextColor(
            Color.WHITE
        )

        /*
         * 🔴 गोल Channel unread badge
         */
        background =
            GradientDrawable().apply {

                shape =
                    GradientDrawable.OVAL

                setColor(
                    Color.RED
                )
            }

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
        dp(context, 24),
        dp(context, 24)
    ).apply {

        gravity =
            Gravity.TOP or
                Gravity.END

        rightMargin =
            dp(context, 3)

        topMargin =
            dp(context, 1)
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

    val normalStart =
        when {
            label.contains("Home") ->
                "#DBEAFE"

            label.contains("Channel") ->
                "#DCFCE7"

            label.contains("Notice") ->
                "#FEF3C7"

            label.contains("Imp Info") ->
                "#FCE7F3"

            else ->
                "#E0E7FF"
        }

    val normalEnd =
        when {
            label.contains("Home") ->
                "#93C5FD"

            label.contains("Channel") ->
                "#86EFAC"

            label.contains("Notice") ->
                "#FCD34D"

            label.contains("Imp Info") ->
                "#F9A8D4"

            else ->
                "#C7D2FE"
        }

    return TextView(context).apply {

        text = label

        gravity =
            Gravity.CENTER

        textSize = 10.5f

        typeface =
            Typeface.DEFAULT_BOLD

        /*
         * ACTIVE BUTTON
         *
         * जिस page पर हैं,
         * वही button हल्का Blue रहेगा।
         */
        setTextColor(
            if (active) {
                Color.parseColor("#075985")
            } else {
                Color.rgb(
                    30,
                    41,
                    59
                )
            }
        )

        background =
            GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                if (active) {

                    intArrayOf(
                        Color.parseColor("#BFDBFE"),
                        Color.parseColor("#60A5FA")
                    )

                } else {

                    intArrayOf(
                        Color.parseColor(normalStart),
                        Color.parseColor(normalEnd)
                    )
                }

            ).apply {

                cornerRadius =
                    dp(
                        context,
                        18
                    ).toFloat()

                /*
                 * Active button पर
                 * थोड़ा साफ Blue border.
                 */
                setStroke(
                    dp(context, 1),
                    if (active) {
                        Color.parseColor("#2563EB")
                    } else {
                        Color.parseColor("#D9E3F0")
                    }
                )
            }

        elevation =
            if (active) {
                dp(context, 3).toFloat()
            } else {
                dp(context, 1).toFloat()
            }

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
