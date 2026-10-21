
package com.shiksharojgar.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ShikshaPostsActivity : AppCompatActivity() {

    private lateinit var adapter: PostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        val heading = TextView(this).apply {
            text = "📚 Shiksha Rojgar की सभी नई पोस्ट"
            textSize = 18f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(29, 78, 216))
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 18, 12, 18)
        }

        val recycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@ShikshaPostsActivity)
            setPadding(8, 8, 8, 8)
            clipToPadding = false
            isNestedScrollingEnabled = false
        }

        adapter = PostAdapter(emptyList()) { post ->
            startActivity(
                Intent(
                    this@ShikshaPostsActivity,
                    WebViewActivity::class.java
                ).putExtra("url", post.url)
            )
        }

        recycler.adapter = adapter

        root.addView(
            heading,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            recycler,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)

        FeedLoader.load { posts ->
            adapter.submit(posts)
        }
    }
}
