package com.shiksharojgar.app

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.util.Linkify
import android.text.method.LinkMovementMethod
import android.util.Patterns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ChannelActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    private var phoneVerificationId: String? = null

    private lateinit var list: LinearLayout
    private lateinit var followBtn: TextView
    private lateinit var followerCountView: TextView
    private lateinit var commentStatusView: TextView
    private lateinit var feedScroll: ScrollView
    private lateinit var channelStrip: LinearLayout

    private lateinit var latestPostButton: TextView

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
private var unreadCountAtOpen = 0
    
    private var followingState = false

    private var pendingPostId: String? = null

    private val fmt =
        SimpleDateFormat(
            "dd MMM yyyy, hh:mm a",
            Locale("hi", "IN")
        )

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
private fun samePostContent(
    a: ChannelPost,
    b: ChannelPost
): Boolean {

    return a.id == b.id &&
        a.title == b.title &&
        a.body == b.body &&
        a.imageUrl == b.imageUrl &&
        a.fileUrl == b.fileUrl &&
        a.fileName == b.fileName &&
        a.imageMime == b.imageMime &&
        a.fileMime == b.fileMime &&
        a.category == b.category &&
        a.createdAt == b.createdAt &&
        a.commentsEnabled == b.commentsEnabled
}
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
unreadCountAtOpen =
    getSharedPreferences(
        "sr_notifications",
        MODE_PRIVATE
    ).getInt(
        "channel_unread",
        0
    )
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

                if (::followerCountView.isInitialized) {
                    followerCountView.text =
                        "👥 $followerCount"
                }


                if (::commentStatusView.isInitialized) {
                    commentStatusView.text =
                        if (comments) {
                            "💬"
                        } else {
                            "🔒"
                        }
                }

            }
        }

        ChannelRepository.posts(
    { p ->

        val oldPosts =
            posts.toList()

        val structureChanged =
            oldPosts.size != p.size ||
                oldPosts.indices.any { index ->
                    !samePostContent(
                        oldPosts[index],
                        p[index]
                    )
                }

        val oldNewest =
            posts.lastOrNull()?.id

        posts.clear()
        posts.addAll(p)

        runOnUiThread {

            /*
             * केवल Like / Comment / View / Share count बदला है।
             * पूरा feed दोबारा मत बनाओ।
             */
            if (
                !structureChanged &&
                !firstFeedRender
            ) {
                return@runOnUiThread
            }

            val newNewest =
                posts.lastOrNull()?.id

            val shouldGoToNewest =
                firstFeedRender ||
                    oldNewest != newNewest

            renderPosts(
                preservePosition =
                    !shouldGoToNewest
            )

            if (shouldGoToNewest) {

    feedScroll.post {

        feedScroll.fullScroll(
            View.FOCUS_DOWN
        
        )
    }
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

                    updateTopicSubscription(following)
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

    private fun updateTopicSubscription(
        following: Boolean
    ) {

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

    private fun scrollToPost(
        index: Int
    ) {

        if (
            !::feedScroll.isInitialized ||
            !::list.isInitialized
        ) {
            return
        }

        if (
            index < 0 ||
            index >= posts.size
        ) {
            return
        }

        val postId =
            posts[index].id

        /*
         * Every post has a blue divider after it.
         * Therefore child index != post index.
         *
         * We use the post ID tag instead.
         */
        var target: View? = null

        for (i in 0 until list.childCount) {

            val child =
                list.getChildAt(i)

            if (child.tag == postId) {
                target = child
                break
            }
        }

        target?.let { child ->

            feedScroll.post {

                feedScroll.smoothScrollTo(
                    0,
                    child.top
                )
                updateLatestPostButtonVisibility()
            }
        }
    }
private fun createLatestPostButton(): TextView {

    return TextView(this).apply {

    latestPostButton = this

        text = "˅\n˅"

textSize = 15f

setLineSpacing(
    -2f,
    0.85f
)

        gravity = Gravity.CENTER

        typeface = Typeface.DEFAULT_BOLD

        setTextColor(Color.WHITE)

        background =
    android.graphics.drawable.GradientDrawable().apply {

        shape =
            android.graphics.drawable.GradientDrawable.OVAL

        setColor(
            Color.parseColor("#0EA5E9")
        )

        setStroke(
            dp(1),
            Color.parseColor("#7DD3FC")
        )
    }

        elevation = dp(8).toFloat()

        isClickable = true
        isFocusable = true

        setPadding(
            dp(6),
            dp(2),
            dp(6),
            dp(2)
        )

        setOnClickListener {

            scrollToNewest()
        }
    }
}
private fun updateLatestPostButtonVisibility() {

    if (
        !::feedScroll.isInitialized ||
        !::latestPostButton.isInitialized
    ) {
        return
    }

    val child =
        feedScroll.getChildAt(0)

    if (child == null) {
        return
    }

    val atBottom =
        feedScroll.scrollY >=
            child.bottom -
                feedScroll.height -
                dp(8)

    latestPostButton.visibility =
        if (atBottom) {
            View.GONE
        } else {
            View.VISIBLE
        }
}
    private fun scrollToNewest() {

    if (!::feedScroll.isInitialized) {
        return
    }

    feedScroll.post {

        val child =
            feedScroll.getChildAt(0)

        if (child != null) {

            feedScroll.scrollTo(
                0,
                child.bottom
            )
        }
    }
}

    private fun toolButton(
        label: String,
        action: () -> Unit
    ): TextView = TextView(this).apply {

        text = label

        gravity =
            Gravity.CENTER

        textSize = 11f

        typeface =
            Typeface.DEFAULT_BOLD

        setTextColor(
            Color.WHITE
        )

        background =
            GradientFactory.rounded(
                "#174E86"
            )

        isClickable = true
        isFocusable = true

        setOnClickListener {
            action()
        }
    }

    /*
     * CHANNEL UI
     *
     * TOP:
     * Old blue Channel name bar
     * Channel name + follower count + Share
     *
     * MIDDLE:
     * Scrollable feed
     *
     * BOTTOM:
     * Follow + tiny Comments icon
     * Home + Share + Back
     */
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

        /*
         * OLD BLUE CHANNEL HEADER
         */
        val toolbar =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    dp(6),
                    dp(6),
                    dp(6),
                    dp(6)
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
                top + dp(10),
                view.paddingRight,
                dp(6)
            )

            insets
        }

        val back =
            toolButton(
                "‹"
            ) {
                onBackPressedDispatcher
                    .onBackPressed()
            }

        toolbar.addView(
            back,
            LinearLayout.LayoutParams(
                dp(48),
                dp(48)
            )
        )

        /*
         * TITLE + FOLLOWER COUNT
         */
        val titleBox =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    dp(8),
                    0,
                    dp(2),
                    0
                )
            }

        val title =
            TextView(this).apply {

                text =
                    "📢 Shiksha Rojgar Channel"

                textSize = 16f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.WHITE
                )

                gravity =
                    Gravity.CENTER_VERTICAL

                maxLines = 1

                ellipsize =
                    android.text.TextUtils.TruncateAt.END
            }

        titleBox.addView(
            title,
            LinearLayout.LayoutParams(
                -1,
                dp(30)
            )
        )

        followerCountView =
            TextView(this).apply {

                text =
                    "👥 $followerCount"

                textSize = 11f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.WHITE
                )

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        titleBox.addView(
            followerCountView,
            LinearLayout.LayoutParams(
                -1,
                dp(20)
            )
        )

        toolbar.addView(
            titleBox,
            LinearLayout.LayoutParams(
                0,
                dp(50),
                1f
            )
        )

        val channelShare =
            toolButton(
                "↗\nShare"
            ) {
                shareChannel()
            }

        toolbar.addView(
            channelShare,
            LinearLayout.LayoutParams(
                dp(58),
                dp(48)
            )
        )

        root.addView(
            toolbar,
            LinearLayout.LayoutParams(
                -1,
                dp(66)
            )
        )
/*
 * CHANNEL HOME-STYLE STRIP
 * Same design as Home Channel button
 */
 channelStrip =
    LinearLayout(this).apply {

        orientation =
            LinearLayout.HORIZONTAL

        gravity =
            Gravity.CENTER_VERTICAL

        setPadding(
            dp(16),
            dp(8),
            dp(12),
            dp(8)
        )

        background =
            ContextCompat.getDrawable(
                this@ChannelActivity,
                R.drawable.channel_card_bg
            )

        elevation =
            dp(8).toFloat()

        val left =
            LinearLayout(
                this@ChannelActivity
            ).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER_VERTICAL

                val title =
                    TextView(
                        this@ChannelActivity
                    ).apply {

                        text =
                            "📢  शिक्षा रोजगार चैनल"

                        textSize =
                            17f

                        typeface =
                            Typeface.DEFAULT_BOLD

                        setTextColor(
                            Color.WHITE
                        )
                    }

                addView(title)

                followerCountView =
                    TextView(
                        this@ChannelActivity
                    ).apply {

                        text =
                            "👥 Followers: $followerCount"

                        textSize =
                            14f

                        typeface =
                            Typeface.DEFAULT_BOLD

                        setTextColor(
                            Color.WHITE
                        )
                    }

                addView(
                    followerCountView
                )

                val tagline =
                    TextView(
                        this@ChannelActivity
                    ).apply {

                        text =
                            "• शासकीय आदेश • निर्देश • शिक्षा • शिक्षक • विद्यार्थी • नौकरी • परीक्षा • रिजल्ट • महत्वपूर्ण अपडेट के लिए follow करें!"

                        textSize =
                            10.5f

                        setTextColor(
                            Color.WHITE
                        )

                        setPadding(
                            0,
                            dp(2),
                            0,
                            0
                        )
                    }

                addView(tagline)
            }

        addView(
            left,
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )

        val appName =
            TextView(
                this@ChannelActivity
            ).apply {

                text =
                    "शिक्षा रोजगार ऐप"

                textSize =
                    11f

                typeface =
                    Typeface.DEFAULT_BOLD

                gravity =
                    Gravity.CENTER

                setTextColor(
                    Color.WHITE
                )

                setPadding(
                    dp(6),
                    0,
                    0,
                    0
                )
            }

        addView(
            appName,
            LinearLayout.LayoutParams(
                dp(70),
                -2
            )
        )
    }

root.addView(
    channelStrip,
    LinearLayout.LayoutParams(
        -1,
        dp(100)
    ).apply {
        setMargins(
            dp(10),
            dp(12),
            dp(10),
            dp(4)
        )
    }
)
val latestPostButton =
    createLatestPostButton()

val latestPostButtonBox =
    FrameLayout(this).apply {

        addView(
            latestPostButton,
            FrameLayout.LayoutParams(
                dp(42),
                dp(42)
            ).apply {

                gravity =
                    Gravity.END or
                        Gravity.BOTTOM

                setMargins(
                    0,
                    0,
                    dp(14),
                    dp(14)
                )
            }
        )
    }
        /*
         * FEED
         */
        feedScroll =
    ScrollView(this).apply {

        isFillViewport =
            true

        clipToPadding =
            false

        isFocusable =
            false

        isFocusableInTouchMode =
            false
        isSaveEnabled =
            false
        
        descendantFocusability =
    ViewGroup.FOCUS_BLOCK_DESCENDANTS

        setPadding(
            0,
            0,
            0,
            0
        )
    }
    feedScroll.setOnScrollChangeListener {
        _, _, _, _, _ ->

        updateLatestPostButtonVisibility()
    }
        list =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(10),
                    dp(10),
                    dp(10),
                    dp(10)
                )
            }

        feedScroll.addView(
            list,
            FrameLayout.LayoutParams(
                -1,
                -2
            )
        )

        val feedContainer =
    FrameLayout(this).apply {

        addView(
            feedScroll,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        addView(
            latestPostButtonBox,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )
    }

root.addView(
    feedContainer,
    LinearLayout.LayoutParams(
        -1,
        0,
        1f
    )
)
feedScroll.post {
    updateLatestPostButtonVisibility()
}
        /*
         * FOLLOW + COMMENTS
         *
         * Follow occupies most of the row.
         * Comment icon is deliberately tiny.
         */
        val followCommentBar =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    dp(8),
                    dp(4),
                    dp(8),
                    dp(4)
                )

                background =
                    Color.WHITE.toDrawable()

                elevation = 2f
            }

        followBtn =
            TextView(this).apply {

                text =
                    "➕ Follow करें"

                gravity =
                    Gravity.CENTER

                textSize = 13f

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
                    dp(10),
                    dp(5),
                    dp(10),
                    dp(5)
                )

                setOnClickListener {
                    toggleFollow()
                }
            }

        followCommentBar.addView(
            followBtn,
            LinearLayout.LayoutParams(
                0,
                dp(38),
                1f
            )
        )

        /*
         * ONLY ICON.
         * ON = 💬
         * OFF = 🔒
         */
        commentStatusView =
            TextView(this).apply {

                text =
                    if (globalComments) {
                        "💬"
                    } else {
                        "🔒"
                    }

                gravity =
                    Gravity.CENTER

                textSize = 16f

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
                    dp(4),
                    dp(2),
                    dp(4),
                    dp(2)
                )

                contentDescription =
                    if (globalComments) {
                        "Comments ON"
                    } else {
                        "Comments OFF"
                    }
            }

        followCommentBar.addView(
            commentStatusView,
            LinearLayout.LayoutParams(
                dp(42),
                dp(38)
            )
        )

        root.addView(
            followCommentBar,
            LinearLayout.LayoutParams(
                -1,
                dp(46)
            )
        )

        /*
         * HOME / SHARE / BACK
         */
        val bottom =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER

                setPadding(
                    dp(6),
                    dp(4),
                    dp(6),
                    dp(4)
                )

                background =
                    Color.WHITE.toDrawable()

                elevation = 3f
            }

        bottom.addView(
            bottomButton(
                "⌂  Home"
            ) {
                goHome()
            },
            LinearLayout.LayoutParams(
                0,
                dp(44),
                1f
            )
        )

        bottom.addView(
            bottomButton(
                "↗  Share"
            ) {
                shareChannel()
            },
            LinearLayout.LayoutParams(
                0,
                dp(44),
                1f
            ).apply {

                setMargins(
                    dp(6),
                    0,
                    0,
                    0
                )
            }
        )

        bottom.addView(
            bottomButton(
                "‹  Back"
            ) {
                onBackPressedDispatcher
                    .onBackPressed()
            },
            LinearLayout.LayoutParams(
                0,
                dp(44),
                1f
            ).apply {

                setMargins(
                    dp(6),
                    0,
                    0,
                    0
                )
            }
        )

        ViewCompat.setOnApplyWindowInsetsListener(
            bottom
        ) { view, insets ->

            val bottomInset =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                ).bottom

            view.setPadding(
                view.paddingLeft,
                dp(4),
                view.paddingRight,
                dp(4) + bottomInset
            )

            insets
        }

        root.addView(
            bottom,
            LinearLayout.LayoutParams(
                -1,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        return root
    }

    private fun bottomButton(
        label: String,
        action: () -> Unit
    ): TextView = TextView(this).apply {

        text = label

        gravity =
            Gravity.CENTER

        textSize = 14f

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

        gravity =
            Gravity.CENTER

        textSize = 11f

        includeFontPadding =
            false

        maxLines = 1

        typeface =
            Typeface.DEFAULT_BOLD

        setTextColor(
            Color.WHITE
        )

        background =
            GradientFactory.gradient(
                a,
                b
            )

        isClickable = true
        isFocusable = true

        setOnClickListener {
            action()
        }
    }

    /*
     * FOLLOW
     */
    private fun toggleFollow() {

        if (followBusy) {
            return
        }

        val target =
            !followingState

        /*
         * Instant UI.
         */
        followingState =
            target

        updateFollow(target)

        followBtn.isEnabled =
            false

        followBusy =
            true

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

                    followBusy =
                        false

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

                    followBusy =
                        false

                    followBtn.isEnabled =
                        true

                    if (ok) {

                        /*
                         * Count is changed exactly once.
                         */
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
                            "👥 $followerCount"

                        AnalyticsTracker.channelFollow(
                            this,
                            target
                        )

                        updateTopicSubscription(
                            target
                        )

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

                        /*
                         * Roll back only if Firestore failed.
                         */
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

    /*
     * FOLLOWED = WHITE/LIGHT BUTTON
     * NOT gray translucent.
     */
    private fun updateFollow(
        value: Boolean
    ) {

        followingState =
            value

        if (!::followBtn.isInitialized) {
            return
        }

        followBtn.text =
            if (value) {
                "✓  Followed"
            } else {
                "➕ Follow करें"
            }

        if (value) {

            followBtn.setTextColor(
                Color.rgb(
                    14,
                    91,
                    215
                )
            )

            followBtn.background =
                android.graphics.drawable
                    .GradientDrawable()
                    .apply {

                        setColor(
                            Color.WHITE
                        )

                        cornerRadius =
                            dp(26).toFloat()

                        setStroke(
                            dp(1),
                            Color.rgb(
                                14,
                                91,
                                215
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

        /*
     * RENDER FEED
     *
     * IMPORTANT:
     * Rebuilding the post views must NOT move the user
     * away from the post they are currently reading.
     */
    private fun renderPosts(
    preservePosition: Boolean = true
) {

    if (!::list.isInitialized) {
        return
    }
    val unreadStartIndex =
        if (unreadCountAtOpen > 0) {
            (posts.size - unreadCountAtOpen)
                .coerceAtLeast(0)
        } else {
            -1
        }
    var oldScrollY = 0

if (
    preservePosition &&
    ::feedScroll.isInitialized
) {
    oldScrollY =
        feedScroll.scrollY
}

    /*
     * Feed rebuild करें।
     */
    list.removeAllViews()

    if (posts.isEmpty()) {

        list.addView(
            TextView(this).apply {

                text =
                    "अभी चैनल में कोई पोस्ट नहीं है।"

                textSize = 16f

                gravity =
                    Gravity.CENTER

                setPadding(
                    dp(16),
                    dp(24),
                    dp(16),
                    dp(24)
                )
            }
        )

        return
    }

    /*
     * OLD -> NEW
     *
     * नई post हमेशा नीचे रहेगी।
     */
    posts.forEachIndexed { index, post ->

        val postView =
            postView(post)

        /*
         * Post ID को tag में रखें।
         */
        postView.tag =
            post.id

        list.addView(
            postView
        )

        /*
         * Posts के बीच blue divider।
         */
        if (index < posts.lastIndex) {

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
                    dp(7)
                )
            )
        }

        /*
         * एक session में एक ही बार view count।
         */
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

    /*
     * पुरानी visible post को फिर से उसी जगह पर रखें।
     *
     * इससे:
     * Like -> jump नहीं
     * Share -> jump नहीं
     * Comment -> jump नहीं
     * Firebase refresh -> jump नहीं
     */
if (
    preservePosition &&
    ::feedScroll.isInitialized
) {

    feedScroll.post {

        feedScroll.requestLayout()

        feedScroll.post {

            feedScroll.scrollTo(
                0,
                oldScrollY.coerceAtLeast(0)
            )
        }
    }
}
    }
            
        /*
     * SINGLE POST CARD
     *
     * FINAL ACTION ROW:
     *
     * 👍 Like 12
     * ↗ Share 5
     * 💬 Comment 3
     * 👁️ 20
     *
     * No duplicate stats row.
     * No second action row.
     */
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
                    dp(14),
                    dp(14),
                    dp(14),
                    dp(12)
                )

                elevation = 3f
            }

        /*
         * CATEGORY + DATE
         */
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

        /*
         * TITLE
         */
        box.addView(
            TextView(this).apply {

                text =
                    p.title

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
                    dp(5),
                    0,
                    dp(3)
                )
            }
        )

        /*
         * TEXT POST
         *
         * Links inside text remain clickable.
         */
        if (p.body.isNotBlank()) {

            val bodyView =
                TextView(this).apply {

                    text =
                        p.body

                    textSize = 14f

                    setTextColor(
                        Color.DKGRAY
                    )

                    setPadding(
                        0,
                        0,
                        0,
                        dp(8)
                    )

                    autoLinkMask =
                        Linkify.WEB_URLS

                    movementMethod =
                        LinkMovementMethod
                            .getInstance()

                    linksClickable =
                        true

                    setOnClickListener {
                        openFirstUrlFromText(
                            p.body
                        )
                    }
                }

            Linkify.addLinks(
                bodyView,
                Linkify.WEB_URLS
            )

            box.addView(
                bodyView
            )

            /*
             * VIDEO URL
             */
            val firstUrl =
                findFirstUrl(
                    p.body
                )

            if (
                firstUrl != null &&
                isVideoUrl(firstUrl)
            ) {

                box.addView(
                    TextView(this).apply {

                        text =
                            "▶  Video खोलें"

                        gravity =
                            Gravity.CENTER

                        textSize = 13f

                        typeface =
                            Typeface.DEFAULT_BOLD

                        setTextColor(
                            Color.WHITE
                        )

                        background =
                            GradientFactory.gradient(
                                "#DC2626",
                                "#F97316"
                            )

                        setPadding(
                            dp(12),
                            dp(8),
                            dp(12),
                            dp(8)
                        )

                        setOnClickListener {
                            safeOpenUrl(
                                firstUrl
                            )
                        }
                    },
                    LinearLayout.LayoutParams(
                        -1,
                        dp(42)
                    ).apply {
                        setMargins(
                            0,
                            0,
                            0,
                            dp(8)
                        )
                    }
                )
            }
        }

        /*
         * IMAGE
         *
         * Tap opens the existing in-app
         * full image viewer.
         */
        if (p.imageUrl.isNotBlank()) {

            val imageBox =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.VERTICAL

                    setPadding(
                        0,
                        dp(6),
                        0,
                        dp(8)
                    )
                }

            val image =
                ImageView(this).apply {

                    adjustViewBounds =
                        true

                    scaleType =
                        ImageView.ScaleType
                            .FIT_CENTER

                    maxHeight =
                        dp(600)

                    minimumHeight =
                        dp(180)

                    setBackgroundColor(
                        Color.rgb(
                            241,
                            245,
                            249
                        )
                    )

                    setOnClickListener {

                        openImageViewer(
                            p.imageUrl,
                            p.imageMime,
                            p.id
                        )
                    }
                }

            imageBox.addView(
                image,
                LinearLayout.LayoutParams(
                    -1,
                    -2
                )
            )

            val hint =
                TextView(this).apply {

                    text =
                        "🖼️ फोटो पर क्लिक करके पूरा खोलें"

                    textSize = 12f

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
                        dp(4),
                        0,
                        dp(6)
                    )

                    setOnClickListener {
                        image.performClick()
                    }
                }

            imageBox.addView(
                hint
            )

            box.addView(
                imageBox
            )

            ChannelRepository.loadImage(
                p.imageUrl
            ) { bitmap, _ ->

                runOnUiThread {

                    if (bitmap != null) {

                        image.setImageBitmap(
                            bitmap
                        )

                        image.scaleType =
                            ImageView.ScaleType
                                .FIT_CENTER

                    } else {

                        hint.text =
                            "🖼️ फोटो खोलने के लिए यहाँ दबाएँ"
                    }
                }
            }
        }

        /*
         * PDF / DOCUMENT
         */
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
                        dp(7),
                        0,
                        dp(7)
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

        /*
         * ----------------------------------------------------
         * FINAL SINGLE ACTION ROW
         * ----------------------------------------------------
         */

        val actions =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    0,
                    dp(8),
                    0,
                    0
                )
            }

        /*
         * CURRENT COUNTS
         */
        val currentLiked =
            likedState[p.id] ?: false

        val shownLikes =
            likeOverrides[p.id]
                ?: p.likeCount

        val shownShares =
            shareOverrides[p.id]
                ?: p.shareCount

        val shownViews =
            viewOverrides[p.id]
                ?: p.viewCount

        val shownComments =
            p.commentCount

        /*
         * ----------------------------------------------------
         * LIKE
         * ----------------------------------------------------
         */
        val like =
            TextView(this).apply {

                text =
                    if (currentLiked) {
                        "👍 Like $shownLikes"
                    } else {
                        "👍 Like $shownLikes"
                    }

                gravity =
                    Gravity.CENTER

                textSize = 12f

                includeFontPadding =
                    false

                maxLines = 1

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

                        android.graphics.drawable
                            .GradientDrawable()
                            .apply {

                                setColor(
                                    Color.WHITE
                                )

                                cornerRadius =
                                    dp(20).toFloat()

                                setStroke(
                                    dp(1),
                                    Color.rgb(
                                        14,
                                        91,
                                        215
                                    )
                                )
                            }
                    }

                setPadding(
                    dp(8),
                    dp(7),
                    dp(8),
                    dp(7)
                )

                setOnClickListener {

    toggleLike(
        p,
        currentLiked,
        this
    )
}
            }

        actions.addView(
            like,
            LinearLayout.LayoutParams(
                0,
                dp(38),
                1f
            ).apply {

                setMargins(
                    0,
                    0,
                    dp(4),
                    0
                )
            }
        )

        /*
         * ----------------------------------------------------
         * SHARE
         * ----------------------------------------------------
         */
        val share =
            TextView(this).apply {

                text =
                    "↗ Share $shownShares"

                gravity =
                    Gravity.CENTER

                textSize = 12f

                includeFontPadding =
                    false

                maxLines = 1

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
                    dp(8),
                    dp(7),
                    dp(8),
                    dp(7)
                )

                setOnClickListener {

    sharePost(
        p,
        this
    )
}
            }

        actions.addView(
            share,
            LinearLayout.LayoutParams(
                0,
                dp(38),
                1f
            ).apply {

                setMargins(
                    dp(4),
                    0,
                    dp(4),
                    0
                )
            }
        )

        /*
         * ----------------------------------------------------
         * COMMENT
         * ----------------------------------------------------
         */
        val comments =
            TextView(this).apply {

                text =
                    "💬 Comment $shownComments"

                gravity =
                    Gravity.CENTER

                textSize = 12f

                includeFontPadding =
                    false

                maxLines = 1

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    if (
                        globalComments &&
                        p.commentsEnabled
                    ) {
                        Color.rgb(
                            15,
                            118,
                            110
                        )
                    } else {
                        Color.rgb(
                            100,
                            116,
                            139
                        )
                    }
                )

                background =
                    Color.WHITE.toDrawable()

                setPadding(
                    dp(7),
                    dp(7),
                    dp(7),
                    dp(7)
                )

                setOnClickListener {

                    showComments(p)
                }
            }

        actions.addView(
            comments,
            LinearLayout.LayoutParams(
                0,
                dp(38),
                1f
            ).apply {

                setMargins(
                    dp(4),
                    0,
                    dp(4),
                    0
                )
            }
        )

        /*
         * ----------------------------------------------------
         * VIEWS
         *
         * IMPORTANT:
         * Only eye icon + number.
         * No "Views" text.
         * ----------------------------------------------------
         */
        val views =
            TextView(this).apply {

                text =
                    "👁️ $shownViews"

                gravity =
                    Gravity.CENTER

                textSize = 12f

                includeFontPadding =
                    false

                maxLines = 1

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.rgb(
                        71,
                        85,
                        105
                    )
                )

                background =
                    Color.rgb(
                        241,
                        245,
                        249
                    ).toDrawable()

                setPadding(
                    dp(8),
                    dp(7),
                    dp(8),
                    dp(7)
                )

                isClickable =
                    false
            }

        actions.addView(
            views,
            LinearLayout.LayoutParams(
                0,
                dp(38),
                0.72f
            ).apply {

                setMargins(
                    dp(4),
                    0,
                    0,
                    0
                )
            }
        )

        /*
         * ADD ONLY ONE ACTION ROW.
         */
        box.addView(
            actions,
            LinearLayout.LayoutParams(
                -1,
                dp(46)
            )
        )

        /*
         * IMPORTANT:
         * There is intentionally NO separate
         * Views / Shares / Likes / Comments
         * stats row here.
         */

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
                        dp(18)
                    )
                }
        }
    }
        

                    
                        
             
                    
                
    private fun openImageViewer(
        mediaRef: String,
        mime: String,
        postId: String
    ) {

        val image =
            ImageView(this).apply {

                adjustViewBounds =
                    true

                scaleType =
                    ImageView.ScaleType.FIT_CENTER

                setBackgroundColor(
                    Color.BLACK
                )

                setPadding(
                    dp(4),
                    dp(4),
                    dp(4),
                    dp(4)
                )

                maxWidth =
                    dp(600)

                maxHeight =
                    dp(600)
            }

        val container =
            FrameLayout(this).apply {

                setBackgroundColor(
                    Color.BLACK
                )

                setPadding(
                    dp(6),
                    dp(6),
                    dp(6),
                    dp(6)
                )

                addView(
                    image,
                    FrameLayout.LayoutParams(
                        -1,
                        -2
                    ).apply {

                        gravity =
                            Gravity.CENTER

                        width =
                            dp(600)

                        height =
                            dp(600)
                    }
                )
            }

        val dialog =
            AlertDialog.Builder(this)
                .setView(container)
                .setNegativeButton(
                    "Close",
                    null
                )
                .create()

        dialog.window?.setBackgroundDrawable(
            Color.BLACK.toDrawable()
        )

        ChannelRepository.loadImage(
            mediaRef
        ) { bitmap, error ->

            runOnUiThread {

                if (bitmap != null) {

                    image.setImageBitmap(
                        bitmap
                    )

                    dialog.show()

                } else {

                    Toast.makeText(
                        this,
                        error
                            ?: "Image नहीं खुली",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        /*
         * Allow tap on image to close.
         */
        image.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun toggleLike(
    p: ChannelPost,
    currentLiked: Boolean,
    likeButton: TextView
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

        likedState[p.id] =
            target

        val base =
            likeOverrides[p.id]
                ?: p.likeCount

        val newCount =
            (
                base +
                    if (target) {
                        1L
                    } else {
                        -1L
                    }
            ).coerceAtLeast(0L)

        likeOverrides[p.id] =
            newCount

        runOnUiThread {

            likeButton.text =
                "👍 Like $newCount"

            likeButton.setTextColor(
                if (target) {
                    Color.WHITE
                } else {
                    Color.rgb(
                        14,
                        91,
                        215
                    )
                }
            )

            likeButton.background =
                if (target) {

                    GradientFactory.gradient(
                        "#2563EB",
                        "#38BDF8"
                    )

                } else {

                    android.graphics.drawable
                        .GradientDrawable()
                        .apply {

                            setColor(
                                Color.WHITE
                            )

                            cornerRadius =
                                dp(20).toFloat()

                            setStroke(
                                dp(1),
                                Color.rgb(
                                    14,
                                    91,
                                    215
                                )
                            )
                        }
                }
        }

        ChannelRepository.like(
            p.id,
            target
        ) { ok ->

            if (!ok) {

                likedState[p.id] =
                    currentLiked

                likeOverrides[p.id] =
                    p.likeCount

                runOnUiThread {

                    likeButton.text =
                        "👍 Like ${p.likeCount}"

                    likeButton.setTextColor(
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

                    likeButton.background =
                        if (currentLiked) {

                            GradientFactory.gradient(
                                "#2563EB",
                                "#38BDF8"
                            )

                        } else {

                            android.graphics.drawable
                                .GradientDrawable()
                                .apply {

                                    setColor(
                                        Color.WHITE
                                    )

                                    cornerRadius =
                                        dp(20).toFloat()

                                    setStroke(
                                        dp(1),
                                        Color.rgb(
                                            14,
                                            91,
                                            215
                                        )
                                    )
                                }
                        }

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

    /*
     * POST SHARE
     */
    private fun sharePost(
    p: ChannelPost,
    shareButton: TextView
) {

    shareOverrides[p.id] =
        (
            shareOverrides[p.id]
                ?: p.shareCount
        ) + 1L

    runOnUiThread {
        shareButton.text =
            "↗ Share ${shareOverrides[p.id]}"
    }

    AnalyticsTracker.uniquePostShare(
        this,
        p.id
    )

    val web =
        "https://shiksha-rojgar.web.app/channel" +
            "?post=${Uri.encode(p.id)}"

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
                "\n\n🌐 पोस्ट खोलें:\n$web"
            )
        }

    ShareHelper.shareImageOrText(
        this,
        "Shiksha Rojgar Channel",
        text,
        p.imageUrl,
        "Share Channel Post"
    )
}
                            

    /*
     * COMMENTS
     */
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
                    dp(8),
                    dp(4),
                    dp(8),
                    dp(4)
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
                    dp(10),
                    dp(10),
                    dp(10),
                    dp(10)
                )
            }

        val listBox =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL
            }

        val commentScroll =
            ScrollView(this)

        commentScroll.addView(
            listBox
        )

        layout.addView(
            commentScroll,
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
                                dp(4),
                                dp(10),
                                dp(4),
                                dp(10)
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
                                    dp(8),
                                    dp(8),
                                    dp(8),
                                    dp(8)
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
                    dp(12),
                    dp(10),
                    dp(12),
                    dp(10)
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
                    TimeUnit.SECONDS
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
                    dp(12),
                    dp(10),
                    dp(12),
                    dp(10)
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

    /*
     * OFFLINE
     */
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

        safeOpenUrl(url)
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
                    dp(10),
                    dp(10),
                    dp(10),
                    dp(10)
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
                    dp(4),
                    dp(4),
                    dp(4),
                    dp(12)
                )

                autoLinkMask =
                    Linkify.WEB_URLS

                movementMethod =
                    LinkMovementMethod
                        .getInstance()

                Linkify.addLinks(
                    this,
                    Linkify.WEB_URLS
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

                        maxHeight =
                            dp(600)

                        setPadding(
                            0,
                            dp(10),
                            0,
                            dp(10)
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

                updateTopicSubscription(
                    following
                )
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

    /*
     * Find URL from normal text.
     */
    private fun findFirstUrl(
        text: String
    ): String? {

        val matcher =
            Patterns.WEB_URL
                .matcher(text)

        return if (matcher.find()) {

            var url =
                matcher.group()

            if (
                !url.startsWith(
                    "http://"
                ) &&
                !url.startsWith(
                    "https://"
                )
            ) {
                url =
                    "https://$url"
            }

            url

        } else {
            null
        }
    }

    private fun openFirstUrlFromText(
        text: String
    ) {

        val url =
            findFirstUrl(text)

        if (url != null) {
            safeOpenUrl(url)
        }
    }

    private fun isVideoUrl(
        url: String
    ): Boolean {

        val lower =
            url.lowercase(Locale.US)

        return lower.contains(
            "youtube.com"
        ) ||
            lower.contains(
                "youtu.be"
            ) ||
            lower.contains(
                "facebook.com"
            ) ||
            lower.contains(
                "instagram.com"
            ) ||
            lower.endsWith(".mp4") ||
            lower.endsWith(".webm") ||
            lower.endsWith(".m3u8")
    }

    private fun safeOpenUrl(
        url: String
    ) {

        try {

            val finalUrl =
                if (
                    url.startsWith(
                        "http://"
                    ) ||
                    url.startsWith(
                        "https://"
                    ) ||
                    url.startsWith(
                        "shiksharojgar://"
                    )
                ) {
                    url
                } else {
                    "https://$url"
                }

            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(finalUrl)
                )
            )

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
    ):
        android.graphics.drawable.GradientDrawable {

        return android.graphics.drawable
            .GradientDrawable(
                android.graphics.drawable
                    .GradientDrawable
                    .Orientation.TL_BR,
                intArrayOf(
                    Color.parseColor(a),
                    Color.parseColor(b)
                )
            )
            .apply {

                cornerRadius =
                    26f
            }
    }

    fun rounded(
        a: String
    ):
        android.graphics.drawable.GradientDrawable {

        return android.graphics.drawable
            .GradientDrawable()
            .apply {

                setColor(
                    Color.parseColor(a)
                )

                cornerRadius =
                    26f
            }
    }

    fun roundedWhite():
        android.graphics.drawable.GradientDrawable {

        return android.graphics.drawable
            .GradientDrawable()
            .apply {

                setColor(
                    Color.WHITE
                )

                cornerRadius =
                    20f
            }
    }
}
