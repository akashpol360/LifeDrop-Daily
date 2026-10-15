package com.lifedrop.app.ui
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val topics = listOf("Thinking", "Productivity", "Money", "Career", "Health", "Life", "Technology", "Fun")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Onboarding(anim: Boolean, onDone: (Set<String>) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf(setOf<String>()) }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedContent(step, Modifier.weight(1f).fillMaxWidth(), label = "onb") { s ->
            Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                when (s) {
                    0 -> {
                        MascotView(120.dp, mood = Mood.EXCITED)
                        Text("Meet LifeDrop Daily", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text("One small drop every day can change how you see your year.", textAlign = TextAlign.Center, fontSize = 18.sp)
                    }
                    1 -> {
                        var target by remember { mutableIntStateOf(0) }
                        val year = remember { com.lifedrop.app.core.YearMath.of(java.time.LocalDate.now()) }
                        LaunchedEffect(Unit) { target = year.dayOfYear }
                        val n by animateIntAsState(target, if (anim) tween(1400) else snap(), label = "n")
                        Text("$n / ${year.totalDays}", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
                        Text("TIME PASSES", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("See where you are in the year, every day.", textAlign = TextAlign.Center)
                    }
                    2 -> {
                        var shown by remember { mutableIntStateOf(if (anim) 0 else 4) }
                        LaunchedEffect(Unit) { if (anim) repeat(4) { delay(450); shown = it + 1 } }
                        listOf("🧠 THINK", "💪 DO", "📖 REFLECT", "💧 REMEMBER").forEachIndexed { i, t ->
                            AnimatedVisibility(shown > i) { Text(t, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
                            if (i < 3) Text("↓")
                        }
                    }
                    else -> {
                        Text("What do you want more of?", fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            topics.forEach { t ->
                                FilterChip(t in picked, { picked = if (t in picked) picked - t else picked + t },
                                    label = { Text(t) }, modifier = Modifier.heightIn(min = 48.dp))
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            if (step < 3) TextButton({ onDone(picked) }, Modifier.heightIn(min = 48.dp)) { Text("Skip") } else Spacer(Modifier.width(1.dp))
            Button({ if (step < 3) step++ else onDone(picked) }, Modifier.heightIn(min = 48.dp)) { Text(if (step < 3) "Next" else "Get started") }
        }
    }
}
