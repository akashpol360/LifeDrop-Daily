package com.lifedrop.app
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifedrop.app.ads.Ads
import com.lifedrop.app.ads.BannerAd
import com.lifedrop.app.billing.Billing
import com.lifedrop.app.content.DailyContent
import com.lifedrop.app.content.Extra
import com.lifedrop.app.content.Festival
import com.lifedrop.app.content.Questions
import com.lifedrop.app.data.AppPrefs
import com.lifedrop.app.data.AppVm
import com.lifedrop.app.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val prefs by lazy { AppPrefs(applicationContext) }
    private lateinit var billing: Billing

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val systemAnim = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {}
                .launch(android.Manifest.permission.POST_NOTIFICATIONS)
        com.lifedrop.app.notify.Reminders.schedule(this)
        com.lifedrop.app.notify.NotifyExtras.updateOngoing(this)
        Ads.init(this)
        billing = Billing(applicationContext, prefs).also { it.connect() }
        val startTab = intent?.getIntExtra("tab", 0) ?: 0
        val startScreen = intent?.getStringExtra("screen").orEmpty()
        setContent {
            val themeName = if (prefs.theme in Themes.premiumOnly && !prefs.premium) "ocean" else prefs.theme
            val fontName = if (prefs.premium) prefs.font else "default"
            MaterialTheme(colorScheme = Themes.scheme(themeName, isSystemInDarkTheme()), typography = Themes.typography(fontName)) {
                Surface(Modifier.fillMaxSize()) {
                    val ctx = LocalContext.current
                    val vm: AppVm = viewModel()
                    val animationsOn = systemAnim && prefs.anim
                    SideEffect { lifeAnim = animationsOn; lifeAmbient = animationsOn && prefs.animMode == 0 }
                    // Re-read the date every time the app returns to the foreground, so it updates after midnight.
                    var tick by remember { mutableIntStateOf(0) }
                    DisposableEffect(Unit) {
                        val o = androidx.lifecycle.LifecycleEventObserver { _, e ->
                            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) tick++ }
                        this@MainActivity.lifecycle.addObserver(o)
                        onDispose { this@MainActivity.lifecycle.removeObserver(o) }
                    }
                    val today = remember(tick) { LocalDate.now() }
                    val drops = remember { DailyContent.load(ctx) }
                    val feed = remember { Extra.feed(ctx) }
                    val think = remember { Questions.load(ctx, "think.json", "t") }
                    val whatIf = remember { Questions.load(ctx, "whatif.json", "w") }
                    // Calculated off the main thread; results are cached per year.
                    val upcoming by produceState(emptyList<Festival>(), today.year, today) {
                        value = withContext(Dispatchers.Default) { Extra.calendar(ctx, listOf(today.year, today.year + 1)).filter { it.date >= today } }
                    }
                    val festival = upcoming.firstOrNull { it.type == "fest" || it.type == "hol" }
                    val sky = upcoming.firstOrNull { it.type == "sky" }
                    var splash by rememberSaveable { mutableStateOf(true) }
                    var tab by rememberSaveable { mutableIntStateOf(startTab) }
                    var screen by rememberSaveable { mutableStateOf(startScreen) }
                    var reviewYear by rememberSaveable { mutableIntStateOf(0) }
                    val mainBranch = !splash && prefs.onboarded && screen.isEmpty()
                    // Android 15+ draws edge to edge: screens without a Scaffold keep clear of the status and navigation bars.
                    Box(if (mainBranch) Modifier else Modifier.safeDrawingPadding()) {
                        if (splash) Splash(animationsOn) { splash = false }
                        else if (!prefs.onboarded) Onboarding(animationsOn) { prefs.finishOnboarding(it) }
                        else if (screen.isNotEmpty()) {
                            BackHandler { screen = "" }
                            Column {
                                TextButton({ screen = "" }, Modifier.heightIn(min = 48.dp)) { Text("← Back") }
                                when (screen) {
                                    "review" -> ReviewScreen(if (reviewYear != 0) reviewYear else today.year, vm, prefs)
                                    "favs" -> FavoritesScreen(feed, think, whatIf, vm)
                                    "year" -> YearScreen(today, animationsOn)
                                    "festivals" -> CalendarScreen(today) { y -> Extra.year(ctx, y) }
                                    "settings" -> SettingsScreen(prefs, vm) { screen = "pro" }
                                    "pro" -> ProScreen(prefs, billing)
                                    else -> Unit
                                }
                            }
                        } else {
                            val tabs = listOf("🏠" to "Home", "💧" to "Drops", "🧠" to "Think", "📖" to "Journal", "👤" to "Profile")
                            val primary = MaterialTheme.colorScheme.primary
                            Scaffold(bottomBar = {
                                Column {
                                    if (!prefs.premium) BannerAd(Modifier.fillMaxWidth())
                                    NavigationBar {
                                        tabs.forEachIndexed { i, (e, l) ->
                                            val sel = tab == i
                                            val sc by animateFloatAsState(if (sel) 1.25f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "nav")
                                            NavigationBarItem(sel, { tab = i },
                                                icon = { Text(e, fontSize = 20.sp, modifier = Modifier.graphicsLayer { scaleX = sc; scaleY = sc }) },
                                                label = { Text(l, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) },
                                                colors = NavigationBarItemDefaults.colors(indicatorColor = primary.copy(alpha = 0.30f), selectedTextColor = primary))
                                        }
                                    }
                                }
                            }) { pad ->
                                ProFrame(pad, prefs.premium) {
                                    DoodleBackground(themeName, lifeAmbient)
                                    BadgeCelebration(today, vm, prefs)
                                    YearTransition(today, prefs) { reviewYear = it; screen = "review" }
                                    LevelUpCelebration(today, vm, prefs)
                                    NewDayDrop(today, prefs)
                                    Crossfade(tab, animationSpec = if (lifeAmbient) tween(250) else snap(), label = "tab") { t ->
                                        when (t) {
                                            0 -> HomeScreen(today, DailyContent.forDate(drops, today), festival, sky, animationsOn, vm, prefs) { screen = it }
                                            1 -> FeedScreen(feed, vm)
                                            2 -> ThinkScreen(think, whatIf, today, vm, prefs)
                                            3 -> JournalScreen(today, vm, prefs)
                                            else -> ProfileScreen(today, vm, prefs, { reviewYear = 0; screen = "review" }, { screen = "settings" }, { screen = "favs" }, { screen = "pro" })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (::billing.isInitialized) billing.close()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        billing.sync() // re-checks the purchase with Google Play (restores Pro on a new phone, removes it after a refund)
    }
}
