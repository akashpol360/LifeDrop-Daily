package com.lifedrop.app.ui
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.lifedrop.app.ads.rememberAdGate
import com.lifedrop.app.data.AppPrefs
import com.lifedrop.app.data.Reflection
import kotlinx.coroutines.launch
import com.lifedrop.app.content.FeedItem
import com.lifedrop.app.core.Stats
import com.lifedrop.app.data.AppVm
import java.time.LocalDate

@Composable
fun FeedScreen(feed: List<FeedItem>, vm: AppVm) {
    val favs by vm.favs.collectAsState()
    val ctx = LocalContext.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Daily Drops", fontSize = 26.sp, fontWeight = FontWeight.Bold) }
        items(feed, key = { it.id }) { f ->
            val fav = f.id in favs
            LifeCard(f.type) {
                Text(f.text, fontSize = 17.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton({ vm.toggleFav(f.id, fav) }, Modifier.heightIn(min = 48.dp)
                        .semantics { contentDescription = if (fav) "Remove favorite" else "Add favorite" }) { Text(if (fav) "♥ Saved" else "♡ Favorite", modifier = Modifier.popOnChange(fav)) }
                    TextButton({ ShareCard.share(ctx, f.type, f.text) }, Modifier.heightIn(min = 48.dp)) { Text("Share") }
                }
            }
        }
    }
}

private val moods = listOf("😄" to "Great", "😊" to "Good", "😐" to "Okay", "😔" to "Low", "😴" to "Tired")

/** Five equal-width mood buttons: emoji on top, label below, so no label is ever clipped. */
@Composable
private fun MoodPicker(selected: String, onSelect: (String) -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val shape = RoundedCornerShape(16.dp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        moods.forEach { (e, label) ->
            val sel = selected == e
            Column(Modifier.weight(1f).heightIn(min = 56.dp).clip(shape)
                .background(if (sel) primary.copy(alpha = 0.22f) else Color.Transparent)
                .border(if (sel) 2.dp else 1.dp, if (sel) primary else outline, shape)
                .clickable { onSelect(e) }.padding(vertical = 6.dp)
                .semantics { contentDescription = "$label mood" + if (sel) ", selected" else "" },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(e, fontSize = 22.sp)
                Text(label, fontSize = 11.sp, maxLines = 1, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun JournalScreen(today: LocalDate, vm: AppVm, prefs: AppPrefs) {
    val entries by vm.reflections.collectAsState()
    val ads = rememberAdGate(prefs)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Reflection?>(null) }
    var version by remember { mutableIntStateOf(0) }
    var text by remember(editing, version) { mutableStateOf(editing?.text ?: "") }
    var mood by remember(editing, version) { mutableStateOf(editing?.mood ?: "😊") }
    var tags by remember(editing, version) { mutableStateOf(editing?.tags ?: "") }
    var message by remember { mutableStateOf("") }
    var saveCount by remember { mutableIntStateOf(0) }
    var deleting by remember { mutableStateOf<Reflection?>(null) }
    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            LifeCard(if (editing == null) "📖 REFLECT" else "✏️ EDIT ENTRY") {
                MascotView(56.dp, mood = Mood.WRITING)
                Text("What was one good thing that happened today?", fontSize = 18.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                MoodPicker(mood) { mood = it }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(text, { text = it; message = "" }, Modifier.fillMaxWidth(), placeholder = { Text("Write something...") }, minLines = 3)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(tags, { tags = it }, Modifier.fillMaxWidth(), label = { Text("Tags (optional, comma separated)") }, singleLine = true)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button({
                        ads {
                            vm.saveReflection(editing?.id ?: 0L, editing?.date ?: today.toString(), text, mood, tags)
                            message = if (editing == null) "💧 Saved to your Life Journal" else "✓ Entry updated"
                            editing = null; version++; saveCount++
                        }
                    }, enabled = text.isNotBlank(), shape = RoundedCornerShape(50), modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(if (editing == null) "SAVE (+15 XP)" else "UPDATE")
                    }
                    if (editing != null) TextButton({ editing = null; version++; message = "" }, Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
                }
                if (message.isNotEmpty()) Text(message)
                WritingAnim(saveCount)
            }
        }
        if (entries.isEmpty()) item { Text("Your journal is empty. Your first entry starts the story.") }
        items(entries, key = { it.id }) { e ->
            LifeCard("${e.mood}  ${e.date}") {
                Text(e.text)
                if (e.tags.isNotBlank()) Text("🏷 ${e.tags}", fontSize = 12.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton({ editing = e; message = ""; scope.launch { listState.animateScrollToItem(0) } }, Modifier.heightIn(min = 48.dp)) { IconLabel("✏️", "Edit") }
                    TextButton({ deleting = e }, Modifier.heightIn(min = 48.dp)) { IconLabel("🗑", "Delete") }
                }
            }
        }
    }
    deleting?.let { d ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete this entry?") }, text = { Text("This cannot be undone.") },
            confirmButton = { TextButton({ if (editing?.id == d.id) { editing = null; version++ }; vm.deleteReflection(d); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton({ deleting = null }) { Text("Cancel") } })
    }
}
