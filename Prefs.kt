package com.example.floatingplayer

import android.content.Context

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    /** 0 = compact, 1 = normal, 2 = wide */
    fun size(c: Context) = sp(c).getInt("size", 1)

    /** Choosing a preset also forgets any size the user dragged to. */
    fun setSize(c: Context, v: Int) = sp(c).edit().putInt("size", v).remove("w").remove("h").apply()

    /** 50..100 */
    fun opacity(c: Context) = sp(c).getInt("opacity", 100)
    fun setOpacity(c: Context, v: Int) = sp(c).edit().putInt("opacity", v).apply()

    fun autoClose(c: Context) = sp(c).getBoolean("autoClose", true)
    fun setAutoClose(c: Context, v: Boolean) = sp(c).edit().putBoolean("autoClose", v).apply()

    fun remember(c: Context) = sp(c).getBoolean("remember", true)
    fun setRemember(c: Context, v: Boolean) = sp(c).edit().putBoolean("remember", v).apply()

    fun resume(c: Context) = sp(c).getBoolean("resume", true)
    fun setResume(c: Context, v: Boolean) = sp(c).edit().putBoolean("resume", v).apply()

    fun hasPos(c: Context) = sp(c).contains("x")
    fun posX(c: Context) = sp(c).getInt("x", 0)
    fun posY(c: Context) = sp(c).getInt("y", 0)
    fun savePos(c: Context, x: Int, y: Int) = sp(c).edit().putInt("x", x).putInt("y", y).apply()

    // size the user dragged the window to (h = 0 means "height wraps the content")
    fun hasSize(c: Context) = sp(c).contains("w")
    fun sizeW(c: Context) = sp(c).getInt("w", 0)
    fun sizeH(c: Context) = sp(c).getInt("h", 0)
    fun saveSize(c: Context, w: Int, h: Int) = sp(c).edit().putInt("w", w).putInt("h", h).apply()

    // player tools
    fun boost(c: Context) = sp(c).getInt("boost", 0)
    fun setBoost(c: Context, v: Int) = sp(c).edit().putInt("boost", v).apply()
    fun skipSilence(c: Context) = sp(c).getBoolean("skipSilence", false)
    fun setSkipSilence(c: Context, v: Boolean) = sp(c).edit().putBoolean("skipSilence", v).apply()
    fun eq(c: Context) = sp(c).getInt("eq", 0)
    fun setEq(c: Context, v: Int) = sp(c).edit().putInt("eq", v).apply()
    fun wave(c: Context) = sp(c).getBoolean("wave", false)
    fun setWave(c: Context, v: Boolean) = sp(c).edit().putBoolean("wave", v).apply()
    fun tools(c: Context) = sp(c).getBoolean("tools", false)
    fun setTools(c: Context, v: Boolean) = sp(c).edit().putBoolean("tools", v).apply()

    // last folder opened for "play a folder"
    fun lastFolder(c: Context): String? = sp(c).getString("folder", null)
    fun setLastFolder(c: Context, v: String) = sp(c).edit().putString("folder", v).apply()
}
