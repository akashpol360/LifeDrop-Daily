package com.lifedrop.app.ui
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.roundToInt

/** Drop falls, bounces, ripples, title fades in. ~1.6s; skipped when animations are off. */
@Composable
fun Splash(animationsOn: Boolean, onDone: () -> Unit) {
    val fall = remember { Animatable(if (animationsOn) 0f else 1f) }
    val ripple = remember { Animatable(0f) }
    val title = remember { Animatable(if (animationsOn) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animationsOn) {
            fall.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
            ripple.animateTo(1f, tween(500))
            title.animateTo(1f, tween(400))
        }
        onDone()
    }
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Canvas(Modifier.size(200.dp)) {
            val a = 1f - ripple.value
            drawCircle(Color(0xFF4FC3F7).copy(alpha = a), size.width * 0.5f * ripple.value, center, style = Stroke(4f))
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val startY = -maxHeight.value * 0.5f
            MascotView(80.dp, animated = false, modifier = Modifier.align(Alignment.Center)
                .offset { IntOffset(0, ((1f - fall.value) * startY * density).roundToInt()) })
        }
        Column(Modifier.align(Alignment.Center).padding(top = 180.dp).alpha(title.value),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("LifeDrop Daily", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("Today is yours.", fontSize = 16.sp)
        }
    }
}
