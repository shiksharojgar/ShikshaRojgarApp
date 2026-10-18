package com.shiksharojgar.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GroupActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var postsContainer: LinearLayout
    private lateinit var statusView: TextView
    private lateinit var loginButton: Button
    private lateinit var writeButton: Button

    private var feedStarted = false

    private val loginLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                updateLoginState()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(buildScreen())
        updateLoginState()
        loadApprovedPosts()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun buildScreen(): LinearLayout {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(245, 249, 255))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(18), dp(16), dp(18))
            setBackgroundColor(Color.rgb(3, 105, 161))
        }

        val title = TextView(this).apply {
            text = "👥 शिक्षा रोजगार ग्रुप"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "शिक्षा • रोजगार • महत्वपूर्ण अपडेट"
            textSize = 14f
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
            text = "✍️ नई पोस्ट लिखें"
            setOnClickListener {
                openPostEditor()
            }
        }

        root.addView(
            loginButton,
            LinearLayout.LayoutParams(-1, -2).apply {
                setMargins(dp(12), dp(8), dp(12), 0)
            }
        )

        root.addView(
            writeButton,
            LinearLayout.LayoutParams(-1, -2).apply {
                setMargins(dp(12), dp(4), dp(12), 0)
            }
        )

        statusView = TextView(this).apply {
            text = "पोस्ट लोड हो रही हैं..."
            textSize = 15
