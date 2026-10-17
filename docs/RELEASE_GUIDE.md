# LifeDrop Daily - Play Store release guide (v1.0.0)

Developer contact: akpolstudio@gmail.com

## 1. What this build targets
- compileSdk / targetSdk **36** (Google Play requires API 36 for new apps and updates since 31 Aug 2026), minSdk 26.
- Play Billing Library **8.3.0** (Google Play requires 8.0.0 or newer).
- Android Gradle Plugin 8.9.1, Gradle 8.11.1, JDK 17. In Android Studio: **File > Sync**, and accept the prompt to install SDK Platform 36 if asked.

## 2. Put your own AdMob IDs in `local.properties`
The build uses Google's public TEST ad IDs until you add yours (test ads show a "Test Ad" label and earn nothing). Add to `local.properties` (this file is git-ignored):
```
ADMOB_APP_ID=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
ADMOB_BANNER_ID=ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ
ADMOB_INTERSTITIAL_ID=ca-app-pub-XXXXXXXXXXXXXXXX/WWWWWWWWWW
```
In AdMob: add the app (Android, package `com.lifedropdaily.app`), create one Banner and one Interstitial ad unit. For users in the EU/UK also set up the consent message in AdMob > Privacy & messaging. Never tap your own live ads.

## 3. Create the signing key (once) and build the bundle
1. Android Studio > **Build > Generate Signed App Bundle / APK > Android App Bundle > Create new...** Save the `.jks` file and its passwords somewhere safe and **back it up**. Losing it means you cannot update the app without Play App Signing recovery.
2. Choose the **release** build type and finish. The `.aab` appears under `app/release/`.
(Optional: put `storeFile`, `storePassword`, `keyAlias`, `keyPassword` in `keystore.properties` and run `./gradlew bundleRelease`.)
Use **Play App Signing** when Play Console asks (default).

## 4. Play Console checklist
1. Create app: name "LifeDrop Daily" (if the name is taken, change the label in `AndroidManifest.xml` and the store name together), default language, **App**, **Free**.
2. **Main store listing**: use `docs/STORE_LISTING.md`, 512x512 icon, 1024x500 feature graphic, at least 2 phone screenshots (Home, Think, Journal, Calendar, Pro screen).
3. **Privacy policy**: host `docs/PRIVACY_POLICY.md` at a public web page (GitHub Pages works) and paste the link.
4. **App content**: Ads = **Yes, contains ads**. Target audience **13+** (not for children). Content rating questionnaire (expect Everyone / 3+). News app = No. Health = No (the mood picker is for personal journaling only).
5. **Data safety** (suggested answers): the app itself collects no personal data and has no account. Backups: the optional automatic backup is saved by Android to the user's own Google backup, not to your servers, so you do not receive it; answer the Data safety form consistently with docs/PRIVACY_POLICY.md (data is not collected by you, backups are user-controlled and can be turned off in the app). AdMob (third party, free version only) collects the **advertising ID / device identifiers** for advertising. Purchases are handled by Google Play. Data is encrypted in transit; no deletion request needed because nothing is stored on your servers.
6. **In-app product** (Monetize > Products > In-app products): product ID **`premium_unlock`** (must match exactly), name "LifeDrop Pro", description "Remove ads and unlock all themes, fonts, widgets and insights. One-time payment.", **price (see section 5)**, status **Active**. The product only appears after an app build with Billing 8 is uploaded to a testing track.
7. **Testing**: Setup > License testing: add your Gmail accounts. Upload the `.aab` to **Internal testing**, install from the Play link on your phone, then check: Settings/Profile > Go Pro shows the price, Upgrade opens the Google Play sheet, buying unlocks Pro and removes ads, **Restore purchase** works after reinstall. (A build installed directly from Android Studio cannot buy, so the price will not load.)
8. Move to **Production** with a **staged rollout** (20% first), then 100%.

## 4a. Troubleshooting: "Price unavailable" / "Loading price" on the Pro screen
Google Play only returns the price (and allows the purchase) to an app that Play recognizes. A debug APK installed from Android Studio or a file is usually **not** recognized, so the price stays unavailable. This is expected. To see the price:
1. In Play Console create the app with package `com.lifedropdaily.app`, create the one-time product `premium_unlock`, add a **Buy** purchase option with the price and **Activate** both the purchase option and the product.
2. Upload the signed `.aab` (versionCode 1) to **Internal testing**, add your Gmail as a tester, publish the internal release, and install the app from the opt-in link **on a phone signed in with that Gmail**.
3. Add the same Gmail under Settings > License testing so test purchases are free.
4. New products can take a few hours to appear. The product ID must be exactly `premium_unlock`.
The Pro screen then shows the price, "Upgrade for Rs 149" opens the Google Play purchase sheet, and "Try again" re-checks Google Play.

## 4b. If you run into the "target API" or Billing version error
Rebuild with the versions in this project. Do not lower targetSdk or the Billing Library.

## 5. Choosing the price (one-time Pro)
Recommended: **Rs 149** for India (good range Rs 99 to Rs 199), about **US$2.99** elsewhere. Reasons: Indian users are price-sensitive but will pay once for ad-free + themes/fonts + widgets; Rs 149 feels like "a coffee", while Rs 99 sells more units but earns less and Rs 199+ sells noticeably less for a journaling/habit app. Consider a **Rs 99 launch offer** for the first 2 to 4 weeks (Play Console > Promotions), then raise to Rs 149.
Rough earnings per sale at Rs 149: after Google's service fee (about 15% for small developers; check Play Console) and tax, expect roughly Rs 105 to 125. Set the Indian price manually and let Play convert other countries (or set them by hand). Prices can be changed later; existing buyers keep Pro.

## 6. After launch
- Watch Play Console > Android vitals (crashes, ANRs) and reviews; reply to reviews.
- Each autumn: no calendar update is needed (dates are computed); optionally add verified dates to `calendar.json`.
- Every update: increase `versionCode` in `app/build.gradle.kts`, rebuild the signed bundle, upload.
