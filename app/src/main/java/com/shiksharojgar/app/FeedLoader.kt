package com.shiksharojgar.app

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object FeedLoader {
    private val handler = Handler(Looper.getMainLooper())

    private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15000
        readTimeout = 20000
        requestMethod = "GET"
        setRequestProperty("User-Agent", "Mozilla/5.0 (Android) ShikshaRojgarApp/1.2")
        setRequestProperty("Accept", "application/json, application/xml, text/xml, */*")
        instanceFollowRedirects = true
    }

    private fun fetchJson(feedUrl: String, source: String): List<Post> {
        val result = mutableListOf<Post>()
        val connection = open(feedUrl)
        try {
            if (connection.responseCode !in 200..299) throw IllegalStateException("HTTP ${connection.responseCode}")
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(body)
            val entries = root.optJSONObject("feed")?.optJSONArray("entry") ?: return result
            for (i in 0 until minOf(entries.length(), 20)) {
                val entry = entries.optJSONObject(i) ?: continue
                val title = entry.optJSONObject("title")?.optString("\$t", "")?.trim()
                    ?: entry.optJSONObject("title")?.optString("\$t", "")?.trim().orEmpty()
                val published = entry.optJSONObject("published")?.optString("\$t", "")?.trim().orEmpty()
                val links = entry.optJSONArray("link")
                var link = ""
                if (links != null) for (j in 0 until links.length()) {
                    val l = links.optJSONObject(j) ?: continue
                    if (l.optString("rel") == "alternate") { link = l.optString("href"); break }
                }
                if (title.isNotBlank() && link.isNotBlank()) result += Post(title, link, published, source)
            }
        } finally { connection.disconnect() }
        return result
    }

    private fun fetchRss(feedUrl: String, source: String): List<Post> {
        val result = mutableListOf<Post>()
        val connection = open(feedUrl)
        try {
            if (connection.responseCode !in 200..299) throw IllegalStateException("HTTP ${connection.responseCode}")
            val doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(connection.inputStream)
            val items = doc.getElementsByTagName("item")
            for (i in 0 until minOf(items.length, 20)) {
                val node = items.item(i)
                fun text(tag: String): String {
                    val nodes = node.childNodes
                    for (j in 0 until nodes.length) if (nodes.item(j).nodeName == tag) return nodes.item(j).textContent ?: ""
                    return ""
                }
                val title = text("title").trim(); val link = text("link").trim(); val date = text("pubDate").trim()
                if (title.isNotBlank() && link.isNotBlank()) result += Post(title, link, date, source)
            }
        } finally { connection.disconnect() }
        return result
    }

    fun load(callback: (List<Post>) -> Unit) {
        Thread {
            val feed = "https://www.shiksharojgar.com/feeds/posts/default"
            var posts = emptyList<Post>()
            try { posts = fetchJson("$feed?alt=json&max-results=20", "Shiksha Rojgar") } catch (_: Exception) {}
            if (posts.isEmpty()) try { posts = fetchRss("$feed?alt=rss&max-results=20", "Shiksha Rojgar") } catch (_: Exception) {}
            val sorted = posts.sortedByDescending { it.date }.take(20)
            handler.post { callback(sorted) }
        }.start()
    }
}
