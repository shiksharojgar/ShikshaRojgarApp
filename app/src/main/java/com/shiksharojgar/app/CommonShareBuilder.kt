package com.shiksharojgar.app

import android.content.Context
import android.net.Uri

/**
 * ============================================================
 * COMMON SHARE BUILDER
 * ============================================================
 *
 * सभी sections के लिए Central Share System.
 *
 * अभी Channel से शुरू किया जा रहा है।
 * बाद में यही builder:
 * Channel
 * Notice
 * Important Information
 * Study Material / Syllabus
 * Career Guide / Useful Tools
 * आदि के लिए उपयोग होगा।
 *
 * IMPORTANT:
 * Share Text में https:// दिखाई नहीं जाएगा।
 * Internal Direct Post URL app के अंदर use होगा।
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
     * --------------------------------------------------------
     * DIRECT POST LINK
     * --------------------------------------------------------
     *
     * Share के अंदर long URL दिखाने के बजाय
     * app/browser खोलने के लिए यही canonical link रहेगा।
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
     * --------------------------------------------------------
     * SECTION HEADER
     * --------------------------------------------------------
     */
    fun sectionHeader(
        section: Section
    ): String {

        return section.header
    }

    /**
     * --------------------------------------------------------
     * SHARE TEXT
     * --------------------------------------------------------
     *
     * अभी text structure central बनाया जा रहा है।
     * Image share होने पर ShareHelper इसे image के साथ भेजेगा।
     */
    fun buildText(
        post: ChannelPost,
        section: Section
    ): String {

        return buildString {

            // -------------------------------------------------
            // HEADER
            // -------------------------------------------------

            append(
                sectionHeader(section)
            )

            // -------------------------------------------------
            // TITLE
            // -------------------------------------------------

            if (post.title.isNotBlank()) {

                append(
                    "\n\n"
                )

                append(
                    post.title.trim()
                )
            }

            // -------------------------------------------------
            // DATE + TIME
            // -------------------------------------------------

            if (post.createdAt > 0L) {

                append(
                    "\n\n"
                )

                append(
                    formatDateTime(
                        post.createdAt
                    )
                )
            }

            // -------------------------------------------------
            // DESCRIPTION
            // -------------------------------------------------

            if (post.body.isNotBlank()) {

                append(
                    "\n\n"
                )

                append(
                    post.body.trim()
                )
            }

            // -------------------------------------------------
            // PDF
            // -------------------------------------------------

            if (post.fileUrl.isNotBlank()) {

                append(
                    "\n\n📄 "
                )

                append(
                    post.fileName.ifBlank {
                        "PDF / Document"
                    }
                )
            }

            // -------------------------------------------------
            // APP DOWNLOAD TEXT
            // -------------------------------------------------

            append(
                "\n\n"
            )

            append(
                "शिक्षा रोजगार की Official Website 🌐 " +
                    "WWW.ShikshaRojgar.Com 👈 " +
                    "यहां क्लिक करके 📱 Shiksha Rojgar App " +
                    "डाउनलोड इंस्टॉल करें !"
            )

            // -------------------------------------------------
            // VIEW SECTION
            // -------------------------------------------------

            append(
                "\n\n"
            )

            append(
                section.viewText
            )

            /*
             * IMPORTANT:
             *
             * Direct Post URL अभी text में intentionally
             * अलग long URL की तरह नहीं दिखाया जा रहा।
             *
             * अगला चरण Android sharing target के अनुसार
             * clickable/preview handling के लिए होगा।
             */
        }
    }

    /**
     * --------------------------------------------------------
     * DATE + TIME
     * --------------------------------------------------------
     *
     * सभी shared posts में एक common format.
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
