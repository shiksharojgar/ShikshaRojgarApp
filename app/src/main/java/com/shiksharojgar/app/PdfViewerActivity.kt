
package com.shiksharojgar.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import java.io.IOException

class PdfViewerActivity : Activity() {

    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CommonPageUi.applyBlueStatusBar(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(248, 250, 252))
        }

        // APP HEADER
        val header = CommonPageUi.createHeader(
            this,
            onBack = { finish() },
            onShare = {},
            onMenu = {
                CommonPageUi.showAccountMenu(this)
            }
        )

        root.addView(
            header,
            LinearLayout.LayoutParams(-1, dp(58))
        )
val filePath = intent.getStringExtra("pdfFilePath")
val pdfName = filePath?.let { File(it).name }
    ?.takeIf { it.isNotBlank() }
    ?: "PDF फाइल"

val pdfTitleBar = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(dp(16), dp(10), dp(16), dp(10))
    setBackgroundColor(Color.WHITE)
}

pdfTitleBar.addView(TextView(this).apply {
    text = "📄 PDF देखें"
    textSize = 17f
    setTextColor(Color.rgb(25, 65, 110))
    setTypeface(null, android.graphics.Typeface.BOLD)
})

pdfTitleBar.addView(TextView(this).apply {
    text = pdfName
    textSize = 13f
    setTextColor(Color.DKGRAY)
    maxLines = 2
})

root.addView(
    pdfTitleBar,
    LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT)
)
        // PDF SCROLL AREA
        val scrollView = ScrollView(this)

        val pagesLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(10), dp(8), dp(10))
        }

        scrollView.addView(pagesLayout)

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        // BOTTOM NAVIGATION
        val bottomBar = CommonPageUi.createBottomBar(
            this,
            CommonPageUi.Section.MORE
        )

        root.addView(
            bottomBar,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(root)

        

        if (filePath.isNullOrBlank()) {
            showError(pagesLayout, "PDF फाइल उपलब्ध नहीं है।")
            return
        }

        try {
            val pdfFile = File(filePath)

            if (!pdfFile.exists()) {
                showError(pagesLayout, "PDF फाइल नहीं मिली।")
                return
            }

            fileDescriptor = ParcelFileDescriptor.open(
                pdfFile,
                ParcelFileDescriptor.MODE_READ_ONLY
            )

            pdfRenderer = PdfRenderer(fileDescriptor!!)

            renderPdfPages(pagesLayout)

        } catch (e: Exception) {
            showError(
                pagesLayout,
                "PDF नहीं खुल सकी: ${e.message ?: "अज्ञात त्रुटि"}"
            )
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun renderPdfPages(
        pagesLayout: LinearLayout
    ) {
        val renderer = pdfRenderer ?: return

        val screenWidth =
            resources.displayMetrics.widthPixels - dp(24)

        try {
            for (pageIndex in 0 until renderer.pageCount) {

                val page = renderer.openPage(pageIndex)

                val bitmapWidth = screenWidth.coerceAtLeast(300)

                val bitmapHeight =
                    (bitmapWidth.toFloat() *
                        page.height.toFloat() /
                        page.width.toFloat()).toInt()

                val bitmap = Bitmap.createBitmap(
                    bitmapWidth,
                    bitmapHeight,
                    Bitmap.Config.ARGB_8888
                )

                page.render(
                    bitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                )

                page.close()

                val pageImage = ImageView(this).apply {
                    setImageBitmap(bitmap)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    adjustViewBounds = true
                    setBackgroundColor(Color.WHITE)
                    elevation = dp(3).toFloat()
                }

                pagesLayout.addView(
                    pageImage,
                    LinearLayout.LayoutParams(
                        -1,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(16)
                    }
                )
            }

        } catch (e: Exception) {
            showError(
                pagesLayout,
                "PDF पेज लोड नहीं हुए: ${e.message}"
            )
        }
    }

    private fun showError(
        layout: LinearLayout,
        message: String
    ) {
        layout.removeAllViews()

        layout.addView(
            TextView(this).apply {
                text = message
                textSize = 16f
                setTextColor(Color.RED)
                gravity = Gravity.CENTER
                setPadding(dp(20), dp(30), dp(20), dp(30))
            }
        )
    }

    override fun onDestroy() {
        try {
            pdfRenderer?.close()
            fileDescriptor?.close()
        } catch (_: IOException) {
        }

        super.onDestroy()
    }
}
