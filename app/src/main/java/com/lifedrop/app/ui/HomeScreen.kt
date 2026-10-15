package com.lifedrop.app.ui
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.content.Intent
import androidx.compose.ui.unit.sp
import com.lifedrop.app.content.Drop
import com.lifedrop.app.content.Festival
import com.lifedrop.app.core.Stats
import com.lifedrop.app.core.YearMath
import com.lifedrop.app.data.AppVm
import com.lifedrop.app.data.CustomEvent
import com.lifedrop.app.data.AppPrefs
import com.lifedrop.app.ads.rememberAdGate
import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun daysLabel(n: Long) = when { n == 0L -> "TODAY!"; n == 1L -> "1 DAY LEFT"; n > 0 -> "$n DAYS LEFT"; else -> "DONE" }

@Composable
fun HomeScreen(today: LocalDate, drop: Drop, festival: Festival?, sky: Festival?, animationsOn: Boolean, vm: AppVm, prefs: AppPrefs, onOpen: (String) -> Unit) {
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val ads = rememberAdGate(prefs)
    val progress = YearMath.of(today)
    val xp by vm.xp.collectAsState()
    val events by vm.events.collectAsState()
    val sum = Stats.summary(xp, today, prefs.premium)
    val dropDone = xp.any { it.key == "$today:drop" }
    val challengeDone = xp.any { it.key == "$today:challenge" }
    val mood = when { dropDone && challengeDone -> Mood.CELEBRATING; !dropDone -> Mood.THINKING; else -> Mood.MOTIVATING }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CustomEvent?>(null) }
    var deletingEvent by remember { mutableStateOf<CustomEvent?>(null) }
    var limitMsg by remember { mutableStateOf(false) }
    var xpTarget by remember { mutableStateOf<Offset?>(null) }
    var xpSource by remember { mutableStateOf<Offset?>(null) }
    val shown by animateIntAsState(progress.dayOfYear, if (animationsOn) tween(900) else snap(), label = "day")

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (!prefs.premium) item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton({ onOpen("pro") }, Modifier.heightIn(min = 48.dp)) { IconLabel("⭐", "Go Pro") }
            }
        }
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics {
                contentDescription = "Day ${progress.dayOfYear} of ${progress.totalDays}, ${progress.daysLeft} days left" }) {
                MascotView(96.dp, mood = mood)
                Text("$shown", fontSize = 72.sp, fontWeight = FontWeight.ExtraBold)
                Text("DAY OF ${progress.totalDays}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("${progress.daysLeft} DAYS LEFT  •  ${progress.percent}%", fontSize = 14.sp)
            }
        }
        item { YearGrid(progress, animationsOn, Modifier.clickable(onClickLabel = "Open year progress") { onOpen("year") }) }
        if (progress.dayOfYear == 1) item { Text("💧 Your new year starts today.", fontWeight = FontWeight.Bold) }
        if (progress.dayOfYear == progress.totalDays) item { Text("🎉 FULL YEAR COMPLETE", fontWeight = FontWeight.Bold) }
        if (festival != null) item {
            LifeCard("🎉 COMING UP") {
                val flick = if (animationsOn && prefs.animMode == 0) rememberInfiniteTransition(label = "flick").animateFloat(0.92f, 1.1f,
                    infiniteRepeatable(tween(700, easing = EaseInOut), RepeatMode.Reverse), label = "f") else remember { mutableStateOf(1f) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(festival.emoji, fontSize = 24.sp, modifier = Modifier.graphicsLayer { scaleX = flick.value; scaleY = flick.value })
                    Text(" ${festival.name}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Text(daysLabel(ChronoUnit.DAYS.between(today, festival.date)))
                TextButton({ onOpen("festivals") }, Modifier.heightIn(min = 48.dp)) { Text("Open calendar") }
            }
        }
        if (sky != null) item {
            LifeCard("🌌 SKY WATCH") {
                Text("${sky.emoji} ${sky.name}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(daysLabel(ChronoUnit.DAYS.between(today, sky.date)))
                if (sky.note.isNotBlank()) Text(sky.note, fontSize = 12.sp)
            }
        }
        item {
            LifeCard("💧 TODAY'S DROP") {
                Text("\"${drop.question}\"", fontSize = 20.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                BounceButton(if (dropDone) "🤔 THOUGHT ABOUT IT +5 XP" else "THINK ABOUT IT", dropDone) { ads { vm.award(today, "drop", 5) } }
                TextButton(onClick = { ShareCard.share(ctx, "Today I asked myself...", drop.question) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Share this drop") }
            }
        }
        item {
            LifeCard("💪 TODAY'S CHALLENGE") {
                Text(drop.challenge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                var fire by remember { mutableStateOf(false) }
                Box(Modifier.onGloballyPositioned { xpSource = it.positionInRoot() }, contentAlignment = Alignment.Center) {
                    BounceButton(if (challengeDone) "🎉 NICE! +10 XP" else "✓ I'M DONE", challengeDone) { ads { fire = true; haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.award(today, "challenge", 10) } }
                    Confetti(fire, animationsOn, Modifier.matchParentSize())
                    val tx = xpTarget; val sx = xpSource
                    if (tx != null && sx != null) XpFloat(fire, animationsOn, "+10 XP", tx.x - sx.x, tx.y - sx.y) else XpFloat(fire, animationsOn, "+10 XP")
                }
            }
        }
        item {
            val flame = remember { Animatable(1f) }
            DisposableEffect(Unit) { onDispose { xpTarget = null } }
            LaunchedEffect(sum.streak) { if (animationsOn && sum.streak > 0) { flame.snapTo(1.5f); flame.animateTo(1f, spring(Spring.DampingRatioMediumBouncy)) } }
            Row(Modifier.onGloballyPositioned { xpTarget = it.positionInRoot() }, verticalAlignment = Alignment.CenterVertically) {
                Text("🔥", fontSize = 22.sp, modifier = Modifier.graphicsLayer { scaleX = flame.value; scaleY = flame.value })
                Text(" ${sum.streak} DAY STREAK     ⭐ ${sum.xp} XP", fontWeight = FontWeight.Bold)
            }
        }
        item { Text("🎯 MY EVENTS", fontWeight = FontWeight.Bold) }
        if (events.isEmpty()) item { Text("No events yet. Add a birthday, exam or goal.", textAlign = TextAlign.Center) }
        items(events, key = { it.id }) { e ->
            val left = ChronoUnit.DAYS.between(today, LocalDate.parse(e.date))
            LifeCard(e.title) {
                Text(daysLabel(left), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(e.date)
                if (left <= 0) {
                    val done = xp.any { it.type == "event${e.id}" }
                    BounceButton(if (done) "✓ Completed +20 XP" else "Mark as done (+20 XP)", done) { vm.award(today, "event${e.id}", 20) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { editing = e }, modifier = Modifier.heightIn(min = 48.dp)) { IconLabel("✏️", "Edit") }
                    TextButton(onClick = { deletingEvent = e }, modifier = Modifier.heightIn(min = 48.dp)) { IconLabel("🗑", "Delete") }
                }
            }
        }
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedButton(onClick = { ads { if (!prefs.premium && events.size >= 3) limitMsg = true else showAdd = true } },
                    shape = RoundedCornerShape(50), modifier = Modifier.heightIn(min = 48.dp)) { Text("+ ADD EVENT") }
                if (limitMsg) {
                    Text("🔒 The free plan allows 3 events. Unlimited events are part of Pro.", textAlign = TextAlign.Center)
                    TextButton({ onOpen("pro") }, Modifier.heightIn(min = 48.dp)) { IconLabel("⭐", "See Pro") }
                }
            }
        }
    }
    if (showAdd) AddEventDialog(today, null, { showAdd = false }) { t, d -> vm.addEvent(t, d); showAdd = false }
    editing?.let { ev -> AddEventDialog(today, ev, { editing = null }) { t, d -> vm.updateEvent(ev, t, d); editing = null } }
    deletingEvent?.let { d ->
        AlertDialog(onDismissRequest = { deletingEvent = null }, title = { Text("Delete this entry?") }, text = { Text("This cannot be undone.") },
            confirmButton = { TextButton({ vm.deleteEvent(d); deletingEvent = null }) { Text("Delete") } },
            dismissButton = { TextButton({ deletingEvent = null }) { Text("Cancel") } })
    }
}

@Composable
fun BounceButton(label: String, done: Boolean, onClick: () -> Unit) {
    val s by animateFloatAsState(if (done) 1.06f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "btn")
    Button(onClick = onClick, enabled = !done, shape = RoundedCornerShape(50),
        modifier = Modifier.scale(s).heightIn(min = 48.dp)) { Text(label) }
}

@Composable
private fun AddEventDialog(today: LocalDate, initial: CustomEvent?, onDismiss: () -> Unit, onSave: (String, LocalDate) -> Unit) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: today.plusDays(7).toString()) }
    val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initial == null) "New event" else "Edit event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Name (e.g. Interview)") }, singleLine = true)
                OutlinedTextField(date, { date = it }, label = { Text("Date (YYYY-MM-DD)") }, singleLine = true,
                    isError = parsed == null, supportingText = { if (parsed == null) Text("Use format 2026-12-31") })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(title, parsed!!) }, enabled = title.isNotBlank() && parsed != null) { Text("SAVE") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
