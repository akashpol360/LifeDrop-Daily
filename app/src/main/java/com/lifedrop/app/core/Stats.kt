package com.lifedrop.app.core
import com.lifedrop.app.data.XpEvent
import java.time.LocalDate

data class Summary(val xp: Int, val level: Int, val streak: Int, val drops: Int)
data class Badge(val emoji: String, val name: String, val unlocked: Boolean, val hint: String = "")

object Stats {
    const val XP_PER_LEVEL = 100
    /** XP earned inside the current level (0-99). */
    fun xpIntoLevel(xp: Int) = xp % XP_PER_LEVEL
    /** Ten named tiers; there is no maximum level, and everything above 10 stays "Ocean Legend". */
    fun levelTitle(level: Int) = listOf("Little Drop", "Puddle", "Stream", "River", "Lake", "Waterfall", "Rainmaker", "Tide Master", "Sea Guardian", "Ocean Legend").getOrElse(level - 1) { "Ocean Legend" }

    /** The streak stays alive through today, so a day that hasn't ended yet never breaks it. */
    fun summary(events: List<XpEvent>, today: LocalDate, protect: Boolean = false): Summary {
        val days = events.map { LocalDate.parse(it.date) }.toSet()
        var d = if (today in days) today else today.minusDays(1)
        var n = 0
        var spare = protect // Premium streak protection: one missed day can be bridged
        while (true) {
            if (d in days) { n++; d = d.minusDays(1) }
            else if (spare && d.minusDays(1) in days) { spare = false; d = d.minusDays(1) }
            else break
        }
        val total = events.sumOf { it.xp }
        return Summary(total, total / 100 + 1, n, events.filter { it.type == "drop" }.map { it.date }.toSet().size)
    }
    fun badges(s: Summary, journal: Int, events: Int, answers: Int = 0) = listOf(
        Badge("🌱", "First Drop", s.drops >= 1, "Tap \"Think about it\" on your first Daily Drop."),
        Badge("🧠", "Thinker (5 answers)", answers >= 5, "Save 5 answers in the Think tab."),
        Badge("🔥", "7 Day Streak", s.streak >= 7, "Be active 7 days in a row."),
        Badge("🌟", "30 Day Streak", s.streak >= 30, "Be active 30 days in a row."),
        Badge("👑", "100 Day Streak", s.streak >= 100, "Be active 100 days in a row."),
        Badge("💧", "30 Drops", s.drops >= 30, "Complete 30 Daily Drops."),
        Badge("📖", "Journaler (5 entries)", journal >= 5, "Write 5 journal reflections."),
        Badge("🎯", "Goal Setter", events >= 1, "Add your first custom event."),
        Badge("🏆", "100 Drops", s.drops >= 100, "Complete 100 Daily Drops."))
}
