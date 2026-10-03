package com.shiksharojgar.app

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.view.animation.AlphaAnimation
import android.widget.GridLayout
import android.widget.TextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager

class MainActivity : AppCompatActivity() {

    private val liveHandler = Handler(Looper.getMainLooper())
    private var floatingNoticeImage: ImageView? = null
   private var noticeCardView: View? = null
    private val liveTickerRunnable = object : Runnable {
        override fun run() {
            animateTicker()
            liveHandler.postDelayed(this, 120000L)
        }
    }

    private lateinit var adapter: PostAdapter

    private var channelPostsListener:
        com.google.firebase.firestore.ListenerRegistration? = null

    private var homeCategoriesListener:
        com.google.firebase.firestore.ListenerRegistration? = null

    private var channelBaselineReady = false

    private val shiksha = "https://www.shiksharojgar.com/"
    private val vacancy = "https://www.vacancybazaar.in/"
    private val whatsapp =
        "https://www.whatsapp.com/channel/0029Vb6GEDZKLaHxODkeHP1a"
    private val telegram = "https://t.me/Shiksha_Rojgar"
    private val youtube = "https://youtube.com/@mp_education_jobs_news"
    private val facebook =
        "https://www.facebook.com/share/1EGdKsY3zL/"
    private val instagram =
        "https://www.instagram.com/shiksha_rojgar?igsh=MX"
    private val twitter =
        "https://x.com/EducationNewsMP?t=pgn4MwOAqY7-WIaoJgX5_A&s=09"
    private val sharechat =
        "https://sharechat.com/profile/4200901361?d=n"
    private val arattai =
        "https://aratt.ai/@a_s_chouhan"

    private val unreadReceiver =
        object : android.content.BroadcastReceiver() {

            override fun onReceive(
                context: android.content.Context?,
                intent: android.content.Intent?
            ) {
                if (
                    intent?.action ==
                    "com.shiksharojgar.app.CHANNEL_UNREAD_CHANGED" &&
                    ::adapter.isInitialized
                ) {
                    updateChannelBadge()
                    updateNoticeUnreadCount()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        androidx.core.content.ContextCompat.registerReceiver(
            this,
            unreadReceiver,
            android.content.IntentFilter(
                "com.shiksharojgar.app.CHANNEL_UNREAD_CHANGED"
            ),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setContentView(R.layout.activity_main)

        updateChannelBadge()

        AnalyticsTracker.appOpen(this)

        ChannelRepository.ensureSignedIn {
            AnalyticsTracker.appIdentity(this)
        }

        setupFirebaseNotifications()

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.topBar)
        ) { view, insets ->

            val top =
                insets.getInsets(
                    WindowInsetsCompat.Type.statusBars()
                ).top

            view.setPadding(
                view.paddingLeft,
                top + 6,
                view.paddingRight,
                view.paddingBottom
            )

            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.bottomNav)
        ) { view, insets ->

            val bottom =
                insets.getInsets(
                    WindowInsetsCompat.Type.navigationBars()
                ).bottom

            view.setPadding(
                view.paddingLeft,
                5,
                view.paddingRight,
                bottom + 8
            )

            insets
        }

        adapter = PostAdapter(emptyList()) {
            openInApp(it.url)
        }

        findViewById<androidx.recyclerview.widget.RecyclerView>(
            R.id.postsRecycler
        ).apply {

            layoutManager =
                LinearLayoutManager(this@MainActivity)

            adapter = this@MainActivity.adapter
        }

        setupLiveBreaking()

        /*
         * HOME CATEGORIES
         *
         * Categories अब Firestore से आएँगी।
         */
        setupQuickCards()

        findViewById<View>(
    R.id.channelButtonContainer
).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ChannelActivity::class.java
                )
            )
        }

        /*
         * Hidden admin entry:
         * 7 quick taps on logo.
         */
        val adminTapCounter = intArrayOf(0)
        var lastAdminTap = 0L

        findViewById<ImageView>(R.id.logo).setOnClickListener {

            val now = System.currentTimeMillis()

            if (now - lastAdminTap > 2500L) {
                adminTapCounter[0] = 0
            }

            lastAdminTap = now
            adminTapCounter[0]++

            if (adminTapCounter[0] >= 7) {

                adminTapCounter[0] = 0

                startActivity(
                    Intent(
                        this,
                        AdminActivity::class.java
                    )
                )
            }
        }

        setupWebsites()
        setupSocials()

            
                findViewById<TextView>(
            R.id.refreshButton
        ).setOnClickListener {

            val b =
                findViewById<TextView>(
                    R.id.refreshButton
                )

            b.isEnabled = false

            b.text = "↻"

            b.animate()
                .rotationBy(720f)
                .setDuration(700L)
                .withEndAction {
                    b.rotation = 0f
                }
                .start()

            loadPosts {

                b.animate()
                    .rotation(0f)
                    .setDuration(150L)
                    .start()

                b.text = "↻ Refresh"

                b.isEnabled = true
            }
        }

        findViewById<TextView>(
            R.id.searchButton
        ).setOnClickListener {
            showSearchHelp()
        }

        findViewById<TextView>(
            R.id.notificationButton
        ).setOnClickListener {

            val prefs =
                getSharedPreferences(
                    "sr_notifications",
                    MODE_PRIVATE
                )

            val enabled =
                prefs.getBoolean(
                    "updates_enabled",
                    false
                )

            if (enabled) {

                FirebaseMessaging.getInstance()
                    .unsubscribeFromTopic("all_updates")

                prefs.edit()
                    .putBoolean(
                        "updates_enabled",
                        false
                    )
                    .apply()

                Toast.makeText(
                    this,
                    "🔕 Notifications बंद कर दी गईं",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                FirebaseMessaging.getInstance()
                    .subscribeToTopic("all_updates")
                    .addOnCompleteListener { task ->

                        if (task.isSuccessful) {

                            prefs.edit()
                                .putBoolean(
                                    "updates_enabled",
                                    true
                                )
                                .apply()

                            Toast.makeText(
                                this,
                                "🔔 नई Updates की notifications चालू हैं",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {

                            Toast.makeText(
                                this,
                                "Notifications चालू नहीं हो सकीं",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                if (
                    Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission(
                        "android.permission.POST_NOTIFICATIONS"
                    ) != PackageManager.PERMISSION_GRANTED
                ) {

                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(
                            "android.permission.POST_NOTIFICATIONS"
                        ),
                        1001
                    )
                }
            }
        }

        findViewById<TextView>(
            R.id.shareButton
        ).setOnClickListener {
            shareApp()
        }

        findViewById<TextView>(
            R.id.homeNav
        ).setOnClickListener {

            window.decorView
                .findViewById<View>(
                    android.R.id.content
                )
                .scrollTo(0, 0)
        }

        findViewById<TextView>(
            R.id.jobsNav
        ).setOnClickListener {
            openInApp(vacancy)
        }

        findViewById<TextView>(
            R.id.updatesNav
        ).setOnClickListener {

            loadPosts()

            findViewById<android.widget.ScrollView>(
                R.id.contentScroll
            ).post {

                findViewById<View>(
                    R.id.postsRecycler
                ).requestFocus()

                findViewById<android.widget.ScrollView>(
                    R.id.contentScroll
                ).smoothScrollTo(
                    0,
                    findViewById<View>(
                        R.id.postsRecycler
                    ).top
                )
            }
        }

        findViewById<TextView>(
            R.id.menuNav
        ).setOnClickListener {
            showMenu()
        }
// ============================================================
// LATEST POSTS NAVIGATION
// ============================================================

val contentScroll =
    findViewById<android.widget.ScrollView>(
        R.id.contentScroll
    )

val morePostsButton =
    findViewById<TextView>(
        R.id.morePostsButton
    )

val topPostsButton =
    findViewById<TextView>(
        R.id.topPostsButton
    )

morePostsButton.setOnClickListener {

    contentScroll.post {

        contentScroll.smoothScrollTo(
            0,
            findViewById<View>(
                R.id.postsRecycler
            ).top
        )
    }
}

topPostsButton.setOnClickListener {

    contentScroll.smoothScrollTo(
        0,
        0
    )
}

contentScroll.setOnScrollChangeListener {
        v,
        _,
        scrollY,
        _,
        _ ->

    val scrollView =
        v as android.widget.ScrollView

    val child =
        scrollView.getChildAt(0)

    val atBottom =
        scrollY +
        scrollView.height >=
        child.height - 24

    topPostsButton.visibility =
    if (atBottom) {
        View.VISIBLE
    } else {
        View.GONE
    }

morePostsButton.visibility =
    if (atBottom) {
        View.GONE
    } else {
        View.VISIBLE
    }
}
        loadPosts()

        startLocalChannelUnreadTracker()
    }

    // ============================================================
    // CHANNEL UNREAD
    // ============================================================

    private fun startLocalChannelUnreadTracker() {

        val prefs =
            getSharedPreferences(
                "sr_notifications",
                MODE_PRIVATE
            )

        val initialized =
            prefs.getBoolean(
                "channel_unread_initialized",
                false
            )

        channelPostsListener?.remove()

        channelPostsListener =
            FirebaseFirestore.getInstance()
                .collection("channel_posts")
                .orderBy(
                    "createdAt",
                    Query.Direction.DESCENDING
                )
                .limit(100)
                .addSnapshotListener { snap, err ->

                    if (
                        err != null ||
                        snap == null ||
                        snap.isEmpty
                    ) {
                        return@addSnapshotListener
                    }

                    val newest =
                        snap.documents
                            .mapNotNull {
                                it.getLong("createdAt")
                            }
                            .maxOrNull()
                            ?: return@addSnapshotListener

                    if (
                        !initialized &&
                        !channelBaselineReady
                    ) {

                        prefs.edit()
                            .putLong(
                                "channel_last_seen_at",
                                newest
                            )
                            .putBoolean(
                                "channel_unread_initialized",
                                true
                            )
                            .putInt(
                                "channel_unread",
                                0
                            )
                            .apply()

                        channelBaselineReady = true

                        updateChannelBadge()

                        return@addSnapshotListener
                    }

                    channelBaselineReady = true

                    if (
                        prefs.getBoolean(
                            "channel_open",
                            false
                        )
                    ) {

                        prefs.edit()
                            .putInt(
                                "channel_unread",
                                0
                            )
                            .putLong(
                                "channel_last_seen_at",
                                newest
                            )
                            .apply()

                        updateChannelBadge()

                        return@addSnapshotListener
                    }

                    val lastSeen =
                        prefs.getLong(
                            "channel_last_seen_at",
                            0L
                        )

                    val unread =
                        snap.documents.count {

                            (it.getLong("createdAt")
                                ?: 0L) > lastSeen
                        }

                    if (unread > 0) {

                        prefs.edit()
                            .putInt(
                                "channel_unread",
                                unread
                            )
                            .apply()

                        updateChannelBadge()
                    }
                }
    }

    private fun updateChannelBadge() {

        val badge =
            findViewById<TextView>(
                R.id.channelBadge
            )

        val count =
            getSharedPreferences(
                "sr_notifications",
                MODE_PRIVATE
            ).getInt(
                "channel_unread",
                0
            )

        badge.text =
            if (count > 99) {
                "99+"
            } else {
                count.toString()
            }

        badge.visibility =
            if (count > 0) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }
private fun updateNoticeUnreadCount() {

    val noticeBadge =
        findViewById<TextView?>(
            resources.getIdentifier(
                "noticeUnreadBadge",
                "id",
                packageName
            )
        )

    if (noticeBadge == null) {
        return
    }

    FirebaseFirestore.getInstance()
        .collection("home_notices")
        .whereEqualTo(
            "enabled",
            true
        )
        .get()
        .addOnSuccessListener { snapshot ->

            val now =
                System.currentTimeMillis()

            val count =
                snapshot.documents.count { doc ->

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

            noticeBadge.text =
                if (count > 99) {
                    "99+"
                } else {
                    count.toString()
                }

            noticeBadge.visibility =
                if (count > 0) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }
        .addOnFailureListener {

            noticeBadge.visibility =
                View.GONE
        }
}

    // ============================================================
    // LIFECYCLE
    // ============================================================

    override fun onResume() {

        super.onResume()

        if (::adapter.isInitialized) {
            updateChannelBadge()
        }
        updateNoticeUnreadCount()
loadFloatingNoticePhoto()
        liveHandler.removeCallbacks(
            liveTickerRunnable
        )

        liveHandler.post(
            liveTickerRunnable
        )
    }

    override fun onPause() {

    liveHandler.removeCallbacks(
        liveTickerRunnable
    )

    floatingNoticeImage?.clearAnimation()

    super.onPause()
}

    override fun onDestroy() {

        channelPostsListener?.remove()
        channelPostsListener = null

        homeCategoriesListener?.remove()
        homeCategoriesListener = null

        try {
            unregisterReceiver(
                unreadReceiver
            )
        } catch (_: Exception) {
        }

        super.onDestroy()
    }

    // ============================================================
    // FIREBASE NOTIFICATIONS
    // ============================================================

    private fun setupFirebaseNotifications() {

        FirebaseMessaging.getInstance()
            .subscribeToTopic("all_updates")

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val nm =
                getSystemService(
                    NotificationManager::class.java
                )

            nm.createNotificationChannel(
                NotificationChannel(
                    "shiksha_updates",
                    "Shiksha Rojgar Updates",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )

            nm.createNotificationChannel(
                NotificationChannel(
                    "shiksha_channel",
                    "Shiksha Rojgar Channel",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        if (
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(
                "android.permission.POST_NOTIFICATIONS"
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    "android.permission.POST_NOTIFICATIONS"
                ),
                1001
            )
        }
    }

    // ============================================================
    // LIVE BREAKING
    // ============================================================

private fun loadFloatingNoticePhoto() {

    val root =
        findViewById<android.view.ViewGroup>(
            android.R.id.content
        )

    floatingNoticeImage?.let {
        root.removeView(it)
        floatingNoticeImage = null
    }

    val noticeCard =
        noticeCardView
            ?: return

    FirebaseFirestore.getInstance()
        .collection("home_notices")
        .whereEqualTo(
            "enabled",
            true
        )
        .get()
        .addOnSuccessListener { snapshot ->

            val now =
                System.currentTimeMillis()

            val notice =
                snapshot.documents
                    .sortedByDescending {
                        it.getLong(
                            "createdAt"
                        ) ?: 0L
                    }
                    .firstOrNull { doc ->

                        val startAt =
                            doc.getLong(
                                "startAt"
                            ) ?: 0L

                        val endAt =
                            doc.getLong(
                                "endAt"
                            ) ?: 0L

                        val imageUrl =
                            doc.getString(
                                "imageUrl"
                            ).orEmpty()

                        imageUrl.isNotBlank() &&
                        (
                            startAt == 0L ||
                            now >= startAt
                        ) &&
                        (
                            endAt == 0L ||
                            now <= endAt
                        )
                    }
                    ?: return@addOnSuccessListener

            val imageUrl =
                notice.getString(
                    "imageUrl"
                ).orEmpty()

            if (imageUrl.isBlank()) {
                return@addOnSuccessListener
            }

            noticeCard.post {

                val noticeLocation =
                    IntArray(2)

                val rootLocation =
                    IntArray(2)

                noticeCard.getLocationOnScreen(
                    noticeLocation
                )

                root.getLocationOnScreen(
                    rootLocation
                )

                val imageView =
                    ImageView(this).apply {

                        scaleType =
                            ImageView.ScaleType.CENTER_CROP

                        background =
                            GradientDrawable().apply {
                                cornerRadius = 18f
                            }

                        clipToOutline = true

                        elevation = 12f
                    }

                val size =
                    (
                        85 *
                        resources.displayMetrics.density
                    ).toInt()

                val startTop =
                    noticeLocation[1] -
                    rootLocation[1] +
                    noticeCard.height

                val startLeft =
                    noticeLocation[0] -
                    rootLocation[0] +
                    (
                        noticeCard.width -
                        size
                    ) / 2

                val liveLabel =
                    findViewById<View?>(
                        R.id.liveLabel
                    )

                var endTop =
                    root.height -
                    size -
                    20

                if (liveLabel != null) {

                    val liveLocation =
                        IntArray(2)

                    liveLabel.getLocationOnScreen(
                        liveLocation
                    )

                    endTop =
                        (
                            liveLocation[1] -
                            rootLocation[1] -
                            size -
                            8
                        )
                            .coerceAtLeast(
                                startTop
                            )
                }

                val moveDistance =
                    (
                        endTop -
                        startTop
                    )
                        .coerceAtLeast(0)

                val params =
                    android.widget.FrameLayout.LayoutParams(
                        size,
                        size
                    ).apply {

                        leftMargin =
                            startLeft

                        topMargin =
                            startTop
                    }

                floatingNoticeImage =
                    imageView

                root.addView(
                    imageView,
                    params
                )

                ChannelRepository.loadImage(
                    imageUrl
                ) { bitmap, _ ->

                    if (bitmap == null) {
                        return@loadImage
                    }

                    runOnUiThread {

                        imageView.setImageBitmap(
                            bitmap
                        )

                        val animation =
                            android.view.animation.AnimationSet(
                                true
                            ).apply {

                                duration =
                                    7000L

                                fillAfter =
                                    false

                                addAnimation(
                                    android.view.animation.TranslateAnimation(
                                        0f,
                                        0f,
                                        0f,
                                        moveDistance.toFloat()
                                    )
                                )

                                addAnimation(
                                    android.view.animation.ScaleAnimation(
                                        0.65f,
                                        1.0f,
                                        0.65f,
                                        1.0f,
                                        android.view.animation.Animation.RELATIVE_TO_SELF,
                                        0.5f,
                                        android.view.animation.Animation.RELATIVE_TO_SELF,
                                        0.5f
                                    )
                                )

                                addAnimation(
                                    android.view.animation.AlphaAnimation(
                                        0.0f,
                                        1.0f
                                    )
                                )
                            }

                        animation.setAnimationListener(
                            object :
                                android.view.animation.Animation.AnimationListener {

                                override fun onAnimationStart(
                                    animation:
                                        android.view.animation.Animation?
                                ) {
                                }

                                override fun onAnimationRepeat(
                                    animation:
                                        android.view.animation.Animation?
                                ) {
                                }

                                override fun onAnimationEnd(
                                    animation:
                                        android.view.animation.Animation?
                                ) {

                                    imageView.animate()
                                        .alpha(0f)
                                        .setDuration(700L)
                                        .withEndAction {

                                            root.removeView(
                                                imageView
                                            )

                                            if (
                                                floatingNoticeImage ===
                                                imageView
                                            ) {
                                                floatingNoticeImage =
                                                    null
                                            }
                                        }
                                        .start()
                                }
                            }
                        )

                        imageView.startAnimation(
                            animation
                        )
                    }
                }
            }
        }
}
                               

    private fun setupLiveBreaking() {

        val live =
            findViewById<TextView>(
                R.id.liveLabel
            )

        val blink =
            AlphaAnimation(
                0.25f,
                1.0f
            ).apply {

                duration = 650L
                repeatMode = Animation.REVERSE
                repeatCount = Animation.INFINITE
            }

        live.clearAnimation()
        live.startAnimation(blink)

        findViewById<TextView>(
            R.id.ticker
        ).postDelayed(
            {
                animateTicker()
            },
            500L
        )
    }

    // ============================================================
    // COMMON CARD
    // ============================================================

    private fun roundedGradient(
        start: String,
        end: String
    ): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.parseColor(start),
                Color.parseColor(end)
            )
        ).apply {
            cornerRadius = 24f
        }

    private fun card(
        label: String,
        emoji: String,
        colors: Pair<String, String>,
        action: () -> Unit
    ): TextView =
        TextView(this).apply {

            text = "$emoji\n$label"

            gravity = Gravity.CENTER
            textSize = 14f

            setTextColor(Color.WHITE)

            typeface =
                android.graphics.Typeface.DEFAULT_BOLD

            background =
                roundedGradient(
                    colors.first,
                    colors.second
                )

            setPadding(
                8,
                14,
                8,
                14
            )

            isClickable = true
            isFocusable = true
            elevation = 5f

            setOnClickListener {
                action()
            }

            layoutParams =
                GridLayout.LayoutParams().apply {

                    width = 0
                    height = 126

                    columnSpec =
                        GridLayout.spec(
                            GridLayout.UNDEFINED,
                            1f
                        )

                    setMargins(
                        5,
                        5,
                        5,
                        5
                    )
                }
        }

    // ============================================================
    // MANAGED PAGE
    // ============================================================

    private fun openManagedPage(
        pageId: String,
        title: String
    ) {

        startActivity(
            Intent(
                this,
                ContentPageActivity::class.java
            ).apply {

                putExtra(
                    "pageId",
                    pageId
                )

                putExtra(
                    "pageTitle",
                    title
                )
            }
        )
    }

    // ============================================================
    // DYNAMIC HOME CATEGORY DATA
    // ============================================================

    private data class HomeCategory(
        val id: String,
        val name: String,
        val icon: String,
        val url: String,
        val pageId: String,
        val position: Long
    )

    // ============================================================
    // CATEGORY CARD
    // ============================================================

    private fun quickCard(
        label: String,
        emoji: String,
        colors: Pair<String, String>,
        action: () -> Unit
    ): LinearLayout =
        LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            gravity =
                Gravity.CENTER

            background =
                roundedGradient(
                    colors.first,
                    colors.second
                )

            elevation = 5f

            isClickable = true
            isFocusable = true

            setPadding(
                4,
                7,
                4,
                7
            )

            val icon =
                TextView(
                    this@MainActivity
                ).apply {

                    text = emoji

                    textSize = 32f

                    gravity =
                        Gravity.CENTER

                    setTextColor(
                        Color.WHITE
                    )
                }

            addView(
                icon,
                LinearLayout.LayoutParams(
                    -1,
                    90
                )
            )

            val title =
                TextView(
                    this@MainActivity
                ).apply {

                    text = label

                    textSize = 12.5f

                    gravity =
                        Gravity.CENTER

                    setTextColor(
                        Color.WHITE
                    )

                    typeface =
                        android.graphics.Typeface.DEFAULT_BOLD
                }

            addView(
                title,
                LinearLayout.LayoutParams(
                    -1,
                    60
                )
            )

            setOnClickListener {
                action()
            }

            layoutParams =
                GridLayout.LayoutParams().apply {

                    width = 0
                    height = 176

                    columnSpec =
                        GridLayout.spec(
                            GridLayout.UNDEFINED,
                            1f
                        )

                    setMargins(
                        2,
                        3,
                        2,
                        3
                    )
                }
        }

    // ============================================================
    // LIGHT CATEGORY CARD
    // ============================================================

    private fun learningCard(
    label: String,
    emoji: String,
    colors: Pair<String, String>,
    action: () -> Unit
): View {

    val frame =
        android.widget.FrameLayout(this).apply {

            layoutParams =
                GridLayout.LayoutParams().apply {

                    width = 0
                    height = 118

                    columnSpec =
                        GridLayout.spec(
                            GridLayout.UNDEFINED,
                            1f
                        )

                    setMargins(
                        3,
                        4,
                        3,
                        4
                    )
                }
        }

    val card =
        TextView(this).apply {

            text =
                "$emoji\n$label"

            gravity =
                Gravity.CENTER

            textSize = 10.5f

            setTextColor(
                Color.rgb(
                    30,
                    41,
                    59
                )
            )

            typeface =
                android.graphics.Typeface.DEFAULT_BOLD

            background =
                GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(
                        Color.parseColor(
                            colors.first
                        ),
                        Color.parseColor(
                            colors.second
                        )
                    )
                ).apply {

                    cornerRadius = 20f
                }

            setPadding(
                3,
                10,
                3,
                10
            )

            elevation = 3f

            isClickable = true
            isFocusable = true

            setOnClickListener {
                action()
            }
        }

    frame.addView(
        card,
        android.widget.FrameLayout.LayoutParams(
            -1,
            -1
        )
    )

    if (
        label.equals(
            "Notice/Update",
            ignoreCase = true
        ) ||
        label.equals(
            "Notice / Update",
            ignoreCase = true
        )
    ) {
        noticeCardView = frame
        val badge =
            TextView(this).apply {

                id =
                    R.id.noticeUnreadBadge

                text = "0"

                gravity =
                    Gravity.CENTER

                textSize = 13f

                setTextColor(
                    Color.WHITE
                )

                typeface =
                    android.graphics.Typeface.DEFAULT_BOLD

                background =
                    androidx.core.content.ContextCompat.getDrawable(
                        this@MainActivity,
                        R.drawable.badge_bg
                    )

                visibility =
                    View.GONE

                elevation = 8f
            }

        val badgeParams =
            android.widget.FrameLayout.LayoutParams(
                32,
                32
            ).apply {

                gravity =
                    Gravity.TOP or Gravity.END

                topMargin = 2
                rightMargin = 2
            }

        frame.addView(
            badge,
            badgeParams
        )
    }

    return frame
}

    // ============================================================
// OPEN HOME CATEGORY + SUB-CATEGORIES
// ============================================================

private fun openHomeCategory(
    category: HomeCategory
) {
if (category.id == "notices") {
    startActivity(
        Intent(
            this,
            NoticeActivity::class.java
        )
    )
    return
}

    FirebaseFirestore.getInstance()
        .collection("home_subcategories")
        .whereEqualTo(
            "parentId",
            category.id
        )
        .whereEqualTo(
            "enabled",
            true
        )
        .get()
        .addOnSuccessListener { snapshot ->

            val subCategories =
                snapshot.documents
                    .mapNotNull { doc ->

                        val name =
                            doc.getString("name")
                                ?.trim()
                                ?: return@mapNotNull null

                        if (name.isBlank()) {
                            return@mapNotNull null
                        }

                        val icon =
                            doc.getString("icon")
                                ?.trim()
                                .takeUnless {
                                    it.isNullOrBlank()
                                }
                                ?: "📌"

                        val url =
                            doc.getString("url")
                                ?.trim()
                                ?: ""

                        val pageType =
                            doc.getString("pageType")
                                ?.trim()
                                ?: "Website"

                        val position =
                            doc.getLong("position")
                                ?: 9999L

                       HomeSubCategory(
                          id = doc.id,                    
                           name = name,
                            icon = icon,
                            url = url,
                           pageType = pageType,
                          position = position,
                         buttonColor =
                              doc.getString("buttonColor")
                                   ?: "#E3F2FD"
                      )
                    }
                    .sortedBy {
                        it.position
                    }

            /*
             * यदि इस Main Category में
             * कोई Sub-Category नहीं है,
             * तो पुराना direct-open behavior रहेगा।
             */
            if (subCategories.isEmpty()) {

    openMainCategoryDirectly(
        category
    )

    return@addOnSuccessListener
}

val intent =
    Intent(
        this,
        HomeCategoryActivity::class.java
    )

intent.putExtra(
    "categoryId",
    category.id
)

intent.putExtra(
    "categoryName",
    category.name
)

intent.putExtra(
    "categoryIcon",
    category.icon
)

startActivity(intent)
        }
        .addOnFailureListener {

            /*
             * Firestore error होने पर भी
             * पुराना Home link काम करता रहेगा।
             */
            openMainCategoryDirectly(
                category
            )
        }
}


// ============================================================
// HOME SUB-CATEGORY MODEL
// ============================================================
private data class HomeSubCategory(
    val id: String,
    val name: String,
    val icon: String,
    val url: String,
    val pageType: String,
    val position: Long,
    val buttonColor: String
)

// ============================================================
// MAIN CATEGORY DIRECT OPEN
// ============================================================

private fun openMainCategoryDirectly(
    category: HomeCategory
) {

    /*
     * Managed App Page
     */
    if (category.pageId.isNotBlank()) {

        openManagedPage(
            category.pageId,
            category.name
        )

        return
    }

    /*
     * Website
     */
    if (category.url.isNotBlank()) {

        openInApp(
            category.url
        )

        return
    }

    Toast.makeText(
        this,
        "${category.name} का link अभी उपलब्ध नहीं है",
        Toast.LENGTH_SHORT
    ).show()
}


// ============================================================
// SHOW HOME SUB-CATEGORY DIALOG
// ============================================================

private fun showHomeSubCategoryDialog(
    category: HomeCategory,
    subCategories: List<HomeSubCategory>
) {

    val outer =
        LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            setPadding(
                18,
                4,
                18,
                8
            )
        }


    /*
     * Main Category header
     */
    val header =
        TextView(this).apply {

            text =
                "${category.icon}  ${category.name}"

            textSize = 19f

            gravity =
                Gravity.CENTER

            setTextColor(
                Color.rgb(
                    7,
                    89,
                    133
                )
            )

            typeface =
                android.graphics.Typeface.DEFAULT_BOLD

            setPadding(
                5,
                8,
                5,
                12
            )
        }

    outer.addView(
        header,
        LinearLayout.LayoutParams(
            -1,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    )


    /*
     * Scroll area
     */
    val scroll =
        android.widget.ScrollView(this).apply {

            isFillViewport = true
        }


    val list =
        LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            setPadding(
                0,
                2,
                0,
                2
            )
        }


    /*
     * Direction indicator
     *
     * नीचे और देखें / ऊपर और देखें
     */
    val indicator =
        TextView(this).apply {

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

            typeface =
                android.graphics.Typeface.DEFAULT_BOLD

            visibility =
                View.GONE

            setPadding(
                4,
                5,
                4,
                5
            )
        }


    /*
     * Sub-category buttons
     */
    subCategories.forEach { sub ->

        val button =
            android.widget.Button(this).apply {

                text =
                    "${sub.icon}  ${sub.name}"

                textSize = 14f

typeface =
    android.graphics.Typeface.DEFAULT_BOLD

setTextColor(
    Color.rgb(
        30,
        41,
        59
    )
)

background =
    GradientDrawable().apply {

        val color =
            try {
                Color.parseColor(
                    sub.buttonColor
                )
            } catch (e: Exception) {
                Color.WHITE
            }

        setColor(
            color
        )

        cornerRadius =
            18f

        setStroke(
            2,
            Color.rgb(
                226,
                232,
                240
            )
        )
    }

minHeight = 0

setPadding(
    6,
    2,
    6,
    2
)

                setOnClickListener {

                    openHomeSubCategory(
                        sub
                    )
                }
            }

        list.addView(
    button,
    LinearLayout.LayoutParams(
        -1,
        48
    ).apply {

        topMargin = 2
        bottomMargin = 2
    }
)
    }


    scroll.addView(
        list,
        android.widget.FrameLayout.LayoutParams(
    -1,
    LinearLayout.LayoutParams.WRAP_CONTENT
)
    )


    outer.addView(
        scroll,
        LinearLayout.LayoutParams(
            -1,
            0,
            1f
        )
    )


    outer.addView(
        indicator,
        LinearLayout.LayoutParams(
            -1,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    )


    /*
     * Scroll indicator logic
     */
    fun updateSubCategoryIndicator() {

        val maxScroll =
            scroll.getChildAt(0)
                ?.height
                ?.minus(scroll.height)
                ?: 0

        val current =
            scroll.scrollY

        if (maxScroll <= 10) {

            indicator.visibility =
                View.GONE

            return
        }

        indicator.visibility =
            View.VISIBLE

        when {

            current <= 10 -> {

                indicator.text =
                    "↓ नीचे और देखें"
            }

            current >= maxScroll - 10 -> {

                indicator.text =
                    "↑ ऊपर और देखें"
            }

            else -> {

                indicator.text =
                    "↕ ऊपर / नीचे स्क्रॉल करें"
            }
        }
    }


    scroll.viewTreeObserver
        .addOnGlobalLayoutListener {

            updateSubCategoryIndicator()
        }


    scroll.setOnScrollChangeListener {
            _: View,
            _: Int,
            _: Int,
            _: Int,
            _: Int ->

        updateSubCategoryIndicator()
    }


    AlertDialog.Builder(this)
        .setTitle(
            "📂 ${category.name}"
        )
        .setView(outer)
        .setNegativeButton(
            "CLOSE",
            null
        )
        .show()
}


// ============================================================
// OPEN SELECTED SUB-CATEGORY
// ============================================================

private fun openHomeSubCategory(
    sub: HomeSubCategory
) {

    if (sub.url.isBlank()) {

        Toast.makeText(
            this,
            "${sub.name} का link अभी उपलब्ध नहीं है",
            Toast.LENGTH_SHORT
        ).show()

        return
    }


    when (
        sub.pageType.lowercase()
    ) {

        "website" -> {

            openInApp(
                sub.url
            )
        }


        "external link" -> {

            openExternal(
                sub.url
            )
        }


        "app page" -> {

            /*
             * App Page में Admin द्वारा
             * url field में pageId रखा जाएगा।
             *
             * जैसे:
             * syllabus
             * study_material
             * career
             * notices
             * tools
             */

            if (
                sub.url.startsWith(
                    "http://"
                ) ||
                sub.url.startsWith(
                    "https://"
                )
            ) {

                openInApp(
                    sub.url
                )

            } else {

                openManagedPage(
                    sub.url,
                    sub.name
                )
            }
        }


        else -> {

            openInApp(
                sub.url
            )
        }
    }
}

    // ============================================================
    // DYNAMIC HOME CATEGORIES
    // ============================================================

    private fun setupQuickCards() {

        val quickGrid =
            findViewById<GridLayout>(
                R.id.quickGrid
            )

        val learningGrid =
            findViewById<GridLayout>(
                R.id.learningGrid
            )

        quickGrid.removeAllViews()
        learningGrid.removeAllViews()

        /*
         * पहले पुराने/current categories का fallback।
         *
         * Firestore categories मिलने पर
         * यह पूरा list replace हो जाएगा।
         */
        val fallback =
            listOf(

                HomeCategory(
                    "teacher",
                    "Teacher",
                    "👨‍🏫",
                    "https://www.shiksharojgar.com/2026/03/teacher-govt-employees.html",
                    "",
                    1
                ),

                HomeCategory(
                    "student",
                    "Student",
                    "👨‍🎓",
                    "https://www.shiksharojgar.com/2026/03/College%20%20University%20Students.html",
                    "",
                    2
                ),

                HomeCategory(
                    "school",
                    "School",
                    "🏫",
                    "https://www.shiksharojgar.com/2026/03/school-students-1-12-section-page.html",
                    "",
                    3
                ),

                HomeCategory(
                    "vacancy",
                    "Vacancy",
                    "💼",
                    "https://www.shiksharojgar.com/2026/03/latest-jobs-page.html",
                    "",
                    4
                ),

                HomeCategory(
                    "result",
                    "Result",
                    "🏆",
                    "https://www.shiksharojgar.com/2026/03/results.html",
                    "",
                    5
                ),

                HomeCategory(
                    "admit_card",
                    "Admit Card",
                    "🎫",
                    "https://www.shiksharojgar.com/2026/01/admit-card-download-zone.html",
                    "",
                    6
                ),

                                HomeCategory(
                    "study_material",
                    "Study Material",
                    "📖",
                    "",
                    "study_material",
                    7
                ),

                HomeCategory(
                    "career",
                    "Career Guide",
                    "📚",
                    "",
                    "career",
                    8
                ),

                HomeCategory(
                    "notices",
                    "Notice/Update",
                    "📢",
                    "",
                    "notices",
                    9
                ),

                HomeCategory(
                    "tools",
                    "Useful Tools",
                    "⚙️",
                    "",
                    "tools",
                    10
                )
            )

        renderHomeCategories(
            fallback
        )

        /*
         * Firestore listener
         *
         * Collection:
         * home_categories
         */

        homeCategoriesListener?.remove()

        homeCategoriesListener =
            FirebaseFirestore.getInstance()
                .collection("home_categories")
                .orderBy(
                    "position",
                    Query.Direction.ASCENDING
                )
                .addSnapshotListener { snapshot, error ->

                    if (
                        error != null ||
                        snapshot == null
                    ) {

                        /*
                         * Error होने पर fallback
                         * पहले से Home पर मौजूद रहेगा।
                         */

                        return@addSnapshotListener
                    }

                    val categories =
                        snapshot.documents.mapNotNull { doc ->

                            val name =
                                doc.getString(
                                    "name"
                                )?.trim()
                                    ?: return@mapNotNull null

                            if (name.isBlank()) {
                                return@mapNotNull null
                            }

                            val icon =
                                doc.getString(
                                    "icon"
                                )?.trim()
                                    .takeUnless {
                                        it.isNullOrBlank()
                                    }
                                    ?: "📌"

                            val url =
                                doc.getString(
                                    "url"
                                )?.trim()
                                    ?: ""

                            val pageId =
                                doc.getString(
                                    "pageId"
                                )?.trim()
                                    ?: ""

                            val position =
                                doc.getLong(
                                    "position"
                                ) ?: 9999L

                            HomeCategory(
                                id = doc.id,
                                name = name,
                                icon = icon,
                                url = url,
                                pageId = pageId,
                                position = position
                            )
                        }
                        .sortedBy {
                            it.position
                        }

                    /*
                     * Empty collection होने पर
                     * fallback को बनाए रखें।
                     */
                    if (categories.isEmpty()) {
                        return@addSnapshotListener
                    }

                    renderHomeCategories(
                        categories
                    )
                }
    }

    // ============================================================
    // RENDER CATEGORIES
    // ============================================================

    private fun renderHomeCategories(
        categories: List<HomeCategory>
    ) {

        val quickGrid =
            findViewById<GridLayout>(
                R.id.quickGrid
            )

        val learningGrid =
            findViewById<GridLayout>(
                R.id.learningGrid
            )

        quickGrid.removeAllViews()
        learningGrid.removeAllViews()

        val gradientColors =
            listOf(

                "#0D5BD7" to "#18A0FB",

                "#7C3AED" to "#EC4899",

                "#059669" to "#14B8A6",

                "#F97316" to "#EF4444",

                "#DB2777" to "#7C3AED",

                "#D97706" to "#FACC15"
            )

        /*
         * पहले 6:
         * बड़ा Quick Cards design
         */
        categories
            .take(6)
            .forEachIndexed { index, category ->

                val colors =
                    gradientColors[
                        index %
                            gradientColors.size
                    ]

                quickGrid.addView(
                    quickCard(
                        category.name,
                        category.icon,
                        colors
                    ) {
                        openHomeCategory(
                            category
                        )
                    }
                )
            }

        /*
         * 7th category से आगे:
         * Learning / secondary grid design
         */
        categories
    .drop(6)
    .take(4)
    .forEachIndexed { index, category ->

        val colors =
            when (index) {
                0 ->
                    "#DBEAFE" to "#93C5FD"

                1 ->
                    "#DCFCE7" to "#86EFAC"

                2 ->
                    "#FEF3C7" to "#FCD34D"

                else ->
                    "#FCE7F3" to "#F9A8D4"
            }

        learningGrid.addView(
            learningCard(
                category.name,
                category.icon,
                colors
            ) {
                openHomeCategory(
                    category
                )
            }
        )
    }
    }

    // ============================================================
    // WEBSITES
    // ============================================================

    private fun setupWebsites() {

        val grid =
            findViewById<GridLayout>(
                R.id.websiteGrid
            )

        grid.addView(
            websiteCard(
                "Shiksha Rojgar.com",
                "🎓",
                shiksha,
                "#075985",
                "#0EA5E9",
                R.drawable.logo,
                external = false
            )
        )

        grid.addView(
            websiteCard(
                "VacancyBazaar.in",
                "🚀",
                vacancy,
                "#E11D48",
                "#F59E0B",
                R.drawable.vacancy_logo
            )
        )
    }

    private fun websiteCard(
        label: String,
        emoji: String,
        url: String,
        c1: String,
        c2: String,
        icon: Int,
        external: Boolean = false
    ): LinearLayout =
        LinearLayout(this).apply {

            orientation =
                LinearLayout.HORIZONTAL

            gravity =
                Gravity.CENTER_VERTICAL

            background =
                roundedGradient(
                    c1,
                    c2
                )

            elevation = 6f

            setPadding(
                10,
                10,
                10,
                10
            )

            val logoView =
                ImageView(
                    this@MainActivity
                ).apply {

                    setImageResource(icon)

                    scaleType =
                        ImageView.ScaleType.CENTER_CROP

                    layoutParams =
                        LinearLayout.LayoutParams(
                            48,
                            48
                        )
                }

            addView(logoView)

            val text =
                TextView(
                    this@MainActivity
                ).apply {

                    this.text =
                        "$emoji  $label"

                    gravity =
                        Gravity.CENTER_VERTICAL

                    textSize = 17f

                    setTextColor(
                        Color.WHITE
                    )

                    typeface =
                        android.graphics.Typeface.DEFAULT_BOLD

                    setPadding(
                        10,
                        0,
                        6,
                        0
                    )
                }

            addView(
                text,
                LinearLayout.LayoutParams(
                    0,
                    60,
                    1f
                )
            )

            val openButton =
                TextView(
                    this@MainActivity
                ).apply {

                    this.text = "OPEN  ↗"

                    gravity =
                        Gravity.CENTER

                    textSize = 14f

                    setTextColor(
                        Color.WHITE
                    )

                    typeface =
                        android.graphics.Typeface.DEFAULT_BOLD

                    background =
                        GradientDrawable().apply {

                            setColor(
                                Color.argb(
                                    55,
                                    255,
                                    255,
                                    255
                                )
                            )

                            cornerRadius = 18f

                            setStroke(
                                1,
                                Color.argb(
                                    130,
                                    255,
                                    255,
                                    255
                                )
                            )
                        }

                    setPadding(
                        14,
                        0,
                        14,
                        0
                    )

                    isClickable = true
                    isFocusable = true

                    setOnClickListener {

                        AnalyticsTracker.websiteOpen(
                            this@MainActivity,
                            label
                        )

                        if (external) {
                            openExternal(url)
                        } else {
                            openInApp(url)
                        }
                    }
                }

            addView(
                openButton,
                LinearLayout.LayoutParams(
                    108,
                    52
                ).apply {
                    gravity =
                        Gravity.CENTER_VERTICAL
                }
            )

            setOnClickListener {

                AnalyticsTracker.websiteOpen(
                    this@MainActivity,
                    label
                )

                if (external) {
                    openExternal(url)
                } else {
                    openInApp(url)
                }
            }

            layoutParams =
                GridLayout.LayoutParams().apply {

                    width = 0
                    height = 112

                    columnSpec =
                        GridLayout.spec(
                            GridLayout.UNDEFINED,
                            1f
                        )

                    setMargins(
                        5,
                        5,
                        5,
                        5
                    )
                }
        }

    // ============================================================
    // SOCIAL
    // ============================================================

    private fun social(
        label: String,
        emoji: String,
        url: String,
        c1: String,
        c2: String
    ): TextView =
        card(
            label,
            emoji,
            c1 to c2
        ) {
            openExternal(url)
        }

    private fun setupSocials() {

        val grid =
            findViewById<GridLayout>(
                R.id.socialGrid
            )

        val items =
            listOf(

                Triple(
                    "WhatsApp",
                    "🟢",
                    whatsapp
                ) to
                    ("#16A34A" to "#22C55E"),

                Triple(
                    "Telegram",
                    "✈️",
                    telegram
                ) to
                    ("#0284C7" to "#38BDF8"),

                Triple(
                    "YouTube",
                    "▶️",
                    youtube
                ) to
                    ("#DC2626" to "#F43F5E"),

                Triple(
                    "Facebook",
                    "🔵",
                    facebook
                ) to
                    ("#2563EB" to "#60A5FA"),

                Triple(
                    "Instagram",
                    "📸",
                    instagram
                ) to
                    ("#F97316" to "#A855F7"),

                Triple(
                    "Twitter / X",
                    "𝕏",
                    twitter
                ) to
                    ("#111827" to "#374151"),

                Triple(
                    "ShareChat",
                    "💬",
                    sharechat
                ) to
                    ("#F97316" to "#EF4444"),

                Triple(
                    "Arattai",
                    "💠",
                    arattai
                ) to
                    ("#4F46E5" to "#06B6D4"),

                Triple(
                    "Official Website",
                    "🌐",
                    shiksha
                ) to
                    ("#0284C7" to "#0EA5E9")
            )

        items.forEach { (item, colors) ->

            grid.addView(
                social(
                    item.first,
                    item.second,
                    item.third,
                    colors.first,
                    colors.second
                )
            )
        }
    }

    // ============================================================
    // POSTS
    // ============================================================

    private fun loadPosts(
        done: (() -> Unit)? = null
    ) {

        findViewById<TextView>(
            R.id.ticker
        ).text =
            "Shiksha Rojgar की नई पोस्ट लोड हो रही हैं…"

        animateTicker()

        FeedLoader.load { posts ->

            adapter.submit(posts)

            val latest =
                posts.take(8)

            val title =
                if (latest.isNotEmpty()) {

                    latest.mapIndexed {
                            index,
                            post ->

                        "${index + 1}. ${post.title}"
                    }.joinToString(
                        "     ✦     "
                    )

                } else {

                    "अभी नई पोस्ट नहीं मिली — Refresh करें"
                }

            val tickerText =
                "$title     ✦     $title     ✦     $title"

            findViewById<TextView>(
                R.id.ticker
            ).text = tickerText

            animateTicker()

            findViewById<TextView>(
                R.id.ticker
            ).postDelayed(
                {
                    animateTicker()
                },
                350L
            )

            done?.invoke()
        }
    }

    // ============================================================
    // TICKER
    // ============================================================

    private fun animateTicker() {

        val ticker =
            findViewById<TextView>(
                R.id.ticker
            )

        ticker.post {

            ticker.clearAnimation()

            val parentWidth =
                (ticker.parent as View)
                    .width
                    .toFloat()
                    .coerceAtLeast(300f)

            val textWidth =
                ticker.paint
                    .measureText(
                        ticker.text.toString()
                    )
                    .coerceAtLeast(
                        parentWidth * .35f
                    )

            val from =
                parentWidth

            val to =
                -textWidth

            TranslateAnimation(
                from,
                to,
                0f,
                0f
            ).apply {

                duration = 90000L

                repeatCount =
                    Animation.INFINITE

                repeatMode =
                    Animation.RESTART

                setInterpolator(
                    android.view.animation.LinearInterpolator()
                )

                ticker.startAnimation(this)
            }
        }
    }

    // ============================================================
    // SHARE APP
    // ============================================================

    private fun shareApp() {

        val text =
            "📱 Shiksha Rojgar App\n" +
            "शिक्षा, रोजगार, शिक्षक, विद्यार्थी, Vacancy, Result और Admit Card अपडेट\n\n" +
            "https://shiksha-rojgar.web.app/channel"

        AnalyticsTracker.appShare(this)

        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {

                    type = "text/plain"

                    putExtra(
                        Intent.EXTRA_TEXT,
                        text
                    )
                },
                "Share Shiksha Rojgar"
            )
        )
    }

    // ============================================================
    // OPEN URL
    // ============================================================

    private fun openInApp(
        url: String
    ) {

        startActivity(
            Intent(
                this,
                WebViewActivity::class.java
            ).putExtra(
                "url",
                url
            )
        )
    }

    private fun openExternal(
        url: String
    ) {

        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )
        )
    }

    // ============================================================
    // SEARCH
    // ============================================================

    private fun showSearchHelp() {

        val input =
            android.widget.EditText(this)

        input.hint =
            "जैसे Teacher, SSC, Result"

        AlertDialog.Builder(this)
            .setTitle(
                "🔍 Search Shiksha Rojgar"
            )
            .setView(input)
            .setPositiveButton(
                "Search"
            ) { _, _ ->

                val query =
                    input.text
                        .toString()
                        .trim()

                if (query.isNotEmpty()) {

                    openInApp(
                        "$shiksha/search?q=${
                            Uri.encode(query)
                        }"
                    )
                }
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    // ============================================================
    // MENU
    // ============================================================

    private fun showMenu() {

        val items =
            arrayOf(
                "👨‍🏫 Teacher",
                "👨‍🎓 Student",
                "🏫 School",
                "💼 Vacancy",
                "🎫 Admit Card",
                "🏆 Result",
                "🌐 Shiksha Rojgar",
                "🚀 VacancyBazaar",
                "🟢 WhatsApp",
                "✈️ Telegram",
                "▶️ YouTube",
                "📸 Instagram"
            )

        AlertDialog.Builder(this)
            .setTitle(
                "☰ Shiksha Rojgar Menu"
            )
            .setItems(
                items
            ) { _, which ->

                when (which) {

                    0 ->
                        openInApp(
                            "https://www.shiksharojgar.com/2026/03/teacher-govt-employees.html"
                        )

                    1 ->
                        openInApp(
                            "https://www.shiksharojgar.com/2026/03/College%20%20University%20Students.html"
                        )

                    2 ->
                        openInApp(
                            "https://www.shiksharojgar.com/2026/03/school-students-1-12-section-page.html"
                        )

                    3 ->
                        openInApp(
                            "https://www.shiksharojgar.com/2026/03/latest-jobs-page.html"
                        )

                    4 ->
                        openInApp(
                            "https://www.shiksharojgar.com/2026/01/admit-card-download-zone.html"
                        )

                    5 ->
                        openInApp(
                            "https://www.shiksharojgar.com/2026/03/results.html"
                        )

                    6 ->
                        openInApp(shiksha)

                    7 ->
                        openInApp(vacancy)

                    8 ->
                        openExternal(whatsapp)

                    9 ->
                        openExternal(telegram)

                    10 ->
                        openExternal(youtube)

                    11 ->
                        openExternal(instagram)
                }
            }
            .show()
    }
}
