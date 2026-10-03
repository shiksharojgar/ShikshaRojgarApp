package com.shiksharojgar.app

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import java.io.File

object ShareHelper {

    fun shareImageOrText(
        context: Context,
        title: String,
        text: String,
        imageRef: String = "",
        chooserTitle: String = "Share"
    ) {

        if (imageRef.isBlank()) {
            shareText(
                context,
                title,
                text,
                chooserTitle
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
                    chooserTitle
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

                    out.outputStream().use { stream ->

                        bitmap.compress(
                            Bitmap.CompressFormat.JPEG,
                            92,
                            stream
                        )
                    }

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
                                Intent(
                                    Intent.ACTION_SEND
                                ).apply {

                                    type =
                                        "image/jpeg"

                                    putExtra(
                                        Intent.EXTRA_STREAM,
                                        uri
                                    )

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
                                chooserTitle
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
                            chooserTitle
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
        chooserTitle: String
    ) {

        try {

            context.startActivity(
                Intent.createChooser(
                    Intent(
                        Intent.ACTION_SEND
                    ).apply {

                        type =
                            "text/plain"

                        putExtra(
                            Intent.EXTRA_TEXT,
                            text
                        )

                        putExtra(
                            Intent.EXTRA_TITLE,
                            title
                        )
                    },
                    chooserTitle
                )
            )

        } catch (_: Exception) {
            // No share target available.
        }
    }
}
