package com.lifedrop.app.data
import android.content.Context
import androidx.compose.runtime.*
import java.time.LocalDate

/** Observable settings backed by SharedPreferences. */
class AppPrefs(ctx: Context) {
    private val p = ctx.getSharedPreferences("lifedrop", 0)
    var premium by mutableStateOf(p.getBoolean("premium", false)); private set
    var animMode by mutableStateOf(p.getInt("animMode", if (p.getBoolean("anim", true)) 0 else 2)); private set
    /** 0 = full, 1 = reduced (celebrations only), 2 = off. */
    val anim: Boolean get() = animMode != 2
    var theme by mutableStateOf(p.getString("theme", "ocean")!!); private set
    @JvmName("updatePremium") fun setPremium(v: Boolean) { premium = v; p.edit().putBoolean("premium", v).apply() }
    @JvmName("updateAnimMode") fun setAnimMode(m: Int) { animMode = m; p.edit().putInt("animMode", m).apply() }
    @JvmName("updateTheme") fun setTheme(v: String) { theme = v; p.edit().putString("theme", v).apply() }
    var onboarded by mutableStateOf(p.getBoolean("onb", false)); private set
    /** Topics picked in onboarding (stored for later personalisation). */
    fun finishOnboarding(interests: Set<String>) { onboarded = true; p.edit().putBoolean("onb", true).putStringSet("interests", interests).apply() }
    var morningMin by mutableStateOf(p.getInt("mmin", 480)); private set
    var eveningMin by mutableStateOf(p.getInt("emin", 1200)); private set
    var ongoing by mutableStateOf(p.getBoolean("ongoing", false)); private set
    fun setReminder(kind: String, min: Int) {
        if (kind == "morning") { morningMin = min; p.edit().putInt("mmin", min).apply() }
        else { eveningMin = min; p.edit().putInt("emin", min).apply() }
    }
    @JvmName("updateOngoing") fun setOngoing(v: Boolean) { ongoing = v; p.edit().putBoolean("ongoing", v).apply() }
    fun lastYear(): Int = p.getInt("lastYear", 0)
    fun setLastYear(y: Int) { p.edit().putInt("lastYear", y).apply() }
    var font by mutableStateOf(p.getString("font", "default")!!); private set
    @JvmName("updateFont") fun setFont(v: String) { font = v; p.edit().putString("font", v).apply() }
    fun lastLevel(): Int = p.getInt("lastLevel", 0)
    fun setLastLevel(v: Int) { p.edit().putInt("lastLevel", v).apply() }
    fun lastDropAnim(): String = p.getString("dropAnim", "")!!
    fun setLastDropAnim(v: String) { p.edit().putString("dropAnim", v).apply() }
    fun seenBadges(): Set<String> = p.getStringSet("badges", emptySet())!!.toSet()
    fun markSeen(s: Set<String>) { p.edit().putStringSet("badges", s).apply() }
}
