package com.lifedrop.app.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Gold "PRO MEMBER" pill shown to Premium users. */
@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Row(modifier.clip(RoundedCornerShape(50))
        .background(Brush.horizontalGradient(listOf(Color(0xFFFFE082), Color(0xFFFFA000))))
        .padding(horizontal = 12.dp, vertical = 4.dp)
        .semantics { contentDescription = "Pro member" }, verticalAlignment = Alignment.CenterVertically) {
        Text("👑", fontSize = 12.sp, fontFamily = FontFamily.Default)
        Spacer(Modifier.width(4.dp))
        Text("PRO MEMBER", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF3E2723), fontFamily = FontFamily.Default)
    }
}

/** Main content area; shows the Pro badge in its own strip at the top for Premium users (never overlaps content). */
@Composable
fun ProFrame(pad: PaddingValues, premium: Boolean, content: @Composable BoxScope.() -> Unit) {
    Column(Modifier.padding(pad).consumeWindowInsets(pad).imePadding()) {
        if (premium) Row(Modifier.fillMaxWidth().padding(end = 12.dp, top = 4.dp), horizontalArrangement = Arrangement.End) { ProBadge() }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) { Box(Modifier.widthIn(max = 720.dp).fillMaxSize(), content = content) }
    }
}

/** Emoji + label for buttons. The emoji uses the default font so it stays aligned with the text in every font choice (e.g. Mono). */
@Composable
fun IconLabel(icon: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontFamily = FontFamily.Default)
        Spacer(Modifier.width(6.dp))
        Text(label)
    }
}
