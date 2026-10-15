package com.lifedrop.app.content
import android.content.Context
import com.lifedrop.app.core.Panchang
import org.json.JSONArray
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** type: fest = festival, hol = government holiday, obs = special day, sky = astronomy event, moon = moon phase. */
data class Festival(val emoji: String, val name: String, val date: LocalDate, val type: String = "fest", val note: String = "")
data class FeedItem(val id: String, val type: String, val text: String)

private fun read(ctx: Context, f: String) = JSONArray(ctx.assets.open(f).bufferedReader().use { it.readText() })

object Extra {
    private class Row(val name: String, val emoji: String, val type: String, val w: String, val note: String)
    private val cache = HashMap<Int, List<Festival>>()
    private var rows: List<Row>? = null

    /** calendar.json rows: [name, emoji, type, when, note?]. when = "MM-DD" (every year), "YYYY-MM-DD" (that day only) or "R:month-weekday-nth" (R:5-7-2 = 2nd Sunday of May). */
    private fun rows(ctx: Context): List<Row> = rows ?: read(ctx, "calendar.json").let { a ->
        List(a.length()) { i -> a.getJSONArray(i).let { o -> Row(o.getString(0), o.getString(1), o.getString(2), o.getString(3), if (o.length() > 4) o.getString(4) else "") } }
    }.also { rows = it }

    /**
     * All events of one year. Government-verified dates in calendar.json (2026-2027) win; every other festival, eclipse,
     * moon phase, solstice and planet event is calculated from sun and moon positions (see core/Panchang.java), so any year works.
     */
    fun year(ctx: Context, y: Int): List<Festival> = synchronized(cache) {
        cache.getOrPut(y) {
            val json = ArrayList<Festival>()
            rows(ctx).forEach { r ->
                when {
                    r.w.startsWith("R:") -> {
                        val (m, wd, n) = r.w.drop(2).split("-").map(String::toInt)
                        json += Festival(r.emoji, r.name, LocalDate.of(y, m, 1).with(TemporalAdjusters.dayOfWeekInMonth(n, DayOfWeek.of(wd))), r.type, r.note)
                    }
                    r.w.length == 5 -> { val (m, d) = r.w.split("-").map(String::toInt); json += Festival(r.emoji, r.name, LocalDate.of(y, m, d), r.type, r.note) }
                    else -> { val d = LocalDate.parse(r.w); if (d.year == y) json += Festival(r.emoji, r.name, d, r.type, r.note) }
                }
            }
            val names = json.map { it.name.lowercase() }.toSet()
            val computed = Panchang.generate(y).filter { e ->
                e.name.lowercase() !in names &&
                    !(e.name.contains("Eclipse") && json.any { it.name.contains("Eclipse") && Math.abs(ChronoUnit.DAYS.between(it.date, e.date)) <= 1 })
            }.map { Festival(it.emoji, it.name, it.date, it.type, it.note) }
            (json + computed).sortedBy { it.date }
        }
    }
    fun calendar(ctx: Context, years: List<Int>): List<Festival> = years.flatMap { year(ctx, it) }.sortedBy { it.date }
    fun upcomingFestivals(ctx: Context, today: LocalDate): List<Festival> =
        calendar(ctx, listOf(today.year, today.year + 1)).filter { (it.type == "fest" || it.type == "hol") && it.date >= today }
    fun upcomingSky(ctx: Context, today: LocalDate): List<Festival> =
        calendar(ctx, listOf(today.year, today.year + 1)).filter { it.type == "sky" && it.date >= today }
    fun feed(ctx: Context): List<FeedItem> {
        val a = read(ctx, "feed.json")
        return List(a.length()) { a.getJSONObject(it).let { o -> FeedItem(o.getString("id"), o.getString("t"), o.getString("x")) } }
    }
}
