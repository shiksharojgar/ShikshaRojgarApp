package com.shiksharojgar.app

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging
import java.text.SimpleDateFormat
import java.util.*

class ChannelActivity : AppCompatActivity() {
    private lateinit var list: LinearLayout
    private lateinit var followBtn: TextView
    private lateinit var followerCountView: TextView
    private lateinit var commentStatusView: TextView
    private lateinit var feedScroll: ScrollView
    private var globalComments = true
    private val posts = mutableListOf<ChannelPost>()
    private val likedState = mutableMapOf<String, Boolean>()
    private val likeOverrides = mutableMapOf<String, Long>()
    private val viewOverrides = mutableMapOf<String, Long>()
    private val shareOverrides = mutableMapOf<String, Long>()
    private val viewedSession = mutableSetOf<String>()
    private var followerCount = 0L
    private var followBusy = false
    private var firstFeedRender = true
    private var followingState = false
    private var pendingPostId: String? = null
    private val fmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("hi", "IN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(makeUi())
        pendingPostId = intent.data?.let { uri ->
            uri.getQueryParameter("post") ?: uri.lastPathSegment?.takeIf { uri.path?.contains("/post") == true }
        }
        getSharedPreferences("sr_notifications", MODE_PRIVATE).edit().putInt("channel_unread", 0).putBoolean("channel_open", true).putLong("channel_last_seen_at", System.currentTimeMillis()).putBoolean("channel_unread_initialized", true).apply()
        // Channel content is public and must remain visible even if anonymous sign-in is not enabled.
        // Authentication is requested only when the user follows/comments.
        ChannelRepository.config { count, comments ->
            globalComments = comments
            followerCount = count.coerceAtLeast(0L)
            followerCountView.text = "👥 $followerCount Followers"
            commentStatusView.text = if (comments) "💬 Comments ON" else "🔒 Comments OFF"
            renderPosts()
        }
        ChannelRepository.posts({ p ->
            val oldNewest = posts.lastOrNull()?.id
            posts.clear(); posts.addAll(p)
            renderPosts()
            val newNewest = posts.lastOrNull()?.id
            if (firstFeedRender || oldNewest != newNewest) scrollToNewest()
            pendingPostId?.let { id ->
                val index = posts.indexOfFirst { it.id == id }
                if (index >= 0) scrollToPost(index)
                pendingPostId = null
            }
            firstFeedRender = false
        }, {})
        ChannelRepository.ensureSignedIn { connected ->
            if (connected) {
                ChannelRepository.isFollowing { following ->
                    updateFollow(following)
                    if (following) FirebaseMessaging.getInstance().subscribeToTopic("all_channel_followers")
                    else FirebaseMessaging.getInstance().unsubscribeFromTopic("all_channel_followers")
                }
            } else {
                // Do not block channel reading; only follow/comment needs sign-in.
                updateFollow(false)
            }
        }
    }

    override fun onDestroy() {
        getSharedPreferences("sr_notifications", MODE_PRIVATE).edit().putBoolean("channel_open", false).apply()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingPostId = intent.data?.let { uri ->
            uri.getQueryParameter("post") ?: uri.lastPathSegment?.takeIf { uri.path?.contains("/post") == true }
        }
        if (pendingPostId != null && posts.isNotEmpty()) {
            val index = posts.indexOfFirst { it.id == pendingPostId }
            if (index >= 0) scrollToPost(index)
        }
    }

    private fun scrollToPost(index: Int) {
        if (!::feedScroll.isInitialized || !::list.isInitialized) return
        val child = list.getChildAt(index.coerceIn(0, (list.childCount - 1).coerceAtLeast(0)))
        if (child != null) feedScroll.post { feedScroll.smoothScrollTo(0, child.top) }
    }

    private fun makeUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(245, 248, 252))
        }

        // Back + Home + Share are deliberately kept close to the Channel header.
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(6, 6, 6, 6)
            setBackgroundColor(Color.rgb(6, 59, 122))
        }
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(6, top + 6, 6, 6)
            insets
        }
        toolbar.addView(toolButton("←  Back") { finish() }, LinearLayout.LayoutParams(72, 58))
        toolbar.addView(TextView(this).apply {
            text = "📢  SHIKSHA ROJGAR CHANNEL"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 0, 4, 0)
        }, LinearLayout.LayoutParams(0, 58, 1f))
        toolbar.addView(toolButton("⌂  Home") { goHome() }, LinearLayout.LayoutParams(74, 58))
        toolbar.addView(toolButton("↗  Share") { shareChannel() }, LinearLayout.LayoutParams(82, 58))
        root.addView(toolbar, LinearLayout.LayoutParams(-1, -2))

        val head = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 18)
            background = GradientFactory.gradient("#075985", "#0EA5E9")
        }
        head.addView(TextView(this).apply {
            text = "📢  शिक्षा रोजगार चैनल"
            textSize = 23f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        })
        head.addView(TextView(this).apply {
            text = "सरकारी आदेश • निर्देश • टाइम टेबल • शिक्षा एवं रोजगार अपडेट"
            textSize = 13f
            setTextColor(Color.WHITE)
            setPadding(0, 4, 0, 0)
        })
        followerCountView = TextView(this).apply {
            text = "👥 Followers"
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(0, 9, 0, 0)
        }
        head.addView(followerCountView)
        root.addView(head)

        // Large, attractive Follow control below the channel header.
        val followArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(14, 12, 14, 12)
            background = Color.WHITE.toDrawable()
            elevation = 5f
        }
        followBtn = TextView(this).apply {
            text = "➕  FOLLOW CHANNEL"
            gravity = Gravity.CENTER
            textSize = 15f
            includeFontPadding = false
            maxLines = 1
            ellipsize = null
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = GradientFactory.gradient("#16A34A", "#22C55E")
            setPadding(28, 16, 28, 16)
            isClickable = true
            isFocusable = true
            setOnClickListener { toggleFollow() }
        }
        followArea.addView(followBtn, LinearLayout.LayoutParams(-1, 64).apply {
            setMargins(4, 0, 4, 0)
        })

        val infoRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 0)
        }
        commentStatusView = TextView(this).apply {
            text = "💬 Comments"
            textSize = 12f
            setTextColor(Color.rgb(75, 85, 99))
            gravity = Gravity.CENTER
        }
        infoRow.addView(commentStatusView)
        followArea.addView(infoRow)
        // Follow button is placed at the bottom, immediately above Back/Home/Share.

        // Clearly visible offline section.
        val offlineRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(10, 8, 10, 8)
        }
        offlineRow.addView(TextView(this).apply {
            text = "📥 Offline सामग्री"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(25, 35, 50))
        }, LinearLayout.LayoutParams(0, 48, 1f))
        offlineRow.addView(actionButton("📂  OPEN OFFLINE", "#7C3AED", "#A855F7") { showOfflineList() }, LinearLayout.LayoutParams(160, 48))
        root.addView(offlineRow)

        feedScroll = ScrollView(this)
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10, 4, 10, 20)
        }
        feedScroll.addView(list)
        root.addView(feedScroll, LinearLayout.LayoutParams(-1, 0, 1f))

        // Bottom navigation: Back | Home | Share, so these actions remain available while reading.
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(7, 5, 7, 7)
            setBackgroundColor(Color.WHITE)
            elevation = 16f
        }
        bottom.addView(bottomButton("←  Back") { finish() }, LinearLayout.LayoutParams(0, 56, 1f))
        bottom.addView(bottomButton("⌂  Home") { goHome() }, LinearLayout.LayoutParams(0, 56, 1f))
        bottom.addView(bottomButton("↗  Share") { shareChannel() }, LinearLayout.LayoutParams(0, 56, 1f))
        ViewCompat.setOnApplyWindowInsetsListener(bottom) { view, insets ->
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(7, 5, 7, nav + 7)
            insets
        }
        root.addView(followArea, LinearLayout.LayoutParams(-1, -2))
        root.addView(bottom, LinearLayout.LayoutParams(-1, -2))
        return root
    }

    private fun toolButton(label: String, action: () -> Unit): TextView = TextView(this).apply {
        text = label
        gravity = Gravity.CENTER
        textSize = 11f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        background = GradientFactory.rounded("#174E86")
        isClickable = true
        isFocusable = true
        setOnClickListener { action() }
    }

    private fun bottomButton(label: String, action: () -> Unit): TextView = TextView(this).apply {
        text = label
        gravity = Gravity.CENTER
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.rgb(14, 91, 215))
        background = Color.WHITE.toDrawable()
        isClickable = true
        isFocusable = true
        setOnClickListener { action() }
    }

    private fun actionButton(label: String, a: String, b: String, action: () -> Unit): TextView = TextView(this).apply {
        text = label
        gravity = Gravity.CENTER
        textSize = 11f
        includeFontPadding = false
        maxLines = 1
        gravity = Gravity.CENTER
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        background = GradientFactory.gradient(a, b)
        isClickable = true
        isFocusable = true
        setOnClickListener { action() }
    }

    private fun toggleFollow() {
        if (followBusy) return
        val target = !followingState
        // Immediate local response: UI never waits for Firestore/network.
        followingState = target
        updateFollow(target)
        followBtn.isEnabled = false
        followBusy = true
        ChannelRepository.ensureSignedIn { connected ->
            if (!connected) {
                runOnUiThread {
                    followingState = !target
                    updateFollow(followingState)
                    followBtn.isEnabled = true
                    followBusy = false
                    Toast.makeText(this, "Follow के लिए connection नहीं हो पाया", Toast.LENGTH_SHORT).show()
                }
                return@ensureSignedIn
            }
            ChannelRepository.follow(target) { ok ->
                runOnUiThread {
                    followBusy = false
                    followBtn.isEnabled = true
                    if (ok) {
                        followerCount = (followerCount + if (target) 1L else -1L).coerceAtLeast(0L)
                        followerCountView.text = "👥 $followerCount Followers"
                        AnalyticsTracker.channelFollow(this, target)
                        if (target) FirebaseMessaging.getInstance().subscribeToTopic("all_channel_followers")
                        else FirebaseMessaging.getInstance().unsubscribeFromTopic("all_channel_followers")
                        Toast.makeText(this, if (target) "Channel Follow हो गया" else "Follow हटाया गया", Toast.LENGTH_SHORT).show()
                    } else {
                        followingState = !target
                        updateFollow(followingState)
                        Toast.makeText(this, "Follow अपडेट नहीं हुआ", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updateFollow(v: Boolean) {
        followingState = v
        followBtn.text = if (v) "✓  Followed" else "➕  Follow करें"
        if (v) {
            followBtn.setTextColor(Color.rgb(107,114,128))
            followBtn.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.argb(35, 107, 114, 128)); cornerRadius = 26f
                setStroke(1, Color.argb(70, 107, 114, 128))
            }
        } else {
            followBtn.setTextColor(Color.WHITE)
            followBtn.background = GradientFactory.gradient("#16A34A", "#06B6D4")
        }
    }

    private fun renderPosts() {
        if (!::list.isInitialized) return
        list.removeAllViews()
        if (posts.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "अभी चैनल में कोई पोस्ट नहीं है।"
                textSize = 16f
                setPadding(16, 24, 16, 24)
            })
            return
        }
        posts.forEach { p ->
            list.addView(postView(p))
            if (p != posts.last()) list.addView(View(this).apply { setBackgroundColor(Color.rgb(30, 136, 229)) }, LinearLayout.LayoutParams(-1, 7).apply { setMargins(0, 0, 0, 0) })
            if (viewedSession.add(p.id)) {
                viewOverrides[p.id] = (viewOverrides[p.id] ?: p.viewCount) + 1L
                AnalyticsTracker.uniquePostView(this, p.id)
            }
        }
    }

    private fun scrollToNewest() {
        if (!::feedScroll.isInitialized) return
        feedScroll.post { feedScroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun postView(p: ChannelPost): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientFactory.roundedWhite()
            setPadding(14, 14, 14, 12)
            elevation = 3f
        }
        box.addView(TextView(this).apply {
            text = "${p.category}  •  ${if (p.createdAt > 0) fmt.format(Date(p.createdAt)) else ""}"
            textSize = 11f
            setTextColor(Color.rgb(14, 91, 215))
        })
        box.addView(TextView(this).apply {
            text = p.title
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(25, 35, 50))
            setPadding(0, 5, 0, 3)
        })
        if (p.body.isNotBlank()) box.addView(TextView(this).apply {
            text = p.body
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 0, 0, 8)
        })
        if (p.imageUrl.isNotBlank()) {
            val imageBox = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 6, 0, 8)
            }
            val image = ImageView(this).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setBackgroundColor(Color.rgb(241, 245, 249))
                minimumHeight = 180
                setOnClickListener {
                    ChannelRepository.openMedia(this@ChannelActivity, p.imageUrl, p.imageMime, "image_${p.id}.jpg") { ok, err ->
                        if (!ok) Toast.makeText(this@ChannelActivity, err ?: "Image नहीं खुली", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            imageBox.addView(image, LinearLayout.LayoutParams(-1, 340))
            val hint = TextView(this).apply {
                text = "🖼️ बड़ा करने / पूरा खोलने के लिए फोटो पर क्लिक करें (फोटो गैलरी में खुलेगा)"
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.rgb(7, 89, 133))
                setPadding(0, 4, 0, 6)
                setOnClickListener { image.performClick() }
            }
            imageBox.addView(hint)
            box.addView(imageBox)
            ChannelRepository.loadImage(p.imageUrl) { bitmap, _ ->
                runOnUiThread {
                    if (bitmap != null) {
                        image.setImageBitmap(bitmap)
                        image.scaleType = ImageView.ScaleType.CENTER_CROP
                    } else {
                        hint.text = "🖼️ फोटो खोलने के लिए यहाँ दबाएँ"
                    }
                }
            }
        }
        if (p.fileUrl.isNotBlank()) box.addView(TextView(this).apply {
            text = "📄 ${p.fileName.ifBlank { "PDF/Document" }}  •  OPEN"
            textSize = 13f
            setTextColor(Color.rgb(14, 91, 215))
            setPadding(0, 5, 0, 5)
            setOnClickListener {
                ChannelRepository.openMedia(this@ChannelActivity, p.fileUrl, p.fileMime, p.fileName.ifBlank { "document.pdf" }) { ok, err ->
                    if (!ok) Toast.makeText(this@ChannelActivity, err ?: "PDF नहीं खुली", Toast.LENGTH_SHORT).show()
                }
            }
        })
        val actions = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 7, 0, 0)
        }
        val currentLiked = likedState[p.id] ?: false
        val initialLikeCount = likeOverrides[p.id] ?: p.likeCount
        val like = TextView(this).apply {
            text = if (currentLiked) "👍 Liked $initialLikeCount" else "👍 Like $initialLikeCount"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (currentLiked) Color.WHITE else Color.rgb(14, 91, 215))
            background = if (currentLiked) GradientFactory.gradient("#2563EB", "#38BDF8") else Color.WHITE.toDrawable()
            setPadding(10, 7, 12, 7)
            setOnClickListener {
                ChannelRepository.ensureSignedIn { connected ->
                    if (!connected) { Toast.makeText(this@ChannelActivity, "Like के लिए sign-in नहीं हो पाया", Toast.LENGTH_SHORT).show(); return@ensureSignedIn }
                    ChannelRepository.isLiked(p.id) { liked ->
                        val target = !liked
                        likedState[p.id] = target
                        val base = likeOverrides[p.id] ?: p.likeCount
                        likeOverrides[p.id] = (base + if (target) 1L else -1L).coerceAtLeast(0L)
                        // Instant visual reaction before Firestore finishes.
                        text = if (target) "👍 Liked ${likeOverrides[p.id] ?: 0L}" else "👍 Like ${likeOverrides[p.id] ?: 0L}"
                        setTextColor(if (target) Color.WHITE else Color.rgb(14, 91, 215))
                        background = if (target) GradientFactory.gradient("#2563EB", "#38BDF8") else Color.WHITE.toDrawable()
                        ChannelRepository.like(p.id, target) { ok ->
                            if (!ok) {
                                likedState[p.id] = liked
                                likeOverrides[p.id] = p.likeCount
                                runOnUiThread { val rollbackCount = likeOverrides[p.id] ?: p.likeCount; text = if (liked) "👍 Liked $rollbackCount" else "👍 Like $rollbackCount"; setTextColor(if (liked) Color.WHITE else Color.rgb(14, 91, 215)); background = if (liked) GradientFactory.gradient("#2563EB", "#38BDF8") else Color.WHITE.toDrawable() }
                                Toast.makeText(this@ChannelActivity, "Like अपडेट नहीं हुआ", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
        val comments = TextView(this).apply {
            text = "💬 ${p.commentCount}"
            textSize = 13f
            setPadding(6, 7, 12, 7)
            setOnClickListener { showComments(p) }
        }
        val offline = TextView(this).apply {
            text = if (OfflineStore.isSaved(this@ChannelActivity, p.id)) "✓ Offline" else "📥 Offline"
            textSize = 13f
            setTextColor(Color.rgb(14, 91, 215))
            setPadding(6, 7, 12, 7)
            setOnClickListener {
                if (!OfflineStore.isSaved(this@ChannelActivity, p.id)) {
                    text = "⏳ Saving…"
                    ChannelRepository.saveOffline(this@ChannelActivity, p) { ok, err ->
                        runOnUiThread {
                            text = if (ok) { AnalyticsTracker.offlineDownload(this@ChannelActivity, p.id); "✓ Offline Saved" } else "📥 Offline"
                            Toast.makeText(this@ChannelActivity, if (ok) "आदेश Offline सेव हो गया" else "सेव नहीं हुआ: $err", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else openOffline(p)
            }
        }
        actions.addView(like); actions.addView(comments); actions.addView(offline)
        val share = TextView(this).apply {
            text = "↗  SHARE"
            textSize = 14f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = GradientFactory.gradient("#075985", "#2563EB")
            setPadding(16, 7, 16, 7)
            minWidth = 104
            minHeight = 46
            setOnClickListener { sharePost(p) }
        }
        actions.addView(share)
        box.addView(actions)
        val shownLikes = likeOverrides[p.id] ?: p.likeCount
        val shownViews = viewOverrides[p.id] ?: p.viewCount
        val shownShares = shareOverrides[p.id] ?: p.shareCount
        box.addView(TextView(this).apply {
            text = "👁 Views: $shownViews    ↗ Shares: $shownShares    👍 Likes: $shownLikes    💬 Comments: ${p.commentCount}"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(71, 85, 105))
            setPadding(8, 10, 8, 4)
        })
        box.setOnClickListener { /* card itself does not navigate */ }
        return box.apply {
            layoutParams = LinearLayout.LayoutParams(-1, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 18)
            }
        }
    }

    private fun sharePost(p: ChannelPost) {
        shareOverrides[p.id] = (shareOverrides[p.id] ?: p.shareCount) + 1L
        renderPosts()
        AnalyticsTracker.uniquePostShare(this, p.id)
        val web = "https://shiksha-rojgar.web.app/channel?post=${Uri.encode(p.id)}"
        val app = "shiksharojgar://channel/post/${Uri.encode(p.id)}"
        val text = buildString {
            append("📢 शिक्षा रोजगार चैनल\n")
            if (p.title.isNotBlank()) append(p.title)
            if (p.body.isNotBlank()) append("\n\n${p.body}")
            if (p.fileUrl.isNotBlank()) append("\n\n📄 PDF/Document: ${p.fileUrl}")
            append("\n\n🌐 Channel Link: $web")
            append("\n📲 App Link: $app")
        }
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }, "Share Channel Post"))
    }

    private fun showComments(p: ChannelPost) {
        if (!globalComments || !p.commentsEnabled) {
            Toast.makeText(this, "इस पोस्ट पर Comments बंद हैं", Toast.LENGTH_SHORT).show()
            return
        }
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(8, 4, 8, 4) }
        val input = EditText(this).apply { hint = "अपनी टिप्पणी लिखें…" }
        val listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        layout.addView(listBox); layout.addView(input)
        val dialog = AlertDialog.Builder(this).setTitle("💬 Comments").setView(layout).setPositiveButton("Comment", null).setNegativeButton("Close", null).create()
        ChannelRepository.comments(p.id) { cs ->
            listBox.removeAllViews()
            cs.forEach { c -> listBox.addView(TextView(this).apply {
                text = "${c["name"] ?: "उपयोगकर्ता"}: ${c["text"] ?: ""}"
                textSize = 13f
                setPadding(4, 5, 4, 5)
            }) }
        }
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val t = input.text.toString().trim()
                if (t.isNotEmpty()) {
                    ChannelRepository.ensureSignedIn { connected ->
                        if (!connected) {
                            Toast.makeText(this@ChannelActivity, "Comment के लिए sign-in नहीं हो पाया", Toast.LENGTH_SHORT).show()
                            return@ensureSignedIn
                        }
                        ChannelRepository.addComment(p.id, t) { ok ->
                            runOnUiThread {
                                if (ok) input.setText("")
                                else Toast.makeText(this@ChannelActivity, "Comment पोस्ट नहीं हुआ", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
        dialog.show()
    }

    private fun showOfflineList() {
        val saved = OfflineStore.all(this)
        if (saved.isEmpty()) {
            AlertDialog.Builder(this).setTitle("📥 Offline सामग्री").setMessage("अभी कोई सामग्री Offline सेव नहीं है।\nकिसी पोस्ट पर 📥 Offline दबाकर सेव करें।").setPositiveButton("OK", null).show()
            return
        }
        val labels = saved.map { "📄 ${it.second}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("📥 Offline Downloads").setItems(labels) { _, which ->
            val id = saved[which].first
            val p = posts.firstOrNull { it.id == id } ?: ChannelPost(id = id, title = saved[which].second)
            openOffline(p)
        }.setNegativeButton("Close", null).show()
    }

    private fun shareChannel() {
        AnalyticsTracker.channelShare(this)
        // Primary link is a normal HTTPS App Link. Once the website association is
        // published, Android can route it directly into this ChannelActivity.
        val text = "📢 शिक्षा रोजगार चैनल\n" +
                "सरकारी आदेश, निर्देश, टाइम टेबल, शिक्षा एवं रोजगार अपडेट\n\n" +
                "Shiksha Rojgar App\n\n" +
                "🌐 Channel Link (App/Browser):\nhttps://shiksha-rojgar.web.app/channel\n\n" +
                "📲 Direct App Link:\nshiksharojgar://channel\n\n" +
                "🌐 Website: https://www.shiksharojgar.com/"
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }, "Share Channel"))
    }

    private fun goHome() {
        val i = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(i)
        finish()
    }

    private fun open(url: String) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }

    private fun openOffline(p: ChannelPost) {
        val f = ChannelRepository.openOffline(this, p)
        if (f == null) {
            Toast.makeText(this, "Offline फाइल नहीं मिली", Toast.LENGTH_SHORT).show()
            return
        }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(10, 10, 10, 10) }
        val txt = TextView(this).apply {
            text = java.io.File(f, "post.txt").takeIf { it.exists() }?.readText() ?: (p.title + "\n\n" + p.body)
            textSize = 15f
        }
        box.addView(txt)
        val image = java.io.File(f, "image.jpg")
        if (image.exists()) {
            val iv = ImageView(this).apply {
                setImageBitmap(android.graphics.BitmapFactory.decodeFile(image.absolutePath))
                adjustViewBounds = true
                setPadding(0, 10, 0, 10)
            }
            box.addView(iv)
        }
        val pdf = f.listFiles()?.firstOrNull { it.name.lowercase().endsWith(".pdf") }
        if (pdf != null) box.addView(Button(this).apply {
            text = "📄 PDF खोलें"
            setOnClickListener {
                try {
                    val uri = androidx.core.content.FileProvider.getUriForFile(this@ChannelActivity, "com.shiksharojgar.app.fileprovider", pdf)
                    startActivity(Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    })
                } catch (_: Exception) {
                    Toast.makeText(this@ChannelActivity, "PDF खोलने के लिए PDF viewer चाहिए", Toast.LENGTH_LONG).show()
                }
            }
        })
        AlertDialog.Builder(this).setTitle("📥 Offline आदेश").setView(box).setPositiveButton("OK", null).show()
    }
}

private fun Int.toDrawable(): android.graphics.drawable.ColorDrawable = android.graphics.drawable.ColorDrawable(this)

object GradientFactory {
    fun gradient(a: String, b: String) = android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor(a), Color.parseColor(b))).apply { cornerRadius = 26f }
    fun rounded(a: String) = android.graphics.drawable.GradientDrawable().apply { setColor(Color.parseColor(a)); cornerRadius = 26f }
    fun roundedWhite() = android.graphics.drawable.GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = 20f }
}
