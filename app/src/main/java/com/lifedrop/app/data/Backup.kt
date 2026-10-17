package com.lifedrop.app.data

import android.content.Context
import android.net.Uri
import androidx.glance.appwidget.updateAll
import androidx.room.withTransaction
import com.lifedrop.app.notify.NotifyExtras
import com.lifedrop.app.notify.Reminders
import com.lifedrop.app.ui.Themes
import com.lifedrop.app.widget.LifeWidget
import com.lifedrop.app.widget.StreakWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate

/** A problem with a backup file that can be shown to the user as is. */
class BackupException(message: String) : Exception(message)

/** Everything stored in one backup. Pro is not part of it: purchases belong to the Google account. */
data class BackupData(
    val createdAt: Long,
    val prefs: JSONObject,
    val reflections: List<Reflection>,
    val xp: List<XpEvent>,
    val events: List<CustomEvent>,
    val favorites: List<Favorite>,
    val answers: List<Answer>
) {
    val isEmpty: Boolean get() = reflections.isEmpty() && xp.isEmpty() && events.isEmpty() && favorites.isEmpty() && answers.isEmpty()
}

/**
 * Backup and restore.
 *  - Manual: the user saves one JSON file anywhere (Drive, Downloads, WhatsApp ...) and restores from it later.
 *  - Automatic: a copy of the same JSON is kept in the app's private files folder. Android's Auto Backup
 *    (see res/xml/data_extraction_rules.xml) saves only that one file to the user's Google backup and puts it back
 *    after a reinstall, when the app offers to restore it.
 */
object Backup {
    const val FORMAT = 1
    private const val APP = "LifeDrop Daily"
    private const val AUTO_FILE = "auto_backup.json"
    private const val MAX_BYTES = 20_000_000
    private const val PREFS = "lifedrop"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var pending: Job? = null

    private fun sp(c: Context) = c.getSharedPreferences(PREFS, 0)
    fun autoEnabled(c: Context) = sp(c).getBoolean("autoBackup", true)
    fun autoFile(c: Context) = File(c.filesDir, AUTO_FILE)
    fun fileName(date: LocalDate = LocalDate.now()) = "LifeDrop-backup-$date.json"
    private fun handled(c: Context) = sp(c).getBoolean("autoRestoreHandled", false)
    fun markHandled(c: Context) { sp(c).edit().putBoolean("autoRestoreHandled", true).apply() }

    // ---------------------------------------------------------------- create

    private fun prefsJson(c: Context): JSONObject {
        val s = sp(c)
        return JSONObject().apply {
            put("theme", s.getString("theme", "ocean") ?: "ocean")
            put("font", s.getString("font", "default") ?: "default")
            put("animMode", s.getInt("animMode", if (s.getBoolean("anim", true)) 0 else 2))
            put("morningMin", s.getInt("mmin", 480))
            put("eveningMin", s.getInt("emin", 1200))
            put("ongoing", s.getBoolean("ongoing", false))
            put("onboarded", s.getBoolean("onb", false))
            put("interests", JSONArray((s.getStringSet("interests", emptySet()) ?: emptySet<String>()).toList()))
            put("lastYear", s.getInt("lastYear", 0))
            put("lastLevel", s.getInt("lastLevel", 0))
            put("dropAnim", s.getString("dropAnim", "") ?: "")
            put("badges", JSONArray((s.getStringSet("badges", emptySet()) ?: emptySet<String>()).toList()))
        }
    }

    /** Reads the database in one consistent snapshot and returns the backup as JSON text. */
    suspend fun create(c: Context): String = withContext(Dispatchers.IO) {
        val db = LifeDb.get(c)
        val dao = db.dao()
        val o = JSONObject()
        o.put("app", APP)
        o.put("format", FORMAT)
        o.put("createdAt", System.currentTimeMillis())
        o.put("prefs", prefsJson(c))
        db.withTransaction {
            o.put("reflections", JSONArray().also { a -> dao.reflectionList().forEach { a.put(JSONObject().put("id", it.id).put("date", it.date).put("text", it.text).put("mood", it.mood).put("tags", it.tags)) } })
            o.put("xp", JSONArray().also { a -> dao.xpList().forEach { a.put(JSONObject().put("key", it.key).put("date", it.date).put("type", it.type).put("xp", it.xp)) } })
            o.put("events", JSONArray().also { a -> dao.eventList().forEach { a.put(JSONObject().put("id", it.id).put("title", it.title).put("date", it.date)) } })
            o.put("favorites", JSONArray().also { a -> dao.favList().forEach { a.put(it.id) } })
            o.put("answers", JSONArray().also { a -> dao.answerList().forEach { a.put(JSONObject().put("qid", it.qid).put("text", it.text).put("date", it.date)) } })
        }
        o.toString()
    }

    /** Writes a backup file to a location the user picked in the system file picker. */
    suspend fun writeTo(c: Context, uri: Uri) {
        val json = create(c)
        withContext(Dispatchers.IO) {
            val out = c.contentResolver.openOutputStream(uri, "wt") ?: throw BackupException("Could not open the file.")
            out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
        }
    }

    // ---------------------------------------------------------------- read

    suspend fun read(c: Context, uri: Uri): BackupData = withContext(Dispatchers.IO) {
        val input = c.contentResolver.openInputStream(uri) ?: throw BackupException("Could not open that file.")
        parse(input.use { readLimited(it) })
    }

    private fun readLimited(s: java.io.InputStream): String {
        val buf = ByteArrayOutputStream()
        val tmp = ByteArray(8192)
        var total = 0
        while (true) {
            val n = s.read(tmp)
            if (n < 0) break
            total += n
            if (total > MAX_BYTES) throw BackupException("This file is too large to be a LifeDrop backup.")
            buf.write(tmp, 0, n)
        }
        return buf.toString("UTF-8")
    }

    /** Checks the whole file before anything is changed, so a bad file can never damage the current data. */
    fun parse(text: String): BackupData {
        val o = try { JSONObject(text) } catch (e: Exception) { throw BackupException("This is not a LifeDrop backup file.") }
        if (o.optString("app") != APP) throw BackupException("This is not a LifeDrop backup file.")
        val fmt = o.optInt("format", 0)
        if (fmt < 1) throw BackupException("This backup file is damaged and cannot be restored.")
        if (fmt > FORMAT) throw BackupException("This backup was made by a newer version of LifeDrop Daily. Please update the app first.")
        try {
            fun day(s: String): String { LocalDate.parse(s); return s }
            fun JSONArray.objs(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
            val reflections = o.getJSONArray("reflections").objs().map {
                Reflection(it.getLong("id"), day(it.getString("date")), it.getString("text"), it.getString("mood"), it.optString("tags", ""))
            }
            val xp = o.getJSONArray("xp").objs().map { XpEvent(it.getString("key"), day(it.getString("date")), it.getString("type"), it.getInt("xp")) }
            val events = o.getJSONArray("events").objs().map { CustomEvent(it.getLong("id"), it.getString("title"), day(it.getString("date"))) }
            val favs = o.getJSONArray("favorites").let { a -> (0 until a.length()).map { Favorite(a.getString(it)) } }
            val answers = o.getJSONArray("answers").objs().map { Answer(it.getString("qid"), it.getString("text"), day(it.getString("date"))) }
            return BackupData(o.optLong("createdAt", 0L), o.optJSONObject("prefs") ?: JSONObject(), reflections, xp, events, favs, answers)
        } catch (e: BackupException) {
            throw e
        } catch (e: Exception) {
            throw BackupException("This backup file is damaged and cannot be restored.")
        }
    }

    // ---------------------------------------------------------------- restore

    /** Replaces the data on this phone with the backup. All or nothing: it runs in one database transaction. */
    suspend fun restore(c: Context, prefs: AppPrefs, d: BackupData) {
        val app = c.applicationContext
        withContext(Dispatchers.IO) {
            val db = LifeDb.get(app)
            val dao = db.dao()
            db.withTransaction {
                dao.clearXp(); dao.clearReflections(); dao.clearEvents(); dao.clearFavs(); dao.clearAnswers()
                dao.putReflections(d.reflections)
                dao.putXp(d.xp)
                dao.putEvents(d.events)
                dao.putFavs(d.favorites)
                dao.putAnswers(d.answers)
            }
        }
        withContext(Dispatchers.Main) { applyPrefs(prefs, d.prefs) }
        withContext(Dispatchers.IO) {
            runCatching { Reminders.schedule(app); NotifyExtras.updateOngoing(app) }
            runCatching { LifeWidget().updateAll(app); StreakWidget().updateAll(app) }
        }
        markHandled(app)
        scheduleAuto(app, 500L)
    }

    private fun applyPrefs(prefs: AppPrefs, o: JSONObject) {
        o.optString("theme", "").let { if (it in Themes.names) prefs.setTheme(it) }
        o.optString("font", "").let { f -> if (Themes.fonts.any { it.first == f }) prefs.setFont(f) }
        if (o.has("animMode")) prefs.setAnimMode(o.optInt("animMode", 0).coerceIn(0, 2))
        if (o.has("morningMin")) prefs.setReminder("morning", o.optInt("morningMin", 480).coerceIn(0, 1439))
        if (o.has("eveningMin")) prefs.setReminder("evening", o.optInt("eveningMin", 1200).coerceIn(0, 1439))
        if (o.has("ongoing")) prefs.setOngoing(o.optBoolean("ongoing", false))
        if (o.optBoolean("onboarded", false)) {
            val interests = o.optJSONArray("interests")?.let { a -> (0 until a.length()).map { a.optString(it) }.toSet() } ?: emptySet()
            prefs.finishOnboarding(interests)
        }
        prefs.setLastYear(o.optInt("lastYear", 0))
        prefs.setLastLevel(o.optInt("lastLevel", 0))
        prefs.setLastDropAnim(o.optString("dropAnim", ""))
        o.optJSONArray("badges")?.let { a -> prefs.markSeen((0 until a.length()).map { a.optString(it) }.toSet()) }
    }

    // ---------------------------------------------------------------- automatic backup

    /** Saves the automatic copy a moment after the last change (changes in quick succession are combined). */
    fun scheduleAuto(c: Context, delayMs: Long = 2000L) {
        val app = c.applicationContext
        pending?.cancel()
        pending = scope.launch { delay(delayMs); runCatching { autoNow(app) } }
    }

    fun deleteAuto(c: Context) {
        pending?.cancel()
        autoFile(c).delete()
        File(c.filesDir, "$AUTO_FILE.tmp").delete()
    }

    private suspend fun dbEmpty(c: Context): Boolean {
        val dao = LifeDb.get(c).dao()
        return dao.xpList().isEmpty() && dao.reflectionList().isEmpty() && dao.eventList().isEmpty() && dao.favList().isEmpty() && dao.answerList().isEmpty()
    }

    suspend fun autoNow(c: Context) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val f = autoFile(c)
            if (!autoEnabled(c)) { f.delete(); return@withLock }
            val empty = dbEmpty(c)
            // After a reinstall the saved copy is waiting for the user's choice: never overwrite it with an empty one.
            if (f.exists() && !handled(c) && empty) return@withLock
            if (empty) { f.delete(); return@withLock }
            val tmp = File(c.filesDir, "$AUTO_FILE.tmp")
            tmp.writeText(create(c), Charsets.UTF_8)
            if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
            markHandled(c)
        }
    }

    /** A saved copy that Android put back after a reinstall, if this phone has no data yet. */
    suspend fun pendingAuto(c: Context): BackupData? = withContext(Dispatchers.IO) {
        val f = autoFile(c)
        if (!f.exists() || handled(c)) return@withContext null
        if (!dbEmpty(c)) { markHandled(c); return@withContext null }
        val data = runCatching { parse(f.readText(Charsets.UTF_8)) }.getOrNull()
        if (data == null || data.isEmpty) null else data
    }
}
