package com.example.countdown

import android.content.Context
import android.graphics.Color
import java.util.Calendar

data class Config(
    var title: String, var target: Long,
    var color: Int, var glow: Int, var dim: Int
) {
    fun save(c: Context) = c.getSharedPreferences("cfg", 0).edit()
        .putString("title", title).putLong("target", target)
        .putInt("color", color).putInt("glow", glow).putInt("dim", dim).apply()

    companion object {
        fun load(c: Context): Config {
            val sp = c.getSharedPreferences("cfg", 0)
            val def = Calendar.getInstance().apply { set(2026, Calendar.DECEMBER, 17, 0, 0, 0) }.timeInMillis
            return Config(
                sp.getString("title", "DOOMSDAY IS COMING")!!, sp.getLong("target", def),
                sp.getInt("color", Color.WHITE), sp.getInt("glow", Color.parseColor("#39FF14")),
                sp.getInt("dim", 90)
            )
        }
    }
}
