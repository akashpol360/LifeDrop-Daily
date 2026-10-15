package com.lifedrop.app
import com.lifedrop.app.content.DailyContent
import com.lifedrop.app.content.Drop
import com.lifedrop.app.core.Insights
import com.lifedrop.app.core.Stats
import com.lifedrop.app.core.YearMath
import com.lifedrop.app.data.XpEvent
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class CoreTests {
    private fun ev(d: LocalDate, type: String = "drop") = XpEvent("$d:$type", "$d", type, 5)

    @Test fun normalYearHas365Days() {
        assertEquals(365, YearMath.of(LocalDate.of(2026, 6, 1)).totalDays)
        assertEquals(1, YearMath.of(LocalDate.of(2027, 1, 1)).dayOfYear)
    }
    @Test fun lastDayOfYearHasZeroDaysLeft() {
        val p = YearMath.of(LocalDate.of(2026, 12, 31))
        assertEquals(365, p.dayOfYear); assertEquals(0, p.daysLeft)
    }
    @Test fun leapYearHas366Days() {
        assertEquals(366, YearMath.of(LocalDate.of(2028, 1, 1)).totalDays)
        val p = YearMath.of(LocalDate.of(2028, 12, 31))
        assertEquals(366, p.dayOfYear); assertEquals(0, p.daysLeft)
    }
    @Test fun sept29Of2026IsDay272() {
        val p = YearMath.of(LocalDate.of(2026, 9, 29))
        assertEquals(272, p.dayOfYear); assertEquals(93, p.daysLeft)
    }
    @Test fun dailyDropIsDeterministicAndCycles() {
        val list = List(7) { Drop("q$it", "c$it") }
        val d = LocalDate.of(2026, 9, 29)
        assertEquals(DailyContent.forDate(list, d), DailyContent.forDate(list, d))
        assertNotEquals(DailyContent.forDate(list, d), DailyContent.forDate(list, d.plusDays(1)))
        assertEquals(DailyContent.forDate(list, d), DailyContent.forDate(list, d.plusDays(7)))
    }
    @Test fun streakCountsConsecutiveDaysIncludingToday() {
        val t = LocalDate.of(2026, 9, 29)
        assertEquals(3, Stats.summary(listOf(ev(t), ev(t.minusDays(1)), ev(t.minusDays(2))), t).streak)
    }
    @Test fun streakSurvivesUntilTodayEnds() {
        val t = LocalDate.of(2026, 9, 29)
        assertEquals(2, Stats.summary(listOf(ev(t.minusDays(1)), ev(t.minusDays(2))), t).streak)
    }
    @Test fun streakBreaksAfterAMissedDay() {
        val t = LocalDate.of(2026, 9, 29)
        assertEquals(0, Stats.summary(listOf(ev(t.minusDays(2))), t).streak)
        assertEquals(0, Stats.summary(emptyList(), t).streak)
    }
    @Test fun xpAndLevel() {
        val t = LocalDate.of(2026, 9, 29)
        val s = Stats.summary(List(25) { ev(t.minusDays(it.toLong())) }, t)
        assertEquals(125, s.xp); assertEquals(2, s.level); assertEquals(25, s.drops)
    }
    @Test fun longestStreakInYear() {
        val d = LocalDate.of(2026, 3, 1)
        val evs = listOf(0L, 1L, 2L, 5L, 6L).map { ev(d.plusDays(it)) }
        assertEquals(3, Insights.review(evs, emptyList(), 2026, 0).longest)
    }
    @Test fun reviewIgnoresOtherYears() {
        val evs = listOf(ev(LocalDate.of(2025, 12, 31)), ev(LocalDate.of(2026, 1, 1)))
        assertEquals(1, Insights.review(evs, emptyList(), 2026, 0).drops)
    }
    @Test fun streakProtectionBridgesOneMissedDay() {
        val t = LocalDate.of(2026, 9, 29)
        val evs = listOf(ev(t), ev(t.minusDays(2)), ev(t.minusDays(3)))
        assertEquals(1, Stats.summary(evs, t, false).streak)
        assertEquals(3, Stats.summary(evs, t, true).streak)
    }
    @Test fun levelProgress() {
        assertEquals(35, Stats.xpIntoLevel(35)); assertEquals(0, Stats.xpIntoLevel(100)); assertEquals(5, Stats.xpIntoLevel(205))
        assertEquals("Little Drop", Stats.levelTitle(1)); assertEquals("Ocean Legend", Stats.levelTitle(25))
        // 35 XP is the most one day can give (5 drop + 5 think + 10 challenge + 15 journal), so level 2 needs 100 XP
        assertEquals(2, Stats.summary(List(4) { ev(LocalDate.of(2026, 1, 1).plusDays(it.toLong()), "challenge").copy(xp = 35) }, LocalDate.of(2026, 1, 4)).level)
    }
}
