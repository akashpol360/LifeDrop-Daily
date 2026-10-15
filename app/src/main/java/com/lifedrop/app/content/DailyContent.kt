package com.lifedrop.app.content
import android.content.Context
import org.json.JSONArray
import java.time.LocalDate

data class Drop(val question: String, val challenge: String)

object DailyContent {
    fun load(ctx: Context): List<Drop> {
        val text = ctx.assets.open("drops.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(text)
        return List(arr.length()) { i -> arr.getJSONObject(i).let { Drop(it.getString("q"), it.getString("c")) } }
    }
    // Deterministic: same date always yields the same drop.
    fun forDate(list: List<Drop>, date: LocalDate): Drop =
        list[Math.floorMod(date.toEpochDay(), list.size.toLong()).toInt()]
}
