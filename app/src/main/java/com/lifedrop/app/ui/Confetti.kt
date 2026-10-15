package com.lifedrop.app.ui
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val colors = listOf(Color(0xFFFF8A65), Color(0xFF4FC3F7), Color(0xFFFFCA28), Color(0xFF81C784), Color(0xFFBA68C8))

/** One-shot burst (about 1s) when [fire] turns true. Skipped when animations are off. Decorative only. */
@Composable
fun Confetti(fire: Boolean, enabled: Boolean, modifier: Modifier = Modifier.fillMaxSize()) {
    val p = remember { Animatable(1f) }
    LaunchedEffect(fire) {
        if (fire && enabled) { p.snapTo(0f); p.animateTo(1f, tween(1000)) }
    }
    if (p.value < 1f) Canvas(modifier) {
        val t = p.value
        for (i in 0 until 28) {
            val a = i / 28f * 2f * PI.toFloat()
            val speed = 220f + (i % 5) * 60f
            val x = center.x + cos(a) * speed * t
            val y = center.y + sin(a) * speed * t + 260f * t * t
            drawCircle(colors[i % colors.size].copy(alpha = 1f - t), 7f, Offset(x, y))
        }
    }
}
