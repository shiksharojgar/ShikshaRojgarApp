package com.shiksharojgar.app

import android.net.Uri

/**
 * ============================================================
 * COMMON SHARE BUILDER
 * ============================================================
 *
 * सभी sections के लिए Central Share System.
 *
 * एक ही जगह से:
 * • Section Header
 * • Direct Post Link
 * • App Download Link
 * • Date + Time
 * • Description
 * • Inline Clickable Links
 * • View Section / Post
 *
 * नियंत्रित होंगे।
 */
object CommonShareBuilder {

    enum class Section(
        val header: String,
        val viewText: String
    ) {

        CHANNEL(
            "📱 Shiksha Rojgar App 📢 शिक्षा रोजगार चैनल",
            "View Channel 👈"
        ),

        NOTICE(
            "📱 Shiksha Rojgar App 🔔 Notice / Update",
            "View Notice 👈"
        ),

        IMPORTANT_INFORMATION(
            "📱 Shiksha Rojgar App 📌 Important Information / Imp Info",
            "View Imp Info 👈"
        ),

        STUDY_MATERIAL(
            "📱 Shiksha Rojgar App 📚 Study Material / Syllabus",
            "View Study Material 👈"
        ),

        CAREER_TOOLS(
            "📱 Shiksha Rojgar App 🛠️ Career Guide / Useful Tools",
            "View Career / Tools 👈"
        )
    }

    /**
     * ============================================================
     * DIRECT POST URL
     * ============================================================
     */
    fun directPostUrl(
        postId: String
    ): String {

        if (postId.isBlank()) {
            return "https://shiksha-rojgar.web.app/channel"
        }

        return "https://shiksha-rojgar.web.app/channel" +
            "?post=${Uri.encode(postId)}"
    }

    /**
     * ============================================================
     * SECTION HEADER
     * ============================================================
     */
    fun sectionHeader(
        section: Section
    ): String {

        return section.header
    }

    /**
     * ============================================================
     * APP DOWNLOAD URL
     * ============================================================
     */
    fun appDownloadUrl(): String {

        return "https://shiksha-rojgar.web.app/"
    }

    /**
     * ============================================================
     * SHARE TEXT
     * ============================================================
     *
     * Plain text fallback.
     *
     * इसमें long URL दिखाई नहीं जाएगी।
     */
    
    fun buildText(
        post: ChannelPost,
        section: Section
    ): String {

        val postUrl = directPostUrl(post.id)

        return buildString {

            // SECTION HEADER — BOLD
            append("*")
            append(sectionHeader(section))
            append("*")

            // DIRECT POST LINK — TOP
            append("\n\n*पोस्ट देखें 👇*")
            append("\n")
            append(postUrl)

            // TITLE — BOLD
            if (post.title.isNotBlank()) {
                append("\n\n*")
                append(post.title.trim())
                append("*")
            }

            // DESCRIPTION — BOLD
            if (post.body.isNotBlank()) {
                append("\n\n*")
                append(post.body.trim())
                append("*")
            }

            // PDF / DOCUMENT
            if (post.fileUrl.isNotBlank()) {
                append("\n\n*📄 ")
                append(
                    post.fileName.ifBlank {
                        "PDF / Document"
                    }
                )
                append("*\n")
                append(post.fileUrl)
            }

            // OFFICIAL WEBSITE HEADING
            append("\n\n")
            append("*✅ शिक्षा रोजगार की Official Website 🌐*")

            // URL — KEEP PLAIN FOR CLICKABILITY
            append("\nhttps://www.shiksharojgar.com/")

            // APP DOWNLOAD MESSAGE — BOLD
            append(
                "\n*👈 यहां क्लिक करके 📱 Shiksha Rojgar App " +
                    "डाउनलोड इंस्टॉल करें !*"
            )
        }
    }



    /**
     * ============================================================
     * SHARE HTML
     * ============================================================
     *
     * HTML share में:
     *
     * 1. पूरा Section Header clickable
     * 2. Direct Post URL hidden
     * 3. App Download text clickable
     * 4. Description के अंदर links clickable
     * 5. https:// visible नहीं होगा
     * 6. View Section / Post clickable
     */
    fun buildShareHtml(
        post: ChannelPost,
        section: Section
    ): String {

        val postUrl =
            directPostUrl(
                post.id
            )

        val appUrl =
            appDownloadUrl()

        return buildString {

            // =================================================
            // CLICKABLE SECTION HEADER
            // =================================================

            append(
                "<a href=\""
            )

            append(
                postUrl
            )

            append(
                "\">"
            )

            append(
                android.text.Html.escapeHtml(
                    sectionHeader(section)
                )
            )

            append(
                "</a>"
            )

            // =================================================
            // TITLE
            // =================================================

            if (post.title.isNotBlank()) {

                append("<br><br>")

                append(
                    android.text.Html.escapeHtml(
                        post.title.trim()
                    )
                )
            }

            // =================================================
            // DATE + TIME
            // =================================================

            if (post.createdAt > 0L) {

                append("<br><br>")

                append(
                    android.text.Html.escapeHtml(
                        formatDateTime(
                            post.createdAt
                        )
                    )
                )
            }

            // =================================================
            // DESCRIPTION + INLINE LINKS
            // =================================================

            if (post.body.isNotBlank()) {

                append("<br><br>")

                append(
                    descriptionToHtml(
                        post.body.trim()
                    )
                )
            }

            // =================================================
            // PDF / DOCUMENT
            // =================================================

            if (post.fileUrl.isNotBlank()) {

                append("<br><br>📄 ")

                append(
                    android.text.Html.escapeHtml(
                        post.fileName.ifBlank {
                            "PDF / Document"
                        }
                    )
                )
            }

            // =================================================
            // APP DOWNLOAD
            // =================================================

            append("<br><br>")

            append(
                "<a href=\""
            )

            append(
                appUrl
            )

            append(
                "\">"
            )

            append(
                "शिक्षा रोजगार की Official Website 🌐 " +
                    "WWW.ShikshaRojgar.Com 👈 " +
                    "यहां क्लिक करके 📱 Shiksha Rojgar App " +
                    "डाउनलोड इंस्टॉल करें !"
            )

            append(
                "</a>"
            )

            // =================================================
            // VIEW SECTION / POST
            // =================================================

            append("<br><br>")

            append(
                "<a href=\""
            )

            append(
                postUrl
            )

            append(
                "\">"
            )

            append(
                android.text.Html.escapeHtml(
                    section.viewText
                )
            )

            append(
                "</a>"
            )
        }
    }

    /**
     * ============================================================
     * DESCRIPTION → HTML
     * ============================================================
     *
     * उदाहरण:
     *
     * MP Board की पूरी जानकारी
     * www.shiksharojgar.com पर देखें।
     *
     * केवल URL clickable होगा।
     *
     * https:// visible नहीं होगा।
     */
    private fun descriptionToHtml(
        text: String
    ): String {

        val escaped =
            android.text.Html.escapeHtml(
                text
            ).replace(
                "\n",
                "<br>"
            )

        val pattern =
            java.util.regex.Pattern.compile(
                "(?i)(https?://|www\\.)[^\\s<]+"
            )

        val matcher =
            pattern.matcher(
                escaped
            )

        val result =
            StringBuffer()

        while (matcher.find()) {

            var visibleUrl =
                matcher.group()

            var trailing =
                ""

            while (
                visibleUrl.isNotEmpty() &&
                visibleUrl.last() in
                charArrayOf(
                    '.',
                    ',',
                    '!',
                    '?',
                    ')',
                    ']',
                    '}'
                )
            ) {

                trailing =
                    visibleUrl.last() +
                        trailing

                visibleUrl =
                    visibleUrl.dropLast(1)
            }

            val href =
                if (
                    visibleUrl.startsWith(
                        "http://",
                        ignoreCase = true
                    ) ||
                    visibleUrl.startsWith(
                        "https://",
                        ignoreCase = true
                    )
                ) {

                    visibleUrl

                } else {

                    "https://$visibleUrl"
                }

            val displayUrl =
                visibleUrl
                    .replaceFirst(
                        Regex(
                            "^https?://"
                        ),
                        ""
                    )

            val replacement =
                "<a href=\"" +
                    href +
                    "\">" +
                    displayUrl +
                    "</a>" +
                    trailing

            matcher.appendReplacement(
                result,
                java.util.regex.Matcher.quoteReplacement(
                    replacement
                )
            )
        }

        matcher.appendTail(
            result
        )

        return result.toString()
    }

    /**
     * ============================================================
     * DATE + TIME
     * ============================================================
     */
    private fun formatDateTime(
        timestamp: Long
    ): String {

        return try {

            val formatter =
                java.text.SimpleDateFormat(
                    "d MMMM yyyy, h:mm a",
                    java.util.Locale(
                        "hi",
                        "IN"
                    )
                )

            formatter.format(
                java.util.Date(
                    timestamp
                )
            )

        } catch (_: Exception) {

            ""
        }
    }
}
