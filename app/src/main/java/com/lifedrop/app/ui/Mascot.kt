package com.lifedrop.app.ui
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class Mood { HAPPY, THINKING, CELEBRATING, SLEEPING, SURPRISED, CURIOUS, READING, WRITING, CONFUSED, EXCITED, MOTIVATING, RELAXING, RUNNING }

/** Decorative; the parent supplies any accessibility description. */
@Composable
fun MascotView(size: Dp = 120.dp, modifier: Modifier = Modifier, mood: Mood = Mood.HAPPY, animated: Boolean = true) {
    val bodyC = MaterialTheme.colorScheme.secondary
    val px = with(LocalDensity.current) { size.toPx() }
    val bob: State<Float> = if (animated && lifeAmbient)
        rememberInfiniteTransition(label = "mascot").animateFloat(-1f, 1f, infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse), label = "bob")
    else remember { mutableStateOf(0f) }
    Canvas(modifier.size(size).graphicsLayer { translationY = bob.value * px * 0.05f }) {
        val w = this.size.width; val h = this.size.height; val cx = w / 2
        val ink = Color(0xFF1B3A57); val eyeY = h * 0.6f; val lw = w * 0.035f
        val body = Path().apply {
            moveTo(cx, 0f); cubicTo(w, h * 0.5f, w * 0.85f, h, cx, h); cubicTo(w * 0.15f, h, 0f, h * 0.5f, cx, 0f); close()
        }
        drawPath(body, bodyC)
        drawPath(body, ink, style = Stroke(w * 0.04f, cap = StrokeCap.Round))
        fun arch(x: Float, up: Boolean) = drawArc(ink, if (up) 180f else 0f, 180f, false,
            Offset(x - w * 0.05f, eyeY - w * 0.05f), Size(w * 0.1f, w * 0.1f), style = Stroke(lw, cap = StrokeCap.Round))
        val lx = w * 0.38f; val rx = w * 0.62f
        when (mood) {
            Mood.CELEBRATING -> { arch(lx, true); arch(rx, true) }
            Mood.SLEEPING, Mood.RELAXING -> { arch(lx, false); arch(rx, false) }
            Mood.SURPRISED -> { drawCircle(ink, w * 0.07f, Offset(lx, eyeY)); drawCircle(ink, w * 0.07f, Offset(rx, eyeY)) }
            else -> { drawCircle(ink, w * 0.05f, Offset(lx, eyeY)); drawCircle(ink, w * 0.05f, Offset(rx, eyeY)) }
        }
        when (mood) {
            Mood.CELEBRATING, Mood.EXCITED -> drawArc(ink, 0f, 180f, true, Offset(w * 0.4f, h * 0.66f), Size(w * 0.2f, h * 0.13f))
            Mood.THINKING -> drawLine(ink, Offset(w * 0.42f, h * 0.74f), Offset(w * 0.58f, h * 0.71f), lw, StrokeCap.Round)
            Mood.SLEEPING -> drawCircle(ink, w * 0.03f, Offset(cx, h * 0.73f), style = Stroke(lw))
            Mood.SURPRISED -> drawCircle(ink, w * 0.05f, Offset(cx, h * 0.74f))
            Mood.CONFUSED -> drawLine(ink, Offset(w * 0.42f, h * 0.73f), Offset(w * 0.58f, h * 0.75f), lw, StrokeCap.Round)
            Mood.HAPPY, Mood.CURIOUS, Mood.READING, Mood.WRITING, Mood.RELAXING, Mood.RUNNING, Mood.MOTIVATING -> drawArc(ink, 20f, 140f, false, Offset(w * 0.4f, h * 0.62f), Size(w * 0.2f, h * 0.14f), style = Stroke(lw, cap = StrokeCap.Round))
        }
        if (mood == Mood.THINKING) {
            drawCircle(ink.copy(alpha = 0.5f), w * 0.02f, Offset(w * 0.86f, h * 0.2f))
            drawCircle(ink.copy(alpha = 0.5f), w * 0.03f, Offset(w * 0.92f, h * 0.12f))
        }
        when (mood) {
            Mood.CURIOUS -> drawLine(ink, Offset(rx - w * 0.06f, eyeY - w * 0.12f), Offset(rx + w * 0.06f, eyeY - w * 0.16f), lw, StrokeCap.Round)
            Mood.CONFUSED -> {
                drawLine(ink, Offset(lx - w * 0.06f, eyeY - w * 0.15f), Offset(lx + w * 0.06f, eyeY - w * 0.11f), lw, StrokeCap.Round)
                drawLine(ink, Offset(rx - w * 0.06f, eyeY - w * 0.11f), Offset(rx + w * 0.06f, eyeY - w * 0.15f), lw, StrokeCap.Round)
            }
            Mood.READING -> drawRoundRect(Color(0xFFFFCA28), Offset(w * 0.3f, h * 0.84f), Size(w * 0.4f, h * 0.12f), androidx.compose.ui.geometry.CornerRadius(6f))
            Mood.EXCITED -> {
                drawCircle(Color(0xFFFFCA28), w * 0.025f, Offset(w * 0.12f, h * 0.35f)); drawCircle(Color(0xFFFFCA28), w * 0.025f, Offset(w * 0.9f, h * 0.3f))
                drawCircle(Color(0xFFFFCA28), w * 0.02f, Offset(w * 0.08f, h * 0.5f))
            }
            Mood.MOTIVATING -> {
                drawLine(ink, Offset(lx - w * 0.06f, eyeY - w * 0.12f), Offset(lx + w * 0.06f, eyeY - w * 0.15f), lw, StrokeCap.Round)
                drawLine(ink, Offset(rx - w * 0.06f, eyeY - w * 0.15f), Offset(rx + w * 0.06f, eyeY - w * 0.12f), lw, StrokeCap.Round)
                drawLine(ink, Offset(w * 0.88f, h * 0.62f), Offset(w * 0.97f, h * 0.42f), lw, StrokeCap.Round); drawCircle(ink, w * 0.045f, Offset(w * 0.97f, h * 0.4f))
            }
            Mood.RELAXING -> {
                drawCircle(Color(0xFFFF8A80).copy(alpha = 0.5f), w * 0.05f, Offset(w * 0.27f, h * 0.7f)); drawCircle(Color(0xFFFF8A80).copy(alpha = 0.5f), w * 0.05f, Offset(w * 0.73f, h * 0.7f))
            }
            Mood.RUNNING -> for (k in 0 until 3) drawLine(ink.copy(alpha = 0.5f), Offset(w * 0.0f, h * (0.5f + k * 0.1f)), Offset(w * (0.12f - k * 0.02f), h * (0.5f + k * 0.1f)), lw, StrokeCap.Round)
            Mood.WRITING -> drawLine(Color(0xFFFF8A65), Offset(w * 0.72f, h * 0.9f), Offset(w * 0.92f, h * 0.68f), w * 0.05f, StrokeCap.Round)
            else -> {}
        }
        drawCircle(Color.White.copy(alpha = 0.6f), w * 0.05f, Offset(w * 0.3f, h * 0.45f))
    }
}
