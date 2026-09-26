package com.shiksharojgar.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object OfflineStore {
    private const val PREF = "offline_posts"
    fun mark(context: Context, post: ChannelPost, path: String) {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val a = JSONArray(p.getString("items", "[]"))
        var exists = false
        for (i in 0 until a.length()) if (a.optJSONObject(i)?.optString("id") == post.id) exists = true
        if (!exists) a.put(JSONObject().apply { put("id", post.id); put("title", post.title); put("path", path) })
        p.edit().putString("items", a.toString()).apply()
    }
    fun isSaved(context: Context, id: String): Boolean {
        val a = JSONArray(context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("items", "[]"))
        for (i in 0 until a.length()) if (a.optJSONObject(i)?.optString("id") == id) return true
        return false
    }
    fun path(context: Context, id: String): String? {
        val a = JSONArray(context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("items", "[]"))
        for (i in 0 until a.length()) if (a.optJSONObject(i)?.optString("id") == id) return a.optJSONObject(i)?.optString("path")
        return null
    }
    fun all(context: Context): List<Pair<String,String>> {
        val a = JSONArray(context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("items", "[]")); val out=mutableListOf<Pair<String,String>>()
        for (i in 0 until a.length()) a.optJSONObject(i)?.let { out += it.optString("id") to it.optString("title") }
        return out
    }
}
