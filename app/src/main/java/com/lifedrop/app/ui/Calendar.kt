package com.lifedrop.app.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifedrop.app.content.Festival
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val typeLabel = mapOf("fest" to "Festival", "hol" to "Holiday", "obs" to "Special day", "sky" to "Sky", "moon" to "Moon phase")
private fun rank(t: String) = when (t) { "fest", "hol" -> 0; "obs" -> 1; "sky" -> 2; else -> 3 }
private val filters = listOf("all" to "All", "fest" to "Festivals", "hol" to "Holidays", "obs" to "Special days", "sky" to "Sky", "moon" to "Moon")

/** Kalnirnay-style month calendar: festivals, holidays, special days and sky events (eclipses, meteor showers, planets). */
@Composable
fun CalendarScreen(today: LocalDate, load: (Int) -> List<Festival>) {
    val minM = YearMonth.of(today.year - 2, 1)
    val maxM = YearMonth.of(today.year + 30, 12)
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var filter by remember { mutableStateOf("all") }
    var selected by remember { mutableStateOf<LocalDate?>(today) }
    val loaded = remember { mutableStateMapOf<Int, List<Festival>>() }
    LaunchedEffect(month.year) {
        if (!loaded.containsKey(month.year)) loaded[month.year] = withContext(Dispatchers.Default) { load(month.year) }
    }
    val loading = !loaded.containsKey(month.year)
    val events = loaded[month.year].orEmpty()
    val shown = remember(events, filter) { events.filter { filter == "all" || it.type == filter }.sortedWith(compareBy({ it.date }, { rank(it.type) })) }
    val byDate = remember(shown) { shown.groupBy { it.date } }
    val monthEvents = remember(shown, month) { shown.filter { YearMonth.from(it.date) == month } }
    val tertiary = MaterialTheme.colorScheme.tertiary
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val offset = month.atDay(1).dayOfWeek.value % 7 // Sunday-first
    val len = month.lengthOfMonth()
    val rows = (offset + len + 6) / 7
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton({ month = month.minusMonths(1) }, Modifier.heightIn(min = 48.dp), enabled = month > minM) { Text("◀") }
                Text("${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                TextButton({ month = month.plusMonths(1) }, Modifier.heightIn(min = 48.dp), enabled = month < maxM) { Text("▶") }
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                filters.forEach { (k, l) -> FilterChip(filter == k, { filter = k }, { Text(l) }, Modifier.heightIn(min = 48.dp)) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth()) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach {
                    Text(it, Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (w in 0 until rows) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (d in 0 until 7) {
                            val day = w * 7 + d - offset + 1
                            if (day < 1 || day > len) Spacer(Modifier.weight(1f).aspectRatio(0.8f))
                            else {
                                val date = month.atDay(day)
                                val ev = byDate[date].orEmpty()
                                val isToday = date == today
                                val isSel = date == selected
                                val shape = RoundedCornerShape(10.dp)
                                Column(Modifier.weight(1f).aspectRatio(0.8f).clip(shape)
                                    .background(when { isSel -> primary.copy(alpha = 0.25f); isToday -> tertiary.copy(alpha = 0.2f); else -> Color.Transparent })
                                    .border(if (isToday) 2.dp else 1.dp, if (isToday) tertiary else outline, shape)
                                    .clickable { selected = date }
                                    .semantics { contentDescription = "$day ${month.month}, " + if (ev.isEmpty()) "no events" else ev.joinToString { it.name } },
                                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Text("$day", fontSize = 13.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                                    if (ev.isNotEmpty()) {
                                        Text(ev.first().emoji, fontSize = 11.sp)
                                        if (ev.size > 1) Text("+${ev.size - 1}", fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        selected?.let { sel ->
            item {
                val evs = byDate[sel].orEmpty()
                LifeCard("${sel.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())}, ${sel.dayOfMonth} ${sel.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${sel.year}") {
                    if (evs.isEmpty()) Text("No events on this day.")
                    evs.forEach { e ->
                        Text("${e.emoji} ${e.name}", fontWeight = FontWeight.Bold)
                        Text(typeLabel[e.type].orEmpty() + if (e.note.isNotBlank()) " • ${e.note}" else "", fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
        item { Text("This month", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        if (loading) item { Text("Calculating dates...") }
        if (monthEvents.isEmpty()) item { Text("Nothing listed for this month with the current filter.") }
        items(monthEvents, key = { it.date.toString() + it.name }) { e ->
            Row(Modifier.fillMaxWidth().clickable { selected = e.date }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${e.date.dayOfMonth} ${e.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())}", Modifier.width(64.dp), fontWeight = FontWeight.Bold)
                Text(e.emoji, fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.name)
                    Text(typeLabel[e.type].orEmpty() + if (e.note.isNotBlank()) " • ${e.note}" else "", fontSize = 11.sp)
                }
            }
        }
        item {
            Text("2026-2027 dates follow the Government of India (DoPT) holiday lists. Every other year is calculated on your phone from sun and moon positions, so it works for any year but can differ by a day for some festivals (regional customs, Bhadra and similar rules). Moon-sighting dates (Eid, Muharram, Milad-un-Nabi) can also shift by a day. Eclipse visibility depends on where you are.", fontSize = 11.sp)
        }
    }
}
