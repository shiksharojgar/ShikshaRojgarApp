package com.shiksharojgar.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GroupActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var statusView: TextView
    private lateinit var loginButton: Button
    private lateinit var writeButton: Button
    private lateinit var reportButton: Button
    private lateinit var postsContainer: LinearLayout
    private lateinit var pendingContainer: LinearLayout

    private var feedStarted = false

    private val loginLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                updateLoginState()
                loadMyPendingPosts()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(buildScreen())
    updateLoginState()

    val postId = intent?.data?.getQueryParameter("post")

    if (!postId.isNullOrBlank()) {
        openSharedGroupPost(postId)
    } else {
        loadApprovedPosts()
    }
}

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun buildScreen(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(245, 249, 255))
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                systemBars.bottom
            )
            insets
        }

        root.addView(
            CommonPageUi.createHeader(
                context = this,
                onBack = { finish() },
                onShare = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "📱 Shiksha Rojgar App 👥 शिक्षा रोजगार ग्रुप\nhttps://shiksha-rojgar.web.app/"
                        )
                    }
                    startActivity(
                        Intent.createChooser(shareIntent, "ग्रुप शेयर करें")
                    )
                },
                                onMenu = {
                    CommonPageUi.showAccountMenu(this)
                }
            )
        )
        
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(18), dp(16), dp(18))
            setBackgroundColor(Color.rgb(3, 105, 161))
        }

        val title = TextView(this).apply {
            text = "👥 शिक्षा रोजगार ग्रुप"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "• पोस्ट करें • उपयोगी जानकारी शेयर • एक दूसरे की मदद •"
            textSize = 13f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        header.addView(title)
        header.addView(subtitle)
        root.addView(header)

        loginButton = Button(this).apply {
            setOnClickListener {
                openLogin()
            }
        }

                writeButton = Button(this).apply {
            text = "➕ ✍️ अपनी पोस्ट लिखें…                         ➤"
            textSize = 14f
    isAllCaps = false
    typeface = Typeface.DEFAULT_BOLD
    setTextColor(Color.WHITE)
    gravity = Gravity.CENTER
    setPadding(dp(8), dp(4), dp(8), dp(4))
    setBackgroundTintList(
        android.content.res.ColorStateList.valueOf(
            Color.rgb(22, 101, 52)
        )
    )
    setOnClickListener {
        openPostEditor()
    }
}

        reportButton = Button(this).apply {
            text = "🛡️ शिकायत / सुझाव भेजें"
            setOnClickListener {
                openReportDialog(null)
            }
        }

        root.addView(loginButton, buttonParams())
root.addView(reportButton, buttonParams())

        statusView = TextView(this).apply {
            text = "पोस्ट लोड हो रही हैं..."
            textSize = 15f
            setTextColor(Color.DKGRAY)
            setPadding(dp(14), dp(10), dp(14), dp(6))
        }

        root.addView(statusView)

        val scroll = ScrollView(this)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(4), dp(12), dp(20))
        }

        content.addView(sectionTitle("🕒 आपकी Pending पोस्ट"))

        pendingContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        content.addView(pendingContainer)
        content.addView(sectionTitle("📢 मंजूर ग्रुप पोस्ट"))

        postsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        content.addView(postsContainer)
        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
root.addView(
    writeButton,
    LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        setMargins(dp(10), dp(6), dp(10), dp(8))
    }
)
        root.addView(
            CommonPageUi.createBottomBar(
                this,
                CommonPageUi.Section.MORE
            ),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        return root
    }

    private fun buttonParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(dp(12), dp(3), dp(12), dp(1))
        }
    }

    private fun sectionTitle(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(3, 105, 161))
            setPadding(dp(2), dp(12), dp(2), dp(8))
        }
    }

    private fun updateLoginState() {
        val user = auth.currentUser

        if (user == null || user.isAnonymous) {
            loginButton.text = "🔐 लॉगिन / रजिस्ट्रेशन करें"

            pendingContainer.removeAllViews()
            addMessage(
                pendingContainer,
                "Pending पोस्ट देखने के लिए लॉगिन करें।"
            )
            return
        }

        loginButton.text = "✅ लॉगिन: ${user.email ?: "यूज़र"}"

        db.collection("group_profiles")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val name = document.getString("displayName")

                if (!name.isNullOrBlank()) {
                    loginButton.text = "✅ ${name.take(40)}"
                }
            }

        loadMyPendingPosts()
    }

    private fun openLogin() {
        loginLauncher.launch(
            Intent(this, GroupAuthActivity::class.java)
        )
    }

    private fun openPostEditor() {
        val user = auth.currentUser

        if (user == null || user.isAnonymous) {
            AlertDialog.Builder(this)
                .setTitle("लॉगिन आवश्यक है")
                .setMessage(
                    "ग्रुप में पोस्ट लिखने के लिए पहले ईमेल और पासवर्ड से रजिस्ट्रेशन या लॉगिन करें।"
                )
                .setPositiveButton("लॉगिन / रजिस्ट्रेशन") { _, _ ->
                    openLogin()
                }
                .setNegativeButton("अभी नहीं", null)
                .show()
            return
        }

     // Gmail verification required before posting
if (!user.isEmailVerified) {
    user.reload()
        .addOnSuccessListener {
            if (user.isEmailVerified) {
                openPostEditor()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Gmail सत्यापन आवश्यक है")
                    .setMessage(
                        "ग्रुप में पोस्ट करने से पहले अपने Gmail में " +
                            "आया सत्यापन लिंक खोलें। ईमेल न मिले तो " +
                            "नीचे से दोबारा भेज सकते हैं।"
                    )
                    .setPositiveButton("ईमेल दोबारा भेजें") { _, _ ->
                        user.sendEmailVerification()
                            .addOnSuccessListener {
                                toast(
                                    "सत्यापन लिंक आपके Gmail पर भेज दिया गया है।"
                                )
                            }
                            .addOnFailureListener {
                                toast(
                                    "ईमेल नहीं भेजा जा सका। बाद में प्रयास करें।"
                                )
                            }
                    }
                    .setNegativeButton("बंद करें", null)
                    .show()
            }
        }
        .addOnFailureListener {
            toast(
                "Gmail सत्यापन की जाँच नहीं हो सकी। " +
                    "नेटवर्क जाँचकर दोबारा प्रयास करें।"
            )
        }

    return
}

        val nameInput = EditText(this).apply {
            hint = "आपका नाम"
            setSingleLine(true)
            filters = arrayOf(InputFilter.LengthFilter(40))
        }

        val bodyInput = EditText(this).apply {
            hint = "अपनी पोस्ट लिखें (अधिकतम 5000 अक्षर)"
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 5
            gravity = Gravity.TOP
            filters = arrayOf(InputFilter.LengthFilter(5000))
        }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(nameInput)
            addView(bodyInput)
        }

        db.collection("group_profiles")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                nameInput.setText(
                    document.getString("displayName").orEmpty()
                )
            }

        val dialog = AlertDialog.Builder(this)
            .setTitle("नई ग्रुप पोस्ट")
            .setMessage(
                "पोस्ट Admin की मंजूरी के बाद ही सार्वजनिक दिखेगी।"
            )
            .setView(box)
            .setPositiveButton("पोस्ट भेजें", null)
            .setNegativeButton("रद्द करें", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val name = nameInput.text.toString().trim()
                    val body = bodyInput.text.toString().trim()

                    if (name.isBlank() || name.length > 40) {
                        toast("नाम 1 से 40 अक्षरों के बीच रखें।")
                        return@setOnClickListener
                    }

                    if (body.isBlank() || body.length > 5000) {
                        toast("पोस्ट 1 से 5000 अक्षरों के बीच लिखें।")
                        return@setOnClickListener
                    }

                    GroupRepository.submitPost(name, body) {
                            success, error ->
                        if (success) {
                            toast(
                                "पोस्ट भेज दी गई है। Admin की मंजूरी के बाद दिखेगी।"
                            )
                            dialog.dismiss()
                            loadMyPendingPosts()
                        } else {
                            toast(error ?: "पोस्ट नहीं भेजी जा सकी।")
                        }
                    }
                }
        }

        dialog.show()
    }

    private fun openSharedGroupPost(postId: String) {
    GroupRepository.approvedPostById(
        postId = postId,
        onResult = { post ->
            if (post == null) {
                Toast.makeText(
                    this,
                    "यह पोस्ट उपलब्ध नहीं है या इसकी समय-सीमा समाप्त हो गई है।",
                    Toast.LENGTH_LONG
                ).show()

                loadApprovedPosts()
            } else {
                postsContainer.removeAllViews()
                statusView.text = "📌 शेयर की गई ग्रुप पोस्ट"

                addPostCard(postsContainer, post, false)
            }
        },
        onError = {
            Toast.makeText(
                this,
                "पोस्ट खोलने में समस्या हुई। कृपया दोबारा प्रयास करें।",
                Toast.LENGTH_LONG
            ).show()

            loadApprovedPosts()
        }
    )
}
    
    private fun loadApprovedPosts() {
        if (feedStarted) return

        feedStarted = true

        GroupRepository.approvedPosts(
            onResult = { posts ->
                postsContainer.removeAllViews()
                statusView.text = "मंजूर पोस्ट: ${posts.size}"

                if (posts.isEmpty()) {
                    addMessage(
                        postsContainer,
                        "अभी कोई मंजूर पोस्ट नहीं है।"
                    )
                } else {
                    posts.forEach { post ->
                        addPostCard(postsContainer, post, false)
                    }
                }
            },
            onError = {
                statusView.text = "पोस्ट लोड नहीं हो सकीं।"

                addMessage(
                    postsContainer,
                        "नेटवर्क या अन्य समस्या हो सकती है।"
                )
            }
        )
    }

    private fun loadMyPendingPosts() {
        val user = auth.currentUser ?: return

        if (user.isAnonymous) return

        db.collection("group_posts")
            .whereEqualTo("uid", user.uid)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snapshot ->
                pendingContainer.removeAllViews()

                val documents = snapshot.documents.sortedBy {
                    it.getTimestamp("createdAt")
                        ?.toDate()?.time ?: 0L
                }

                if (documents.isEmpty()) {
                    addMessage(
                        pendingContainer,
                        "आपकी कोई Pending पोस्ट नहीं है।"
                    )
                    return@addOnSuccessListener
                }

                documents.forEach { document ->
                    val post = GroupPost(
                        id = document.id,
                        uid = document.getString("uid").orEmpty(),
                        displayName = document.getString("displayName").orEmpty(),
                        body = document.getString("body").orEmpty(),
                        status = document.getString("status") ?: "pending",
                        createdAt = document.getTimestamp("createdAt")
                            ?.toDate()?.time ?: 0L
                    )

                    addPostCard(pendingContainer, post, true)
                }
            }
            .addOnFailureListener {
                pendingContainer.removeAllViews()

                addMessage(
                    pendingContainer,
                    "Pending पोस्ट लोड नहीं हो सकीं।"
                )
            }
    }

    private fun addPostCard(
        container: LinearLayout,
        post: GroupPost,
        isPending: Boolean
    ) {
        
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(Color.WHITE)
        }

        // पोस्ट के ऊपर हरी लाइन
        val greenLine = View(this).apply {
            setBackgroundColor(Color.rgb(22, 131, 74))
        }

        card.addView(
            greenLine,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(3)
            ).apply {
                bottomMargin = dp(10)
            }
        )

        // यूजर का नाम और उपलब्ध मोबाइल नंबर
        val userInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val name = TextView(this).apply {
            text = "👤 ${post.displayName}"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(7, 89, 133))
        }

        val mobile = TextView(this).apply {
            text = "📱 मोबाइल नंबर लोड हो रहा है…"
            textSize = 13f
            setTextColor(Color.DKGRAY)
        }

        userInfo.addView(name)
        userInfo.addView(mobile)
        card.addView(userInfo)

        if (post.uid.isNotBlank()) {
            db.collection("group_profiles")
                .document(post.uid)
                .get()
                .addOnSuccessListener { profile ->
                    val number = profile.getString("mobileNumber")
                    mobile.text = if (!number.isNullOrBlank()) {
                        "📱 $number"
                    } else {
                        "📱 मोबाइल नंबर उपलब्ध नहीं"
                    }
                }
                .addOnFailureListener {
                    mobile.text = "📱 मोबाइल नंबर उपलब्ध नहीं"
                }
        } else {
            mobile.text = "📱 मोबाइल नंबर उपलब्ध नहीं"
        }

        val dateText = if (post.createdAt > 0L) {
            SimpleDateFormat(
                "dd-MM-yyyy hh:mm a",
                Locale("hi", "IN")
            ).format(Date(post.createdAt))
        } else {
            "समय उपलब्ध नहीं"
        }

        val date = TextView(this).apply {
            text = "📅 $dateText"
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, dp(5), 0, dp(8))
        }

        card.addView(date)

        // पहली लाइन को शीर्षक और शेष टेक्स्ट को विवरण मानें।
        val lines = post.body.lines()
        val titleText = lines.firstOrNull().orEmpty()
        val remainingBody = lines.drop(1).joinToString("\n").trim()

        val titleView = TextView(this).apply {
            text = titleText
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(31, 41, 55))
            setPadding(0, dp(4), 0, dp(6))
        }

        card.addView(titleView)

        if (remainingBody.isNotBlank()) {
            val body = TextView(this).apply {
                text = remainingBody
                textSize = 15f
                setTextColor(Color.rgb(31, 41, 55))
                setPadding(0, dp(4), 0, dp(8))
                autoLinkMask = Linkify.WEB_URLS
                movementMethod = LinkMovementMethod.getInstance()
            }

            card.addView(body)
        }


                // --------------------------------------------
        // GROUP POST ACTION BUTTONS
        // Like + Share + Complaint
        // --------------------------------------------

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        }

        if (isPending) {

            val deleteButton = Button(this).apply {
                text = "🗑️ अपनी पोस्ट हटाएँ"
                setOnClickListener {
                    confirmDelete(post)
                }
            }

            actions.addView(
                deleteButton,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    -2
                )
            )

        } else {

             var likeCount = 0L
            var shareCount = 0L
            var isLiked = false

            val likeButton = Button(this).apply {
                text = "👍 Like $likeCount"
                textSize = 12f
                isAllCaps = false
                minWidth = 0
                setPadding(dp(2), 0, dp(2), 0)
            }

            val shareButton = Button(this).apply {
                text = "↗️ Share $shareCount"
                textSize = 12f
                isAllCaps = false
                minWidth = 0
                setPadding(dp(2), 0, dp(2), 0)
            }

            val reportPostButton = Button(this).apply {
                text = "⚑ शिकायत"
                textSize = 12f
                isAllCaps = false
                minWidth = 0
                setPadding(dp(2), 0, dp(2), 0)

                setOnClickListener {
                    openReportDialog(post.id)
                }
            }

            // तीनों बटन मोबाइल में बराबर जगह लेंगे।
            val actionParams = LinearLayout.LayoutParams(
                0,
                -2,
                1f
            ).apply {
                marginEnd = dp(2)
            }

            actions.addView(likeButton, actionParams)

            actions.addView(
                shareButton,
                LinearLayout.LayoutParams(
                    0,
                    -2,
                    1f
                ).apply {
                    marginEnd = dp(2)
                }
            )

            actions.addView(
                reportPostButton,
                LinearLayout.LayoutParams(
                    0,
                    -2,
                    1f
                )
            )

                        // शुरुआती Like काउंट पढ़ें।
            db.collection("group_posts")
                .document(post.id)
                .collection("likes")
                .get()
                .addOnSuccessListener { snapshot ->

                    likeCount = snapshot.size().toLong()

                    likeButton.text = if (isLiked) {
                        "👍 Liked $likeCount"
                    } else {
                        "👍 Like $likeCount"
                    }
                }
                .addOnFailureListener { error ->
                    android.util.Log.w(
                        "GroupActivity",
                        "Like काउंट नहीं पढ़ा जा सका।",
                        error
                    )
                }

            // शुरुआती Share काउंट पढ़ें।
            db.collection("group_posts")
                .document(post.id)
                .collection("shares")
                .get()
                .addOnSuccessListener { snapshot ->

                    shareCount = snapshot.size().toLong()
                    shareButton.text = "↗️ Share $shareCount"
                }
                .addOnFailureListener { error ->
                    android.util.Log.w(
                        "GroupActivity",
                        "Share काउंट नहीं पढ़ा जा सका।",
                        error
                    )
                }

            // पता करें कि मौजूदा यूज़र ने Like किया है या नहीं।
            val currentUser = auth.currentUser

            if (currentUser != null && !currentUser.isAnonymous) {
                db.collection("group_posts")
                    .document(post.id)
                    .collection("likes")
                    .document(currentUser.uid)
                    .get()
                    .addOnSuccessListener { likeDocument ->

                        isLiked = likeDocument.exists()

                        likeButton.text = if (isLiked) {
                            "👍 Liked $likeCount"
                        } else {
                            "👍 Like $likeCount"
                        }
                    }
            }

            // Like / Unlike
            likeButton.setOnClickListener {

                GroupRepository.toggleLike(post.id) {
                        liked, count, error ->

                    if (error != null) {
                        toast(error)
                    } else {
                        isLiked = liked
                        likeCount = count

                        likeButton.text = if (isLiked) {
                            "👍 Liked $likeCount"
                        } else {
                            "👍 Like $likeCount"
                        }
                    }
                }
            }

            // Share
            shareButton.setOnClickListener {

                val shareText =
                    "*📱 Shiksha Rojgar App  👥 शिक्षा रोजगार ग्रुप*\n\n" +
                    "*पोस्ट देखें 👇*\n" +
                    "https://shiksha-rojgar.web.app/group?post=${post.id}\n\n" +
                    "*पोस्ट की जानकारी:*\n" +
                    "${post.body}\n\n" +
                    "*📅 तारीख और समय: $dateText*\n\n" +
                    "*✅ शिक्षा रोजगार की Official Website 🌐*\n" +
                    "https://www.shiksharojgar.com/\n" +
                    "*☝️ यहां क्लिक करके 📱 Shiksha Rojgar App डाउनलोड इंस्टॉल करें !*"

                // मौजूदा शेयर सुविधा चालू रखें।
                ShareHelper.shareImageOrText(
                    this@GroupActivity,
                    "शिक्षा रोजगार ग्रुप पोस्ट",
                    shareText,
                    chooserTitle = "Shiksha Rojgar पर शेयर करें"
                )

                // शेयर काउंट अपडेट करने का प्रयास करें।
                GroupRepository.recordShare(post.id) {
                        success, count, error ->

                    if (success) {
                        shareCount = count
                        shareButton.text = "↗️ Share $shareCount"
                    } else {
                        android.util.Log.w(
                            "GroupActivity",
                            error ?: "Share काउंट अपडेट नहीं हुआ।"
                        )
                    }
                }
            }
        }

        if (actions.childCount > 0) {
            card.addView(actions)
        }
        

        container.addView(
            card,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(12)
            }
        )
    }

    private fun confirmDelete(post: GroupPost) {
        AlertDialog.Builder(this)
            .setTitle("अपनी पोस्ट हटाएँ?")
            .setMessage("यह पोस्ट ग्रुप से हटा दी जाएगी।")
            .setPositiveButton("हटाएँ") { _, _ ->
                GroupRepository.deletePost(post.id) { success, error ->
                    if (success) {
                        toast("पोस्ट हटा दी गई।")
                        loadMyPendingPosts()
                    } else {
                        toast(error ?: "पोस्ट हटाई नहीं जा सकी।")
                    }
                }
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun openReportDialog(postId: String?) {
        val user = auth.currentUser

        if (user == null || user.isAnonymous) {
            AlertDialog.Builder(this)
                .setTitle("लॉगिन आवश्यक है")
                .setMessage(
                    "शिकायत या सुझाव भेजने के लिए पहले लॉगिन करें।"
                )
                .setPositiveButton("लॉगिन करें") { _, _ ->
                    openLogin()
                }
                .setNegativeButton("रद्द करें", null)
                .show()
            return
        }

        val subjectInput = EditText(this).apply {
            hint = "शिकायत / सुझाव का विषय"
            setSingleLine(true)
            filters = arrayOf(InputFilter.LengthFilter(150))
        }

        val messageInput = EditText(this).apply {
            hint = "विस्तार से लिखें"
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
            gravity = Gravity.TOP
            filters = arrayOf(InputFilter.LengthFilter(3000))
        }

        val solutionInput = EditText(this).apply {
            hint = "आपका सुझाया समाधान (वैकल्पिक)"
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 2
            gravity = Gravity.TOP
            filters = arrayOf(InputFilter.LengthFilter(2000))
        }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(subjectInput)
            addView(messageInput)
            addView(solutionInput)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("निजी शिकायत / सुझाव")
            .setMessage(
                "यह जानकारी केवल आप और Admin पढ़ सकेंगे।"
            )
            .setView(box)
            .setPositiveButton("भेजें", null)
            .setNegativeButton("रद्द करें", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val subject = subjectInput.text.toString().trim()
                    val message = messageInput.text.toString().trim()
                    val solution = solutionInput.text.toString().trim()

                    if (subject.isBlank() || message.isBlank()) {
                        toast("विषय और शिकायत दोनों भरें।")
                        return@setOnClickListener
                    }

                    GroupRepository.submitReport(
                        subject,
                        message,
                        solution,
                        postId
                    ) { success, error ->
                        if (success) {
                            toast(
                                "आपकी निजी शिकायत Admin को भेज दी गई है।"
                            )
                            dialog.dismiss()
                        } else {
                            toast(error ?: "शिकायत भेजी नहीं जा सकी।")
                        }
                    }
                }
        }

        dialog.show()
    }

    private fun addMessage(
        container: LinearLayout,
        message: String
    ) {
        val text = TextView(this).apply {
            this.text = message
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        container.addView(text)
    }

    private fun toast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}
