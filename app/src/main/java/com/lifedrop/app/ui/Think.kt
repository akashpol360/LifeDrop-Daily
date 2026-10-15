package com.lifedrop.app.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifedrop.app.ads.rememberAdGate
import com.lifedrop.app.content.Question
import com.lifedrop.app.data.AppPrefs
import com.lifedrop.app.data.AppVm
import java.time.LocalDate

@Composable
fun ThinkScreen(think: List<Question>, whatIf: List<Question>, today: LocalDate, vm: AppVm, prefs: AppPrefs) {
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var open by rememberSaveable { mutableStateOf("") }
    val answers by vm.answers.collectAsState()
    val favs by vm.favs.collectAsState()
    val ctx = LocalContext.current
    val ads = rememberAdGate(prefs)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { MascotView(72.dp, mood = if (mode == 0) Mood.CURIOUS else Mood.THINKING) } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(mode == 0, { mode = 0 }, { Text("🧠 Think") }, Modifier.heightIn(min = 48.dp))
                FilterChip(mode == 1, { mode = 1 }, { Text("🤔 What if?") }, Modifier.heightIn(min = 48.dp))
            }
        }
        items(if (mode == 0) think else whatIf, key = { it.id }) { q ->
            val saved = answers.firstOrNull { it.qid == q.id }
            val fav = q.id in favs
            LifeCard(q.cat + if (saved != null) "  •  ✓ answered" else "") {
                Text(q.text, fontSize = 18.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                if (open == q.id) {
                    var t by remember(q.id, saved?.text) { mutableStateOf(saved?.text ?: "") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(t, { t = it }, Modifier.fillMaxWidth(), placeholder = { Text("Write your answer...") }, minLines = 2)
                    Spacer(Modifier.height(16.dp))
                    Button({ ads { vm.saveAnswer(q.id, t, today) } }, enabled = t.isNotBlank(), shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("SAVE +5 XP") }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        TextButton({ vm.toggleFav(q.id, fav) }, Modifier.heightIn(min = 48.dp)) { Text(if (fav) "♥ Saved" else "♡ Favorite", modifier = Modifier.popOnChange(fav)) }
                        TextButton({ ShareCard.share(ctx, "Today I asked myself...", q.text) }, Modifier.heightIn(min = 48.dp)) { Text("Share") }
                        TextButton({ open = "" }, Modifier.heightIn(min = 48.dp)) { Text("Close") }
                    }
                } else {
                    TextButton({ ads { open = q.id } }, Modifier.heightIn(min = 48.dp)) {
                        Text(if (saved != null) "View / edit answer" else "Think about it")
                    }
                }
            }
        }
    }
}
