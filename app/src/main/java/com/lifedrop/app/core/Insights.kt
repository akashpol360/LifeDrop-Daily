package com.lifedrop.app.core
import com.lifedrop.app.data.Reflection
import com.lifedrop.app.data.XpEvent
import java.time.LocalDate

data class Review(val activeDays: Int, val drops: Int, val challenges: Int, val reflections: Int, val xp: Int, val longest: Int, val events: Int)

object Insights {
    fun review(xp: List<XpEvent>, journal: List<Reflection>, year: Int, eventCount: Int): Review {
        val y = xp.filter { it.date.startsWith("$year-") }
        val days = y.map { LocalDate.parse(it.date) }.toSortedSet().toList()
        var best = 0; var run = 0
        days.forEachIndexed { i, d -> run = if (i > 0 && days[i - 1].plusDays(1) == d) run + 1 else 1; if (run > best) best = run }
        return Review(days.size, y.count { it.type == "drop" }, y.count { it.type == "challenge" },
            journal.count { it.date.startsWith("$year-") }, y.sumOf { it.xp }, best, eventCount)
    }
    fun monthlyXp(xp: List<XpEvent>, year: Int) = IntArray(12).also { a ->
        xp.filter { it.date.startsWith("$year-") }.forEach { a[LocalDate.parse(it.date).monthValue - 1] += it.xp }
    }
}
