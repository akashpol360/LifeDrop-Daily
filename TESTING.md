# LifeDrop Daily - release test checklist

Run on a real phone with the **internal testing** build from Google Play (see docs/RELEASE_GUIDE.md).

- First launch: splash, onboarding, notification permission (Android 13+), correct day number.
- Daily drop stays the same all day; challenge, think answer and journal entry give XP once per day; all four give +10.
- Journal: add several entries a day, edit and delete (with confirmation); My Events: add, edit, delete (with confirmation), mark done.
- Free version: banner shows above the tabs, interstitial after the listed buttons, "Go Pro" on Home/Profile/Settings opens the Pro screen with the Google Play price.
- Buy Pro with a license-tester account: ads disappear, PRO MEMBER badge appears, themes/fonts/widgets unlock. Restore purchase after reinstall. Refund test: Pro turns off after Google Play updates.
- Calendar: festivals, moon phases, eclipses; switch months and years.
- Widgets (small/medium/large, Progress, Streak), reminders, optional notification-bar item, sky alerts.
- Dark mode, font size 200%, TalkBack on Home/Profile/Pro, rotation, small phone and tablet, airplane mode (core features still work).
- Android 15/16 phone: content is not hidden behind the status or navigation bar; keyboard does not cover text fields.

## Closed testing with Google test billing
- Build the release bundle (`bundleRelease`) and upload it to the Closed testing track. Raise `versionCode` for every upload.
- Add testers' Gmail accounts to the closed testing list AND to Play Console > Settings > License testing.
- Testers install from the Play opt-in link. Tapping Upgrade opens the real Google Play sheet with a "test purchase, you will not be charged" notice.
- Make sure `premium_unlock` is Active (Monetize > Products > In-app products) with a Buy purchase option.

## Backup and restore (version 1.0.1)
1. Use the app for a day: finish a drop, think answer, journal entry, add an event, change the theme. Note streak and XP.
2. Settings > Backup & Restore > **Back up**, save the file (Downloads or Drive). Toast "Backup saved" appears.
3. Settings > Privacy > **Reset all progress and data**. Streak, journal and events are gone.
4. Settings > Backup & Restore > **Restore**, pick the file. The summary shows level, XP, streak and counts. Confirm: everything returns, theme and reminder times are back, widgets update.
5. Pick a wrong file (a photo or text file): "This is not a LifeDrop backup file." and nothing changes.
6. Pro is not changed by a restore. After uninstalling, use **Restore purchase** on the Pro screen.
7. Automatic backup: leave "Automatic backup" on, use the app, then send it to the background. Check with `adb shell bmgr backupnow com.lifedropdaily.app`, then `adb uninstall com.lifedropdaily.app`, install the same build again and run `adb shell bmgr restore`; on first launch "Welcome back" offers to restore. Choose Restore or Start fresh.
8. Turn "Automatic backup" off: the copy on the phone is deleted (Settings shows no "Last copy").
