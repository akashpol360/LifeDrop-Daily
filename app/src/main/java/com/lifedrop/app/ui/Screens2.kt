package com.lifedrop.app.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifedrop.app.content.Festival
import com.lifedrop.app.core.YearMath
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun YearScreen(today: LocalDate, anim: Boolean) {
    val p = YearMath.of(today)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MascotView(80.dp)
                Text("${p.dayOfYear} / ${p.totalDays}", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold)
                Text("${p.percent}% of ${today.year} done • ${p.daysLeft} days, about ${p.daysLeft / 7} weeks and ${12 - today.monthValue} months left",
                    textAlign = TextAlign.Center)
            }
        }
        item { YearGrid(p, anim) }
        items((1..12).toList(), key = { it }) { m ->
            val ym = YearMonth.of(today.year, m)
            val len = ym.lengthOfMonth()
            val passed = when { m < today.monthValue -> len; m == today.monthValue -> today.dayOfMonth; else -> 0 }
            Column(Modifier.fillMaxWidth()) {
                Row { Text(ym.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()), Modifier.weight(1f)); Text("$passed / $len") }
                LinearProgressIndicator(progress = { passed / len.toFloat() }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun FestivalsScreen(fests: List<Festival>, today: LocalDate) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("🎉 Upcoming days", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        if (fests.isEmpty()) item { Text("No upcoming festivals in the list.") }
        items(fests, key = { it.name + it.date }) { f ->
            LifeCard("${f.emoji} ${f.name}") {
                Text(f.date.toString()); Text(daysLabel(ChronoUnit.DAYS.between(today, f.date)), fontWeight = FontWeight.Bold)
            }
        }
    }
}
