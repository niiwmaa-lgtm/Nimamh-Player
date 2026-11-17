package com.example.floatingplayer

import android.content.Context

/** Remembers the playback position and bookmarks of each file (keyed by file name + size). */
object ResumeStore {
    private fun sp(c: Context) = c.getSharedPreferences("resume", Context.MODE_PRIVATE)

    private var saves = 0

    fun key(name: String, size: Long) = "$name|$size"

    fun getPos(c: Context, key: String): Long = sp(c).getLong("p:$key", 0L)

    fun setPos(c: Context, key: String, pos: Long) {
        sp(c).edit().putLong("p:$key", pos).putLong("t:$key", System.currentTimeMillis()).apply()
        saves++
        if (saves % 40 == 0) prune(c)
    }

    fun clearPos(c: Context, key: String) {
        sp(c).edit().remove("p:$key").remove("t:$key").apply()
    }

    fun bookmarks(c: Context, key: String): MutableList<Long> {
        val s = sp(c).getString("b:$key", "") ?: ""
        val out = ArrayList<Long>()
        for (part in s.split(",")) {
            val v = part.toLongOrNull()
            if (v != null) out.add(v)
        }
        out.sort()
        return out
    }

    fun saveBookmarks(c: Context, key: String, list: List<Long>) {
        val e = sp(c).edit()
        if (list.isEmpty()) e.remove("b:$key") else e.putString("b:$key", list.joinToString(","))
        e.apply()
    }

    /** Keep the store small: drop the oldest remembered positions. */
    private fun prune(c: Context) {
        val all = sp(c).all
        val times = ArrayList<Pair<String, Long>>()
        for ((k, v) in all) {
            if (k.startsWith("t:") && v is Long) times.add(Pair(k.substring(2), v))
        }
        if (times.size <= 400) return
        times.sortBy { it.second }
        val e = sp(c).edit()
        for (i in 0 until times.size - 300) {
            e.remove("p:" + times[i].first).remove("t:" + times[i].first)
        }
        e.apply()
    }
}
