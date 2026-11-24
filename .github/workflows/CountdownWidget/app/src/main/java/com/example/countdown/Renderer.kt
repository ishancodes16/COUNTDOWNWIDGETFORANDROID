package com.example.countdown

import android.content.Context
import android.graphics.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object Renderer {
    fun render(c: Context, w: Int = 800, h: Int = 400): Bitmap {
        val cfg = Config.load(c)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val wf = w.toFloat(); val hf = h.toFloat()
        cv.clipPath(Path().apply { addRoundRect(RectF(0f, 0f, wf, hf), 40f, 40f, Path.Direction.CW) })
        cv.drawColor(Color.BLACK)

        File(c.filesDir, "bg.jpg").takeIf { it.exists() }?.let { f ->
            BitmapFactory.decodeFile(f.path)?.let { b ->
                val s = maxOf(wf / b.width, hf / b.height)
                val dw = b.width * s; val dh = b.height * s
                cv.drawBitmap(b, null, RectF((wf - dw) / 2, (hf - dh) / 2, (wf + dw) / 2, (hf + dh) / 2), Paint(Paint.FILTER_BITMAP_FLAG))
            }
        }
        cv.drawColor(Color.argb(cfg.dim, 0, 0, 0))

        val font = File(c.filesDir, "font.ttf").takeIf { it.exists() }
            ?.let { runCatching { Typeface.createFromFile(it) }.getOrNull() }
            ?: Typeface.create("sans-serif-condensed", Typeface.BOLD)
        fun paint(size: Float, glow: Boolean) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font; textSize = size; color = cfg.color; textAlign = Paint.Align.CENTER
            if (glow) setShadowLayer(size * 0.5f, 0f, 0f, cfg.glow)
        }

        // Title (shrinks to fit)
        val t = paint(hf * 0.13f, true).apply { letterSpacing = 0.12f }
        while (t.measureText(cfg.title) > wf * 0.9f && t.textSize > 10f) t.textSize -= 2f
        cv.drawText(cfg.title.uppercase(), wf / 2, hf * 0.22f, t)

        // Countdown
        val diff = maxOf(0L, cfg.target - System.currentTimeMillis())
        val mins = diff / 60000
        val vals = listOf(mins / 1440, (mins / 60) % 24, mins % 60)
        val labels = listOf("DAYS", "HOURS", "MINUTES")
        val n = paint(hf * 0.30f, true); val l = paint(hf * 0.07f, false).apply { letterSpacing = 0.2f }
        vals.forEachIndexed { i, v ->
            val x = wf * (i + 0.5f) / 3
            cv.drawText("%02d".format(v), x, hf * 0.62f, n)
            cv.drawText(labels[i], x, hf * 0.74f, l)
        }

        // Current time + target date
        val small = paint(hf * 0.075f, false)
        small.textAlign = Paint.Align.LEFT
        cv.drawText("NOW " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), wf * 0.05f, hf * 0.93f, small)
        small.textAlign = Paint.Align.RIGHT
        cv.drawText(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(cfg.target)).uppercase(), wf * 0.95f, hf * 0.93f, small)
        return bmp
    }
}
