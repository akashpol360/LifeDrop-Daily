import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Secrets live in local.properties / keystore.properties (never commit them). See docs/RELEASE_GUIDE.md.
val localProps = Properties().apply { val f = rootProject.file("local.properties"); if (f.exists()) f.inputStream().use { load(it) } }
val keystoreProps = Properties().apply { val f = rootProject.file("keystore.properties"); if (f.exists()) f.inputStream().use { load(it) } }
fun cfg(name: String, default: String): String = localProps.getProperty(name) ?: providers.gradleProperty(name).orNull ?: default

android {
    namespace = "com.lifedrop.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.lifedrop.daily"
        minSdk = 26; targetSdk = 36
        versionCode = 2; versionName = "1.0.0"
        // Google's public TEST ad IDs are used until you add your own AdMob IDs to local.properties.
        // Google Play product used for the Pro purchase. Release always uses the real product.
        buildConfigField("String", "PRO_PRODUCT_ID", "\"premium_unlock\"")
        manifestPlaceholders["admobAppId"] = cfg("ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")
        buildConfigField("String", "ADMOB_BANNER_ID", "\"" + cfg("ADMOB_BANNER_ID", "ca-app-pub-3940256099942544/6300978111") + "\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"" + cfg("ADMOB_INTERSTITIAL_ID", "ca-app-pub-3940256099942544/1033173712") + "\"")
    }
    signingConfigs {
        if (keystoreProps.getProperty("storeFile") != null) create("release") {
            storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(bom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("com.google.android.gms:play-services-ads:23.3.0")
    implementation("com.android.billingclient:billing:8.3.0")
    testImplementation("junit:junit:4.13.2")
    ksp("androidx.room:room-compiler:2.6.1")
}
