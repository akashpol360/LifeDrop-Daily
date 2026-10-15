package com.lifedrop.app.ui
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifedrop.app.core.Stats
import com.lifedrop.app.data.AppPrefs
import com.lifedrop.app.data.AppVm
import com.lifedrop.app.ads.rememberAdGate
import java.time.LocalDate

@Composable
fun ProfileScreen(today: LocalDate, vm: AppVm, prefs: AppPrefs, onReview: () -> Unit, onSettings: () -> Unit, onFavs: () -> Unit, onPro: () -> Unit) {
    val xp by vm.xp.collectAsState()
    val journal by vm.reflections.collectAsState()
    val events by vm.events.collectAsState()
    val answers by vm.answers.collectAsState()
    val s = Stats.summary(xp, today, prefs.premium)
    val ads = rememberAdGate(prefs)
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var detail by remember { mutableStateOf<com.lifedrop.app.core.Badge?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MascotView(96.dp, mood = when { s.streak >= 7 -> Mood.CELEBRATING; s.streak >= 3 -> Mood.RUNNING; else -> Mood.HAPPY })
                Text("Level ${s.level}", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(Stats.levelTitle(s.level), fontSize = 14.sp)
                LinearProgressIndicator(progress = { Stats.xpIntoLevel(s.xp) / Stats.XP_PER_LEVEL.toFloat() }, modifier = Modifier.fillMaxWidth(0.7f).padding(vertical = 4.dp))
                Text("${Stats.xpIntoLevel(s.xp)} / ${Stats.XP_PER_LEVEL} XP to Level ${s.level + 1}", fontSize = 12.sp)
                Text("XP: Drop 5 · Think 5 · Challenge 10 · Journal 15 · all four +10 · event done +20 · streak bonus at 3, 7, 14, 30, 60, 100 days", fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                if (prefs.premium) ProBadge()
                else Button(onPro, shape = RoundedCornerShape(50), modifier = Modifier.heightIn(min = 48.dp)) { IconLabel("⭐", "Go Pro") }
                Text("🔥 ${s.streak} day streak   ⭐ ${s.xp} XP   💧 ${s.drops} drops")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button({ ads { onReview() } }, shape = RoundedCornerShape(50), modifier = Modifier.heightIn(min = 48.dp)) { IconLabel("🎉", "Year Review") }
                    OutlinedButton(onSettings, shape = RoundedCornerShape(50), modifier = Modifier.heightIn(min = 48.dp)) { IconLabel("⚙️", "Settings") }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onFavs, shape = RoundedCornerShape(50), modifier = Modifier.heightIn(min = 48.dp)) { IconLabel("♥", "Favorites") }
                Spacer(Modifier.height(8.dp))
                Text("🏆 Achievements", fontWeight = FontWeight.Bold)
            }
        }
        items(Stats.badges(s, journal.size, events.size, answers.size), key = { it.name }) { b ->
            Card(Modifier.fillMaxWidth().clickable { detail = b }.semantics { contentDescription = "${b.name}, ${if (b.unlocked) "unlocked" else "locked"}" }) {
                Row(Modifier.padding(16.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (b.unlocked) b.emoji else "🔒", fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(b.name, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text(if (b.unlocked) "Unlocked" else "Locked")
                }
            }
        }
    }
    detail?.let { b ->
        AlertDialog(onDismissRequest = { detail = null }, confirmButton = { TextButton({ detail = null }) { Text("OK") } },
            dismissButton = { if (b.unlocked) TextButton({ ShareCard.share(ctx, "I unlocked a badge on LifeDrop Daily", "${b.emoji} ${b.name}") }) { Text("Share") } },
            title = { Text("${if (b.unlocked) b.emoji else "🔒"} ${b.name}") },
            text = { Text(b.hint + "\n" + if (b.unlocked) "Unlocked!" else "Not unlocked yet.") })
    }
}
