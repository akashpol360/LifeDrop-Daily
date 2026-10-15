package com.lifedrop.app.ui
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifedrop.app.data.AppPrefs
import com.lifedrop.app.data.AppVm
import com.lifedrop.app.notify.NotifyExtras
import com.lifedrop.app.notify.Reminders

private fun fmt(m: Int) = "%02d:%02d".format(m / 60, m % 60)

@Composable
private fun Toggle(label: String, on: Boolean, set: (Boolean) -> Unit) =
    Row(Modifier.heightIn(min = 48.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f)); Switch(on, set) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(prefs: AppPrefs, vm: AppVm, onPro: () -> Unit) {
    val ctx = LocalContext.current
    val journal by vm.reflections.collectAsState()
    var editing by remember { mutableStateOf("") }
    val sysAnim = remember { android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column {
                Text("⚙️ Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("🎨 Appearance", fontWeight = FontWeight.Bold)
                Text("Animations")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Full", "Reduced", "Off").forEachIndexed { i, l -> FilterChip(prefs.animMode == i, { prefs.setAnimMode(i) }, { Text(l) }, Modifier.heightIn(min = 48.dp)) }
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    MascotView(64.dp, mood = when (prefs.animMode) { 0 -> Mood.EXCITED; 1 -> Mood.RELAXING; else -> Mood.SLEEPING })
                }
                Text(when (prefs.animMode) {
                    0 -> "Full: floating mascot, drifting doodles, card bounce, counting numbers and confetti."
                    1 -> "Reduced: numbers count and confetti play, but decorative motion stops."
                    else -> "Off: nothing animates."
                }, fontSize = 12.sp)
                if (!sysAnim) Text("Your phone's own animation setting is off, so nothing will animate until you turn it on.", fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Text("Theme")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Themes.names.forEach { n ->
                        val locked = n in Themes.premiumOnly && !prefs.premium
                        FilterChip(prefs.theme == n && !locked, {
                            if (!locked) prefs.setTheme(n) else onPro()
                        }, { Text((if (locked) "🔒 " else "") + n.replaceFirstChar { it.uppercase() }) }, Modifier.heightIn(min = 48.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Font")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Themes.fonts.forEach { (key, label) ->
                        val locked = key != "default" && !prefs.premium
                        val selected = (if (prefs.premium) prefs.font else "default") == key
                        FilterChip(selected, {
                            if (!locked) prefs.setFont(key) else onPro()
                        }, { Text((if (locked) "🔒 " else "") + label) }, Modifier.heightIn(min = 48.dp))
                    }
                }
            }
        }
        item {
            Column {
                Text("⭐ Premium", fontWeight = FontWeight.Bold)
                Text("No ads • All 6 themes • Custom fonts • Unlimited events • Streak protection • Advanced insights • Premium widget", fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                if (prefs.premium) { ProBadge(); Spacer(Modifier.height(4.dp)); Text("✓ Premium is active. Thank you!", fontWeight = FontWeight.Bold) }
                else Button(onPro, shape = androidx.compose.foundation.shape.RoundedCornerShape(50), modifier = Modifier.heightIn(min = 48.dp)) { IconLabel("⭐", "Go Pro") }
            }
        }
        item {
            Column {
                Text("🔔 Notifications", fontWeight = FontWeight.Bold)
                ReminderToggles()
                Row(Modifier.heightIn(min = 48.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Morning time", Modifier.weight(1f)); OutlinedButton({ editing = "morning" }) { Text(fmt(prefs.morningMin)) }
                }
                Row(Modifier.heightIn(min = 48.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Evening time", Modifier.weight(1f)); OutlinedButton({ editing = "evening" }) { Text(fmt(prefs.eveningMin)) }
                }
                Toggle("Show day count in notification bar", prefs.ongoing) { prefs.setOngoing(it); NotifyExtras.updateOngoing(ctx) }
                Text("Reminders may arrive a few minutes late to save battery.", fontSize = 12.sp)
            }
        }
        item { PrivacyTools(journal, vm, prefs) }
    }
    if (editing.isNotEmpty()) {
        val init = if (editing == "morning") prefs.morningMin else prefs.eveningMin
        val st = rememberTimePickerState(init / 60, init % 60, true)
        AlertDialog(onDismissRequest = { editing = "" }, title = { Text("Reminder time") }, text = { TimeInput(st) },
            confirmButton = { TextButton({ prefs.setReminder(editing, st.hour * 60 + st.minute); Reminders.schedule(ctx); editing = "" }) { Text("OK") } },
            dismissButton = { TextButton({ editing = "" }) { Text("Cancel") } })
    }
}
