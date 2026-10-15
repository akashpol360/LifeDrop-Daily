package com.lifedrop.app.ui
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Global animation switch (set from MainActivity). Cards skip their entrance when false. */
var lifeAnim by mutableStateOf(true)
/** Decorative motion (floating, drifting, card bounce): Full mode only. */
var lifeAmbient by mutableStateOf(true)

/** Notebook-style card: dashed hand-drawn border, cursive heading, small bouncy entrance. */
@Composable
fun LifeCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    var shown by remember { mutableStateOf(!lifeAmbient) }
    LaunchedEffect(Unit) { shown = true }
    val s by animateFloatAsState(if (shown) 1f else 0.95f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), label = "in")
    val a by animateFloatAsState(if (shown) 1f else 0f, tween(300), label = "a")
    val line = MaterialTheme.colorScheme.primary
    Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()
        .graphicsLayer { scaleX = s; scaleY = s; alpha = a }
        .drawWithContent {
            drawContent()
            drawRoundRect(line, cornerRadius = CornerRadius(24.dp.toPx()),
                style = Stroke(3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f))))
        }) {
        Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
