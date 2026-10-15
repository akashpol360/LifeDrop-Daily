package com.lifedrop.app.ads
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.lifedrop.app.BuildConfig
import com.lifedrop.app.data.AppPrefs

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Ad unit IDs come from local.properties (Google test IDs by default). See docs/RELEASE_GUIDE.md. */
object Ads {
    val BANNER_ID: String get() = BuildConfig.ADMOB_BANNER_ID
    val INTERSTITIAL_ID: String get() = BuildConfig.ADMOB_INTERSTITIAL_ID
    private var interstitial: InterstitialAd? = null
    private var loading = false

    fun init(activity: Activity) {
        Thread {
            MobileAds.initialize(activity.applicationContext) {}
            activity.runOnUiThread { load(activity) }
        }.start()
    }

    fun load(ctx: Context) {
        if (interstitial != null || loading) return
        loading = true
        InterstitialAd.load(ctx, INTERSTITIAL_ID, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad; loading = false }
            override fun onAdFailedToLoad(error: LoadAdError) { interstitial = null; loading = false }
        })
    }

    /** Shows an interstitial for free users when one is ready; Premium users never see it. */
    fun show(activity: Activity, prefs: AppPrefs) {
        if (prefs.premium) return
        val ad = interstitial
        if (ad == null) { load(activity); return }
        interstitial = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { load(activity) }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { load(activity) }
        }
        ad.show(activity)
    }
}

/** Returns a function: gate { action } runs the action, then shows an interstitial to free users. */
@Composable
fun rememberAdGate(prefs: AppPrefs): (() -> Unit) -> Unit {
    val ctx = LocalContext.current
    return remember(ctx, prefs) {
        val gate: (() -> Unit) -> Unit = { action ->
            action()
            ctx.findActivity()?.let { Ads.show(it, prefs) }
        }
        gate
    }
}

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val adView = remember {
        AdView(ctx).apply { setAdSize(AdSize.BANNER); adUnitId = Ads.BANNER_ID; loadAd(AdRequest.Builder().build()) }
    }
    DisposableEffect(adView) { onDispose { adView.destroy() } }
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { AndroidView(factory = { adView }) }
}
