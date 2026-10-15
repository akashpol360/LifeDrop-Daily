package com.lifedrop.app.ui
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

object Themes {
    val names = listOf("ocean", "sunset", "forest", "candy", "lavender", "mint")
    val premiumOnly = setOf("sunset", "forest", "candy", "lavender", "mint")
    val fonts = listOf("default" to "Default", "serif" to "Serif", "mono" to "Mono", "cursive" to "Handwritten")
    private data class T(val p: Long, val c: Long, val bgL: Long, val bgD: Long, val pD: Long, val body: Long, val acc: Long, val cardD: Long)

    fun scheme(name: String, dark: Boolean): ColorScheme {
        val t = when (name) {
            "sunset" -> T(0xFFE64A19, 0xFFFFE0B2, 0xFFFFF3E0, 0xFF2B1410, 0xFFFF8A65, 0xFFFFB74D, 0xFFC62828, 0xFF4A2A1E)
            "forest" -> T(0xFF2E7D32, 0xFFC8E6C9, 0xFFF1F8E9, 0xFF10261A, 0xFF81C784, 0xFF81C784, 0xFFF9A825, 0xFF1E4030)
            "candy" -> T(0xFFC2185B, 0xFFF8BBD0, 0xFFFFF0F6, 0xFF2A1020, 0xFFF48FB1, 0xFFF48FB1, 0xFF7E57C2, 0xFF4A2038)
            "lavender" -> T(0xFF6A1B9A, 0xFFE1BEE7, 0xFFF8F0FF, 0xFF1E1230, 0xFFCE93D8, 0xFFB39DDB, 0xFFFFB300, 0xFF3A2456)
            "mint" -> T(0xFF00796B, 0xFFB2DFDB, 0xFFF0FBF9, 0xFF0D2622, 0xFF80CBC4, 0xFF80CBC4, 0xFFFF7043, 0xFF17423B)
            else -> T(0xFF0288D1, 0xFFE1F5FE, 0xFFF1FBFF, 0xFF0D2137, 0xFF4FC3F7, 0xFF4FC3F7, 0xFFFF8A65, 0xFF163A5A)
        }
        // secondaryContainer is a translucent primary so selected chips and the selected tab stand out on tinted cards.
        return if (dark) darkColorScheme(primary = Color(t.pD), secondary = Color(t.body), tertiary = Color(t.acc),
            background = Color(t.bgD), surface = Color(t.bgD), surfaceContainerHighest = Color(t.cardD), surfaceContainer = Color(t.cardD),
            secondaryContainer = Color(t.pD).copy(alpha = 0.38f), primaryContainer = Color(t.cardD))
        else lightColorScheme(primary = Color(t.p), secondary = Color(t.body), tertiary = Color(t.acc), primaryContainer = Color(t.c),
            background = Color(t.bgL), surface = Color(t.bgL), surfaceContainerHighest = Color(t.c), surfaceContainer = Color(t.c),
            secondaryContainer = Color(t.p).copy(alpha = 0.28f))
    }

    fun typography(font: String): Typography {
        val ff = when (font) { "serif" -> FontFamily.Serif; "mono" -> FontFamily.Monospace; "cursive" -> FontFamily.Cursive; else -> FontFamily.Default }
        val b = Typography()
        return b.copy(
            displayLarge = b.displayLarge.copy(fontFamily = ff), displayMedium = b.displayMedium.copy(fontFamily = ff), displaySmall = b.displaySmall.copy(fontFamily = ff),
            headlineLarge = b.headlineLarge.copy(fontFamily = ff), headlineMedium = b.headlineMedium.copy(fontFamily = ff), headlineSmall = b.headlineSmall.copy(fontFamily = ff),
            titleLarge = b.titleLarge.copy(fontFamily = ff), titleMedium = b.titleMedium.copy(fontFamily = ff), titleSmall = b.titleSmall.copy(fontFamily = ff),
            bodyLarge = b.bodyLarge.copy(fontFamily = ff), bodyMedium = b.bodyMedium.copy(fontFamily = ff), bodySmall = b.bodySmall.copy(fontFamily = ff),
            labelLarge = b.labelLarge.copy(fontFamily = ff), labelMedium = b.labelMedium.copy(fontFamily = ff), labelSmall = b.labelSmall.copy(fontFamily = ff))
    }
}
