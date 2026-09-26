package com.shiksharojgar.app

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.webkit.DownloadListener
import android.widget.Toast
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceError
import android.widget.TextView
import android.view.View
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class WebViewActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private lateinit var loadingOverlay: LinearLayout
    private lateinit var loadingText: TextView
    private val downloadHandler = Handler(Looper.getMainLooper())

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.webTopBar)) { view, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(view.paddingLeft, top + 4, view.paddingRight, view.paddingBottom)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.webBottomBar)) { view, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(view.paddingLeft, 5, view.paddingRight, bottom + 8)
            insets
        }

        web = findViewById(R.id.web)
        loadingOverlay = findViewById(R.id.pageLoadingOverlay)
        loadingText = findViewById(R.id.loadingText)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.loadsImagesAutomatically = true
        web.settings.setSupportZoom(false)
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = false

            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                showLoading("कृपया प्रतीक्षा करें…\nपेज लोड हो रहा है")
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                hideLoading()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                super.onReceivedError(view, request, error)
                if (request.isForMainFrame) {
                    showLoading("इंटरनेट कनेक्शन की प्रतीक्षा है…\nकृपया थोड़ी देर प्रतीक्षा करें")
                }
            }
        }
        web.webChromeClient = WebChromeClient()
        web.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            downloadApk(url, userAgent, contentDisposition, mimeType)
        })

        findViewById<TextView>(R.id.backButton).setOnClickListener { goBackOrClose() }
        findViewById<TextView>(R.id.bottomBackButton).setOnClickListener { goBackOrClose() }
        findViewById<TextView>(R.id.shareButton).setOnClickListener { shareCurrentPage() }
        findViewById<TextView>(R.id.homeButton).setOnClickListener { finish() }
        findViewById<TextView>(R.id.bottomHomeButton).setOnClickListener { finish() }
        findViewById<TextView>(R.id.bottomShareButton).setOnClickListener { shareCurrentPage() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { goBackOrClose() }
        })

        web.loadUrl(intent.getStringExtra("url") ?: "https://www.shiksharojgar.com/")
    }

    private fun showLoading(message: String) {
        if (!::loadingOverlay.isInitialized) return
        loadingText.text = message
        loadingOverlay.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        if (!::loadingOverlay.isInitialized) return
        loadingOverlay.visibility = View.GONE
    }

    private fun downloadApk(url: String, userAgent: String?, contentDisposition: String?, mimeType: String?) {
        if (!url.contains(".apk", ignoreCase = true) && mimeType != "application/vnd.android.package-archive") {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            return
        }

        try {
            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle("Shiksha Rojgar App")
                setDescription("APK डाउनलोड हो रहा है… डाउनलोड पूरा होते ही Install स्क्रीन खुलेगी।")
                setMimeType("application/vnd.android.package-archive")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
                if (!userAgent.isNullOrBlank()) addRequestHeader("User-Agent", userAgent)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "ShikshaRojgar-App.apk")
            }
            val downloadId = manager.enqueue(request)
            Toast.makeText(this, "APK डाउनलोड शुरू हो गया है…", Toast.LENGTH_SHORT).show()
            watchDownload(manager, downloadId)
        } catch (e: Exception) {
            Toast.makeText(this, "डाउनलोड शुरू नहीं हो सका", Toast.LENGTH_LONG).show()
        }
    }

    private fun watchDownload(manager: DownloadManager, downloadId: Long) {
        downloadHandler.postDelayed(object : Runnable {
            override fun run() {
                val cursor = manager.query(DownloadManager.Query().setFilterById(downloadId))
                cursor.use { c ->
                    if (!c.moveToFirst()) return
                    val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            val downloadedUri = manager.getUriForDownloadedFile(downloadId)
                            if (downloadedUri != null) {
                                openInstaller(downloadedUri)
                            } else {
                                Toast.makeText(this@WebViewActivity, "APK डाउनलोड हो गया है, लेकिन Install स्क्रीन नहीं खुली। Downloads में APK पर टैप करें।", Toast.LENGTH_LONG).show()
                            }
                            return
                        }
                        DownloadManager.STATUS_FAILED -> {
                            Toast.makeText(this@WebViewActivity, "APK डाउनलोड असफल हुआ। कृपया फिर प्रयास करें।", Toast.LENGTH_LONG).show()
                            return
                        }
                    }
                }
                downloadHandler.postDelayed(this, 700L)
            }
        }, 700L)
    }

    private fun openInstaller(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Install स्क्रीन नहीं खुल सकी। Downloads से APK खोलें।", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareCurrentPage() {
        val url = web.url ?: intent.getStringExtra("url") ?: "https://www.shiksharojgar.com/"
        val title = web.title ?: "Shiksha Rojgar"
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "$title\n$url")
        }, "Share Page"))
    }

    private fun goBackOrClose() {
        if (web.canGoBack()) web.goBack() else finish()
    }

    override fun onDestroy() {
        downloadHandler.removeCallbacksAndMessages(null)
        web.stopLoading()
        web.destroy()
        super.onDestroy()
    }
}
