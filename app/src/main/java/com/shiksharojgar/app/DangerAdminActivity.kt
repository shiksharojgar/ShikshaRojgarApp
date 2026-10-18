package com.shiksharojgar.app

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class DangerAdminActivity : AppCompatActivity() {

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
val root = FrameLayout(this).apply {
    setBackgroundColor(Color.BLACK)
    clipChildren = false
    clipToPadding = false
}

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        val warning = TextView(this).apply {
            text = "⚠"
            textSize = 78f
            gravity = Gravity.CENTER
            setTextColor(Color.RED)
        }

        val title = TextView(this).apply {
            text = "DANGER ZONE"
            textSize = 29f
            gravity = Gravity.CENTER
            setTextColor(Color.RED)
            typeface = Typeface.DEFAULT_BOLD
        }

        val subtitle = TextView(this).apply {
            text = "RESTRICTED ACCESS"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }

        val message = TextView(this).apply {
            text = "यह प्रतिबंधित क्षेत्र है।\n" +
                "अनधिकृत प्रवेश की अनुमति नहीं है।"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(22), 0, dp(30))
        }

        val backButton = Button(this).apply {
            text = "← वापस होम पेज पर जाएँ"
            textSize = 15f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.rgb(145, 20, 20))
                cornerRadius = dp(12).toFloat()
            }
            setOnClickListener {
                finish()
            }
        }

        content.addView(
            warning,
            LinearLayout.LayoutParams(-1, dp(100))
        )

        content.addView(
            title,
            LinearLayout.LayoutParams(-1, dp(48))
        )

        content.addView(
            subtitle,
            LinearLayout.LayoutParams(-1, dp(38))
        )

        content.addView(
            message,
            LinearLayout.LayoutParams(-1, -2)
        )

        content.addView(
            backButton,
            LinearLayout.LayoutParams(-1, dp(54))
        )

        root.addView(
            content,
            FrameLayout.LayoutParams(-1, -1)
        )

        // छोटा छिपा हुआ डेंजर आइकॉन
        val secretIcon = TextView(this).apply {
    text = "⚠"
    textSize = 19f
    gravity = Gravity.CENTER
    alpha = 0.90f
    setTextColor(Color.RED)
    contentDescription = "Warning"

    background = GradientDrawable().apply {
        setColor(Color.rgb(55, 0, 0))
        setStroke(dp(1), Color.rgb(170, 0, 0))
        cornerRadius = dp(21).toFloat()
    }
}

        val tapCount = intArrayOf(0)
        var lastTap = 0L

        secretIcon.setOnClickListener {
            val now = System.currentTimeMillis()

            if (now - lastTap > 2500L) {
                tapCount[0] = 0
            }

            lastTap = now
            tapCount[0]++

            if (tapCount[0] >= 7) {
                tapCount[0] = 0

                startActivity(
                    android.content.Intent(
                        this,
                        AdminActivity::class.java
                    )
                )
            }
        }

        val secretIconParams = FrameLayout.LayoutParams(
    dp(42),
    dp(42),
    Gravity.BOTTOM or Gravity.END
).apply {
    marginEnd = dp(12)
    bottomMargin = dp(12)
}

root.addView(secretIcon, secretIconParams)y.END
            )
        )

        setContentView(root)
    }
}
