package com.shiksharojgar.app

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import java.text.SimpleDateFormat
import java.util.*

class ChannelActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    private var phoneVerificationId: String? = null

    private lateinit var list: LinearLayout
    private lateinit var followBtn: TextView
    private lateinit var followerCountView: TextView
    private lateinit var commentStatusView: TextView
    private lateinit var feedScroll: ScrollView

    private var globalComments = true

    private val posts = mutableListOf<ChannelPost>()

    private val likedState =
        mutableMapOf<String, Boolean>()

    private val likeOverrides =
        mutableMapOf<String, Long>()

    private val viewOverrides =
        mutableMapOf<String, Long>()

    private val shareOverrides =
        mutableMapOf<String, Long>()

    private val viewedSession =
        mutableSetOf<String>()

    private var followerCount = 0L

    private var followBusy = false

    private var firstFeedRender = true

    private var followingState = false

    private var pendingPostId: String? = null

    private val fmt =
        SimpleDateFormat(
            "dd MMM yyyy, hh:mm a",
            Locale("hi", "IN")
        )

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(makeUi())

        pendingPostId =
            intent.data?.let { uri ->

                uri.getQueryParameter("post")
                    ?: uri.lastPathSegment?.takeIf {
                        uri.path?.contains("/post") == true
                    }
            }

        getSharedPreferences(
            "sr_notifications",
            MODE_PRIVATE
        )
            .edit()
            .putInt(
                "channel_unread",
                0
            )
            .putBoolean(
                "channel_open",
                true
            )
            .putLong(
                "channel_last_seen_at",
                System.currentTimeMillis()
            )
            .putBoolean(
                "channel_unread_initialized",
                true
            )
            .apply()

        ChannelRepository.config { count, comments ->

            globalComments = comments

            followerCount =
                count.coerceAtLeast(0L)

            runOnUiThread {

                followerCountView.text =
                    "👥 $followerCount Followers"

                commentStatusView.text =
                    if (comments) {
                        "💬 Comments ON"
                    } else {
                        "🔒 Comments OFF"
                    }

                renderPosts()
            }
        }

        ChannelRepository.posts(
            { p ->

                val oldNewest =
                    posts.lastOrNull()?.id

                posts.clear()
                posts.addAll(p)

                runOnUiThread {

                    renderPosts()

                    val newNewest =
                        posts.lastOrNull()?.id

                    if (
                        firstFeedRender ||
                        oldNewest != newNewest
                    ) {
                        scrollToNewest()
                    }

                    pendingPostId?.let { id ->

                        val index =
                            posts.indexOfFirst {
                                it.id == id
                            }

                        if (index >= 0) {
                            scrollToPost(index)
                        }

                        pendingPostId = null
                    }

                    firstFeedRender = false
                }
            },
            {}
        )

        ChannelRepository.ensureSignedIn { connected ->

            if (connected) {

                ChannelRepository.isFollowing { following ->

                    runOnUiThread {
                        updateFollow(following)
                    }

                    if (following) {

                        FirebaseMessaging
                            .getInstance()
                            .subscribeToTopic(
                                "all_channel_followers"
                            )

                    } else {

                        FirebaseMessaging
                            .getInstance()
                            .unsubscribeFromTopic(
                                "all_channel_followers"
                            )
                    }
                }

            } else {

                runOnUiThread {
                    updateFollow(false)
                }
            }
        }
    }

    override fun onDestroy() {

        getSharedPreferences(
            "sr_notifications",
            MODE_PRIVATE
        )
            .edit()
            .putBoolean(
                "channel_open",
                false
            )
            .apply()

        super.onDestroy()
    }

    override fun onNewIntent(
        intent: Intent
    ) {
        super.onNewIntent(intent)

        setIntent(intent)

        pendingPostId =
            intent.data?.let { uri ->

                uri.getQueryParameter("post")
                    ?: uri.lastPathSegment?.takeIf {
                        uri.path?.contains("/post") == true
                    }
            }

        pendingPostId?.let { id ->

            if (posts.isNotEmpty()) {

                val index =
                    posts.indexOfFirst {
                        it.id == id
                    }

                if (index >= 0) {
                    scrollToPost(index)
                }
            }
        }
    }

    private fun scrollToPost(
        index: Int
    ) {

        if (
            !::feedScroll.isInitialized ||
            !::list.isInitialized
        ) {
            return
        }

        if (list.childCount <= 0) {
            return
        }

        val child =
            list.getChildAt(
                index.coerceIn(
                    0,
                    list.childCount - 1
                )
            )

        if (child != null) {

            feedScroll.post {

                feedScroll.smoothScrollTo(
                    0,
                    child.top
                )
            }
        }
    }

    private fun scrollToNewest() {

        if (!::feedScroll.isInitialized) {
            return
        }

        feedScroll.post {

            feedScroll.fullScroll(
                View.FOCUS_DOWN
            )
        }
    }

private fun toolButton(
    label: String,
    action: () -> Unit
): TextView = TextView(this).apply {

    text = label
    gravity = Gravity.CENTER
    textSize = 11f
    typeface = Typeface.DEFAULT_BOLD

    setTextColor(Color.WHITE)

    background =
        GradientFactory.rounded("#174E86")

    isClickable = true
    isFocusable = true

    setOnClickListener {
        action()
    }
}
    
    private fun makeUi(): View {

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setBackgroundColor(
                    Color.rgb(
                        245,
                        248,
                        252
                    )
                )
            }

        val toolbar =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    6,
                    6,
                    6,
                    6
                )

                setBackgroundColor(
                    Color.rgb(
                        6,
                        59,
                        122
                    )
                )
            }

        ViewCompat.setOnApplyWindowInsetsListener(
            toolbar
        ) { view, insets ->

            val top =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                ).top

            view.setPadding(
                view.paddingLeft,
                top,
                view.paddingRight,
                view.paddingBottom
            )

            insets
        }

        val back =
            toolButton(
                "‹",
                {
                    onBackPressedDispatcher.onBackPressed()
                }
            )

        toolbar.addView(
            back,
            LinearLayout.LayoutParams(
                48,
                48
            )
        )

        val title =
            TextView(this).apply {

                text =
                    "📢 Shiksha Rojgar Channel"

                textSize = 17f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.WHITE
                )

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    8,
                    0,
                    4,
                    0
                )

                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        48,
                        1f
                    )
            }

        toolbar.addView(title)

        val channelShare =
            toolButton(
                "↗\nShare",
                {
                    shareChannel()
                }
            )

        toolbar.addView(
            channelShare,
            LinearLayout.LayoutParams(
                58,
                48
            )
        )

        root.addView(
            toolbar,
            LinearLayout.LayoutParams(
                -1,
                56
            )
        )

        val header =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    14,
                    12,
                    14,
                    8
                )

                background =
                    GradientFactory.roundedWhite()
            }

        header.addView(
            TextView(this).apply {

                text =
                    "📢 शिक्षा एवं रोजगार अपडेट"

                textSize = 20f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.rgb(
                        6,
                        59,
                        122
                    )
                )
            }
        )

        header.addView(
            TextView(this).apply {

                text =
                    "सरकारी आदेश, शिक्षा, रोजगार, टाइम टेबल और महत्वपूर्ण अपडेट"

                textSize = 13f

                setTextColor(
                    Color.DKGRAY
                )

                setPadding(
                    0,
                    4,
                    0,
                    4
                )
            }
        )

        val stats =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    0,
                    5,
                    0,
                    2
                )
            }

        followerCountView =
            TextView(this).apply {

                text =
                    "👥 $followerCount Followers"

                textSize = 13f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.rgb(
                        14,
                        91,
                        215
                    )
                )

                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        -2,
                        1f
                    )
            }

        commentStatusView =
            TextView(this).apply {

                text =
                    if (globalComments) {
                        "💬 Comments ON"
                    } else {
                        "🔒 Comments OFF"
                    }

                textSize = 12f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.rgb(
                        71,
                        85,
                        105
                    )
                )
            }

        stats.addView(
            followerCountView
        )

        stats.addView(
            commentStatusView
        )

        header.addView(stats)

        root.addView(
            header,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )

        val followArea =
            LinearLayout(this).apply {

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    12,
                    8,
                    12,
                    8
                )

                background =
                    Color.WHITE.toDrawable()
            }

        followBtn =
            TextView(this).apply {

                text =
                    "➕  Follow करें"

                gravity =
                    Gravity.CENTER

                textSize = 14f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.WHITE
                )

                background =
                    GradientFactory.gradient(
                        "#16A34A",
                        "#06B6D4"
                    )

                isClickable = true
                isFocusable = true

                setPadding(
                    18,
                    10,
                    18,
                    10
                )

                setOnClickListener {
                    toggleFollow()
                }
            }

        followArea.addView(
            followBtn,
            LinearLayout.LayoutParams(
                0,
                48,
                1f
            )
        )

        val offlineButton =
            TextView(this).apply {

                text =
                    "📥 Offline"

                gravity =
                    Gravity.CENTER

                textSize = 13f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.rgb(
                        14,
                        91,
                        215
                    )
                )

                background =
                    Color.rgb(
                        239,
                        246,
                        255
                    ).toDrawable()

                setPadding(
                    12,
                    8,
                    12,
                    8
                )

                setOnClickListener {
                    showOfflineList()
                }
            }

        followArea.addView(
            offlineButton,
            LinearLayout.LayoutParams(
                105,
                48
            ).apply {

                setMargins(
                    8,
                    0,
                    0,
                    0
                )
            }
        )

        root.addView(
            followArea,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )

        feedScroll =
            ScrollView(this).apply {

                isFillViewport = true
            }

        list =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    10,
                    10,
                    10,
                    10
                )
            }

        feedScroll.addView(
            list,
            android.widget.FrameLayout.LayoutParams(
                -1,
                -2
            )
        )

        root.addView(
            feedScroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        val bottom =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER

                setPadding(
                    6,
                    6,
                    6,
                    6
                )

                background =
                    Color.WHITE.toDrawable()
            }

        bottom.addView(
            bottomButton(
                "⌂  Home",
                {
                    goHome()
                }
            ),
            LinearLayout.LayoutParams(
                0,
                48,
                1f
            )
        )

        bottom.addView(
            bottomButton(
                "↗  Share",
                {
                    shareChannel()
                }
            ),
            LinearLayout.LayoutParams(
                0,
                48,
                1f
            ).apply {

                setMargins(
                    6,
                    0,
                    0,
                    0
                )
            }
        )

        bottom.addView(
            bottomButton(
                "‹  Back",
                {
                    onBackPressedDispatcher.onBackPressed()
                }
            ),
            LinearLayout.LayoutParams(
                0,
                48,
                1f
            ).apply {

                setMargins(
                    6,
                    0,
                    0,
                    0
                )
            }
        )

        root.addView(
            bottom,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )

        return root
    }
    private fun bottomButton(
        label: String,
        action: () -> Unit
    ): TextView = TextView(this).apply {

        text = label
        gravity = Gravity.CENTER
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD

        setTextColor(
            Color.rgb(14, 91, 215)
        )

        background =
            Color.WHITE.toDrawable()

        isClickable = true
        isFocusable = true

        setOnClickListener {
            action()
        }
    }

    private fun actionButton(
        label: String,
        a: String,
        b: String,
        action: () -> Unit
    ): TextView = TextView(this).apply {

        text = label
        gravity = Gravity.CENTER
        textSize = 11f
        includeFontPadding = false
        maxLines = 1
        typeface = Typeface.DEFAULT_BOLD

        setTextColor(Color.WHITE)

        background =
            GradientFactory.gradient(a, b)

        isClickable = true
        isFocusable = true

        setOnClickListener {
            action()
        }
    }

    private fun toggleFollow() {

        if (followBusy) return

        val target =
            !followingState

        // तुरंत UI बदलना
        followingState = target
        updateFollow(target)

        followBtn.isEnabled = false
        followBusy = true

        ChannelRepository.ensureSignedIn { connected ->

            if (!connected) {

                runOnUiThread {

                    followingState =
                        !target

                    updateFollow(
                        followingState
                    )

                    followBtn.isEnabled =
                        true

                    followBusy = false

                    Toast.makeText(
                        this,
                        "Follow के लिए connection नहीं हो पाया",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                return@ensureSignedIn
            }

            ChannelRepository.follow(
                target
            ) { ok ->

                runOnUiThread {

                    followBusy = false

                    followBtn.isEnabled =
                        true

                    if (ok) {

                        followerCount =
                            (
                                followerCount +
                                    if (target) {
                                        1L
                                    } else {
                                        -1L
                                    }
                            ).coerceAtLeast(0L)

                        followerCountView.text =
                            "👥 $followerCount Followers"

                        AnalyticsTracker.channelFollow(
                            this,
                            target
                        )

                        if (target) {

                            FirebaseMessaging
                                .getInstance()
                                .subscribeToTopic(
                                    "all_channel_followers"
                                )

                        } else {

                            FirebaseMessaging
                                .getInstance()
                                .unsubscribeFromTopic(
                                    "all_channel_followers"
                                )
                        }

                        Toast.makeText(
                            this,
                            if (target) {
                                "Channel Follow हो गया"
                            } else {
                                "Follow हटाया गया"
                            },
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        followingState =
                            !target

                        updateFollow(
                            followingState
                        )

                        Toast.makeText(
                            this,
                            "Follow अपडेट नहीं हुआ",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun updateFollow(
        value: Boolean
    ) {

        followingState = value

        followBtn.text =
            if (value) {
                "✓  Followed"
            } else {
                "➕  Follow करें"
            }

        if (value) {

            followBtn.setTextColor(
                Color.rgb(
                    107,
                    114,
                    128
                )
            )

            followBtn.background =
                android.graphics.drawable
                    .GradientDrawable()
                    .apply {

                        setColor(
                            Color.argb(
                                35,
                                107,
                                114,
                                128
                            )
                        )

                        cornerRadius =
                            26f

                        setStroke(
                            1,
                            Color.argb(
                                70,
                                107,
                                114,
                                128
                            )
                        )
                    }

        } else {

            followBtn.setTextColor(
                Color.WHITE
            )

            followBtn.background =
                GradientFactory.gradient(
                    "#16A34A",
                    "#06B6D4"
                )
        }
    }

    private fun renderPosts() {

        if (!::list.isInitialized) {
            return
        }

        list.removeAllViews()

        if (posts.isEmpty()) {

            list.addView(
                TextView(this).apply {

                    text =
                        "अभी चैनल में कोई पोस्ट नहीं है।"

                    textSize = 16f

                    setPadding(
                        16,
                        24,
                        16,
                        24
                    )
                }
            )

            return
        }

        posts.forEach { post ->

            list.addView(
                postView(post)
            )

            // हर पोस्ट के बीच नीला gap
            if (post != posts.last()) {

                list.addView(
                    View(this).apply {

                        setBackgroundColor(
                            Color.rgb(
                                30,
                                136,
                                229
                            )
                        )
                    },
                    LinearLayout.LayoutParams(
                        -1,
                        7
                    ).apply {

                        setMargins(
                            0,
                            0,
                            0,
                            0
                        )
                    }
                )
            }

            // एक session में एक post का view
            // केवल एक बार count होगा
            if (viewedSession.add(post.id)) {

                viewOverrides[post.id] =
                    (
                        viewOverrides[post.id]
                            ?: post.viewCount
                    ) + 1L

                AnalyticsTracker.uniquePostView(
                    this,
                    post.id
                )
            }
        }
    }

    private fun postView(
        p: ChannelPost
    ): View {

        val box =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                background =
                    GradientFactory.roundedWhite()

                setPadding(
                    14,
                    14,
                    14,
                    12
                )

                elevation = 3f
            }

        // Category + Date
        box.addView(
            TextView(this).apply {

                text =
                    "${p.category}  •  ${
                        if (p.createdAt > 0) {
                            fmt.format(
                                Date(
                                    p.createdAt
                                )
                            )
                        } else {
                            ""
                        }
                    }"

                textSize = 11f

                setTextColor(
                    Color.rgb(
                        14,
                        91,
                        215
                    )
                )
            }
        )

        // Title
        box.addView(
            TextView(this).apply {

                text = p.title

                textSize = 18f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.rgb(
                        25,
                        35,
                        50
                    )
                )

                setPadding(
                    0,
                    5,
                    0,
                    3
                )
            }
        )

        // Text-only post भी पूरी तरह allowed
        if (p.body.isNotBlank()) {

            box.addView(
                TextView(this).apply {

                    text = p.body

                    textSize = 14f

                    setTextColor(
                        Color.DKGRAY
                    )

                    setPadding(
                        0,
                        0,
                        0,
                        8
                    )
                }
            )
        }

        // Image
        if (p.imageUrl.isNotBlank()) {

            val imageBox =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.VERTICAL

                    setPadding(
                        0,
                        6,
                        0,
                        8
                    )
                }

            val image =
                ImageView(this).apply {

                    adjustViewBounds = true

                    scaleType =
                        ImageView.ScaleType.CENTER_INSIDE

                    setBackgroundColor(
                        Color.rgb(
                            241,
                            245,
                            249
                        )
                    )

                    minimumHeight = 180

                    setOnClickListener {

                        ChannelRepository.openMedia(
                            this@ChannelActivity,
                            p.imageUrl,
                            p.imageMime,
                            "image_${p.id}.jpg"
                        ) { ok, err ->

                            if (!ok) {

                                Toast.makeText(
                                    this@ChannelActivity,
                                    err
                                        ?: "Image नहीं खुली",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

            imageBox.addView(
                image,
                LinearLayout.LayoutParams(
                    -1,
                    340
                )
            )

            val hint =
                TextView(this).apply {

                    text =
                        "🖼️ फोटो पर क्लिक करके पूरा खोलें"

                    textSize = 13f

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
                        4,
                        0,
                        6
                    )

                    setOnClickListener {
                        image.performClick()
                    }
                }

            imageBox.addView(hint)

            box.addView(imageBox)

            ChannelRepository.loadImage(
                p.imageUrl
            ) { bitmap, _ ->

                runOnUiThread {

                    if (bitmap != null) {

                        image.setImageBitmap(
                            bitmap
                        )

                        image.scaleType =
                            ImageView.ScaleType.CENTER_CROP

                    } else {

                        hint.text =
                            "🖼️ फोटो खोलने के लिए यहाँ दबाएँ"
                    }
                }
            }
        }

        // PDF / Document
        if (p.fileUrl.isNotBlank()) {

            box.addView(
                TextView(this).apply {

                    text =
                        "📄 ${
                            p.fileName.ifBlank {
                                "PDF/Document"
                            }
                        }  •  OPEN"

                    textSize = 13f

                    typeface =
                        Typeface.DEFAULT_BOLD

                    setTextColor(
                        Color.rgb(
                            14,
                            91,
                            215
                        )
                    )

                    setPadding(
                        0,
                        7,
                        0,
                        7
                    )

                    setOnClickListener {

                        ChannelRepository.openMedia(
                            this@ChannelActivity,
                            p.fileUrl,
                            p.fileMime,
                            p.fileName.ifBlank {
                                "document.pdf"
                            }
                        ) { ok, err ->

                            if (!ok) {

                                Toast.makeText(
                                    this@ChannelActivity,
                                    err
                                        ?: "PDF नहीं खुली",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            )
        }

        val actions =
            LinearLayout(this).apply {

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    0,
                    7,
                    0,
                    0
                )
            }

        val currentLiked =
            likedState[p.id] ?: false

        val initialLikeCount =
            likeOverrides[p.id]
                ?: p.likeCount

        // Like button
        val like =
            TextView(this).apply {

                text =
                    if (currentLiked) {
                        "👍 Liked $initialLikeCount"
                    } else {
                        "👍 Like $initialLikeCount"
                    }

                textSize = 13f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    if (currentLiked) {
                        Color.WHITE
                    } else {
                        Color.rgb(
                            14,
                            91,
                            215
                        )
                    }
                )

                background =
                    if (currentLiked) {

                        GradientFactory.gradient(
                            "#2563EB",
                            "#38BDF8"
                        )

                    } else {

                        Color.WHITE.toDrawable()
                    }

                setPadding(
                    10,
                    7,
                    12,
                    7
                )

                setOnClickListener {

                    toggleLike(
                        p,
                        currentLiked
                    )
                }
            }

        // केवल एक बार Like add होगा
        actions.addView(
            like,
            LinearLayout.LayoutParams(
                0,
                42,
                1f
            )
        )

        val comments =
            TextView(this).apply {

                text =
                    "💬 ${p.commentCount}"

                textSize = 13f

                setPadding(
                    6,
                    7,
                    12,
                    7
                )

                setOnClickListener {
                    showComments(p)
                }
            }

        actions.addView(
            comments
        )

        val offline =
            TextView(this).apply {

                text =
                    if (
                        OfflineStore.isSaved(
                            this@ChannelActivity,
                            p.id
                        )
                    ) {
                        "✓ Offline"
                    } else {
                        "📥 Offline"
                    }

                textSize = 13f

                setTextColor(
                    Color.rgb(
                        14,
                        91,
                        215
                    )
                )

                setPadding(
                    6,
                    7,
                    12,
                    7
                )

                setOnClickListener {

                    if (
                        !OfflineStore.isSaved(
                            this@ChannelActivity,
                            p.id
                        )
                    ) {

                        text = "⏳ Saving…"

                        ChannelRepository.saveOffline(
                            this@ChannelActivity,
                            p
                        ) { ok, err ->

                            runOnUiThread {

                                text =
                                    if (ok) {

                                        AnalyticsTracker
                                            .offlineDownload(
                                                this@ChannelActivity,
                                                p.id
                                            )

                                        "✓ Offline Saved"

                                    } else {

                                        "📥 Offline"
                                    }

                                Toast.makeText(
                                    this@ChannelActivity,
                                    if (ok) {
                                        "आदेश Offline सेव हो गया"
                                    } else {
                                        "सेव नहीं हुआ: $err"
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                    } else {

                        openOffline(p)
                    }
                }
            }

        actions.addView(
            offline
        )

        val share =
            TextView(this).apply {

                text = "↗  SHARE"

                textSize = 14f

                gravity =
                    Gravity.CENTER

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.WHITE
                )

                background =
                    GradientFactory.gradient(
                        "#075985",
                        "#2563EB"
                    )

                setPadding(
                    16,
                    7,
                    16,
                    7
                )

                minWidth = 104
                minHeight = 46

                setOnClickListener {
                    sharePost(p)
                }
            }

        actions.addView(
            share
        )

        box.addView(
            actions
        )

        val shownLikes =
            likeOverrides[p.id]
                ?: p.likeCount

        val shownViews =
            viewOverrides[p.id]
                ?: p.viewCount

        val shownShares =
            shareOverrides[p.id]
                ?: p.shareCount

        box.addView(
            TextView(this).apply {

                text =
                    "👁 Views: $shownViews    " +
                    "↗ Shares: $shownShares    " +
                    "👍 Likes: $shownLikes    " +
                    "💬 Comments: ${p.commentCount}"

                textSize = 12f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.rgb(
                        71,
                        85,
                        105
                    )
                )

                setPadding(
                    8,
                    10,
                    8,
                    4
                )
            }
        )

        return box.apply {

            layoutParams =
                LinearLayout.LayoutParams(
                    -1,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {

                    setMargins(
                        0,
                        0,
                        0,
                        18
                    )
                }
        }
    }
        private fun toggleLike(
        p: ChannelPost,
        currentLiked: Boolean
    ) {

        ChannelRepository.ensureSignedIn { connected ->

            if (!connected) {

                runOnUiThread {

                    Toast.makeText(
                        this@ChannelActivity,
                        "Like के लिए sign-in नहीं हो पाया",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                return@ensureSignedIn
            }

            val target =
                !currentLiked

            // तुरंत UI update
            likedState[p.id] =
                target

            val base =
                likeOverrides[p.id]
                    ?: p.likeCount

            likeOverrides[p.id] =
                (
                    base +
                        if (target) {
                            1L
                        } else {
                            -1L
                        }
                ).coerceAtLeast(0L)

            runOnUiThread {
                renderPosts()
            }

            ChannelRepository.like(
                p.id,
                target
            ) { ok ->

                if (!ok) {

                    runOnUiThread {

                        likedState[p.id] =
                            currentLiked

                        likeOverrides[p.id] =
                            p.likeCount

                        renderPosts()

                        Toast.makeText(
                            this@ChannelActivity,
                            "Like अपडेट नहीं हुआ",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun sharePost(
        p: ChannelPost
    ) {

        // Share count तुरंत बढ़ाएँ
        shareOverrides[p.id] =
            (
                shareOverrides[p.id]
                    ?: p.shareCount
            ) + 1L

        renderPosts()

        AnalyticsTracker.uniquePostShare(
            this,
            p.id
        )

        val web =
            "https://shiksha-rojgar.web.app/channel" +
                "?post=${Uri.encode(p.id)}"

        val app =
            "shiksharojgar://channel/post/" +
                Uri.encode(p.id)

        val text =
            buildString {

                append(
                    "📢 शिक्षा रोजगार चैनल\n"
                )

                if (p.title.isNotBlank()) {
                    append(p.title)
                }

                if (p.body.isNotBlank()) {

                    append(
                        "\n\n${p.body}"
                    )
                }

                if (p.fileUrl.isNotBlank()) {

                    append(
                        "\n\n📄 PDF/Document: " +
                            p.fileUrl
                    )
                }

                append(
                    "\n\n🌐 Channel Link:\n$web"
                )

                append(
                    "\n📲 Direct App Link:\n$app"
                )
            }

        try {

            startActivity(
                Intent.createChooser(
                    Intent(
                        Intent.ACTION_SEND
                    ).apply {

                        type =
                            "text/plain"

                        putExtra(
                            Intent.EXTRA_TEXT,
                            text
                        )
                    },
                    "Share Channel Post"
                )
            )

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Share करने के लिए कोई app उपलब्ध नहीं है",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showComments(
        p: ChannelPost
    ) {

        if (
            !globalComments ||
            !p.commentsEnabled
        ) {

            Toast.makeText(
                this,
                "इस पोस्ट पर Comments बंद हैं",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val layout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    8,
                    4,
                    8,
                    4
                )
            }

        val input =
            EditText(this).apply {

                hint =
                    "अपनी टिप्पणी लिखें…"

                minLines = 2

                gravity =
                    Gravity.TOP

                setPadding(
                    10,
                    10,
                    10,
                    10
                )
            }

        val listBox =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL
            }

        layout.addView(
            listBox,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        layout.addView(
            input,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "💬 Comments"
                )
                .setView(layout)
                .setPositiveButton(
                    "Comment",
                    null
                )
                .setNegativeButton(
                    "Close",
                    null
                )
                .create()

        ChannelRepository.comments(
            p.id
        ) { comments ->

            runOnUiThread {

                listBox.removeAllViews()

                if (comments.isEmpty()) {

                    listBox.addView(
                        TextView(this).apply {

                            text =
                                "अभी कोई Comment नहीं है।"

                            textSize = 13f

                            setTextColor(
                                Color.GRAY
                            )

                            setPadding(
                                4,
                                10,
                                4,
                                10
                            )
                        }
                    )

                } else {

                    comments.forEach { comment ->

                        listBox.addView(
                            TextView(this).apply {

                                text =
                                    "${comment["name"] ?: "उपयोगकर्ता"}: " +
                                        "${comment["text"] ?: ""}"

                                textSize = 13f

                                setTextColor(
                                    Color.rgb(
                                        40,
                                        50,
                                        60
                                    )
                                )

                                setPadding(
                                    8,
                                    8,
                                    8,
                                    8
                                )

                                background =
                                    Color.rgb(
                                        245,
                                        247,
                                        250
                                    ).toDrawable()
                            }
                        )
                    }
                }
            }
        }

        dialog.setOnShowListener {

            dialog
                .getButton(
                    AlertDialog.BUTTON_POSITIVE
                )
                .setOnClickListener {

                    val commentText =
                        input.text
                            .toString()
                            .trim()

                    if (
                        commentText.isEmpty()
                    ) {

                        Toast.makeText(
                            this,
                            "पहले Comment लिखें",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@setOnClickListener
                    }

                    startPhoneVerificationForComment(
                        p,
                        commentText,
                        input,
                        dialog
                    )
                }
        }

        dialog.show()
    }

    private fun startPhoneVerificationForComment(
        p: ChannelPost,
        text: String,
        input: EditText,
        dialog: AlertDialog
    ) {

        val phoneInput =
            EditText(this).apply {

                hint =
                    "+91XXXXXXXXXX"

                inputType =
                    android.text.InputType
                        .TYPE_CLASS_PHONE

                setPadding(
                    12,
                    10,
                    12,
                    10
                )
            }

        AlertDialog.Builder(this)
            .setTitle(
                "📱 Mobile Verification"
            )
            .setMessage(
                "Comment करने के लिए अपना मोबाइल नंबर दर्ज करें।"
            )
            .setView(phoneInput)
            .setPositiveButton(
                "OTP भेजें"
            ) { _, _ ->

                val phone =
                    phoneInput.text
                        .toString()
                        .trim()

                if (
                    phone.isEmpty()
                ) {

                    Toast.makeText(
                        this,
                        "मोबाइल नंबर दर्ज करें",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }

                val normalizedPhone =
                    when {

                        phone.startsWith(
                            "+91"
                        ) -> phone

                        phone.startsWith(
                            "91"
                        ) &&
                            phone.length >= 12 ->
                            "+$phone"

                        else ->
                            "+91$phone"
                    }

                sendCommentOtp(
                    p,
                    text,
                    input,
                    dialog,
                    normalizedPhone
                )
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    private fun sendCommentOtp(
        p: ChannelPost,
        text: String,
        input: EditText,
        dialog: AlertDialog,
        phone: String
    ) {

        val options =
            com.google.firebase.auth.PhoneAuthOptions
    .newBuilder(auth)
                .setPhoneNumber(
                    phone
                )
                .setTimeout(
                    60L,
                    java.util.concurrent
                        .TimeUnit.SECONDS
                )
                .setActivity(this)
                .setCallbacks(
                    object :
                        PhoneAuthProvider
                            .OnVerificationStateChangedCallbacks() {

                        override fun onVerificationCompleted(
                            credential:
                                PhoneAuthCredential
                        ) {

                            auth
                                .signInWithCredential(
                                    credential
                                )
                                .addOnSuccessListener {

                                    postVerifiedComment(
                                        p,
                                        text,
                                        input,
                                        dialog
                                    )
                                }
                        }

                        override fun onVerificationFailed(
                            e: FirebaseException
                        ) {

                            Toast.makeText(
                                this@ChannelActivity,
                                "OTP नहीं भेजा जा सका: " +
                                    (
                                        e.message
                                            ?: "Unknown error"
                                    ),
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        override fun onCodeSent(
                            verificationId: String,
                            token:
                                PhoneAuthProvider
                                    .ForceResendingToken
                        ) {

                            phoneVerificationId =
                                verificationId

                            showOtpDialog(
                                p,
                                text,
                                input,
                                dialog
                            )
                        }
                    }
                )    
.build()

        PhoneAuthProvider
            .verifyPhoneNumber(
                options
            )
    }

    private fun showOtpDialog(
        p: ChannelPost,
        text: String,
        input: EditText,
        dialog: AlertDialog
    ) {

        val otpInput =
            EditText(this).apply {

                hint =
                    "6 अंकों का OTP"

                inputType =
                    android.text.InputType
                        .TYPE_CLASS_NUMBER

                filters =
                    arrayOf(
                        android.text.InputFilter
                            .LengthFilter(6)
                    )

                setPadding(
                    12,
                    10,
                    12,
                    10
                )
            }

        AlertDialog.Builder(this)
            .setTitle(
                "🔐 OTP दर्ज करें"
            )
            .setMessage(
                "आपके मोबाइल नंबर पर भेजा गया OTP दर्ज करें।"
            )
            .setView(otpInput)
            .setPositiveButton(
                "Verify"
            ) { _, _ ->

                val otp =
                    otpInput.text
                        .toString()
                        .trim()

                if (
                    otp.length != 6
                ) {

                    Toast.makeText(
                        this,
                        "6 अंकों का OTP दर्ज करें",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }

                val verificationId =
                    phoneVerificationId

                if (
                    verificationId
                        .isNullOrBlank()
                ) {

                    Toast.makeText(
                        this,
                        "OTP session समाप्त हो गया। दोबारा OTP भेजें।",
                        Toast.LENGTH_LONG
                    ).show()

                    return@setPositiveButton
                }

                val credential =
                    PhoneAuthProvider
                        .getCredential(
                            verificationId,
                            otp
                        )

                auth
                    .signInWithCredential(
                        credential
                    )
                    .addOnCompleteListener { task ->

                        if (
                            task.isSuccessful
                        ) {

                            postVerifiedComment(
                                p,
                                text,
                                input,
                                dialog
                            )

                        } else {

                            Toast.makeText(
                                this,
                                task.exception
                                    ?.message
                                    ?: "OTP गलत है",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    private fun postVerifiedComment(
        p: ChannelPost,
        text: String,
        input: EditText,
        dialog: AlertDialog
    ) {

        ChannelRepository.addComment(
            p.id,
            text
        ) { ok ->

            runOnUiThread {

                if (ok) {

                    input.setText("")

                    Toast.makeText(
                        this,
                        "✅ Comment पोस्ट हो गया",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()

                } else {

                    Toast.makeText(
                        this,
                        "Comment पोस्ट नहीं हुआ",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
        private fun showOfflineList() {

        val saved =
            OfflineStore.all(this)

        if (saved.isEmpty()) {

            AlertDialog.Builder(this)
                .setTitle(
                    "📥 Offline सामग्री"
                )
                .setMessage(
                    "अभी कोई सामग्री Offline सेव नहीं है।\n" +
                        "किसी पोस्ट पर 📥 Offline दबाकर सेव करें।"
                )
                .setPositiveButton(
                    "OK",
                    null
                )
                .show()

            return
        }

        val labels =
            saved.map { item ->

                "📄 ${item.second}"

            }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(
                "📥 Offline Downloads"
            )
            .setItems(
                labels
            ) { _, which ->

                val id =
                    saved[which].first

                val savedTitle =
                    saved[which].second

                val p =
                    posts.firstOrNull {
                        it.id == id
                    }
                        ?: ChannelPost(
                            id = id,
                            title = savedTitle
                        )

                openOffline(p)
            }
            .setNegativeButton(
                "Close",
                null
            )
            .show()
    }

    private fun shareChannel() {

        AnalyticsTracker.channelShare(
            this
        )

        val text =
            "📢 शिक्षा रोजगार चैनल\n\n" +
                "सरकारी आदेश, निर्देश, टाइम टेबल, " +
                "शिक्षा एवं रोजगार अपडेट\n\n" +
                "Shiksha Rojgar App\n\n" +
                "🌐 Channel Link (App/Browser):\n" +
                "https://shiksha-rojgar.web.app/channel\n\n" +
                "📲 Direct App Link:\n" +
                "shiksharojgar://channel\n\n" +
                "🌐 Website:\n" +
                "https://www.shiksharojgar.com/"

        try {

            startActivity(
                Intent.createChooser(
                    Intent(
                        Intent.ACTION_SEND
                    ).apply {

                        type =
                            "text/plain"

                        putExtra(
                            Intent.EXTRA_TEXT,
                            text
                        )
                    },
                    "Share Channel"
                )
            )

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Share करने के लिए कोई app उपलब्ध नहीं है",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

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

    private fun open(
        url: String
    ) {

        try {

            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )
            )

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Link खोलने के लिए browser उपलब्ध नहीं है",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openOffline(
        p: ChannelPost
    ) {

        val folder =
            ChannelRepository.openOffline(
                this,
                p
            )

        if (folder == null) {

            Toast.makeText(
                this,
                "Offline फाइल नहीं मिली",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val box =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    10,
                    10,
                    10,
                    10
                )
            }

        val scroll =
            ScrollView(this)

        val content =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL
            }

        scroll.addView(
            content
        )

        val textFile =
            java.io.File(
                folder,
                "post.txt"
            )

        val text =
            if (textFile.exists()) {

                try {
                    textFile.readText()
                } catch (_: Exception) {
                    p.title + "\n\n" + p.body
                }

            } else {

                p.title + "\n\n" + p.body
            }

        content.addView(
            TextView(this).apply {

                this.text =
                    text

                textSize = 15f

                setTextColor(
                    Color.rgb(
                        30,
                        40,
                        50
                    )
                )

                setPadding(
                    4,
                    4,
                    4,
                    12
                )
            }
        )

        val imageFile =
            java.io.File(
                folder,
                "image.jpg"
            )

        if (imageFile.exists()) {

            val bitmap =
                android.graphics.BitmapFactory
                    .decodeFile(
                        imageFile.absolutePath
                    )

            if (bitmap != null) {

                content.addView(
                    ImageView(this).apply {

                        setImageBitmap(
                            bitmap
                        )

                        adjustViewBounds =
                            true

                        scaleType =
                            ImageView.ScaleType
                                .FIT_CENTER

                        setPadding(
                            0,
                            10,
                            0,
                            10
                        )
                    },
                    LinearLayout.LayoutParams(
                        -1,
                        -2
                    )
                )
            }
        }

        val pdf =
            folder
                .listFiles()
                ?.firstOrNull { file ->

                    file.name
                        .lowercase(
                            Locale.US
                        )
                        .endsWith(".pdf")
                }

        if (pdf != null) {

            content.addView(
                Button(this).apply {

                    this.text =
    "📄 PDF खोलें"
                    setOnClickListener {

                        try {

                            val uri =
                                androidx.core.content
                                    .FileProvider
                                    .getUriForFile(
                                        this@ChannelActivity,
                                        "com.shiksharojgar.app.fileprovider",
                                        pdf
                                    )

                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW
                                ).apply {

                                    setDataAndType(
                                        uri,
                                        "application/pdf"
                                    )

                                    addFlags(
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    )

                                    addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK
                                    )
                                }
                            )

                        } catch (_: Exception) {

                            Toast.makeText(
                                this@ChannelActivity,
                                "PDF खोलने के लिए PDF viewer चाहिए",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            )
        }

        box.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        AlertDialog.Builder(this)
            .setTitle(
                "📥 Offline आदेश"
            )
            .setView(box)
            .setPositiveButton(
                "OK",
                null
            )
            .show()
    }
        private fun showPostShareResult(
        post: ChannelPost,
        success: Boolean
    ) {

        if (!success) {

            Toast.makeText(
                this,
                "Share count update नहीं हुआ",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        Toast.makeText(
            this,
            "Post share हो गई",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun refreshFollowState() {

        ChannelRepository.ensureSignedIn { connected ->

            if (!connected) {

                runOnUiThread {
                    updateFollow(false)
                }

                return@ensureSignedIn
            }

            ChannelRepository.isFollowing { following ->

                runOnUiThread {
                    updateFollow(following)
                }

                if (following) {

                    FirebaseMessaging
                        .getInstance()
                        .subscribeToTopic(
                            "all_channel_followers"
                        )

                } else {

                    FirebaseMessaging
                        .getInstance()
                        .unsubscribeFromTopic(
                            "all_channel_followers"
                        )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (::followBtn.isInitialized) {
            refreshFollowState()
        }

        getSharedPreferences(
            "sr_notifications",
            MODE_PRIVATE
        )
            .edit()
            .putBoolean(
                "channel_open",
                true
            )
            .putInt(
                "channel_unread",
                0
            )
            .putLong(
                "channel_last_seen_at",
                System.currentTimeMillis()
            )
            .apply()
    }

    override fun onPause() {

        getSharedPreferences(
            "sr_notifications",
            MODE_PRIVATE
        )
            .edit()
            .putBoolean(
                "channel_open",
                false
            )
            .apply()

        super.onPause()
    }

    private fun showSimpleMessage(
        title: String,
        message: String
    ) {

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(
                "OK",
                null
            )
            .show()
    }

    private fun safeOpenUrl(
        url: String
    ) {

        try {

            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )

            startActivity(intent)

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Link खोलने में समस्या हुई",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
        private fun Int.toDrawable():
        android.graphics.drawable.ColorDrawable =
    android.graphics.drawable.ColorDrawable(this)

object GradientFactory {

    fun gradient(
        a: String,
        b: String
    ): android.graphics.drawable.GradientDrawable {

        return android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.parseColor(a),
                Color.parseColor(b)
            )
        ).apply {

            cornerRadius = 26f
        }
    }

    fun rounded(
        a: String
    ): android.graphics.drawable.GradientDrawable {

        return android.graphics.drawable.GradientDrawable()
            .apply {

                setColor(
                    Color.parseColor(a)
                )

                cornerRadius = 26f
            }
    }

    fun roundedWhite():
            android.graphics.drawable.GradientDrawable {

        return android.graphics.drawable.GradientDrawable()
            .apply {

                setColor(Color.WHITE)

                cornerRadius = 20f
            }
    }
}
