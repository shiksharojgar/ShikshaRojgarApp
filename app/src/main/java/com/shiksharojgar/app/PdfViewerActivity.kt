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

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(245, 245, 245))
        }

        val header = TextView(this).apply {
            text = "📄 PDF देखें"
            textSize = 19f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(18, 14, 18, 14)
            setBackgroundColor(Color.rgb(25, 90, 160))
        }

        root.addView(
            header,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val scrollView = ScrollView(this)

        val pagesLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 10, 8, 10)
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

        setContentView(root)

        val filePath = intent.getStringExtra("pdfFilePath")

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

    private fun renderPdfPages(
        pagesLayout: LinearLayout
    ) {
        val renderer = pdfRenderer ?: return

        val screenWidth =
            resources.displayMetrics.widthPixels -
                (resources.displayMetrics.density * 24).toInt()

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
                    elevation = 3f
                }

                pagesLayout.addView(
                    pageImage,
                    LinearLayout.LayoutParams(
                        -1,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 16
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
                setPadding(20, 30, 20, 30)
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
