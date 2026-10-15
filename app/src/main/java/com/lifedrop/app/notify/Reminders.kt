package com.lifedrop.app.notify
import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.glance.appwidget.updateAll
import com.lifedrop.app.MainActivity
import com.lifedrop.app.R
import com.lifedrop.app.content.Extra
import com.lifedrop.app.widget.LifeWidget
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object Reminders {
    fun schedule(ctx: Context) {
        val sp = ctx.getSharedPreferences("lifedrop", 0)
        arm(ctx, "morning", sp.getInt("mmin", 480), 1); arm(ctx, "evening", sp.getInt("emin", 1200), 2)
    }

    /** Inexact daily alarm: no exact-alarm permission and no battery cost; may fire a few minutes late. */
    private fun arm(ctx: Context, kind: String, minutes: Int, code: Int) {
        val now = ZonedDateTime.now()
        var t = now.withHour(minutes / 60).withMinute(minutes % 60).withSecond(0).withNano(0)
        if (!t.isAfter(now)) t = t.plusDays(1)
        val pi = PendingIntent.getBroadcast(ctx, code, Intent(ctx, ReminderReceiver::class.java).putExtra("kind", kind),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        ctx.getSystemService(AlarmManager::class.java)
            .setInexactRepeating(AlarmManager.RTC, t.toInstant().toEpochMilli(), AlarmManager.INTERVAL_DAY, pi)
    }

    fun show(ctx: Context, kind: String) {
        if (Build.VERSION.SDK_INT >= 33 && ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("daily", "Daily reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val morning = kind == "morning"
        var body = if (morning) "Your next LifeDrop is waiting." else "Take a moment to reflect."
        if (morning) Extra.upcomingFestivals(ctx, LocalDate.now()).firstOrNull()?.let {
            val d = ChronoUnit.DAYS.between(LocalDate.now(), it.date)
            if (d <= 3) body += "\n🎉 ${it.name} is ${if (d == 0L) "today" else "in $d days"}!"
        }
        val open = PendingIntent.getActivity(ctx, if (morning) 10 else 11,
            Intent(ctx, MainActivity::class.java).putExtra("tab", if (morning) 0 else 3),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        nm.notify(if (morning) 1 else 2, Notification.Builder(ctx, "daily").setSmallIcon(R.drawable.ic_stat_drop)
            .setContentTitle(if (morning) "☀️ Good morning!" else "🌙 Before today ends...")
            .setContentText(body).setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(open).setAutoCancel(true).build())
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val kind = i.getStringExtra("kind") ?: return
        val on = c.getSharedPreferences("lifedrop", 0).getBoolean(kind, true)
        val pr = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try { LifeWidget().updateAll(c); if (on) Reminders.show(c, kind); if (kind == "morning") { NotifyExtras.eventAlerts(c); if (c.getSharedPreferences("lifedrop", 0).getBoolean("sky", true)) NotifyExtras.skyAlerts(c) }; NotifyExtras.updateOngoing(c) } finally { pr.finish() }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Reminders.schedule(c)
        val pr = goAsync()
        CoroutineScope(Dispatchers.Default).launch { try { LifeWidget().updateAll(c); NotifyExtras.updateOngoing(c) } finally { pr.finish() } }
    }
}
