package com.lifedrop.app.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifedrop.app.ads.findActivity
import com.lifedrop.app.BuildConfig
import com.lifedrop.app.billing.Billing
import com.lifedrop.app.billing.ProState
import com.lifedrop.app.data.AppPrefs

/** The Pro screen: shows what Pro includes and the one-time price from Google Play, then opens the Google Play purchase sheet. */
@Composable
fun ProScreen(prefs: AppPrefs, billing: Billing) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { billing.connect() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MascotView(96.dp, mood = if (prefs.premium) Mood.CELEBRATING else Mood.EXCITED)
                Text("LifeDrop Pro", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Text("One payment. No ads. Yours forever.", fontSize = 15.sp, textAlign = TextAlign.Center)
            }
        }
        item {
            LifeCard("What you get") {
                listOf("🚫  No ads, ever", "🎨  All 6 themes", "✍️  Custom fonts", "🎯  Unlimited custom events", "🛡️  Streak protection",
                    "📊  Advanced year insights", "🧩  Progress and Streak widgets").forEach {
                    Text(it, Modifier.fillMaxWidth().padding(vertical = 4.dp), fontSize = 16.sp)
                }
            }
        }
        item {
            if (prefs.premium) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ProBadge()
                    Spacer(Modifier.height(8.dp))
                    Text("You are a Pro member. Thank you for supporting LifeDrop!", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (billing.state) {
                        ProState.READY -> {
                            Text(billing.price.orEmpty(), fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
                            Text("one-time payment • no subscription", fontSize = 13.sp)
                        }
                        ProState.LOADING -> {
                            CircularProgressIndicator(Modifier.size(32.dp))
                            Spacer(Modifier.height(6.dp))
                            Text("Loading price from Google Play...", fontSize = 14.sp)
                        }
                        ProState.UNAVAILABLE -> Text("Price unavailable", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Button({
                        when (billing.state) {
                            ProState.READY -> ctx.findActivity()?.let { billing.buy(it) }
                            ProState.UNAVAILABLE -> billing.retry()
                            ProState.LOADING -> Unit
                        }
                    }, enabled = billing.state != ProState.LOADING, shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Text(when (billing.state) {
                            ProState.READY -> "Upgrade for ${billing.price.orEmpty()}"
                            ProState.UNAVAILABLE -> "Try again"
                            ProState.LOADING -> "Please wait..."
                        }, fontWeight = FontWeight.Bold)
                    }
                    billing.message?.let { Spacer(Modifier.height(8.dp)); Text(it, fontSize = 13.sp, textAlign = TextAlign.Center) }
                    if (BuildConfig.DEBUG) {
                        Spacer(Modifier.height(8.dp))
                        Text("Debug build: Google Play only returns the price to builds installed from a Play Console testing track. " +
                            "Product ${Billing.PRODUCT_ID}, last response code ${billing.lastCode ?: "none"}.", fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                    TextButton({ billing.restore() }, Modifier.heightIn(min = 48.dp)) { Text("Restore purchase") }
                    Text("Paid through Google Play and linked to your Google account, so you can restore it on a new phone.", fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
