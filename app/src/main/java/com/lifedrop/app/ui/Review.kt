package com.lifedrop.app.ui
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifedrop.app.core.Insights
import com.lifedrop.app.data.AppPrefs
import com.lifedrop.app.data.AppVm

@Composable
private fun StatRow(emoji: String, label: String, value: Int, anim: Boolean) {
    val v by animateIntAsState(value, if (anim) tween(900) else snap(), label = label)
    LifeCard("$emoji $label") { Text("$v", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold) }
}

@Composable
fun ReviewScreen(startYear: Int, vm: AppVm, prefs: AppPrefs) {
    var year by remember { mutableIntStateOf(startYear) }
    val xp by vm.xp.collectAsState()
    val journal by vm.reflections.collectAsState()
    val ev by vm.events.collectAsState()
    val r = Insights.review(xp, journal, year, ev.size)
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val monthly = Insights.monthlyXp(xp, year)
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MascotView(88.dp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton({ year-- }, Modifier.heightIn(min = 48.dp)) { Text("◀ ${year - 1}") }
                    Text("YOUR $year", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    TextButton({ year++ }, Modifier.heightIn(min = 48.dp), enabled = year < startYear) { Text("${year + 1} ▶") }
                }
            }
        }
        item { StatRow("💧", "DAILY DROPS", r.drops, prefs.anim) }
        item { StatRow("💪", "CHALLENGES", r.challenges, prefs.anim) }
        item { StatRow("📖", "REFLECTIONS", r.reflections, prefs.anim) }
        item { StatRow("🔥", "LONGEST STREAK (DAYS)", r.longest, prefs.anim) }
        item { StatRow("⭐", "XP", r.xp, prefs.anim) }
        item { StatRow("📅", "ACTIVE DAYS", r.activeDays, prefs.anim) }
        item { StatRow("🎯", "CUSTOM EVENTS", r.events, prefs.anim) }
        item {
            LifeCard("📊 ADVANCED INSIGHTS") {
                if (!prefs.premium) Text("🔒 Monthly activity insights are a Premium feature.", textAlign = TextAlign.Center)
                else {
                    val max = monthly.max()
                    val best = if (max == 0) "none yet" else months[monthly.indexOf(max)]
                    Text("Most active month: $best", Modifier.semantics { contentDescription = "Most active month: $best" })
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.height(80.dp).fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        monthly.forEach { m ->
                            Box(Modifier.weight(1f).padding(horizontal = 2.dp)
                                .fillMaxHeight(if (max == 0) 0.03f else (m.toFloat() / max).coerceAtLeast(0.03f))
                                .background(MaterialTheme.colorScheme.primary))
                        }
                    }
                    Row(Modifier.fillMaxWidth()) { months.forEach { Text(it.take(1), Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 11.sp) } }
                }
            }
        }
        item { Text("You made it through another year.\nCarry the lessons forward.", textAlign = TextAlign.Center, fontSize = 18.sp) }
        item {
            Button({ ShareCard.share(ctx, "My $year on LifeDrop Daily", "💧 ${r.drops} drops · 💪 ${r.challenges} challenges · 📖 ${r.reflections} reflections · 🔥 ${r.longest} day streak") },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(50), modifier = Modifier.heightIn(min = 48.dp)) { Text("Share my year") }
        }
    }
}
