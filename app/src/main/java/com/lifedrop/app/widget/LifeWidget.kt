package com.lifedrop.app.widget
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.action.actionStartActivity
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.color.ColorProvider
import com.lifedrop.app.MainActivity
import com.lifedrop.app.content.*
import com.lifedrop.app.core.*
import com.lifedrop.app.data.LifeDb
import com.lifedrop.app.data.AppPrefs
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val TabKey = ActionParameters.Key<Int>("tab")
private val ScreenKey = ActionParameters.Key<String>("screen")
private fun open(tab: Int, screen: String = "") = actionStartActivity<MainActivity>(actionParametersOf(TabKey to tab, ScreenKey to screen))

class LifeWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(110.dp, 110.dp), DpSize(250.dp, 110.dp), DpSize(250.dp, 250.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = LocalDate.now()
        val p = YearMath.of(today)
        val drop = DailyContent.forDate(DailyContent.load(context), today)
        val fest = Extra.upcomingFestivals(context, today).firstOrNull()
        val s = Stats.summary(LifeDb.get(context).dao().xp().first(), today, AppPrefs(context).premium)
        provideContent { Body(p, drop, fest, s, today) }
    }

    @Composable
    private fun Body(p: YearProgress, drop: Drop, fest: Festival?, s: Summary, today: LocalDate) {
        val size = LocalSize.current
        val wide = size.width >= 200.dp
        val tall = size.height >= 200.dp
        val fg = ColorProvider(day = Color(0xFF1B3A57), night = Color(0xFFE1F5FE))
        val bg = ColorProvider(day = Color(0xFFE1F5FE), night = Color(0xFF0D2137))
        fun st(sz: Int, bold: Boolean = false) = TextStyle(color = fg, fontSize = sz.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Column(GlanceModifier.fillMaxSize().background(bg).cornerRadius(24.dp).padding(12.dp).clickable(open(0))) {
            Text("💧 LifeDrop Daily", style = st(12, true))
            Text("${p.dayOfYear} / ${p.totalDays}", style = st(if (wide) 32 else 26, true), modifier = GlanceModifier.clickable(open(0, "year")))
            Text("${p.daysLeft} DAYS LEFT", style = st(13, true))
            if (wide) {
                Text("🔥 ${s.streak} day streak   ⭐ ${s.xp} XP", style = st(12))
                if (fest != null) Text("🎉 ${fest.name}: ${ChronoUnit.DAYS.between(today, fest.date)} days", style = st(12), maxLines = 1)
            } else Text("Make today count.", style = st(11), maxLines = 1)
            if (tall) {
                Spacer(GlanceModifier.height(8.dp))
                Text("🧠 TODAY'S QUESTION", style = st(11, true))
                Text("\"${drop.question}\"", style = st(14), maxLines = 3, modifier = GlanceModifier.clickable(open(2)))
                Spacer(GlanceModifier.height(6.dp))
                Text("💪 ${drop.challenge}", style = st(12), maxLines = 2)
                Spacer(GlanceModifier.height(6.dp))
                Text("📖 Tap to reflect", style = st(12, true), modifier = GlanceModifier.clickable(open(3)))
            } else if (wide) {
                Text("\"${drop.question}\"", style = st(12), maxLines = 2)
            }
        }
    }
}

class LifeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LifeWidget()
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // Midnight / manual date or timezone change: redraw so the day number is right without opening the app.
        if (intent.action in setOf(Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)) {
            val pr = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try { LifeWidget().updateAll(context); ProgressWidget().updateAll(context); StreakWidget().updateAll(context); com.lifedrop.app.notify.NotifyExtras.updateOngoing(context) } finally { pr.finish() }
            }
        }
    }
}

/** Premium widget: the whole year as 20 segments. Free users see an unlock message. */
class ProgressWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = LocalDate.now()
        val p = YearMath.of(today)
        val premium = AppPrefs(context).premium
        provideContent {
            val fg = ColorProvider(day = Color(0xFF1B3A57), night = Color(0xFFE1F5FE))
            val bg = ColorProvider(day = Color(0xFFE1F5FE), night = Color(0xFF0D2137))
            val on = ColorProvider(day = Color(0xFF0288D1), night = Color(0xFF4FC3F7))
            val off = ColorProvider(day = Color(0x33455A64), night = Color(0x33E1F5FE))
            fun st(sz: Int, bold: Boolean = false) = TextStyle(color = fg, fontSize = sz.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
            Column(GlanceModifier.fillMaxSize().background(bg).cornerRadius(24.dp).padding(14.dp).clickable(open(0))) {
                Text("💧 Year progress", style = st(12, true))
                if (!premium) Text("🔒 This widget is part of Premium.", style = st(14))
                else {
                    Text("${p.percent}%", style = st(34, true))
                    Text("Day ${p.dayOfYear} / ${p.totalDays} • ${p.daysLeft} days left", style = st(13))
                    Spacer(GlanceModifier.height(8.dp))
                    val filled = (p.fraction * 20).toInt()
                    Row(GlanceModifier.fillMaxWidth()) {
                        for (i in 0 until 20) Box(GlanceModifier.defaultWeight().height(14.dp).padding(end = 2.dp)) {
                            Box(GlanceModifier.fillMaxSize().background(if (i < filled) on else off).cornerRadius(3.dp)) {}
                        }
                    }
                }
            }
        }
    }
}

class ProgressWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ProgressWidget()
}

/** Premium widget: streak, XP and progress to the next level. */
class StreakWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = LocalDate.now()
        val premium = AppPrefs(context).premium
        val s = Stats.summary(LifeDb.get(context).dao().xp().first(), today, premium)
        provideContent {
            val fg = ColorProvider(day = Color(0xFF1B3A57), night = Color(0xFFE1F5FE))
            val bg = ColorProvider(day = Color(0xFFFFF3E0), night = Color(0xFF2B1410))
            val on = ColorProvider(day = Color(0xFFFF7043), night = Color(0xFFFF8A65))
            val off = ColorProvider(day = Color(0x33455A64), night = Color(0x33E1F5FE))
            fun st(sz: Int, bold: Boolean = false) = TextStyle(color = fg, fontSize = sz.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
            Column(GlanceModifier.fillMaxSize().background(bg).cornerRadius(24.dp).padding(14.dp).clickable(open(4))) {
                Text("💧 Streak and level", style = st(12, true))
                if (!premium) Text("🔒 This widget is part of Premium.", style = st(14))
                else {
                    Text("🔥 ${s.streak} days", style = st(30, true))
                    Text("Level ${s.level} · ${Stats.levelTitle(s.level)}", style = st(13))
                    Text("⭐ ${s.xp} XP · ${Stats.xpIntoLevel(s.xp)}/${Stats.XP_PER_LEVEL} to next level", style = st(12))
                    Spacer(GlanceModifier.height(8.dp))
                    val filled = Stats.xpIntoLevel(s.xp) / 10
                    Row(GlanceModifier.fillMaxWidth()) {
                        for (i in 0 until 10) Box(GlanceModifier.defaultWeight().height(12.dp).padding(end = 2.dp)) {
                            Box(GlanceModifier.fillMaxSize().background(if (i < filled) on else off).cornerRadius(3.dp)) {}
                        }
                    }
                }
            }
        }
    }
}

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
}
