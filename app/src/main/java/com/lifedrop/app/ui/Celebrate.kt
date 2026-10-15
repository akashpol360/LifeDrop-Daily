package com.lifedrop.app.ui
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifedrop.app.core.Badge
import com.lifedrop.app.core.Stats
import com.lifedrop.app.data.AppPrefs
import com.lifedrop.app.data.AppVm
import java.time.LocalDate

/** Shows a one-time celebration when a new achievement unlocks (badge pops in with confetti). */
@Composable
fun BadgeCelebration(today: LocalDate, vm: AppVm, prefs: AppPrefs) {
    val xp by vm.xp.collectAsState()
    val journal by vm.reflections.collectAsState()
    val events by vm.events.collectAsState()
    val answers by vm.answers.collectAsState()
    val unlocked = Stats.badges(Stats.summary(xp, today, prefs.premium), journal.size, events.size, answers.size).filter { it.unlocked }
    var pending by remember { mutableStateOf<Badge?>(null) }
    LaunchedEffect(unlocked.map { it.name }) {
        val seen = prefs.seenBadges()
        val fresh = unlocked.firstOrNull { it.name !in seen }
        if (fresh != null) { pending = fresh; prefs.markSeen(seen + unlocked.map { it.name }) }
    }
    pending?.let { b ->
        AlertDialog(onDismissRequest = { pending = null },
            confirmButton = { TextButton({ pending = null }) { Text("Nice!") } },
            title = { Text("New badge unlocked!") },
            text = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    var go by remember { mutableStateOf(!lifeAnim) }
                    LaunchedEffect(Unit) { go = true }
                    val sc by animateFloatAsState(if (go) 1f else 0f, spring(Spring.DampingRatioMediumBouncy), label = "badge")
                    Box(Modifier.size(120.dp), Alignment.Center) {
                        Text(b.emoji, fontSize = 64.sp, modifier = Modifier.graphicsLayer { scaleX = sc; scaleY = sc })
                        Confetti(true, lifeAnim, Modifier.matchParentSize())
                    }
                    Text(b.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Sticker collected 🎉")
                }
            })
    }
}


/** On the first launch of a new year: the old year moves into history and the new one starts. */
@Composable
fun YearTransition(today: LocalDate, prefs: AppPrefs, onReview: (Int) -> Unit) {
    var old by remember { mutableIntStateOf(0) }
    LaunchedEffect(today.year) {
        val seen = prefs.lastYear()
        if (seen != 0 && seen < today.year) old = seen
        prefs.setLastYear(today.year)
    }
    if (old != 0) AlertDialog(onDismissRequest = { old = 0 },
        title = { Text("🎉 $old is complete!") },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(110.dp), Alignment.Center) { MascotView(80.dp, mood = Mood.CELEBRATING); Confetti(true, lifeAnim, Modifier.matchParentSize()) }
                Text("Your new year starts today.")
                Text("💧 1 / ${today.lengthOfYear()}", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
        },
        confirmButton = { TextButton({ val y = old; old = 0; onReview(y) }) { Text("See my $old review") } },
        dismissButton = { TextButton({ old = 0 }) { Text("Start ${today.year}") } })
}

/** One-time celebration when the level goes up. */
@Composable
fun LevelUpCelebration(today: LocalDate, vm: AppVm, prefs: AppPrefs) {
    val xp by vm.xp.collectAsState()
    val level = Stats.summary(xp, today, prefs.premium).level
    var shown by remember { mutableIntStateOf(0) }
    LaunchedEffect(level, xp.isEmpty()) {
        if (xp.isEmpty()) return@LaunchedEffect
        val last = prefs.lastLevel()
        if (last == 0 || level < last) prefs.setLastLevel(level)
        else if (level > last) { shown = level; prefs.setLastLevel(level) }
    }
    if (shown > 0) AlertDialog(onDismissRequest = { shown = 0 },
        confirmButton = { TextButton({ shown = 0 }) { Text("Keep going!") } },
        title = { Text("Level $shown reached!") },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(120.dp), Alignment.Center) { MascotView(80.dp, mood = Mood.CELEBRATING); Confetti(true, lifeAnim, Modifier.matchParentSize()) }
                Text(Stats.levelTitle(shown), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Your daily drops are adding up.")
            }
        })
}
