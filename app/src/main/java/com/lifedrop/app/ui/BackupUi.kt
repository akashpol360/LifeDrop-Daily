package com.lifedrop.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifedrop.app.core.Stats
import com.lifedrop.app.data.AppPrefs
import com.lifedrop.app.data.Backup
import com.lifedrop.app.data.BackupData
import com.lifedrop.app.data.BackupException
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date

private fun stamp(ms: Long): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ms))

/** A short, readable summary of what a backup contains. */
private fun summary(d: BackupData): String {
    val s = Stats.summary(d.xp, LocalDate.now())
    val made = if (d.createdAt > 0) "Saved ${stamp(d.createdAt)}\n" else ""
    return made + "Level ${s.level} • ${s.xp} XP • ${s.streak} day streak\n" +
        "${d.reflections.size} journal entries • ${d.answers.size} answers • ${d.events.size} events"
}

/** Settings > Backup & Restore. Sits in the same style as the other settings sections. */
@Composable
fun BackupTools(prefs: AppPrefs) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var found by remember { mutableStateOf<BackupData?>(null) }
    var busy by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    fun toast(m: String) = Toast.makeText(ctx, m, Toast.LENGTH_LONG).show()

    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val ok = runCatching { Backup.writeTo(ctx, uri) }.isSuccess
            busy = false
            if (ok) { prefs.markManualBackup(System.currentTimeMillis()); toast("Backup saved. Keep the file somewhere safe, like Google Drive.") }
            else toast("Could not save the backup. Please choose another location.")
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val r = runCatching { Backup.read(ctx, uri) }
            busy = false
            r.onSuccess { found = it }
                .onFailure { toast(if (it is BackupException) it.message ?: "Could not read that file." else "Could not read that file.") }
        }
    }
    val autoSaved = remember(refresh, prefs.autoBackup) { Backup.autoFile(ctx).let { if (it.exists()) it.lastModified() else 0L } }

    Column(Modifier.fillMaxWidth()) {
        Text("💾 Backup & Restore", fontWeight = FontWeight.Bold)
        Text("Save your streak, XP, journal, events and settings in a file. Restore it after reinstalling or on a new phone.", fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ save.launch(Backup.fileName()) }, Modifier.weight(1f).heightIn(min = 48.dp), enabled = !busy, shape = RoundedCornerShape(50)) { IconLabel("💾", "Back up") }
            OutlinedButton({ open.launch(arrayOf("*/*")) }, Modifier.weight(1f).heightIn(min = 48.dp), enabled = !busy, shape = RoundedCornerShape(50)) { IconLabel("📥", "Restore") }
        }
        if (prefs.lastManualBackup > 0L) Text("Last backup file: ${stamp(prefs.lastManualBackup)}", fontSize = 12.sp)
        Row(Modifier.heightIn(min = 48.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Automatic backup", Modifier.weight(1f))
            Switch(prefs.autoBackup, {
                prefs.setAutoBackup(it)
                if (it) Backup.scheduleAuto(ctx, 300L) else Backup.deleteAuto(ctx)
                refresh++
            })
        }
        Text(
            if (prefs.autoBackup)
                "Keeps a small copy of your progress that Android can save to your own Google backup, so it can come back after a reinstall. " +
                    "This needs Google backup to be on in your phone settings, and Android decides when it runs (about once a day)." +
                    (if (autoSaved > 0L) "\nLast copy on this phone: ${stamp(autoSaved)}" else "")
            else "Off. No backup copy is kept on this phone.",
            fontSize = 12.sp)
    }

    found?.let { d ->
        AlertDialog(onDismissRequest = { found = null },
            title = { Text("Restore this backup?") },
            text = { Text(summary(d) + "\n\nThis replaces the progress, journal and events on this phone with the backup.") },
            confirmButton = {
                TextButton({
                    found = null
                    scope.launch {
                        busy = true
                        val ok = runCatching { Backup.restore(ctx, prefs, d) }.isSuccess
                        busy = false
                        toast(if (ok) "Backup restored." else "Could not restore the backup. Your current data was not changed.")
                        refresh++
                    }
                }) { Text("Restore") }
            },
            dismissButton = { TextButton({ found = null }) { Text("Cancel") } })
    }
}

/** After a reinstall, Android may put the automatic backup copy back. This asks the user whether to use it. */
@Composable
fun AutoRestorePrompt(prefs: AppPrefs) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var found by remember { mutableStateOf<BackupData?>(null) }
    LaunchedEffect(Unit) { found = runCatching { Backup.pendingAuto(ctx) }.getOrNull() }
    found?.let { d ->
        AlertDialog(onDismissRequest = {},
            title = { Text("Welcome back! 💧") },
            text = { Text("We found a saved copy of your progress:\n\n" + summary(d) + "\n\nRestore it?") },
            confirmButton = {
                TextButton({
                    found = null
                    scope.launch {
                        val ok = runCatching { Backup.restore(ctx, prefs, d) }.isSuccess
                        Toast.makeText(ctx, if (ok) "Progress restored." else "Could not restore the saved copy.", Toast.LENGTH_LONG).show()
                    }
                }) { Text("Restore my progress") }
            },
            dismissButton = { TextButton({ Backup.markHandled(ctx); found = null }) { Text("Start fresh") } })
    }
}
