package com.lifedrop.app.billing
import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.*
import com.android.billingclient.api.*
import com.lifedrop.app.BuildConfig
import com.lifedrop.app.data.AppPrefs

enum class ProState { LOADING, READY, UNAVAILABLE }

/**
 * One-time "Pro" purchase through Google Play Billing (library 8.x).
 * The price shown to the user always comes from Google Play, so it is localized (for example in rupees).
 * Entitlement is re-checked with Google Play on every launch: a purchase unlocks Pro, a refund removes it.
 */
class Billing(ctx: Context, private val prefs: AppPrefs) : PurchasesUpdatedListener {
    companion object { val PRODUCT_ID: String = BuildConfig.PRO_PRODUCT_ID }
    private val main = Handler(Looper.getMainLooper())
    private val client: BillingClient = BillingClient.newBuilder(ctx).setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection().build()
    private var details: ProductDetails? = null
    private var connecting = false

    /** Localized price from Google Play, or null until it has loaded. */
    var price by mutableStateOf<String?>(null); private set
    /** LOADING while asking Google Play, READY when the price is known, UNAVAILABLE when Google Play cannot offer the product. */
    var state by mutableStateOf(ProState.LOADING); private set
    /** Short status or error text for the Pro screen. */
    var message by mutableStateOf<String?>(null); private set
    /** Last Google Play response code (shown in debug builds only). */
    var lastCode by mutableStateOf<Int?>(null); private set

    private fun post(block: () -> Unit) { main.post(block) }

    private fun explain(code: Int): String = when (code) {
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "Google Play billing is not available on this phone. Make sure the Play Store is installed, updated and signed in."
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE, BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
        BillingClient.BillingResponseCode.NETWORK_ERROR -> "Could not reach Google Play. Check your internet connection and try again."
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> "Pro is not available for purchase yet. Please try again later."
        BillingClient.BillingResponseCode.DEVELOPER_ERROR -> "Pro is not set up correctly in Google Play yet."
        BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> "This phone's Google Play does not support in-app purchases."
        else -> "Google Play could not load Pro (code $code). Please try again."
    }

    fun connect() {
        if (client.isReady) { queryDetails(); return }
        if (connecting) return
        connecting = true
        state = ProState.LOADING
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                post { lastCode = result.responseCode }
                if (result.responseCode == BillingClient.BillingResponseCode.OK) { queryDetails(); syncPurchases(false) }
                else post { state = ProState.UNAVAILABLE; message = explain(result.responseCode) }
            }
            override fun onBillingServiceDisconnected() { connecting = false }
        })
        // If Google Play never answers (no Play Store, no network), stop showing "loading" forever.
        main.postDelayed({
            if (state == ProState.LOADING && price == null) { connecting = false; state = ProState.UNAVAILABLE; message = "Google Play did not respond. Check your connection and try again." }
        }, 10_000)
    }

    /** "Try again" on the Pro screen. */
    fun retry() { message = null; state = ProState.LOADING; connect() }

    fun close() { client.endConnection() }

    private fun queryDetails() {
        val product = QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build()
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()) { result, queryResult ->
            val code = result.responseCode
            val d = if (code == BillingClient.BillingResponseCode.OK) queryResult.productDetailsList.firstOrNull() else null
            details = d
            val p = d?.oneTimePurchaseOfferDetails?.formattedPrice ?: d?.oneTimePurchaseOfferDetailsList?.firstOrNull()?.formattedPrice
            post {
                lastCode = code
                price = p
                if (p != null) { state = ProState.READY; message = null }
                else {
                    state = ProState.UNAVAILABLE
                    message = if (code == BillingClient.BillingResponseCode.OK) "Google Play does not offer Pro for this install yet. Please try again later." else explain(code)
                }
            }
        }
    }

    /** Opens the Google Play purchase sheet. */
    fun buy(activity: Activity) {
        val d = details
        if (d == null) { retry(); return }
        message = null
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build())).build()
        val r = client.launchBillingFlow(activity, params)
        if (r.responseCode != BillingClient.BillingResponseCode.OK) { lastCode = r.responseCode; message = "Could not open Google Play (code ${r.responseCode}). Please try again." }
    }

    /** "Restore purchase" button. */
    fun restore() { if (client.isReady) syncPurchases(true) else { message = "Connecting to Google Play..."; connect() } }

    /** Silent re-check, called every time the app opens. */
    fun sync() { if (client.isReady) syncPurchases(false) else connect() }

    private fun syncPurchases(manual: Boolean) {
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) { result, list ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                if (manual) post { message = "Could not reach Google Play. Check your connection and try again." }
                return@queryPurchasesAsync
            }
            val owned = list.filter { PRODUCT_ID in it.products }
            val purchased = owned.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            val pending = owned.any { it.purchaseState == Purchase.PurchaseState.PENDING }
            owned.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }.forEach {
                client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(it.purchaseToken).build()) {}
            }
            post {
                when {
                    purchased -> { prefs.setPremium(true); message = null }
                    pending -> message = "Your payment is pending. Pro unlocks as soon as Google Play confirms it."
                    else -> { prefs.setPremium(false); if (manual) message = "No Pro purchase found for this Google account." }
                }
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> syncPurchases(true)
            BillingClient.BillingResponseCode.USER_CANCELED -> post { message = null }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> syncPurchases(true)
            else -> post { lastCode = result.responseCode; message = "The purchase did not go through (code ${result.responseCode}). You have not been charged. Please try again." }
        }
    }
}
