package com.lifedrop.app.notify
import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.lifedrop.app.MainActivity
import com.lifedrop.app.R
import com.lifedrop.app.content.Extra
import com.lifedrop.app.core.YearMath
import com.lifedrop.app.data.LifeDb
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object NotifyExtras {
    private fun allowed(ctx: Context) = Build.VERSION.SDK_INT < 33 ||
        ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Optional always-visible "Day X/Y" notification (user setting). */
    fun updateOngoing(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (!ctx.getSharedPreferences("lifedrop", 0).getBoolean("ongoing", false)) { nm.cancel(100); return }
        if (!allowed(ctx)) return
        nm.createNotificationChannel(NotificationChannel("status", "Year status", NotificationManager.IMPORTANCE_LOW))
        val p = YearMath.of(LocalDate.now())
        val open = PendingIntent.getActivity(ctx, 12, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        nm.notify(100, Notification.Builder(ctx, "status").setSmallIcon(R.drawable.ic_stat_drop)
            .setContentTitle("💧 LifeDrop Daily").setContentText("Day ${p.dayOfYear}/${p.totalDays} • ${p.daysLeft} days left")
            .setOngoing(true).setOnlyAlertOnce(true).setContentIntent(open).build())
    }

    /** Morning alerts for custom events that are today, tomorrow or in 3 days. */
    suspend fun eventAlerts(ctx: Context) {
        if (!allowed(ctx)) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("daily", "Daily reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val today = LocalDate.now()
        LifeDb.get(ctx).dao().events().first().forEach { e ->
            val d = ChronoUnit.DAYS.between(today, LocalDate.parse(e.date))
            val msg = when (d) { 0L -> "is today!"; 1L -> "is tomorrow."; 3L -> "is in 3 days."; else -> return@forEach }
            val open = PendingIntent.getActivity(ctx, 13, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            nm.notify(200 + e.id.toInt(), Notification.Builder(ctx, "daily").setSmallIcon(R.drawable.ic_stat_drop)
                .setContentTitle("🎯 ${e.title}").setContentText("Your event $msg You've got this!").setContentIntent(open).setAutoCancel(true).build())
        }
    }

    /** Morning alert for sky events (eclipses, meteor showers, planet oppositions, solstices) that are today or tomorrow. */
    suspend fun skyAlerts(ctx: Context) {
        if (!allowed(ctx)) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("daily", "Daily reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val today = LocalDate.now()
        val open = PendingIntent.getActivity(ctx, 14, Intent(ctx, MainActivity::class.java).putExtra("screen", "festivals"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        Extra.calendar(ctx, listOf(today.year, today.year + 1)).filter { it.type == "sky" && ChronoUnit.DAYS.between(today, it.date) in 0..1 }.forEachIndexed { i, e ->
            val msg = if (e.date == today) "today" else "tomorrow"
            nm.notify(300 + i, Notification.Builder(ctx, "daily").setSmallIcon(R.drawable.ic_stat_drop).setContentTitle("🌌 ${e.name}")
                .setContentText("${e.name} is $msg. ${e.note}").setStyle(Notification.BigTextStyle().bigText("${e.name} is $msg. ${e.note}")).setContentIntent(open).setAutoCancel(true).build())
        }
    }
}
