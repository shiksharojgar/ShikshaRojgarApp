package com.shiksharojgar.app

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import android.text.Html
import java.io.File

object ShareHelper {

    fun shareImageOrText(
    context: Context,
    title: String,
    text: String,
    imageRef: String = "",
    chooserTitle: String = "Share",
htmlText: String = ""
) {

        if (imageRef.isBlank()) {
            shareText(
    context,
    title,
    text,
    chooserTitle,
    htmlText
)
            return
        }

        ChannelRepository.loadImage(
            imageRef
        ) { bitmap, _ ->

            if (bitmap == null) {

                shareText(
    context,
    title,
    text,
    chooserTitle,
    htmlText
)

                return@loadImage
            }

            Thread {

                try {

                    val dir =
                        File(
                            context.cacheDir,
                            "shared_media"
                        )

                    dir.mkdirs()

                    val now =
                        System.currentTimeMillis()

                    dir.listFiles()
                        ?.filter {
                            now - it.lastModified() >
                                6L * 60L * 60L * 1000L
                        }
                        ?.forEach {
                            it.delete()
                        }
val out =
    File(
        dir,
        "share_$now.jpg"
    )

/*
 * ============================================================
 * SHARE WATERMARK
 * ============================================================
 *
 * Shared image पर हल्का watermark।
 * Original image crop नहीं होगी।
 */
val watermarkedBitmap =
    bitmap.copy(
        Bitmap.Config.ARGB_8888,
        true
    )

val canvas =
    android.graphics.Canvas(
        watermarkedBitmap
    )

val density =
    context.resources.displayMetrics.density

val padding =
    (12f * density)

val textSize =
    (12f * density)

val paint =
    android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG
    ).apply {

        color =
            android.graphics.Color.WHITE

        alpha =
            185

        this.textSize =
            textSize

        typeface =
            android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.BOLD
            )

        setShadowLayer(
            3f * density,
            1f * density,
            1f * density,
            android.graphics.Color.BLACK
        )
    }

val line1 =
    "🌐 ShikshaRojgar.com"

val line2 =
    "📱 Shiksha Rojgar App"

val x =
    padding

val y2 =
    watermarkedBitmap.height -
        padding

val y1 =
    y2 -
        (textSize + 4f * density)

canvas.drawText(
    line1,
    x,
    y1,
    paint
)

canvas.drawText(
    line2,
    x,
    y2,
    paint
)

out.outputStream().use { stream ->

    watermarkedBitmap.compress(
        Bitmap.CompressFormat.JPEG,
        92,
        stream
    )
}

watermarkedBitmap.recycle()
bitmap.recycle()

                    val uri =
                        FileProvider.getUriForFile(
                            context,
                            "com.shiksharojgar.app.fileprovider",
                            out
                        )

                    Handler(
                        Looper.getMainLooper()
                    ).post {

                        try {

                            val intent =
    Intent(Intent.ACTION_SEND).apply {

        type = "image/jpeg"

        putExtra(
            Intent.EXTRA_STREAM,
            uri
        )

        // WhatsApp और अन्य apps के लिए plain caption
        putExtra(
            Intent.EXTRA_TEXT,
            text
        )

        putExtra(
            Intent.EXTRA_TITLE,
            title
        )

        addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        clipData =
            ClipData.newRawUri(
                "Shiksha Rojgar image",
                uri
            )
    }

context.startActivity(
    Intent.createChooser(
        intent,
        chooserTitle
    )
)

                        } catch (_: Exception) {

                            shareText(
    context,
    title,
    text,
    chooserTitle,
    htmlText
)
                        }
                    }

                } catch (_: Exception) {

                    Handler(
                        Looper.getMainLooper()
                    ).post {

                        shareText(
    context,
    title,
    text,
    chooserTitle,
    htmlText
)
                    }
                }
            }.start()
        }
    }

    private fun shareText(
    context: Context,
    title: String,
    text: String,
    chooserTitle: String,
    htmlText: String = ""
) {

    try {

        val intent =
            Intent(Intent.ACTION_SEND).apply {

                type = "text/plain"

                val styledText =
                    if (htmlText.isNotBlank()) {
                        Html.fromHtml(
                            htmlText,
                            Html.FROM_HTML_MODE_LEGACY
                        )
                    } else {
                        text
                    }

                putExtra(
                    Intent.EXTRA_TEXT,
                    styledText
                )

                putExtra(
                    Intent.EXTRA_TITLE,
                    title
                )

                if (htmlText.isNotBlank()) {

                    putExtra(
                        Intent.EXTRA_HTML_TEXT,
                        htmlText
                    )
                }
            }

        context.startActivity(
            Intent.createChooser(
                intent,
                chooserTitle
            )
        )

    } catch (_: Exception) {

        // Share fail होने पर app crash नहीं होगी।
    }
}
}
