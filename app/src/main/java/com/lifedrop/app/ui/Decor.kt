package com.lifedrop.app.ui
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Faint doodles behind the main screens, different for every theme. Drifts and twinkles only in Full animation mode. */
@Composable
fun DoodleBackground(theme: String, animated: Boolean) {
    val c = MaterialTheme.colorScheme.primary.copy(alpha = 0.11f)
    val a = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
    val tState: State<Float> = if (animated)
        rememberInfiniteTransition(label = "doodle").animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse), label = "p")
    else remember { mutableStateOf(0.5f) }
    Canvas(Modifier.fillMaxSize()) {
        val t = tState.value
        val w = size.width; val h = size.height
        val dx = (t - 0.5f) * 40f; val dy = (t - 0.5f) * 12f
        fun tw(col: Color, k: Float) = col.copy(alpha = col.alpha * (0.55f + 0.9f * (if (k > 0f) t else 1f - t)))
        fun star(cx: Float, cy: Float, r: Float, col: Color) {
            val p = Path()
            for (i in 0 until 10) {
                val rad = if (i % 2 == 0) r else r * 0.45f
                val an = (-PI / 2 + i * PI / 5).toFloat()
                val x = cx + cos(an) * rad; val y = cy + sin(an) * rad
                if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
            p.close(); drawPath(p, col)
        }
        fun cloud(cx: Float, cy: Float, r: Float, col: Color) {
            drawCircle(col, r, Offset(cx, cy)); drawCircle(col, r * 0.8f, Offset(cx + r, cy + r * 0.2f)); drawCircle(col, r * 0.7f, Offset(cx - r, cy + r * 0.3f))
        }
        fun drop(cx: Float, cy: Float, r: Float, col: Color) {
            drawCircle(col, r, Offset(cx, cy))
            drawPath(Path().apply { moveTo(cx - r * 0.9f, cy - r * 0.4f); lineTo(cx, cy - r * 2.2f); lineTo(cx + r * 0.9f, cy - r * 0.4f); close() }, col)
        }
        fun heart(cx: Float, cy: Float, r: Float, col: Color) {
            drawCircle(col, r * 0.55f, Offset(cx - r * 0.5f, cy)); drawCircle(col, r * 0.55f, Offset(cx + r * 0.5f, cy))
            drawPath(Path().apply { moveTo(cx - r * 1.02f, cy + r * 0.15f); lineTo(cx, cy + r * 1.3f); lineTo(cx + r * 1.02f, cy + r * 0.15f); close() }, col)
        }
        fun pine(cx: Float, cy: Float, r: Float, col: Color) {
            for (k in 0 until 3) {
                val top = cy - r * 1.6f + k * r * 0.7f; val base = cy - r * 0.4f + k * r * 0.7f; val half = r * (0.7f + k * 0.25f)
                drawPath(Path().apply { moveTo(cx, top); lineTo(cx - half, base); lineTo(cx + half, base); close() }, col)
            }
            drawRect(col, Offset(cx - r * 0.12f, cy + r), Size(r * 0.24f, r * 0.5f))
        }
        fun leaf(cx: Float, cy: Float, r: Float, col: Color) = rotate(35f, Offset(cx, cy)) { drawOval(col, Offset(cx - r * 0.5f, cy - r), Size(r, r * 2f)) }
        fun mushroom(cx: Float, cy: Float, r: Float, col: Color) {
            drawArc(col, 180f, 180f, true, Offset(cx - r, cy - r), Size(r * 2f, r * 2f)); drawRect(col, Offset(cx - r * 0.3f, cy), Size(r * 0.6f, r * 0.9f))
        }
        fun sun(cx: Float, cy: Float, r: Float, col: Color) {
            drawCircle(col, r, Offset(cx, cy))
            for (i in 0 until 8) {
                val an = (i * PI / 4).toFloat()
                drawLine(col, Offset(cx + cos(an) * r * 1.4f, cy + sin(an) * r * 1.4f), Offset(cx + cos(an) * r * 1.9f, cy + sin(an) * r * 1.9f), 5f, StrokeCap.Round)
            }
        }
        fun bird(cx: Float, cy: Float, r: Float, col: Color) {
            drawArc(col, 200f, 140f, false, Offset(cx - r, cy - r * 0.5f), Size(r, r), style = Stroke(4f, cap = StrokeCap.Round))
            drawArc(col, 200f, 140f, false, Offset(cx, cy - r * 0.5f), Size(r, r), style = Stroke(4f, cap = StrokeCap.Round))
        }
        fun lolly(cx: Float, cy: Float, r: Float, col: Color) {
            drawCircle(col, r, Offset(cx, cy)); drawCircle(col, r * 0.55f, Offset(cx, cy), style = Stroke(4f))
            drawLine(col, Offset(cx, cy + r), Offset(cx, cy + r * 2.6f), 6f, StrokeCap.Round)
        }
        fun moon(cx: Float, cy: Float, r: Float, col: Color) = drawArc(col, 40f, 280f, true, Offset(cx - r, cy - r), Size(r * 2f, r * 2f))
        when (theme) {
            "lavender" -> {
                moon(w * .85f, h * .1f, 24f, a); star(w * .12f, h * .15f, 18f, tw(c, 1f)); star(w * .9f, h * .5f, 20f, tw(a, -1f))
                star(w * .1f, h * .55f, 14f, tw(c, -1f)); cloud(w * .8f - dx, h * .88f, 34f, c); star(w * .5f, h * .96f, 12f, tw(a, 1f))
            }
            "mint" -> {
                leaf(w * .12f, h * .15f + dy, 16f, a); leaf(w * .9f, h * .3f - dy, 14f, c); drop(w * .1f, h * .6f, 16f, c)
                drop(w * .9f, h * .75f + dy, 14f, a); leaf(w * .15f, h * .88f, 18f, c); cloud(w * .8f - dx, h * .1f, 26f, c)
            }
            "sunset" -> {
                sun(w * .85f, h * .09f, 26f, a); cloud(w * .14f + dx, h * .14f, 30f, c)
                bird(w * .3f + dx, h * .3f, 22f, c); bird(w * .38f + dx, h * .27f, 15f, c); bird(w * .86f - dx, h * .6f, 18f, c)
                cloud(w * .8f - dx, h * .88f, 36f, c); star(w * .08f, h * .5f, 18f, tw(a, 1f))
            }
            "forest" -> {
                pine(w * .08f, h * .2f, 22f, c); pine(w * .92f, h * .45f, 26f, c); pine(w * .9f, h * .85f, 20f, c)
                leaf(w * .15f, h * .62f + dy, 16f, a); leaf(w * .88f, h * .14f - dy, 14f, a); mushroom(w * .1f, h * .88f, 20f, a); leaf(w * .75f, h * .3f + dy, 12f, c)
            }
            "candy" -> {
                heart(w * .88f, h * .09f + dy, 18f, a); heart(w * .1f, h * .4f - dy, 14f, c); lolly(w * .12f, h * .12f, 20f, a)
                lolly(w * .9f, h * .66f, 18f, c); star(w * .85f, h * .3f, 22f, tw(a, 1f)); star(w * .08f, h * .75f, 18f, tw(c, -1f)); heart(w * .85f, h * .9f, 16f, c)
            }
            else -> {
                star(w * .9f, h * .08f, 28f, tw(c, 1f)); star(w * .08f, h * .35f, 20f, tw(c, -1f)); star(w * .85f, h * .62f, 24f, tw(c, 1f))
                cloud(w * .15f + dx, h * .12f, 34f, c); cloud(w * .8f - dx, h * .88f, 40f, c)
                drop(w * .1f, h * .78f + dy, 18f, c); drop(w * .92f, h * .4f - dy, 14f, c)
            }
        }
    }
}

/** "+10 XP" flies from the button toward the XP counter ([dx], [dy] in px); rises straight up when the counter is not on screen. */
@Composable
fun XpFloat(fire: Boolean, enabled: Boolean, label: String, dx: Float = 0f, dy: Float = -160f) {
    val t = remember { Animatable(1f) }
    LaunchedEffect(fire) { if (fire && enabled) { t.snapTo(0f); t.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) } }
    if (t.value < 1f) Text("⭐ $label", fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold, fontSize = 22.sp,
        modifier = Modifier.graphicsLayer {
            translationX = dx * t.value; translationY = dy * t.value
            alpha = 1f - t.value * t.value; scaleX = 1f - 0.4f * t.value; scaleY = 1f - 0.4f * t.value
        })
}
