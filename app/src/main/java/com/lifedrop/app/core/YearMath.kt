package com.lifedrop.app.core
import java.time.LocalDate

data class YearProgress(val dayOfYear: Int, val totalDays: Int) {
    val daysLeft get() = totalDays - dayOfYear
    val fraction get() = dayOfYear / totalDays.toFloat()
    val percent get() = (fraction * 100).toInt()
}

object YearMath {
    // Uses the real year length (365 or 366), never a hardcoded value.
    fun of(date: LocalDate) = YearProgress(date.dayOfYear, date.lengthOfYear())
}
