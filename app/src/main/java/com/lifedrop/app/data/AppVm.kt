package com.lifedrop.app.data
import android.app.Application
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifedrop.app.widget.LifeWidget
import com.lifedrop.app.widget.StreakWidget
import com.lifedrop.app.core.Stats
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class AppVm(app: Application) : AndroidViewModel(app) {
    private val dao = LifeDb.get(app).dao()
    private fun <T> Flow<T>.hot(init: T) = stateIn(viewModelScope, SharingStarted.Eagerly, init)
    val reflections = dao.reflections().hot(emptyList())
    val xp = dao.xp().hot(emptyList())
    val events = dao.events().hot(emptyList())
    val favs = dao.favs().hot(emptyList())
    val answers = dao.answers().hot(emptyList())
    /** Every change that the widget shows also refreshes the widget. */
    private fun run(block: suspend () -> Unit) = viewModelScope.launch {
        block()
        runCatching { LifeWidget().updateAll(getApplication<Application>()); StreakWidget().updateAll(getApplication<Application>()) }
    }
    fun award(date: LocalDate, type: String, points: Int) = run { dao.addXp(XpEvent("$date:$type", date.toString(), type, points)); bonus(date) }
    fun saveReflection(id: Long, date: String, text: String, mood: String, tags: String) = run {
        dao.saveReflection(Reflection(id = id, date = date, text = text.trim(), mood = mood, tags = tags.trim()))
        if (id == 0L) dao.addXp(XpEvent("$date:reflect", date, "reflect", 15))
        bonus(LocalDate.parse(date))
    }
    fun deleteReflection(r: Reflection) = run { dao.deleteReflection(r) }
    fun saveAnswer(qid: String, text: String, today: LocalDate) = run {
        dao.saveAnswer(Answer(qid, text.trim(), today.toString()))
        dao.addXp(XpEvent("$today:think", today.toString(), "think", 5))
        bonus(today)
    }
    /** +10 XP when all four daily actions are done, and streak bonuses at 3, 7, 14, 30, 60 and 100 days (each once). */
    private suspend fun bonus(date: LocalDate) {
        val all = dao.xpList()
        val types = all.filter { it.date == date.toString() }.map { it.type }.toSet()
        if (setOf("drop", "think", "challenge", "reflect").all { it in types }) dao.addXp(XpEvent("$date:all", date.toString(), "all", 10))
        val streak = Stats.summary(dao.xpList(), date).streak
        if (streak in setOf(3, 7, 14, 30, 60, 100)) dao.addXp(XpEvent("$date:streak$streak", date.toString(), "streak", if (streak >= 30) 100 else if (streak >= 7) 50 else 25))
    }
    fun deleteJournal() = run { dao.clearReflections() }
    fun resetAll() = run { dao.clearXp(); dao.clearReflections(); dao.clearEvents(); dao.clearFavs(); dao.clearAnswers() }
    fun addEvent(title: String, date: LocalDate) = run { dao.addEvent(CustomEvent(title = title.trim(), date = date.toString())) }
    fun updateEvent(old: CustomEvent, title: String, date: LocalDate) = run { dao.updateEvent(old.copy(title = title.trim(), date = date.toString())) }
    fun deleteEvent(e: CustomEvent) = run { dao.deleteEvent(e) }
    fun toggleFav(id: String, on: Boolean) = run { if (on) dao.delFav(id) else dao.addFav(Favorite(id)) }
}
