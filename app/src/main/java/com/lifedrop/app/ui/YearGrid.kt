package com.lifedrop.app.ui
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.lifedrop.app.core.YearProgress

/** One water drop per day: filled = passed, big outlined = today, faint outline = ahead. */
@Composable
fun YearGrid(progress: YearProgress, animate: Boolean, modifier: Modifier = Modifier) {
    val reveal = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(progress.dayOfYear) {
        if (animate) reveal.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    val dropC = MaterialTheme.colorScheme.secondary
    val todayC = MaterialTheme.colorScheme.tertiary
    val ink = MaterialTheme.colorScheme.onSurface
    val dropPath = remember {
        Path().apply {
            moveTo(0f, -1.3f)
            cubicTo(0.95f, -0.1f, 0.95f, 0.95f, 0f, 0.95f)
            cubicTo(-0.95f, 0.95f, -0.95f, -0.1f, 0f, -1.3f)
            close()
        }
    }
    val cols = 20
    val rows = (progress.totalDays + cols - 1) / cols
    Canvas(modifier.fillMaxWidth().aspectRatio(cols.toFloat() / rows).semantics {
        contentDescription = "Year progress: ${progress.percent} percent, day ${progress.dayOfYear} of ${progress.totalDays}" }) {
        val cell = size.width / cols
        val r = cell * 0.36f
        val shown = (progress.dayOfYear * reveal.value).toInt()
        for (i in 0 until progress.totalDays) {
            val day = i + 1
            val cx = (i % cols + 0.5f) * cell
            val cy = (i / cols + 0.55f) * cell
            val k = if (day == progress.dayOfYear) 1.3f else 1f
            withTransform({ translate(cx, cy); scale(r * k, r * k, Offset.Zero) }) {
                when {
                    day == progress.dayOfYear -> { drawPath(dropPath, todayC); drawPath(dropPath, ink, style = Stroke(0.2f)) }
                    day <= shown -> drawPath(dropPath, dropC)
                    else -> drawPath(dropPath, Color.Gray.copy(alpha = 0.4f), style = Stroke(0.2f))
                }
            }
        }
    }
}
