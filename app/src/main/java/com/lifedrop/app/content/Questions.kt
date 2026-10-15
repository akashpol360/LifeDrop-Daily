package com.lifedrop.app.content
import android.content.Context
import org.json.JSONArray

data class Question(val id: String, val cat: String, val text: String)

object Questions {
    /** File format: [["Category","Question text"], ...]. Ids are prefix+index, so keep the order stable. */
    fun load(ctx: Context, file: String, prefix: String): List<Question> {
        val a = JSONArray(ctx.assets.open(file).bufferedReader().use { it.readText() })
        return List(a.length()) { i -> a.getJSONArray(i).let { Question("$prefix$i", it.getString(0), it.getString(1)) } }
    }
}
