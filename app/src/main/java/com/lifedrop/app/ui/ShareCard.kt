package com.lifedrop.app.ui
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/** Draws a 1080x1080 LifeDrop card with the mascot and the text, then shares it as an image. */
object ShareCard {
    fun share(ctx: Context, header: String, text: String) {
        val size = 1080
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(0xFFE1F5FE.toInt())
        val ink = 0xFF1B3A57.toInt()
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val cx = size / 2f
        val drop = Path().apply {
            moveTo(cx, 70f); cubicTo(cx + 150f, 230f, cx + 120f, 330f, cx, 330f)
            cubicTo(cx - 120f, 330f, cx - 150f, 230f, cx, 70f); close()
        }
        p.style = Paint.Style.FILL; p.color = 0xFF4FC3F7.toInt(); c.drawPath(drop, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 8f; p.color = ink; c.drawPath(drop, p)
        p.style = Paint.Style.FILL; c.drawCircle(cx - 45f, 235f, 12f, p); c.drawCircle(cx + 45f, 235f, 12f, p)
        p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND; c.drawArc(cx - 40f, 235f, cx + 40f, 290f, 20f, 140f, false, p)
        val head = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = ink; textSize = 46f; typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC) }
        val body = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = ink; textSize = if (text.length > 110) 50f else 64f; typeface = Typeface.DEFAULT_BOLD }
        c.save(); c.translate(100f, 400f)
        StaticLayout.Builder.obtain(header, 0, header.length, head, 880).setAlignment(Layout.Alignment.ALIGN_CENTER).build().draw(c)
        c.translate(0f, 90f)
        val q = "\"$text\""
        StaticLayout.Builder.obtain(q, 0, q.length, body, 880).setAlignment(Layout.Alignment.ALIGN_CENTER).build().draw(c)
        c.restore()
        head.textAlign = Paint.Align.CENTER; c.drawText("💧 LifeDrop Daily", cx, 1020f, head)
        val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
        val f = File(dir, "lifedrop_card.png")
        FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        val i = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TEXT, "$header\n\"$text\"\n\n💧 LifeDrop Daily")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(i, "Share"))
    }
}
