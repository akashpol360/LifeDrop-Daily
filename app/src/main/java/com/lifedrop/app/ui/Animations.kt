package com.lifedrop.app.ui
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifedrop.app.data.AppPrefs
import java.time.LocalDate

/** Heart-pop micro-interaction: the element bounces when [key] changes (e.g. favorite toggled). */
@Composable
fun Modifier.popOnChange(key: Boolean): Modifier {
    val sc = remember { Animatable(1f) }
    var prev by remember { mutableStateOf(key) }
    LaunchedEffect(key) {
        if (prev != key) { prev = key; if (lifeAnim) { sc.snapTo(1.45f); sc.animateTo(1f, spring(Spring.DampingRatioMediumBouncy)) } }
    }
    return this.graphicsLayer { scaleX = sc.value; scaleY = sc.value }
}

/** A pencil writes a line when [play] increases (journal saved). */
@Composable
fun WritingAnim(play: Int) {
    val p = remember { Animatable(1f) }
    LaunchedEffect(play) { if (play > 0 && lifeAnim) { p.snapTo(0f); p.animateTo(1f, tween(900)) } }
    if (p.value < 1f) BoxWithConstraints(Modifier.fillMaxWidth().height(36.dp)) {
        val wPx = with(LocalDensity.current) { maxWidth.toPx() }
        val color = MaterialTheme.colorScheme.primary
        Canvas(Modifier.matchParentSize()) {
            drawLine(color, Offset(size.width * 0.05f, size.height * 0.8f), Offset(size.width * (0.05f + 0.8f * p.value), size.height * 0.8f), 5f, StrokeCap.Round)
        }
        Text("✏️", fontSize = 22.sp, modifier = Modifier.graphicsLayer { translationX = wPx * (0.05f + 0.8f * p.value) - 12f; translationY = -2f })
    }
}

/** First open of each day (Full animation mode): a water drop falls, bounces and ripples. */
@Composable
fun NewDayDrop(today: LocalDate, prefs: AppPrefs) {
    val fall = remember { Animatable(0f) }
    val ripple = remember { Animatable(0f) }
    var running by remember { mutableStateOf(false) }
    val color = MaterialTheme.colorScheme.secondary
    val ink = MaterialTheme.colorScheme.onSurface
    LaunchedEffect(today) {
        val key = today.toString()
        if (prefs.lastDropAnim() != key) {
            prefs.setLastDropAnim(key)
            if (lifeAmbient) {
                running = true
                fall.snapTo(0f); ripple.snapTo(0f)
                fall.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
                ripple.animateTo(1f, tween(600))
                running = false
            }
        }
    }
    if (running) Canvas(Modifier.fillMaxSize()) {
        val cx = size.width / 2; val ty = size.height * 0.3f; val y = ty * fall.value; val r = 16f
        drawPath(Path().apply { moveTo(cx, y - r * 2.2f); lineTo(cx - r * 0.9f, y - r * 0.4f); lineTo(cx + r * 0.9f, y - r * 0.4f); close() }, color)
        drawCircle(color, r, Offset(cx, y))
        if (ripple.value > 0f) drawCircle(ink.copy(alpha = 0.35f * (1f - ripple.value)), 20f + 120f * ripple.value, Offset(cx, ty + r), style = Stroke(4f))
    }
}
