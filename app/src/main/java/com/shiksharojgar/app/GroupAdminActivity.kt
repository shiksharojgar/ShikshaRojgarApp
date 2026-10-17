package com.shiksharojgar.app

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class GroupAdminActivity : AppCompatActivity() {

    private lateinit var listBox: LinearLayout
    private lateinit var statusView: TextView

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(buildScreen())
        loadPendingPosts()
    }

    private fun buildScreen(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(248, 250, 252))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.rgb(7, 89, 133))
        }

        val backButton = TextView(this).apply {
            text = "←"
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setOnClickListener { finish() }
        }

        header.addView(
            backButton,
            LinearLayout.LayoutParams(dp(48), dp(48))
        )

        val title = TextView(this).apply {
            text = "🛡️ ग्रुप पोस्ट समीक्षा"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
        }

        header.addView(
            title,
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )

        root.addView(header)

        statusView = TextView(this).apply {
            text = "Pending पोस्ट लोड हो रही हैं..."
            textSize = 15f
            setTextColor(Color.DKGRAY)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

        root.addView(statusView)

        val scroll = ScrollView(this)

        listBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(4), dp(12), dp(24))
        }

        scroll.addView(listBox)

        root.addView(
            scroll,
            LinearLayout.LayoutParams( -1, 0, 1f)
        )

        val refreshButton = TextView(this).apply {
            text = "🔄 सूची दोबारा लोड करें"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(25, 118, 210))
            setPadding(dp(12), dp(14), dp(12), dp(14))
            setOnClickListener { loadPendingPosts() }
        }

        root.addView(
            refreshButton,
            LinearLayout.LayoutParams(-1, -2)
        )

        return root
    }

    private fun loadPendingPosts() {
        listBox.removeAllViews()
        statusView.text = "Pending पोस्ट लोड हो रही हैं..."

        GroupRepository.pendingPosts(
            onResult = { posts ->
                listBox.removeAllViews()

                statusView.text = "कुल Pending पोस्ट: ${posts.size}"

                if (posts.isEmpty()) {
                    showMessage("अभी समीक्षा के लिए कोई पोस्ट नहीं है।")
                    return@pendingPosts
                }

                posts.forEach { post ->
                    addPostCard(post)
                }
            },
            onError = { error ->
                listBox.removeAllViews()
                statusView.text = "पोस्ट लोड नहीं हो सकीं।"

                showMessage(
                    "अनुमति नहीं मिली या नेटवर्क समस्या है। " +
                        "Admin अधिकार और Firestore Rules जाँचें।"
                )
            }
        )
    }

    private fun addPostCard(post: GroupPost) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(Color.WHITE)
        }

        val name = TextView(this).apply {
            text = "👤 ${post.displayName}"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(7, 89, 133))
        }

        card.addView(name)

        val body = TextView(this).apply {
            text = post.body
            textSize = 16f
            setTextColor(Color.rgb(31, 41, 55))
            setPadding(0, dp(10), 0, dp(12))
            autoLinkMask = android.text.util.Linkify.WEB_URLS
            movementMethod =
                android.text.method.LinkMovementMethod.getInstance()
        }

        card.addView(body)

        val date = TextView(this).apply {
            text = if (post.createdAt > 0L) {
                "पोस्ट समय: " + java.text.SimpleDateFormat(
                    "dd-MM-yyyy hh:mm a",
                    java.util.Locale("hi", "IN")
                ).format(java.util.Date(post.createdAt))
            } else {
                "पोस्ट समय उपलब्ध नहीं"
            }

            textSize = 12f
            setTextColor(Color.GRAY)
        }

        card.addView(date)

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }

        val approveButton = TextView(this).apply {
            text = "✅ मंजूर करें"
            gravity = Gravity.CENTER
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(dp(10), dp(12), dp(10), dp(12))
            setBackgroundColor(Color.rgb(22, 163, 74))
            setOnClickListener {
                showExpiryOptions(post)
            }
        }

        val rejectButton = TextView(this).apply {
            text = "❌ रिजेक्ट"
            gravity = Gravity.CENTER
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(dp(10), dp(12), dp(10), dp(12))
            setBackgroundColor(Color.rgb(185, 28, 28))
            setOnClickListener {
                confirmReject(post)
            }
        }

        buttons.addView(
            approveButton,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        buttons.addView(
            rejectButton,
            LinearLayout.LayoutParams(0, -2, 1f).apply {
                marginStart = dp(8)
            }
        )

        card.addView(buttons)

        listBox.addView(
            card,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(12)
            }
        )
    }

    private fun showExpiryOptions(post: GroupPost) {
        val options = arrayOf(
            "7 दिन",
            "8 दिन",
            "30 दिन",
            "अपनी अवधि चुनें"
        )

        AlertDialog.Builder(this)
            .setTitle("पोस्ट कितने दिन दिखे?")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> approve(post, 7)
                    1 -> approve(post, 8)
                    2 -> approve(post, 30)
                    3 -> showCustomExpiry(post)
                }
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun showCustomExpiry(post: GroupPost) {
        val input = android.widget.EditText(this).apply {
            hint = "दिन लिखें (1 से 365)"
            inputType =
                android.text.InputType.TYPE_CLASS_NUMBER
        }

        AlertDialog.Builder(this)
            .setTitle("अपनी अवधि चुनें")
            .setView(input)
            .setPositiveButton("मंजूर करें") { _, _ ->
                val days = input.text.toString().toIntOrNull()

                if (days == null || days !in 1..365) {
                    showMessage("1 से 365 के बीच दिन लिखें।")
                } else {
                    approve(post, days)
                }
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun approve(post: GroupPost, days: Int) {
        GroupRepository.approvePost(post.id, days) { success, error ->
            if (success) {
                showMessage("पोस्ट $days दिन के लिए मंजूर हो गई।")
                loadPendingPosts()
            } else {
                showMessage(error ?: "पोस्ट मंजूर नहीं हुई।")
            }
        }
    }

    private fun confirmReject(post: GroupPost) {
        AlertDialog.Builder(this)
            .setTitle("पोस्ट रिजेक्ट करें?")
            .setMessage("यह पोस्ट सार्वजनिक ग्रुप में नहीं दिखाई जाएगी।")
            .setPositiveButton("रिजेक्ट करें") { _, _ ->
                GroupRepository.rejectPost(post.id) { success, error ->
                    if (success) {
                        showMessage("पोस्ट रिजेक्ट कर दी गई।")
                        loadPendingPosts()
                    } else {
                        showMessage(error ?: "पोस्ट रिजेक्ट नहीं हुई।")
                    }
                }
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}
