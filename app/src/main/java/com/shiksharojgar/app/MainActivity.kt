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
    private val liveTickerRunnable = object : Runnable {
        override fun run() {
            animateTicker()
            liveHandler.postDelayed(this, 120000L)
        }
    }
    private lateinit var adapter: PostAdapter
    private var channelPostsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var channelBaselineReady = false
    private val shiksha = "https://www.shiksharojgar.com/"
    private val vacancy = "https://www.vacancybazaar.in/"
    private val whatsapp = "https://www.whatsapp.com/channel/0029Vb6GEDZKLaHxODkeHP1a"
    private val telegram = "https://t.me/Shiksha_Rojgar"
    private val youtube = "https://youtube.com/@mp_education_jobs_news"
    private val facebook = "https://www.facebook.com/share/1EGdKsY3zL/"
    private val instagram = "https://www.instagram.com/shiksha_rojgar?igsh=MX"
    private val twitter = "https://x.com/EducationNewsMP?t=pgn4MwOAqY7-WIaoJgX5_A&s=09"
    private val sharechat = "https://sharechat.com/profile/4200901361?d=n"
    private val arattai = "https://aratt.ai/@a_s_chouhan"

    private val unreadReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == "com.shiksharojgar.app.CHANNEL_UNREAD_CHANGED" && ::adapter.isInitialized) updateChannelBadge()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.content.ContextCompat.registerReceiver(this, unreadReceiver, android.content.IntentFilter("com.shiksharojgar.app.CHANNEL_UNREAD_CHANGED"), androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
        setContentView(R.layout.activity_main)
        updateChannelBadge()
        AnalyticsTracker.appOpen(this)
        ChannelRepository.ensureSignedIn { AnalyticsTracker.appIdentity(this) }
        setupFirebaseNotifications()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBar)) { view, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(view.paddingLeft, top + 6, view.paddingRight, view.paddingBottom)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottomNav)) { view, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(view.paddingLeft, 5, view.paddingRight, bottom + 8)
            insets
        }
        adapter = PostAdapter(emptyList()) { openInApp(it.url) }
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.postsRecycler).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
        setupLiveBreaking()
        setupQuickCards()
        findViewById<TextView>(R.id.channelButton).setOnClickListener { startActivity(Intent(this, ChannelActivity::class.java)) }
        // Hidden admin entry: not shown in the normal UI. Seven quick taps on the logo opens the secured admin login.
        val adminTapCounter = intArrayOf(0)
        var lastAdminTap = 0L
        findViewById<ImageView>(R.id.logo).setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastAdminTap > 2500L) adminTapCounter[0] = 0
            lastAdminTap = now
            adminTapCounter[0]++
            if (adminTapCounter[0] >= 7) {
                adminTapCounter[0] = 0
                startActivity(Intent(this, AdminActivity::class.java))
            }
        }
        setupWebsites()
        setupSocials()
        findViewById<TextView>(R.id.refreshButton).setOnClickListener {
            val b=findViewById<TextView>(R.id.refreshButton)
            b.text="↻ Refreshing…"
            b.isEnabled=false
            b.animate().rotationBy(720f).setDuration(700L).withEndAction{b.rotation=0f}.start()
            loadPosts {
                b.animate().rotation(0f).setDuration(150L).start()
                b.text="↻ Refresh"
                b.isEnabled=true
            }
        }
        findViewById<TextView>(R.id.searchButton).setOnClickListener { showSearchHelp() }
        findViewById<TextView>(R.id.notificationButton).setOnClickListener {
            val prefs = getSharedPreferences("sr_notifications", MODE_PRIVATE)
            val enabled = prefs.getBoolean("updates_enabled", false)
            if (enabled) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("all_updates")
                prefs.edit().putBoolean("updates_enabled", false).apply()
                Toast.makeText(this, "🔕 Notifications बंद कर दी गईं", Toast.LENGTH_SHORT).show()
            } else {
                FirebaseMessaging.getInstance().subscribeToTopic("all_updates").addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        prefs.edit().putBoolean("updates_enabled", true).apply()
                        Toast.makeText(this, "🔔 नई Updates की notifications चालू हैं", Toast.LENGTH_SHORT).show()
                    } else Toast.makeText(this, "Notifications चालू नहीं हो सकीं", Toast.LENGTH_SHORT).show()
                }
                if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf("android.permission.POST_NOTIFICATIONS"), 1001)
                }
            }
        }
        findViewById<TextView>(R.id.shareButton).setOnClickListener { shareApp() }
        findViewById<TextView>(R.id.homeNav).setOnClickListener { window.decorView.findViewById<View>(android.R.id.content).scrollTo(0, 0) }
        findViewById<TextView>(R.id.jobsNav).setOnClickListener { openInApp(vacancy) }
        findViewById<TextView>(R.id.updatesNav).setOnClickListener {
            loadPosts()
            findViewById<android.widget.ScrollView>(R.id.contentScroll).post {
                findViewById<View>(R.id.postsRecycler).requestFocus()
                findViewById<android.widget.ScrollView>(R.id.contentScroll).smoothScrollTo(0, findViewById<View>(R.id.postsRecycler).top)
            }
        }
        findViewById<TextView>(R.id.menuNav).setOnClickListener { showMenu() }
        loadPosts()
        startLocalChannelUnreadTracker()
    }

    private fun startLocalChannelUnreadTracker() {
        val prefs = getSharedPreferences("sr_notifications", MODE_PRIVATE)
        val initialized = prefs.getBoolean("channel_unread_initialized", false)
        channelPostsListener?.remove()
        channelPostsListener = FirebaseFirestore.getInstance().collection("channel_posts")
            .orderBy("createdAt", Query.Direction.DESCENDING).limit(100)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null || snap.isEmpty) return@addSnapshotListener
                val newest = snap.documents.mapNotNull { it.getLong("createdAt") }.maxOrNull() ?: return@addSnapshotListener
                if (!initialized && !channelBaselineReady) {
                    prefs.edit().putLong("channel_last_seen_at", newest).putBoolean("channel_unread_initialized", true).putInt("channel_unread", 0).apply()
                    channelBaselineReady = true
                    updateChannelBadge()
                    return@addSnapshotListener
                }
                channelBaselineReady = true
                if (prefs.getBoolean("channel_open", false)) {
                    prefs.edit().putInt("channel_unread", 0).putLong("channel_last_seen_at", newest).apply()
                    updateChannelBadge()
                    return@addSnapshotListener
                }
                val lastSeen = prefs.getLong("channel_last_seen_at", 0L)
                val unread = snap.documents.count { (it.getLong("createdAt") ?: 0L) > lastSeen }
                if (unread > 0) {
                    prefs.edit().putInt("channel_unread", unread).apply()
                    updateChannelBadge()
                }
            }
    }

    private fun updateChannelBadge() {
        val badge = findViewById<TextView>(R.id.channelBadge)
        val count = getSharedPreferences("sr_notifications", MODE_PRIVATE).getInt("channel_unread", 0)
        badge.text = if (count > 99) "99+" else count.toString()
        badge.visibility = if (count > 0) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) updateChannelBadge()
        liveHandler.removeCallbacks(liveTickerRunnable)
        liveHandler.post(liveTickerRunnable)
    }

    override fun onDestroy() {
        channelPostsListener?.remove()
        channelPostsListener = null
        try { unregisterReceiver(unreadReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onPause() {
        liveHandler.removeCallbacks(liveTickerRunnable)
        super.onPause()
    }

    private fun setupFirebaseNotifications() {
        FirebaseMessaging.getInstance().subscribeToTopic("all_updates")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm=getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel("shiksha_updates","Shiksha Rojgar Updates",NotificationManager.IMPORTANCE_HIGH))
            nm.createNotificationChannel(NotificationChannel("shiksha_channel","Shiksha Rojgar Channel",NotificationManager.IMPORTANCE_HIGH))
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,arrayOf("android.permission.POST_NOTIFICATIONS"),1001)
        }
    }

    private fun setupLiveBreaking() {
        val live = findViewById<TextView>(R.id.liveLabel)
        val blink = AlphaAnimation(0.25f, 1.0f).apply {
            duration = 650L
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        live.clearAnimation()
        live.startAnimation(blink)
        findViewById<TextView>(R.id.ticker).postDelayed({ animateTicker() }, 500L)
    }

    private fun roundedGradient(start: String, end: String): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor(start), Color.parseColor(end))
    ).apply { cornerRadius = 24f }

    private fun card(label: String, emoji: String, colors: Pair<String,String>, action: () -> Unit): TextView = TextView(this).apply {
        text = "$emoji\n$label"
        gravity = Gravity.CENTER
        textSize = 14f
        setTextColor(Color.WHITE)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        background = roundedGradient(colors.first, colors.second)
        setPadding(8, 14, 8, 14)
        isClickable = true
        isFocusable = true
        elevation = 5f
        setOnClickListener { action() }
        layoutParams = GridLayout.LayoutParams().apply {
            width = 0; height = 126
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(5, 5, 5, 5)
        }
    }

    private fun openManagedPage(pageId: String, title: String) {
        startActivity(Intent(this, ContentPageActivity::class.java).apply {
            putExtra("pageId", pageId)
            putExtra("pageTitle", title)
        })
    }

    private fun quickCard(label: String, emoji: String, colors: Pair<String, String>, action: () -> Unit): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        background = roundedGradient(colors.first, colors.second)
        elevation = 5f
        isClickable = true
        isFocusable = true
        setPadding(4, 7, 4, 7)
        val icon = TextView(this@MainActivity).apply {
            text = emoji
            textSize = 32f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }
        addView(icon, LinearLayout.LayoutParams(-1, 90))
        val title = TextView(this@MainActivity).apply {
            text = label
            textSize = 12.5f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        addView(title, LinearLayout.LayoutParams(-1, 60))
        setOnClickListener { action() }
        layoutParams = GridLayout.LayoutParams().apply {
            width = 0
            height = 176
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(2, 3, 2, 3)
        }
    }

    private fun setupQuickCards() {
        val grid = findViewById<GridLayout>(R.id.quickGrid)
        val items = listOf(
            Triple("Teacher", "👨‍🏫", { openInApp("https://www.shiksharojgar.com/2026/03/teacher-govt-employees.html") }),
            Triple("Student", "👨‍🎓", { openInApp("https://www.shiksharojgar.com/2026/03/College%20%20University%20Students.html") }),
            Triple("School", "🏫", { openInApp("https://www.shiksharojgar.com/2026/03/school-students-1-12-section-page.html") }),
            Triple("Vacancy", "💼", { openInApp("https://www.shiksharojgar.com/2026/03/latest-jobs-page.html") }),
            Triple("Result", "🏆", { openInApp("https://www.shiksharojgar.com/2026/03/results.html") }),
            Triple("Admit Card", "🎫", { openInApp("https://www.shiksharojgar.com/2026/01/admit-card-download-zone.html") })
        )
        val colors = listOf("#0D5BD7" to "#18A0FB", "#7C3AED" to "#EC4899", "#059669" to "#14B8A6", "#F97316" to "#EF4444", "#DB2777" to "#7C3AED", "#D97706" to "#FACC15")
        items.forEachIndexed { i, item -> grid.addView(quickCard(item.first, item.second, colors[i], item.third)) }

        val managed = findViewById<GridLayout>(R.id.learningGrid)
        val pages: List<Triple<String, String, () -> Unit>> = listOf(
            Triple("Syllabus", "🎓", { openManagedPage("syllabus", "Syllabus") }),
            Triple("Notices", "📢", { openManagedPage("notices", "Notices") }),
            Triple("Career Guide", "📚", { openManagedPage("career", "Career Guide") }),
            Triple("Useful Tools", "⚙️", { openManagedPage("tools", "Useful Tools") })
        )
        pages.forEach { (label, emoji, action) ->
            managed.addView(TextView(this).apply {
                text = "$emoji\n$label"; gravity = Gravity.CENTER; textSize = 10.5f; setTextColor(Color.rgb(30,41,59)); typeface = android.graphics.Typeface.DEFAULT_BOLD
                background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = 20f; setStroke(2, Color.rgb(226,232,240)) }
                setPadding(3, 10, 3, 10); elevation = 3f; isClickable = true; isFocusable = true; setOnClickListener { action() }
                layoutParams = GridLayout.LayoutParams().apply { width=0; height=118; columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f); setMargins(3,4,3,4) }
            })
        }
    }

    private fun setupWebsites() {
        val grid = findViewById<GridLayout>(R.id.websiteGrid)
        grid.addView(websiteCard("Shiksha Rojgar.com", "🎓", shiksha, "#075985", "#0EA5E9", R.drawable.logo, external = false))
        grid.addView(websiteCard("VacancyBazaar.in", "🚀", vacancy, "#E11D48", "#F59E0B", R.drawable.vacancy_logo))
    }

    private fun websiteCard(label: String, emoji: String, url: String, c1: String, c2: String, icon: Int, external: Boolean = false): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = roundedGradient(c1, c2)
        elevation = 6f
        setPadding(10, 10, 10, 10)
        val logoView = ImageView(this@MainActivity).apply {
            setImageResource(icon)
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(48, 48)
        }
        addView(logoView)
        val text = TextView(this@MainActivity).apply {
            this.text = "$emoji  $label"
            gravity = Gravity.CENTER_VERTICAL
            textSize = 17f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(10, 0, 6, 0)
        }
        addView(text, LinearLayout.LayoutParams(0, 60, 1f))

        val openButton = TextView(this@MainActivity).apply {
            this.text = "OPEN  ↗"
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.argb(55, 255, 255, 255))
                cornerRadius = 18f
                setStroke(1, Color.argb(130, 255, 255, 255))
            }
            setPadding(14, 0, 14, 0)
            isClickable = true
            isFocusable = true
            setOnClickListener { AnalyticsTracker.websiteOpen(this@MainActivity, label); if (external) openExternal(url) else openInApp(url) }
        }
        addView(openButton, LinearLayout.LayoutParams(108, 52).apply {
            gravity = Gravity.CENTER_VERTICAL
        })
        setOnClickListener { AnalyticsTracker.websiteOpen(this@MainActivity, label); if (external) openExternal(url) else openInApp(url) }
        layoutParams = GridLayout.LayoutParams().apply { width=0; height=112; columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f); setMargins(5,5,5,5) }
    }

    private fun social(label: String, emoji: String, url: String, c1: String, c2: String): TextView = card(label, emoji, c1 to c2) { openExternal(url) }

    private fun setupSocials() {
        val grid = findViewById<GridLayout>(R.id.socialGrid)
        val items = listOf(
            Triple("WhatsApp", "🟢", whatsapp) to ("#16A34A" to "#22C55E"),
            Triple("Telegram", "✈️", telegram) to ("#0284C7" to "#38BDF8"),
            Triple("YouTube", "▶️", youtube) to ("#DC2626" to "#F43F5E"),
            Triple("Facebook", "🔵", facebook) to ("#2563EB" to "#60A5FA"),
            Triple("Instagram", "📸", instagram) to ("#F97316" to "#A855F7"),
            Triple("Twitter / X", "𝕏", twitter) to ("#111827" to "#374151"),
            Triple("ShareChat", "💬", sharechat) to ("#F97316" to "#EF4444"),
            Triple("Arattai", "💠", arattai) to ("#4F46E5" to "#06B6D4"),
            Triple("Official Website", "🌐", shiksha) to ("#0284C7" to "#0EA5E9")
        )
        items.forEach { (item, colors) -> grid.addView(social(item.first, item.second, item.third, colors.first, colors.second)) }
    }

    private fun loadPosts(done: (() -> Unit)? = null) {
        findViewById<TextView>(R.id.ticker).text = "Shiksha Rojgar की नई पोस्ट लोड हो रही हैं…"
        animateTicker()
        FeedLoader.load { posts ->
            adapter.submit(posts)
            val latest = posts.take(8)
            val title = if (latest.isNotEmpty()) {
                latest.mapIndexed { index, post -> "${index + 1}. ${post.title}" }
                    .joinToString("     ✦     ")
            } else {
                "अभी नई पोस्ट नहीं मिली — Refresh करें"
            }
            // Duplicate the complete sequence. This keeps the ticker populated while the animation loops.
            val tickerText = "$title     ✦     $title     ✦     $title"
            findViewById<TextView>(R.id.ticker).text = tickerText
            animateTicker()
            findViewById<TextView>(R.id.ticker).postDelayed({ animateTicker() }, 350L)
            done?.invoke()
        }
    }

    private fun animateTicker() {
        val ticker = findViewById<TextView>(R.id.ticker)
        ticker.post {
            ticker.clearAnimation()
            val parentWidth = (ticker.parent as View).width.toFloat().coerceAtLeast(300f)
            val textWidth = ticker.paint.measureText(ticker.text.toString()).coerceAtLeast(parentWidth * .35f)
            val from = parentWidth
            val to = -textWidth
            TranslateAnimation(from, to, 0f, 0f).apply {
                duration = 90000L
                repeatCount = Animation.INFINITE
                repeatMode = Animation.RESTART
                setInterpolator(android.view.animation.LinearInterpolator())
                ticker.startAnimation(this)
            }
        }
    }

    private fun shareApp() {
        val text = "📱 Shiksha Rojgar App\nशिक्षा, रोजगार, शिक्षक, विद्यार्थी, Vacancy, Result और Admit Card अपडेट\n\nhttps://shiksha-rojgar.web.app/channel"
        AnalyticsTracker.appShare(this)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }, "Share Shiksha Rojgar"))
    }

    private fun openInApp(url: String) { startActivity(Intent(this, WebViewActivity::class.java).putExtra("url", url)) }
    private fun openExternal(url: String) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }

    private fun showSearchHelp() {
        val input = android.widget.EditText(this)
        input.hint = "जैसे Teacher, SSC, Result"
        AlertDialog.Builder(this).setTitle("🔍 Search Shiksha Rojgar").setView(input).setPositiveButton("Search") { _, _ ->
            val query = input.text.toString().trim()
            if (query.isNotEmpty()) openInApp("$shiksha/search?q=${Uri.encode(query)}")
        }.setNegativeButton("Cancel", null).show()
    }

    private fun showMenu() {
        val items = arrayOf("👨‍🏫 Teacher", "👨‍🎓 Student", "🏫 School", "💼 Vacancy", "🎫 Admit Card", "🏆 Result", "🌐 Shiksha Rojgar", "🚀 VacancyBazaar", "🟢 WhatsApp", "✈️ Telegram", "▶️ YouTube", "📸 Instagram")
        AlertDialog.Builder(this).setTitle("☰ Shiksha Rojgar Menu").setItems(items) { _, which -> when(which) {
            0 -> openInApp("https://www.shiksharojgar.com/2026/03/teacher-govt-employees.html")
            1 -> openInApp("https://www.shiksharojgar.com/2026/03/College%20%20University%20Students.html")
            2 -> openInApp("https://www.shiksharojgar.com/2026/03/school-students-1-12-section-page.html")
            3 -> openInApp("https://www.shiksharojgar.com/2026/03/latest-jobs-page.html")
            4 -> openInApp("https://www.shiksharojgar.com/2026/01/admit-card-download-zone.html")
            5 -> openInApp("https://www.shiksharojgar.com/2026/03/results.html")
            7 -> openInApp(vacancy)
            6 -> openInApp(shiksha)
            8 -> openExternal(whatsapp); 9 -> openExternal(telegram); 10 -> openExternal(youtube); 11 -> openExternal(instagram)
        }}.show()
    }
}
