
package com.shiksharojgar.app

import android.net.Uri
import android.text.Html
import java.net.HttpURLConnection
import java.net.URL

data class VideoLinkPreview(
    val title: String = "",
    val description: String = "",
    val imageUrl: String = ""
)

object VideoLinkPreviewFetcher {

    fun fetch(
        link: String,
        done: (VideoLinkPreview?, String?) -> Unit
    ) {
        Thread {
            var connection: HttpURLConnection? = null

            try {
                val uri = Uri.parse(link.trim())

                require(
                    uri.scheme in listOf("https", "http") &&
                        !uri.host.isNullOrBlank()
                ) {
                    "सही वेबसाइट या वीडियो लिंक डालें।"
                }

                val youtubeId = Regex(
                    """(?:youtu\.be/|[?&]v=|youtube\.com/(?:embed|shorts)/)([A-Za-z0-9_-]{11})"""
                ).find(link)?.groupValues?.getOrNull(1)

                connection = URL(link).openConnection()
                    as HttpURLConnection

                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.instanceFollowRedirects = true
                connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36"
                )

                val html = connection.inputStream
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText().take(2_000_000) }

                fun meta(key: String): String {
                    val escaped = Regex.escape(key)

                    val tags = Regex(
                        """<meta\b[^>]*>""",
                        RegexOption.IGNORE_CASE
                    )

                    for (tagMatch in tags.findAll(html)) {
                        val tag = tagMatch.value

                        val property = Regex(
                            """(?:property|name)\s*=\s*["']([^"']+)["']""",
                            RegexOption.IGNORE_CASE
                        ).find(tag)?.groupValues?.get(1)

                        if (!property.equals(key, true)) continue

                        return Regex(
                            """content\s*=\s*["']([^"']*)["']""",
                            RegexOption.IGNORE_CASE
                        ).find(tag)?.groupValues?.get(1)
                            ?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString() }
                            .orEmpty()
                    }

                    return ""
                }

                val pageTitle = meta("og:title").ifBlank {
                    meta("twitter:title").ifBlank {
                        Regex(
                            """<title[^>]*>(.*?)</title>""",
                            setOf(
                                RegexOption.IGNORE_CASE,
                                RegexOption.DOT_MATCHES_ALL
                            )
                        ).find(html)?.groupValues?.get(1)
                            ?.let {
                                Html.fromHtml(
                                    it,
                                    Html.FROM_HTML_MODE_LEGACY
                                ).toString().trim()
                            }.orEmpty()
                    }
                }

                val description = meta("og:description").ifBlank {
                    meta("description").ifBlank {
                        meta("twitter:description")
                    }
                }

                
var image = if (!youtubeId.isNullOrBlank()) {
    "https://img.youtube.com/vi/$youtubeId/hqdefault.jpg"
} else {
    meta("og:image").ifBlank {
        meta("twitter:image")
    }
}

if (image.isNotBlank() && youtubeId.isNullOrBlank()) {
    image = try {
        URL(URL(link), image).toString()
    } catch (_: Exception) {
        ""
    }
}


                val result = VideoLinkPreview(
                    title = pageTitle.trim(),
                    description = description.trim(),
                    imageUrl = image.trim()
                )

                android.os.Handler(
                    android.os.Looper.getMainLooper()
                ).post {
                    done(result, null)
                }
            } catch (e: Exception) {
                val youtubeId = Regex(
                    """(?:youtu\.be/|[?&]v=|youtube\.com/(?:embed/|shorts/|live/)([A-Za-z0-9_-]{11})"""
                ).find(link)?.groupValues?.getOrNull(1)

                val fallback = if (youtubeId != null) {
                    VideoLinkPreview(
                        imageUrl = "https://img.youtube.com/vi/$youtubeId/hqdefault.jpg"
                    )
                } else {
                    null
                }

                android.os.Handler(
                    android.os.Looper.getMainLooper()
                ).post {
                    done(
                        fallback,
                        e.message ?: "इस लिंक की जानकारी प्राप्त नहीं हो सकी।"
                    )
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }
}
