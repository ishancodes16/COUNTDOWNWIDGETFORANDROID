package com.example.countdown

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class CountdownWidget : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) { push(c, m, ids); schedule(c) }
    override fun onEnabled(c: Context) = schedule(c)
    override fun onDisabled(c: Context) {
        (c.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(tickIntent(c))
    }
    override fun onReceive(c: Context, i: Intent) {
        super.onReceive(c, i)
        if (i.action == TICK) refreshAll(c)
    }

    companion object {
        const val TICK = "com.example.countdown.TICK"
        private fun tickIntent(c: Context) = PendingIntent.getBroadcast(
            c, 0, Intent(c, CountdownWidget::class.java).setAction(TICK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        fun schedule(c: Context) {
            val next = (System.currentTimeMillis() / 60000 + 1) * 60000
            (c.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
                .setAndAllowWhileIdle(AlarmManager.RTC, next, tickIntent(c))
        }

        fun push(c: Context, m: AppWidgetManager, ids: IntArray) {
            if (ids.isEmpty()) return
            val open = PendingIntent.getActivity(c, 1, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            val bmp = Renderer.render(c)
            for (id in ids) {
                val rv = RemoteViews(c.packageName, R.layout.widget)
                rv.setImageViewBitmap(R.id.img, bmp)
                rv.setOnClickPendingIntent(R.id.img, open)
                m.updateAppWidget(id, rv)
            }
        }

        fun refreshAll(c: Context) {
            val m = AppWidgetManager.getInstance(c)
            val ids = m.getAppWidgetIds(ComponentName(c, CountdownWidget::class.java))
            push(c, m, ids)
            if (ids.isNotEmpty()) schedule(c)
        }
    }
}
