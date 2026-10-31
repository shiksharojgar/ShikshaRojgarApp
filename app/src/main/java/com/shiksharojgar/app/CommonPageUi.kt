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
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast
import android.app.Activity
import androidx.core.view.WindowCompat
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

object CommonPageUi {
    fun applyBlueStatusBar(activity: Activity) {
    val window = activity.window
    val blue = Color.rgb(29, 78, 216)

    window.statusBarColor =
        if (Build.VERSION.SDK_INT >= 35) {
            Color.TRANSPARENT
        } else {
            blue
        }

    window.navigationBarColor = blue

    if (Build.VERSION.SDK_INT >= 29) {
        window.isNavigationBarContrastEnforced = false
    }

    val insetsController =
        WindowCompat.getInsetsController(
            window,
            window.decorView
        )

    insetsController.isAppearanceLightStatusBars = false
    insetsController.isAppearanceLightNavigationBars = false

    val insetsController =
        WindowCompat.getInsetsController(
            window,
            window.decorView
        )

    insetsController.isAppearanceLightStatusBars = false
    insetsController.isAppearanceLightNavigationBars = false

    if (Build.VERSION.SDK_INT >= 35) {
        val decor = window.decorView as? ViewGroup
            ?: return

        val tag = "sr_blue_status_bar_overlay"

        var strip = decor.findViewWithTag<View>(tag)

        if (strip == null) {
            strip = View(activity).apply {
                this.tag = tag
                setBackgroundColor(blue)
                isClickable = false
                importantForAccessibility =
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }

            decor.addView(
                strip,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    Gravity.TOP
                )
            )
        }

        val statusStrip = strip

        ViewCompat.setOnApplyWindowInsetsListener(
            decor
        ) { _, insets ->
            val height = insets.getInsets(
                WindowInsetsCompat.Type.statusBars()
            ).top

            val params =
                statusStrip.layoutParams as FrameLayout.LayoutParams

            if (params.height != height) {
                params.height = height
                statusStrip.layoutParams = params
            }

            insets
        }

        ViewCompat.requestApplyInsets(decor)
    }
}
    
    fun coloredHeaderOptionsAdapter(
        context: Context,
        options: Array<String>
    ): ArrayAdapter<String> {
        val colors = intArrayOf(
            Color.rgb(25, 118, 210),   // नीला
            Color.rgb(0, 137, 123),    // हरा-नीला
            Color.rgb(239, 108, 0),    // नारंगी
            Color.rgb(106, 27, 154),   // बैंगनी
            Color.rgb(46, 125, 50),    // हरा
            Color.rgb(198, 40, 40),    // लाल
            Color.rgb(57, 73, 171),    // इंडिगो
            Color.rgb(173, 20, 87)     // गुलाबी
        )

        return object : ArrayAdapter<String>(
            context,
            android.R.layout.simple_list_item_1,
            options
        ) {
            override fun getView(
                position: Int,
                convertView: View?,
                parent: android.view.ViewGroup
            ): View {
                val itemView = super.getView(
                    position,
                    convertView,
                    parent
                ) as TextView

                itemView.setTextColor(Color.WHITE)
                itemView.setTypeface(null, Typeface.BOLD)
                itemView.setBackgroundColor(
                    colors[position % colors.size]
                )
                itemView.setPadding(
                    dp(context, 16),
                    dp(context, 12),
                    dp(context, 16),
                    dp(context, 12)
                )

                return itemView
            }
        }
    }
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

fun showAnchoredOptions(
    context: Context,
    anchor: View,
    options: Array<String>,
    onSelect: (Int) -> Unit
) {
    if (options.isEmpty()) return

    val list = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(
            dp(context, 4),
            dp(context, 4),
            dp(context, 4),
            dp(context, 4)
        )
        background = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dp(context, 12).toFloat()
            setStroke(dp(context, 1), Color.parseColor("#CBD5E1"))
        }
    }

    var popup: PopupWindow? = null

    options.forEachIndexed { index, option ->
        val row = TextView(context).apply {
            text = option
            textSize = 14f
            setTextColor(Color.WHITE)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                dp(context, 14),
                dp(context, 12),
                dp(context, 14),
                dp(context, 12)
            )

            val colors = intArrayOf(
                Color.parseColor("#1DA1F2"),
                Color.parseColor("#00897B"),
                Color.parseColor("#EF6C00"),
                Color.parseColor("#6A1B9A"),
                Color.parseColor("#2E7D32"),
                Color.parseColor("#C62828")
            )

            setBackgroundColor(colors[index % colors.size])
            isClickable = true
            isFocusable = true
        }

        list.addView(
            row,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(context, 3)
            }
        )

        row.setOnClickListener {
            popup?.dismiss()
            onSelect(index)
        }
    }

    
    
    val width = minOf(
        dp(context, 280),
        (context.resources.displayMetrics.widthPixels * 0.82f).toInt()
    )

    // ड्रॉपडाउन की वास्तविक सामग्री के अनुसार ऊँचाई मापें।
    list.measure(
        View.MeasureSpec.makeMeasureSpec(
            width,
            View.MeasureSpec.EXACTLY
        ),
        View.MeasureSpec.makeMeasureSpec(
            0,
            View.MeasureSpec.UNSPECIFIED
        )
    )

    val maxPopupHeight = minOf(
        dp(context, 420),
        (context.resources.displayMetrics.heightPixels * 0.60f).toInt()
    )

    val popupHeight = minOf(
        list.measuredHeight,
        maxPopupHeight
    ).coerceAtLeast(dp(context, 1))

    popup = PopupWindow(
        list,
        width,
        popupHeight,
        true
    ).apply {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        elevation = dp(context, 12).toFloat()
        isOutsideTouchable = true
    }

    popup.showAsDropDown(
        anchor,
        0,
        -dp(context, 3),
        Gravity.END
    )


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
    onMenu: (View) -> Unit
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
                 Color.parseColor("#1D4ED8")
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
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }


    
    
     val appName =
         TextView(context).apply {
             text = "📱 Shiksha Rojgar App"
            textSize = 15f
             typeface = Typeface.DEFAULT_BOLD
             maxLines = 1
             gravity = Gravity.CENTER
             setTextColor(Color.WHITE)
        }

     val tagline =
    TextView(context).apply {
             text = "शिक्षा से सफलता तक, हर कदम आपके साथ!"
            textSize = 10f
             maxLines = 2
             gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
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
    val websiteBox =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true

            setOnClickListener {
    FirebaseFirestore.getInstance()
        .collection("header_website_links")
        .get()
        .addOnSuccessListener { snapshot ->
            val links = snapshot.documents
                .filter { it.getBoolean("enabled") ?: true }
                .sortedBy { it.getLong("position") ?: Long.MAX_VALUE }

            if (links.isEmpty()) {
                Toast.makeText(
                    context,
                    "अभी कोई Website लिंक उपलब्ध नहीं है।",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val options = links.map { doc ->
                    "${doc.getString("icon") ?: "🌐"} ${doc.getString("label") ?: "Website"}"
                }.toTypedArray()

                
showAnchoredOptions(context, this@apply, options) { which ->
    val url = links[which].getString("url").orEmpty()

    if (url.startsWith("https://") || url.startsWith("http://")) {
        context.startActivity(
            Intent(
                context,
                WebViewActivity::class.java
            ).putExtra("url", url)
        )
    }
}

            }
        }
        .addOnFailureListener {
            Toast.makeText(
                context,
                "Website लिंक लोड नहीं हुए।",
                Toast.LENGTH_SHORT
            ).show()
        }
}
        }

    val websiteIcon =
        TextView(context).apply {
            text = "🌐"
            textSize = 17f
            gravity = Gravity.CENTER
        }

    val websiteText =
        TextView(context).apply {
            text = "Website"
            textSize = 8f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
             setTextColor(Color.WHITE)
        }

    websiteBox.addView(
        websiteIcon,
        LinearLayout.LayoutParams(-1, dp(context, 25))
    )

    websiteBox.addView(
        websiteText,
        LinearLayout.LayoutParams(-1, dp(context, 17))
    )

    header.addView(
        websiteBox,
        LinearLayout.LayoutParams(
            dp(context, 48),
            dp(context, 52)
        )
    )
val contactIcon =
    TextView(context).apply {
        text = "🤝"
        textSize = 17f
        gravity = Gravity.CENTER
        setTextColor(Color.YELLOW)
    }

    val contactText =
        TextView(context).apply {
            text = "हमसे जुड़ें"
            textSize = 8f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }

    val contactBox =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true

            setOnClickListener {
                FirebaseFirestore.getInstance()
                    .collection("header_social_links")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val links = snapshot.documents
                            .filter { it.getBoolean("enabled") ?: true }
                            .sortedBy {
                                it.getLong("position") ?: Long.MAX_VALUE
                            }

                        if (links.isEmpty()) {
                            Toast.makeText(
                                context,
                                "अभी कोई Social Media लिंक उपलब्ध नहीं है।",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            val options = links.map { doc ->
                                "${doc.getString("icon") ?: "🔗"} ${
                                    doc.getString("label") ?: "Social Media"
                                }"
                            }.toTypedArray()

                            
showAnchoredOptions(context, this@apply, options) { which ->
    val url = links[which]
        .getString("url")
        .orEmpty()

    if (
        url.startsWith("https://") ||
        url.startsWith("http://")
    ) {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )
        )
    }
}

                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            context,
                            "Social Media लिंक लोड नहीं हुए।",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }

            addView(
                contactIcon,
                LinearLayout.LayoutParams(-1, dp(context, 25))
            )

            addView(
                contactText,
                LinearLayout.LayoutParams(-1, dp(context, 17))
            )
        }

    header.addView(
        contactBox,
        LinearLayout.LayoutParams(
            dp(context, 58),
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

            
              setTextColor(Color.WHITE)


            isClickable = true
            isFocusable = true

            setOnClickListener {
    onMenu(this)
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

    /**
     * साझा Account Menu:
     * Login/Register, Profile और वास्तविक Firebase Logout.
     */
    fun showAccountMenu(context: Context) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user == null || user.isAnonymous) {
            AlertDialog.Builder(context)
                .setTitle("👤 यूज़र अकाउंट")
                .setItems(
                    arrayOf(
                        "👤  Login करें"
                    )
                ) { _, which ->
                    if (which == 0) {
                        context.startActivity(
                            Intent(
                                context,
                                GroupAuthActivity::class.java
                            )
                        )
                    }
                }
                .setNegativeButton("बंद करें", null)
                .show()

            return
        }

        AlertDialog.Builder(context)
            .setTitle("👤 यूज़र अकाउंट")
            .setItems(
                arrayOf(
                    "📋 अपनी प्रोफाइल देखें",
                    "🚪 Logout करें"
                )
            ) { _, which ->

                when (which) {
                    0 -> {
                        val currentUser = auth.currentUser

                                                
                        if (currentUser == null) {
                            Toast.makeText(
                                context,
                                "पहले लॉगिन करें।",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setItems
                        }
                        currentUser.reload().addOnCompleteListener {
                            
                        FirebaseFirestore.getInstance()
                            .collection("group_profiles")
                            .document(currentUser.uid)
                            .get()
                            .addOnSuccessListener { document ->

                                val username =
                                    document.getString("displayName")
                                        ?: currentUser.displayName
                                        ?: "उपलब्ध नहीं"

                                val email =
                                    currentUser.email
                                        ?: document.getString("email")
                                        ?: "उपलब्ध नहीं"

                                                                val mobile =
                                    document.getString("mobileNumber")
                                        ?: "रिकॉर्ड उपलब्ध नहीं"

                                AlertDialog.Builder(context)
                                    .setMessage(
    "यूज़रनेम: $username\n\n" +
        "ईमेल: $email\n\n" +
        "Gmail सत्यापन: " +
        if (currentUser.isEmailVerified) {
            "सत्यापित ✅\n\n"
        } else {
            "सत्यापित नहीं ⚠️\n\n"
        } +
        "मोबाइल नंबर: $mobile\n\n" +
        "पासवर्ड: ••••••••"
)
                                    .setNeutralButton("✏️ प्रोफाइल संपादित करें") { _, _ ->

                                        val nameInput = EditText(context).apply {
                                            hint = "यूज़रनेम"
                                            setText(
                                                username.takeUnless {
                                                    it == "उपलब्ध नहीं"
                                                } ?: ""
                                            )
                                            isSingleLine = true
                                        }

                                        val mobileInput = EditText(context).apply {
                                            hint = "10 अंकों का मोबाइल नंबर"
                                            inputType =
                                                android.text.InputType.TYPE_CLASS_NUMBER
                                            filters = arrayOf(
                                                android.text.InputFilter.LengthFilter(10)
                                            )
                                            setText(
                                                mobile.takeUnless {
                                                    it == "रिकॉर्ड उपलब्ध नहीं"
                                                } ?: ""
                                            )
                                            isSingleLine = true
                                        }

                                        val form = LinearLayout(context).apply {
                                            orientation = LinearLayout.VERTICAL
                                            setPadding(32, 8, 32, 0)
                                            addView(nameInput)
                                            addView(mobileInput)
                                        }

                                        AlertDialog.Builder(context)
                                            .setTitle("✏️ प्रोफाइल अपडेट करें")
                                            .setView(form)
                                            .setNegativeButton("रद्द करें", null)
                                            .setPositiveButton("सेव करें") { _, _ ->

                                                val newName =
                                                    nameInput.text.toString().trim()

                                                val newMobile =
                                                    mobileInput.text.toString().trim()

                                                if (newName.isBlank() ||
                                                    newName.length > 40
                                                ) {
                                                    Toast.makeText(
                                                        context,
                                                        "यूज़रनेम 1 से 40 अक्षरों का रखें।",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                } else if (
                                                    !newMobile.matches(
                                                        Regex("^[0-9]{10}$")
                                                    )
                                                ) {
                                                    Toast.makeText(
                                                        context,
                                                        "मोबाइल नंबर ठीक 10 अंकों का दर्ज करें।",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                } else {

                                                    FirebaseFirestore.getInstance()
                                                        .collection("group_profiles")
                                                        .document(currentUser.uid)
                                                        .set(
                                                            mapOf(
                                                                "uid" to currentUser.uid,
                                                                "displayName" to newName,
                                                                "email" to (
                                                                    currentUser.email ?: ""
                                                                ),
                                                                "mobileNumber" to newMobile,
                                                                "updatedAt" to
                                                                    com.google.firebase.firestore.FieldValue.serverTimestamp()
                                                            ),
                                                            com.google.firebase.firestore.SetOptions.merge()
                                                        )
                                                        .addOnSuccessListener {
    val profileUpdates =
        com.google.firebase.auth.UserProfileChangeRequest
            .Builder()
            .setDisplayName(newName)
            .build()

    currentUser.updateProfile(profileUpdates)
        .addOnCompleteListener {
                        context.getSharedPreferences(
                "shiksha_auth",
                Context.MODE_PRIVATE
            ).edit()
                                .putString(
                    "last_username",
                    newName
                )
                .putString(
                    "last_mobile",
                    newMobile
                )
                .putString(
                    "last_email",
                    currentUser.email ?: ""
                )
                .apply()

            Toast.makeText(
                context,
                "प्रोफाइल सफलतापूर्वक अपडेट हो गई।",
                Toast.LENGTH_LONG
            ).show()
        }
}
                                                        .addOnFailureListener {
                                                            Toast.makeText(
                                                                context,
                                                                "प्रोफाइल अपडेट नहीं हुई। Firestore Rules जाँचें।",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                }
                                            }
                                            .show()
                                    }
                                    .setPositiveButton("ठीक है", null)
                                    .show()
                                    
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    context,
                                    "प्रोफाइल लोड नहीं हो सकी।",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    }
                    
                    1 -> {
                        AlertDialog.Builder(context)
                            .setTitle("Logout करें?")
                            .setMessage(
                                "क्या आप अपने अकाउंट से बाहर निकलना चाहते हैं?"
                            )
                            .setNegativeButton("नहीं", null)
                            .setPositiveButton("Logout") { _, _ ->
                                                                context.getSharedPreferences(
                                    "shiksha_auth",
                                    Context.MODE_PRIVATE
                                ).edit()
                                    .remove("last_username")
                                    .remove("last_mobile")
                                    .apply()

                                auth.signOut()

                                Toast.makeText(
                                    context,
                                    "आप सफलतापूर्वक Logout हो गए।",
                                    Toast.LENGTH_SHORT
                                ).show()

                                val intent = Intent(
                                    context,
                                    MainActivity::class.java
                                ).apply {
                                    flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }

                                context.startActivity(intent)
                            }
                            .show()
                    }
                }
            }
            .setNegativeButton("बंद करें", null)
            .show()
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

                if (context is MainActivity) {

                    context.findViewById<android.widget.ScrollView>(
                        R.id.contentScroll
                    ).smoothScrollTo(
                        0,
                        0
                    )

                } else {

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
                dp(context, 50)
            )
        )

        val badge =
    TextView(context).apply {

        id = R.id.channelBadge

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
        elevation = dp(context, 8).toFloat()
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

        // =====================================================
        // CHANNEL UNREAD BADGE: OTHER PAGES AUTO-REFRESH
        // =====================================================
        val unreadReceiver =
            object : android.content.BroadcastReceiver() {

                override fun onReceive(
                    receiverContext: Context?,
                    intent: Intent?
                ) {
                    if (
                        intent?.action !=
                        "com.shiksharojgar.app.CHANNEL_UNREAD_CHANGED"
                    ) {
                        return
                    }

                    if (
                        activeSection ==
                        Section.CHANNEL
                    ) {
                        badge.visibility = View.GONE
                        return
                    }

                    val unread =
                        receiverContext
                            ?.getSharedPreferences(
                                "sr_notifications",
                                Context.MODE_PRIVATE
                            )
                            ?.getInt(
                                "channel_unread",
                                0
                            ) ?: 0

                    if (unread > 0) {
                        badge.text =
                            if (unread > 99) {
                                "99+"
                            } else {
                                unread.toString()
                            }

                        badge.visibility = View.VISIBLE
                    } else {
                        badge.visibility = View.GONE
                    }
                }
            }

        var unreadReceiverRegistered = false

        channelBox.addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {

                override fun onViewAttachedToWindow(
                    view: View
                ) {
                    if (!unreadReceiverRegistered) {
                        androidx.core.content.ContextCompat.registerReceiver(
                            context,
                            unreadReceiver,
                            android.content.IntentFilter(
                                "com.shiksharojgar.app.CHANNEL_UNREAD_CHANGED"
                            ),
                            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
                        )

                        unreadReceiverRegistered = true
                    }
                }

                override fun onViewDetachedFromWindow(
                    view: View
                ) {
                    if (unreadReceiverRegistered) {
                        context.unregisterReceiver(
                            unreadReceiver
                        )

                        unreadReceiverRegistered = false
                    }
                }
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
        // Notice और Imp Info के लाल unread badges
        val noticeBox = FrameLayout(context)
        addUnreadBadge(
            context,
            noticeBox,
            notice,
            "notice_bottom_unread",
            activeSection == Section.NOTICE
        )

        val impBox = FrameLayout(context)
        addUnreadBadge(
            context,
            impBox,
            imp,
            "imp_info_bottom_unread",
            activeSection == Section.IMP_INFO
        )
        
        /*
         * MORE
         */
        
var moreAnchor: View? = null

val more =
    createBottomButton(
        context,
        "☰\nMore ▼",
        activeSection != Section.HOME &&
            activeSection != Section.CHANNEL &&
            activeSection != Section.NOTICE &&
            activeSection != Section.IMP_INFO
    ) {
        moreAnchor?.let { anchor ->
            showMoreMenu(context, anchor)
        }
    }

moreAnchor = more



        bottom.addView(
            home,
            weightParams(context)
        )

        bottom.addView(
            channelBox,
            weightParams(context)
        )

                bottom.addView(
            noticeBox,
            weightParams(context)
        )

        bottom.addView(
            impBox,
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
        
        
        val iconEnd = label.indexOf('\n').let {
            if (it >= 0) it else label.length
        }

        text = android.text.SpannableString(label).apply {
            setSpan(
                android.text.style.RelativeSizeSpan(1.25f),
                0,
                iconEnd,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            if (iconEnd < label.length) {
                setSpan(
                    android.text.style.RelativeSizeSpan(0.85f),
                    iconEnd + 1,
                    label.length,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        gravity = Gravity.CENTER
        textSize = 13f
        maxLines = 2
        includeFontPadding = false
        typeface = Typeface.DEFAULT_BOLD

        
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
    

    private fun addUnreadBadge(
        context: Context,
        box: FrameLayout,
        button: TextView,
        prefKey: String,
        hideBadge: Boolean
    ) {
        box.addView(
            button,
            FrameLayout.LayoutParams(
                -1,
                dp(context, 50)
            )
        )

        val badge = TextView(context).apply {
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)

            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.RED)
            }

            visibility = View.GONE
            elevation = dp(context, 8).toFloat()
        }

        fun refreshBadge() {
            val count = context
                .getSharedPreferences(
                    "sr_notifications",
                    Context.MODE_PRIVATE
                )
                .getInt(prefKey, 0)
if (count <= 0) {
    badge.visibility = View.GONE
} else {
    badge.text = if (count > 99) {
        "99+"
    } else {
        count.toString()
    }

    badge.visibility = View.VISIBLE
}
        }

        box.addView(
            badge,
            FrameLayout.LayoutParams(
    dp(context, 32),
    dp(context, 32)
).apply {
    gravity = Gravity.TOP or Gravity.END
    rightMargin = dp(context, 1)
    topMargin = dp(context, 0)
}
        )

        refreshBadge()

        val receiver =
            object : android.content.BroadcastReceiver() {
                override fun onReceive(
                    receiverContext: Context?,
                    intent: Intent?
                ) {
                    refreshBadge()
                }
            }

        var registered = false

        box.addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(
                    view: View
                ) {
                    if (!registered) {
                        androidx.core.content.ContextCompat.registerReceiver(
                            context,
                            receiver,
                            android.content.IntentFilter(
                                "com.shiksharojgar.app.UNREAD_COUNTS_CHANGED"
                            ),
                            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
                        )

                        registered = true
                        refreshBadge()
                    }
                }

                override fun onViewDetachedFromWindow(
                    view: View
                ) {
                    if (registered) {
                        try {
                            context.unregisterReceiver(receiver)
                        } catch (_: IllegalArgumentException) {
                        }

                        registered = false
                    }
                }
            }
        )
    }

    private fun weightParams(
        context: Context
    ): LinearLayout.LayoutParams {

        return LinearLayout.LayoutParams(
            0,
            dp(context, 50),
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
    context: Context,
    anchor: View
) {
    Toast.makeText(
        context,
        "More मेनू लोड हो रहा है…",
        Toast.LENGTH_SHORT
    ).show()

    FirebaseFirestore.getInstance()
        .collection("more_menu_items")
        .whereEqualTo("enabled", true)
        .get()
        .addOnSuccessListener { snap ->

            val items = snap.documents.sortedBy {
                it.getLong("position") ?: 0L
            }

            if (items.isEmpty()) {
                Toast.makeText(
                    context,
                    "More Menu अभी Admin द्वारा सेट नहीं किया गया है.",
                    Toast.LENGTH_SHORT
                ).show()
                return@addOnSuccessListener
            }

            val screenWidth =
                context.resources.displayMetrics.widthPixels
            val screenHeight =
                context.resources.displayMetrics.heightPixels

            val popupWidth = minOf(
                dp(context, 300),
                (screenWidth * 0.84f).toInt()
            )

            val popupHeight = (screenHeight * 0.60f).toInt()

            val scrollView = ScrollView(context)

            val list = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    dp(context, 10),
                    dp(context, 8),
                    dp(context, 10),
                    dp(context, 8)
                )
                setBackgroundColor(Color.WHITE)
            }

            scrollView.addView(
                list,
                FrameLayout.LayoutParams(
    FrameLayout.LayoutParams.MATCH_PARENT,
    FrameLayout.LayoutParams.WRAP_CONTENT
)
            )

            
val maxPopupHeight = minOf(
    dp(context, 420),
    (screenHeight * 0.60f).toInt()
)

val popup = PopupWindow(
    scrollView,
    popupWidth,
    LinearLayout.LayoutParams.WRAP_CONTENT,
    true
).apply {
    setBackgroundDrawable(
        ColorDrawable(Color.TRANSPARENT)
    )
    elevation = dp(context, 12).toFloat()
    isOutsideTouchable = true
    isFocusable = true
}


            val menuStack =
                mutableListOf<Pair<List<com.google.firebase.firestore.DocumentSnapshot>, String>>()

            
fun renderMenu(
    menuItems: List<com.google.firebase.firestore.DocumentSnapshot>,
    title: String
) {


                list.removeAllViews()

                val topBar = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(
                        dp(context, 8),
                        dp(context, 6),
                        dp(context, 4),
                        dp(context, 10)
                    )
                }

                if (menuStack.isNotEmpty()) {
                    val backButton = TextView(context).apply {
                        text = "←"
                        textSize = 24f
                        setTextColor(Color.parseColor("#1565C0"))
                        gravity = Gravity.CENTER
                    }

                    topBar.addView(
                        backButton,
                        LinearLayout.LayoutParams(
                            dp(context, 42),
                            dp(context, 42)
                        )
                    )

                    backButton.setOnClickListener {
                        if (menuStack.isNotEmpty()) {
                            val previous = menuStack.removeAt(
                                menuStack.lastIndex
                            )
                            renderMenu(previous.first, previous.second)
                        }
                    }
                }

                val titleView = TextView(context).apply {
                    text = title
                    textSize = 17f
                    setTextColor(Color.parseColor("#172033"))
                    setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                    gravity = Gravity.CENTER_VERTICAL
                }

                topBar.addView(
                    titleView,
                    LinearLayout.LayoutParams(
                        0,
                        dp(context, 42),
                        1f
                    )
                )

                val closeButton = TextView(context).apply {
                    text = "✕"
                    textSize = 19f
                    setTextColor(Color.DKGRAY)
                    gravity = Gravity.CENTER
                }

                topBar.addView(
                    closeButton,
                    LinearLayout.LayoutParams(
                        dp(context, 38),
                        dp(context, 42)
                    )
                )

                closeButton.setOnClickListener {
                    popup.dismiss()
                }

                list.addView(
                    topBar,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                )

                menuItems.forEach { doc ->

                    val iconText = doc.getString("icon") ?: "☰"
                    val labelText = doc.getString("label") ?: "Menu"
                    val menuType =
                        doc.getString("menuType")?.lowercase().orEmpty()

                    val colorText =
                        doc.getString("color") ?: "#E3F2FD"
                    
val textColorValue = try {
    Color.parseColor(
        doc.getString("textColor") ?: "#172033"
    )
} catch (_: Exception) {
    Color.parseColor("#172033")
}
    

                    val cardColor = try {
                        Color.parseColor(colorText)
                    } catch (_: Exception) {
                        Color.parseColor("#E3F2FD")
                    }

                    val card = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(
                            dp(context, 10),
                            dp(context, 6),
                            dp(context, 8),
                            dp(context, 6)
                        )

                        background = GradientDrawable().apply {
                            setColor(cardColor)
                            cornerRadius = dp(context, 12).toFloat()
                            setStroke(
                                dp(context, 1),
                                Color.parseColor("#D1D5DB")
                            )
                        }

                        isClickable = true
                        isFocusable = true
                    }

                    val iconView = TextView(context).apply {
                        text = iconText
                        textSize = 23f
                        gravity = Gravity.CENTER
                    }

                    card.addView(
                        iconView,
                        LinearLayout.LayoutParams(
                            dp(context, 42),
                            dp(context, 42)
                        )
                    )

                    val nameView = TextView(context).apply {
                        text = if (menuType == "section") {
                            "$labelText  ▼"
                        } else {
                            labelText
                        }
                        textSize = 14f
                        setTextColor(textColorValue)
                        setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(
                            dp(context, 8),
                            0,
                            dp(context, 4),
                            0
                        )
                    }

                    card.addView(
                        nameView,
                        LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                    )

                    val arrow = TextView(context).apply {
                        text = "›"
                        textSize = 26f
                        setTextColor(textColorValue)
                        gravity = Gravity.CENTER
                    }

                    card.addView(
                        arrow,
                        LinearLayout.LayoutParams(
                            dp(context, 24),
                            dp(context, 40)
                        )
                    )

                    list.addView(
                        card,
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = dp(context, 6)
                        }
                    )

                    card.setOnClickListener {
                        if (menuType == "section") {

                            val sectionId =
                                doc.getString("menuId")?.trim().orEmpty()

                            if (sectionId.isBlank()) {
                                Toast.makeText(
                                    context,
                                    "इस सेक्शन की ID सेट नहीं है।",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@setOnClickListener
                            }

                            val children = items.filter { child ->
                                child.getString("parentId")
                                    ?.trim() == sectionId &&
                                    child.getString("menuType")
                                        ?.lowercase() != "section"
                            }

                            if (children.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "इस सेक्शन में अभी कोई विकल्प नहीं है।",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@setOnClickListener
                            }

                            menuStack.add(menuItems to title)
                            renderMenu(children, "› $labelText")

                        } else {
                            popup.dismiss()
                            openMoreItem(context, doc)
                        }
                    }
                }
            }

            val rootItems = items.filter { doc ->
                val menuType =
                    doc.getString("menuType")?.lowercase().orEmpty()
                val parentId =
                    doc.getString("parentId")?.trim().orEmpty()

                menuType == "section" || parentId.isBlank()
            }

            
renderMenu(rootItems, "☰ More")

scrollView.measure(
    View.MeasureSpec.makeMeasureSpec(
        popupWidth,
        View.MeasureSpec.EXACTLY
    ),
    View.MeasureSpec.makeMeasureSpec(
        0,
        View.MeasureSpec.UNSPECIFIED
    )
)

val actualPopupHeight = minOf(
    scrollView.measuredHeight,
    maxPopupHeight
).coerceAtLeast(dp(context, 1))

popup.height = actualPopupHeight


if (anchor.isAttachedToWindow) {
    popup.showAsDropDown(
        anchor,
        0,
        -dp(context, 3),
        Gravity.END
    )
} else {
    Toast.makeText(
        context,
        "More मेनू खोलने के लिए दोबारा टैप करें।",
        Toast.LENGTH_SHORT
    ).show()
}
        }
        .addOnFailureListener {
            Toast.makeText(
                context,
                "More Menu लोड नहीं हो सका।",
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
                        "videos" -> {
    context.startActivity(
        Intent(
            context,
            VideoActivity::class.java
        )
    )
                        }
                        "group" -> {
                            context.startActivity(
                                Intent(
                                    context,
                                    GroupActivity::class.java
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
                        
                        "teacher",
                        "student",
                        "school",
                        "vacancy",
                        "result",
                        "admit_card" -> {

                            val categoryId = doc.getString("target")
                                ?.trim()
                                ?.lowercase()
                                .orEmpty()

                            val categoryName = label
                            val categoryIcon = doc.getString("icon")
                                ?.trim()
                                .orEmpty()

                            context.startActivity(
                                Intent(
                                    context,
                                    HomeCategoryActivity::class.java
                                ).apply {
                                    putExtra("categoryId", categoryId)
                                    putExtra("categoryName", categoryName)
                                    putExtra("categoryIcon", categoryIcon)
                                }
                            )
                        }
                        
                        else -> {
                            val pageId = doc.getString("target")
?.trim()
?.lowercase()
.orEmpty()

                            if (pageId.isNotBlank()) {
                                context.startActivity(
                                    Intent(
                                        context,
                                        ContentPageActivity::class.java
                                    ).apply {
                                        putExtra("pageId", pageId)
                                        putExtra("pageTitle", label)
                                        putExtra("activeSection", "MORE")
                                    }
                                )
                            } else {
                                Toast.makeText(
                                    context,
                                    "इस पेज की ID उपलब्ध नहीं है।",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
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
                    val shareText =
                        if (content.isNotBlank()) content else url

                    ShareHelper.shareImageOrText(
                        context,
                        "Shiksha Rojgar App",
                        shareText,
                        chooserTitle = "Share"
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
