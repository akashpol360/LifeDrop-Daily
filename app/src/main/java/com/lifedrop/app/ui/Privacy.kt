package com.lifedrop.app.ui
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifedrop.app.data.AppVm
import com.lifedrop.app.data.AppPrefs
import com.lifedrop.app.ads.rememberAdGate
import com.lifedrop.app.data.Reflection

@Composable
fun PrivacyTools(journal: List<Reflection>, vm: AppVm, prefs: AppPrefs) {
    val ads = rememberAdGate(prefs)
    val ctx = LocalContext.current
    var ask by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth()) {
        Text("🔒 Privacy", fontWeight = FontWeight.Bold)
        Text("Everything stays on this phone. No account is needed.", fontSize = 12.sp)
        TextButton({ ads {
            val entries = vm.reflections.value
            if (entries.isEmpty()) {
                Toast.makeText(ctx, "Your journal is empty. Write a reflection first.", Toast.LENGTH_SHORT).show()
            } else {
                val body = entries.sortedBy { it.date }.joinToString("\n\n") { "${it.date} ${it.mood}\n${it.text}" + if (it.tags.isNotBlank()) "\nTags: ${it.tags}" else "" }
                val i = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, "My LifeDrop Daily journal").putExtra(Intent.EXTRA_TEXT, body)
                runCatching { ctx.startActivity(Intent.createChooser(i, "Export journal")) }
                    .onFailure { Toast.makeText(ctx, "No app available to share the journal.", Toast.LENGTH_SHORT).show() }
            }
        } }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Export journal (${journal.size})") }
        TextButton({ ads { ask = "journal" } }, Modifier.heightIn(min = 48.dp)) { Text("Delete journal") }
        TextButton({ ads { ask = "all" } }, Modifier.heightIn(min = 48.dp)) { Text("Reset all progress and data") }
    }
    if (ask.isNotEmpty()) AlertDialog(onDismissRequest = { ask = "" },
        title = { Text(if (ask == "journal") "Delete your journal?" else "Reset everything?") },
        text = { Text("This cannot be undone.") },
        confirmButton = { TextButton({ if (ask == "journal") vm.deleteJournal() else vm.resetAll(); ask = "" }) { Text("Delete") } },
        dismissButton = { TextButton({ ask = "" }) { Text("Cancel") } })
}
