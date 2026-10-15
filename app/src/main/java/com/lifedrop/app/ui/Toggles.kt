package com.lifedrop.app.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun ReminderToggles() {
    val p = LocalContext.current.getSharedPreferences("lifedrop", 0)
    var m by remember { mutableStateOf(p.getBoolean("morning", true)) }
    var e by remember { mutableStateOf(p.getBoolean("evening", true)) }
    var sk by remember { mutableStateOf(p.getBoolean("sky", true)) }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("☀️ Morning reminder", Modifier.weight(1f))
            Switch(m, { m = it; p.edit().putBoolean("morning", it).apply() })
        }
        Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🌙 Evening reflection", Modifier.weight(1f))
            Switch(e, { e = it; p.edit().putBoolean("evening", it).apply() })
        }
        Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🌌 Sky event alerts (eclipses, meteor showers)", Modifier.weight(1f))
            Switch(sk, { sk = it; p.edit().putBoolean("sky", it).apply() })
        }
    }
}
