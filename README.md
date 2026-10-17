# LifeDrop Daily

See where you are in the year and make today count. LifeDrop Daily is a calm, private Android app that gives you one question, one small challenge and one reflection every day. It works fully offline and needs no account.

- **Package name:** `com.lifedropdaily.app`
- **Developer:** Akash Pol
- **Contact:** akpolstudio@gmail.com
- **Privacy policy:** [docs/PRIVACY_POLICY.md](docs/PRIVACY_POLICY.md)

## Features
- Year grid showing how much of the year has passed and what is left
- Countdowns to festivals and your own events
- Daily question, challenge and reflection (Think tab)
- XP, streaks and sticker badges, plus a year review
- Private journal with tags and favorites, with export and delete options
- **Backup & Restore** (Settings): save all progress to a file and restore it after reinstalling, plus an optional automatic backup through Android Auto Backup (your own Google backup)
- Reminder, event, festival and sky notifications, plus an optional day-count notification
- Home-screen widgets
- **Pro (one-time purchase):** no ads, all themes and custom fonts, unlimited events, streak protection, advanced insights and extra widgets

## Tech stack
- Kotlin and Jetpack Compose (Material 3)
- Room for local storage
- Jetpack Glance for widgets
- Google Play Billing Library 8.3.0 for the Pro purchase (product ID `premium_unlock`)
- Google AdMob for ads in the free version
- minSdk 26, compileSdk / targetSdk 36
- Android Gradle Plugin 8.9.1, Gradle 8.11.1, JDK 17

## Getting started
1. Install the latest Android Studio and open this folder as a project.
2. Let Gradle sync. Accept the prompt to install SDK Platform 36 if asked.
3. Run the `debug` build on an emulator or phone.

Debug builds use Google's public **test** ad IDs. Google Play cannot return the Pro price to a build installed directly from Android Studio or an APK file, so the purchase button only works for an app installed from a Play Console testing track.

## Configuration (not committed to Git)
Create these files in the project root. Both are listed in `.gitignore`.

`local.properties` (optional, your own AdMob IDs):
```
ADMOB_APP_ID=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
ADMOB_BANNER_ID=ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ
ADMOB_INTERSTITIAL_ID=ca-app-pub-XXXXXXXXXXXXXXXX/WWWWWWWWWW
```

`keystore.properties` (for signed release builds):
```
storeFile=C:/Keys/lifedrop-release.jks
storePassword=your_keystore_password
keyAlias=lifedrop
keyPassword=your_key_password
```
Never commit your `.jks` file or passwords.

## Build a release
```
./gradlew bundleRelease
```
The signed `.aab` is created under `app/release/`. Raise `versionCode` in `app/build.gradle.kts` before every upload to Google Play. In Android Studio you can also use **Build > Generate Signed App Bundle / APK**.

## Backup and restore
- **Manual:** Settings > Backup & Restore > **Back up** saves one `.json` file (streak, XP, journal, answers, favorites, events and settings) wherever the user picks, using Android's file picker (no storage permission needed). **Restore** reads the file, checks it, shows what it contains and replaces the data on the phone in one database transaction. The Pro purchase is never part of a backup; it comes back with **Restore purchase**.
- **Automatic:** the app keeps a copy of the same data in `files/auto_backup.json`. `res/xml/data_extraction_rules.xml` and `res/xml/backup_rules.xml` tell Android Auto Backup to save **only that file** to the user's Google backup. After a reinstall the app finds the restored copy and asks whether to use it. The switch in Settings turns the copy off and deletes it.
- Code: `data/Backup.kt` (create, validate, restore, auto copy) and `ui/BackupUi.kt` (Settings section and the restore prompt).
- Backup format version is `1` (`Backup.FORMAT`). Increase it if the backup contents ever change.

## Testing in-app purchases
1. Upload the release bundle to a Play Console **Closed** or **Internal** testing track.
2. Add testers' Gmail accounts to the track and to **Settings > License testing**.
3. Testers install from the Play opt-in link and tap **Upgrade**. Google shows a test purchase and nothing is charged.

More detail is in [TESTING.md](TESTING.md).

## Documentation
| File | What it covers |
|---|---|
| [docs/RELEASE_GUIDE.md](docs/RELEASE_GUIDE.md) | Play Store release and Play Console checklist |
| [docs/STORE_LISTING.md](docs/STORE_LISTING.md) | Store listing text |
| [docs/PRIVACY_POLICY.md](docs/PRIVACY_POLICY.md) | Privacy policy (also as `.txt` and [`privacy-policy.html`](docs/privacy-policy.html)) |
| [docs/UPDATE_CALENDAR_YEARLY.md](docs/UPDATE_CALENDAR_YEARLY.md) | Yearly calendar and festival data update |
| [TESTING.md](TESTING.md) | Testing notes |

## License
Copyright (c) Akash Pol. All rights reserved. Add a license here if you want to open-source the project.
