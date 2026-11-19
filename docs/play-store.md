# Publishing Reel Block on Google Play

Status on 19 Nov 2026: the code and the files are ready. The steps in **What only the owner can do** are
not done. Google reviews every app that uses the accessibility API, so approval is not certain.

## Free and Pro

| | Free | Pro (one Google Play purchase) |
|---|---|---|
| Close the Instagram Reels viewer | Yes | Yes |
| Pause, Quick Settings tile, self-check | Yes | Yes |
| This week chart, today and all-time counts | Yes | Yes |
| Schedule: block Reels only at chosen times and days | | Yes |
| Last 12 months chart (counts kept 366 days instead of 30) | | Yes |

In-app product ID: **`pro`** (one-time, "in-app product", not a subscription). The code is in
[`Pro.java`](../src/dev/local/reelblock/Pro.java). The app asks Google Play what is owned each time it
opens, so a reinstall or a new phone restores Pro, and a refund removes it.

## What is in the repo

| Play requirement | Where | Source of the rule |
|---|---|---|
| Android App Bundle | `./build.sh` makes `ReelBlock.aab` (bundletool 1.18.3) | Play Console requires AAB for new apps |
| targetSdk 36 | `AndroidManifest.xml` | [Target API level](https://developer.android.com/google/play/requirements/target-sdk): from 31 Aug 2026 |
| Billing Library 8 or later | 8.3.0, pinned in `build.sh`; version meta-data in the manifest | [Deprecation FAQ](https://developer.android.com/google/play/billing/deprecation-faq): v8 until 31 Aug 2027 |
| Acknowledge purchases in 3 days | `Pro.apply` | Play Billing docs |
| Back works at targetSdk 36 | `OnBackInvokedCallback` in `MainActivity` | [Android 16 changes](https://developer.android.com/about/versions/16/behavior-changes-16): `onBackPressed` is not called |
| Edge-to-edge | `Ui.edgeToEdge` (already in 2.0) | Android 15/16 changes |
| Prominent disclosure before the accessibility setting | Welcome page 2, **Agree and continue** (already in 2.0) | [Accessibility API policy](https://support.google.com/googleplay/android-developer/answer/10964491) |
| Updates only through Play | **Get update** opens the Play listing when Play installed the app | Device and Network Abuse policy |
| Privacy policy | [docs/privacy.md](privacy.md) | User Data policy |
| Icon 512 x 512 PNG | [store/play-icon-512.png](store/play-icon-512.png) | Store listing |
| Feature graphic 1024 x 500 JPEG | [store/play-feature-graphic.jpg](store/play-feature-graphic.jpg) | Store listing |

Phone screenshots: use `docs/screens/after-home-on-light.png`, `after-welcome-2-light.png`,
`after-pause-light.png` and `after-home-on-dark.png` (431 x 958, within Play's 320 to 3840 px and 2:1 limits),
plus `out/screens/Shots/home_pro_light.png` after `tools/screens/run.sh`. These are Robolectric renders,
not photos of a phone. Play allows that, but real phone screenshots look better.

## What only the owner can do

1. **Developer account.** Create a Play Console account (one-time $25 fee) and finish identity
   verification. A personal account created after 13 Nov 2023 must run a **closed test with at least 12
   testers, opted in for at least 14 days in a row**, before it can publish to production
   ([Play Console Help 14151465](https://support.google.com/googleplay/android-developer/answer/14151465)).
2. **Create the app.** Name "Reel Block", default language English, App, Free. ("Free" means free to
   download. In-app purchases are allowed.) Add a payments profile, so you can sell Pro.
3. **Signing.** Choose Play App Signing. To let people move between the GitHub APK and the Play app without
   uninstalling, upload the existing key (`debug.keystore`, alias `reelblock`, the certificate in
   `release.sh`) with the **"Export and upload a key from Java keystore"** option (Google's PEPK tool). If you
   let Google make a new key, Play and GitHub installs cannot update each other. Back up `debug.keystore`
   first: if it is lost, GitHub users cannot get updates.
4. **In-app product.** Monetize > Products > In-app products > Create: ID `pro`, name "Reel Block Pro",
   description "A schedule and a 12-month chart. One payment.", your price, Activate.
   You can make the product only after you upload a build that has the BILLING permission (step 5).
5. **Internal testing.** Run `NO_INSTALL=1 ./build.sh`, upload `ReelBlock.aab` to Testing > Internal
   testing, and add your Google account as a tester and as a **license tester** (Settings > License testing).
   Install from the opt-in link, buy Pro with the test card, and check: the Pro rows appear; the schedule
   works; uninstall, reinstall, and Pro comes back; refund it in Order management, open the app, and Pro goes.
6. **App content** (Policy > App content):
   - **Privacy policy:** `https://github.com/SuzalShrestha/reelblock/blob/main/docs/privacy.md` (after merge).
   - **Ads:** No.
   - **App access:** All functions are available without special access. Note for the reviewer: "Turn on
     Reel Block in Settings > Accessibility. Then open Instagram and tap Reels: the app presses Back."
   - **Content rating** (IARC questionnaire): category Utility/Productivity; answer No to violence, sexual
     content, language, drugs, gambling, user interaction, sharing location, and digital purchases **Yes**.
   - **Target audience:** 13 and over (any under-13 group adds the Families policy). Not designed for children.
   - **Data safety:** "No data collected" and "No data shared". Reasons, from [Google's Data safety
     help](https://support.google.com/googleplay/android-developer/answer/10787469): "collect" means sent off
     the phone, and Reel Block sends nothing (no INTERNET permission, telemetry stubbed out). Card data that
     Google Play collects directly does not need to be declared. Purchase status is processed only on the
     phone. *Not checked at the source:* whether acknowledging a purchase to the Play Store app counts as
     "sharing". It is a transfer to Google Play acting as your payment service, which the help calls exempt.
     If a reviewer disagrees, declare "Purchase history: shared, for app functionality".
   - **Accessibility API declaration:** say Reel Block is **not** an accessibility tool
     (`isAccessibilityTool=false`). Purpose: "Closes the Instagram Reels viewer, so the user can use
     Instagram without endless short videos (digital wellbeing). It reads only the view ID of the open
     Instagram screen and presses Back. It works only in com.instagram.android." Data: none collected or
     shared. **Video:** record the phone screen (Settings > screen recorder) from a fresh install: the welcome
     page, the **What Reel Block can see** page, tap **Agree and continue**, then the accessibility setting.
     Upload it to YouTube as unlisted and paste the link.
7. **Store listing.** Use the text below, the icon, the feature graphic and the screenshots. Category:
   Productivity. Do not use Instagram's logo or name in the icon or the app name.
8. **Release.** Closed testing (12 testers, 14 days, if step 1 asks for it), then Production.

## Store listing text

**App name** (30 max): `Reel Block`

**Short description** (80 max): `Closes Instagram Reels the moment they open. Keep your feed, DMs and Stories.`

**Full description:**

```
You open Instagram to answer one message. Forty minutes later you are still swiping Reels.

Reel Block closes the Instagram Reels viewer the moment it opens, by pressing Back for you. Everything
else in Instagram keeps working: your feed, Stories, messages, profiles and posting.

FREE
• Closes Reels from the Reels tab
• Pause for 15 minutes, 1 hour, or until you turn it back on
• Quick Settings tile
• A chart of the Reels it closed this week
• A self-check that tells you when Android stopped it or an Instagram update changed the Reels screen

PRO (one payment, no subscription)
• Schedule: block Reels only at the times and days you choose, for example work hours
• A chart of the last 12 months

PRIVATE BY DESIGN
• No internet permission. Nothing can leave your phone.
• No account, no ads, no analytics, no logs.
• It works only in Instagram and never reads your messages, posts, photos or passwords.
• Open source: github.com/SuzalShrestha/reelblock

HOW IT WORKS
Reel Block uses Android's accessibility service to see the name of the open Instagram screen. When that
screen is the Reels viewer, it presses Back. Before you turn it on, the app shows exactly what it can see
and asks you to agree. You can turn it off at any time in Settings > Accessibility.

Reel Block is not made by or connected to Instagram or Meta.
```

## Known limits

- Purchases are not tested on a phone yet. That needs step 5 (a build installed from the internal track).
- The schedule hero does not change by itself when a schedule window starts or ends while the app is open.
  It changes the next time the app opens. Blocking itself always follows the schedule.
- Pro status is a private preference, so a rooted phone can turn it on. Server-side checks are not worth it at
  a one-time price.
- Welcome page 3 shows the Android 13 "restricted setting" help on every phone. Apps installed from Play do
  not need it. It is harmless.

## iOS and the App Store

Not built, by decision (19 Nov 2026). Reel Block's method has no iOS equivalent: iOS gives no app a way to
see or control another app's screen. Apple's Screen Time APIs (FamilyControls, ManagedSettings,
DeviceActivity) can shield a **whole** app such as Instagram, on a schedule or after a time limit, or block
web domains. They cannot block one screen (Reels) inside an app. They also need Apple to grant the Family
Controls entitlement. So an iOS app would be a different product, for example:

- a Screen Time app that shields all of Instagram on a schedule, sold with StoreKit 2, or
- a Safari extension that blocks `instagram.com/reels` on the web only.

Check before building: Apple's Family Controls entitlement request and its current App Review rules for
Screen Time apps. These were not read for this note.
