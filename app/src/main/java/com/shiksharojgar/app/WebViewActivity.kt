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
import android.view.View
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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

        /*
         * =========================================================
         * ANDROID 15 / EDGE-TO-EDGE INSETS
         * =========================================================
         */

        
val oldBottomBar =
    findViewById<LinearLayout>(R.id.webBottomBar)

val rootLayout =
    oldBottomBar.parent as LinearLayout

val bottomIndex =
    rootLayout.indexOfChild(oldBottomBar)

rootLayout.removeView(oldBottomBar)

val commonBottomBar =
    CommonPageUi.createBottomBar(
        this,
        CommonPageUi.Section.MORE
    )

rootLayout.addView(
    commonBottomBar,
    bottomIndex,
    LinearLayout.LayoutParams(
        -1,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
)

ViewCompat.setOnApplyWindowInsetsListener(
    commonBottomBar
) { view, insets ->

    val bottom =
        insets.getInsets(
            WindowInsetsCompat.Type.navigationBars()
        ).bottom

    view.setPadding(
        view.paddingLeft,
        dp 4,
        view.paddingRight,
        bottom + dp 4
    )

    insets
}


        ViewCompat.setOnApplyWindowInsetsListener(
        

        /*
         * =========================================================
         * VIEW REFERENCES
         * =========================================================
         */

        web = findViewById(R.id.web)
        loadingOverlay = findViewById(R.id.pageLoadingOverlay)
        loadingText = findViewById(R.id.loadingText)
        

        /*
         * =========================================================
         * WEBVIEW SETTINGS
         * =========================================================
         */

        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.loadsImagesAutomatically = true
        web.settings.setSupportZoom(false)

        web.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                return false
            }

            override fun onPageStarted(
                view: WebView,
                url: String,
                favicon: android.graphics.Bitmap?
            ) {
                super.onPageStarted(view, url, favicon)

                showLoading(
                    "कृपया प्रतीक्षा करें…\nपेज लोड हो रहा है"
                )
            }

            override fun onPageFinished(
                view: WebView,
                url: String
            ) {
                super.onPageFinished(view, url)

                hideLoading()
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)

                if (request.isForMainFrame) {
                    showLoading(
                        "इंटरनेट कनेक्शन की प्रतीक्षा है…\nकृपया थोड़ी देर प्रतीक्षा करें"
                    )
                }
            }
        }

        web.webChromeClient = WebChromeClient()

        /*
         * =========================================================
         * APK DOWNLOAD
         * =========================================================
         */

        web.setDownloadListener(
            DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->

                downloadApk(
                    url,
                    userAgent,
                    contentDisposition,
                    mimeType
                )
            }
        )

        /*
         * =========================================================
         * TOP HEADER BACK
         * =========================================================
         */

        findViewById<TextView>(
            R.id.webBackButton
        ).setOnClickListener {
            goBackOrClose()
        }

        
         

        

        /*
         * =========================================================
         * SYSTEM BACK
         * =========================================================
         */

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {
                    goBackOrClose()
                }
            }
        )

        /*
         * =========================================================
         * LOAD REQUESTED URL
         * =========================================================
         */

        web.loadUrl(
            intent.getStringExtra("url")
                ?: "https://www.shiksharojgar.com/"
        )
    }

    /*
     * =========================================================
     * REFRESH UNREAD COUNT
     * =========================================================
     */

        override fun onResume() {
        super.onResume()
    }

    /*
     * =========================================================
     * LOADING
     * =========================================================
     */

    private fun showLoading(message: String) {

        if (!::loadingOverlay.isInitialized) return

        loadingText.text = message
        loadingOverlay.visibility = View.VISIBLE
    }

    private fun hideLoading() {

        if (!::loadingOverlay.isInitialized) return

        loadingOverlay.visibility = View.GONE
    }




    /*
     * =========================================================
     * HOME
     * =========================================================
     */

    private fun goHome() {

        try {

            val intent = Intent(
                this,
                MainActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            startActivity(intent)
            finish()

        } catch (_: Exception) {

            finish()
        }
    }

    /*
     * =========================================================
     * CHANNEL
     * =========================================================
     */

    private fun openChannel() {

        try {

            startActivity(
                Intent(
                    this,
                    ChannelActivity::class.java
                )
            )

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Channel अभी उपलब्ध नहीं है।",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /*
     * =========================================================
     * NOTICE
     * =========================================================
     */

    private fun openNotice() {

        try {

            startActivity(
                Intent(
                    this,
                    NoticeActivity::class.java
                )
            )

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Notice अभी उपलब्ध नहीं है।",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /*
     * =========================================================
     * IMPORTANT INFORMATION
     * =========================================================
     */

    private fun openImportantInformation() {

        try {

            startActivity(
                Intent(
                    this,
                    ContentPageActivity::class.java
                ).apply {

                    putExtra(
                        "pageId",
                        "important_information"
                    )

                    putExtra(
                        "pageTitle",
                        "📌 Important Information"
                    )
                }
            )

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Important Information अभी उपलब्ध नहीं है।",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /*
     * =========================================================
     * MORE
     * =========================================================
     */

    private fun openMore() {

        Toast.makeText(
            this,
            "More",
            Toast.LENGTH_SHORT
        ).show()
    }

    /*
     * =========================================================
     * APK DOWNLOAD
     * =========================================================
     */

    private fun downloadApk(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?
    ) {

        if (
            !url.contains(
                ".apk",
                ignoreCase = true
            ) &&
            mimeType !=
            "application/vnd.android.package-archive"
        ) {

            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )
            )

            return
        }

        try {

            val manager =
                getSystemService(
                    Context.DOWNLOAD_SERVICE
                ) as DownloadManager

            val request =
                DownloadManager.Request(
                    Uri.parse(url)
                ).apply {

                    setTitle(
                        "Shiksha Rojgar App"
                    )

                    setDescription(
                        "APK डाउनलोड हो रहा है… डाउनलोड पूरा होते ही Install स्क्रीन खुलेगी।"
                    )

                    setMimeType(
                        "application/vnd.android.package-archive"
                    )

                    setNotificationVisibility(
                        DownloadManager.Request
                            .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                    )

                    setAllowedOverMetered(true)
                    setAllowedOverRoaming(true)

                    if (!userAgent.isNullOrBlank()) {

                        addRequestHeader(
                            "User-Agent",
                            userAgent
                        )
                    }

                    setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        "ShikshaRojgar-App.apk"
                    )
                }

            val downloadId =
                manager.enqueue(request)

            Toast.makeText(
                this,
                "APK डाउनलोड शुरू हो गया है…",
                Toast.LENGTH_SHORT
            ).show()

            watchDownload(
                manager,
                downloadId
            )

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "डाउनलोड शुरू नहीं हो सका",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /*
     * =========================================================
     * WATCH APK DOWNLOAD
     * =========================================================
     */

    private fun watchDownload(
        manager: DownloadManager,
        downloadId: Long
    ) {

        downloadHandler.postDelayed(
            object : Runnable {

                override fun run() {

                    val cursor =
                        manager.query(
                            DownloadManager.Query()
                                .setFilterById(downloadId)
                        )

                    cursor.use { c ->

                        if (!c.moveToFirst()) {
                            return
                        }

                        val status =
                            c.getInt(
                                c.getColumnIndexOrThrow(
                                    DownloadManager.COLUMN_STATUS
                                )
                            )

                        when (status) {

                            DownloadManager.STATUS_SUCCESSFUL -> {

                                val downloadedUri =
                                    manager.getUriForDownloadedFile(
                                        downloadId
                                    )

                                if (downloadedUri != null) {

                                    openInstaller(
                                        downloadedUri
                                    )

                                } else {

                                    Toast.makeText(
                                        this@WebViewActivity,
                                        "APK डाउनलोड हो गया है, लेकिन Install स्क्रीन नहीं खुली। Downloads में APK पर टैप करें।",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                                return
                            }

                            DownloadManager.STATUS_FAILED -> {

                                Toast.makeText(
                                    this@WebViewActivity,
                                    "APK डाउनलोड असफल हुआ। कृपया फिर प्रयास करें।",
                                    Toast.LENGTH_LONG
                                ).show()

                                return
                            }
                        }
                    }

                    downloadHandler.postDelayed(
                        this,
                        700L
                    )
                }
            },
            700L
        )
    }

    /*
     * =========================================================
     * OPEN APK INSTALLER
     * =========================================================
     */

    private fun openInstaller(uri: Uri) {

        try {

            val intent =
                Intent(Intent.ACTION_VIEW).apply {

                    setDataAndType(
                        uri,
                        "application/vnd.android.package-archive"
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )

                    addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                    )
                }

            startActivity(intent)

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Install स्क्रीन नहीं खुल सकी। Downloads से APK खोलें।",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /*
     * =========================================================
     * WEB BACK
     * =========================================================
     */

    private fun goBackOrClose() {

        if (web.canGoBack()) {

            web.goBack()

        } else {

            finish()
        }
    }

    /*
     * =========================================================
     * CLEANUP
     * =========================================================
     */

    override fun onDestroy() {

        downloadHandler.removeCallbacksAndMessages(
            null
        )

        if (::web.isInitialized) {

            web.stopLoading()
            web.destroy()
        }

        super.onDestroy()
    }
}
