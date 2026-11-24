package com.example.countdown

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var cfg: Config
    private lateinit var dateBtn: Button
    private val fmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    private val pickBg = registerForActivityResult(ActivityResultContracts.GetContent()) { it?.let(::saveBg) }
    private val pickFont = registerForActivityResult(ActivityResultContracts.GetContent()) { u ->
        u?.let { contentResolver.openInputStream(it)?.use { i -> File(filesDir, "font.ttf").outputStream().use { o -> i.copyTo(o) } }
            toast("Font saved") }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    private fun btn(t: String, f: () -> Unit) = Button(this).apply { text = t; setOnClickListener { f() } }
    private fun field(hint: String, v: String) = EditText(this).apply { this.hint = hint; setText(v); inputType = InputType.TYPE_CLASS_TEXT }
    private fun hex(i: Int) = String.format("#%06X", 0xFFFFFF and i)

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        cfg = Config.load(this)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 40, 40, 40) }
        val title = field("Title text", cfg.title)
        val color = field("Text colour (#RRGGBB)", hex(cfg.color))
        val glow = field("Glow colour (#RRGGBB)", hex(cfg.glow))
        val dim = SeekBar(this).apply { max = 230; progress = cfg.dim }
        dateBtn = btn(fmt.format(Date(cfg.target))) { pickDate() }

        col.addView(TextView(this).apply { text = "Title" }); col.addView(title)
        col.addView(TextView(this).apply { text = "Target date & time" }); col.addView(dateBtn)
        col.addView(btn("Choose background image") { pickBg.launch("image/*") })
        col.addView(btn("Choose custom font (.ttf)") { pickFont.launch("*/*") })
        col.addView(color); col.addView(glow)
        col.addView(TextView(this).apply { text = "Background darkness" }); col.addView(dim)
        col.addView(btn("Save & update widget") {
            cfg.title = title.text.toString()
            runCatching { cfg.color = Color.parseColor(color.text.toString()) }
            runCatching { cfg.glow = Color.parseColor(glow.text.toString()) }
            cfg.dim = dim.progress
            cfg.save(this); CountdownWidget.refreshAll(this); toast("Saved")
        })
        col.addView(btn("Add widget to home screen") {
            val m = AppWidgetManager.getInstance(this)
            if (m.isRequestPinAppWidgetSupported)
                m.requestPinAppWidget(ComponentName(this, CountdownWidget::class.java), null, null)
            else toast("Long-press home screen > Widgets > Countdown Widget")
        })
        setContentView(ScrollView(this).apply { addView(col) })
    }

    private fun pickDate() {
        val cal = Calendar.getInstance().apply { timeInMillis = cfg.target }
        DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d)
            TimePickerDialog(this, { _, h, mi ->
                cal.set(Calendar.HOUR_OF_DAY, h); cal.set(Calendar.MINUTE, mi); cal.set(Calendar.SECOND, 0)
                cfg.target = cal.timeInMillis; dateBtn.text = fmt.format(cal.time)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun saveBg(u: Uri) {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, o) }
        var s = 1; while (o.outWidth / s > 1600) s *= 2
        val b = contentResolver.openInputStream(u)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = s })
        } ?: return
        File(filesDir, "bg.jpg").outputStream().use { b.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        toast("Background saved - press Save & update")
    }
}
